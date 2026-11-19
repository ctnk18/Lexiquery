package com.project.lexiquery.service;

import com.project.lexiquery.config.LexiQueryProperties;
import com.project.lexiquery.entity.QueryHistory;
import com.project.lexiquery.entity.Role;
import com.project.lexiquery.exception.LexiQueryException;
import com.project.lexiquery.model.*;
import com.project.lexiquery.repository.QueryHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Orchestrates the whole AI pipeline:
 *  1. ML injection screening  2. role-scoped schema  3. ML schema linking (RAG over schema)
 *  4. ML complexity routing   5. RULE or LLM generation with retrieved few-shot examples
 *  6. SQL validation + read-only execution, with LLM self-correction on failure
 *  7. audit logging
 */
@Service
public class LexiQueryService {
    private static final Logger log = LoggerFactory.getLogger(LexiQueryService.class);

    private final LexiQueryProperties props;
    private final SchemaService schemaService;
    private final MlClient ml;
    private final RuleBasedGenerator rule;
    private final LlmSqlGenerator llm;
    private final SqlValidatorService validator;
    private final QueryExecutorService executor;
    private final ExampleRetriever examples;
    private final AccessPolicy policy;
    private final RateLimiter rateLimiter;
    private final QueryHistoryRepository history;

    public LexiQueryService(LexiQueryProperties props, SchemaService schemaService, MlClient ml, RuleBasedGenerator rule,
                          LlmSqlGenerator llm, SqlValidatorService validator, QueryExecutorService executor,
                          ExampleRetriever examples, AccessPolicy policy, RateLimiter rateLimiter,
                          QueryHistoryRepository history) {
        this.props = props; this.schemaService = schemaService; this.ml = ml; this.rule = rule; this.llm = llm;
        this.validator = validator; this.executor = executor; this.examples = examples; this.policy = policy;
        this.rateLimiter = rateLimiter; this.history = history;
    }

    public QueryResult process(String question, String username, Role role, Strategy strategy, boolean persist) {
        long start = System.nanoTime();
        String sql = null, used = null, complexity = null, status = "SUCCESS", message = null;
        int attempts = 0;
        QueryData data = null;

        try {
            if (persist && !rateLimiter.tryAcquire(username))
                throw new LexiQueryException("BLOCKED", "Rate limit exceeded. Please wait a minute and try again.");

            // 1. ML security screening of the user's input
            if (ml.injectionScore(question) >= props.ml().injectionThreshold())
                throw new LexiQueryException("BLOCKED", "Request blocked: it looks like a prompt/SQL injection attempt.");

            // 2. role-scoped schema
            Schema full = schemaService.getSchema();
            Set<String> allowed = policy.allowedTables(role, full);
            if (allowed.isEmpty()) throw new LexiQueryException("BLOCKED", "No tables are available for your role.");
            Schema scoped = full.restrictTo(allowed);

            // 3. ML schema linking: shrink the prompt to the relevant tables (+ FK neighbours)
            Schema ctx = narrow(question, scoped);
            List<Example> shots = examples.topK(question, 3, allowed);

            // 4. ML routing
            if (strategy == Strategy.HYBRID) complexity = ml.classify(question);

            // 5. generation
            if (strategy == Strategy.RULE || (strategy == Strategy.HYBRID && "SIMPLE".equals(complexity))) {
                Optional<String> r = rule.generate(question, scoped);
                if (r.isPresent()) { sql = r.get(); used = "RULE"; }
                else if (strategy == Strategy.RULE)
                    throw new LexiQueryException("FAILED", "The rule-based engine could not interpret this question.");
            }
            if (sql == null) { used = "LLM"; sql = llm.generate(question, ctx, shots, null, null); }

            // 6. validate -> execute -> self-correct
            int maxRetries = props.limits().maxRetries();
            while (true) {
                attempts++;
                try {
                    String safe = validator.validate(sql, allowed);
                    data = executor.execute(safe);
                    sql = safe;
                    break;
                } catch (LexiQueryException | DataAccessException ex) {
                    String err = ex instanceof LexiQueryException ? ex.getMessage() : "Database error: " + rootMessage(ex);
                    boolean canRetry = strategy != Strategy.RULE && attempts <= maxRetries;
                    if (!canRetry) {
                        if (ex instanceof LexiQueryException n) throw n;
                        throw new LexiQueryException("FAILED", err);
                    }
                    log.info("Self-correction attempt {} after: {}", attempts, err);
                    used = "LLM";
                    sql = llm.generate(question, ctx, shots, sql, err);
                }
            }
        } catch (LexiQueryException e) {
            status = e.getStatus();
            message = e.getMessage();
        } catch (RuntimeException e) {
            log.error("Unexpected error", e);
            status = "FAILED";
            message = "Unexpected error while processing the question.";
        }

        long latency = (System.nanoTime() - start) / 1_000_000;
        Long id = null;
        if (persist) {
            QueryHistory h = new QueryHistory();
            h.setUsername(username);
            h.setQuestion(truncate(question, 600));
            h.setGeneratedSql(sql);
            h.setStrategy(used);
            h.setStatus(status);
            h.setMessage(truncate(message, 400));
            h.setRowCount(data == null ? null : data.rows().size());
            h.setLatencyMs(latency);
            h.setAttempts(attempts);
            id = history.save(h).getId();
        }
        return QueryResult.builder()
                .historyId(id).question(question).sql(sql).strategyUsed(used).complexity(complexity)
                .status(status).message(message)
                .columns(data == null ? List.of() : data.columns())
                .rows(data == null ? List.of() : data.rows())
                .latencyMs(latency).attempts(attempts).build();
    }

    private Schema narrow(String question, Schema scoped) {
        if (scoped.tables().size() <= 4) return scoped;
        List<String> linked = ml.linkTables(question, scoped, 3);
        if (linked.isEmpty()) return scoped;
        Set<String> keep = new HashSet<>();
        linked.forEach(n -> keep.add(n.toLowerCase()));
        keep.addAll(scoped.neighbours(keep));
        return scoped.restrictTo(keep);
    }

    private static String rootMessage(Throwable t) {
        while (t.getCause() != null) t = t.getCause();
        return truncate(t.getMessage(), 250);
    }

    private static String truncate(String s, int n) { return s == null || s.length() <= n ? s : s.substring(0, n); }
}
