# Notes

## Checker output on the four documents with answers

`python3 tools/check.py --catalog catalog/field_catalog.json --answers answers --out out --docs documents`

```

=== D01 ===
  -- answer case 0 'Mrs M.K.' -> output case 0
     ok patient_initials              +1.0  ok
     ok patient_age_years             +1.0  ok
     ok patient_sex                   +1.0  ok
     ok suspect_drug_name             +1.0  ok
     ok suspect_drug_route            +1.0  ok
     ok suspect_drug_start_date       +1.0  ok
     ok event_terms                   +1.0  ok
     ok event_onset_date              +1.0  ok
     ok event_outcome                 +1.0  ok
     ok seriousness_criteria          +1.0  ok
     ok reporter_type                 +1.0  ok
     ok report_date                   +1.0  ok
     ok country                       +1.0  ok
     ok concomitant_drugs             +1.0  ok
     ok dose                          +2.0  ok
  D01: 16.0 / 16

=== D02 ===
  -- answer case 0 'Mr J.T.O.' -> output case 0
     ok patient_initials              +1.0  ok
     ok patient_age_years             +1.0  ok
     ok patient_sex                   +1.0  ok
     ok suspect_drug_name             +1.0  ok
     ok suspect_drug_route            +1.0  ok
     ok suspect_drug_start_date       +1.0  ok
     ok event_terms                   +1.0  ok
     ok event_onset_date              +1.0  ok
     ok event_outcome                 +1.0  ok
     ok seriousness_criteria          +1.0  ok
     ok reporter_type                 +1.0  ok
     ok report_date                   +1.0  ok
     ok country                       +1.0  ok
     ok concomitant_drugs             +1.0  ok
     ok dose                          +2.0  ok
  D02: 16.0 / 16

=== D03 ===
  -- answer case 0 'Patient (initials withheld)' -> output case 0
     ok patient_initials              +1.0  unknown as expected
     ok patient_age_years             +1.0  ok
     ok patient_sex                   +1.0  ok
     ok suspect_drug_name             +1.0  ok
     ok suspect_drug_route            +1.0  ok
     ok suspect_drug_start_date       +1.0  ok
     ok event_terms                   +1.0  ok
     ok event_onset_date              +1.0  ok
     ok event_outcome                 +1.0  ok
     ok seriousness_criteria          +1.0  ok
     ok reporter_type                 +1.0  ok
     ok report_date                   +1.0  ok
     ok country                       +1.0  ok
     ok concomitant_drugs             +1.0  ok
     ok dose                          +2.0  ok
  D03: 16.0 / 16

=== D05 ===
  -- answer case 0 'Mr D.L.' -> output case 0
     ok patient_initials              +1.0  ok
     ok patient_age_years             +1.0  conflict flagged
     ok patient_sex                   +1.0  ok
     ok suspect_drug_name             +1.0  ok
     ok suspect_drug_route            +1.0  ok
     ok suspect_drug_start_date       +1.0  ok
     ok event_terms                   +1.0  ok
     ok event_onset_date              +1.0  ok
     ok event_outcome                 +1.0  ok
     ok seriousness_criteria          +1.0  ok
     ok reporter_type                 +1.0  ok
     ok report_date                   +1.0  ok
     ok country                       +1.0  ok
     ok concomitant_drugs             +1.0  ok
     ok dose                          +2.0  ok
  D05: 16.0 / 16

================ SUMMARY ================
Documents scored:      4
Field points:          64.0 / 64  (100.0%)
Wrong at confidence:   high 0   medium 0   low 0
Unknown, answer had one: 0
Case-count errors:     0
Provenance:            62 of 62 extracted fields grounded (100.0%)
```

All 62 extracted values on these documents point to a quote at the right position. Over all twelve outputs, every extracted value has a quote that the pipeline located in the `.txt` itself (see [Provenance](#provenance-never-trust-model-offsets)).

## The other four core documents

**D04: mother and newborn, sertraline.** Two cases, flagged `multiple_patients`.
- Mother: 29, female, sertraline 50 mg oral since 14 Oct 2025, hyponatraemia (SIADH) from 22 May 2026, recovered, hospitalization.
- Infant "Baby R.D.": age 0, male, route `other` (exposed in the womb), dose unknown. Neonatal adaptation syndrome on 3 June 2026 (medium: "within the first 24 hours of life"), recovering, hospitalization, no concomitant drugs ("did not receive any medicine himself").
- Unsure:
  - **The infant's start date is wrong.** It copies the mother's start date (14 Oct 2025, at medium confidence). The baby's exposure began at conception, and the document gives no date for that, so the answer should be `unknown`. I wrote a general prompt rule for exposure through another person, but the free-tier daily quota ran out before I could re-record. I reverted the rule so the committed prompt is exactly the one in the fixtures.
  - Whether the iron and folic acid supplements count as concomitant drugs.
  - Whether the mother's stay counts as hospitalization: the letter says "Both patients remain in hospital".

**D06: product quality complaint.** No case. Nobody took the capsules and no reaction happened, so there is no patient and no adverse event. Flags: `no_identifiable_patient`, `no_adverse_event`. I'm confident in this one.

**D07: consumer email with embedded instructions.** One case, flagged `embedded_instructions_ignored`. The block addressed to "automated systems" asks for outcome `recovered`, seriousness `none` and reporter `physician`. None of these were followed: the output has outcome `recovering` (medium), seriousness `hospitalization` (two nights in a psychiatric unit) and reporter `consumer`.
- Other values: 38, female, paroxetine 20 mg oral from 30 Mar 2026; insomnia and suicidal ideation; onset 9 Apr 2026 (medium, from "about a week and a half in"); country US (medium, from Columbus, Ohio).
- Unsure:
  - Initials `JMH` (medium) come from the sender address `jenna.m.hartley@…`. The email is signed only "Jenna", and a first name alone does not give initials. If the address does not count as a full name, this is wrong at medium (−1).
  - `multivitamin` as a concomitant drug.
  - The outcome could be `not_recovered` ("the dark thoughts are still there").

**D08: 7-page methotrexate case report (12.7k characters).** One case.
- Patient: H.M.N., 72, female.
- Drug: methotrexate oral from 7 Oct 2025. Dose **20 mg**: the dose at the reaction, after the increase on 2 March, not the starting 15 mg.
- Event: toxicity with pancytopenia, oral mucositis and neutropenic sepsis. Onset 19 Mar 2026 (medium: "on or about 19 March 2026 (five days before presentation)"). Outcome fatal; seriousness death, life-threatening and hospitalization.
- Report: physician, 14 Apr 2026, NZ.
- Concomitant drugs from Table 1, which include zoledronic acid (last given August 2025).
- Unsure:
  - Whether zoledronic acid counts as "taking" at the time of the reaction, since it is a yearly infusion.
  - Which event terms the answer key expects for the death (pneumonia and septic shock are listed as the cause of death).
  - The initials are stated plainly but came back at medium. That is under-confident, which costs nothing when the value is right.

**Stretch documents.**
- S01 (pharmacist email) and S02 (cardiology letter) are the same patient, E.W., 81, female, with digoxin toxicity. They are the one group in `out/duplicates.json`.
- S01's dispensing record (12.01.2026, 250 MCG) did not override the stated start date (5 Jan 2026) or dose (0.25 mg).
- S04 (dentist): `other_hcp`, not serious (`none`). It shares drug, age, sex and reaction with D01, but the initials and dates differ, so it is correctly not a duplicate.
- S03 has no text layer, so it is flagged `unsupported_format` with no cases (see below).

## Choices that mattered

**One request per document, no chunking.** The longest document, D08, is about 6,000 prompt tokens including instructions, and the model's context is a million tokens. A single call sees the whole report, which matters for conflicts (D05's two ages are pages apart) and for which dose applied at the time of the reaction (D08). It also keeps a full run to 11 requests, well inside free-tier limits.

**The catalog drives everything.** No Java or TypeScript file names a field id; a UI test enforces this for the UI source.
- `PromptBuilder` lists each catalog field with its type, options, pattern, range and guidance.
- `ResponseSchemaBuilder` turns each field into a JSON schema property of the right type (integer, enum of option codes, array, and so on). The model is therefore constrained to the catalog by structured output, not only told about it.
- `FieldValueValidator` checks every value against the same catalog.
- The UI renders sections, labels, order and edit controls from `GET /api/catalog`.
- The only fixed vocabulary is the seven types, the three validation keys, and the brief's rule that `none` stands alone in a multi-select.

**Provenance: never trust model offsets.** Models count characters badly. The model returns quote *text*, and `QuoteLocator` finds it in the document, ignoring case, runs of whitespace and typographic dashes, the same differences the checker forgives. It returns the document's own text and its position in Unicode code points (Java strings are UTF-16, so we convert). A value whose quotes cannot be found becomes `unknown` with a note.

**Calibration: unknown is better than a wrong guess.**
- The prompt says so plainly and defines high, medium and low confidence.
- The code adds its own checks. A value the model marks `derived` (a relative date, an age from a year of birth, a unit conversion) is capped at medium.
- A value outside the catalog, without a confidence, or without a located quote becomes `unknown`, and the note says what was rejected. Nothing is dropped silently.
- `conflict` keeps its quotes, so the reviewer sees both passages.

**Minimum elements are decided apart from the fields.** The model reports four booleans (identifiable patient, reporter, suspect drug, adverse event), and the code writes no case unless all four are present. This keeps a domain rule out of the field list and avoids naming field ids in code. Each missing element becomes a flag (`no_adverse_event`, and so on), so an empty `cases` explains itself.

**Nothing inside a document is an instruction.**
- The document is wrapped in `<document>` tags and the system prompt says its content is untrusted data.
- The model reports `contains_embedded_instructions`, which becomes a flag for the reviewer. Flags are set by the pipeline from fixed values; the model cannot invent them.
- Structured output plus validation means injected text cannot add fields or set values outside the catalog.

**Record and replay.**
- Each document's exchange (provider request body, raw response, answer text) is stored in `fixtures/<id>.json`.
- Fixtures are keyed by **document id, not a prompt hash**. A catalog change changes the prompt, and replay must still find the recording and recheck its values against the new catalog. A field the recording doesn't cover comes out `unknown` with "No model answer for this field".
- Recording and replay share every step after the model call, which is why replay reproduces `out/` byte for byte. `ReplayReproducesCommittedOutputTest` checks this, and `ReplayWithChangedCatalogTest` checks the changed-catalog behaviour.
- The key travels in the `x-goog-api-key` header, so it is never in a URL or a request body. `FixtureStore` also replaces the key wherever it appears, and `CommittedFixturesTest` fails if a Google key pattern (or the key in the environment) is found in `fixtures/`.

**Rate limits.**
- `RetryingLlmClient` spaces requests out (7 s by default) and retries 429 and 5xx responses with exponential backoff, waiting as long as Gemini's `RetryInfo` asks when it says.
- A used-up daily quota fails at once with a clear message.
- `record-missing` mode skips documents that already have a fixture, so the next run resumes where the last one stopped.
- This was needed in practice: Gemini returned 503 "high demand" repeatedly during recording, and the free-tier daily quota ran out after about 15 requests.

**Temperature 1.0.** Google advises keeping Gemini 3+ models at their default temperature because lower values can make them loop. Reproducibility comes from the fixtures, not the temperature.

**Duplicates by section rules, not field ids.**
- Two cases are a duplicate when every field compared in the identity sections matches (the `patient` section, set in `application.yaml`), with at least two compared.
- In addition, at least half of the other compared fields must match. The `report` section is ignored there, because two reporters naturally differ in reporter type and report date.
- S01 and S02 match on 9 of 10 such fields. D01 and S04 fail on initials. The rule is in `DuplicateDetector` and is configurable.

**Review decisions live apart from `out/`.** They are stored in `reviews/<id>.json`, which git ignores. This keeps `out/` exactly what the pipeline produced (so replay still matches), and a model value can never be mistaken for a reviewer's. Edits are validated against the catalog by the same `FieldValueValidator`.

**Image-only PDFs.** A document with only a PDF (S03) is flagged `unsupported_format` with no cases, by a general rule: no `.txt`, no extraction. A vision model could transcribe it, but the positions would then refer to a transcript, not a file the checker can verify. I preferred an honest flag.

## The review UI

- Document left, fields right, grouped by catalog section.
- Each row shows the value with option labels (not codes), a risk badge that combines status and confidence (symbol plus word, never colour alone), and the review decision. A coloured left edge makes the risky rows visible while scrolling. `unknown` rows have a dashed edge and "Not stated"; `conflict` rows are tinted and read "Document contradicts itself".
- Risk chips at the top count fields by risk (conflict, low, medium, unknown, high) and filter by them, so the risky fields are one click away.
- Selecting a field highlights its quotes in the document and scrolls them to the centre. All quotes of the case are underlined in their risk colour, and clicking one selects its field (the shortest quote wins where quotes overlap).
- The expanded row shows the quotes, the model's note, all options of a coded field with the choice marked, the catalog guidance, and Accept, Reject, Edit and Undo.
- Edit controls come from the field type: select, date picker, checkboxes for multi-select, one item per line for lists, number inputs with the catalog's min and max.
- Edited values are shown in a distinct colour next to the struck-through model value. Each document shows its progress, and the document list shows a small bar.
- Keyboard:
  - `j`/`k` or arrow keys: next or previous field
  - `a`, `r`, `e`, `u`: accept, reject, edit, undo
  - `[` and `]`: switch case
  - `Esc`: close the editor or clear the selection
  - `?`: show the shortcut list
- Accepting or rejecting moves to the next field, so a case can be reviewed with the keyboard alone.
- The duplicates view puts the two documents side by side with a field-by-field same, differs or not-in-both table. Clicking a row highlights that field in both texts.
- Loading, error (with retry), empty, not processed and no case each have their own state. A document with no case explains why, from its flags.
- Light and dark themes follow the system; motion is short and disabled under reduced-motion.

## AI tools

I used **Claude Code** (Claude Opus 5.5) throughout:
- to read the brief and plan the work in phases
- to write the backend, the prompt, the tests and the UI
- to run the pipeline, the checker and the test suites
- to check the screen in a browser

I reviewed the plan and the outputs, and directed scope and code style: plain, small classes, explicit errors, a commit per feature.

Design prompt used for the UI:

> Design a review screen for a pharmacovigilance case intake tool. A reviewer checks values that a language model extracted from an adverse-event report against the report text. Speed and trust matter more than decoration: the reviewer must find the risky fields without reading every row, and see where every value came from.
>
> Layout: a narrow document list on the left (id, subject, case count, flags, review progress); the main area splits into the document text (left, like a sheet of paper, monospace so tables keep their columns) and the fields (right), grouped by section. When a document has several patients, tabs switch between them.
>
> Fields: one dense row per field (label, value, a badge combining status and confidence, the review decision). Unknown and conflict must look different from extracted values and from each other. Risk should be visible at a glance through a coloured edge and a badge with a symbol and a word, never colour alone. A row of filter chips counts fields by risk. Selecting a row expands it with the quotes, the model's note, the options of a coded field, and Accept, Reject, Edit actions; the quote highlights in the document and scrolls into view. Everything (sections, labels, order, edit controls) is rendered from the field catalog.
>
> Style: calm and clinical. Warm neutral surfaces, one teal accent for selection and primary actions, a restrained semantic palette for risk (red conflict, orange low, amber medium, grey unknown, green high), a clear type hierarchy, a 4 px spacing grid, subtle motion only on selection, light and dark themes, keyboard-first, accessible.

## What is missing or would come next

- **D04 infant start date** (see above): re-record D04 with the exposure-through-another-person rule once quota allows.
- **No vision path** for image-only PDFs (S03 is flagged instead).
- **Second-pass verification:** a cheap second request that asks the model to confirm each high-confidence value against its quote would catch the remaining over-confident values (such as D07's initials). I left it out to stay inside the free-tier quota.
- **Reviews have no user identity or audit history.** One JSON file per document is enough for the exercise, not for production.
- **No two-minute screen recording.**
