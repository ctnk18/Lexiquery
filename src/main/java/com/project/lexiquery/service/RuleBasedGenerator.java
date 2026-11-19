package com.project.lexiquery.service;

import com.project.lexiquery.model.Schema;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Baseline: deterministic regex templates for simple questions (the "classic NLP" approach in the comparison). */
@Service
public class RuleBasedGenerator {
    private static final Pattern LIST  = Pattern.compile("^(?:show|list|display|get|give me|find)(?: me)?(?: all| the)?\\s+(?:all\\s+)?(\\w+)$");
    private static final Pattern COUNT = Pattern.compile("^(?:how many|count(?: of)?(?: all)?(?: the)?|number of)\\s+(\\w+)$");
    private static final Pattern TOP   = Pattern.compile("^(?:show |list |get )?(?:the )?top\\s+(\\d{1,3})\\s+(\\w+)\\s+by\\s+(\\w+)$");

    public Optional<String> generate(String question, Schema schema) {
        String q = question.toLowerCase().replaceAll("[?.!]+$", "").trim();

        Matcher m = TOP.matcher(q);
        if (m.matches()) {
            Optional<Schema.Table> t = findTable(m.group(2), schema);
            if (t.isPresent()) {
                String col = m.group(3);
                boolean exists = t.get().columns().stream().anyMatch(c -> c.name().equalsIgnoreCase(col));
                if (exists) return Optional.of("SELECT * FROM " + t.get().name() + " ORDER BY " + col + " DESC LIMIT " + m.group(1));
            }
        }
        m = COUNT.matcher(q);
        if (m.matches()) {
            Optional<Schema.Table> t = findTable(m.group(1), schema);
            if (t.isPresent()) return Optional.of("SELECT COUNT(*) AS total FROM " + t.get().name());
        }
        m = LIST.matcher(q);
        if (m.matches()) {
            Optional<Schema.Table> t = findTable(m.group(1), schema);
            if (t.isPresent()) return Optional.of("SELECT * FROM " + t.get().name());
        }
        return Optional.empty();
    }

    private Optional<Schema.Table> findTable(String word, Schema schema) {
        String w = stem(word);
        return schema.tables().stream().filter(t -> stem(t.name()).equals(w)).findFirst();
    }

    private static String stem(String s) {
        s = s.toLowerCase().replace("_", "");
        return s.endsWith("s") ? s.substring(0, s.length() - 1) : s;
    }
}
