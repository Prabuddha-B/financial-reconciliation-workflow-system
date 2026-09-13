# Sprint 4 – Automated Testing & Quality Assurance

> **Project:** Financial Reconciliation and Workflow Support System (FRWSS)  
> **Sprint:** Sprint 4  
> **Branch:** `testing`  
> **Base Branch:** `main`  
> **Sprint Status:** Planning

---

# Sprint Goal

Establish a structured and professional automated testing process for the Financial Reconciliation and Workflow Support System (FRWSS). The objective of this sprint is to verify the correctness of the implemented business logic, improve software quality, ensure system stability, and prepare the project for final evaluation through comprehensive unit testing.

---

# Sprint Objectives

- Verify the correctness of implemented business logic.
- Build a maintainable automated unit testing suite.
- Improve software quality and reliability.
- Detect regressions before merging into the main branch.
- Increase confidence in future modifications.
- Produce evidence of software verification for the final project evaluation.
- Improve overall code quality without introducing new features.

---

# Sprint Scope

Sprint 4 focuses exclusively on testing activities.

Included:

- Unit Testing
- Mocking external dependencies
- Improving testability
- Minor refactoring required for testing
- Bug fixes discovered during testing
- Code coverage reporting
- Testing documentation

Excluded:

- New Features
- UI Redesign
- Database schema changes
- Performance optimization
- Major refactoring unrelated to testing

---

# Branching Strategy

```
main
│
└── testing
      │
      ├── testing/member1
      ├── testing/member2
      ├── testing/member3
      └── testing/member4
```

### Branch Responsibilities

| Branch          | Purpose                                  |
|-----------------|------------------------------------------|
| testing         | Sprint 4 integration branch              |
| testing/member1 | Unit testing for Member 1 implementation |
| testing/member2 | Unit testing for Member 2 implementation |
| testing/member3 | Unit testing for Member 3 implementation |
| testing/member4 | Unit testing for Member 4 implementation |

Each developer branch will only contain testing-related work.

---

# Testing Standards

The following principles must be followed throughout Sprint 4.

## 1. Atomic Development

Only one class should be tested at a time.

Example:

WorkflowServiceImpl

↓

Write Tests

↓

Run Tests

↓

Refactor (if required)

↓

Commit

---

## 2. Business Logic First

Priority should always be given to testing:

- Business Rules
- Validation Logic
- Workflow Logic
- Security Decisions
- Calculations
- Audit Behaviour

---

## 3. No Coverage Chasing

Coverage percentage is not the primary goal.

Meaningful tests are preferred over high percentages.

---

## 4. Mock External Dependencies

Repositories

Services

Security Context

HTTP Session

MultipartFile

Model

Audit Services

External Services

should always be mocked.

---

## 5. One Responsibility Per Test

Each unit test should verify only one behaviour.

---

# Test Naming Convention

Test Class

```
<ClassName>Test
```

Example

```
WorkflowServiceImplTest
```

Test Method

```
method_condition_expectedResult()
```

Example

```
updateWorkflowState_validTransition_updatesWorkflow()

updateWorkflowState_invalidTransition_throwsException()
```

---

# Testing Workflow

For every class:

Inspect Class

↓

Review Business Rules

↓

Identify Dependencies

↓

Prepare Test Inventory

↓

Implement Unit Tests

↓

Run Tests

↓

Review Coverage

↓

Commit

↓

Merge into testing

---

# Definition of Done (DoD)

A class is considered complete only if:

- All planned unit tests are implemented.
- All tests pass.
- No existing functionality is broken.
- External dependencies are mocked.
- No unnecessary refactoring has been introduced.
- Code follows project coding standards.
- Changes are committed with meaningful commit messages.

---

# Commit Message Convention

Examples:

```
test: add WorkflowServiceImpl unit tests

test: add WorkflowTransitionValidator tests

test: add TaxEstimationService tests

refactor: improve WorkflowServiceImpl testability

docs: update Sprint 4 testing progress
```

---

# Module Testing Order

| Priority | Module | Status |
|----------|----------|--------|
| 1 | Prabuddha Contribution | Pending |
| 2 | Member 2 Contribution | Pending |
| 3 | Member 3 Contribution | Pending |
| 4 | Member 4 Contribution | Pending |

---
# Coverage Goals

| Component | Target |
|------------|---------|
| Service Layer | 90% |
| Validation | 100% |
| Controllers | 80% |
| Security Helpers | 90% |
| Overall Project | 85%+ |

Coverage is used as an indicator only.

Business correctness takes priority.

---

# Sprint Deliverables

- Sprint 4 Testing Plan
- Unit Test Inventory
- Automated Unit Tests
- Mock-based Test Suite
- Coverage Report
- Updated Documentation
- Final Testing Summary

---

# Notes

- Sprint 4 introduces no new application features.
- Only testing, quality assurance, and stability improvements are permitted.
- Any bugs discovered during testing must be fixed before merging into the `testing` branch.
- All testing activities should remain traceable through Git history.