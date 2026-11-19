package com.project.lexiquery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

/** Doubles as the audit log AND the learning signal (feedback) for few-shot retrieval. */
@Entity
@Table(name = "query_history", indexes = {@Index(columnList = "username"), @Index(columnList = "status")})
@Getter @Setter
public class QueryHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)  private String username;
    @Column(nullable = false, length = 600) private String question;
    @Column(columnDefinition = "TEXT")      private String generatedSql;
    @Column(length = 20)  private String strategy;     // RULE / LLM (what actually produced the SQL)
    @Column(length = 20)  private String status;       // SUCCESS / FAILED / BLOCKED
    @Column(length = 400) private String message;
    private Integer rowCount;
    private Long latencyMs;
    private Integer attempts;
    private Integer feedback;                          // +1 / -1 / null
    private Instant createdAt = Instant.now();
}
