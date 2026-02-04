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

|         Job         |                 Description                 |
|---------------------|---------------------------------------------|
| **Integrity Check** | Verify file hashes and database consistency |
| **Reindex Search**  | Rebuild full-text search index              |
| **Export Archive**  | Create backup ZIP of all documents          |

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

1. **Run Integrity Check** periodically to verify file consistency
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

---

## Version

MatrosDMS v1.0 | © 2026 MatrosDMS Team
