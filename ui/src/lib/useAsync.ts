import { useCallback, useEffect, useState } from 'react';

export type AsyncState<T> =
  | { status: 'loading' }
  | { status: 'error'; error: Error }
  | { status: 'ready'; data: T };

/**
 * Runs a loader whenever its dependencies change and tracks loading, error and result.
 * A result that arrives after the dependencies changed again is dropped.
 */
export function useAsync<T>(
  load: () => Promise<T>,
  deps: unknown[],
): [AsyncState<T>, () => void, (data: T) => void] {
  const [state, setState] = useState<AsyncState<T>>({ status: 'loading' });
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let current = true;
    setState({ status: 'loading' });
    load().then(
      (data) => current && setState({ status: 'ready', data }),
      (error: unknown) =>
        current && setState({ status: 'error', error: error instanceof Error ? error : new Error(String(error)) }),
    );
    return () => {
      current = false;
    };
  }, [...deps, attempt]);

  const reload = useCallback(() => setAttempt((n) => n + 1), []);
  const replace = useCallback((data: T) => setState({ status: 'ready', data }), []);
  return [state, reload, replace];
}
