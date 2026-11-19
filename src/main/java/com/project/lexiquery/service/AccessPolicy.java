package com.project.lexiquery.service;

import com.project.lexiquery.config.LexiQueryProperties;
import com.project.lexiquery.entity.Role;
import com.project.lexiquery.model.Schema;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/** Role-based table access: ADMIN/ANALYST see everything, VIEWER only whitelisted tables. */
@Component
public class AccessPolicy {
    private final Set<String> viewerTables = new LinkedHashSet<>();

    public AccessPolicy(LexiQueryProperties props) {
        if (props.policy() != null && props.policy().viewerTables() != null)
            props.policy().viewerTables().forEach(t -> viewerTables.add(t.trim().toLowerCase()));
    }

    public Set<String> allowedTables(Role role, Schema schema) {
        Set<String> all = schema.tableNames();
        if (role == Role.VIEWER) { Set<String> s = new LinkedHashSet<>(all); s.retainAll(viewerTables); return s; }
        return all;
    }
}
