package com.project.lexiquery.service;

import com.project.lexiquery.config.LexiQueryProperties;
import com.project.lexiquery.model.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Client for the Python ML microservice (embeddings + scikit-learn classifiers).
 * Every call degrades gracefully, so the Java app still works if the ML service is down.
 */
@Service
public class MlClient {
    private static final Logger log = LoggerFactory.getLogger(MlClient.class);
    private final boolean enabled;
    private final RestClient http;

    public MlClient(LexiQueryProperties props) {
        this.enabled = props.ml().enabled();
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(2000);
        f.setReadTimeout(8000);
        this.http = RestClient.builder().baseUrl(props.ml().baseUrl()).requestFactory(f).build();
    }

    /** Semantic schema linking: returns the top-k most relevant table names for the question. */
    @SuppressWarnings("unchecked")
    public List<String> linkTables(String question, Schema schema, int topK) {
        if (!enabled) return List.of();
        try {
            List<Map<String, Object>> tables = new ArrayList<>();
            for (Schema.Table t : schema.tables())
                tables.add(Map.of("name", t.name(), "columns", t.columns().stream().map(Schema.Column::name).toList()));
            Map<String, Object> resp = post("/link", Map.of("question", question, "tables", tables, "top_k", topK));
            List<Map<String, Object>> ranked = (List<Map<String, Object>>) resp.get("tables");
            List<String> out = new ArrayList<>();
            for (Map<String, Object> m : ranked) out.add(String.valueOf(m.get("name")));
            return out;
        } catch (Exception e) {
            log.warn("ML /link unavailable: {}", e.getMessage());
            return List.of();
        }
    }

    /** SIMPLE / MEDIUM / COMPLEX; defaults to COMPLEX (routes to the LLM) when the service is unavailable. */
    public String classify(String question) {
        if (!enabled) return "COMPLEX";
        try {
            return String.valueOf(post("/classify", Map.of("text", question)).get("label"));
        } catch (Exception e) {
            log.warn("ML /classify unavailable: {}", e.getMessage());
            return "COMPLEX";
        }
    }

    /** Probability (0..1) that the text is an injection / jailbreak attempt. 0 when unavailable. */
    public double injectionScore(String text) {
        if (!enabled) return 0.0;
        try {
            return ((Number) post("/risk", Map.of("text", text)).get("score")).doubleValue();
        } catch (Exception e) {
            log.warn("ML /risk unavailable: {}", e.getMessage());
            return 0.0;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String path, Object body) {
        return http.post().uri(path).contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(Map.class);
    }
}
