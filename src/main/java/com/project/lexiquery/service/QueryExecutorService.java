package com.project.lexiquery.service;

import com.project.lexiquery.model.QueryData;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

/** Plain JDBC execution on the READ-ONLY target connection (timeout + max rows configured on the template). */
@Service
public class QueryExecutorService {
    private final JdbcTemplate jdbc;

    public QueryExecutorService(@Qualifier("targetJdbcTemplate") JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public QueryData execute(String validatedSql) {
        return jdbc.query(validatedSql, (ResultSetExtractor<QueryData>) rs -> {
            ResultSetMetaData md = rs.getMetaData();
            int n = md.getColumnCount();
            List<String> cols = new ArrayList<>();
            for (int i = 1; i <= n; i++) cols.add(md.getColumnLabel(i));
            List<List<Object>> rows = new ArrayList<>();
            while (rs.next()) {
                List<Object> row = new ArrayList<>(n);
                for (int i = 1; i <= n; i++) row.add(rs.getObject(i));
                rows.add(row);
            }
            return new QueryData(cols, rows);
        });
    }
}
