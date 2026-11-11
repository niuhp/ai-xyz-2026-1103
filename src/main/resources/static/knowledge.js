const input = document.querySelector("#knowledge-description");
const buttons = [...document.querySelectorAll("#knowledge-baseline,#knowledge-retrieval")];
const panels = {BASELINE: document.querySelector("#baseline-result"), RETRIEVAL: document.querySelector("#retrieval-result")};
const examples = {
    export: "教学运维台导出 Excel，选择一整年的数据，等待很久提示超时；缩小日期范围后可以导出。应该先排查什么？",
    dictionary: "教学运维台的同一个字典类型里，相同字典值能重复添加，下拉框出现多个选项，希望避免重复。",
    no_match: "量子纠缠实验的光子偏振发生变化，如何调整光学仪器？"
};
let demo = false;
let activeController = null;
let generation = 0;
function element(tag, text, className) {
    const node = document.createElement(tag);
    node.textContent = text;
    if (className) node.className = className;
    return node;
}
function reset() {
    generation++;
    activeController?.abort();
    for (const [mode, panel] of Object.entries(panels)) {
        panel.replaceChildren(element("h2", mode === "BASELINE" ? "不查知识库" : "检索后生成"), element("p", "等待生成建议。"));
    }
}
input.addEventListener("input", () => { input.setCustomValidity(""); reset(); });
document.querySelector("#knowledge-scenario").addEventListener("change", reset);
document.querySelectorAll("[data-knowledge-example]").forEach(button => {
    button.addEventListener("click", () => { input.value = examples[button.dataset.knowledgeExample]; input.setCustomValidity(""); reset(); });
});
fetch("/api/v1/knowledge").then(response => {
    if (!response.ok) throw new Error();
    return response.json();
}).then(info => {
    document.querySelector("#knowledge-info").textContent = info.caseCount + " 条教学案例 · 本地 TF-IDF 词项检索 · 语料 " + info.corpusVersion;
}).catch(() => { document.querySelector("#knowledge-info").textContent = "知识库信息暂时无法加载，请检查服务。"; });
if (new URLSearchParams(location.search).get("demo") === "1") {
    fetch("/api/v1/demo").then(response => response.ok ? response.json() : null).then(info => {
        demo = info?.enabled === true;
        document.querySelector("#knowledge-demo").hidden = !demo;
        if (demo) document.querySelector("#knowledge-disclosure").textContent = "描述与案例只发送给本机模拟服务，不消耗真实模型额度；回答为固定演示数据。";
    }).catch(() => {});
}
input.value = examples.export;
buttons.forEach(button => button.addEventListener("click", () => generate(button.id.endsWith("baseline") ? "BASELINE" : "RETRIEVAL")));
async function generate(mode) {
    const description = input.value.trim();
    if (!description) {
        input.setCustomValidity("请填写具体问题描述");
        input.reportValidity();
        return;
    }
    input.setCustomValidity("");
    const panel = panels[mode];
    const title = mode === "BASELINE" ? "不查知识库 · 通用建议" : "检索后生成 · 教学案例参考";
    const current = generation;
    const controller = new AbortController();
    activeController = controller;
    buttons.forEach(item => { item.disabled = true; });
    panel.replaceChildren(element("h2", title), element("p", "正在处理…"));
    try {
        const marker = demo ? "[demo:" + document.querySelector("#knowledge-scenario").value + ":" + crypto.randomUUID() + "] " : "";
        const response = await fetch("/api/v1/knowledge/advice", {
            method: "POST", headers: {"Content-Type": "application/json"}, signal: controller.signal,
            body: JSON.stringify({description: marker + description, mode})
        });
        const result = await response.json();
        if (current !== generation) return;
        panel.replaceChildren(element("h2", title), element("p", response.ok ? result.message : result.message || "本次处理失败"));
        if (!response.ok) {
            if (result.action) panel.append(element("p", result.action, "notice"));
        } else {
            renderAdvice(panel, result);
        }
        panel.append(element("p", "请求编号 " + (result.requestId || "未知") + " · " + (result.status || result.code || "未知状态")
            + (response.ok ? " · " + result.elapsedMillis + " ms" : ""), "meta"));
        const details = element("details", "");
        details.append(element("summary", "查看接口 JSON"), element("pre", JSON.stringify(result, null, 2)));
        panel.append(details);
    } catch (error) {
        if (current === generation) panel.replaceChildren(element("h2", title), element("p", "未取得可用建议，请检查连接后重试。输入已保留。"));
    } finally {
        activeController = null;
        buttons.forEach(item => { item.disabled = false; });
    }
}
function renderAdvice(panel, result) {
    const advice = result.advice;
    if (advice) {
        panel.append(element("h3", "排查方向"), element("p", advice.summary));
        const steps = element("ol", "");
        for (const step of advice.steps) {
            const row = element("li", step.text);
            for (const evidence of step.evidence) {
                row.append(element("blockquote", "[" + evidence.caseId + "] 原文：“" + evidence.quote + "”", "evidence"));
            }
            steps.append(row);
        }
        panel.append(steps, element("h3", "适用条件与待核实项"));
        for (const limitation of advice.limitations) panel.append(element("p", limitation));
    }
    if (result.sources.length) {
        panel.append(element("h3", "本次召回的原始案例"), element("p", "召回不等于引用；请展开核对版本、症状与适用范围。分数不是正确率。", "meta"));
        for (const source of result.sources) {
            const doc = source.document;
            const details = element("details", "", "case-content");
            details.append(element("summary", "[" + doc.id + "] " + doc.title + " · 词项相似度 " + source.score.toFixed(3)));
            details.append(element("p", "教学模拟 · " + doc.system + " / " + doc.module + " · " + doc.version + " · " + doc.updatedAt));
            details.append(element("p", "症状：" + doc.symptoms), element("p", "处理记录：" + doc.resolution), element("p", "适用范围：" + doc.applicability));
            panel.append(details);
        }
    }
}
