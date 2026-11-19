package com.project.lexiquery.service;

import com.project.lexiquery.config.LexiQueryProperties;
import com.project.lexiquery.model.Schema;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/** Reads table/column/foreign-key metadata of the target DB from INFORMATION_SCHEMA (cached 10 min). */
@Service
public class SchemaService {
    private static final long TTL_MS = 10 * 60 * 1000;

    private final JdbcTemplate jdbc;
    private final String schemaName;
    private volatile Schema cached;
    private volatile long loadedAt;

    public SchemaService(@Qualifier("targetJdbcTemplate") JdbcTemplate jdbc, LexiQueryProperties props) {
        this.jdbc = jdbc;
        this.schemaName = props.target().schema();
    }

    public Schema getSchema() {
        if (cached == null || System.currentTimeMillis() - loadedAt > TTL_MS) refresh();
        return cached;
    }

    public synchronized void refresh() {
        Map<String, List<Schema.Column>> cols = new LinkedHashMap<>();
        jdbc.query("""
                SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, COLUMN_KEY
                FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ?
                ORDER BY TABLE_NAME, ORDINAL_POSITION""",
                rs -> {
                    cols.computeIfAbsent(rs.getString(1), k -> new ArrayList<>())
                        .add(new Schema.Column(rs.getString(2), rs.getString(3), "PRI".equals(rs.getString(4))));
                }, schemaName);

        List<Schema.ForeignKey> fks = jdbc.query("""
                SELECT TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
                FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = ? AND REFERENCED_TABLE_NAME IS NOT NULL""",
                (rs, i) -> new Schema.ForeignKey(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)),
                schemaName);

        List<Schema.Table> tables = new ArrayList<>();
        cols.forEach((name, list) -> tables.add(new Schema.Table(name, list)));
        this.cached = new Schema(tables, fks);
        this.loadedAt = System.currentTimeMillis();
    }
}
