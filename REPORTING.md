# Reporting feature

This folder contains a small scaffold for the reporting feature implemented under the package `com.frwss.system.reporting`.

Endpoints added (when the application is running):
- GET /reports — HTML summary page (Thymeleaf template)
- GET /reports/export/csv — CSV download of metrics

Notes for maintainers:
- The current implementation returns placeholder metrics. Replace the placeholder logic in ReportService with real calculations using existing repositories (e.g., accounting, receipts, purchases).
- Files added:
  - src/main/java/com/frwss/system/reporting/Report.java
  - src/main/java/com/frwss/system/reporting/ReportService.java
  - src/main/java/com/frwss/system/reporting/ReportController.java
  - src/main/resources/templates/reports/index.html
  - src/main/resources/static/css/report.css

How to run locally:
- mvn spring-boot:run
- Open http://localhost:8080/reports

