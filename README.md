# Courier — Company Mail Management Backend

Courier is a **Spring Boot backend application for managing company mail (courrier)** in the French administrative
sense.

It handles **incoming and outgoing mail**, attachments metadata, and administrative workflows, with a clean, modular
architecture designed for long-term evolution.

> This is **not** a logistics or shipping platform.  
> Courier focuses on **administrative mail registration, traceability, and documentation**.

---

## Table of contents

<!-- TOC -->
* [Courier — Company Mail Management Backend](#courier--company-mail-management-backend)
  * [Table of contents](#table-of-contents)
  * [✨ Features](#-features)
  * [🏗️ Technical overview](#-technical-overview)
  * [🧠 Architectural principles](#-architectural-principles)
  * [🚀 Project status](#-project-status)
  * [📁 Repository structure (simplified)](#-repository-structure-simplified)
  * [📚 Documentation](#-documentation)
  * [👤 Author](#-author)
<!-- TOC -->

---

## ✨ Features

- 📥 **Incoming mail (Réception)**
    - Register received mail
    - Attach metadata (pièces jointes)
    - History tracking

- 📤 **Outgoing mail (Envoi)**
    - Register outgoing mail
    - Recipients and references
    - Attachment metadata

- 🧾 **Administrative documents**
    - Designed to support PDF bordereaux / registers
    - Company settings reusable across documents

- 🔐 **Security**
    - JWT-based authentication
    - Secure-by-default endpoints
    - Centralized security configuration

- 🧩 **Clean architecture**
    - Feature-first modular structure
    - Clear separation between web, application, domain, and persistence
    - Designed for future extensions (desktop/offline clients, batch jobs, etc.)

---

## 🏗️ Technical overview

- **Language**: Java
- **Framework**: Spring Boot
- **Persistence**: Spring Data JPA + Hibernate
- **Database**: PostgreSQL
- **Migrations**: Flyway
- **Security**: Spring Security (JWT) and OAuth2 Resource Server
- **Validation**: Jakarta Bean Validation
- **Observability**: Spring Boot Actuator
- **Testing**: JUnit 5, Testcontainers (PostgreSQL)

---

## 🧠 Architectural principles

- **Feature-first packaging** (`envoi`, `reception`, `settings`, …)
- DTOs owned by features, not tied to HTTP-only packages
- Boring controllers, business logic in services
- Explicit error models and pagination contracts

The architecture is intentionally **pragmatic**: clean boundaries where they matter, without unnecessary ceremony.

---

## 🚀 Project status

This project is actively developed as part of a **portfolio-grade backend** showcasing:

- real-world Spring Boot practices
- clean layering
- database migrations
- security configuration
- testable design

---

## 📁 Repository structure (simplified)

```

io.github.rivon0507.courier
├── common
├── security
├── envoi
├── reception
├── settings
└── CourierApplication.java

```

For detailed architectural conventions, see: [ARCHITECTURE.md](ARCHITECTURE.md)


---

## 📚 Documentation

- 📄 **Setup & development instructions**  
  See: [SETUP.md](SETUP.md)

- 🏗️ **Architecture & code organization**  
  See: [ARCHITECTURE.md](ARCHITECTURE.md)

---

## 👤 Author

**Flavien TSIRIHERIVONJY**  
Software Engineering & Databases  
Java / Spring Boot Backend Developer