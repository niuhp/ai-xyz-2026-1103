package com.niuhp.aixyz.knowledge;

import com.niuhp.aixyz.exception.ProcessingError;
import com.niuhp.aixyz.exception.TicketProcessingException;
import com.niuhp.aixyz.service.RetrievalCompareService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * 验证融合的跨路去重、来源名次、确定排序与降级边界，不模拟质量提升。
 *
 * @author niuhaipeng
 */
class ReciprocalRankFusionTest {

    private static final String FIRST = "KB-001";
    private static final String SECOND = "KB-002";
    private static final String THIRD = "KB-003";
    private static final String QUESTION = "问题";
    private final ReciprocalRankFusion fusion = new ReciprocalRankFusion();

    @Test
    void shouldUseRanksRatherThanRawScoresAndKeepInputsUnchanged() {
        CaseMatchDTO first = match(FIRST, 0.9);
        var result = fusion.fuse(List.of(first, match(SECOND, 0.8)),
                List.of(match(SECOND, 0.99), match(THIRD, 0.98)));
        assertThat(result).extracting(item -> item.getDocument().getId()).containsExactly(SECOND, FIRST, THIRD);
        CaseMatchDTO winner = result.getFirst();
        assertThat(winner.getScore()).isCloseTo(1.0 / 62 + 1.0 / 61, offset(0.0000001));
        assertThat(winner.getLexicalRank()).isEqualTo(2);
        assertThat(winner.getSemanticRank()).isEqualTo(1);
        assertThat(winner.getLexicalScore()).isEqualTo(0.8);
        assertThat(winner.getSemanticScore()).isEqualTo(0.99);
        assertThat(first.getScore()).isEqualTo(0.9);
        assertThat(first.getLexicalRank()).isNull();
        var changed = fusion.fuse(List.of(match(FIRST, 0.2), match(SECOND, 0.1)),
                List.of(match(SECOND, 0.6), match(THIRD, 0.5)));
        assertThat(changed).extracting(CaseMatchDTO::getScore).containsExactlyElementsOf(
                result.stream().map(CaseMatchDTO::getScore).toList());
    }

    @Test
    void shouldDeduplicateWithinRouteBreakTiesByIdAndCapResults() {
        var result = fusion.fuse(List.of(match(SECOND, 0.8), match(SECOND, 0.7), match(THIRD, 0.6)),
                List.of(match(FIRST, 0.9), match("KB-004", 0.8)));
        assertThat(result).hasSize(3).extracting(item -> item.getDocument().getId())
                .containsExactly(FIRST, SECOND, THIRD);
        assertThat(result.get(1).getScore()).isCloseTo(1.0 / 61, offset(0.0000001));
        assertThat(fusion.fuse(List.of(), List.of())).isEmpty();
    }

    @Test
    void shouldReuseBothPathsOnceAndExposeFailureInsteadOfCompleteNoMatch() {
        CaseKnowledgeBase lexical = mock(CaseKnowledgeBase.class);
        SemanticCaseIndex semantic = mock(SemanticCaseIndex.class);
        when(lexical.search(QUESTION)).thenReturn(List.of(match(FIRST, 0.6)));
        when(semantic.search(QUESTION)).thenThrow(new TicketProcessingException(ProcessingError.EMBEDDING_UNAVAILABLE));
        when(semantic.information()).thenReturn(Map.of());
        RetrievalCompareService service = new RetrievalCompareService(lexical, semantic, fusion);
        var result = service.compare(QUESTION, "test");
        assertThat(result.getHybrid().getStatus()).isEqualTo("DEGRADED");
        assertThat(result.getHybrid().getThreshold()).isNull();
        assertThat(result.getHybrid().getSemanticStatus()).isEqualTo("EMBEDDING_UNAVAILABLE");
        assertThat(result.getHybrid().getMatches()).extracting(item -> item.getDocument().getId()).containsExactly(FIRST);
        verify(lexical, times(1)).search(QUESTION);
        verify(semantic, times(1)).search(QUESTION);
        when(lexical.search(QUESTION)).thenReturn(List.of());
        assertThat(service.compare(QUESTION, "test").getHybrid().getStatus()).isEqualTo("DEGRADED");
        doReturn(List.of()).when(semantic).search(QUESTION);
        assertThat(service.compare(QUESTION, "test").getHybrid().getStatus()).isEqualTo("NO_MATCH");
    }

    private CaseMatchDTO match(String id, double score) {
        KnowledgeCaseDTO document = new KnowledgeCaseDTO();
        document.setId(id);
        CaseMatchDTO match = new CaseMatchDTO();
        match.setDocument(document);
        match.setScore(score);
        return match;
    }
}
