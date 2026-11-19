package com.project.lexiquery.model;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

/** Class (not record) so that JSP EL can read the properties via getters. */
@Getter @Builder
public class QueryResult {
    private final Long historyId;
    private final String question;
    private final String sql;
    private final String strategyUsed;
    private final String complexity;
    private final String status;
    private final String message;
    private final List<String> columns;
    private final List<List<Object>> rows;
    private final long latencyMs;
    private final int attempts;

    public boolean isSuccess() { return "SUCCESS".equals(status); }
}
