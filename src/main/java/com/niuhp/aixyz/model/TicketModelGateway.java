package com.niuhp.aixyz.model;

/**
 * 工单提取的外部模型边界。
 *
 * @author niuhaipeng
 */
public interface TicketModelGateway {

    /**
     * 将用户描述发送给模型，返回解析后的候选字段和用量。
     *
     * @param description 用户原始描述
     * @return 候选提取结果
     */
    ExtractionResult extract(String description);
}
