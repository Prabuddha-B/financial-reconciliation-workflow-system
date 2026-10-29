# FRWSS — Workflow Feature Branch

**Branch:** `feature/workflow-Prabuddha`  
**Project:** Financial Reconciliation and Workflow Support System (FRWSS)  


## 1. Branch Purpose

This branch contains workflow-management implementation for FRWSS. The workflow module provides a controlled lifecycle for records submitted to review, verification, or rejection. It connects workflow state changes with transition history, role-aware web interactions, and the system audit service.

FRWSS is a reconciliation and workflow support system. It does **not** execute real financial transactions, payments, or transfers.

> **Contribution attribution note:** This README describes functionality present in the supplied branch snapshot. A ZIP snapshot does not establish which lines were authored by a particular contributor or whether every file differs from the parent branch. Commit history or a branch diff is required for definitive attribution.

## 2. Workflow Functionality Present in This Branch

### Workflow lifecycle

The workflow state model defines these states:

- `ENTERED` — initial state when a workflow record is created.
- `REVIEWED` — record has passed the review stage.
- `VERIFIED` — record has been verified.
- `REJECTED` — record has been rejected after review.

The transition validator permits the following transitions:

| Current state | Requested state | Supported |
|---|---|---|
| `ENTERED` | `REVIEWED` | Yes |
| `REVIEWED` | `VERIFIED` | Yes |
| `REVIEWED` | `REJECTED` | Yes |
| Any other transition | Any other state | No |

### Separation of responsibilities

The workflow implementation uses a layered structure:

- **Controllers** expose REST endpoints and render Thymeleaf workflow pages.
- **Service layer** implements workflow creation, state changes, history retrieval, and latest-activity retrieval.
- **Repositories** persist workflow records and transition logs through Spring Data JPA.
- **Entities and DTOs** represent persisted workflow state and request/response data.
- **Transition validator** centralizes the allowed state-transition rules.
- **Audit integration** records workflow state changes through the application's audit service.
- **Reconciliation integration** forwards verified or rejected manual-resolution outcomes to the reconciliation service when the workflow source type is `RECONCILIATION`.

### Review controls visible in the implementation

The workflow service checks that:
- a requested state transition is allowed;
- a justification is supplied;
- the workflow creator does not review or approve their own workflow;
- the same user does not perform consecutive transitions.

The workflow web controller also applies role checks: users with `APPROVER` or `ADMIN` roles can perform the review transition, while users with `AUDITOR` or `ADMIN` roles can verify or reject workflows. These controls are part of the current implementation and should be validated against the project's final security requirements.

### Transition history

Each successful state change creates a `WorkflowTransitionLog` entry containing:
- workflow identifier;
- previous and new states;
- acting user;
- justification;
- transition timestamp.

This supports inspection of the workflow's state-change history. The transition log is separate from the application's audit service; do not describe the workflow transition table alone as a cryptographic hash-chain.

## 3. Main Workflow Files

| File or package | Responsibility |
|---|---|
| `workflow/controller/WorkflowController.java` | REST API for creating, listing, retrieving, and updating workflow records and reading history |
| `workflow/controller/WorkflowViewController.java` | Thymeleaf page routes and form-driven state transitions |
| `workflow/service/WorkflowService.java` | Workflow service contract |
| `workflow/service/WorkflowServiceImpl.java` | Workflow business logic and audit/reconciliation integration |
| `workflow/validation/WorkflowTransitionValidator.java` | Allowed state-transition rules |
| `workflow/model/WorkflowRecord.java` | Current workflow state and source-record metadata |
| `workflow/model/WorkflowState.java` | Workflow state enumeration |
| `workflow/model/WorkflowTransitionLog.java` | Persistent state-transition history |
| `workflow/repository/WorkflowRecordRepository.java` | Workflow record persistence and source lookup |
| `workflow/repository/WorkflowTransitionLogRepository.java` | Transition-history persistence and latest-transition lookup |
| `src/main/resources/templates/workflow/approvals.html` | Workflow approvals page |
| `src/main/resources/templates/workflow/status.html` | Workflow status and history page |

The package namespace is `com.frwss.system.workflow`.

## 4. Workflow Endpoints

The REST controller is rooted at `/api/workflows`.

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/workflows` | Create a workflow record |
| `GET` | `/api/workflows` | Retrieve all workflow records |
| `GET` | `/api/workflows/{workflowId}` | Retrieve one workflow |
| `GET` | `/api/workflows/{workflowId}/history` | Retrieve transition history |
| `PUT` | `/api/workflows/{workflowId}/state` | Request a workflow state change |

The web interface provides these routes:

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/workflow/approvals` | Display workflow approval records |
| `GET` | `/workflow/status/{workflowId}` | Display workflow state and transition history |
| `POST` | `/workflow/{workflowId}/transition` | Submit a state transition from the web interface |

The exact accessibility of these routes depends on the application's active Spring Security configuration.

## 5. Technology Stack

The supplied project configuration identifies the following technologies:

- Java 17
- Spring Boot
- Spring MVC
- Spring Data JPA and Hibernate
- Spring Security
- Thymeleaf
- Bootstrap
- Maven
- PostgreSQL JDBC driver

## 6. Run the Application Locally

### Prerequisites

- JDK 17
- A compatible PostgreSQL instance
- Git, if checking out the branch
- The project's database and application configuration

### Start on Windows PowerShell

From the repository root:

```powershell
.\mvnw.cmd spring-boot:run
```

### Run the Maven verification lifecycle

```powershell
.\mvnw.cmd clean verify
```

On systems where the Maven Wrapper is unavailable, use an installed Maven version:

```bash
mvn spring-boot:run
mvn clean verify
```

Configure the database using the application's supported configuration before starting it. Do not commit real credentials or sensitive data.

The commands above describe how to invoke the build and application. They are **not a claim that the build or tests passed**; execute them in the target environment and record the actual result.

## 7. Manual Workflow Verification Checklist

Use an appropriately configured local development database and test accounts.

- [ ] Create a workflow and confirm its initial state is `ENTERED`.
- [ ] Confirm `ENTERED → REVIEWED` is accepted for an authorized reviewer.
- [ ] Confirm `REVIEWED → VERIFIED` is accepted for an authorized auditor.
- [ ] Confirm `REVIEWED → REJECTED` is accepted for an authorized auditor.
- [ ] Confirm unsupported transitions are rejected.
- [ ] Confirm a missing or blank justification is rejected.
- [ ] Confirm the workflow creator cannot review or approve their own workflow.
- [ ] Confirm the same user cannot perform consecutive transitions.
- [ ] Confirm unauthorized roles cannot submit restricted transitions through the web interface.
- [ ] Confirm each successful transition creates a history record with actor, states, justification, and timestamp.
- [ ] Confirm workflow state changes are sent to the audit service.
- [ ] For `RECONCILIATION` source records, verify that verification/rejection invokes the corresponding reconciliation operation.
- [ ] Verify missing workflow identifiers produce the expected not-found behavior.

Record the date, environment, test data, actual result, and evidence for each check. Do not mark a check as passed until it has been executed.

## 8. Test Status and Evidence

The supplied ZIP snapshot does not contain a `src/test` test-source tree. Therefore, this README does not claim that automated workflow unit or integration tests are included in this branch snapshot.

The `clean verify` command can be used to run the Maven verification lifecycle, but its actual outcome must be confirmed in the development environment. Add links to test reports, screenshots, or CI runs here once they exist.

## 9. Known Review Points

The following points should be considered during final integration and review:

1. **Exception specificity:** some workflow validation failures currently use generic `RuntimeException` instances. Confirm that the global exception handling maps them to appropriate user-facing responses.
2. **Input validation:** confirm that incoming DTOs and workflow identifiers are validated at the API boundary.
3. **Authorization consistency:** review API-level authorization as well as the role checks in `WorkflowViewController`.
4. **Audit and transaction behavior:** verify the intended transaction boundary when a workflow transition, transition log, audit entry, and reconciliation operation are performed together.
5. **Data integrity:** verify source-record identifiers and the workflow's relationship to reconciliation records before release.

These are review points, not assertions that a defect has been reproduced.

## 10. Scope and Safety Boundary

The workflow module supports review and approval of reconciliation-related records. It must not be used to execute payments, transfers, or other real financial transactions. Development and demonstration should use synthetic or anonymized data.


