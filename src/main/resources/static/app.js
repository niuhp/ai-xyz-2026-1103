"use strict";

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

document.querySelectorAll("[data-example]").forEach(button => {
    button.addEventListener("click", () => {
        description.value = examples[button.dataset.example];
        description.focus();
    });
});

document.querySelector("#draft-form").addEventListener("submit", async event => {
    event.preventDefault();
    const text = description.value.trim();
    if (!text) {
        description.setCustomValidity("请输入问题描述");
        description.reportValidity();
        return;
    }
    description.setCustomValidity("");
    submitButton.disabled = true;
    submitButton.textContent = "正在整理…";
    document.querySelector("#result").hidden = true;
    try {
        const response = await fetch("/api/v1/ticket-drafts", {
            method: "POST", headers: {"Content-Type": "application/json"},
            body: JSON.stringify({description: text})
        });
        const result = await response.json();
        renderResult(result, response.ok);
    } catch (error) {
        renderResult({message: "无法连接服务或响应无法读取，请稍后重试。"}, false);
    } finally {
        submitButton.disabled = false;
        submitButton.textContent = "生成草稿";
    }
});

description.addEventListener("input", () => description.setCustomValidity(""));

function renderResult(result, ok) {
    document.querySelector("#result").hidden = false;
    const title = document.querySelector("#result-title");
    const message = document.querySelector("#result-message");
    const fields = document.querySelector("#draft-fields");
    fields.replaceChildren();
    title.textContent = ok ? {
        READY: "草稿已整理 · 请人工核对", NEEDS_INFO: "还需要补充信息", UNSUPPORTED: "这段内容不属于软件问题反馈"
    }[result.status] || "处理结果" : "本次未能生成草稿";
    message.textContent = ok ? result.status === "NEEDS_INFO"
        ? "请补充：" + result.missingFields.join("、") + "。"
        : result.status === "READY" ? "必要字段已具备，确认内容后可手动录入工单系统。"
            : "请描述具体的软件故障、异常或改进建议。" : result.message;
    if (ok && result.draft) {
        Object.entries(labels).forEach(([key, label]) => {
            const dt = document.createElement("dt");
            const dd = document.createElement("dd");
            dt.textContent = label;
            dd.textContent = key === "category" ? categories[result.draft[key]] || result.draft[key]
                : result.draft[key] || "未提供";
            fields.append(dt, dd);
        });
    }
    const usage = result.usage && result.usage.totalTokens != null
        ? " · " + result.usage.totalTokens + " Token（上游报告，非费用账单）" : "";
    document.querySelector("#result-meta").textContent = (result.requestId ? "请求编号 " + result.requestId : "")
        + (ok ? " · " + result.elapsedMillis + " ms" + usage : "");
    document.querySelector("#result-json").textContent = JSON.stringify(result, null, 2);
}
