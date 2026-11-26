import type { ReactNode } from 'react';

type Props = {
  tone: 'loading' | 'empty' | 'error';
  title: string;
  children?: ReactNode;
  onRetry?: () => void;
};

/** The one way the UI shows loading, empty and error states, so they all look alike. */
export function StateMessage({ tone, title, children, onRetry }: Props) {
  return (
    <div className={`state-message state-message--${tone}`} role={tone === 'error' ? 'alert' : 'status'}>
      {tone === 'loading' && <span className="spinner" aria-hidden="true" />}
      <p className="state-message__title">{title}</p>
      {children && <div className="state-message__body">{children}</div>}
      {onRetry && (
        <button type="button" className="button" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}
