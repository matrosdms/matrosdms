# 🛠️ Tech Stack & Architecture

MatrosDMS is built on a modern, robust Jakarta EE and Cloud-Native stack, designed for performance, maintainability, and security.

## 🏗️ Backend (Server)

The backend is a monolithic Spring Boot application designed for high throughput and reliability.

|         Component         |        Technology        |                              Description                              |
|---------------------------|--------------------------|-----------------------------------------------------------------------|
| **Core Framework**        | **Java 25 (LTS)**        | Leveraging Virtual Threads and Foreign Function API.                  |
| **Application Framework** | **Spring Boot 3.5.10**   | DI, Web, Security, Actuator.                                          |
| **Search Engine**         | **Hibernate Search 7**   | Lucene-based full-text indexing (No external OpenSearch/ES required). |
| **Database**              | **H2 (Embedded)**        | Lightweight, file-based SQL database. Zero setup.                     |
| **Migration**             | **Flyway**               | Automatic DB schema versioning and migration.                         |
| **Content Extraction**    | **Apache Tika**          | Text extraction from PDF, Office, Images.                             |
| **OCR**                   | **Tesseract (via Tika)** | Optical Character Recognition for scanned assets.                     |
| **Email**                 | **Apache James**         | Protocol implementations for embedded SMTP/IMAP servers.              |
| **Security**              | **Bouncy Castle**        | Argon2id Key Derivation, AES-256-CTR Encryption.                      |
| **Scheduling**            | **DbScheduler**          | Persistent task scheduling independent of application state.          |

### Key Architectural Decisions

- **Local-First**: No cloud dependencies. All data (DB, Index, Files) lives in a single defined `MATROS_DATA_DIR`.
- **Encrypted Storage**: Files are encrypted at rest using a per-installation salt and user password.
- **Embedded Services**: Email and Database servers are embedded to simplify deployment.

---

## 🎨 Frontend (UI)

The frontend is a Single Page Application (SPA) providing a desktop-class experience in the browser.

|      Component       |     Technology     |                    Description                     |
|----------------------|--------------------|----------------------------------------------------|
| **Framework**        | **Vue 3**          | Using Composition API and `<script setup>`.        |
| **Language**         | **TypeScript**     | Strict typing for robustness.                      |
| **Build Tool**       | **Vite**           | Extremely fast hot-reload (HMR) and bundling.      |
| **Styling**          | **Tailwind CSS**   | Utility-first CSS framework.                       |
| **State Management** | **TanStack Query** | Server state caching and synchronization.          |
| **Date Handling**    | **VueUse**         | Composable utilities.                              |
| **Icons**            | **Lucide Vue**     | Consistent icon set.                               |
| **PDF Viewer**       | **PDF.js**         | Native browser rendering of encrypted PDF streams. |

---

## 📦 Build System

A Maven-based multi-module build optimized for CI/CD and developer ergonomics.

- **Maven Profiles**:
  - `release`: Full build (Java + Node.js build).
  - `with-frontend`: Builds UI resources into JAR `static/` folder.
  - `windows-portable`: Uses `jpackage` to bundle a JRE + JAR into a `.exe`.
- **Frontend Integration**: Node.js/NPM are installed locally within the `frontend/` folder during build (sandbox).
- **Versioning**: CI-Friendly versioning using `${revision}` property.

## 🤖 AI & Automation

- **Local Inference**: Supports integration with **Ollama** (Llama3, Mistral) for category prediction.
- **Fallback**: Heuristic engine (Rule-based) when AI is disabled.
- **Protocol**: Standard OpenAI-compatible API client.

