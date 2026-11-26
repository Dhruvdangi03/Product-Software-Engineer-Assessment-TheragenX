// Types of the REST API. They mirror the backend records (snake_case, as served).
// Nothing here names a catalog field: fields are always looked up through the catalog.

export type FieldType =
  | 'string'
  | 'integer'
  | 'number'
  | 'date'
  | 'coded'
  | 'coded_multi'
  | 'string_list';

export interface FieldOption {
  code: string;
  label: string;
}

export interface Validation {
  pattern?: string;
  min?: number;
  max?: number;
}

export interface FieldDefinition {
  id: string;
  label: string;
  section: string;
  type: FieldType;
  options: FieldOption[];
  validation: Validation;
  guidance?: string;
}

export interface Section {
  id: string;
  label: string;
}

export interface Catalog {
  catalog_version: string;
  sections: Section[];
  fields: FieldDefinition[];
}

export type FieldStatus = 'extracted' | 'unknown' | 'conflict';
export type Confidence = 'high' | 'medium' | 'low';

/** A value as JSON: text, number or a list of texts, depending on the field type. */
export type FieldValue = string | number | string[];

export interface Provenance {
  quote: string;
  /** Offset in Unicode code points, inclusive. */
  start: number;
  /** Offset in Unicode code points, exclusive. */
  end: number;
}

export interface FieldResult {
  status: FieldStatus;
  value?: FieldValue;
  confidence?: Confidence;
  provenance?: Provenance[];
  note?: string;
}

export interface ExtractedCase {
  case_index: number;
  label?: string;
  fields: Record<string, FieldResult>;
}

export interface DocumentExtraction {
  document_id: string;
  catalog_version: string;
  model?: string;
  flags: string[];
  cases: ExtractedCase[];
}

export type ReviewDecision = 'accepted' | 'rejected' | 'edited';
export type ReviewAction = 'accept' | 'reject' | 'edit' | 'reset';

export interface FieldReview {
  decision: ReviewDecision;
  value?: FieldValue;
  reviewed_at: string;
}

export interface DocumentReview {
  /** Keyed by case index, then by field id. */
  cases: Record<string, Record<string, FieldReview>>;
}

export interface ReviewProgress {
  reviewed: number;
  total: number;
}

export interface DocumentSummary {
  id: string;
  title: string;
  processed: boolean;
  case_count: number;
  flags: string[];
  review_progress: ReviewProgress;
}

export interface DocumentDetail {
  id: string;
  title: string;
  text?: string;
  extraction?: DocumentExtraction;
  review: DocumentReview;
  review_progress: ReviewProgress;
}

export type ComparisonOutcome = 'same' | 'different' | 'not_compared';

export interface FieldComparison {
  field_id: string;
  label: string;
  section: string;
  outcome: ComparisonOutcome;
}

export interface DuplicateView {
  document_ids: [string, string];
  case_indexes: [number, number];
  confidence?: Confidence;
  reason?: string;
  fields: FieldComparison[];
}
