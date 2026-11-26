import { useState, type FormEvent } from 'react';
import type { FieldDefinition, FieldValue } from '../api/types';
import { asList } from '../lib/catalog';

type Props = {
  field: FieldDefinition;
  initialValue?: FieldValue;
  error?: string;
  onSave: (value: FieldValue) => void;
  onCancel: () => void;
};

/**
 * The input control for a field, chosen by its catalog type: options for coded fields,
 * a date picker for dates, checkboxes for multi-select, one line per item for lists.
 * The backend validates the value against the catalog and its message is shown here.
 */
export function FieldEditor({ field, initialValue, error, onSave, onCancel }: Props) {
  const [draft, setDraft] = useState<FieldValue | undefined>(initialValue);

  const submit = (event: FormEvent) => {
    event.preventDefault();
    const value = toValue(field, draft);
    if (value !== undefined) {
      onSave(value);
    }
  };

  return (
    <form
      className="field-editor"
      onSubmit={submit}
      onKeyDown={(event) => {
        if (event.key === 'Escape') {
          event.stopPropagation();
          onCancel();
        }
      }}
    >
      <Control field={field} value={draft} onChange={setDraft} />
      {error && (
        <p className="field-editor__error" role="alert">
          {error}
        </p>
      )}
      <div className="field-editor__actions">
        <button type="submit" className="button button--primary">
          Save
        </button>
        <button type="button" className="button" onClick={onCancel}>
          Cancel
        </button>
      </div>
    </form>
  );
}

type ControlProps = {
  field: FieldDefinition;
  value?: FieldValue;
  onChange: (value: FieldValue) => void;
};

function Control({ field, value, onChange }: ControlProps) {
  const label = `New value for ${field.label}`;
  switch (field.type) {
    case 'coded':
      return (
        <select aria-label={label} autoFocus value={value === undefined ? '' : String(value)} onChange={(e) => onChange(e.target.value)}>
          <option value="" disabled>
            Choose…
          </option>
          {field.options.map((option) => (
            <option key={option.code} value={option.code}>
              {option.label}
            </option>
          ))}
        </select>
      );
    case 'coded_multi': {
      const selected = asList(value);
      const toggle = (code: string) =>
        onChange(selected.includes(code) ? selected.filter((c) => c !== code) : [...selected, code]);
      return (
        <fieldset className="option-checkboxes" aria-label={label}>
          {field.options.map((option) => (
            <label key={option.code} className="option-checkbox">
              <input type="checkbox" checked={selected.includes(option.code)} onChange={() => toggle(option.code)} />
              {option.label}
            </label>
          ))}
        </fieldset>
      );
    }
    case 'date':
      return <input type="date" aria-label={label} autoFocus value={value === undefined ? '' : String(value)} onChange={(e) => onChange(e.target.value)} />;
    case 'integer':
    case 'number':
      return (
        <input
          type="number"
          aria-label={label}
          autoFocus
          step={field.type === 'integer' ? 1 : 'any'}
          min={field.validation.min}
          max={field.validation.max}
          value={value === undefined ? '' : String(value)}
          onChange={(e) => onChange(e.target.value)}
        />
      );
    case 'string_list':
      return (
        <textarea
          aria-label={`${label}, one per line`}
          autoFocus
          rows={4}
          value={asList(value).join('\n')}
          onChange={(e) => onChange(e.target.value.split('\n'))}
        />
      );
    case 'string':
      return (
        <input
          type="text"
          aria-label={label}
          autoFocus
          pattern={field.validation.pattern}
          value={value === undefined ? '' : String(value)}
          onChange={(e) => onChange(e.target.value)}
        />
      );
  }
}

/** Converts what the control holds into the JSON value the catalog type expects. */
function toValue(field: FieldDefinition, draft: FieldValue | undefined): FieldValue | undefined {
  if (draft === undefined) {
    return undefined;
  }
  switch (field.type) {
    case 'integer':
    case 'number':
      return draft === '' ? undefined : Number(draft);
    case 'string_list':
      return asList(draft).map((item) => item.trim()).filter((item) => item.length > 0);
    default:
      return draft;
  }
}
