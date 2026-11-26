import type { ReviewProgress } from '../api/types';

export function ProgressBar({ progress, compact = false }: { progress: ReviewProgress; compact?: boolean }) {
  const percent = progress.total === 0 ? 0 : Math.round((progress.reviewed / progress.total) * 100);
  const done = progress.total > 0 && progress.reviewed === progress.total;
  return (
    <div className={`progress ${compact ? 'progress--compact' : ''} ${done ? 'progress--done' : ''}`}>
      <div
        className="progress__track"
        role="progressbar"
        aria-valuemin={0}
        aria-valuemax={progress.total}
        aria-valuenow={progress.reviewed}
        aria-label="Review progress"
      >
        <div className="progress__fill" style={{ width: `${percent}%` }} />
      </div>
      {!compact && (
        <span className="progress__label">
          {progress.reviewed} of {progress.total} fields reviewed
        </span>
      )}
    </div>
  );
}
