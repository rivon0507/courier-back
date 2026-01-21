# Courier Backend — Setup & Development Guide

This document explains how to **set up, run, and test** the Courier Spring Boot backend locally.

It’s meant to be practical: clone → configure → run.

---

## Table of contents

<!-- TOC -->
* [Courier Backend — Setup & Development Guide](#courier-backend--setup--development-guide)
  * [Table of contents](#table-of-contents)
  * [1) Prerequisites](#1-prerequisites)
  * [2) Clone the repository](#2-clone-the-repository)
  * [3) Environment configuration (`.env`)](#3-environment-configuration-env)
  * [4) Database setup (recommended: Docker Compose)](#4-database-setup-recommended-docker-compose)
    * [Option A — Docker Compose (recommended)](#option-a--docker-compose-recommended)
    * [Option B — Manual Docker run (only if you prefer)](#option-b--manual-docker-run-only-if-you-prefer)
  * [5) Run the application](#5-run-the-application)
  * [6) Actuator & health checks](#6-actuator--health-checks)
  * [7) Running tests](#7-running-tests)
  * [8) Common issues](#8-common-issues)
    * [Docker not running (tests fail)](#docker-not-running-tests-fail)
    * [Database connection refused](#database-connection-refused)
  * [9) Useful commands (quick reference)](#9-useful-commands-quick-reference)
<!-- TOC -->

---

## 1) Prerequisites

Install:

- **Java 21**
- **Docker**
  - Required for PostgreSQL (local dev) and Testcontainers (tests)
- **Git**
- **Docker Compose** (recommended)

Verify:

```bash
java -version
docker --version
docker compose version
````

---

## 2) Clone the repository

```bash
git clone https://github.com/rivon0507/courier-back.git
cd courier-back
```

---

## 3) Environment configuration (`.env`)

This project uses a versioned `.env.example`.
For local development, create your `.env` by copying it:

```bash
cp .env.example .env
```

Then edit `.env` values to match your environment.

---

## 4) Database setup (recommended: Docker Compose)

### Option A — Docker Compose (recommended)

Start PostgreSQL (and any other services defined):

```bash
docker compose up -d
```

Then confirm your `.env` matches the Compose DB settings (host, port, db name, user, password).

### Option B — Manual Docker run (only if you prefer)

If you start PostgreSQL manually, **make sure your `.env` points to the correct DB**.

Example:

```bash
docker run --name courier-postgres \
  -e POSTGRES_DB=courier \
  -e POSTGRES_USER=courier \
  -e POSTGRES_PASSWORD=courier \
  -p 5432:5432 \
  -d postgres:16
```

And your `.env` must match, e.g.:

```env
DB_URL=jdbc:postgresql://localhost:5432/courier
DB_USERNAME=courier
DB_PASSWORD=courier
```

---

## 5) Run the application

Using Gradle:

```bash
./gradlew bootRun
```

Or build and run the JAR:

```bash
./gradlew build
java -jar build/libs/*.jar
```

Default URL:

```
http://localhost:8080
```

(Port may differ depending on configuration.)

Flyway migrations run automatically at startup.

---

## 6) Actuator & health checks

If Actuator is enabled and exposed, health endpoints are typically:

```
/actuator/health
```

Exposure depends on configuration.

---

## 7) Running tests

Run all tests:

```bash
./gradlew test
```

Notes:

* Integration tests use **Testcontainers**
* Docker must be running
* PostgreSQL containers are started automatically for tests

---

## 8) Common issues

### Docker not running (tests fail)

If you see errors like:

```text
Could not find a valid Docker environment
```

Start Docker and retry.

---

### Database connection refused

Check:

* DB container is running (`docker ps`)
* `.env` values match the running DB (host/port/db/user/password)
* You didn’t start a second DB on a different port

---

## 9) Useful commands (quick reference)

```bash
cp .env.example .env   # create local env file
docker compose up -d   # start local services
./gradlew bootRun      # run app
./gradlew test         # run tests
./gradlew build        # build jar
docker ps              # list running containers
```

---
