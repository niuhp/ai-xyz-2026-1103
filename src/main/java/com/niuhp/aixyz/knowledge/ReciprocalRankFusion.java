package com.niuhp.aixyz.knowledge;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 等权倒数排名融合；只用名次，保留各路原始分数用于解释，不代表概率。
 *
 * @author niuhaipeng
 */
@Component
public class ReciprocalRankFusion {

    public static final String METHOD = "HYBRID_RRF";
    public static final int RRF_K = 60;
    private static final int MAX_RESULTS = 3;

    public List<CaseMatchDTO> fuse(List<CaseMatchDTO> lexical, List<CaseMatchDTO> semantic) {
        Map<String, CaseMatchDTO> candidates = new HashMap<>(8);
        add(candidates, lexical, true);
        add(candidates, semantic, false);
        return candidates.values().stream().sorted(Comparator.comparingDouble(CaseMatchDTO::getScore).reversed()
                .thenComparing(match -> match.getDocument().getId())).limit(MAX_RESULTS).toList();
    }

    private void add(Map<String, CaseMatchDTO> candidates, List<CaseMatchDTO> matches, boolean lexical) {
        Set<String> seen = new HashSet<>(4);
        int rank = 0;
        for (CaseMatchDTO match : matches) {
            String id = match.getDocument().getId();
            if (!seen.add(id)) {
                continue;
            }
            rank++;
            CaseMatchDTO candidate = candidates.computeIfAbsent(id, key -> {
                CaseMatchDTO created = new CaseMatchDTO();
                created.setDocument(match.getDocument());
                created.setScore(0.0);
                return created;
            });
            candidate.setScore(candidate.getScore() + 1.0 / (RRF_K + rank));
            if (lexical) {
                candidate.setLexicalRank(rank);
                candidate.setLexicalScore(match.getScore());
            } else {
                candidate.setSemanticRank(rank);
                candidate.setSemanticScore(match.getScore());
            }
        }
    }
}
