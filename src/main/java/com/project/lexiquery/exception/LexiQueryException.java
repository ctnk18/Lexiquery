package com.project.lexiquery.exception;

public class LexiQueryException extends RuntimeException {
    private final String status;   // FAILED or BLOCKED
    public LexiQueryException(String status, String message) { super(message); this.status = status; }
    public String getStatus() { return status; }
}
