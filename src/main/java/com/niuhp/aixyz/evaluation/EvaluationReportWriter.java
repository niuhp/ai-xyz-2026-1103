package com.niuhp.aixyz.evaluation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * 导出本地 JSON 和移动端可读 HTML；所有文本转义，不包含密钥。
 *
 * @author niuhaipeng
 */
@Component
public class EvaluationReportWriter {

    private final JsonMapper json;

    public EvaluationReportWriter(JsonMapper json) {
        this.json = json;
    }

    public void write(EvaluationReportDTO report, Path directory) throws IOException {
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("report.json"), json.writerWithDefaultPrettyPrinter()
                .writeValueAsString(report), StandardCharsets.UTF_8);
        Files.writeString(directory.resolve("report.html"), render(report), StandardCharsets.UTF_8);
    }

    public String render(EvaluationReportDTO report) {
        StringBuilder html = new StringBuilder("<!doctype html><html lang=zh-CN><meta charset=utf-8>"
                + "<meta name=viewport content=width=device-width,initial-scale=1><title>检索回归评测</title><style>"
                + "body{font:16px/1.8 system-ui;color:#26354c;background:#f4f7fc;margin:0}main{max-width:1100px;"
                + "margin:auto;padding:28px 18px}h1,h2,h3{color:#245bd3}section{background:white;padding:20px;"
                + "border-radius:12px;margin:20px 0}table{border-collapse:collapse;width:100%;font-size:14px}"
                + "th,td{border:1px solid #d8e2f0;padding:10px;text-align:left}th{background:#edf3fc}"
                + ".scroll{overflow:auto}.note{background:#fff6df;padding:16px}.bad{color:#b42828}pre{white-space:pre-wrap;"
                + "overflow-wrap:anywhere;font-size:13px}.badge{font-size:13px;color:#64748b}</style><main>");
        html.append("<p class=badge>JAVA × AI · 实战 09</p><h1>检索回归评测报告</h1><p>")
                .append(escape(report.getRunLabel())).append(" · ").append(escape(report.getCreatedAt()))
                .append("</p><p>样本集 ").append(escape(report.getDatasetVersion())).append(" · 划分 ")
                .append(escape(report.getSplit())).append(" · ").append(report.getRows().size()).append(" 条</p>");
        html.append("<p class=note>教学集，非独立生产验收。质量指标只在该方法正常完成的题目上统计，必须同时看完成率和分母。"
                + "Precision 按非空输出题目宏平均，Recall 按有期望案例且完成的题目宏平均；二者分母不同。"
                + "耗时包含失败题目，RERANK 使用检索加重排总耗时，首次向量建库计入。Token 仅为有效聊天响应报告的用量，"
                + "不含 Embedding 和失败调用；不能据此计算完整账单。</p><section><h2>方法概览</h2><div class=scroll><table>"
                + "<tr><th>方法</th><th>正常完成/全部</th><th>精确集合/完成</th><th>首位命中/有答案</th>"
                + "<th>误拒绝/有答案</th><th>正确空选/无答案</th><th>宏P / 宏R</th><th>均耗时ms</th><th>已报告Token</th></tr>");
        for (EvaluationMetricDTO metric : report.getMetrics()) {
            html.append("<tr>");
            cells(html, metric.getMethod(), metric.getCompleted() + "/" + metric.getTotal(),
                    metric.getExact() + "/" + metric.getCompleted(),
                    metric.getTop1Hits() + "/" + metric.getPositiveCompleted(),
                    metric.getFalseRejections() + "/" + metric.getPositiveCompleted(),
                    metric.getCorrectAbstentions() + "/" + metric.getNegativeCompleted(),
                    number(metric.getMacroPrecision()) + " / " + number(metric.getMacroRecall()),
                    number(metric.getMeanElapsedMillis()), metric.getReportedTotalTokens() + " ("
                            + metric.getTokenObservations() + " 次有效响应)");
            html.append("</tr>");
        }
        html.append("</table></div></section><section><h2>逐题结果与失败定位</h2>");
        for (EvaluationRowDTO row : report.getRows()) {
            html.append("<h3>").append(escape(row.getSample().getId())).append("</h3><p>")
                    .append(escape(row.getSample().getDescription())).append("</p><p>人工期望：")
                    .append(escape(row.getSample().getExpectedIds().toString())).append("</p><p>标注理由：")
                    .append(escape(row.getSample().getRationale())).append("</p><div class=scroll><table>"
                            + "<tr><th>方法</th><th>状态</th><th>实际候选</th><th>漏选</th><th>多选</th></tr>");
            for (EvaluationObservationDTO observation : row.getObservations()) {
                boolean complete = Boolean.TRUE.equals(observation.getCompleted());
                html.append("<tr>");
                cells(html, observation.getMethod(), observation.getStatus(), observation.getActualIds().toString(),
                        complete ? row.getSample().getExpectedIds().stream()
                                .filter(id -> !observation.getActualIds().contains(id)).toList().toString() : "未完成，不评分",
                        complete ? observation.getActualIds().stream()
                                .filter(id -> !row.getSample().getExpectedIds().contains(id)).toList().toString()
                                : "未完成，不评分");
                html.append("</tr>");
            }
            html.append("</table></div>");
        }
        html.append("</section><section><h2>与基线比较</h2><p>")
                .append(escape(report.getComparisonNote())).append("</p>");
        for (EvaluationChangeDTO change : report.getChanges()) {
            if (!"UNCHANGED".equals(change.getKind())) {
                html.append("<p class=bad>").append(escape(change.getSampleId())).append(" · ")
                        .append(escape(change.getMethod())).append(" · ").append(escape(change.getKind()))
                        .append(" · ").append(escape(change.getBefore().toString())).append(" → ")
                        .append(escape(change.getAfter().toString())).append("</p>");
            }
        }
        html.append("</section><section><h2>复现信息（无密钥）</h2><pre>")
                .append(escape(json.writerWithDefaultPrettyPrinter().writeValueAsString(report.getConfiguration())))
                .append("</pre><p>数据 SHA256：").append(escape(report.getDatasetSha256()))
                .append("</p><p>语料 SHA256：").append(escape(report.getCorpusSha256()))
                .append("</p></section></main></html>");
        return html.toString();
    }

    private void cells(StringBuilder html, String... values) {
        for (String value : values) {
            html.append("<td>").append(escape(value)).append("</td>");
        }
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value, StandardCharsets.UTF_8.name());
    }

    private String number(Double value) {
        return value == null ? "未定义" : String.format(Locale.ROOT, "%.3f", value);
    }
}
