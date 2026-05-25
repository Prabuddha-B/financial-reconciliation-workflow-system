# Financial Reconciliation and Workflow Support System (FRWSS)

A centralized enterprise-style reconciliation and workflow management platform developed as a final-year undergraduate capstone project for CSC3213 – Project in Computer Science I.

The system focuses on:

- Financial data ingestion
- Reconciliation workflows
- Auditability
- Workflow approvals
- Exception tracking
- Immutable audit support

> **IMPORTANT**
>
> This system DOES NOT process real financial transactions or payments.  
> It is strictly a reconciliation and workflow support platform.

---

# Project Overview

The Financial Reconciliation and Workflow Support System (FRWSS) is designed to streamline organizational reconciliation operations by integrating multiple financial data sources into a centralized workflow-driven environment.

The platform supports:

- Automated data ingestion
- Manual financial record entry
- Validation and duplicate detection
- Workflow-based review processes
- Audit-ready traceability
- Administrative oversight

---

# Core Features

## Ingestion Module

- CSV ingestion
- XLSX ingestion
- Automated ingestion pipeline
- Manual financial record entry
- Validation workflows
- Duplicate detection

## Workflow Management

- Workflow state transitions
- Approval/review pipeline
- Task tracking
- Exception management

## Audit & Traceability

- Immutable audit logs
- Historical tracking
- Reconciliation tracking
- Activity monitoring

## User Management

- Role-based access control (RBAC)
- User authentication
- Access restriction by role

---

# Technology Stack

## Backend

- Java 17
- Spring Boot
- Spring MVC
- Spring Data JPA
- Hibernate

## Frontend

- Thymeleaf
- Bootstrap 5
- HTML5
- CSS3
- JavaScript

## Database

- MySQL

## Build & Dependency Management

- Maven

## Version Control

- Git
- GitHub

---

# Project Structure

```text
src/
├── main/
│   ├── java/com/frwss/system/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── model/
│   │   ├── dto/
│   │   └── config/
│   │
│   └── resources/
│       ├── templates/
│       ├── static/
│       └── application.properties
│
└── test/
```

---

# Package Standard

The project follows the standardized package namespace:

```java
com.frwss.system
```

All modules and future development should strictly follow this structure.

---

# Current Integrated Modules

## Completed

- Ingestion frontend
- Manual entry workflows
- Backend ingestion services
- Repository integration
- Package standardization
- CSV/XLSX support
- Validation support


---


# Future Enhancements

- Hash-chain audit logging
- AI-assisted anomaly detection
- Advanced reconciliation analytics
- Dashboard visualizations
- Docker deployment

---

# Contributors

- Group A – CSC3213 Project in Computer Science I
- University of Peradeniya

---

# Academic Context

This project is developed as part of the undergraduate Computer Science curriculum using Agile/Prototyping methodology.

---

# License

Academic Use Only.  
Not intended for production financial operations.
