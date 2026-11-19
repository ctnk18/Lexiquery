package com.project.lexiquery.model;

import java.util.*;
import java.util.stream.Collectors;

/** Lightweight in-memory snapshot of the target database schema. */
public class Schema {
    public record Column(String name, String type, boolean primaryKey) {}
    public record Table(String name, List<Column> columns) {}
    public record ForeignKey(String table, String column, String refTable, String refColumn) {}

    private final List<Table> tables;
    private final List<ForeignKey> foreignKeys;

    public Schema(List<Table> tables, List<ForeignKey> foreignKeys) {
        this.tables = tables; this.foreignKeys = foreignKeys;
    }

    public List<Table> tables() { return tables; }
    public List<ForeignKey> foreignKeys() { return foreignKeys; }

    public Set<String> tableNames() {
        return tables.stream().map(t -> t.name().toLowerCase()).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Subset of the schema containing only the given tables (case-insensitive). */
    public Schema restrictTo(Set<String> names) {
        List<Table> t = tables.stream().filter(x -> names.contains(x.name().toLowerCase())).toList();
        List<ForeignKey> f = foreignKeys.stream()
                .filter(x -> names.contains(x.table().toLowerCase()) && names.contains(x.refTable().toLowerCase())).toList();
        return new Schema(t, f);
    }

    /** Tables directly connected through a foreign key to any of the given tables. */
    public Set<String> neighbours(Collection<String> names) {
        Set<String> out = new HashSet<>();
        for (ForeignKey fk : foreignKeys) {
            if (names.contains(fk.table().toLowerCase())) out.add(fk.refTable().toLowerCase());
            if (names.contains(fk.refTable().toLowerCase())) out.add(fk.table().toLowerCase());
        }
        return out;
    }

    public String toPromptText() {
        StringBuilder sb = new StringBuilder();
        for (Table t : tables) {
            sb.append("TABLE ").append(t.name()).append("(");
            sb.append(t.columns().stream()
                    .map(c -> c.name() + " " + c.type() + (c.primaryKey() ? " PK" : ""))
                    .collect(Collectors.joining(", ")));
            sb.append(")\n");
        }
        if (!foreignKeys.isEmpty()) {
            sb.append("FOREIGN KEYS:\n");
            for (ForeignKey f : foreignKeys)
                sb.append("  ").append(f.table()).append('.').append(f.column())
                  .append(" -> ").append(f.refTable()).append('.').append(f.refColumn()).append('\n');
        }
        return sb.toString();
    }
}
