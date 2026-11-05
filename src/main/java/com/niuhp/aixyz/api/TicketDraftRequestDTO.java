package com.niuhp.aixyz.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 生成草稿的请求，只接受用户问题描述。
 *
 * @author niuhaipeng
 */
public class TicketDraftRequestDTO {

    /** 公开或已脱敏的问题描述。 */
    @NotBlank(message = "问题描述不能为空")
    @Size(max = 4000, message = "问题描述不能超过 4000 个字符")
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    @Override
    public String toString() {
        return "TicketDraftRequestDTO{description=[REDACTED]}";
    }
}
