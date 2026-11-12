const input = document.querySelector("#retrieval-description");
const button = document.querySelector("#compare-button");
const panels = {lexical: document.querySelector("#lexical-result"), semantic: document.querySelector("#semantic-result")};
const titles = {lexical: "TF-IDF · 词项检索", semantic: "Embedding · 语义检索"};
const examples = {
    literal: "导出 Excel 选择全年数据等待很久提示超时，缩小日期范围后正常",
    synonym: "想把整年的报表下载到电脑，一直转圈拿不到文件；只选一周就能拿到。",
    code: "登录成功后访问业务接口立即返回 401",
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
    for (const [key, panel] of Object.entries(panels)) panel.replaceChildren(node("h2", titles[key]), node("p", "等待对照。"));
    document.querySelector("#compare-meta").textContent = "";
}
function info(index) {
    document.querySelector("#index-info").textContent = "向量索引 " + index.state + " · 模型 " + (index.model || "未配置")
        + " · 维度 " + index.dimensions + " · 指纹 " + index.fingerprint.slice(0, 12);
}
fetch("/api/v1/knowledge/semantic").then(response => response.json()).then(info)
    .catch(() => { document.querySelector("#index-info").textContent = "索引状态读取失败，请检查服务"; });
{
    fetch("/api/v1/demo").then(response => response.ok ? response.json() : null).then(result => {
        if (result?.enabled) document.querySelector("#retrieval-notice").textContent = "本地协议模拟：向量为人工固定数组，只验证请求、排序、校验与展示，不具备语义理解能力，也不能用来证明检索效果。不消耗真实模型额度。";
    }).catch(() => {});
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
    controller = new AbortController();
    button.disabled = true;
    try {
        for (const [key, panel] of Object.entries(panels)) panel.replaceChildren(node("h2", titles[key]), node("p", "正在检索…"));
        const response = await fetch("/api/v1/knowledge/compare", {method: "POST", headers: {"Content-Type": "application/json"},
            signal: controller.signal, body: JSON.stringify({description})});
        const result = await response.json();
        if (current !== generation) return;
        if (!response.ok) throw new Error(result.message || "本次对照失败");
        for (const key of Object.keys(panels)) render(panels[key], titles[key], result[key]);
        info(result.semanticIndex);
        document.querySelector("#compare-meta").textContent = "语料 " + result.corpusVersion + " · 请求编号 " + result.requestId;
    } catch (error) {
        if (current === generation) for (const [key, panel] of Object.entries(panels)) panel.replaceChildren(node("h2", titles[key]), node("p", error.message || "连接失败，输入已保留"));
    } finally { controller = null; button.disabled = false; }
});
function render(panel, title, result) {
    panel.replaceChildren(node("h2", title), node("p", result.message), node("p", result.status + " · 阈值 " + result.threshold + " · " + result.elapsedMillis + " ms"));
    for (const match of result.matches) {
        const doc = match.document;
        const details = node("details", "");
        details.open = true;
        details.append(node("summary", "[" + doc.id + "] " + doc.title + " · 相似度 " + match.score.toFixed(3)),
            node("p", "症状：" + doc.symptoms), node("p", "适用范围：" + doc.applicability));
        panel.append(details);
    }
    const raw = node("details", ""); raw.append(node("summary", "查看检索 JSON"), node("pre", JSON.stringify(result, null, 2))); panel.append(raw);
}
