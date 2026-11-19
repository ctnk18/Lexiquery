package com.project.lexiquery.service;

import com.project.lexiquery.entity.Role;
import com.project.lexiquery.model.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Research harness: runs a benchmark (question @@ gold SQL) through every strategy and reports
 * EXECUTION ACCURACY (result set equals the gold query's result set), failure count and latency.
 */
@Service
public class EvaluationService {
    private final LexiQueryService nl;
    private final SqlValidatorService validator;
    private final QueryExecutorService executor;
    private final SchemaService schemaService;

    public EvaluationService(LexiQueryService nl, SqlValidatorService validator, QueryExecutorService executor,
                             SchemaService schemaService) {
        this.nl = nl; this.validator = validator; this.executor = executor; this.schemaService = schemaService;
    }

    public List<EvalRow> run(String adminUser) {
        List<String[]> items = loadBenchmark();
        Set<String> all = schemaService.getSchema().tableNames();
        List<EvalRow> out = new ArrayList<>();

        for (Strategy s : Strategy.values()) {
            int correct = 0, failed = 0;
            long totalLatency = 0;
            for (String[] item : items) {
                List<String> gold = normalize(executor.execute(validator.validate(item[1], all)));
                QueryResult r = nl.process(item[0], adminUser, Role.ADMIN, s, false);
                totalLatency += r.getLatencyMs();
                if (!r.isSuccess()) { failed++; continue; }
                if (gold.equals(normalize(new QueryData(r.getColumns(), r.getRows())))) correct++;
            }
            out.add(new EvalRow(s.name(), items.size(), correct, failed, items.isEmpty() ? 0 : totalLatency / items.size()));
        }
        return out;
    }

    private List<String[]> loadBenchmark() {
        List<String[]> items = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new ClassPathResource("benchmark/sample.csv").getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] p = line.split("@@", 2);
                if (p.length == 2) items.add(new String[]{p[0].trim(), p[1].trim()});
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read benchmark file", e);
        }
        return items;
    }

    /** Order-insensitive, column-name-insensitive comparison of result values. */
    private static List<String> normalize(QueryData d) {
        List<String> rows = new ArrayList<>();
        for (List<Object> row : d.rows()) {
            StringBuilder sb = new StringBuilder();
            for (Object c : row) sb.append(cell(c)).append('|');
            rows.add(sb.toString());
        }
        Collections.sort(rows);
        return rows;
    }

    private static String cell(Object o) {
        if (o == null) return "null";
        if (o instanceof Number) {
            try { return new BigDecimal(o.toString()).stripTrailingZeros().toPlainString(); }
            catch (NumberFormatException ignored) { }
        }
        return o.toString();
    }
}
