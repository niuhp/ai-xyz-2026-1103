const input = document.querySelector("#rerank-description");
const button = document.querySelector("#rerank-button");
const before = document.querySelector("#before-result");
const after = document.querySelector("#after-result");
const meta = document.querySelector("#rerank-meta");
const examples = {
    synonym: "想把整年的报表下载到电脑，一直转圈拿不到文件；只选一周就能拿到。",
    duplicate: "部署成多台服务后，同一个定时任务在一分钟里跑了好几遍。",
    vague: "报表有点问题，不知道是哪里坏了。",
    irrelevant: "量子纠缠实验的光子偏振发生变化，如何调整光学仪器？"
};
let generation = 0;
let controller = null;
function node(tag, text) {
    const element = document.createElement(tag);
    element.textContent = text;
    return element;
}
function reset() {
    generation++;
    controller?.abort();
    before.replaceChildren(node("h2", "重排前 · RRF 候选"), node("p", "等待对照。"));
    after.replaceChildren(node("h2", "重排后 · 可参考案例"), node("p", "等待对照。"));
    meta.textContent = "";
}
function card(match) {
    const doc = match.document;
    const element = node("section", "");
    element.append(node("h3", "[" + doc.id + "] " + doc.title), node("p", "症状：" + doc.symptoms),
        node("p", "适用范围：" + doc.applicability));
    return element;
}
function render(result) {
    const source = result.retrieval.hybrid;
    const ranked = result.rerank;
    before.replaceChildren(node("h2", "重排前 · RRF 候选"), node("p", source.status + " · " + source.message));
    for (const match of source.matches) before.append(card(match));
    after.replaceChildren(node("h2", "重排后 · 可参考案例"), node("p", ranked.status + " · " + ranked.message));
    for (const match of ranked.matches) after.append(card(match));
    if (ranked.assessment) {
        after.append(node("h3", "逐条判断（含未保留项）"));
        for (const item of ranked.assessment.assessments) {
            after.append(node("p", "[" + item.caseId + "] 分级 " + item.grade + " · " + item.reason));
            if (item.evidence) after.append(node("blockquote", "案例原文：" + item.evidence));
        }
    }
    const usage = ranked.usage;
    meta.textContent = "语料 " + result.retrieval.corpusVersion + " · 模型 " + (ranked.model || "未获得有效模型结果")
        + " · 检索及融合 " + source.elapsedMillis + " ms · 重排 " + ranked.elapsedMillis + " ms · 总计 " + result.elapsedMillis
        + " ms · Token " + (usage?.totalTokens ?? "未返回") + " · 请求编号 " + result.retrieval.requestId;
    const raw = node("details", "");
    raw.append(node("summary", "查看完整 JSON"), node("pre", JSON.stringify(result, null, 2)));
    after.append(raw);
}
input.value = examples.synonym;
input.addEventListener("input", () => { input.setCustomValidity(""); reset(); });
document.querySelectorAll("[data-query]").forEach(item => item.addEventListener("click", () => {
    input.value = examples[item.dataset.query]; input.setCustomValidity(""); reset();
}));
button.addEventListener("click", async () => {
    const description = input.value.trim();
    if (!description) { input.setCustomValidity("请填写问题描述"); input.reportValidity(); return; }
    input.setCustomValidity("");
    const current = generation;
    const active = new AbortController();
    controller = active;
    button.disabled = true;
    try {
        before.replaceChildren(node("h2", "重排前 · RRF 候选"), node("p", "正在检索并判断…"));
        after.replaceChildren(node("h2", "重排后 · 可参考案例"), node("p", "等待模型结果…"));
        const response = await fetch("/api/v1/knowledge/rerank", {method: "POST", headers: {"Content-Type": "application/json"},
            signal: active.signal, body: JSON.stringify({description})});
        const result = await response.json();
        if (current !== generation) return;
        if (!response.ok) throw new Error(result.message || "本次对照失败");
        render(result);
    } catch (error) {
        if (current === generation) after.replaceChildren(node("h2", "重排未完成"), node("p", error.message || "连接失败，输入已保留"));
    } finally {
        if (controller === active) controller = null;
        button.disabled = false;
    }
});
