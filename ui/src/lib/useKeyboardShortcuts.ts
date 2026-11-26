import { useEffect, useRef } from 'react';

/** Handlers by key, as in KeyboardEvent.key ('j', 'ArrowDown', '?'). */
export type Shortcuts = Record<string, () => void>;

/**
 * Global single-key shortcuts. They are ignored while the reviewer types in a form control,
 * and when a modifier key is held, so browser shortcuts keep working.
 */
export function useKeyboardShortcuts(shortcuts: Shortcuts) {
  // A ref, so the listener always sees the latest handlers without re-subscribing on every render.
  const latest = useRef(shortcuts);
  latest.current = shortcuts;

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.ctrlKey || event.metaKey || event.altKey || isTyping(event.target)) {
        return;
      }
      const handler = latest.current[event.key];
      if (handler) {
        event.preventDefault();
        handler();
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, []);
}

function isTyping(target: EventTarget | null): boolean {
  return (
    target instanceof HTMLInputElement ||
    target instanceof HTMLTextAreaElement ||
    target instanceof HTMLSelectElement ||
    (target instanceof HTMLElement && target.isContentEditable)
  );
}
