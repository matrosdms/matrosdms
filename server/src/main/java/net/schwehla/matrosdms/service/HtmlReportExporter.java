/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import net.schwehla.matrosdms.entity.DBCategory;
import net.schwehla.matrosdms.entity.DBItem;

public class HtmlReportExporter {

	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	public String export(List<DBItem> items) {
		StringBuilder html = new StringBuilder();

		html.append("<!DOCTYPE html>\n<html lang=\"de\">\n<head>\n");
		html.append("<meta charset=\"UTF-8\">\n");
		html.append("<title>MatrosDMS Report</title>\n");
		html.append("<style>\n");
		html.append("body { font-family: monospace; font-size: 13px; padding: 16px; }\n");
		html.append("table { border-collapse: collapse; width: 100%; }\n");
		html.append("th, td { border: 1px solid #ccc; padding: 4px 8px; text-align: left; white-space: nowrap; }\n");
		html.append("th.hdr { background: #f0f0f0; cursor: pointer; user-select: none; }\n");
		html.append("th.hdr.asc::after  { content: ' ↑'; }\n");
		html.append("th.hdr.desc::after { content: ' ↓'; }\n");
		html.append("th.flt { background: #fafafa; padding: 2px 4px; }\n");
		html.append(
				"th.flt input { font-family: monospace; font-size: 12px; padding: 2px 4px; border: 1px solid #ccc; box-sizing: border-box; }\n");
		html.append("th.flt input.full { width: 100%; }\n");
		html.append("tr:nth-child(even) td { background: #f9f9f9; }\n");
		html.append("</style>\n</head>\n<body>\n");

		html.append("<p style=\"margin-bottom:8px\">Generated: ").append(LocalDate.now().format(DATE_FMT))
				.append("</p>\n");
		html.append("<table id=\"t\">\n<thead>\n");

		// Columns: ID, Context, Name, Description, Issue Date, Store (fused), Tags,
		// UUID, Filename
		// Store column (index 5) = store.shortname + storageItemIdentifier e.g. "s1"
		String[] headers = { "ID", "Context", "Name", "Description", "Issue Date", "Store", "Tags", "UUID",
				"Filename" };
		String[] types = { "num", "str", "str", "str", "str", "storenr", "str", "str", "str" };
		int STORE_COL = 5;

		// Row 1: sortable headers
		html.append("<tr>\n");
		for (int i = 0; i < headers.length; i++) {
			html.append("<th class=\"hdr\" onclick=\"sortTable(").append(i).append(",'").append(types[i])
					.append("')\">")
					.append(headers[i]).append("</th>\n");
		}
		html.append("</tr>\n");

		// Row 2: per-column filter inputs
		// Store column gets two inputs side-by-side: [store▏] [nr▏]
		html.append("<tr>\n");
		for (int i = 0; i < headers.length; i++) {
			if (i == STORE_COL) {
				html.append("<th class=\"flt\" style=\"min-width:90px;\">")
						.append("<input class=\"\" style=\"width:48%;margin-right:3%\" type=\"text\" placeholder=\"store\" oninput=\"filterTable()\" data-col=\"5\" data-part=\"store\">")
						.append("<input class=\"\" style=\"width:45%\" type=\"text\" placeholder=\"nr\" oninput=\"filterTable()\" data-col=\"5\" data-part=\"nr\">")
						.append("</th>\n");
			} else {
				html.append(
						"<th class=\"flt\"><input class=\"full\" type=\"text\" placeholder=\"\" oninput=\"filterTable()\" data-col=\"")
						.append(i).append("\"></th>\n");
			}
		}
		html.append("</tr>\n</thead>\n<tbody id=\"tb\">\n");

		for (DBItem item : items) {
			String context = item.getInfoContext() != null ? esc(item.getInfoContext().getName()) : "";
			String date = item.getIssueDate() != null ? item.getIssueDate().format(DATE_FMT) : "";
			// Fuse store shortname + LfdNr into one value, e.g. "s1"
			String store = item.getStore() != null ? esc(item.getStore().getShortname()) : "";
			String lfdnr = item.getStorageItemIdentifier() != null ? esc(item.getStorageItemIdentifier()) : "";
			String storeCell = store + lfdnr;
			String filename = item.getFile() != null ? esc(item.getFile().getFilename()) : "";
			String uuid = item.getUuid() != null ? item.getUuid().toString() : "";
			String tags = item.getKindList().stream()
					.map(DBCategory::getName)
					.map(this::esc)
					.collect(Collectors.joining(", "));

			html.append("<tr>")
					.append("<td>").append(item.getId()).append("</td>")
					.append("<td>").append(context).append("</td>")
					.append("<td>").append(esc(item.getName())).append("</td>")
					.append("<td>").append(esc(item.getDescription())).append("</td>")
					.append("<td>").append(date).append("</td>")
					.append("<td>").append(storeCell).append("</td>")
					.append("<td>").append(tags).append("</td>")
					.append("<td>").append(uuid).append("</td>")
					.append("<td>").append(filename).append("</td>")
					.append("</tr>\n");
		}

		html.append("</tbody>\n</table>\n");
		html.append(JS);
		html.append("</body>\n</html>\n");

		return html.toString();
	}

	private String esc(Object raw) {
		if (raw == null)
			return "";
		return String.valueOf(raw)
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;");
	}

	private static final String JS = """
			<script>
			var sortCol = -1, sortDir = 1;

			function parseStoreNr(s) {
			  s = (s || '').trim().toLowerCase();
			  var m = s.match(/^([a-z]+)(\\d+)(.*)$/);
			  if (m) return { p: m[1], n: parseInt(m[2], 10), rest: m[3] };
			  return { p: s, n: -1, rest: '' };
			}

			function cmpStoreNr(a, b) {
			  var pa = parseStoreNr(a), pb = parseStoreNr(b);
			  if (pa.p !== pb.p) return pa.p.localeCompare(pb.p);
			  if (pa.n !== pb.n) return pa.n - pb.n;
			  return pa.rest.localeCompare(pb.rest);
			}

			function cmp(a, b, type) {
			  if (type === 'num')     return (parseFloat(a) || 0) - (parseFloat(b) || 0);
			  if (type === 'storenr') return cmpStoreNr(a, b);
			  return a.localeCompare(b);
			}

			function sortTable(col, type) {
			  var dir = (sortCol === col) ? -sortDir : 1;
			  sortCol = col; sortDir = dir;

			  var tb   = document.getElementById('tb');
			  var rows = Array.from(tb.querySelectorAll('tr'));
			  rows.sort(function(a, b) {
			    return cmp(a.cells[col].innerText, b.cells[col].innerText, type) * dir;
			  });
			  rows.forEach(function(r) { tb.appendChild(r); });

			  document.querySelectorAll('#t thead th.hdr').forEach(function(th, i) {
			    th.className = 'hdr' + (i === col ? (dir === 1 ? ' asc' : ' desc') : '');
			  });
			}

			function filterTable() {
			  // Collect standard column filters
			  var filters = {};
			  document.querySelectorAll('#t thead th.flt input:not([data-part])').forEach(function(inp) {
			    var q = inp.value.toLowerCase().trim();
			    if (q) filters[parseInt(inp.dataset.col)] = q;
			  });

			  // Store double-filter: match prefix (store) and suffix (nr) independently
			  var storeFilter = document.querySelector('input[data-col="5"][data-part="store"]').value.toLowerCase().trim();
			  var nrFilter    = document.querySelector('input[data-col="5"][data-part="nr"]').value.toLowerCase().trim();

			  document.querySelectorAll('#tb tr').forEach(function(row) {
			    var show = true;

			    // Standard column filters
			    for (var col in filters) {
			      var cell = row.cells[col];
			      if (!cell || !cell.innerText.toLowerCase().includes(filters[col])) {
			        show = false; break;
			      }
			    }

			    // Store column double filter
			    if (show && (storeFilter || nrFilter)) {
			      var storeCell = (row.cells[5] ? row.cells[5].innerText.toLowerCase() : '');
			      var parsed    = storeCell.match(/^([a-z]*)(.*)$/);
			      var prefix    = parsed ? parsed[1] : storeCell;
			      var nr        = parsed ? parsed[2] : '';
			      if (storeFilter && !prefix.includes(storeFilter)) show = false;
			      if (nrFilter    && !nr.includes(nrFilter))         show = false;
			    }

			    row.style.display = show ? '' : 'none';
			  });
			}

			sortTable(5, 'storenr');
			</script>
			""";
}
