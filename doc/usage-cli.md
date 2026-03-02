# Matrosdms CLI

Command-line tool for interacting with a Matrosdms server.
Requires Java 17+ and a running Matrosdms server instance.

---

## Installation

Build the JAR from the project root:

```bash
mvn -pl cli package -DskipTests
```

The fat-JAR is produced at:

```
cli/target/cli-0-SNAPSHOT.jar
```

Create a wrapper script for convenience (optional):

**Linux / macOS** — `~/bin/matros`:
```bash
#!/usr/bin/env bash
java -jar /opt/matrosdms/cli.jar "$@"
```

**Windows** — `matros.cmd`:
```bat
@echo off
java -jar "C:\tools\matrosdms\cli.jar" %*
```

---

## Commands

### `login`

Authenticate against a Matrosdms server and persist the session locally.  
The session (JWT + refresh token) is stored in `~/.matros/session.json`.  
Subsequent commands read the token from this file automatically — no need to pass credentials every time.

```
matros login --server <url> --user <username> [--password <password>]
```

| Option | Short | Required | Description |
|---|---|---|---|
| `--server` | `-s` | ✔ | Server base URL, e.g. `http://localhost:8080` |
| `--user` | `-u` | ✔ | Username |
| `--password` | `-p` | — | Password. If omitted, prompted interactively (recommended) |

**Examples:**

```bash
# Password prompted interactively (no password in shell history)
matros login --server http://localhost:8080 --user admin

# Password inline (scripts / CI)
matros login --server https://dms.example.com --user bot --password s3cr3t
```

**Output:**

```
✔  Logged in as 'admin' @ http://localhost:8080
   Session saved to: C:\Users\alice\.matros\session.json
```

---

### `find-duplicate`

Recursively scans a folder and moves every file that already exists in the DMS
into a separate target folder.  
Files **not** found in the DMS are left untouched.  
The relative subfolder structure is preserved in the target.  
Existing files in the target are never overwritten — a numeric suffix (`_1`, `_2`, …) is appended instead.

Requires an active session — run `matros login` first.

```
matros find-duplicate --docfolder <path> --duplicate-folder <path> [--dry-run]
```

| Option | Short | Required | Description |
|---|---|---|---|
| `--docfolder` | `-d` | ✔ | Source folder to scan recursively |
| `--duplicate-folder` | `-t` | ✔ | Target folder for duplicates |
| `--dry-run` | — | — | Log what would be moved without actually moving anything |

**Examples:**

```bash
# Preview first
matros find-duplicate --docfolder F:\inbox --duplicate-folder F:\duplicates --dry-run

# Perform the move
matros find-duplicate --docfolder F:\inbox --duplicate-folder F:\duplicates

# Subfolders are preserved:
#   F:\inbox\contracts\2024\invoice.pdf  →  F:\duplicates\contracts\2024\invoice.pdf
```

**Log output (INFO level):**

```
15:03:01 INFO  === find-duplicate started ===
15:03:01 INFO    Source  : F:\inbox
15:03:01 INFO    Target  : F:\duplicates
15:03:01 INFO    Dry-run : false
15:03:02 INFO    [DUPLICATE]  contracts\2024\invoice.pdf  (a3f9e1...)
15:03:02 INFO    [MOVED]      contracts\2024\invoice.pdf → contracts\2024\invoice.pdf
15:03:05 INFO  === find-duplicate completed ===
15:03:05 INFO    Scanned   : 142
15:03:05 INFO    Duplicates: 37
15:03:05 INFO    Moved     : 37
15:03:05 INFO    Errors    : 0
Done.  Scanned: 142  |  Duplicates: 37  |  Moved: 37  |  Errors: 0
```

**Exit codes:**

| Code | Meaning |
|---|---|
| `0` | Success, no errors |
| `1` | Fatal error (bad arguments, source folder missing, not logged in) |
| `2` | Completed but at least one file could not be processed |

---

### `decrypt`

Offline recovery tool. Decrypts an encrypted store file (`.enc`) without a
running server — useful for disaster recovery.

```
matros decrypt <file.enc> --password <pwd> --salt <salt> [--out <output>]
```

| Argument / Option | Required | Description |
|---|---|---|
| `<file>` | ✔ | Path to the `.enc` file |
| `--password` / `-p` | ✔ | Store encryption password |
| `--salt` / `-s` | ✔ | Store encryption salt |
| `--out` / `-o` | — | Output file path. If omitted, decrypted content is written to stdout |

**Examples:**

```bash
# Decrypt to file
matros decrypt /vault/abc123.enc -p "mypassword" -s "mysalt" -o restored.pdf

# Decrypt to stdout and pipe to viewer
matros decrypt /vault/abc123.enc -p "mypassword" -s "mysalt" | evince -
```

---

## Session file

The session is persisted as plain JSON at `~/.matros/session.json`:

```json
{
  "serverUrl" : "http://localhost:8080",
  "username"  : "admin",
  "token"     : "eyJhbGci...",
  "refreshToken" : "...",
  "savedAt"   : "2026-03-01T14:05:33Z"
}
```

To log out / clear the session simply delete the file:

```bash
rm ~/.matros/session.json        # Linux / macOS
del %USERPROFILE%\.matros\session.json   # Windows
```

---

## Logging verbosity

The log level can be adjusted at runtime via a system property:

```bash
java -Dlogging.level.net.schwehla.matrosdms.cli=DEBUG -jar cli.jar find-duplicate ...
```

| Level | Content |
|---|---|
| `INFO` (default) | Start/stop banners, duplicates found, files moved, summary |
| `DEBUG` | Per-file hash, API URL, skip reasons, empty-dir cleanup |
| `WARN` | Non-fatal issues (unreadable session file, cleanup failures) |
| `ERROR` | Per-file processing failures |
