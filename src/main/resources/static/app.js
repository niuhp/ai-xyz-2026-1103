"use strict";

import {consumeSse} from "./sse.mjs";

const examples = {
    complete: "我使用 RuoYi-Vue 3.8.9，在系统管理的字典管理里，给同一个字典类型添加两条数据，字典值相同、标签不同，两次都提示成功。现在下拉框里有两个选项，希望重复添加时提示字典值已经存在。",
    missing: "昨天下午开始导出失败，提示超时，重新登录也没用。",
    unrelated: "帮我写一首关于秋天的诗。"
};
const labels = {
    system: "系统名称", module: "功能模块", category: "问题分类", summary: "问题摘要",
    actualResult: "实际现象", expectedResult: "预期结果", occurredAtText: "发生时间", version: "系统版本"
};
const categories = {
    DATA_DUPLICATION: "重复数据", QUERY: "查询", EXPORT: "导出", ACCESS: "登录 / 权限", OTHER: "其他"
};
const description = document.querySelector("#description");
const submitButton = document.querySelector("#submit-button");
const confirmButton = document.querySelector("#confirm-button");
let activeDraftId = null;
let demoEnabled = false;
if (new URLSearchParams(location.search).get("demo") === "1") {
    fetch("/api/v1/demo").then(response => response.ok ? response.json() : null).then(config => {
        demoEnabled = config?.enabled === true;
        document.querySelector("#demo-controls").hidden = !demoEnabled;
        if (demoEnabled) {
            description.value = examples.complete;
            document.querySelector(".actions span").textContent = "演示数据仅发送到本机模拟服务，不消耗模型额度。";
        }
    }).catch(() => { demoEnabled = false; });
}

document.querySelectorAll("[data-example]").forEach(button => {
    button.addEventListener("click", () => {
        description.value = examples[button.dataset.example];
        description.focus();
    });
});

const modeSelect = document.querySelector("#generation-mode");
const cancelButton = document.querySelector("#cancel-button");
let activeController = null;
cancelButton.addEventListener("click", () => activeController?.abort());

document.querySelector("#draft-form").addEventListener("submit", async event => {
    event.preventDefault();
    const text = description.value.trim();
    if (!text) {
        description.setCustomValidity("请输入问题描述");
        description.reportValidity();
        return;
    }
    description.setCustomValidity("");
    const streaming = modeSelect.value === "stream";
    const controller = new AbortController();
    activeController = controller;
    submitButton.disabled = true;
    modeSelect.disabled = true;
    confirmButton.hidden = true;
    activeDraftId = null;
    submitButton.textContent = "正在整理…";
    cancelButton.hidden = false;
    document.querySelector("#result").hidden = true;
    document.querySelector("#stream-panel").hidden = !streaming;
    document.querySelector("#stream-text").textContent = "";
    document.querySelector("#stream-status").textContent = "正在连接服务…";
    document.querySelector("#stream-timing").textContent = "";
    const startedAt = performance.now();
    let firstDeltaAt = null;
    let terminal = false;
    try {
        const response = await fetch("/api/v1/ticket-drafts" + (streaming ? "/stream" : ""), {
            method: "POST", signal: controller.signal,
            headers: {"Content-Type": "application/json", "Accept": "text/event-stream, application/json"},
            body: JSON.stringify({description: demoEnabled
                ? "[demo:" + document.querySelector("#demo-scenario").value + ":" + crypto.randomUUID() + "] " + text
                : text})
        });
        if (!response.ok || !streaming) {
            const result = await response.json();
            renderResult(result, response.ok);
            terminal = true;
            if (streaming) document.querySelector("#stream-status").textContent = "请求未进入生成阶段";
            return;
        }
        if (!response.headers.get("Content-Type")?.includes("text/event-stream")) {
            throw new Error("服务器没有返回流式响应");
        }
        await consumeSse(response.body, (event, data) => {
            if (terminal) return;
            const body = JSON.parse(data);
            if (event === "status") {
                document.querySelector("#stream-status").textContent = body.message;
            } else if (event === "delta") {
                if (typeof body.text !== "string") throw new Error("无效的片段");
                const preview = document.querySelector("#stream-text");
                if (preview.textContent.length + body.text.length > 32000) throw new Error("预览内容过长");
                preview.textContent += body.text;
                if (firstDeltaAt === null && body.text.length) firstDeltaAt = performance.now();
                document.querySelector("#stream-status").textContent = "正在接收模型片段，尚未形成有效草稿";
                document.querySelector("#stream-timing").textContent = firstDeltaAt === null ? ""
                    : "首个模型片段：" + ((firstDeltaAt - startedAt) / 1000).toFixed(2) + " 秒（浏览器观测）";
            } else if (event === "result" || event === "failure") {
                renderResult(body, event === "result");
                terminal = true;
                document.querySelector("#stream-status").textContent = event === "result"
                    ? "完整响应已校验，处理结果见下方" : "本次生成失败，预览内容不可提交";
                document.querySelector("#stream-timing").textContent += " · 总耗时："
                    + ((performance.now() - startedAt) / 1000).toFixed(2) + " 秒（浏览器观测）";
                return false;
            }
        });
        if (!terminal) throw new Error("连接提前结束，未收到最终结果");
    } catch (error) {
        if (!terminal) {
            const cancelled = controller.signal.aborted;
            renderResult({message: cancelled ? "已停止等待，本次没有收到可用草稿。"
                : "响应中断或无法读取，本次未能取得可用草稿。", action:
                "输入已保留；预览尚未校验。停止本地等待不保证上游停止生成或计费，重试前请留意用量。"}, false);
            if (streaming) document.querySelector("#stream-status").textContent = cancelled
                ? "已停止等待 · 预览未完成" : "连接异常 · 预览不可提交";
        }
    } finally {
        controller.abort();
        activeController = null;
        submitButton.disabled = false;
        modeSelect.disabled = false;
        cancelButton.hidden = true;
        submitButton.textContent = "生成草稿";
    }
});

description.addEventListener("input", () => description.setCustomValidity(""));

confirmButton.addEventListener("click", async () => {
    if (!activeDraftId) return;
    confirmButton.disabled = true;
    confirmButton.textContent = "正在创建…";
    const draft = Object.fromEntries([...document.querySelectorAll("[data-draft-field]")]
        .map(field => [field.dataset.draftField, field.value]));
    try {
        const response = await fetch("/api/v1/ticket-drafts/confirm", {
            method: "POST", headers: {"Content-Type": "application/json"},
            body: JSON.stringify({draftId: activeDraftId, draft})
        });
        const result = await response.json();
        if (!response.ok) throw new Error(result.message || "工单创建失败，请检查必填信息。");
        document.querySelector("#ticket-success").textContent = "工单已创建，编号：" + result.ticketId;
        document.querySelector("#ticket-success").hidden = false;
        confirmButton.textContent = "已创建";
    } catch (error) {
        document.querySelector("#ticket-success").textContent = error.message;
        document.querySelector("#ticket-success").hidden = false;
        confirmButton.disabled = false;
        confirmButton.textContent = "确认并创建工单";
    }
});

function renderResult(result, ok) {
    document.querySelector("#result").hidden = false;
    const title = document.querySelector("#result-title");
    const message = document.querySelector("#result-message");
    const action = document.querySelector("#result-action");
    action.hidden = ok || !result.action;
    action.textContent = ok ? "" : result.action || "";
    const fields = document.querySelector("#draft-fields");
    fields.replaceChildren();
    activeDraftId = ok ? result.draftId || null : null;
    confirmButton.hidden = !activeDraftId;
    confirmButton.disabled = false;
    confirmButton.textContent = "确认并创建工单";
    document.querySelector("#ticket-success").hidden = true;
    title.textContent = ok ? {
        READY: "草稿已整理 · 请人工核对", NEEDS_INFO: "还需要补充信息", UNSUPPORTED: "这段内容不属于软件问题反馈"
    }[result.status] || "处理结果" : "本次未能生成草稿";
    message.textContent = ok ? result.status === "NEEDS_INFO"
        ? "请在下方补充缺失内容，核对完整后确认创建。缺少：" + result.missingFields.join("、") + "。"
        : result.status === "READY" ? "请核对并按需修改下方字段，再确认创建。"
            : "请描述具体的软件故障、异常或改进建议。" : result.message;
    if (ok && result.draft) {
        Object.entries(labels).forEach(([key, label]) => {
            const fieldLabel = document.createElement("label");
            fieldLabel.textContent = label;
            let input;
            if (key === "category") {
                input = document.createElement("select");
                Object.entries(categories).forEach(([value, name]) => {
                    const option = document.createElement("option");
                    option.value = value;
                    option.textContent = name;
                    input.append(option);
                });
            } else if (["actualResult", "expectedResult"].includes(key)) {
                input = document.createElement("textarea");
            } else {
                input = document.createElement("input");
                input.maxLength = key === "summary" ? 200 : 100;
            }
            input.dataset.draftField = key;
            input.value = result.draft[key] || "";
            fieldLabel.append(input);
            fields.append(fieldLabel);
        });
    }
    const usage = result.usage && result.usage.totalTokens != null
        ? " · " + result.usage.totalTokens + " Token（上游报告，非费用账单）" : "";
    document.querySelector("#result-meta").textContent = (result.requestId ? "请求编号 " + result.requestId : "")
        + (ok ? " · " + result.elapsedMillis + " ms" + usage : result.code ? " · " + result.code : "");
    document.querySelector("#result-json").textContent = JSON.stringify(result, null, 2);
}
