import { useEffect, useMemo, useRef } from 'react';
import { buildSegments, fieldForSegment, type Span } from '../lib/highlights';
import type { Risk } from '../lib/risk';

type Props = {
  text: string;
  spans: Span[];
  /** The risk of each field that has quotes, to colour its highlights. */
  riskByField: Record<string, Risk>;
  selectedFieldId?: string;
  onSelectField?: (fieldId: string) => void;
};

const prefersReducedMotion = () => window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;

/**
 * The document with every quote of the current case highlighted. The selected field's quotes
 * stand out and scroll into view; clicking a highlight selects its field.
 */
export function DocumentText({ text, spans, riskByField, selectedFieldId, onSelectField }: Props) {
  const segments = useMemo(() => buildSegments(text, spans), [text, spans]);
  const containerRef = useRef<HTMLPreElement>(null);

  useEffect(() => {
    if (!selectedFieldId) {
      return;
    }
    const first = containerRef.current?.querySelector('[data-selected="true"]');
    first?.scrollIntoView?.({ block: 'center', behavior: prefersReducedMotion() ? 'auto' : 'smooth' });
  }, [selectedFieldId, segments]);

  return (
    <pre className="document-text" ref={containerRef}>
      {segments.map((segment) => {
        if (segment.fieldIds.length === 0) {
          return <span key={segment.start}>{segment.text}</span>;
        }
        const fieldId = fieldForSegment(segment, spans);
        const selected = selectedFieldId !== undefined && segment.fieldIds.includes(selectedFieldId);
        const risk = fieldId ? riskByField[fieldId] : undefined;
        return (
          <mark
            key={segment.start}
            className={`quote quote--${risk ?? 'high'} ${selected ? 'is-selected' : ''}`}
            data-selected={selected}
            onClick={() => fieldId && onSelectField?.(fieldId)}
          >
            {segment.text}
          </mark>
        );
      })}
    </pre>
  );
}
