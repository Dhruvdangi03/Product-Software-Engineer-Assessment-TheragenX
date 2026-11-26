import type { Catalog, DocumentDetail } from '../api/types';

/** A made-up catalog with one field of each kind, so tests never rely on the real catalog's ids. */
export const testCatalog: Catalog = {
  catalog_version: 'test-1',
  sections: [
    { id: 'person', label: 'Person' },
    { id: 'visit', label: 'Visit' },
  ],
  fields: [
    { id: 'code_name', label: 'Code name', section: 'person', type: 'string', options: [], validation: { pattern: '^[A-Z]{2}$' } },
    { id: 'colour', label: 'Colour', section: 'visit', type: 'coded', options: [{ code: 'red', label: 'Red' }, { code: 'blue', label: 'Blue' }], validation: {} },
    { id: 'visit_date', label: 'Visit date', section: 'visit', type: 'date', options: [], validation: {} },
    { id: 'tags', label: 'Tags', section: 'visit', type: 'coded_multi', options: [{ code: 'a', label: 'Tag A' }, { code: 'b', label: 'Tag B' }], validation: {} },
    { id: 'notes', label: 'Notes', section: 'visit', type: 'string_list', options: [], validation: {} },
  ],
};

export const TEST_TEXT = 'Patient AB came on 2 March 2026. Colour red.';

export const testDetail: DocumentDetail = {
  id: 'T1',
  title: 'Test document',
  text: TEST_TEXT,
  extraction: {
    document_id: 'T1',
    catalog_version: 'test-1',
    flags: [],
    cases: [
      {
        case_index: 0,
        label: 'Patient AB',
        fields: {
          code_name: { status: 'extracted', value: 'AB', confidence: 'high', provenance: [{ quote: 'Patient AB', start: 0, end: 10 }] },
          colour: { status: 'extracted', value: 'red', confidence: 'low', provenance: [{ quote: 'Colour red', start: 33, end: 43 }] },
          visit_date: { status: 'conflict', note: '2 or 3 March' },
          tags: { status: 'unknown' },
          notes: { status: 'extracted', value: [], confidence: 'medium', provenance: [{ quote: 'came on 2 March 2026', start: 11, end: 31 }] },
        },
      },
    ],
  },
  review: { cases: {} },
  review_progress: { reviewed: 0, total: 5 },
};
