package com.niuhp.aixyz.dto.request;

import com.niuhp.aixyz.knowledge.AdviceMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 第五篇知识库接口数据，只保存显式字段。
 *
 * @author niuhaipeng
 */
public class KnowledgeAdviceRequestDTO {

    @NotBlank
    @Size(max = 4000)
    private String description;

    @NotNull
    private AdviceMode mode;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AdviceMode getMode() {
        return mode;
    }

    public void setMode(AdviceMode mode) {
        this.mode = mode;
    }

    @Override
    public String toString() {
        return "KnowledgeAdviceRequestDTO{content=[REDACTED]}";
    }
}
