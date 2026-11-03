package com.niuhaipeng.aixyz.domain;

/**
 * 草稿状态，由 Java 根据有效字段计算。
 *
 * @author niuhaipeng
 */
public enum DraftStatus {
    /** 必要信息齐全，仍需用户复核，不表示已创建工单。 */
    READY,

    /** 需要补充必要信息。 */
    NEEDS_INFO,

    /** 不是本示例支持的软件问题反馈。 */
    UNSUPPORTED;
}
