# 💻 Development Guide

Welcome to the MatrosDMS developer guide. This project uses a tailored Maven build system designed to be fast for backend iteration while supporting full-stack release builds.

## ⚡ Quick Start

### Prerequisites

- **JDK 25+** (Required for Virtual Threads & FFM API)
- **Maven 3.9+**
- **Node.js 20+** (Optional - Maven can install a local version)

### Build Commands

|       Goal       |            Command            |                    Description                    |
|------------------|-------------------------------|---------------------------------------------------|
| **Backend Loop** | `mvn install`                 | Fastest (15s). Builds backend only. Skips UI/CLI. |
| **Full Stack**   | `mvn install -Pwith-frontend` | Builds server + frontend. Embeds UI in JAR.       |
| **Release**      | `mvn install -Prelease`       | Builds everything (Server, UI, CLI).              |
| **Format Code**  | `mvn spotless:apply -Pformat` | Auto-formats Java code to Eclipse style.          |

---

## 📦 Modular Build Profiles

To keep build times low, we use Maven Profiles to activate modules on demand.

### 1. Default Profile (Server Only)

*Active by default.*
- **Modules**: `server`
- **Use case**: Daily backend coding, API testing, DB migration logic.
- **Time**: ~10-20 seconds.

### 2. Frontend Profile (`-Pwith-frontend`)

- **Modules**: `server`, `frontend`
- **Mechanism**:
  - Uses `frontend-maven-plugin` to install a local Node.js/NPM in `frontend/node/`.
  - Runs `npm install` and `npm run build`.
  - Copies `dist/` assets to `server/src/main/resources/static`.
- **Use case**: Full-stack verification, UI integration testing.

### 3. CLI Profile (`-Pwith-cli`)

- **Modules**: `server`, `cli`
- **Use case**: Developing the command-line importer tool.

### 4. Release Profile (`-Prelease`)

- **Modules**: All (`server`, `frontend`, `cli`)
- **Use case**: CI/CD pipelines, production artifacts.

---

## 🧹 Code Quality & Standards

We use **Spotless** to enforce a bit-perfect code style (Eclipse Formatter). This eliminates "style wars" in code reviews.

### Configuration

- **Style**: Eclipse Formatter (4 spaces indent)
- **Config**: `spec/eclipse-formatter.xml`
- **License**: `spec/license_header.txt` (Auto-applied)
- **Imports**: Sorted strictly (java, javax, jakarta, org, com, net)

### Commands

- **Check (CI)**: `mvn verify -Pcheck-format`
  - Fails the build if code is unformatted.
- **Apply (Dev)**: `mvn spotless:apply -Pformat`
  - Fixes your code automatically. Run this before committing!

---

## 🚀 Native & Portable Packaging

### Windows Portable (`-Pwindows-portable`)

Creates a self-contained folder that runs without an installed JRE.
- **Tool**: `jpackage` (JDK 25+)
- **Output**: `server/target/portable/`
- **Command**:

```bash
mvn package -Pwindows-portable,with-frontend
```

### Native Image (`-Pnative`)

Compiles to a native binary using GraalVM.
- **Prereq**: GraalVM installed or Docker (`-Pnative-image-buildtools`)
- **Start time**: Instant (<0.1s)
- **Command**: `mvn -Pnative native:compile`

---

## 🔧 Core Build Internals

### Versioning (`${revision}`)

We use **CI-Friendly Versioning**.
- You only change the version **once** in the root `pom.xml`.
- The `flatten-maven-plugin` ensures that installed JARs resolve `${revision}` to a concrete number (e.g., `1.0.0`) so other modules can depend on them.

### Database Migrations

- **Flyway** manages schema changes.
- Locations: `server/src/main/resources/db/migration/`
- H2 Database file is created in generic user data folder if not specified.

---

## 🚀 Releases & Deployment

### Branching Strategy

**Simple two-branch model:**
- **`main`** - Production-ready code, triggers releases
- **`develop`** - Integration branch for features

**Workflow:**

```bash
# Feature development
git checkout develop
git checkout -b feature/new-feature
# ... work ...
git push origin feature/new-feature
# Create PR: feature/new-feature → develop

# Release process  
git checkout main
git merge develop
git push origin main    # 🚀 Triggers automatic release
```

### Creating a Release

**Option A: Push to Main (Recommended)**

```bash
# Merge develop to main triggers release
git checkout main
git merge develop
git push origin main    # Automatic release with timestamp version
```

**Option B: Manual Tags**

```bash
# Manual semantic versioning
git tag v1.2.3
git push origin v1.2.3  # Automatic release with tag version
```

### GitHub Actions Setup

**For Main-branch releases** (`.github/workflows/release.yml`):

```yaml
name: Release
on:
  push:
    branches: [main]
  push:
    tags: ['v*']

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: matrosdms/matrosdms

jobs:
  release:
    runs-on: ubuntu-latest
    permissions:
      contents: write
      packages: write
    steps:
      - uses: actions/checkout@v4
      
      - uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
          
      - name: Generate Version
        id: version
        run: |
          if [[ "${{ github.ref_type }}" == "tag" ]]; then
            echo "VERSION=${GITHUB_REF#refs/tags/}" >> $GITHUB_OUTPUT
          else
            echo "VERSION=main-$(date +%Y%m%d-%H%M%S)" >> $GITHUB_OUTPUT
          fi
        
      - name: Build Release
        run: mvn install -Prelease -Drevision=${{ steps.version.outputs.VERSION }}
        
      - name: Setup Docker Buildx
        uses: docker/setup-buildx-action@v3
        
      - name: Login to GHCR
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
          
      - name: Build and Push Docker
        uses: docker/build-push-action@v5
        with:
          platforms: linux/amd64,linux/arm64
          push: true
          tags: |
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:latest
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ steps.version.outputs.VERSION }}
            
      - name: Create GitHub Release  
        uses: softprops/action-gh-release@v1
        if: github.ref_type == 'tag'
        with:
          files: |
            server/target/*.jar
            cli/target/*.jar
          generate_release_notes: true
          
      - name: Create Development Build
        if: github.ref_type != 'tag'
        run: |
          echo "Built development version: ${{ steps.version.outputs.VERSION }}"
          echo "Docker image: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ steps.version.outputs.VERSION }}"
```

### Release Artifacts

**On push to `main`:**
- ✅ Docker image: `ghcr.io/matrosdms/matrosdms:main-20260127-143022`
- ✅ Updated `latest` tag
- ✅ Build artifacts (no GitHub release)

**On tag push:**
- ✅ Docker image: `ghcr.io/matrosdms/matrosdms:v1.2.3`
- ✅ GitHub Release with JAR files
- ✅ Release notes

