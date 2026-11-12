package com.niuhp.aixyz.knowledge;

/**
 * 两种独立的检索方式，保留词项检索作为对照。
 *
 * @author niuhaipeng
 */
public enum RetrievalMethod {
    /** 本地词项检索。 */
    TFIDF,
    /** 通过独立向量模型进行语义检索。 */
    SEMANTIC
}
