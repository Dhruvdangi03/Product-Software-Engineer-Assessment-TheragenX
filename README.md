# Case Intake

Extracts structured adverse-event cases from free-text reports with a language model, and lets a reviewer check every value against the text it came from.

- **Pipeline** (Java 21, Spring Boot 3.5): reads `documents/`, asks the model for every field in `catalog/field_catalog.json`, checks each answer, and writes `out/<DOCUMENT_ID>.json`.
- **REST API** (Spring Boot): serves the catalog, the documents and the outputs, and records review decisions.
- **Review UI** (React 19, TypeScript, Vite): the document and its fields side by side.

Provider and model: **Google Gemini, `gemini-3.5-flash`**, which the API currently serves as `gemini-3.6-flash`. `gemini-2.5-flash` is no longer offered to new API keys.

## Start the backend and the UI from fixtures (no key needed)

Requirements: Java 21+, Node 20+, and `curl`.

```bash
./start.sh
```

Then open <http://localhost:5173>. The script installs the UI dependencies on its first run, builds the backend, starts it on port 8080 from the committed `out/`, and starts the UI on port 5173. Stop both with Ctrl+C.

To start them separately:

```bash
./mvnw spring-boot:run          # API on http://localhost:8080/api
npm --prefix ui ci && npm --prefix ui run dev   # UI on http://localhost:5173
```

## Rebuild `out/` from the recordings

```bash
./replay.sh                          # committed catalog
./replay.sh path/to/other_catalog.json
```

Replay needs no key and no network. It runs the whole pipeline on the recorded model answers in `fixtures/`. With a changed catalog, every recorded value is checked against the new catalog. A value that no longer passes, or a field the recordings don't cover, comes out `unknown` with a note saying why.

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

Every setting in `src/main/resources/application.yaml` can be overridden the same way, for example `--intake.llm.model=gemini-3-flash-preview` or `--intake.documents-dir=other/folder`.

## Tests

```bash
./mvnw test                       # backend: unit tests, replay reproduces out/, changed catalog, API
npm --prefix ui test              # UI
npm --prefix ui run typecheck     # npx tsc --noEmit
```

Both test suites run without network once the dependencies are installed.

See [TESTING.md](TESTING.md) for a step-by-step check of every feature.

## Score with the checker

```bash
python3 tools/check.py --catalog catalog/field_catalog.json --answers answers --out out --docs documents
```

## Layout

```
src/main/java/com/theragenx/intake/
  catalog/      catalog model, loader, FieldValueValidator (the only place types and rules are checked)
  llm/          LlmClient interface, GeminiClient, RetryingLlmClient, FixtureStore (record and replay)
  extraction/   ExtractionPipeline, PromptBuilder, ResponseSchemaBuilder, QuoteLocator, FieldResolver, DocumentExtractor
  output/       output records (match schema/extraction.schema.json), OutputStore, schema validation
  duplicates/   DuplicateDetector
  review/       review decisions, stored in reviews/ (git-ignored), apart from out/
  api/          IntakeController and DocumentService
  cli/          the --pipeline entry point
src/main/resources/prompts/system-prompt.md   the system prompt
ui/src/
  api/          typed API client
  components/   ReviewScreen, FieldPanel, FieldRow, FieldEditor, DocumentText, DuplicatesView, ...
  lib/          catalog helpers, risk ranking, highlight segments, keyboard shortcuts
fixtures/       recorded model exchanges, one per document
out/            pipeline outputs and duplicates.json
```

See [NOTES.md](NOTES.md) for results, decisions and known issues.
