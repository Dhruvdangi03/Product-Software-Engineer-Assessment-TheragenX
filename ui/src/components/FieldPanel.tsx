import type { Catalog, ExtractedCase, FieldReview, FieldValue, ReviewAction } from '../api/types';
import { groupBySection } from '../lib/catalog';
import { RISK_LABELS, RISKS_BY_PRIORITY, riskOf, type Risk } from '../lib/risk';
import { FieldRow } from './FieldRow';

export type RiskFilter = 'all' | Risk;

type Props = {
  catalog: Catalog;
  extractedCase: ExtractedCase;
  reviews: Record<string, FieldReview>;
  filter: RiskFilter;
  onFilterChange: (filter: RiskFilter) => void;
  selectedFieldId?: string;
  editingFieldId?: string;
  errorByField: Record<string, string>;
  onSelectField: (fieldId: string) => void;
  onReview: (fieldId: string, action: ReviewAction, value?: FieldValue) => void;
  onStartEdit: (fieldId: string) => void;
  onCancelEdit: () => void;
};

/**
 * The fields of one case, grouped by catalog section in catalog order. The summary at the top
 * counts fields by risk and doubles as a filter, so the risky fields are one click away.
 */
export function FieldPanel(props: Props) {
  const { catalog, extractedCase, reviews, filter, onFilterChange } = props;
  const counts = countByRisk(catalog, extractedCase);
  const visible = (fieldId: string) => filter === 'all' || riskOf(extractedCase.fields[fieldId]) === filter;

  return (
    <div className="field-panel">
      <div className="risk-filter" role="group" aria-label="Show fields by risk">
        <button type="button" className={`risk-filter__chip ${filter === 'all' ? 'is-active' : ''}`} onClick={() => onFilterChange('all')}>
          All <span className="risk-filter__count">{catalog.fields.length}</span>
        </button>
        {RISKS_BY_PRIORITY.filter((risk) => counts[risk] > 0).map((risk) => (
          <button
            key={risk}
            type="button"
            className={`risk-filter__chip risk-filter__chip--${risk} ${filter === risk ? 'is-active' : ''}`}
            aria-pressed={filter === risk}
            onClick={() => onFilterChange(filter === risk ? 'all' : risk)}
          >
            {RISK_LABELS[risk]} <span className="risk-filter__count">{counts[risk]}</span>
          </button>
        ))}
      </div>

      {groupBySection(catalog).map(({ section, fields }) => {
        const shown = fields.filter((field) => visible(field.id));
        if (shown.length === 0) {
          return null;
        }
        return (
          <section key={section.id} className="field-section" aria-labelledby={`section-${section.id}`}>
            <h3 id={`section-${section.id}`} className="field-section__title">
              {section.label}
            </h3>
            <ul className="field-section__rows">
              {shown.map((field) => (
                <FieldRow
                  key={field.id}
                  field={field}
                  result={extractedCase.fields[field.id]}
                  review={reviews[field.id]}
                  selected={props.selectedFieldId === field.id}
                  editing={props.editingFieldId === field.id}
                  error={props.errorByField[field.id]}
                  onSelect={() => props.onSelectField(field.id)}
                  onReview={(action, value) => props.onReview(field.id, action, value)}
                  onStartEdit={() => props.onStartEdit(field.id)}
                  onCancelEdit={props.onCancelEdit}
                />
              ))}
            </ul>
          </section>
        );
      })}
    </div>
  );
}

function countByRisk(catalog: Catalog, extractedCase: ExtractedCase): Record<Risk, number> {
  const counts: Record<Risk, number> = { conflict: 0, low: 0, medium: 0, unknown: 0, high: 0 };
  catalog.fields.forEach((field) => {
    counts[riskOf(extractedCase.fields[field.id])] += 1;
  });
  return counts;
}
