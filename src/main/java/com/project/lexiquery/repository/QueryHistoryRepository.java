package com.project.lexiquery.repository;

import com.project.lexiquery.entity.QueryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface QueryHistoryRepository extends JpaRepository<QueryHistory, Long> {
    List<QueryHistory> findTop50ByUsernameOrderByCreatedAtDesc(String username);
    List<QueryHistory> findTop200ByFeedbackAndStatusOrderByCreatedAtDesc(Integer feedback, String status);
    List<QueryHistory> findTop20ByStatusOrderByCreatedAtDesc(String status);
    long countByStatus(String status);

    @Query("select h.strategy, count(h), avg(h.latencyMs) from QueryHistory h where h.status = 'SUCCESS' group by h.strategy")
    List<Object[]> strategyStats();
}
