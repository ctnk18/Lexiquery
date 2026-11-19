package com.project.lexiquery.service;

import com.project.lexiquery.entity.QueryHistory;
import com.project.lexiquery.exception.LexiQueryException;
import com.project.lexiquery.model.Example;
import com.project.lexiquery.repository.QueryHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Retrieval-augmented few-shot learning: past questions that users rated positively (and succeeded) are retrieved
 * by token-overlap similarity and injected into the prompt. The system improves as feedback accumulates.
 */
@Service
public class ExampleRetriever {
    private final QueryHistoryRepository history;
    private final SqlValidatorService validator;

    public ExampleRetriever(QueryHistoryRepository history, SqlValidatorService validator) {
        this.history = history; this.validator = validator;
    }

    public List<Example> topK(String question, int k, Set<String> allowedTables) {
        Set<String> q = tokens(question);
        record Scored(double score, Example ex) {}
        List<Scored> scored = new ArrayList<>();
        for (QueryHistory h : history.findTop200ByFeedbackAndStatusOrderByCreatedAtDesc(1, "SUCCESS")) {
            double sim = jaccard(q, tokens(h.getQuestion()));
            if (sim < 0.3) continue;
            try { validator.validate(h.getGeneratedSql(), allowedTables); }   // never leak examples the user may not see
            catch (LexiQueryException e) { continue; }
            scored.add(new Scored(sim, new Example(h.getQuestion(), h.getGeneratedSql())));
        }
        scored.sort(Comparator.comparingDouble(Scored::score).reversed());
        return scored.stream().limit(k).map(Scored::ex).toList();
    }

    private static Set<String> tokens(String s) {
        Set<String> out = new HashSet<>();
        for (String t : s.toLowerCase().split("[^a-z0-9]+")) if (t.length() > 2) out.add(t);
        return out;
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        Set<String> inter = new HashSet<>(a); inter.retainAll(b);
        Set<String> union = new HashSet<>(a); union.addAll(b);
        return (double) inter.size() / union.size();
    }
}
