package com.project.lexiquery.service;

import com.project.lexiquery.config.LexiQueryProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/** Per-user sliding-window limiter: protects the LLM budget and the database from abuse. */
@Component
public class RateLimiter {
    private final int perMinute;
    private final ConcurrentHashMap<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public RateLimiter(LexiQueryProperties props) { this.perMinute = props.limits().requestsPerMinute(); }

    public boolean tryAcquire(String user) {
        long now = System.currentTimeMillis();
        Deque<Long> q = hits.computeIfAbsent(user, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > 60_000) q.pollFirst();
            if (q.size() >= perMinute) return false;
            q.addLast(now);
            return true;
        }
    }
}
