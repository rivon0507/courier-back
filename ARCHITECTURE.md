# Architecture — Courier Backend

This document describes the **code organization and architectural conventions**
of the Courier Spring Boot backend.

For product overview and features, see [README.md](README.md).

---

## Table of contents

<!-- TOC -->
* [Architecture — Courier Backend](#architecture--courier-backend)
  * [Table of contents](#table-of-contents)
  * [1) High-level structure](#1-high-level-structure)
  * [2) Feature package structure](#2-feature-package-structure)
    * [Responsibilities](#responsibilities)
  * [3) DTO & mapping conventions](#3-dto--mapping-conventions)
  * [4) Shared web contracts](#4-shared-web-contracts)
  * [5) Security](#5-security)
  * [6) Persistence & migrations](#6-persistence--migrations)
  * [7) Naming & component scanning](#7-naming--component-scanning)
<!-- TOC -->

---

## 1) High-level structure

The codebase uses a **feature-first** package structure.

```

io.github.rivon0507.courier
├── CourierApplication.java
├── common
│   └── web
│       ├── error
│       └── pagination
├── security
├── envoi
├── reception
└── settings

```

---

## 2) Feature package structure

Each feature (`envoi`, `reception`, `settings`, …) follows the same internal layout:

```

<feature>
├── web          # REST controllers (HTTP entrypoints)
├── dto          # Feature DTOs (used by controller + service)
├── application  # Services / use-cases (transaction boundary)
├── domain       # Entities and domain rules
└── persistence  # Spring Data repositories
```

### Responsibilities

* **web**: routing, request/response handling, HTTP concerns
* **dto**: input/output models owned by the feature
* **application**: business logic and orchestration
* **domain**: core entities and invariants
* **persistence**: database access only

---

## 3) DTO & mapping conventions

* Feature DTOs live in `<feature>.dto`
* Services are allowed to use feature DTOs directly
* Mapping from entity to DTO should use mapstruct
* Mapper classes are put in `application` (e.g., `EnvoiMapper`).

DTOs may use Jakarta Validation annotations.

---

## 4) Shared web contracts

Global HTTP contracts are centralized:

```
common.web.error       # error response models + exception handler
common.web.pagination  # pagination response models
```

Feature DTOs must not contain error or pagination models.

---

## 5) Security

Security is cross-cutting and lives in:

```
io.github.rivon0507.courier.security
```

This includes:

* security configuration
* JWT utilities
* authentication filters (if any)

Security logic must not be duplicated inside feature packages.

---

## 6) Persistence & migrations

* PostgreSQL with Spring Data JPA
* Schema changes are handled via **Flyway** migrations
* Hibernate schema generation is not relied on in production

Shared domain concepts (e.g. attachment metadata) live in a shared domain package
when used by multiple features.

---

## 7) Naming & component scanning

* All code lives under the base package:

  ```
  io.github.rivon0507.courier
  ```

---
