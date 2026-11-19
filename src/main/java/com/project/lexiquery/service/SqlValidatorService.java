package com.project.lexiquery.service;

import com.project.lexiquery.config.LexiQueryProperties;
import com.project.lexiquery.exception.LexiQueryException;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Security gate between the AI and the database (defence in depth: the MySQL account is also SELECT-only).
 * The model's output is never trusted: it is parsed and must be ONE plain SELECT over allowed tables.
 */
@Service
public class SqlValidatorService {
    private static final Pattern DANGEROUS = Pattern.compile(
            "(?i)\\b(sleep|benchmark|load_file|get_lock|release_lock|sys_exec|sys_eval|outfile|dumpfile|information_schema|mysql|performance_schema|sys)\\b");

    private final String targetSchema;
    private final int maxRows;

    public SqlValidatorService(LexiQueryProperties props) {
        this.targetSchema = props.target().schema().toLowerCase();
        this.maxRows = props.limits().maxRows();
    }

    /** @return the validated SQL (with an enforced LIMIT) or throws LexiQueryException("BLOCKED", ...) */
    public String validate(String sql, Set<String> allowedTables) {
        if (sql == null || sql.isBlank()) throw blocked("Empty query.");
        String s = sql.trim();
        while (s.endsWith(";")) s = s.substring(0, s.length() - 1).trim();

        if (s.contains(";"))                                   throw blocked("Multiple statements are not allowed.");
        if (s.contains("--") || s.contains("/*") || s.contains("#")) throw blocked("SQL comments are not allowed.");
        if (DANGEROUS.matcher(s).find())                       throw blocked("Query uses a forbidden function or system schema.");

        Statement stmt;
        try {
            stmt = CCJSqlParserUtil.parse(s);
        } catch (JSQLParserException e) {
            throw blocked("Query could not be parsed as valid SQL.");
        }
        if (!(stmt instanceof PlainSelect ps))
            throw blocked("Only a single plain SELECT statement is allowed.");

        for (String raw : new TablesNamesFinder().getTables(stmt)) {
            String name = raw.replace("`", "").toLowerCase();
            int dot = name.lastIndexOf('.');
            if (dot >= 0) {
                if (!name.substring(0, dot).equals(targetSchema)) throw blocked("Cross-schema access is not allowed.");
                name = name.substring(dot + 1);
            }
            if (!allowedTables.contains(name)) throw blocked("Table '" + name + "' is not permitted for your role.");
        }

        // Enforce a row cap even if the model omitted or inflated LIMIT
        Limit limit = ps.getLimit();
        Expression rc = limit == null ? null : limit.getRowCount();
        if (!(rc instanceof LongValue lv) || lv.getValue() > maxRows) {
            Limit l = new Limit();
            l.setRowCount(new LongValue(maxRows));
            ps.setLimit(l);
        }
        return ps.toString();
    }

    private static LexiQueryException blocked(String msg) { return new LexiQueryException("BLOCKED", msg); }
}
