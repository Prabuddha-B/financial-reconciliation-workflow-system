# FRWSS — Testing Branch

**Project:** Financial Reconciliation and Workflow Support System (FRWSS)  
**Course:** CSC3213 – Project in Computer Science I  
**Branch purpose:** Automated testing and quality assurance  
**Primary test framework:** JUnit 5, Mockito, Spring Boot Test

## 1. Purpose of This Branch

This branch consolidates automated tests for the existing FRWSS application. It is a development branch within the same FRWSS repository, **not a separate application**.

The test suite is intended to verify implemented business logic, validation, workflow transitions, security decisions, audit-chain behaviour, and interactions with persistence-related dependencies. Integration tests exercise selected service-to-database flows.

FRWSS is a reconciliation and workflow support system. Any payment-related tests in this branch exercise a **simulated payment gateway** and payment-reconciliation logic; they do not execute real payments or transfers.

## 2. Testing Scope

The checked-in test source contains **39 Java test classes**:

- **36 unit-test classes**
- **3 integration-test classes**
- **280 declared test methods/annotations** in total, including two parameterized test methods. The number of executed test cases can be higher because parameterized tests run once per supplied argument set.

The inventory below is based on the source files in this branch. It is not a statement that every test passed in a single final run.

### 2.1 Unit-test coverage by area

| Area | Test classes | Focus |
|---|---|---|
| Ingestion | `IngestionServiceTest`, `IngestionControllerTest` | CSV ingestion, preview/save paths, record types, duplicate/invalid inputs, controller responses, session and service-failure paths |
| Manual entry | `ManualEntryControllerTest` | Search, add and update flows for receipt, payroll, stock-purchase and accounting records; missing keys, duplicate identifiers, unsupported types and not-found cases |
| Reconciliation | `ReconciliationServiceTest` | Exact, partial and one-to-many matching; orphan records; manual resolution; approval/rejection; workflow hand-off |
| Workflow | `WorkflowServiceImplTest`, `WorkflowControllerTest`, `WorkflowViewControllerTest`, `WorkflowTransitionValidatorTest` | Workflow creation, state transitions, transition rules, role checks, history, error handling and persistence/audit/reconciliation failure paths |
| Tax | `TaxConfigurationServiceTest`, `TaxEstimationServiceTest`, `AdminTaxConfigurationControllerTest`, `TaxEstimationControllerTest` | Tax configuration retrieval/update, estimation, monetary boundaries and rounding, inactive/missing configuration and failure paths |
| Audit | `AuditServiceTest` | Genesis hash, hash chaining, tamper detection, chain verification, chain-break acknowledgement and filtered log retrieval |
| Security | `SecurityContextAdapterTest`, `GlobalModelAttributesAdviceTest` | Authentication/principal handling, role checks, development/security modes and model attributes |
| Messaging and notifications | `MessagingServiceTest`, `NotificationServiceTest` | Message sending/replying, inbox/sent/conversation retrieval, read/delete permissions and workflow/reconciliation/system notifications |
| Simulated payment | `SimulatedPaymentGatewayServiceTest`, `PaymentReconciliationServiceTest` | Simulated success/failure, status/refund validation, reconciliation against accounting records, audit interactions and summary statistics |
| Domain models and DTOs | `AccountingRecordTest`, `StockPurchaseTest`, `UserTest`, `ManualEntryFormTest`, `TaxConfigurationTest`, `TaxEstimationRequestDtoTest`, `TaxEstimationResponseDtoTest`, `WorkflowRecordTest`, `WorkflowStateTest`, `WorkflowTransitionLogTest`, `WorkflowHistoryResponseDtoTest`, `WorkflowRequestDtoTest`, `WorkflowResponseDtoTest`, `WorkflowStateUpdateRequestDtoTest` | Field assignment, defaults, enum values and data-holder behaviour |
| Exceptions | `WorkflowNotFoundExceptionTest`, `InvalidWorkflowTransitionExceptionTest`, `GlobalExceptionHandlerTest` | Exception messages and HTTP/error-handler responses |
| Application advice | `GlobalModelAttributesAdviceTest` | Shared user and role attributes added to MVC models |

`IngestionControllerTest` is stored under the existing misspelled directory `src/test/java/com/frwss/system/contoller/`. `WorkflowTransitionValidatorTest` is also stored in a non-standard directory and declares a package name that differs from the normal production package hierarchy. These layout issues do not change the test inventory, but are worth correcting in a separate, carefully verified cleanup.

### 2.2 Integration tests

The integration tests use `@SpringBootTest`, the `test` profile, and `@Transactional`. They use the Spring application context and real repositories rather than mocking the main persistence path.

| Test class | Integrated flow | Main assertions |
|---|---|---|
| `IngestionIntegrationTest` | CSV upload → ingestion preview/validation → record save → receipt repository | Valid record count, save count, database count and stored receipt fields |
| `WorkflowIntegrationTest` | Workflow service → workflow record/transition-log repositories → history retrieval | Workflow creation, state transition, persisted state, transition log and history |
| `TaxConfigurationIntegrationTest` | Tax configuration repository/service → database | Configuration creation/retrieval, tax-rate update and persisted description |

These are focused integration scenarios, not a full end-to-end test of every application screen or module.

## 3. Technology and Test Design

The Maven project specifies Java 17 and Spring Boot 4.0.5. The test-related dependencies include:

- **JUnit Jupiter / JUnit 5** — test cases and assertions.
- **Mockito** — mocks, stubbing, argument capture and interaction verification.
- **Spring Boot Test** — Spring context and integration tests.
- **Spring Security Test** — security-related testing support.
- **H2 Database** — in-memory test database.
- **MockMultipartFile** — simulated multipart file uploads.
- **Spring MVC test utilities** — model and web-layer test support.

Unit tests isolate the class under test and mock its external dependencies where appropriate. Integration tests deliberately use real Spring-managed services/repositories and the H2 test configuration to check selected persistence flows.

The test configuration is located at `src/test/resources/application-test.properties`:

- JDBC URL: in-memory H2 database (`jdbc:h2:mem:testdb`)
- Hibernate schema lifecycle: `create-drop`
- Flyway: disabled for the test profile
- SQL logging: disabled

The test profile is activated explicitly by the three integration-test classes.

## 4. Running the Tests

Run commands from the repository root, where `pom.xml` and `mvnw.cmd` are located.

### Windows PowerShell

Run the complete suite:

```powershell
.\mvnw.cmd test
```

Run a clean test build:

```powershell
.\mvnw.cmd clean test
```

Run one unit-test class:

```powershell
.\mvnw.cmd -Dtest=ReconciliationServiceTest test
```

Run one integration-test class:

```powershell
.\mvnw.cmd -Dtest=IngestionIntegrationTest test
```

Other useful targeted commands:

```powershell
.\mvnw.cmd -Dtest=WorkflowServiceImplTest test
.\mvnw.cmd -Dtest=TaxConfigurationIntegrationTest test
.\mvnw.cmd -Dtest=WorkflowIntegrationTest test
```

On Linux/macOS, use `./mvnw` instead of `.\mvnw.cmd`.

A successful run should finish with Maven reporting `BUILD SUCCESS`. Record the actual final test count from that run rather than relying on an earlier IDE screenshot or a source-file count.

## 5. Results and Evidence

### Source inventory

Inspection of the uploaded branch snapshot found:

- 39 Java test classes.
- 36 unit-test classes and 3 integration-test classes.
- 278 `@Test` declarations and 2 `@ParameterizedTest` declarations (280 declared test methods/annotations overall).

These figures describe the checked-in test sources. They are not the same as the number of executed test invocations, because parameterized tests may run multiple times.

### Execution evidence available during this review

A prior IDE screenshot shared during development showed **200 tests passed** in that particular run. A later screenshot showed `IngestionIntegrationTest` passing after the test database configuration and trimmed CSV-value expectation were corrected. A focused `AdminTaxConfigurationControllerTest` run was also reported passing after the test was updated to supply `BindingResult`.

These are separate results from earlier runs. They do **not** establish that all 280 declared test methods or all three integration tests pass together on the exact snapshot represented by this ZIP.

During this review, a fresh Maven run could not be completed because the Maven wrapper was unable to download its Maven distribution from Maven Central in the available environment. Therefore, this README intentionally does not claim a fresh full-suite pass.

**Before final submission:** run `.\mvnw.cmd clean test` in your development environment and update this section with the actual summary from the final run.

## 6. Known Limitations and Notes

- No JaCoCo coverage percentage is reported; coverage instrumentation is not required for this branch's stated testing approach.
- A source inventory cannot prove that every business-rule branch is covered. The test suite should be maintained as production logic evolves.
- Unit tests with mocked repositories verify the unit's behaviour and interactions; they do not independently prove database query or schema correctness.
- The three integration tests cover selected happy-path lifecycle flows. They do not replace comprehensive integration testing for every module.
- `SimulatedPaymentGatewayServiceTest` exercises a simulated gateway. It must not be described as testing real payment processing.
- The test package/directory inconsistencies described in Section 2 are cleanup candidates and should only be changed with a subsequent full test run.
- Do not commit production credentials or secrets in test configuration.

## 7. Requirements and Project Traceability

Use this test branch together with the project documents already maintained by Group A:

- `CSC3213_21_Group_A_Project_Charter.pdf` — project scope and boundaries.
- `Group_A_SRS.pdf` — software requirements.
- `Group_A_RTM_v1.pdf` — requirements traceability matrix.
- `Group_A_WBS_v1.pdf` — work breakdown and planned activities.
- `SPRINT_4_TESTING_PLAN.md` — testing sprint goal, approach, standards, and definition of done.

Map requirements to test classes/methods through the RTM. Requirement IDs are intentionally not listed here because they have not been verified against the RTM in this branch snapshot.

## 8. Branch Context and Definition of Done

This branch is the team's testing branch in the same FRWSS repository. It should be integrated using the team's agreed Git workflow; it should not be deployed or treated as a separate product.

Before declaring this testing work complete:

1. Run `.\mvnw.cmd clean test` from the repository root.
2. Confirm the full suite finishes with `BUILD SUCCESS`.
3. Record the actual executed test count and any skipped tests.
4. Review failures and resolve or document them.
5. Ensure only intended test, test-configuration, and documentation changes are committed.
6. Retain the final Maven output or IDE test-run screenshot as evidence.
