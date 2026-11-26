import type { FieldDefinition, FieldResult, FieldReview, FieldValue, ReviewAction } from '../api/types';
import { asList, formatValue } from '../lib/catalog';
import { riskOf } from '../lib/risk';
import { FieldEditor } from './FieldEditor';
import { ReviewBadge } from './ReviewBadge';
import { RiskBadge } from './RiskBadge';

type Props = {
  field: FieldDefinition;
  result?: FieldResult;
  review?: FieldReview;
  selected: boolean;
  editing: boolean;
  error?: string;
  onSelect: () => void;
  onReview: (action: ReviewAction, value?: FieldValue) => void;
  onStartEdit: () => void;
  onCancelEdit: () => void;
};

export function FieldRow({ field, result, review, selected, editing, error, onSelect, onReview, onStartEdit, onCancelEdit }: Props) {
  const risk = riskOf(result);
  const decision = review?.decision;

  return (
    <li className={`field-row field-row--${risk} ${selected ? 'is-selected' : ''} ${decision ? `is-${decision}` : ''}`}>
      <button
        type="button"
        className="field-row__summary"
        aria-expanded={selected}
        data-field-row
        onClick={onSelect}
      >
        <span className="field-row__label">{field.label}</span>
        <span className="field-row__value">
          <DisplayedValue field={field} result={result} review={review} />
        </span>
        <span className="field-row__badges">
          {decision && <ReviewBadge decision={decision} />}
          <RiskBadge risk={risk} />
        </span>
      </button>

      {selected && (
        <div className="field-row__details">
          {result?.note && <p className="field-row__note">{result.note}</p>}
          {result?.provenance && result.provenance.length > 0 && (
            <ul className="field-row__quotes" aria-label="Quotes from the document">
              {result.provenance.map((quote) => (
                <li key={`${quote.start}-${quote.end}`}>
                  <q>{quote.quote}</q>
                </li>
              ))}
            </ul>
          )}
          {(field.type === 'coded' || field.type === 'coded_multi') && <OptionList field={field} result={result} review={review} />}
          {field.guidance && <p className="field-row__guidance">{field.guidance}</p>}

          {editing ? (
            <FieldEditor
              field={field}
              initialValue={review?.value ?? result?.value}
              error={error}
              onSave={(value) => onReview('edit', value)}
              onCancel={onCancelEdit}
            />
          ) : (
            <ReviewActions decision={decision} canAccept={result !== undefined} error={error} onReview={onReview} onStartEdit={onStartEdit} />
          )}
        </div>
      )}
    </li>
  );
}

function DisplayedValue({ field, result, review }: { field: FieldDefinition; result?: FieldResult; review?: FieldReview }) {
  if (review?.decision === 'edited') {
    return (
      <>
        <span className="value value--edited">{formatValue(field, review.value)}</span>
        {result?.value !== undefined && <s className="value value--replaced">{formatValue(field, result.value)}</s>}
      </>
    );
  }
  if (!result || result.status === 'unknown') {
    return <span className="value value--empty">Not stated</span>;
  }
  if (result.status === 'conflict') {
    return <span className="value value--conflict">Document contradicts itself</span>;
  }
  const text = formatValue(field, result.value);
  return review?.decision === 'rejected' ? <s className="value">{text}</s> : <span className="value">{text}</span>;
}

/** Every option of a coded field, with the current choice marked, so the reviewer sees the alternatives. */
function OptionList({ field, result, review }: { field: FieldDefinition; result?: FieldResult; review?: FieldReview }) {
  const chosen = asList(review?.decision === 'edited' ? review.value : result?.value);
  return (
    <ul className="option-list" aria-label={`Options for ${field.label}`}>
      {field.options.map((option) => (
        <li key={option.code} className={chosen.includes(option.code) ? 'is-chosen' : ''}>
          {option.label}
        </li>
      ))}
    </ul>
  );
}

type ActionsProps = {
  decision?: FieldReview['decision'];
  canAccept: boolean;
  error?: string;
  onReview: (action: ReviewAction) => void;
  onStartEdit: () => void;
};

function ReviewActions({ decision, canAccept, error, onReview, onStartEdit }: ActionsProps) {
  return (
    <>
      <div className="review-actions">
        <button type="button" className="button button--accept" disabled={!canAccept} onClick={() => onReview('accept')}>
          Accept <kbd>A</kbd>
        </button>
        <button type="button" className="button button--reject" onClick={() => onReview('reject')}>
          Reject <kbd>R</kbd>
        </button>
        <button type="button" className="button" onClick={onStartEdit}>
          Edit <kbd>E</kbd>
        </button>
        {decision && (
          <button type="button" className="button button--quiet" onClick={() => onReview('reset')}>
            Undo <kbd>U</kbd>
          </button>
        )}
      </div>
      {error && (
        <p className="field-editor__error" role="alert">
          {error}
        </p>
      )}
    </>
  );
}
