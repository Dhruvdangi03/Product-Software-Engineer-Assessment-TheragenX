import { useState } from 'react';
import { api } from '../api/client';
import type { Catalog, ComparisonOutcome, DocumentDetail, DuplicateView, ExtractedCase, FieldValue } from '../api/types';
import { formatValue, groupBySection } from '../lib/catalog';
import { riskByField, spansOf } from '../lib/review';
import { useAsync } from '../lib/useAsync';
import { caseLabel } from './CaseSwitcher';
import { DocumentText } from './DocumentText';
import { StateMessage } from './StateMessage';

const OUTCOME_LABELS: Record<ComparisonOutcome, string> = {
  same: 'Same',
  different: 'Differs',
  not_compared: 'Not in both',
};

export function DuplicatesView({ catalog }: { catalog: Catalog }) {
  const [state, reload] = useAsync(() => api.duplicates(), []);

  if (state.status === 'loading') {
    return <StateMessage tone="loading" title="Loading duplicates…" />;
  }
  if (state.status === 'error') {
    return (
      <StateMessage tone="error" title="Could not load duplicates" onRetry={reload}>
        {state.error.message}
      </StateMessage>
    );
  }
  if (state.data.length === 0) {
    return <StateMessage tone="empty" title="No duplicates found">No two documents describe the same case.</StateMessage>;
  }
  return (
    <div className="duplicates">
      {state.data.map((pair) => (
        <DuplicatePair key={pair.document_ids.join('-')} catalog={catalog} pair={pair} />
      ))}
    </div>
  );
}

/** Two documents side by side, with the fields that match and the fields that differ. */
function DuplicatePair({ catalog, pair }: { catalog: Catalog; pair: DuplicateView }) {
  const [idA, idB] = pair.document_ids;
  const [state, reload] = useAsync(() => Promise.all([api.document(idA), api.document(idB)]), [idA, idB]);
  const [selectedFieldId, setSelectedFieldId] = useState<string>();

  if (state.status === 'loading') {
    return <StateMessage tone="loading" title={`Loading ${idA} and ${idB}…`} />;
  }
  if (state.status === 'error') {
    return (
      <StateMessage tone="error" title={`Could not load ${idA} and ${idB}`} onRetry={reload}>
        {state.error.message}
      </StateMessage>
    );
  }

  const [documentA, documentB] = state.data;
  const caseA = documentA.extraction?.cases[pair.case_indexes[0]];
  const caseB = documentB.extraction?.cases[pair.case_indexes[1]];
  const outcomes = Object.fromEntries(pair.fields.map((field) => [field.field_id, field.outcome]));
  const counts = countOutcomes(pair);

  return (
    <article className="duplicate-pair">
      <header className="duplicate-pair__header">
        <p className="review-screen__eyebrow">
          Possible duplicate{pair.confidence && ` · ${pair.confidence} confidence`}
        </p>
        <h2 className="review-screen__title">
          {idA} and {idB} describe the same case
        </h2>
        {pair.reason && <p className="duplicate-pair__reason">{pair.reason}</p>}
        <p className="duplicate-pair__counts">
          <span className="outcome outcome--same">{counts.same} same</span>
          <span className="outcome outcome--different">{counts.different} differ</span>
          <span className="outcome outcome--not_compared">{counts.not_compared} not in both</span>
        </p>
      </header>

      <table className="comparison">
        <thead>
          <tr>
            <th scope="col">Field</th>
            <th scope="col">
              {idA}
              {caseA && <span className="comparison__case"> · {caseLabel(caseA)}</span>}
            </th>
            <th scope="col">
              {idB}
              {caseB && <span className="comparison__case"> · {caseLabel(caseB)}</span>}
            </th>
            <th scope="col">Match</th>
          </tr>
        </thead>
        {groupBySection(catalog).map(({ section, fields }) => (
          <tbody key={section.id}>
            <tr className="comparison__section">
              <th colSpan={4} scope="colgroup">
                {section.label}
              </th>
            </tr>
            {fields.map((field) => {
              const outcome = outcomes[field.id] ?? 'not_compared';
              return (
                <tr
                  key={field.id}
                  className={`comparison__row comparison__row--${outcome} ${selectedFieldId === field.id ? 'is-selected' : ''}`}
                  onClick={() => setSelectedFieldId(selectedFieldId === field.id ? undefined : field.id)}
                >
                  <th scope="row">{field.label}</th>
                  <td>{cellValue(caseA, field.id, (value) => formatValue(field, value))}</td>
                  <td>{cellValue(caseB, field.id, (value) => formatValue(field, value))}</td>
                  <td>
                    <span className={`outcome outcome--${outcome}`}>{OUTCOME_LABELS[outcome]}</span>
                  </td>
                </tr>
              );
            })}
          </tbody>
        ))}
      </table>

      <div className="duplicate-pair__texts">
        <SideText catalog={catalog} detail={documentA} extractedCase={caseA} selectedFieldId={selectedFieldId} onSelect={setSelectedFieldId} />
        <SideText catalog={catalog} detail={documentB} extractedCase={caseB} selectedFieldId={selectedFieldId} onSelect={setSelectedFieldId} />
      </div>
    </article>
  );
}

type SideTextProps = {
  catalog: Catalog;
  detail: DocumentDetail;
  extractedCase?: ExtractedCase;
  selectedFieldId?: string;
  onSelect: (fieldId: string) => void;
};

function SideText({ catalog, detail, extractedCase, selectedFieldId, onSelect }: SideTextProps) {
  return (
    <section className="pane pane--document" aria-label={`Text of ${detail.id}`}>
      <h3 className="pane__title">
        {detail.id} · {detail.title}
      </h3>
      {detail.text && (
        <DocumentText
          text={detail.text}
          spans={extractedCase ? spansOf(catalog, extractedCase) : []}
          riskByField={extractedCase ? riskByField(catalog, extractedCase) : {}}
          selectedFieldId={selectedFieldId}
          onSelectField={onSelect}
        />
      )}
    </section>
  );
}

function cellValue(extractedCase: ExtractedCase | undefined, fieldId: string, format: (value: FieldValue) => string) {
  const result = extractedCase?.fields[fieldId];
  if (!result || result.status !== 'extracted' || result.value === undefined) {
    return <span className="value value--empty">{result?.status === 'conflict' ? 'Conflict' : 'Not stated'}</span>;
  }
  return format(result.value);
}

function countOutcomes(pair: DuplicateView): Record<ComparisonOutcome, number> {
  const counts: Record<ComparisonOutcome, number> = { same: 0, different: 0, not_compared: 0 };
  pair.fields.forEach((field) => {
    counts[field.outcome] += 1;
  });
  return counts;
}
