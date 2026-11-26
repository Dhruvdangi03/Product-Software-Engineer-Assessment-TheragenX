# Case Intake

Extracts structured adverse-event cases from free-text reports with a language model, and lets a reviewer check every value against the text it came from.

- **Pipeline** (Java 21, Spring Boot 3.5): reads `documents/`, asks the model for every field in `catalog/field_catalog.json`, checks each answer against the catalog and the text, and writes `out/<DOCUMENT_ID>.json`.
- **REST API** (Spring Boot): serves the catalog, the documents and the outputs, and records review decisions.
- **Review UI** (React 19, TypeScript, Vite): the document and its fields side by side.

The catalog is the only place a field is defined. Prompts, the model's response schema, validation and the UI all read it, so a field added to the catalog flows through without a code change.

## Provider and model

**Google Gemini, `gemini-3.5-flash`**, which the API currently serves as `gemini-3.6-flash` (see `modelVersion` in `fixtures/`). `gemini-2.5-flash` is no longer offered to new API keys. The temperature stays at Gemini 3's default of 1.0, as Google advises. Reproducibility comes from the recorded fixtures.

## Status

| Item | Status |
|---|---|
| Part 1: extraction pipeline, record and replay | Done |
| Part 2: REST API | Done |
| Part 3: review UI | Done |
| Stretch: the four stretch documents | Done. S01, S02 and S04 are extracted. S03 is image-only, so it is flagged `unsupported_format` with no cases |
| Stretch: duplicates | Done. S01 and S02, in `out/duplicates.json`, with a side-by-side view in the UI |
| Stretch: review actions | Done. Accept, reject, edit and undo over `PATCH`, with progress per document |
| Stretch: quote-to-field and keyboard | Done |

Checker on the four documents with answers: **64.0 / 64 (100%)**, no wrong values at any confidence, 62 of 62 extracted fields grounded. The full output and the notes on the other documents are in [NOTES.md](NOTES.md).

## Requirements

- Java 21+ (the Maven wrapper downloads Maven itself)
- Node 20+ and npm
- `curl`, used by `start.sh`
- Python 3, only for the checker

## Start the backend and the UI from fixtures (no key needed)

```bash
./start.sh
```

Then open <http://localhost:5173>. The script does four things:

1. installs the UI dependencies on its first run
2. builds the backend
3. starts the backend on port 8080, serving the committed `out/`
4. starts the UI on port 5173

Ctrl+C stops both.

If the UI shows "Could not load…", the backend did not start. The usual cause is that port 8080 is already in use. Free the port and run the script again.

To start them separately:

```bash
./mvnw spring-boot:run                          # API on http://localhost:8080/api
npm --prefix ui ci && npm --prefix ui run dev   # UI on http://localhost:5173 (proxies /api to 8080)
```

## Rebuild `out/` from the recordings

```bash
./replay.sh                          # committed catalog
./replay.sh path/to/other_catalog.json
```

Replay needs no key and no network. It runs the whole pipeline on the recorded model answers in `fixtures/` and reproduces the committed `out/` exactly; `git status out/` shows nothing afterwards.

With a changed catalog, every recorded value is checked against the new catalog. A value that no longer passes, or a field the recordings don't cover (such as a newly added one), comes out `unknown` with a note saying why.

## Run the pipeline against the model

The API key comes from the environment and is never written to a file in the repository.

```bash
export GEMINI_API_KEY=...            # or put it in .env, which git ignores, and run: set -a; . ./.env; set +a
./mvnw -q -DskipTests package

# Call the model only for documents that have no fixture yet. An interrupted run resumes here.
java -jar target/case-intake.jar --pipeline --intake.llm.mode=record-missing

# Call the model for every document and overwrite the fixtures, for example after a prompt change.
java -jar target/case-intake.jar --pipeline --intake.llm.mode=record-all
```

| `intake.llm.mode` | What it does | Needs a key |
|---|---|---|
| `replay` (default) | Answers only from `fixtures/`, with no network | No |
| `record-missing` | Calls the model for documents without a fixture, and replays the rest | Yes |
| `record-all` | Calls the model for every document and overwrites its fixture | Yes |

Each recording is saved to `fixtures/<id>.json` with the key removed. The key travels in a header, never in the URL or the body.

**Rate limits.** Requests are spaced 7 s apart. A 429 or 5xx response is retried with exponential backoff, waiting as long as Gemini's `RetryInfo` asks. A used-up daily quota stops the run at once with a clear message, and `record-missing` resumes from there later.

Every setting in `src/main/resources/application.yaml` can be overridden on the command line, for example `--intake.llm.model=gemini-3-flash-preview` or `--intake.documents-dir=other/folder`.

## Tests

```bash
./mvnw test                       # backend: 72 tests, covering unit, replay-reproduces-out/, changed catalog and API
npm --prefix ui test              # UI: 22 tests (Vitest)
npm --prefix ui run typecheck     # npx tsc --noEmit
```

Both suites run without network once the dependencies are installed. The UI suite also fails if any catalog field id appears in the UI source.

See [TESTING.md](TESTING.md) for a step-by-step check of every feature, with expected results.

## Score with the checker

```bash
python3 tools/check.py --catalog catalog/field_catalog.json --answers answers --out out --docs documents
```

## REST API

The UI talks to the backend only through these endpoints. Snake_case JSON throughout.

| Method and path | Purpose |
|---|---|
| `GET /api/catalog` | The catalog the UI renders from |
| `GET /api/documents` | List: id, title, case count, flags, review progress |
| `GET /api/documents/{id}` | The document text, its extraction output and its review decisions |
| `PATCH /api/documents/{id}/cases/{caseIndex}/fields/{fieldId}` | Review one field. Body: `{"action": "accept" \| "reject" \| "edit" \| "reset", "value": ...}`, where `value` is needed only for `edit` |
| `GET /api/duplicates` | Duplicate groups with a field-by-field comparison |

An edited value is checked against the catalog with the same validator the pipeline uses, and an invalid edit returns HTTP 400. Unknown documents, cases or fields return 404.

Review decisions are stored in `reviews/<id>.json`, apart from `out/`, so the outputs stay exactly what the pipeline produced. The folder is git-ignored; delete it to start over.

## Review UI

- Document text on the left. Fields on the right, grouped by catalog section, in catalog order, with catalog labels.
- Each field has a risk badge that combines status and confidence, shown with a symbol and a word, not colour alone. `unknown` and `conflict` rows each have their own look.
- Filter chips at the top count the fields at each risk level.
- Selecting a field highlights its quotes in the document and scrolls them into view. Clicking a quote selects its field.
- Edit controls follow the field type: a select for `coded`, a date picker for `date`, checkboxes for `coded_multi`.
- Edited values appear in a distinct colour next to the struck-through model value. Each document shows its review progress.
- Loading, error, empty and no-case states each have their own screen. A document with no case explains why, from its flags.

Keyboard:

| Key | Action |
|---|---|
| `j` / `k`, `↓` / `↑` | Next or previous field |
| `a` / `r` / `e` / `u` | Accept, reject, edit, undo |
| `[` / `]` | Previous or next case |
| `Esc` | Close the editor or clear the selection |
| `?` | Show the shortcuts |

## Configuration

Settings are in `src/main/resources/application.yaml`, under `intake`:

| Key | Default | Meaning |
|---|---|---|
| `catalog-path`, `documents-dir`, `output-dir`, `fixtures-dir`, `reviews-dir`, `schema-dir` | repository folders | Paths, relative to the repository root |
| `llm.mode` | `replay` | See the table above |
| `llm.provider`, `llm.model`, `llm.base-url` | `gemini`, `gemini-3.5-flash` | Which `LlmClient` and model to use |
| `llm.api-key` | `${GEMINI_API_KEY}` | Read from the environment only |
| `llm.min-interval-between-requests`, `llm.max-attempts`, `llm.initial-backoff` | `7s`, `5`, `10s` | Rate-limit handling |
| `duplicates.*` | `patient` identity section, `report` ignored, at least 2 identity fields compared, 0.5 match ratio | Duplicate rule |

## Layout

```
catalog/        field_catalog.json: the single source of field definitions
documents/      D01–D08 (.txt + .pdf) and stretch/ S01–S04
schema/         output schemas; every output is validated against them before it is written
answers/        answers for D01, D02, D03, D05 (read by the checker)
tools/check.py  the exercise's checker
fixtures/       recorded model exchanges, one per document (S03 has none: no text)
out/            pipeline outputs and duplicates.json
reviews/        review decisions made in the UI (git-ignored)
replay.sh       rebuilds out/ from fixtures/
start.sh        starts backend and UI from fixtures

src/main/java/com/theragenx/intake/
  catalog/      catalog model, loader, FieldValueValidator (the only place types and rules are checked)
  llm/          LlmClient interface, GeminiClient, RetryingLlmClient, FixtureStore (record and replay)
  extraction/   ExtractionPipeline, PromptBuilder, ResponseSchemaBuilder, QuoteLocator, FieldResolver, DocumentExtractor
  output/       output records (match schema/extraction.schema.json), OutputStore, schema validation
  duplicates/   DuplicateDetector
  review/       review decisions, stored in reviews/ apart from out/
  api/          IntakeController and DocumentService
  config/       IntakeProperties (application.yaml) and the LLM client wiring
  cli/          the --pipeline entry point
src/main/resources/prompts/system-prompt.md   the system prompt
ui/src/
  api/          typed API client
  components/   ReviewScreen, FieldPanel, FieldRow, FieldEditor, DocumentText, DuplicatesView, ...
  lib/          catalog helpers, risk ranking, highlight segments, keyboard shortcuts
```

## Known limitations

Details are in [NOTES.md](NOTES.md#what-is-missing-or-would-come-next).

- **D04, infant's start date.** It copies the mother's start date where it should be `unknown`. A general prompt fix was written, but the free-tier quota ran out before D04 could be re-recorded.
- **No vision path.** S03, an image-only PDF, is flagged `unsupported_format` and not extracted.
- **Adding a provider takes a little code.** Switching between providers that already exist is configuration only (`intake.llm.provider`). A new provider needs an `LlmClient` implementation plus one branch in `config/IntakeConfiguration.java`.
- **Reviews have no user identity or audit history.**

See [NOTES.md](NOTES.md) for results, decisions and AI tool use, and [TESTING.md](TESTING.md) for how to check every feature.
