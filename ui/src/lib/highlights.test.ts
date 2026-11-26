import { describe, expect, it } from 'vitest';
import { buildSegments, fieldForSegment, type Span } from './highlights';

describe('buildSegments', () => {
  it('cuts the text at every quote boundary and keeps all text', () => {
    const segments = buildSegments('Mrs M.K., 54 years', [{ fieldId: 'age', start: 10, end: 12 }]);

    expect(segments.map((s) => s.text)).toEqual(['Mrs M.K., ', '54', ' years']);
    expect(segments[1]?.fieldIds).toEqual(['age']);
    expect(segments.map((s) => s.text).join('')).toBe('Mrs M.K., 54 years');
  });

  it('counts positions in code points, as the backend does', () => {
    // The emoji is one code point but two UTF-16 units.
    const segments = buildSegments('\u{1F600} rash', [{ fieldId: 'event', start: 2, end: 6 }]);

    expect(segments.find((s) => s.fieldIds.length > 0)?.text).toBe('rash');
  });

  it('marks overlapping quotes with every field that covers them', () => {
    const spans: Span[] = [
      { fieldId: 'sentence', start: 0, end: 20 },
      { fieldId: 'date', start: 8, end: 14 },
    ];

    const overlap = buildSegments('Started 2 May 2026 here', spans).find((s) => s.start === 8);

    expect(overlap?.fieldIds).toEqual(['sentence', 'date']);
    expect(fieldForSegment(overlap!, spans)).toBe('date');
  });

  it('ignores spans outside the text', () => {
    expect(buildSegments('short', [{ fieldId: 'x', start: 3, end: 99 }])).toHaveLength(1);
  });
});
