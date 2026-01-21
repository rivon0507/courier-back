# Git Commit Instructions (for Copilot)

These rules define how to write commit messages in this repository.
When generating commit messages, follow them strictly.

## 1) Format

Use this exact format:

`<type>(<scope>): <subject>`

Optional body:

`<body>`

Optional footer(s):

`<footer>`

### Examples
- feat(auth): add refresh token rotation
- fix(envoi): prevent null reference when printing bordereau
- ci(github): run integration tests with Testcontainers
- docs(readme): add local setup instructions
- chore(deps): bump Spring Boot to 3.4.x

## 2) Allowed types

Use ONLY these types:

- feat: new user-facing feature
- fix: bug fix (user-visible)
- docs: documentation-only changes
- style: formatting only (no logic changes) — e.g., whitespace, lint formatting
- refactor: code change that neither fixes a bug nor adds a feature
- perf: performance improvements
- test: add or change tests only
- build: build system changes (Gradle/Maven/NPM), build scripts, docker build
- ci: CI pipeline changes (GitHub Actions, workflows)
- chore: maintenance tasks that don't fit above (tooling, cleanup, misc)

Notes:
- Prefer `ci` for workflow changes and `build` for build tooling.
- Prefer `chore` for repo maintenance not tied to runtime behavior.

## 3) Scope

Scope is required and should be short and meaningful.
Use one of:
- a feature/module: auth, envoi, reception, settings, pdf, users
- a layer/package: api, domain, infra, persistence, web, security
- tooling areas: deps, docker, github, gradle, npm

### Scope rules
- Use lowercase.
- Use hyphens if needed: `api-client`, `pdf-bordereau`.
- If truly cross-cutting, use a broad scope like `core` or `platform`.

## 4) Subject line rules

The subject must:
- be in **imperative** mood (e.g., "add", "fix", "remove", "refactor")
- be **present tense**
- NOT end with a period `.`
- be concise (aim <= 72 chars)
- describe *what* changed, not *how* you felt

Good:
- feat(auth): add JWT authentication filter
Bad:
- feat(auth): added JWT authentication filter.
- fix: fixed stuff
- chore: updates

## 5) Body rules (when needed)

Add a body when:
- the change is non-trivial
- you need to explain reasoning, constraints, or tradeoffs
- you want to mention behavior changes

Body formatting:
- Wrap lines ~72-100 chars
- Use bullet points if it helps readability
- Explain *why* and *what*, not low-level diff details

Example body:

refactor(envoi): split validation into dedicated service

- Moves validation rules out of controller layer
- Keeps error responses unchanged
- Prepares for reuse in offline-first adapter

## 6) Breaking changes

If a change breaks compatibility, mark it clearly:

- Add `!` after type or scope, and
- Add a `BREAKING CHANGE:` footer explaining impact/migration

Example:
feat(api)!: rename /envoi endpoints

BREAKING CHANGE: `/api/v1/envoi` is now `/api/v1/shipments`. Update clients.

## 7) Issue/PR linking (footer)

If you want to reference issues:

- Use footer keywords:
    - `Refs: #123`
    - `Closes: #123` (only if the commit fully resolves it)

Examples:
- fix(pdf): handle missing logo
  Refs: #88

- feat(settings): add company profile screen
  Closes: #42

## 8) Quick decision guide for type

- New capability for users? -> feat
- Bug fix? -> fix
- Only docs? -> docs
- Only formatting? -> style
- Restructure without behavior change? -> refactor
- Faster/more efficient? -> perf
- Tests only? -> test
- Gradle/NPM/Docker build config? -> build
- GitHub Actions / CI config? -> ci
- Cleanup/tooling/misc? -> chore

## 10) More examples

- feat(envoi): add create shipment endpoint
- fix(security): reject expired JWT tokens
- refactor(domain): extract Address value object
- test(auth): add refresh token integration tests
- perf(persistence): optimize shipment search query
- build(docker): add multi-stage build for backend
- ci(github): cache Gradle dependencies
- style(web): run formatter on controllers
- docs(api): document error response format
- chore(deps): update Flyway and Postgres driver
