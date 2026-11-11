package com.niuhp.aixyz.knowledge;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 词项索引的可复现召回样本，不把教学样本上的结果当成通用检索准确率。
 *
 * @author niuhaipeng
 */
class CaseKnowledgeBaseTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();

    @AfterAll
    static void close() {
        FACTORY.close();
    }

    @Test
    void shouldRankSymptomsAndKeepBoundedDeterministicResults() throws Exception {
        CaseKnowledgeBase index = new CaseKnowledgeBase(JsonMapper.builder().build(), FACTORY.getValidator(), 0.16);
        assertThat(index.size()).isEqualTo(16);
        var export = index.search("导出 Excel 选择全年数据等待很久提示超时，缩小日期范围后正常");
        assertThat(export).isNotEmpty().hasSizeLessThanOrEqualTo(3);
        assertThat(export.get(0).getDocument().getId()).isEqualTo("KB-001");
        assertThat(export).allSatisfy(match -> assertThat(match.getScore()).isBetween(0.16, 1.0));
        assertThat(index.search("同一个字典类型中相同字典值重复添加，下拉框出现多个选项").get(0)
                .getDocument().getId()).isEqualTo("KB-004");
        assertThat(index.search("相邻两页出现相同记录，分页列表数据重复").get(0)
                .getDocument().getId()).isEqualTo("KB-009");
        assertThat(index.search("时间相差八小时，页面时间与数据库时间不一致").get(0)
                .getDocument().getId()).isEqualTo("KB-014");
        assertThat(index.search("导出 Excel 选择全年数据等待很久提示超时，缩小日期范围后正常").stream()
                .map(match -> match.getDocument().getId()).toList()).isEqualTo(export.stream()
                        .map(match -> match.getDocument().getId()).toList());
    }

    @Test
    void shouldAbstainForUnknownAndGenericQueries() throws Exception {
        CaseKnowledgeBase index = new CaseKnowledgeBase(JsonMapper.builder().build(), FACTORY.getValidator(), 0.16);
        for (String query : new String[]{"", "系统问题", "量子纠缠实验的光子偏振发生变化，如何调整光学仪器？",
            "帮我写一首秋天的诗", "🙂🙂🙂", "xkcd_foobar"}) {
            assertThat(index.search(query)).as(query).isEmpty();
        }
    }
}
