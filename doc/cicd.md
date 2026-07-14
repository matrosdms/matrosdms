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
* **`build` (Ubuntu):** compiles backend + frontend once. Every desktop job reuses that one JAR — nothing is recompiled per platform.
* **`build-windows-portable` (Windows), `build-linux-targz` and `build-linux-deb` (Ubuntu):** the three desktop packages. All three call the same composite action (`.github/actions/desktop-package`), so Linux and Windows get identical treatment.
* **`docker-release` (Ubuntu):** builds and pushes the image with semver tags (`1.0.0`, `1.0`, `1`, and `latest` on full releases only).
* **`github-release`:** collects every artifact and publishes the release.

### How the desktop packages are built

All three render the icon (from `MatrosBadge`, with a bare `javac` of two dependency-free classes — no binary icon is checked in, so it cannot drift from the brand) and then call `jpackage` with a bundled runtime, the icon, and `-Djava.awt.headless=false` so the tray works.

**The desktop packages ship no AOT cache**, on purpose: it would add ~156 MB to every download for a faster start (roughly a third off cold boot). Only the Docker image trains one, where the cost is an image layer rather than a user download.

> **If you ever want the cache in the desktop bundles**, be aware of the trap: an AOT cache is pinned to the exact runtime image that produced it — the JVM compares `lib/modules` and silently rejects a cache trained against any other, costing you the size for no gain. `jpackage` strips the `java` launcher out of the runtime it jlinks, so it cannot be trained against. You must `jlink` the runtime yourself, train against it, and hand that same image back via `--runtime-image`.

---

## 📦 Artifacts & Deliverables

When a release is created, the following assets are available:

1. **Docker Image** — `docker pull mschwehl/matrosdms:latest`
   Runs headless (no tray, no browser auto-open). **Boots about 30% faster** (measured back-to-back: 7.0s → 4.7s) thanks to a JDK AOT cache trained *inside* the image — it has to be built there, because a cache baked on the host is pinned to the host's JDK and would simply be rejected by the container's.

2. **Windows Portable** — `MatrosDMS-<version>-windows-portable.zip`
   Self-contained app-image with a bundled runtime. Launches windowed (no console) and lives in the **system tray**.

3. **Linux Tarball** — `MatrosDMS-<version>-linux.tar.gz`
   The same self-contained app-image as Windows: bundled runtime, branded icon, tray and splash. Includes `install-desktop-entry.sh` to register a menu entry.

4. **Linux .deb** — `MatrosDMS-<version>-linux.deb`
   Native package with a real desktop entry (Office menu group, icon, uninstall).

5. **Headless JAR** — `MatrosDMS-<version>-headless.jar`
   Usage: `java -jar MatrosDMS-<version>-headless.jar`. Needs a Java 25 JRE. No tray: a plain `java -jar` leaves Spring Boot's forced `java.awt.headless=true` in place, which is exactly the gate the tray checks.

> The desktop bundles are ~250 MB. They deliberately carry no AOT cache — see above.

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
* We use `eclipse-temurin:25-jdk-noble` (Ubuntu) because the native libraries for PDFBox/Tika have compatibility issues with Alpine's `musl` libc.

**Version is `0-SNAPSHOT` in artifacts?**
* The pipeline uses `-Drevision=${TAG}` to override the version during the Maven build. Ensure the command in `release.yml` includes this flag.

**Java Preview Errors?**
* There should be none: preview features are off. Nothing in the codebase uses a preview API, and `--enable-preview` has to be repeated by *every* launcher (jar, Docker, jpackage) or the app won't start — a standing trap for no benefit. `javac` also rejects the flag whenever the JDK is newer than the `release` level, so it broke the build on each JDK bump. If you re-add it, you must add it to the compiler config, the Docker `ENTRYPOINT`, and all three jpackage invocations.

**Tray doesn't appear on Linux?**
* Expected on GNOME, which dropped legacy tray support; `SystemTray.isSupported()` returns false and the app logs a line and carries on. KDE, XFCE, MATE and Cinnamon work. The web UI is always reachable at `http://localhost:9090` regardless.

**`Unable to map shared spaces` in the container log?**
* The Docker image's AOT cache was rejected — it is pinned to the exact JDK that created it, so this means the base image's JDK changed without the training step re-running. Rebuild the image without a cached layer. It is never fatal: the container starts anyway, just at the un-cached speed.
