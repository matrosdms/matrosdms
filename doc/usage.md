# MatrosDMS User Guide

## Overview

MatrosDMS is a professional Document Management System designed for organizing, categorizing, and retrieving documents efficiently. The application uses a three-pane layout with powerful filtering and search capabilities.

---

## Application Layout

### Main Interface

```
┌─────────────────────────────────────────────────────────────────────────┐
│  [Filter Bar: WHO | WHAT | WHERE]              [Search...]              │
├──────────────┬──────────────────┬───────────────────────────────────────┤
│  SIDEBAR     │  CONTEXT LIST    │  ITEM DETAILS / PREVIEW               │
│  - Categories│  - Folders       │  - Document Properties                │
│  - Inbox     │  - Item Count    │  - Timeline View                      │
│  - Actions   │  - Tags          │  - Forms                              │
└──────────────┴──────────────────┴───────────────────────────────────────┘
```

### Panels

|      Panel       |                      Description                       |
|------------------|--------------------------------------------------------|
| **Sidebar**      | Category tree, Inbox, or Action list depending on mode |
| **Context List** | Folders/contexts matching current category selection   |
| **Detail Pane**  | Document list, preview, forms, or timeline view        |

---

## Keyboard Shortcuts

### Global

|  Shortcut  |              Action              |
|------------|----------------------------------|
| `Ctrl + K` | Open global search               |
| `Escape`   | Close dialogs, cancel operations |

### Forms (Create/Edit)

|  Shortcut  |        Action         |
|------------|-----------------------|
| `Ctrl + S` | Save / Submit form    |
| `Escape`   | Cancel and close form |

### Category Tree Navigation

The category tree follows Windows Explorer keyboard conventions:

|     Shortcut      |                       Action                       |
|-------------------|----------------------------------------------------|
| `↑` / `↓`         | Navigate up/down through nodes                     |
| `→`               | Expand node, or move to first child if expanded    |
| `←`               | Collapse node, or jump to parent if collapsed      |
| `Home`            | Jump to first node                                 |
| `End`             | Jump to last node                                  |
| `Enter` / `Space` | Select the focused node                            |
| `F`               | Add focused node to filter bar                     |
| `*`               | Expand all children of current node                |
| `A-Z`, `0-9`      | Type-ahead: jump to next node starting with letter |

### Data Tables / Lists

| Shortcut  |       Action       |
|-----------|--------------------|
| `↑` / `↓` | Navigate rows      |
| `Enter`   | Select / Open item |
| `Home`    | Jump to first row  |
| `End`     | Jump to last row   |

---

## Filter Bar

The filter bar at the top allows multi-dimensional filtering:

### Dimensions

| Dimension | Color  |           Description            |
|-----------|--------|----------------------------------|
| **WHO**   | Blue   | People, organizations, contacts  |
| **WHAT**  | Green  | Topics, subjects, document types |
| **WHERE** | Orange | Locations, departments, projects |

### Adding Filters

1. **Drag & Drop**: Drag a category from the tree onto a filter box
2. **Double-Click**: Double-click a category node
3. **Keyboard**: Focus a node and press `F`

### Removing Filters

- Click the `×` on individual filter tags
- Click the eraser icon to clear all filters

---

## Search

### Quick Search (`Ctrl + K`)

Type to search across all documents. Supports:

- **Fulltext search**: Just type your query
- **Property filters**: Use `property:value` syntax

### Search Operators

|      Syntax      |      Example       |        Description         |
|------------------|--------------------|----------------------------|
| `text`           | `invoice`          | Fulltext search            |
| `folder:Name`    | `folder:Medical`   | Filter by context/folder   |
| `store:Code`     | `store:ARCH`       | Filter by storage location |
| `date:>2024`     | `date:>2024-01-01` | Date after                 |
| `date:<2024`     | `date:<2024-12-31` | Date before                |
| `source:TYPE`    | `source:EMAIL`     | Filter by source type      |
| `who:Name`       | `who:Martin`       | Filter by person           |
| `what:Topic`     | `what:Insurance`   | Filter by topic            |
| `where:Location` | `where:Munich`     | Filter by location         |

### Command Mode

Type `>` to enter command mode:

|     Command      |         Description         |
|------------------|-----------------------------|
| `> new context`  | Create a new context/folder |
| `> new category` | Create a new category       |
| `> settings`     | Open settings               |
| `> theme`        | Toggle dark/light mode      |

---

## Document Management

### Inbox

The inbox shows files pending organization:

1. **Upload**: Drag files onto the inbox or use the upload button
2. **Process**: Drag inbox items onto a context to file them
3. **AI Suggestions**: Items with AI predictions show suggested categories

### Contexts (Folders)

Contexts are organizational containers for documents:

- Create via `+` button or `Ctrl + K` → `> new context`
- Edit by selecting and clicking the pencil icon
- Archive to hide without deleting

### Items (Documents)

Each document can have:

- **Name**: Display title
- **Issue Date**: The document's effective date
- **Categories**: WHO/WHAT/WHERE classifications
- **Store**: Physical storage location (for paper originals)
- **Attributes**: Custom metadata fields

---

## Views

### Table View

Default spreadsheet-style view with sortable columns.

### Timeline View

Chronological visualization of documents:

- Shows time gaps between documents
- Color-coded by context
- Drag items to move between folders

### Split View

Toggle between:

- **Full List**: Table/timeline takes full width
- **Split**: List on left, preview on right

---

## Drag & Drop

|   Source   |   Target   |             Action             |
|------------|------------|--------------------------------|
| Inbox file | Context    | Create new document in context |
| Document   | Context    | Move document to context       |
| Category   | Filter bar | Add filter                     |
| Document   | Item Stack | Collect for batch operations   |

---

## Settings

Access via sidebar → Settings icon

### Master Data

|        Section        |        Description         |
|-----------------------|----------------------------|
| **Stores**            | Physical storage locations |
| **Users**             | System user accounts       |
| **Attributes**        | Custom field definitions   |
| **Import Categories** | Bulk import category trees |
| **System Jobs**       | Background task management |

### System Jobs

Open **Run System Task**, select a job and press **Start**. Selecting alone starts nothing, and a job
that is already queued or running is not started a second time. Select a job in the list to see its
result and log.

|         Job         |                                     Description                                     |
|---------------------|-------------------------------------------------------------------------------------|
| **Integrity Check** | Every document has its file and the file is unchanged; every file belongs to a document |
| **Reindex Search**  | Rebuild full-text search index                                                      |
| **Export Archive**  | Create backup ZIP of all documents                                                  |

### Document store check

After every start MatrosDMS compares the document store with the database. If they disagree,
administrators see a yellow note below the header:

- **Documents in the store have no database entry**: the database is older than the documents. It
  fell back to an older state, or an older backup was restored. The documents themselves are fine; what
  is missing is their entry (title, context, categories). The Integrity Check lists them with their
  dates. To file them again they have to be decrypted into the inbox; MatrosDMS has no button for that
  yet.
- **Documents in the database have no file**: the store folder is not available (a cloud drive not
  mounted?) or files were removed.
- **Files belong to no document**: something was copied into the store folder by hand.

Running the Integrity Check again updates the note.

---

## Google Tasks & Calendar

Tasks assigned to you can be pushed to Google Tasks, and optionally their due dates into an extra
"MatrosDMS" Google calendar. Ticking a task off or moving its date in Google comes back to MatrosDMS. It is
off until an administrator adds Google OAuth credentials; then each user connects their own
account under **My Profile**. Setup and details: [google-sync.md](google-sync.md).

---

## Tips & Best Practices

### Organization

1. **Use the WHO/WHAT/WHERE dimensions** consistently
2. **Create contexts** for logical groupings (e.g., per project, per person)
3. **Tag documents** with multiple categories for cross-referencing

### Efficiency

1. **Learn keyboard shortcuts** for faster navigation
2. **Use the filter bar** to narrow down large document sets
3. **Double-click categories** to quickly add filters
4. **Use search operators** for precise queries

### Data Integrity

1. **Run Integrity Check** periodically to verify file consistency; after restoring a backup it lists
   the documents added since, which need filing again
2. **Use Archives** for obsolete contexts (instead of deleting)
3. **Regular backups** via Export Archive job

---

## Troubleshooting

### Search not finding documents?

1. Wait for indexing to complete after upload
2. Run "Reindex Search" from System Jobs
3. Check that the document has text content (PDFs need OCR)

### Slow performance?

1. Use filters to reduce displayed items
2. Check System Jobs for running background tasks
3. Consider archiving old contexts

### Upload failures?

1. Check file size limits
2. Ensure file type is supported
3. Check network connectivity

### "MatrosDMS could not start" because of the database

Before it touches the database, MatrosDMS checks that it may run on it. The window says which
case it is and nothing has been changed yet:

| Message | What happened | What to do |
|---|---|---|
| *MatrosDMS x.y is too old for this database* | A newer MatrosDMS already updated this database | Install the current version |
| *The database is older than the last time MatrosDMS ran* | The database file was set back to an older state, e.g. H2 fell back to an old state after an unclean shutdown, or an old copy was put in its place | [Restore the newest backup](#restoring-a-backup) |
| *The database does not match this MatrosDMS version* | The migration history differs: an old copy, or a development build used this database | [Restore the newest backup](#restoring-a-backup) |

For the second case, MatrosDMS keeps a small file `matros.generation` next to the database. It
holds the newest save point the database had on disk, raised at every start and every 5 minutes.
A database behind that file has lost data, so MatrosDMS stops instead of working on, and
overwriting the good backups with, the old state.

The startup backup is taken from the database file **before** H2 opens it. H2 repairs an
uncleanly closed file while opening it, and if that sets it back, the file is rewritten within
seconds, so a backup taken after the open could only capture the result. MatrosDMS copies the file
next to itself (100 MB: about 50 ms on an SSD) and zips the copy into `db-backup` once the start
succeeded. So `matros-db-startup-…zip` holds the state before that start, also before any
database migration of a new version. When MatrosDMS refuses to start, the copy becomes
`matros-db-incident-…zip` instead: the file as it was, possibly with data newer than the last
backup. Do not restore it, but keep it. Repeated refused starts do not add more of them.

Startup and incident backups are cleaned up like all others (see `app.backup` in
`application.yaml`): everything from the last 3 days, then the newest of each day up to 90 days,
never fewer than 10. Switch the startup backup off with `app.backup.on-startup: false` or
`MATROS_BACKUP_ON_STARTUP=false`.

### Restoring a backup

MatrosDMS writes a database snapshot at every start and every night to
`<repository>/db-backup` (`matros-db-startup-…zip`, `matros-db-daily-…zip`). Do not use a
`matros-db-incident-…zip`: that is the file a refused start found. To go back to a backup:

1. Quit MatrosDMS (tray icon → Quit).
2. In `<data dir>/workspace/db`, move `matros.mv.db` and `matros.generation` into a new folder,
   e.g. `<data dir>/workspace/db-quarantine-<date>`. Do not delete them until everything works.
3. Unzip the backup into `<data dir>/workspace/db`. It contains one file, `matros.mv.db`.
4. Start MatrosDMS. It records a new save point and runs as before. The documents themselves live
   in the repository and are not part of the database backup, so nothing else needs restoring.
   Documents added after the backup are still in the store but have no entry any more: the yellow
   note says how many, and the Integrity Check lists them (see [Document store check](#document-store-check)).
5. What changed after the backup is in the [change journal](#change-journal): every line later than
   the time in the backup's file name.

### Change journal

Every successful change made in MatrosDMS to documents, contexts, categories, attribute types and
tasks is written as one line to `<repository>/journal/changes.jsonl`, next to the database backups
and never into the database. Whatever happens to the database, the journal still says what was
done: after restoring a backup, the lines after the backup's time are what is missing.

Each line is a JSON object: `time`, `user`, `method`, `path`, `status`, and the `request` and
`response` bodies (passwords, secrets and tokens blanked out). The response of a create holds the
id the new object got, which later lines refer to. Logins, users, stores and settings are not
journaled.

At the end of each month, or when the active file passes 5 MB, it is packed as
`changes-<yyyy-MM>.<n>.jsonl.gz`. Files are kept for `app.journal.keep-months` (default 120, i.e. ten
years; a household writes a few MB a year), at most 1 GB in total. `MATROS_JOURNAL_ENABLED=false`
switches it off.

### Working on a copy (developers)

Never point a development build at a production data directory: it migrates the database to
schema versions no release knows yet, and a jar rebuilt while it runs can crash H2 while it
closes the file. Give the development build its own data directory and repository, for example
a copy made from the newest backup, and its own port (`--server.port=9199`).

---

### Multi Tenancy

#### Profiles (recommended for desktop builds)

Create a `profiles.properties`. It is looked up in two places, first match wins:

1. the working directory MatrosDMS is started from (e.g. `D:\cloud\matrosdms` next to the jar —
   handy when that folder is cloud-synced, the tenant list travels with the install)
2. `~/.matrosdms/profiles.properties` (Windows: `C:\Users\<you>\.matrosdms\profiles.properties`)

```properties
last-active=household
profile.household.data-dir=D:/cloud/matrosdms/household
profile.invest.data-dir=D:/cloud/matrosdms/invest
# optional per profile: separate repository (keeps backups clean), a pinned port,
# and a display label for the tray/splash (defaults to a prettified name:
# "demo-a" becomes "Demo A")
profile.invest.repository-path=D:/cloud/matrosdms/repository/invest
profile.invest.port=9191
profile.invest.label=Family Invest
```

Use forward slashes in paths (`.properties` treats backslash as an escape). On start, MatrosDMS
boots the `last-active` profile. The tray icon gets a **Switch Profile** submenu — picking another
profile saves it as last-active and restarts the server on that data directory. The tray tooltip
shows which profile is running.

**Every profile gets its own port**, so tenants never share a browser origin (no leaked JWT /
localStorage between tenants) and their URLs stay bookmarkable per tenant. An explicit
`profile.<name>.port` is honored; profiles without one are assigned the next free port from 9090
upward, in alphabetical order. Auto-assigned ports can shift when you add or remove profiles —
pin the port if you rely on a bookmark. The tray menu shows each profile's port.

Precedence: an explicit data directory always wins over the profile config, so existing setups
keep working unchanged:

1. a bare program argument — a desktop shortcut like `MatrosDMS.exe D:\matrosdms\household`
2. the `MATROS_DATA_DIR` environment variable (the `startMatros.cmd` way below)
3. `last-active` from `~/.matrosdms/profiles.properties`
4. the `./data` default

#### Batch file (classic way)

create a batch-file startMatros.cmd

```
@echo off
cd /d "%~dp0"

rem --- assign data and repository paths ---
set MATROS_DATA_DIR=%~1
set MATROS_REPOSITORY_PATH=%~2

rem --- default port ---
set DEFAULT_PORT=9090

rem --- if third parameter exists, use it; otherwise use default ---
if "%~3"=="" (
    set PORT=%DEFAULT_PORT%
) else (
    set PORT=%~3
)

echo Starting MatrosDMS on port %PORT%
echo MATROS_DATA_DIR=%MATROS_DATA_DIR%
echo MATROS_REPOSITORY_PATH=%MATROS_REPOSITORY_PATH%

java -jar server-0-SNAPSHOT.jar --server.port=%PORT% --app.start-browser=true

pause
```

## Using Windows Shortcuts to Launch Tenants

You can now create standard Windows Shortcuts (.lnk) on your desktop to launch specific tenants instantly.
Right-click on your desktop, select New > Shortcut, and use the following format for the Target:

### EXAMPLE

D:\cloud\matrosdms\startMatros.cmd d:/matrosdms/demo-invest d:/cloud/matrosdms/repository/demo-invest 9091

This creates the inbox in d:/matrosdms/demo-invest/inbox

and the repository in d:/cloud/matrosdms/repository/demo-invest

it is important to separate both as we dont to backup a running system. H2 dont like that

#### TrayTools

you can use a tray-tool https://apps.microsoft.com/detail/9NMR3QLVBSXK?hl=neutral&gl=DE&ocid=pdpshare to launch the shortcut from the system tray

## Version

MatrosDMS v1.0 | © 2026 MatrosDMS Team
