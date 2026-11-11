package com.niuhp.aixyz.knowledge;

/**
 * 同一问题的有资料与无资料对照。
 *
 * @author niuhaipeng
 */
public enum AdviceMode {
    /** 先检索案例，未命中就不调用模型。 */
    RETRIEVAL,
    /** 不检索、不提供引用，只返回通用建议。 */
    BASELINE
}
