package com.niuhp.aixyz.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 同题检索对照输入，不调用生成模型。
 *
 * @author niuhaipeng
 */
public class RetrievalCompareRequestDTO {

    @NotBlank
    @Size(max = 4000)
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "RetrievalCompareRequestDTO{content=[REDACTED]}";
    }
}
