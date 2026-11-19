package com.project.lexiquery.service;

import com.project.lexiquery.config.LexiQueryProperties;
import com.project.lexiquery.exception.LexiQueryException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SqlValidatorServiceTest {

    private final SqlValidatorService validator = new SqlValidatorService(new LexiQueryProperties(
            new LexiQueryProperties.Target("", "", "", "lexiquery_target"),
            new LexiQueryProperties.Ml(false, "", 0.8),
            new LexiQueryProperties.Llm(false, "openai", "", "", "m", 100),
            new LexiQueryProperties.Limits(100, 10, 20, 2),
            new LexiQueryProperties.Policy(List.of())));

    private final Set<String> allowed = Set.of("customers", "orders");

    @Test
    void acceptsSimpleSelectAndEnforcesLimit() {
        String out = validator.validate("SELECT * FROM customers", allowed);
        assertTrue(out.toUpperCase().contains("LIMIT 100"), out);
    }

    @Test
    void capsExcessiveLimit() {
        String out = validator.validate("SELECT * FROM customers LIMIT 100000", allowed);
        assertTrue(out.toUpperCase().contains("LIMIT 100"), out);
        assertFalse(out.contains("100000"), out);
    }

    @Test
    void acceptsJoin() {
        assertDoesNotThrow(() -> validator.validate(
                "SELECT c.name, COUNT(o.id) FROM customers c JOIN orders o ON o.customer_id = c.id GROUP BY c.name", allowed));
    }

    @Test
    void blocksDestructiveStatements() {
        assertBlocked("DROP TABLE customers");
        assertBlocked("DELETE FROM customers");
        assertBlocked("UPDATE customers SET name = 'x'");
    }

    @Test
    void blocksStackedQueriesAndComments() {
        assertBlocked("SELECT * FROM customers; DROP TABLE customers");
        assertBlocked("SELECT * FROM customers -- hi");
        assertBlocked("SELECT * FROM customers /* x */");
    }

    @Test
    void blocksSystemSchemasAndDangerousFunctions() {
        assertBlocked("SELECT * FROM information_schema.tables");
        assertBlocked("SELECT SLEEP(10)");
        assertBlocked("SELECT * FROM customers INTO OUTFILE '/tmp/x'");
    }

    @Test
    void blocksDisallowedTables() {
        assertBlocked("SELECT * FROM payments");
    }

    private void assertBlocked(String sql) {
        LexiQueryException ex = assertThrows(LexiQueryException.class, () -> validator.validate(sql, allowed), sql);
        assertEquals("BLOCKED", ex.getStatus());
    }
}
