package com.niuhp.aixyz.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 检索或重排对照输入；是否调用聊天模型由所用接口决定。
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
