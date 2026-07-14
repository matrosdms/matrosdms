# Technical Debt & Bottleneck Inventory

Architecture audit + fix rounds of 2026-07-05 (GUI → backend, deep dive into auto-classification / LLM / prediction).
**[FIXED]** = repaired during the audit session (round 1 = audit quick wins, round 2 = prioritized fix pass). Everything else is open, ranked.

---

## 1. AI / Auto-Classification / Prediction

### Fixed

- **[FIXED]** `OllamaPredictionStrategy`: semaphore permit leaked on interrupt while waiting for the GPU slot.
- **[FIXED]** `OllamaPredictionStrategy`: hallucinated `contextUuid`/`kindUuid` accepted unvalidated → now checked against candidate lists; confidence clamped 0–1; per-field confidences reflect actually-set values.
- **[FIXED]** `OllamaPredictionStrategy`: Ollama JSON mode (`format: "json"`); NPE guard for unloaded prompt template.
- **[FIXED]** `PredictionService`: `@Transactional` pinned a DB connection (pool = 10) for the whole LLM call (up to 10 min).
- **[FIXED]** `ItemAiController`: `@Cacheable` cached 404/503 error responses forever; now 2xx only.
- **[FIXED]** `RAGService`: NPE when a search hit had no stored text layer.
- **[FIXED]** `HeuristicPredictionStrategy`: stray `A-z` regex range admitted `[ \ ] ^ _` ` into similarity tokens.
- **[FIXED]** Heuristic O(contexts) ingest cost mitigated: per-context filename history now cached (`ContextHistoryService`, Caffeine 5-min TTL) instead of one Lucene search per context per document. *Full redesign (single recency query grouped by `MSearchResult.contextUuid`, or an in-memory filename→context index) remains the better long-term shape.*
- **[FIXED]** Frontend `AiService.ts`: chat posted to non-existent `/api/ai/ask` and served ELIZA chatbot answers disguised as real RAG replies. Now calls `/api/ai/chat` (120s timeout); ELIZA only via explicit `eliza:` prefix.
- **[FIXED]** `InboxItem.vue` rendered `prediction.attributes` raw incl. the 768-float `_vector`; underscore keys hidden, confidence displays clamped (also `AiProposalCard.vue`).
- **[FIXED]** `/items/{uuid}/ai/transform` frontend call now has a 120s `AbortSignal` timeout.
- **[FIXED — round 3] Heuristic date extraction was 100% broken.** PHASE C ran the date regex against the OpenNLP-tokenized text, which inserts spaces around punctuation (`2024-03-05` → `2024 - 03 - 05`), so NO date ever matched — every document got a null documentDate. Now parses from the raw text; regex also widened to accept `5. März 2024`. Covered by `HeuristicPredictionStrategyTest`.
- **[FIXED — round 3] Embedding dead-end completed into real semantic search.** Deleted `EmbeddingStep` (wrote `_vector` into predictions where nothing read it). Embeddings now persist to a dedicated `item_embedding` table (`DBItemEmbedding`, Flyway `V50`) at ingest via `SemanticSearchService` (afterCommit, never in-tx), with cosine ranking exposed at `GET /api/search/semantic`. Cleaned up on single + batch delete. Gated by `app.ai.embedding.enabled` (default false). Brute-force cosine scan — fine for personal scale (see Open).
- **[FIXED — round 3] Fake AI chat honesty + status.** ELIZA restored as the *intended* no-LLM-at-home fallback (tries real `/api/ai/chat` first, falls back with ELIZA credited in `sources`; explicit `eliza:` still forces it). `RAGService`/`ItemAiController` now return **502** on LLM failure (was 200-with-error-string / 503, which blanked the SPA).

### Open

- **Semantic search is a brute-force cosine scan** (`SemanticSearchService.search` → `findAll()` + rank). Fine for thousands of docs; a hundred-thousand-doc corpus wants an ANN index.
- **No concurrency guard on chat/transform** — `RAGService.chat` and `ItemAiController.generateContent` hit Ollama unguarded, competing with ingest classification (which respects `ai.concurrency`).
- **Fake per-field confidences (Ollama path)** — one overall LLM confidence copied onto every field. Ask the LLM for per-field scores or hide per-field bars for strategy `ollama`.
- **Prompt injection surface** — document text interpolated unfenced into the classification prompt. UUID validation caps the damage; consider delimiter fencing.
- `PredictionService` recomputes kinds via `categoryRepository.findAll()` + per-row `getRootFor` on every document; cacheable.
- `PredictionService.resolveAttributeUuid` is dead code.
- `findBestDate` returns the *first* date in the text, not the most plausible one (letterhead/birth date can win over the invoice date).
- `EmbeddingService` uses deprecated Ollama `/api/embeddings` (current: `/api/embed`); raw `Map` types.

## 2. Frontend (Vue 3 / TS / TanStack Query)

### Fixed

- **[FIXED]** SSE stream died permanently after graceful server close and after 401 during token rotation (`useServerEvents.ts`); both now reconnect with backoff.
- **[FIXED]** 401-refresh retry lost POST/PUT/PATCH bodies (`api/client.ts`); bodied requests cloned pre-send.
- **[FIXED]** Bundle: 1.57 MB single chunk → ~426 kB startup (~139 kB gzip). Async views in `App.vue`, lucide namespace isolated behind async `DynamicIcon` wrapper, pdfjs lazy via `DocumentPreview`, `manualChunks` for pdfjs/vendor.
- **[FIXED]** Dead code deleted: `TimelineView.vue`, `ContextVisTimeline.vue`, `SetupWizard.vue`, tombstones `WelcomeTour.vue`/`LegalView.vue`; `vis-timeline`/`vis-data` removed from package.json.
- **[FIXED]** Global Vue Query defaults: no retry on 4xx, max 2 retries otherwise (`main.ts`).
- **[FIXED]** `isAdmin` now checks `role === EUserRole.ADMIN` (was: true for any logged-in user).
- **[FIXED]** Jobs tab polling 2s → 15s (SSE already invalidates `['jobs']`).
- **[FIXED]** `ItemService.getByContext`: page size 50 → 200; a malformed row now drops only that row (per-row parse fallback + warn) instead of blanking the whole folder.
- **[FIXED — round 3] State management de-drifted to a single source of truth.** Inbox live state used to live in THREE places (Query cache + Pinia `workflow.liveInboxFiles`/processing/progress/analysis maps + ad-hoc refs) that merged and drifted. SSE events now patch the `['inbox']` query cache directly (`upsertInboxFile`/`removeInboxFile` in `useInboxQueries`); the store keeps only creation-flow/drag UI state. All consumers (`InboxList`, `useItemForm`, `DocumentPreview`) read from the cache. Watchdog uses `inboxUpdateTimes`.
- **[FIXED — round 3] Manual inbox context assignment now persists** — new `POST /api/inbox/{hash}/assign` writes it into `pipeline.json` (read back by `readJobState` on reload). Frontend does an optimistic cache update with rollback in the acting tab; the server also emits a `PipelineResultEvent` for other tabs (rides the same path as normal completion — see the STATUS-dispatch note below). (Was: local-store only, lost on reload.)

### Open

- Prediction payloads still serialized into drag `dataTransfer` (bulky, but `_vector` no longer present).
- **Possible SSE dispatch mismatch (investigate)** — `SseEventListener` broadcasts `PipelineResultEvent`/status as `EBroadcastSource.INBOX` (→ `process:"INBOX"`), but `useServerEvents.handleEvent` only processes `STATUS`/`COMPLETE`/`ERROR` under `process === 'PIPELINE'` (INBOX only matches `FILE_ADDED`). If so, final live status updates arrive only via the `['inbox']` invalidate/refetch, not the `fileState` merge. Pre-existing (not introduced this session); worth confirming against the running app and aligning the source enum with the frontend's expectation.
- History spam: every panel change does `pushState`; context deep-link restore is an empty stub (`useRouteSync.ts`).
- A single 503/504 flips the whole app into the offline overlay (`client.ts` + `App.vue`).
- AI chat history in localStorage unbounded; Enter sends during IME composition (`AiView.vue`).
- Per-row query observers in inbox list (deprecated `useMatrosData` per card, kind-tree query per `AiProposalCard`); inbox list not virtualized.
- 153 `any` occurrences; duplicated tree-lookup/byte-format/blob-open helpers; hardcoded `/api/api/stream/updates` (see backend route-prefix item); `catch(e){throw new Error(e.message)}` strips API error codes.

## 3. Backend (Spring Boot)

### Fixed

- **[FIXED]** **Mail servers bind 127.0.0.1 by default** (were listening on ALL interfaces while accepting any credentials — the app is local-first, so loopback is the correct default). `MATROS_MAIL_BIND=0.0.0.0` exposes them on the LAN; in that case set `app.mail.require-auth=true`, which turns on real credential verification against the user DB (`MailAuthenticator` → `UserService.login`) for IMAP `LOGIN`/`AUTHENTICATE PLAIN` and SMTP `AUTH PLAIN`/`AUTH LOGIN`, and makes SMTP demand auth before MAIL/RCPT/DATA. Default is `false` so existing local Thunderbird profiles keep working unchanged.
- **[FIXED]** SMTP DATA size now enforced (50 MB, was advertise-only → disk-fill DoS); IMAP APPEND literal capped at 50 MB (was `char[clientDeclaredSize]` → OOM DoS).
- **[FIXED]** **Role enforcement**: `@EnableMethodSecurity` + `@PreAuthorize("hasRole('ADMIN')")` on AdminController (jobs, backup), UserController create/delete (update: admin or self), ConfigController writes.
- **[FIXED]** H2 console disabled by default (`MATROS_H2_CONSOLE=true` to enable); frame-options `disable` → `sameOrigin`.
- **[FIXED]** CORS: reflect-any-origin+credentials replaced by an allowlist (`matros.security.allowed-origins`, defaults = local Vite dev ports); preflights from unknown origins get 403, unknown origins get no CORS headers.
- **[FIXED]** SSRF in `EmailEmbeddingStep`: http/https only, public IPs only (blocks loopback/RFC1918/link-local incl. 169.254.169.254/multicast), 5s/10s timeouts, max 30 resources per email.
- **[FIXED]** Batch delete no longer orphans encrypted blobs — `ItemBatchService.batchDelete` moves files to trash post-commit like `hardDeleteItem`.
- **[FIXED]** Pipeline concurrency bounded by semaphore wired to `app.processing.concurrency` (was unbounded `@Async` per file).
- **[FIXED]** Genuine `IOException`s → 500 instead of empty 200 (`GlobalExceptionHandler`); broken-pipe filtering kept.
- **[FIXED]** Backup endpoint reports failure as 500 (was always "ok").
- **[FIXED]** `AdminService` export no longer mutates the shared `ObjectMapper` (uses `copy()`).
- **[FIXED]** `SearchService.reindexAll`: dropped wrapping `@Transactional`; interrupt flag restored.
- **[FIXED]** `MatrosLocalStore`: strips only a trailing `.enc` (was `replace()` on any occurrence).
- **[FIXED]** Upload-lock race in `InboxFileManager` (removal now outside the synchronized block, owner-keyed).
- **[FIXED]** CSV formula-injection guard in `ReportService` (non-numeric cells starting with `= + - @`).
- **[FIXED]** Static `SimpleDateFormat` shared across IMAP threads → immutable `DateTimeFormatter` (`MimeHelper`).
- **[FIXED]** `TikaService` racy lazy init → final fields in constructor (class stays `@Lazy`).
- **[FIXED]** Inbox `{hash}` path params constrained to `[a-fA-F0-9]{64}` at the route level (they flow into `Paths.get` + recursive deletes).
- **[FIXED]** `GET /items` without `context` returns an explicit 400 (was: silent empty page).
- **[FIXED]** `System.err` → slf4j in `ItemService`.

### Open — needs a decision or bigger refactor

1. **Default secrets are production-usable** (JWT HMAC fallback, store password `CHANGE-THIS-PASSWORD` + static salt, DB `admin`/`admin`). *Deliberately NOT failing startup on defaults — that would brick every fresh portable install. Recommended: fail fast when a non-loopback bind/`prod` profile is combined with default secrets.*
2. `ItemIngestionFacade` `@Transactional` spans hashing + encryption + store I/O; move store writes out (compensation logic already exists at :188-199).
3. `AdminService` integrity check / export: unbounded `findAll()`, whole-store re-hash/decrypt inside read-only transactions, N+1 on `kindList`/`attributes`. Paginate, drop the wrapping transaction.
4. `BodyContentHandler(-1)` unbounded extraction buffer in one `TikaService` path.
5. Systematic exception swallowing in the ingest path (`InboxPipelineService`, `InboxFileManager`, `TextExtractionStep`, `MimeHelper`) — add `log.warn` with hash context.
6. N+1 in `ReportService`/export (`kindList` per row) — fetch-join or `@BatchSize`.
7. Encryption metadata drift: config `AES_CTR` vs hardwired AES-GCM vs stored label — unify to one enum.
8. JWT filter does a DB user lookup per request; cache briefly or trust token claims.
9. `ThumbnailService` loads whole PDFs via `readAllBytes`, races on concurrent misses.
10. Route-prefix confusion: global `/api` + `NotificationController` hardcoded `/api/stream` → `/api/api/stream/updates`; frontend hardcodes the double prefix; security matchers duplicated. Fix backend, regenerate client.
11. `InboxWatchService`: WRITE-open stability probe strands read-only files silently; re-hashes every candidate each 2s tick.
12. Unauthenticated `/api/system/**` leaks repository/tenant name.
13. Mail servers still speak plaintext only (no STARTTLS/implicit TLS) — credentials cross the wire unencrypted if exposed beyond loopback. Also `isSafeTarget` DNS check is resolve-then-fetch (theoretical DNS-rebinding window).

---

## 4. Test coverage (round 3)

Was: effectively zero (one scratch probe). Now: a focused unit suite under `server/src/test`, **35 tests, all green** (`mvn -pl server test`). Deliberately no `@SpringBootTest` — the app binds mail-server ports at startup, so context tests would be flaky/port-bound.

- `HeuristicPredictionStrategyTest` (8) — date extraction (ISO / `05.03.2024` / `5. März 2024` / 2-digit year / invalid / none), Aho-Corasick context frequency, null-text safety. **This suite caught the 100%-broken date parser.**
- `OllamaPredictionStrategyTest` (7) — valid mapping, hallucinated-UUID rejection, confidence clamping, JSON-in-prose extraction, unparseable/empty response safety, invalid-date handling.
- `MailAuthenticatorTest` (6) — correct/incorrect creds, blank/null, SASL PLAIN decode, malformed base64.
- `ReportServiceTest` (5) — CSV formula-injection prefixing, numbers exempt, quote doubling, null.
- `VectorMathTest` (5) — byte round-trip, cosine identity/orthogonal/opposite/zero-vector.
- `MimeHelperTest` (2) — IMAP date format + 8-thread concurrency (thread-safety regression guard).
- `ContextHistoryServiceTest` (2) — blank filtering + lowercasing.

Open: no controller/integration tests (would need a test profile that disables the mail servers); Mockito emits a self-attaching-agent warning (cosmetic — future-proof by adding it as a `-javaagent` in surefire).

*Round 1+2 verified: `mvn -pl server compile` clean, `vue-tsc --noEmit` clean, `vite build` clean (~426 kB startup, was 1.57 MB).*
*Round 3 verified: `mvn -pl server test` BUILD SUCCESS (35 tests), `vue-tsc --noEmit` clean, `vite build` clean.*

### Breaking-change notes (round 2)

- SMTP/IMAP now bind **127.0.0.1** (local Thunderbird unaffected). Devices on other hosts (e.g. a LAN scanner) need `MATROS_MAIL_BIND=0.0.0.0` — and should then also set `app.mail.require-auth=true` (opt-in real credential checks; off by default so existing local mail profiles keep working).
- H2 console needs `MATROS_H2_CONSOLE=true` now.
- Cross-origin API access needs the origin listed in `matros.security.allowed-origins` (same-origin production UI and local Vite dev unaffected).
- Admin-only now enforced for: job triggers, backups, user create/delete, config writes.
- `GET /api/items` without `context` returns 400 instead of an empty page.
- H2 is now fsynced cyclically (`CHECKPOINT SYNC`, every 5 min, `app.database.checkpoint-interval-ms`) so closing the console window can't lose more than one interval of committed work.

### New in round 3

- New Flyway migration **`V50__item_embedding.sql`** creates the `item_embedding` table. Runs automatically on next startup.
- New endpoints: `POST /api/inbox/{hash}/assign?contextUuid=…` (persist manual assignment), `GET /api/search/semantic?q=…&topK=…` (vector search; returns `{enabled:false}` when embeddings are off). **Frontend `schema.ts` is not yet regenerated** — the assign call uses an `as any` cast; regenerate the OpenAPI client against a running backend to type it and to consume semantic search in the UI.
- Semantic search is opt-in: set `app.ai.embedding.enabled=true` (`MATROS_AI_EMBED_ENABLED`) + an Ollama embedding model (`MATROS_AI_EMBED_MODEL`, default `nomic-embed-text`). Off by default; existing installs are unaffected and embeddings backfill only for newly ingested documents (add a reindex job later to backfill old ones).
