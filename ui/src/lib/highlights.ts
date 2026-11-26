/** A quote of one field, in code points of the document text. */
export interface Span {
  fieldId: string;
  start: number;
  end: number;
}

/** A run of text with the same set of quotes over it. */
export interface Segment {
  text: string;
  start: number;
  end: number;
  fieldIds: string[];
}

/**
 * Cuts the text at every quote boundary, so each segment is covered by a fixed set of fields.
 * Positions are Unicode code points, as in the backend output, so we index the text as code
 * points (Array.from) rather than UTF-16 units (string indexes).
 */
export function buildSegments(text: string, spans: Span[]): Segment[] {
  const codePoints = Array.from(text);
  const valid = spans.filter((span) => span.start >= 0 && span.end <= codePoints.length && span.start < span.end);
  const cuts = new Set<number>([0, codePoints.length]);
  valid.forEach((span) => {
    cuts.add(span.start);
    cuts.add(span.end);
  });
  const boundaries = [...cuts].sort((a, b) => a - b);

  const segments: Segment[] = [];
  for (let i = 0; i < boundaries.length - 1; i++) {
    const start = boundaries[i]!;
    const end = boundaries[i + 1]!;
    const fieldIds = valid.filter((span) => span.start <= start && span.end >= end).map((span) => span.fieldId);
    segments.push({ text: codePoints.slice(start, end).join(''), start, end, fieldIds: [...new Set(fieldIds)] });
  }
  return segments;
}

/**
 * The field a click on a segment selects: of the quotes covering it, the shortest one,
 * because it is the most specific (a date quote inside a longer sentence quote).
 */
export function fieldForSegment(segment: Segment, spans: Span[]): string | undefined {
  const covering = spans.filter((span) => span.start <= segment.start && span.end >= segment.end);
  covering.sort((a, b) => a.end - a.start - (b.end - b.start));
  return covering[0]?.fieldId;
}
