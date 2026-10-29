# FRWSS UI/UX Refactor — Individual Contribution

**Branch:** `feature/ui-ux-refactor`  
**Project:** Financial Reconciliation and Workflow Support System (FRWSS)  
**Course:** CSC3213 — Project in Computer Science I  
**Contribution statement:** This README documents the UI/UX refactoring work attributed to my contribution to this branch. The contributor reports completing approximately 95% of the UI/UX update, with the remaining work completed by a teammate.


## 1. Branch Purpose

The purpose of this branch is to improve the visual consistency, usability, and enterprise presentation of the FRWSS web interface while retaining the existing Spring Boot and Thymeleaf application structure.

The UI/UX work introduces a more consistent design language across shared page fragments and functional modules, including dashboards, reconciliation, audit logs, reporting, workflow pages, ingestion, administration, authentication, and supporting screens.

FRWSS remains a reconciliation and workflow support platform. It does **not** execute real financial transactions, payments, or transfers.

## 2. My UI/UX Contribution

The following areas are present in the supplied branch snapshot and describe the scope of the UI/UX refactor. The precise attribution of individual files should be confirmed against Git history before using this list as formal authorship evidence.

### 2.1 Shared design system

The shared header defines reusable design tokens and utility styles, including:

- Primary and semantic colors.
- Light-mode surfaces, borders, text hierarchy, and shadows.
- Standardized radius and spacing-related visual conventions.
- Reusable card, title, subtitle, panel, and page-header classes.
- Inter typography through Google Fonts.
- Bootstrap 5.3.3 and Bootstrap Icons integration.

The semantic tokens provide a foundation for more consistent styling across pages.

### 2.2 Light and dark theme support

The branch includes a theme stylesheet and JavaScript theme controller:

- `static/css/theme-dark.css` defines dark-mode values for surfaces, borders, text, cards, tables, and other common UI elements.
- `static/js/theme-toggle.js` applies Bootstrap's `data-bs-theme` attribute, reads the stored theme preference, respects the operating-system preference when no saved choice exists, and persists the selected theme in `localStorage`.
- Theme toggle styling is included in the shared navigation styles.

The implementation should be tested in both themes across all key pages to identify component-specific contrast or styling gaps.

### 2.3 Shared navigation and layout fragments

The shared Thymeleaf fragments include:

- `templates/fragments/header.html` — shared page metadata, dependencies, typography, design tokens, and common visual utilities.
- `templates/fragments/navbar.html` — navigation spacing, link hover/focus treatment, icon alignment, badge placement, theme-toggle presentation, profile section styling, and dropdown styling.
- `templates/fragments/footer.html` — consistent footer layout, brand/system-name hierarchy, border, and responsive adjustments.
- `templates/fragments/scripts.html` — shared JavaScript functions for message and notification badges and other shared page behavior.

These fragments are central to a consistent experience across modules.

### 2.4 Dashboard and module page styling

The supplied branch contains UI implementations or styling for the following pages:

| Area | UI files present in the snapshot | Visible design work |
|---|---|---|
| Home dashboard | `templates/home.html` | Dashboard hero, design tokens, light/dark visual variables, cards and dashboard styling |
| Administrator dashboard | `templates/admin/dashboard.html` | Enterprise-style dashboard layout, hero header, statistic cards and dark-mode styling |
| Audit logs | `templates/audit/logs.html` | Audit-focused hero, KPI cards, responsive styles and visual hierarchy |
| Reconciliation | `templates/reconciliation/dashboard.html` and related reconciliation templates | Shared visual tokens, statistic cards, secondary navigation and result-oriented page styling |
| Reports | `templates/report/reports.html`, `static/css/report.css` | Report-format cards, PDF/Excel visual differentiation, responsive heading treatment and reporting styles |
| Ingestion | `templates/ingestion/` | Dashboard, upload, manual entry, form and result screens |
| Workflow | `templates/workflow/` | Approval and status pages |
| Administration | `templates/admin/` | User-management and tax-configuration screens |
| Security | `templates/security/` | Login, PIN verification, account switching and access-denied screens |
| Other application areas | `templates/messages/`, `templates/notifications/`, `templates/payment/` | Messaging, notifications and payment-dashboard presentation |

The presence of a template in the branch does not by itself prove that the template was newly created or fully redesigned in this branch. Use a diff against the parent branch to identify the exact UI changes.

### 2.5 Responsive and interaction-aware styling

The CSS visible in the snapshot includes responsive media queries, hover/focus treatments, transitions, and layout adjustments. The UI review should verify that these styles remain usable at desktop, tablet, and mobile widths.

Visual changes must preserve the behavior of existing Thymeleaf and Bootstrap integrations, including:

- `th:href`, `th:action`, and other Thymeleaf bindings.
- Forms, event handlers, and existing JavaScript integrations.
- Bootstrap dropdown and collapse behavior.
- Role-based navigation and conditional rendering.
- Existing controller routes and form submission flows.

This README documents the UI/UX scope; it does not claim that every route or interaction has already passed regression testing.

## 3. Main UI/UX Files

| File or directory | Responsibility |
|---|---|
| `src/main/resources/templates/fragments/header.html` | Shared page head, dependencies, typography and design tokens |
| `src/main/resources/templates/fragments/navbar.html` | Shared navigation, hover/focus states, icons, badges and profile styling |
| `src/main/resources/templates/fragments/footer.html` | Shared footer and responsive styling |
| `src/main/resources/templates/fragments/scripts.html` | Shared badge and page-level JavaScript functions |
| `src/main/resources/static/css/theme-dark.css` | Dark-theme semantic tokens and component overrides |
| `src/main/resources/static/js/theme-toggle.js` | Theme selection, application and persistence |
| `src/main/resources/static/css/common/admin-management.css` | Shared administration-management styling |
| `src/main/resources/static/css/report.css` | Reporting-related styles |
| `src/main/resources/templates/home.html` | Home dashboard presentation |
| `src/main/resources/templates/audit/logs.html` | Audit-log presentation |
| `src/main/resources/templates/reconciliation/` | Reconciliation page templates |
| `src/main/resources/templates/report/reports.html` | Report and export page |
| `src/main/resources/templates/admin/` | Administrator dashboard and management pages |
| `src/main/resources/templates/ingestion/` | Ingestion screens |
| `src/main/resources/templates/workflow/` | Workflow approval and status screens |
| `src/main/resources/templates/security/` | Authentication and access-related screens |

## 4. Technology and UI Stack

The supplied project uses:

- Java 17 and Spring Boot.
- Thymeleaf server-rendered templates.
- HTML5 and CSS3.
- Bootstrap 5.3.3.
- Bootstrap Icons.
- JavaScript.
- Inter typography loaded through Google Fonts.
- Maven for project build and dependency management.

The UI refactor is implemented within the existing application rather than introducing a separate frontend application.

## 5. UI Verification Checklist

Complete these checks against a running local application and record actual results.

- [ ] Shared header, navigation, and footer render correctly on key routes.
- [ ] Light mode renders correctly.
- [ ] Dark mode renders correctly.
- [ ] Theme selection persists after navigation and page reload.
- [ ] Theme selection behaves correctly when no saved preference exists.
- [ ] Navigation links and dropdowns remain functional.
- [ ] Message and notification badges retain their expected behavior.
- [ ] Forms, validation feedback, and submit actions remain functional.
- [ ] Role-based navigation and restricted screens remain correct.
- [ ] Home, admin, audit, reconciliation, reporting, ingestion, and workflow pages render without layout regressions.
- [ ] Tables, status badges, cards, and forms remain readable in both themes.
- [ ] Pages remain usable at desktop, tablet, and mobile viewport widths.
- [ ] Browser console and server logs show no new UI-related errors.
- [ ] Existing routes, Thymeleaf bindings, and JavaScript behavior are unchanged by visual-only edits.

Do not mark a checklist item as passed until it has been tested.

## 6. Build and Run

### Prerequisites

- JDK 17.
- A PostgreSQL or other database configuration supported by the current project configuration.
- Maven Wrapper included in the repository.

### Run on Windows PowerShell

From the repository root:

```powershell
.\mvnw.cmd spring-boot:run
```

### Run verification

```powershell
.\mvnw.cmd clean verify
```

If the Maven Wrapper is not available, use an installed Maven version:

```bash
mvn spring-boot:run
mvn clean verify
```

These commands describe how to execute the application and verification lifecycle. They do not represent a claim that the application or test suite has passed; record the actual output from your environment.

## 7. Contribution Attribution and Evidence

The contributor reports that approximately 95% of the UI/UX update was completed by me and the remaining work by a teammate. The supplied ZIP does not contain `.git` history, commit authorship, or a parent-branch diff, so it is not possible to independently assign exact files to each person from this archive alone.

Before final submission, complete this section using verified repository evidence:

- **My contribution:** UI/UX design and implementation across the files confirmed by the branch diff and commit history.
- **Teammate's contribution:** _List the specific pages, components, or fixes completed by the teammate after confirming with them._
- **Branch:** `feature/ui-ux-refactor`
- **Pull request:** _Add the GitHub pull request URL._
- **Commits:** _Add relevant commit hashes or links._
- **Verification evidence:** _Add screenshots, test results, or CI links for checks actually completed._

Do not use the percentage alone as proof of authorship. A reviewed file list and commit/PR history provide stronger evidence for examiners.

## 8. Scope Boundary

This branch concerns presentation and user interaction. It must not introduce real financial transaction execution or expand FRWSS beyond the approved reconciliation and workflow support scope. Any functional changes discovered during UI integration should be reviewed separately and tested against the existing application behavior.

## 9. Academic Context

This branch is part of the CSC3213 FRWSS group project. Its purpose is to improve the consistency, accessibility, responsiveness, and enterprise presentation of the existing application while preserving established module behavior. This README is intended to help examiners locate the UI implementation and review its contribution and verification evidence.
