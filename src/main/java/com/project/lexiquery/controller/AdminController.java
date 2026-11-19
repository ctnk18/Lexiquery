package com.project.lexiquery.controller;

import com.project.lexiquery.repository.QueryHistoryRepository;
import com.project.lexiquery.service.EvaluationService;
import com.project.lexiquery.service.SchemaService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AdminController {
    private final QueryHistoryRepository history;
    private final EvaluationService evaluation;
    private final SchemaService schemaService;

    public AdminController(QueryHistoryRepository history, EvaluationService evaluation, SchemaService schemaService) {
        this.history = history; this.evaluation = evaluation; this.schemaService = schemaService;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        fill(model);
        return "admin";
    }

    @PostMapping("/admin/evaluate")
    public String evaluate(Authentication auth, Model model) {
        fill(model);
        model.addAttribute("evalRows", evaluation.run(auth.getName()));
        return "admin";
    }

    @PostMapping("/admin/refresh-schema")
    public String refreshSchema() {
        schemaService.refresh();
        return "redirect:/admin";
    }

    private void fill(Model model) {
        model.addAttribute("success", history.countByStatus("SUCCESS"));
        model.addAttribute("failed", history.countByStatus("FAILED"));
        model.addAttribute("blocked", history.countByStatus("BLOCKED"));
        model.addAttribute("stats", history.strategyStats());
        model.addAttribute("blockedItems", history.findTop20ByStatusOrderByCreatedAtDesc("BLOCKED"));
    }
}
