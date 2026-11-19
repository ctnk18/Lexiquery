package com.project.lexiquery.controller;

import com.project.lexiquery.entity.QueryHistory;
import com.project.lexiquery.entity.Role;
import com.project.lexiquery.model.QueryResult;
import com.project.lexiquery.model.Strategy;
import com.project.lexiquery.repository.QueryHistoryRepository;
import com.project.lexiquery.service.LexiQueryService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class QueryController {
    private final LexiQueryService service;
    private final QueryHistoryRepository history;

    public QueryController(LexiQueryService service, QueryHistoryRepository history) {
        this.service = service; this.history = history;
    }

    @GetMapping("/query")
    public String page(Model model) {
        model.addAttribute("strategies", Strategy.values());
        model.addAttribute("selected", Strategy.RULE);
        return "query";
    }

    @PostMapping("/query")
    public String run(@RequestParam String question,
                      @RequestParam(defaultValue = "RULE") Strategy strategy,
                      Authentication auth, Model model) {
        String q = question == null ? "" : question.trim();
        model.addAttribute("strategies", Strategy.values());
        model.addAttribute("selected", strategy);
        model.addAttribute("question", q);
        if (q.length() < 3 || q.length() > 500) {
            model.addAttribute("error", "Question must be between 3 and 500 characters.");
            return "query";
        }
        QueryResult result = service.process(q, auth.getName(), roleOf(auth), strategy, true);
        model.addAttribute("result", result);
        return "query";
    }

    @GetMapping("/history")
    public String history(Authentication auth, Model model) {
        model.addAttribute("items", history.findTop50ByUsernameOrderByCreatedAtDesc(auth.getName()));
        return "history";
    }

    /** Thumbs up/down. Positive feedback turns the pair into a retrievable few-shot example. */
    @PostMapping("/feedback")
    public String feedback(@RequestParam Long id, @RequestParam int value, Authentication auth) {
        if (value == 1 || value == -1) {
            history.findById(id).filter(h -> h.getUsername().equals(auth.getName())).ifPresent((QueryHistory h) -> {
                h.setFeedback(value);
                history.save(h);
            });
        }
        return "redirect:/history";
    }

    static Role roleOf(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .map(Role::valueOf).findFirst().orElse(Role.VIEWER);
    }
}
