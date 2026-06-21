package com.frwss.system.reporting;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public String reportsIndex(Model model) {
        Report report = reportService.generateSummaryReport();
        model.addAttribute("report", report);
        return "reports/index"; // src/main/resources/templates/reports/index.html
    }

    @GetMapping("/export/csv")
    @ResponseBody
    public void exportCsv(HttpServletResponse response) throws IOException {
        Report r = reportService.generateSummaryReport();
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.csv");

        StringBuilder sb = new StringBuilder();
        sb.append("metric,value\n");
        Map<String, Object> metrics = r.getMetrics();
        if (metrics != null) {
            metrics.forEach((k, v) -> sb.append(k).append(",").append(v).append("\n"));
        }
        response.getWriter().write(sb.toString());
    }
}
