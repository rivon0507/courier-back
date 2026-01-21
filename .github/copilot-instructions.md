# GitHub Copilot Instructions — courier-back (Spring Boot)

You are assisting on **courier-back**, a Spring Boot backend for a **company mail (“courrier”) management system** (
French administrative sense).
This is **not** a logistics/shipping platform.

Goal: generate code that is **clean, testable, production-minded**, and consistent with the conventions below.

---

## 1) Domain context

The app manages **incoming and outgoing company mail**:

- **Envoi (Outgoing mail / courrier sortant)**: register outgoing mail, recipient info, references, attachments
  metadata (“pièces jointes”), observation (subject).
- **Réception (Incoming mail / courrier entrant)**: register received mail, source, references, attachments metadata.
- **Settings**: organization/company configuration used in documents and defaults.
- **PDF Bordereau / Registers**: administrative documents generated from courrier data (keep design open).
- **Audit & traceability**: timestamps, status transitions, history.

Avoid assumptions about parcel delivery, routing, or carrier integrations.

---

## 2) API versioning

- **No `/v1`** in paths or context-path.
- Endpoints should live under `/api` (or whatever is configured), but do **not** introduce version segments.

---

## 3) Architecture & packages (feature-first)

Use a **feature-first** package structure with shared cross-cutting packages.

Example shape (names may vary, follow existing repo conventions):

```

io.github.rivon0507.courier
├── common
│   └── web
│       ├── error
│       └── pagination
├── security
├── envoi
├── reception
├── settings
└── auth (if separated)

```

### Inside a feature package (envoi/reception/settings)

Use these subpackages (only create what’s needed):

- `web` — controllers (HTTP entrypoints)
- `dto` — request/response DTOs for the feature (shared by controller + service)
- `application` — services / use-cases (transaction boundary)
- `domain` — entities + domain rules
- `persistence` — repositories (Spring Data JPA)

**DTO rule (important):**

- Feature DTOs live in `<feature>.dto` (NOT inside `web`).
- Services are allowed to use these feature DTOs directly (pragmatic choice).

**Mapping rule:**

- Mapping from entity to DTO should use mapstruct where possible
- Mapper classes are put in `application` (e.g., `EnvoiMapper`), not in `web`.

---

## 4) Naming conventions (beans & discovery)

Ensure all code stays under the base package of the `@SpringBootApplication` class.

---

## 5) Cross-cutting concerns placement

### Security

Place security infrastructure in `io.github.rivon0507.courier.security`:

- `SecurityConfiguration` (`@Configuration`)
- `AuthFilter` (if custom filter is used)
- JWT utilities/services

Security applies across features; do not place these in `envoi` or `reception`.

### Global error model

Error response records belong to `common.web.error` (not feature DTOs):

- `ValidationErrorResponse` / `FieldViolation` (records)
- `GlobalExceptionHandler` (`@RestControllerAdvice`)

### Pagination models

Pagination records belong to `common.web.pagination`:

- `PageResponse<T>` (record)
- `PageMeta` (record)

Do not return Spring’s `Page<T>` directly as the public contract unless the repo already does so consistently.

---

## 6) Lombok usage

Lombok is allowed but bounded:

- ✅ Use `@Slf4j` for logging in classes that log.
- ⚠️ Use other Lombok annotations only if already established in the repo style.
- ❌ Avoid `@Data` on entities and heavy Lombok patterns that hide behavior.

---

## 7) Persistence & migrations (Flyway)

- All schema changes must come with **Flyway** migration scripts in `classpath:db/migration`.
- Use incremental versioned migrations: `V###__description.sql`.
- Do not rely on Hibernate `ddl-auto` for production schema evolution.
- Add appropriate indexes for foreign keys and frequently queried fields.

### Shared “piece/attachment” concept

If “Piece” (attachment metadata) is shared by envoi and reception:

- Prefer a shared domain type under `common.domain` (e.g., `Attachment` / `PieceJointe`) or a dedicated feature if it
  has its own lifecycle.
- Avoid duplicating the same entity in multiple features unless semantics differ.

---

## 8) Service & transaction rules

- Controllers call services; services call repositories.
- Keep transactional boundaries in the **application/service layer**:
    - annotate service methods with `@Transactional` when needed.
- Repositories must remain persistence-only (no business logic).

---

## 9) REST API rules

- Use noun-based resources, predictable URLs, and correct status codes:
    - `POST` create → `201 Created`
    - `GET` read → `200 OK`
    - validation errors → `400`
    - not found → `404`
    - conflict / invalid state transition → `409`

- Prefer explicit endpoints for state transitions if modeled:
    - e.g., `POST /api/envois/{id}/status` (only if it matches your domain model)

- Use Jakarta Validation on request DTOs (`@NotNull`, `@NotBlank`, `@Size`, etc.).
- Never expose stack traces or raw persistence exceptions to clients.

---

## 10) Error handling

Use a single global exception handler (`@RestControllerAdvice`) that returns consistent error payloads.

Validation errors should include:

- general message
- per-field violations (field + message)

Keep error response records in `common.web.error`.

---

## 11) Logging & observability

- Use SLF4J (via Lombok `@Slf4j` when logging is needed).
- Never log secrets, tokens, or passwords.
- Actuator endpoints should remain configurable per environment.

---

## 12) Testing expectations

When implementing features, generate tests:

- Integration tests where persistence or web wiring matters:
    - Spring Boot tests + Testcontainers PostgreSQL
- Unit tests for pure business rules (JUnit 5, Mockito if needed)

Tests should be deterministic and avoid brittle JSON assertions unless necessary.

---

## 13) What NOT to do

- Don’t introduce `/v1` or other version segments.
- Don’t introduce logistics/shipping assumptions (routing, carriers, parcel tracking).
- Don’t introduce new frameworks or architectural overhauls without explicit request.
- Don’t hardcode environment-specific values (DB URLs, secrets, ports).

---

## 14) When unsure

- Follow existing patterns in the repo (naming, packages, DTO style).
- Prefer minimal, localized changes.
- Keep controllers “boring” (headers, status, mapping calls), business logic in services.

---
End of instructions.