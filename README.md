# FRWSS Security Module

**Project:** Financial Reconciliation and Workflow Support System (FRWSS)  
**Branch:** `feature/security-Nikhila`  
**Contributor:** Nikhila Eranda (S/21/348)

## Overview

The security module provides the application's foundation for user authentication and access control. It includes the initial user and role model, persistence repositories, Spring Security configuration, login processing, and user-facing login and access-denied pages.

The module is part of FRWSS, a Java web application built with Spring Boot. This README describes the security scope only.

## Security features

- User and role entities for representing application identities and roles.
- Repository components for accessing user and role data.
- Spring Security configuration for authentication and access control.
- Authentication controller and login-processing functionality.
- Login and access-denied interfaces.

## Technology

- Java 17
- Spring Boot 4.0.5
- Spring Security
- Spring MVC and Thymeleaf
- Spring Data JPA
- PostgreSQL
- Maven Wrapper

## Run locally

### Prerequisites

- JDK 17 or later
- PostgreSQL

Clone the repository and switch to the security branch:

```bash
git clone https://github.com/shamilaw-sci/CSC3213_20_Group_A.git
cd CSC3213_20_Group_A
git checkout feature/security-Nikhila
```

Configure the application's database connection using the supported local settings. Keep credentials and other secrets out of source control.

Start the application from the repository root.

**Windows PowerShell:**

```powershell
.\mvnw.cmd spring-boot:run
```

**macOS/Linux:**

```bash
./mvnw spring-boot:run
```

Open the local address printed by Spring Boot.

## Security testing

Manual security testing for this module covers:

- Login behaviour with valid and invalid credentials.
- Authentication handling when credentials are missing or incorrect.
- Access-control behaviour for protected pages and operations.
- Login and access-denied page rendering and navigation.
- Reporting of functional or usability issues discovered during testing.

Record the environment, test account/role, steps, expected result, actual result, and evidence when executing these checks. This README does not claim that a particular test run passed.

## Project files

The security implementation resides in the application's Java source and resources under `src/main`. The Maven dependency and build configuration is in `pom.xml`; the Maven Wrapper scripts are `mvnw` and `mvnw.cmd`.

## Development note

Use test accounts and synthetic data in local development. Do not commit passwords, database credentials, tokens, or other secrets.


