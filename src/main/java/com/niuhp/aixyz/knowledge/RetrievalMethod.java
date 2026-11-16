package com.niuhp.aixyz.knowledge;

/**
 * 独立检索与排名融合方式，保留词项检索作为对照。
 *
 * @author niuhaipeng
 */
public enum RetrievalMethod {
    /** 本地词项检索。 */
    TFIDF,
    /** 通过独立向量模型进行语义检索。 */
    SEMANTIC,
    /** 将两条已过滤的候选列表按倒数排名融合。 */
    HYBRID
}
