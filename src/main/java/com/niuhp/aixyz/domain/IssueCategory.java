package com.niuhp.aixyz.domain;

/**
 * 面向工单分派的示例分类，不表示已诊断根因。
 *
 * @author niuhaipeng
 */
public enum IssueCategory {
    /** 重复数据或重复选项。 */
    DATA_DUPLICATION,

    /** 查询、筛选或结果展示异常。 */
    QUERY,

    /** 导入导出异常。 */
    EXPORT,

    /** 登录或访问问题。 */
    ACCESS,

    /** 其他软件问题。 */
    OTHER;
}
