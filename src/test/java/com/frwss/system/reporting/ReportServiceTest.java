package com.frwss.system.reporting;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReportServiceTest {

    @Test
    public void generateSummaryReport_returnsReport() {
        ReportService svc = new ReportService();
        Report r = svc.generateSummaryReport();
        assertThat(r).isNotNull();
        assertThat(r.getTitle()).isEqualTo("Summary Report");
        assertThat(r.getMetrics()).isNotNull();
    }
}
