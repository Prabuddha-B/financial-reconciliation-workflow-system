# Financial Reconciliation and Workflow Support System (FRWSS)

A centralized, enterprise-style reconciliation and workflow management platform developed as a final-year undergraduate capstone project for **CSC3213 – Project in Computer Science I**, University of Peradeniya (Group A).

FRWSS ingests financial records from several sources, automatically reconciles them, routes exceptions through a review and approval workflow, and keeps a tamper-evident audit trail of everything that happens.

> **IMPORTANT**
>
> This system does **not** process real financial transactions. The payment module uses a **simulated** gateway for demonstration only. FRWSS is a reconciliation and workflow support platform, not a payment system.

---

## Table of Contents

1. [What the system does](#what-the-system-does)
2. [Features](#features)
3. [Technology stack](#technology-stack)
4. [Getting started](#getting-started)
5. [User roles and access](#user-roles-and-access)
6. [Using the system](#using-the-system)
7. [Project structure](#project-structure)
8. [Testing](#testing)
9. [Configuration reference](#configuration-reference)
10. [Known limitations](#known-limitations)
11. [Contributors](#contributors)
12. [Academic context and license](#academic-context-and-license)

---

## What the system does

Organizations keep financial records in several places: the accounting books, payroll, receipts and stock purchases. These should agree with each other, but in practice amounts are mistyped, dates slip, and some records have nothing to back them up. Checking this by hand is slow and error-prone.

FRWSS automates that check:
 
1. **Ingest** data from CSV/XLSX files or manual entry, with validation and duplicate detection.
2. **Reconcile** accounting records against payroll, receipts and stock purchases.
3. **Review** anything the system cannot match, through manual resolution and a supervisor approval workflow.
4. **Audit** every action in a hash-chained log that exposes any later tampering.
5. **Report** results as PDF or Excel.

---

## Features

### Data Ingestion

- Upload **CSV and XLSX** files for four data types: accounting records, payroll, receipts and stock purchases.
- Preview step showing valid and invalid rows before anything is saved.
- Validation (missing reference, invalid amount, invalid date, parse errors).
- Duplicate detection, both within a file and against existing database records.
- Flexible column headers (several aliases accepted) and multiple date formats.
- **Manual entry** forms for single records.
- Optimized bulk upload endpoint for large accounting CSV files.

### Reconciliation

- **Exact matching:** reference, amount and date all agree with a payroll, receipt or stock record (100% confidence).
- **Partial matching:** reference and amount agree but the date differs within a configurable tolerance in days. The confidence score drops as the date gap grows.
- **One-to-many (bulk) matching:** a single bulk record is compared to the sum of its child records, classified as exact, partial (within a percentage tolerance) or no match.
- **Orphan detection:** records with no match in any source.
- **Candidate suggestions** to help a reviewer pick a match for an orphan.
- **Manual resolution** of orphans, followed by supervisor **approve or reject**, with optional submission into the workflow module.
- Each run processes records in batches of 10, so large datasets take several runs.

### Workflow management

- State-based review pipeline: `ENTERED` → `REVIEWED` → `VERIFIED` (or `REJECTED`).
- Transition validation and a transition log for every state change.
- Approval and status pages, plus a REST API under `/api/workflows`.

### Audit and traceability

- Every significant action is written to the audit log.
- Each entry stores a **SHA-256 hash chained to the previous entry**, so any edit to history breaks the chain.
- Chain verification with a detailed report of any breaks.
- Auditors can acknowledge a chain break with a justification, which is itself logged.
- Audit viewer with filtering by action type, user and result limit.

### Message and notifications 

- Internal messaging: inbox, sent items, compose, reply, delete, unread counts.
- Notifications for workflow approvals, reconciliation results, approvals/rejections, uploads and reminders.

### Payments (simulated)

- Simulated payment gateway with success/failure outcomes, status lookup and refunds.
- Webhook endpoint at `/api/webhooks`.
- Payment reconciliation that links successful payments to accounting records and checks the amounts match.

### Tax estimation

- Tax estimation tool driven by configurable tax settings.
- Admin pages to manage tax configurations.

### Reporting

- Generate reconciliation reports as **PDF** or **Excel**, with filters, then download.

### Security and administration

- Form login with BCrypt-hashed passwords.
- **Role-based access control** (admin, entry staff, auditor, approver).
- One active session per user, 60-minute session timeout.
- PIN verification step and demo account switching for demonstrations.
- Admin dashboard and user management.
- Security audit logging.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot, Spring MVC, Spring Data JPA (Hibernate), Spring Security |
| Frontend | Thymeleaf, Bootstrap 5, HTML5, CSS3, JavaScript |
| Database | PostgreSQL (developed against a hosted Neon instance; any PostgreSQL works) |
| File handling | Apache Commons CSV, Apache POI (XLSX) |
| Reports | OpenPDF (PDF), Apache POI (Excel) |
| Build | Maven (wrapper included) |
| Testing | JUnit 5, Mockito, Spring Security Test, H2 (integration tests) |
| Version control | Git, GitHub |

---

## Getting started

### Prerequisites

- **JDK 17** (`java -version` should report 17 or higher)
- A **PostgreSQL** database (local install or a hosted one such as Neon) that you can connect to
- Git
- No separate Maven install is needed; use the included wrapper (`./mvnw`, or `mvnw.cmd` on Windows)

### 1. Clone the repository

```bash
git clone <repository-url>
cd CSC3213_20_Group_A
```

### 2. Create the database

Create an empty PostgreSQL database (for example `frwss`). Tables are created automatically on first start (`spring.jpa.hibernate.ddl-auto=update`).

### 3. Set the environment variables

The application reads its database connection from environment variables. These three are **required**:
 
| Variable | Example |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/frwss` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | `your_password` |

**macOS / Linux:**
```bash
export DB_URL="jdbc:postgresql://localhost:5432/frwss"
export DB_USERNAME="postgres"
export DB_PASSWORD="your_password"
```

**Windows (PowerShell):**
```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/frwss"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_password"
```

**IntelliJ IDEA:** add the three variables under *Run > Edit Configurations > Environment variables*.
 
For a hosted database such as Neon, use the JDBC URL from your provider (it normally needs `?sslmode=require`).
 
Optional settings are listed in the [configuration reference](#configuration-reference).

### 4. Run the application

```bash
./mvnw spring-boot:run
```

Open **http://localhost:8080**. You will be redirected to the login page.

### 5. Log In

On first start the system seeds four demo accounts:
 
| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | Admin |
| `entry` | `entry123` | Entry staff |
| `auditor` | `auditor123` | Auditor |
| `approver` | `approver123` | Approver |

> These are demo credentials for academic use. Change or remove them before any real deployment.

### Building a JAR (optional)

```bash
./mvnw clean package
java -jar target/*.jar
```

(The same environment variables must be set.)

---

## User roles and access

| Role | Can do |
|---|---|
| **Admin** | Everything, including user management, tax configuration and the admin dashboard |
| **Entry staff** | Upload files, save ingested data, manual entry |
| **Auditor** | Audit log viewer and chain verification, workflow pages |
| **Approver** | Workflow approvals, workflow pages |
 
All other pages (reconciliation, messaging, notifications, payments, tax estimation, reports) require any logged-in user.
 
---

## Using the system 

A typical end-to-end run:
 
1. **Log in as `entry`** and go to *Ingestion > Upload*. Upload payroll, receipts and stock purchases first, then accounting records. Review the preview and click Save for each.
2. **Go to Reconciliation.**
   - Run **One-to-many** first (this must come before the exact run so bulk records are still detectable).
   - Run **Exact** matching repeatedly until nothing is left (10 records per run).
   - Run **Partial** matching with a date tolerance (3 days is the default) a few times.
3. **Review orphans.** Open an orphan, look at the suggested candidates and resolve it manually. Send it to workflow review.
4. **Log in as `approver`** to approve or reject items in *Workflow*.
5. **Log in as `auditor`** and open *Audit*. Browse the log and run **Verify chain**; it should report the chain as valid.
6. **Try the other modules:** send a message, check notifications, process a simulated payment and reconcile it, run a tax estimate, and generate a PDF/Excel report.

### Sample data

Ready-made CSV files and a guide with the expected results are provided in `test-data/`:
 
| File | Data type |
|---|---|
| `payroll.csv` | PAYROLL |
| `receipts.csv` | RECEIPT |
| `stock_purchases.csv` | STOCK |
| `accounting_records.csv` | ACCOUNTING |
| `accounting_invalid_and_duplicates.csv` | ACCOUNTING (validation test) |
 
See `test-data/README_TEST_DATA.md` for the upload order and the expected exact, partial, orphan and bulk results.
 
---

## Project structure
```text
src/
├── main/
│   ├── java/com/frwss/system/
│   │   ├── controller/       Ingestion, manual entry, home
│   │   ├── service/          Ingestion service
│   │   ├── repository/       Core JPA repositories
│   │   ├── model/            Core entities (accounting, payroll, receipt, stock, user)
│   │   ├── dto/              Ingestion DTOs
│   │   ├── config/           Spring Security configuration
│   │   ├── advice/           Global model attributes
│   │   ├── reconciliation/   Exact / partial / one-to-many matching, orphans, manual review
│   │   ├── audit/            Hash-chained audit log, chain verification, viewer
│   │   ├── workflow/         State machine, transition validation, approvals, REST API
│   │   ├── messaging/        Internal messages and notifications
│   │   ├── payment/          Simulated gateway, payment reconciliation, webhook
│   │   ├── tax/              Tax estimation and configuration
│   │   ├── reporting/        PDF and Excel report generation
│   │   ├── admin/            Admin dashboard
│   │   └── security/         Users, roles, PIN verification, account switching, data seeder
│   │
│   └── resources/
│       ├── templates/        Thymeleaf pages (one folder per module)
│       ├── static/           CSS, JS, images
│       └── application.properties
│
└── test/                     Unit and integration tests (mirrors the main structure)
```

All code lives under the package namespace `com.frwss.system`, and new modules should follow the same layout (controller / service / repository / model / dto).

---

# Testing

Run the full suite:

```bash
./mvnw test
```

Run specific test classes:

```bash
./mvnw test -Dtest=ReconciliationServiceTest,AuditServiceTest
```

The suite covers:
 
- **Service layer:** ingestion, reconciliation, audit, messaging and notifications, simulated payments, payment reconciliation, tax estimation, workflow
- **Controllers:** ingestion, manual entry, workflow, tax
- **Models and DTOs:** validation and entity behavior
- **Security:** security context adapter
- **Integration tests:** ingestion, tax configuration, workflow (using an in-memory H2 database via `src/test/resources/application-test.properties`)
Unit tests use JUnit 5 and Mockito and follow an Arrange / Act / Assert layout. See `SPRINT_4_TESTING_PLAN.md` for the testing approach.

---

## Configuration reference

Beyond the three required database variables, these optional environment variables (with defaults) can be set:
 
| Variable | Default | Purpose |
|---|---|---|
| `SECURITY_ENABLED` | `true` | Turn the security layer on or off |
| `DEV_USERNAME` / `DEV_ROLE` | `admin` / `REVIEWER` | Development fallback identity |
| `DEV_ALLOWED_ROLES` | `REVIEWER,SUPERVISOR,AUDITOR,ADMIN` | Roles accepted for the fallback identity |
| `RECONCILIATION_TOLERANCE_DAYS` | `3` | Default date tolerance for partial matching |
| `RECONCILIATION_CANDIDATE_SEARCH_DAYS` | `7` | Search window for candidate suggestions |
| `AUDIT_HASH_ALGORITHM` | `SHA-256` | Hash algorithm for the audit chain |
| `AUDIT_RETENTION_DAYS` | `365` | Audit retention setting |
| `WORKFLOW_TIMEOUT_MINUTES` | `30` | Workflow timeout setting |
| `LOG_LEVEL` | `INFO` | Application log level |
 
Uploads are limited to **20 MB** per file.

---

## Known limitations

- The payment gateway is **simulated** (about 95% of payments succeed at random); no real money moves.
- Matching is exact on reference and amount, with tolerance only on dates (partial) or percentage (bulk). There is no fuzzy matching on references.
- Reconciliation runs process 10 records at a time.
- Demo accounts and passwords are seeded for academic use only.
- Designed for small to moderate data volumes, not production-scale reconciliation.

## Future enhancements
 
- AI-assisted anomaly detection
- Fuzzy reference matching
- Advanced reconciliation analytics and dashboard visualizations
- Integration with a real payment provider
- Docker deployment

---

## Contributors

**Group A – CSC3213 Project in Computer Science I**, University of Peradeniya


| Team Member | Registration Number |
|---|---|
| Navindu Rajamanthri | S/21/468 |
| Prabuddha Bandara | S/21/324 |
| Sandaru Ruvishan | S/21/347 |
| Nikhila Eranda | S/21/348 |


---

## Academic context and license
 
This project was developed as part of the undergraduate Computer Science curriculum using an Agile / prototyping methodology.
 
**License:** Academic use only. Not intended for production financial operations.

