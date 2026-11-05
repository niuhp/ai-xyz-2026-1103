package com.niuhp.aixyz.service;

import org.springframework.http.HttpStatus;

/**
 * 对外稳定的处理错误，避免泄露模型供应商的原始响应。
 *
 * @author niuhaipeng
 */
public enum ProcessingError {

    /** 输入不符合约束。 */
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "请填写 1 至 4000 字的问题描述，检查 JSON 格式和字段类型"),
    /** 请求资源不存在。 */
    NOT_FOUND(HttpStatus.NOT_FOUND, "请求的资源不存在"),
    /** 请求方法不支持。 */
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "该接口不支持此请求方法"),
    /** 请求内容类型不支持。 */
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "请使用 application/json 提交请求"),
    /** 并发已达本地限制。 */
    BUSY(HttpStatus.SERVICE_UNAVAILABLE, "当前请求较多，请稍后重试"),
    /** 上游模型调用失败。 */
    MODEL_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "模型服务暂不可用，请稍后重试或检查服务配置"),
    /** 上游超时。 */
    MODEL_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "模型响应超时，请稍后重试"),
    /** 上游返回不完整或无法通过校验的内容。 */
    INVALID_MODEL_OUTPUT(HttpStatus.BAD_GATEWAY, "模型返回内容不符合工单结构，请调整描述后重试"),
    /** 未预期的服务错误。 */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "处理失败，请使用请求编号联系维护者");

    /** HTTP 状态。 */
    private final HttpStatus httpStatus;

    /** 安全的对外说明。 */
    private final String message;

    ProcessingError(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
