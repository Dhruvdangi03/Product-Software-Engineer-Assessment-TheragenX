# Testing guide

How to run the project and check every feature by hand. Each step gives the command, what it does, and the result you should see.

Requirements: Java 21+, Node 20+, Python 3 and `curl`. Run every command from the repository root.

Testing changes nothing in the repository, except the review decisions the UI saves to `reviews/`. That folder is git-ignored, and step 8 shows how to remove it.

## Overview

| Step | What it checks | Needs a key? | Time |
|---|---|---|---|
| 1 | Backend tests | No | ~1 min |
| 2 | UI tests and type check | No | ~30 s |
| 3 | Replay rebuilds `out/` exactly | No | ~30 s |
| 4 | Checker score and provenance | No | seconds |
| 5 | No API key in the repository | No | seconds |
| 6 | A catalog change flows through the pipeline and UI | No | ~3 min |
| 7 | Review UI, feature by feature | No | ~5 min |
| 8 | REST API by hand | No | ~2 min |
| 9 | A new document against the real model | Yes | ~2 min |

---

## 1. Backend tests

**Command**

```bash
./mvnw test
```

**What it does:** compiles the Spring Boot backend and runs every JUnit test, offline. The tests cover:
- catalog validation for all seven field types
- locating quotes in the text, ignoring case and whitespace and counting Unicode code points
- turning an invalid or ungrounded answer into `unknown` with a note
- capping derived values at medium confidence
- the minimum-elements rule
- retries and backoff
- removing the API key from fixtures
- the duplicate rule
- replay reproducing `out/`
- replay against a changed catalog
- every API endpoint, including its error responses

**Expected result**
- `BUILD SUCCESS`
- `Tests run: 72, Failures: 0, Errors: 0, Skipped: 0`

## 2. UI tests and type check

**Commands**

```bash
npm --prefix ui ci              # first time only: installs the UI dependencies
npm --prefix ui test            # runs the Vitest suite
npm --prefix ui run typecheck   # runs npx tsc --noEmit
```

**What it does:** runs the React tests, covering:
- highlight positions, including code points and overlapping quotes
- risk ranking
- sections, labels and order coming from the catalog, including a newly added field
- the right edit control for each field type
- keyboard navigation and accepting with `a`
- clicking a quote to select its field
- the no-case state
- a guard that fails if any catalog field id appears in the UI source

Then it type-checks the whole UI.

**Expected result**
- `Test Files  6 passed (6)` and `Tests  22 passed (22)`
- the type check prints nothing, meaning no errors

## 3. Replay rebuilds `out/` without a key

**Commands**

```bash
./replay.sh
git status --short out/
```

**What it does:** builds the backend and runs the full pipeline on the recorded model answers in `fixtures/`, with no network and no key. The output overwrites `out/`; `git status` then shows whether anything changed.

**Expected result**
- one log line per document, D01 to S04, including `S03: no text file, flagged as unsupported format`, ending with `Wrote 12 documents and 1 duplicate group(s)`
- `git status --short out/` prints **nothing**: replay reproduced the committed outputs exactly

## 4. Score with the checker

**Command**

```bash
python3 tools/check.py --catalog catalog/field_catalog.json --answers answers --out out --docs documents
```

**What it does:** runs the exercise's checker. It compares `out/` with the answers for D01, D02, D03 and D05, and checks that the text at every quote's start and end positions is the quote.

**Expected result:** a per-field listing with `ok` on every row, then:

```
Field points:          64.0 / 64  (100.0%)
Wrong at confidence:   high 0   medium 0   low 0
Unknown, answer had one: 0
Case-count errors:     0
Provenance:            62 of 62 extracted fields grounded (100.0%)
```

## 5. No API key in the repository

**Command**

```bash
git log -p | grep -cE "AIza[0-9A-Za-z_-]{35}"
```

**What it does:** searches the whole git history for a Google API key (`AIza` followed by 35 characters).

**Expected result:** `0`.

A plain `grep AIza` finds two lines, but both are the key-detection test's own comment and regex (`CommittedFixturesTest`).

## 6. Catalog change

This is the change the reviewers will try in the follow-up session: a new field must flow through without code changes.

**Step 6.1: copy the catalog**

```bash
cp catalog/field_catalog.json /tmp/catalog2.json
```

What it does: makes a copy to edit, so the committed catalog stays untouched.

Open `/tmp/catalog2.json` and add this object at the end of the `"fields"` list. Put a comma after the previous `}`.

```json
{"id": "patient_weight_kg", "label": "Weight (kg)", "section": "patient", "type": "number", "validation": {"min": 0}}
```

**Step 6.2: replay against the changed catalog**

```bash
./replay.sh /tmp/catalog2.json
git diff --stat out/
```

What it does: replays the same recordings, checks every recorded value against the new catalog, and shows what changed in `out/`.

Expected result:
- `10 files changed, 44 insertions(+)`, with insertions only and no deletions
- `out/D03.json` (or any case) contains:
  ```json
  "patient_weight_kg" : {
    "status" : "unknown",
    "note" : "No model answer for this field (for example, a field added to the catalog after the answer was recorded)."
  }
  ```
- every other value is unchanged

**Step 6.3: see the new field in the UI**

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--intake.catalog-path=/tmp/catalog2.json
npm --prefix ui run dev        # in a second terminal
```

What it does: starts the API with the changed catalog, then the UI.

Expected result: open <http://localhost:5173>. **Weight (kg)** appears in the Patient section of every case, shown as "Not stated", with no code change. Stop both servers with Ctrl+C.

**Step 6.4: restore**

```bash
./replay.sh
git status --short out/
```

What it does: rebuilds `out/` with the committed catalog.

Expected result: `git status` prints nothing.

## 7. Review UI

**Command**

```bash
./start.sh
```

**What it does:**
1. installs the UI dependencies on the first run
2. builds the backend and starts it on port 8080, serving the committed `out/`
3. waits for it to answer
4. starts the UI on port 5173

**Expected result:** Vite prints `Local: http://localhost:5173/`. Open that address. Ctrl+C in this terminal stops both servers.

Then work through this table.

| # | Feature | Do this | Expected result |
|---|---|---|---|
| 7.1 | Document list | Look at the left sidebar | 12 documents with title, case count and flags. D04 says `multiple patients`; D06 says "no case"; S03 says "no case" with `unsupported format`; S01 and S02 say `possible duplicate` |
| 7.2 | Side by side | Open **D01** | Document text on the left. Fields on the right, grouped Patient / Suspect drug / Adverse event / Report, in catalog order with catalog labels |
| 7.3 | Highlight and scroll | Click **Reaction onset date** | The row expands to show its quote. The same quote is outlined in the document and scrolls into view |
| 7.4 | Quote to field | Click any underlined quote in the document | Its field is selected and expanded |
| 7.5 | Conflict at a glance | Open **D05** | The Age row is tinted red: "Document contradicts itself" with a "! Conflict" badge. Expanded, it shows the note and both quotes, "Age: 54" and "45-year-old", which are also outlined in the text |
| 7.6 | Risk filter | In D05 click the **Conflict 1** chip, then **All** | Only the Age row, then all 16 fields |
| 7.7 | Unknown presentation | Open **D04**, press `]` | Switches to Baby R.D. Dose and Dose unit show "Not stated" with a dashed "? Unknown" badge |
| 7.8 | Several cases | In D04 click the tabs, or press `[` and `]` | Switches between Mrs P.R.D. and Baby R.D. |
| 7.9 | No case | Open **D06** | "This document does not describe a reportable case", with the reasons: no identifiable patient, no adverse event |
| 7.10 | Unsupported format | Open **S03** | "No text to show… image-only PDF" |
| 7.11 | Prompt injection | Open **D07** | Flag `embedded instructions ignored`. Outcome is Recovering, Seriousness is Hospitalization, Reporter is Consumer: not what the hidden text asked for |
| 7.12 | Controls from the catalog | In D01 select **Sex** and press `e`; then try **Drug start date**; then **Seriousness** | A dropdown with Male/Female; a date picker; checkboxes for each seriousness option. `Esc` closes the editor |
| 7.13 | Accept | Select a field and press `a` (or click **Accept**) | "✓ Accepted". Selection moves to the next field; the progress bar in the header and the bar under the document in the sidebar go up |
| 7.14 | Reject | Select another field and press `r` | "✕ Rejected", with the value struck through |
| 7.15 | Edit | Select a field, press `e`, change the value, click **Save** | The new value in purple with "✎ Edited", next to the struck-through model value |
| 7.16 | Edit validation | Edit **Patient initials** to `m.k.` and click **Save** | The browser blocks it, because the input carries the catalog pattern `^[A-Z]{1,4}$`. The server-side check is in step 8 |
| 7.17 | Undo | Select a reviewed field and press `u` | The decision is removed |
| 7.18 | Persistence | Reload the page | All decisions are still there (saved in `reviews/<id>.json`) |
| 7.19 | Keyboard | Press `j` / `k` (or ↓ / ↑), then `?` | Moves between fields; the list of keyboard shortcuts opens |
| 7.20 | Duplicates | Click **Duplicates** at the top of the sidebar | "S01 and S02 describe the same case", high confidence, "13 same, 3 differ". A field-by-field table, then both texts side by side. Clicking a row highlights that field in both texts |
| 7.21 | Error state | In another terminal run `pkill -f "java -jar target/case-intake"`, then reload the page | "Could not load…" with a **Try again** button. Restart with `./start.sh` |
| 7.22 | Theme | Switch your system between light and dark mode | The UI follows it |

## 8. REST API by hand

Run these with `./start.sh` running.

| Command | What it does | Expected result |
|---|---|---|
| `curl -s localhost:8080/api/catalog \| head -c 300` | Fetches the catalog the UI renders from | JSON starting with `"catalog_version" : "1.0"` |
| `curl -s localhost:8080/api/documents \| head -c 600` | Lists documents | `id`, `title`, `case_count`, `flags` and `review_progress` for each document |
| `curl -s localhost:8080/api/documents/D04 \| grep -c case_index` | Fetches one document's text and output | `2` (two cases) |
| `curl -s -o /dev/null -w "%{http_code}\n" localhost:8080/api/documents/NOPE` | Asks for a document that does not exist | `404` |
| `curl -s -X PATCH localhost:8080/api/documents/D01/cases/0/fields/patient_sex -H 'Content-Type: application/json' -d '{"action":"accept"}'` | Accepts a field | HTTP 200 and the document's reviews, with `"decision" : "accepted"` |
| `curl -s -X PATCH localhost:8080/api/documents/D01/cases/0/fields/patient_initials -H 'Content-Type: application/json' -d '{"action":"edit","value":"m.k."}'` | Sends an edit the catalog rejects | HTTP 400 with `"detail" : "Edited value for Patient initials is not valid: 'm.k.' does not match the pattern ^[A-Z]{1,4}$"` |
| `curl -s localhost:8080/api/duplicates \| head -c 400` | Fetches duplicate pairs with their field comparison | `"document_ids" : [ "S01", "S02" ]`, `"confidence" : "high"` |

**Clean up**

```bash
rm -rf reviews/
git status
```

What it does: removes the review decisions you made in steps 7 and 8.

Expected result: `nothing to commit, working tree clean`.

## 9. A new document against the real model (optional)

This step needs a Gemini API key and uses the free-tier quota.

**Step 9.1: set the key**

Create `.env` in the repository root containing `GEMINI_API_KEY=your-key`. Git ignores this file.

**Step 9.2: add a document**

Copy one of the existing reports to `documents/D09.txt` and change some details, such as the name, dates and drug.

**Step 9.3: run the pipeline**

```bash
set -a; . ./.env; set +a
./mvnw -q -DskipTests package
java -jar target/case-intake.jar --pipeline --intake.llm.mode=record-missing
```

What it does: loads the key into this shell only, builds the backend, and runs the pipeline in `record-missing` mode. Only documents without a fixture are sent to the model, so here that is D09 alone. Its exchange is saved and every output is rewritten.

Expected result:
- `D09: asking gemini/gemini-3.5-flash`, then `D09: 1 case(s), flags []` (or the flags that apply)
- new files `fixtures/D09.json` and `out/D09.json`
- the other outputs are unchanged
- D09 appears in the UI after a reload

If you see `HTTP 429` or `503`, that is Gemini's rate limit or demand. The client retries on its own. If it still fails, wait and run the same command again; finished documents are kept.

**Step 9.4: undo**

```bash
rm documents/D09.txt fixtures/D09.json out/D09.json
./replay.sh
git status
```

Expected result: the working tree is clean again.

Don't use `--intake.llm.mode=record-all` unless you want to re-record and overwrite every fixture.
