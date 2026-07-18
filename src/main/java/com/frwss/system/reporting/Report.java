package com.frwss.system.reporting;

import java.time.LocalDate;
import java.util.Map;

public class Report {
    private Long id;
    private String title;
    private LocalDate generatedAt;
    private Map<String, Object> metrics;

    public Report() {}

    public Report(Long id, String title, LocalDate generatedAt, Map<String, Object> metrics) {
        this.id = id;
        this.title = title;
        this.generatedAt = generatedAt;
        this.metrics = metrics;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDate generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Map<String, Object> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<String, Object> metrics) {
        this.metrics = metrics;
    }
}
