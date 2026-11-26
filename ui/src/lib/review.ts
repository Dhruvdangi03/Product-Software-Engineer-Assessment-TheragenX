import type { Catalog, DocumentExtraction, DocumentReview, ExtractedCase, ReviewProgress } from '../api/types';
import type { Span } from './highlights';
import { riskOf, type Risk } from './risk';

/** Reviewed fields out of all fields of all cases. Computed here so it updates as soon as a review is saved. */
export function progressOf(catalog: Catalog, extraction: DocumentExtraction | undefined, review: DocumentReview): ReviewProgress {
  const cases = extraction?.cases ?? [];
  const fieldIds = new Set(catalog.fields.map((field) => field.id));
  const reviewed = cases.reduce((count, extractedCase) => {
    const decisions = review.cases[String(extractedCase.case_index)] ?? {};
    return count + Object.keys(decisions).filter((fieldId) => fieldIds.has(fieldId)).length;
  }, 0);
  return { reviewed, total: cases.length * catalog.fields.length };
}

/** Every quote of every catalog field of a case, for highlighting. */
export function spansOf(catalog: Catalog, extractedCase: ExtractedCase): Span[] {
  return catalog.fields.flatMap((field) =>
    (extractedCase.fields[field.id]?.provenance ?? []).map((quote) => ({ fieldId: field.id, start: quote.start, end: quote.end })),
  );
}

export function riskByField(catalog: Catalog, extractedCase: ExtractedCase): Record<string, Risk> {
  return Object.fromEntries(catalog.fields.map((field) => [field.id, riskOf(extractedCase.fields[field.id])]));
}
