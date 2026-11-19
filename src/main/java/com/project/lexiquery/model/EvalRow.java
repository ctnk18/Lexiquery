package com.project.lexiquery.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class EvalRow {
    private final String strategy;
    private final int total;
    private final int correct;
    private final int failed;
    private final long avgLatencyMs;

    public String getAccuracy() { return total == 0 ? "0.0" : String.format("%.1f", 100.0 * correct / total); }
}
