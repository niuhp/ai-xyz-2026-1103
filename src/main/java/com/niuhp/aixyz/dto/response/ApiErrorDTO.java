package com.niuhp.aixyz.dto.response;


/**
 * 对外错误响应，不暴露上游原文、密钥或用户输入。
 *
 * @author niuhaipeng
 */
public class ApiErrorDTO {

    /** 本次请求编号。 */
    private String requestId;

    /** 稳定错误码。 */
    private String code;

    /** 可公开的错误说明。 */
    private String message;

    /** 可执行的处理建议。 */
    private String action;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    @Override
    public String toString() {
        return "ApiErrorDTO{requestId='" + requestId + "', code='" + code + "'}";
    }
}
