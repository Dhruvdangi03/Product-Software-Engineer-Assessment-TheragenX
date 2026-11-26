import type { ExtractedCase } from '../api/types';

type Props = {
  cases: ExtractedCase[];
  activeIndex: number;
  onChange: (index: number) => void;
};

export function caseLabel(extractedCase: ExtractedCase): string {
  return extractedCase.label ?? `Case ${extractedCase.case_index + 1}`;
}

/** Tabs to switch between the patients of a document. Hidden when there is only one. */
export function CaseSwitcher({ cases, activeIndex, onChange }: Props) {
  if (cases.length < 2) {
    return null;
  }
  return (
    <div className="case-switcher" role="tablist" aria-label="Cases in this document">
      {cases.map((extractedCase, index) => (
        <button
          key={extractedCase.case_index}
          type="button"
          role="tab"
          aria-selected={index === activeIndex}
          className={`case-switcher__tab ${index === activeIndex ? 'is-active' : ''}`}
          onClick={() => onChange(index)}
        >
          <span className="case-switcher__number">{index + 1}</span>
          {caseLabel(extractedCase)}
        </button>
      ))}
      <span className="case-switcher__hint">
        <kbd>[</kbd> <kbd>]</kbd> to switch
      </span>
    </div>
  );
}
