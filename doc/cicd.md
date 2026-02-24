# CI/CD Documentation

This project uses **GitHub Actions** for Continuous Integration and Continuous Deployment. The pipelines automatically handle building Java/Spring Boot artifacts, compiling the Frontend, running tests, and publishing Docker images and binaries.

## 🏗 Technology Stack

* **Build System:** Maven (Backend) & Node.js/NPM (Frontend)
* **JDK Version:** Java 25 (Eclipse Temurin)
* **Docker Base Image:** `eclipse-temurin:25-jdk-noble` (Ubuntu 24.04 LTS)
* **Registries:**
* GitHub Container Registry (`ghcr.io`)
* Docker Hub (`docker.io`)

---

## 🔐 Secrets Configuration Manual

To enable the pipeline to push images to Docker Hub, you must configure the following secrets in the GitHub repository settings.

### 1. Generate Docker Hub Token

1. Log in to [hub.docker.com](https://hub.docker.com) as user **`mschwehl`**.
2. Go to **Account Settings** (Avatar in top right) → **Security**.
3. Click **New Access Token**.
4. **Description:** `GitHub Actions CI`
5. **Access permissions:** Select **Read & Write**.
6. Click **Generate** and **copy the token** immediately (you won't see it again).

### 2. Add Secrets to GitHub

1. Navigate to the main page of this GitHub repository.
2. Click on the **Settings** tab (top menu).
3. In the left sidebar, expand **Secrets and variables** and click **Actions**.
4. Click the green **New repository secret** button for each of the following:

| Name                     | Secret Value                    | Description                           |
|:-------------------------|:--------------------------------|:--------------------------------------|
| **`DOCKERHUB_USERNAME`** | `mschwehl`                      | Your Docker Hub username.             |
| **`DOCKERHUB_TOKEN`**    | *[Paste the token from Step 1]* | The Access Token (not your password). |

*Note: The `GITHUB_TOKEN` used for ghcr.io is automatically provided by GitHub Actions and does not need manual configuration.*

---

## 🚀 Workflows

### 1. Continuous Integration (`ci.yml`)

This workflow runs on every push and pull request to active branches to ensure code stability.

* **Triggers:**
* Push/PR to `master`, `main`, `develop`.
* **Nightly Schedule:** Runs automatically at 02:00 UTC.
* **Process:**

1. Sets up **JDK 25**.
2. Restores **Maven** (`~/.m2`) and **NPM** (`~/.npm`) caches.
3. Compiles Backend and Frontend (`mvn install -Pwith-frontend`).
4. Builds a Docker Image using `infra/Dockerfile`.
5. Pushes images to **GHCR** and **Docker Hub**.

* **Docker Tags:**
* `latest`: Pushed from `master`/`main`.
* `develop`: Pushed from `develop`.
* `nightly`: Pushed by the scheduled cron job.

### 2. Release Pipeline (`release.yml`)

This workflow is triggered only when a Semantic Version tag is pushed (e.g., `v1.0.0`). It produces production-ready artifacts.

* **Triggers:** Tags matching `v*.*.*`.
* **Jobs:**
* **`build-and-release` (Windows Runner):**
* Builds the project on Windows to generate platform-specific binaries.
* Creates the **Windows Portable Zip** (`MatrosDMS-1.0.0-windows.zip`).
* Extracts the **Headless JAR** (`MatrosDMS-1.0.0-headless.jar`).
* Creates a **GitHub Release** and uploads these files.
* **`docker-release` (Ubuntu Runner):**
* Builds the Docker image.
* Pushes to registries with Semantic Versioning tags.
* **Docker Tags:**
* `1.0.0` (Exact version)
* `1.0` (Major.Minor)
* `1` (Major)

---

## 📦 Artifacts & Deliverables

When a release is created, the following assets are available:

1. **Docker Image:**

* Command: `docker pull mschwehl/matrosdms:latest`
* *Note:* The container is built with Ubuntu Noble to support Java 25 Preview features.

2. **Windows Portable:**

* A `.zip` file containing the standalone application.

3. **Headless JAR:**

* File: `MatrosDMS-<version>-headless.jar`
* Usage: `java --enable-preview -jar MatrosDMS-<version>-headless.jar`

---

## ⚡ Caching Strategy

To minimize build times and cost, the pipelines utilize aggressive caching:

1. **Maven:** Caches `~/.m2/repository`. Downloads dependencies only when `pom.xml` changes.
2. **NPM:** Caches `~/.npm`. Speeds up the `frontend-maven-plugin` execution.
3. **Docker Layers:** Uses `type=gha` (GitHub Actions Cache). Reuses unchanged Docker layers (e.g., OS base, apt-get installs) so only the application layer is rebuilt.

---

## 📝 How to Publish a Release

To trigger the release pipeline and publish a new version:

1. **Tag the Commit:**

```bash
git tag v1.0.0
```

2. **Push the Tag:**

```bash
git push origin v1.0.0
```

3. **Monitor:** Go to the "Actions" tab in GitHub and watch the **Release** workflow.
4. **Verify:** Check the "Releases" section in GitHub and Docker Hub for the new artifacts.

---

## 🛠 Troubleshooting

**Docker Build Fails on Alpine?**
* We use `eclipse-temurin:25-jdk-noble` (Ubuntu) because Java 25 (Preview) and native libraries for PDFBox/Tika have compatibility issues with Alpine's `musl` libc.

**Version is `0-SNAPSHOT` in artifacts?**
* The pipeline uses `-Drevision=${TAG}` to override the version during the Maven build. Ensure the command in `release.yml` includes this flag.

**Java Preview Errors?**
* The application requires `--enable-preview`. This is baked into the Docker `ENTRYPOINT` and the `pom.xml` compiler configuration.
