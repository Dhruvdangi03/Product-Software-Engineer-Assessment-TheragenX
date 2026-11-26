import { useMemo, useState } from 'react';
import { api } from '../api/client';
import type { Catalog, DocumentDetail, DocumentReview, FieldValue, ReviewAction } from '../api/types';
import { fieldsInDisplayOrder } from '../lib/catalog';
import { progressOf, riskByField, spansOf } from '../lib/review';
import { riskOf } from '../lib/risk';
import { useKeyboardShortcuts } from '../lib/useKeyboardShortcuts';
import { CaseSwitcher } from './CaseSwitcher';
import { DocumentText } from './DocumentText';
import { FieldPanel, type RiskFilter } from './FieldPanel';
import { FlagList } from './FlagList';
import { NoCaseState } from './NoCaseState';
import { ProgressBar } from './ProgressBar';
import { ShortcutsHelp } from './ShortcutsHelp';
import { StateMessage } from './StateMessage';

type Props = {
  catalog: Catalog;
  detail: DocumentDetail;
  onReviewSaved: (review: DocumentReview) => void;
};

/** Document and fields side by side, for one document. Holds the selection and review state. */
export function ReviewScreen({ catalog, detail, onReviewSaved }: Props) {
  const cases = detail.extraction?.cases ?? [];
  const [caseIndex, setCaseIndex] = useState(0);
  const [selectedFieldId, setSelectedFieldId] = useState<string>();
  const [editingFieldId, setEditingFieldId] = useState<string>();
  const [filter, setFilter] = useState<RiskFilter>('all');
  const [errorByField, setErrorByField] = useState<Record<string, string>>({});
  const [showShortcuts, setShowShortcuts] = useState(false);

  const current = cases[caseIndex];
  const reviews = detail.review.cases[String(caseIndex)] ?? {};
  const spans = useMemo(() => (current ? spansOf(catalog, current) : []), [catalog, current]);
  const risks = useMemo(() => (current ? riskByField(catalog, current) : {}), [catalog, current]);

  /** The fields the panel shows, in its order: what next and previous move through. */
  const visibleFieldIds = fieldsInDisplayOrder(catalog)
    .filter((field) => filter === 'all' || riskOf(current?.fields[field.id]) === filter)
    .map((field) => field.id);

  const select = (fieldId: string | undefined) => {
    setSelectedFieldId(fieldId);
    setEditingFieldId(undefined);
  };

  const move = (step: number) => {
    if (visibleFieldIds.length === 0) {
      return;
    }
    const position = selectedFieldId ? visibleFieldIds.indexOf(selectedFieldId) : -1;
    const next = position < 0 ? (step > 0 ? 0 : visibleFieldIds.length - 1) : position + step;
    select(visibleFieldIds[Math.min(Math.max(next, 0), visibleFieldIds.length - 1)]);
  };

  const switchCase = (index: number) => {
    if (index >= 0 && index < cases.length) {
      setCaseIndex(index);
      select(undefined);
    }
  };

  const review = async (fieldId: string, action: ReviewAction, value?: FieldValue) => {
    try {
      const updated = await api.reviewField(detail.id, caseIndex, fieldId, action, value);
      setErrorByField(({ [fieldId]: _cleared, ...rest }) => rest);
      setEditingFieldId(undefined);
      onReviewSaved(updated);
      // Accepting or rejecting moves on, so a reviewer can go through a case with the keyboard alone.
      if (action === 'accept' || action === 'reject') {
        move(1);
      }
    } catch (error) {
      setErrorByField((errors) => ({ ...errors, [fieldId]: error instanceof Error ? error.message : String(error) }));
    }
  };

  const onSelected = (handler: (fieldId: string) => void) => () => {
    if (selectedFieldId && current) {
      handler(selectedFieldId);
    }
  };

  useKeyboardShortcuts({
    j: () => move(1),
    ArrowDown: () => move(1),
    k: () => move(-1),
    ArrowUp: () => move(-1),
    a: onSelected((fieldId) => review(fieldId, 'accept')),
    r: onSelected((fieldId) => review(fieldId, 'reject')),
    e: onSelected((fieldId) => setEditingFieldId(fieldId)),
    u: onSelected((fieldId) => review(fieldId, 'reset')),
    '[': () => switchCase(caseIndex - 1),
    ']': () => switchCase(caseIndex + 1),
    Escape: () => (editingFieldId ? setEditingFieldId(undefined) : select(undefined)),
    '?': () => setShowShortcuts((shown) => !shown),
  });

  return (
    <article className="review-screen">
      <header className="review-screen__header">
        <div className="review-screen__titles">
          <p className="review-screen__eyebrow">
            {detail.id}
            {detail.extraction?.model && <span> · extracted by {detail.extraction.model}</span>}
          </p>
          <h2 className="review-screen__title">{detail.title}</h2>
          <FlagList flags={detail.extraction?.flags ?? []} />
        </div>
        <div className="review-screen__meta">
          {cases.length > 0 && <ProgressBar progress={progressOf(catalog, detail.extraction, detail.review)} />}
          <button type="button" className="button button--quiet" onClick={() => setShowShortcuts((shown) => !shown)}>
            Shortcuts <kbd>?</kbd>
          </button>
        </div>
      </header>

      {showShortcuts && <ShortcutsHelp onClose={() => setShowShortcuts(false)} />}
      <CaseSwitcher cases={cases} activeIndex={caseIndex} onChange={switchCase} />

      <div className="review-screen__panes">
        <section className="pane pane--document" aria-label="Document text">
          {detail.text === undefined ? (
            <StateMessage tone="empty" title="No text to show">
              This document exists only as an image-only PDF. The pipeline flagged it as an unsupported format.
            </StateMessage>
          ) : (
            <DocumentText
              text={detail.text}
              spans={spans}
              riskByField={risks}
              selectedFieldId={selectedFieldId}
              onSelectField={select}
            />
          )}
        </section>

        <section className="pane pane--fields" aria-label="Extracted fields">
          {!detail.extraction && (
            <StateMessage tone="empty" title="Not processed yet">
              Run the pipeline to extract this document.
            </StateMessage>
          )}
          {detail.extraction && !current && <NoCaseState flags={detail.extraction.flags} />}
          {current && (
            <FieldPanel
              catalog={catalog}
              extractedCase={current}
              reviews={reviews}
              filter={filter}
              onFilterChange={setFilter}
              selectedFieldId={selectedFieldId}
              editingFieldId={editingFieldId}
              errorByField={errorByField}
              onSelectField={(fieldId) => select(selectedFieldId === fieldId ? undefined : fieldId)}
              onReview={review}
              onStartEdit={setEditingFieldId}
              onCancelEdit={() => setEditingFieldId(undefined)}
            />
          )}
        </section>
      </div>
    </article>
  );
}
