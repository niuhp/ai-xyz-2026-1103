package com.niuhp.aixyz.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 从描述中提取的工单字段；空字符串表示没有提供。
 *
 * @author niuhaipeng
 */
public class TicketDraftDTO {

    /** 系统名称，不根据演示项目名称猜测。 */
    @NotNull
    @Size(max = 100)
    private String system;

    /** 涉及的功能模块。 */
    @NotNull
    @Size(max = 100)
    private String module;

    /** 示例分类。 */
    @NotNull
    private IssueCategory category;

    /** 问题摘要。 */
    @NotBlank
    @Size(max = 200)
    private String summary;

    /** 用户实际观察到的现象。 */
    @NotNull
    @Size(max = 1000)
    private String actualResult;

    /** 用户明确表达的预期。 */
    @NotNull
    @Size(max = 1000)
    private String expectedResult;

    /** 发生时间原文，不把相对日期猜成具体日期。 */
    @NotNull
    @Size(max = 100)
    private String occurredAtText;

    /** 用户提供的系统版本。 */
    @NotNull
    @Size(max = 100)
    private String version;

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public IssueCategory getCategory() {
        return category;
    }

    public void setCategory(IssueCategory category) {
        this.category = category;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getActualResult() {
        return actualResult;
    }

    public void setActualResult(String actualResult) {
        this.actualResult = actualResult;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }

    public String getOccurredAtText() {
        return occurredAtText;
    }

    public void setOccurredAtText(String occurredAtText) {
        this.occurredAtText = occurredAtText;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
    @Override
    public String toString() {
        return "TicketDraftDTO{category=" + category + ", content=[REDACTED]}";
    }
}
