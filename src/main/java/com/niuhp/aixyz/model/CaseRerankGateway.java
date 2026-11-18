package com.niuhp.aixyz.model;

import com.niuhp.aixyz.knowledge.CaseMatchDTO;
import java.util.List;

/**
 * 对已召回的候选进行相关性判断，不扩大候选集合。
 *
 * @author niuhaipeng
 */
public interface CaseRerankGateway {

    /**
     * 根据用户描述判断给定候选，结果只允许引用这些编号。
     *
     * @param description 问题描述
     * @param candidates 本次混合检索候选
     * @return 未经业务校验的模型判断及用量
     */
    RerankModelResultDTO assess(String description, List<CaseMatchDTO> candidates);
}
