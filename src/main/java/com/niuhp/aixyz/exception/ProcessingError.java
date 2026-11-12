package com.niuhp.aixyz.exception;

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
    /** 无法找到待确认的草稿。 */
    DRAFT_NOT_FOUND(HttpStatus.NOT_FOUND, "草稿不存在或已过期，请重新生成"),
    /** 确认内容缺少必要字段。 */
    DRAFT_INCOMPLETE(HttpStatus.BAD_REQUEST, "请补全系统、模块、分类、摘要、实际现象、预期结果和版本信息"),
    /** 请求方法不支持。 */
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "该接口不支持此请求方法"),
    /** 请求内容类型不支持。 */
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "请使用 application/json 提交请求"),
    /** 并发已达本地限制。 */
    BUSY(HttpStatus.SERVICE_UNAVAILABLE, "当前请求较多，请稍后重试"),
    /** 上游模型调用失败。 */
    MODEL_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "模型服务暂不可用，本次处理未完成"),
    /** 鉴权或模型访问权限错误，不自动重试。 */
    MODEL_AUTH_ERROR(HttpStatus.BAD_GATEWAY, "模型服务鉴权失败，请联系维护者检查密钥与权限"),
    /** 明确的余额或配额不足。 */
    MODEL_QUOTA_EXHAUSTED(HttpStatus.BAD_GATEWAY, "模型服务额度不足，请联系维护者检查账户额度"),
    /** 429 无法一概推断为余额不足。 */
    MODEL_RATE_LIMITED(HttpStatus.SERVICE_UNAVAILABLE, "模型服务请求受限，请稍后再试"),
    /** 参数、模型名称或路由不受支持。 */
    MODEL_REQUEST_REJECTED(HttpStatus.BAD_GATEWAY, "模型请求被拒绝，请联系维护者检查模型与接口配置"),
    /** 网络结果不确定，避免自动重复生成。 */
    MODEL_NETWORK_ERROR(HttpStatus.BAD_GATEWAY, "连接模型服务失败，本次未取得有效结果"),
    /** 上游超时。 */
    MODEL_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "等待模型响应超时，本次未取得有效结果"),
    /** 上游返回不完整或无法通过校验的内容。 */
    INVALID_MODEL_OUTPUT(HttpStatus.BAD_GATEWAY, "模型返回内容不符合工单结构，请调整描述后重试"),
    /** 建议格式或引用不符合本次检索结果。 */
    INVALID_ADVICE_OUTPUT(HttpStatus.BAD_GATEWAY, "模型建议或引用未通过校验，本次结果已丢弃，请勿据此操作"),
    /** 尚未配置独立向量模型。 */
    EMBEDDING_DISABLED(HttpStatus.SERVICE_UNAVAILABLE, "语义检索尚未启用，请配置独立的 Embedding 服务后重启"),
    /** 向量模型调用失败，不回退为其他检索方式。 */
    EMBEDDING_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "向量服务调用失败，请检查独立服务配置、权限、额度或网络"),
    /** 向量缺失、顺序错误、维度变化或包含无效值。 */
    INVALID_EMBEDDING_OUTPUT(HttpStatus.BAD_GATEWAY, "向量结果校验失败，本次结果已丢弃"),
    /** 流结束但缺少模型正常完成标记。 */
    STREAM_INCOMPLETE(HttpStatus.BAD_GATEWAY, "模型响应中断，预览内容未形成有效草稿"),
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

    public String getAction() {
        switch (this) {
            case MODEL_AUTH_ERROR:
            case MODEL_QUOTA_EXHAUSTED:
            case MODEL_REQUEST_REJECTED:
                return "请联系维护者处理配置或额度问题，反复点击不会解决。";
            case MODEL_TIMEOUT:
            case MODEL_NETWORK_ERROR:
            case STREAM_INCOMPLETE:
                return "原始描述已保留。稍后可手动重试；上游可能已处理请求，费用以平台账单为准。";
            case MODEL_RATE_LIMITED:
            case MODEL_UNAVAILABLE:
            case BUSY:
                return "请稍后再试，避免连续点击；重复调用可能产生额外费用。";
            default:
                return "请检查输入；仍有问题时提供请求编号给维护者。";
        }
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
