import { RISK_LABELS, type Risk } from '../lib/risk';

const RISK_SYMBOLS: Record<Risk, string> = {
  conflict: '!',
  low: '○',
  medium: '◐',
  unknown: '?',
  high: '●',
};

const SHORT_LABELS: Record<Risk, string> = {
  conflict: 'Conflict',
  low: 'Low',
  medium: 'Medium',
  unknown: 'Unknown',
  high: 'High',
};

/** Status and confidence in one badge: a symbol and a word, never colour alone. */
export function RiskBadge({ risk }: { risk: Risk }) {
  return (
    <span className={`risk-badge risk-badge--${risk}`} title={RISK_LABELS[risk]}>
      <span className="risk-badge__symbol" aria-hidden="true">
        {RISK_SYMBOLS[risk]}
      </span>
      {SHORT_LABELS[risk]}
    </span>
  );
}
