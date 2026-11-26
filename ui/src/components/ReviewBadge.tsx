import type { ReviewDecision } from '../api/types';

const LABELS: Record<ReviewDecision, string> = {
  accepted: 'Accepted',
  rejected: 'Rejected',
  edited: 'Edited',
};

const SYMBOLS: Record<ReviewDecision, string> = {
  accepted: '✓',
  rejected: '✕',
  edited: '✎',
};

export function ReviewBadge({ decision }: { decision: ReviewDecision }) {
  return (
    <span className={`review-badge review-badge--${decision}`}>
      <span aria-hidden="true">{SYMBOLS[decision]}</span> {LABELS[decision]}
    </span>
  );
}
