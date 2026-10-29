# FRWSS Testing Contribution — `testing-Prabuddha`

## 1. Branch Purpose

`testing-Prabuddha` is a development branch within the **Financial Reconciliation and Workflow Support System (FRWSS)** repository. It consolidates unit-test work, additional domain/data-class tests, and integration-test scenarios for the existing application.

This branch is **not a separate application**. Its purpose is to improve confidence in existing FRWSS functionality through automated tests. The system remains a reconciliation and workflow layer; tests do not execute real payments or transfers.

## 2. Individual Contribution

The testing work on this branch includes the following areas.

### Unit tests

- **Manual entry:** `ManualEntryControllerTest`
- **Ingestion:** `IngestionServiceTest`, `IngestionControllerTest`
- **Workflow:** `WorkflowServiceImplTest`, `WorkflowControllerTest`, `WorkflowViewControllerTest`
- **Tax:** `TaxConfigurationServiceTest`, `TaxEstimationServiceTest`, `AdminTaxConfigurationControllerTest`, `TaxEstimationControllerTest`
- **Security:** `SecurityContextAdapterTest`
- **Model advice:** `GlobalModelAttributesAdviceTest`
- **Entities and domain models:** `AccountingRecordTest`, `StockPurchaseTest`, `UserTest`, `TaxConfigurationTest`, `WorkflowRecordTest`, `WorkflowTransitionLogTest`
- **DTOs:** `ManualEntryFormTest`, `WorkflowRequestDtoTest`, `WorkflowResponseDtoTest`, `WorkflowHistoryResponseDtoTest`, `WorkflowStateUpdateRequestDtoTest`, `TaxEstimationRequestDtoTest`, `TaxEstimationResponseDtoTest`
- **Repositories:** `UserRepositoryTest`, `TaxConfigurationRepositoryTest`, `WorkflowRecordRepositoryTest`, `WorkflowTransitionLogRepositoryTest`
- **Enums and exceptions:** `WorkflowStateTest`, `WorkflowNotFoundExceptionTest`, `InvalidWorkflowTransitionExceptionTest`
- **Application bootstrap:** `FrwssSystemApplicationTest`

Some test classes already existed and were extended. This list identifies the test classes involved in this contribution and does not imply that every class was written entirely from scratch.

### Integration tests

- `IngestionIntegrationTest` — exercises receipt CSV preview/validation and persistence through the real ingestion service and repository.
- `WorkflowIntegrationTest` — covers workflow operations and persistence.
- `TaxConfigurationIntegrationTest` — covers tax-configuration operations and persistence.

The ingestion integration test was observed passing in the IDE. The workflow and tax integration tests were added, but their final combined result should be confirmed by running the complete test suite.

## 3. Testing Approach and Tools

The tests use the project's Java/Spring testing stack:

- **JUnit 5** for test cases and assertions.
- **Mockito** for isolating units of code and verifying interactions with dependencies.
- **Spring Boot Test** for application-context and integration testing.
- **MockMultipartFile** for simulating uploaded CSV files in ingestion tests.
- **Spring test utilities**, such as `ConcurrentModel`, for controller unit tests.
- **Test database configuration** in `src/test/resources/application-test.properties` for tests that require a Spring application context and database.

Unit tests focus on individual methods, validation rules, branches, returned values, and dependency interactions. Integration tests exercise multiple application components together, including persistence where configured.

## 4. How to Run the Tests

Run commands from the repository root, where `pom.xml` and the Maven wrapper are located.

### Run the complete test suite (Windows PowerShell)

```powershell
.\mvnw.cmd test
```

### Clean and run the complete test suite

```powershell
.\mvnw.cmd clean test
```

### Run a single test class

For example:

```powershell
.\mvnw.cmd -Dtest=ManualEntryControllerTest test
```

Run an integration test individually:

```powershell
.\mvnw.cmd -Dtest=IngestionIntegrationTest test
```

On other operating systems, use `./mvnw test`.

If an integration test fails during Spring context startup, check the active test profile, `src/test/resources/application-test.properties`, database configuration, and test dependencies in `pom.xml`. Do not put production credentials in test configuration or commit secrets.

## 5. Results and Evidence

The following results were observed during development:

- The IDE reported **200 tests passed** in one unit-test run.
- `IngestionIntegrationTest` subsequently reported **1 test passed** after the test database configuration and CSV trimming expectation were corrected.
- A focused run of `AdminTaxConfigurationControllerTest` passed after its calls were updated to match the controller's three-parameter method signature, including `BindingResult`.

These are results from separate runs, not a claim that all tests were executed together in one final run. Record the final combined test count and outcome from the latest successful ` .\mvnw.cmd clean test` run on the branch being assessed.

### Known limitations

- The available development evidence does not establish that all three integration tests pass together after every merge.
- No JaCoCo coverage percentage is reported; coverage instrumentation was intentionally not used.
- Tests using mocked repositories verify interaction with those dependencies; database-backed tests are needed to verify actual persistence.
- `IngestionControllerTest` is currently located under the existing misspelled test package directory `contoller`. If the package is reorganized, update the declaration and path together and rerun the suite.

## 6. Traceability

Review this testing work alongside the project's existing requirements and planning documents:

- **Project scope:** `CSC3213_21_Group_A_Project_Charter.pdf`
- **Software requirements:** `Group_A_SRS.pdf`
- **Requirements traceability:** `Group_A_RTM_v1.pdf`
- **Work breakdown and planned activities:** `Group_A_WBS_v1.pdf`

Use the RTM to map each tested requirement to its test class or test method. Requirement IDs are not reproduced here because they have not been verified against the current RTM. If the repository or submission contains a test plan or test evidence document, add a link using its actual path.

## 7. Branch Context

This README documents the testing contribution associated with `testing-Prabuddha`. The branch belongs to the same FRWSS Git repository; it is not a separate application.

Before submitting or merging this branch:

1. Run `.\mvnw.cmd clean test`.
2. Confirm the final test count and that the build reports `BUILD SUCCESS`.
3. Review `git status` and commit the intended test files and test configuration.
4. Save the final test output or IDE screenshot as submission evidence.
