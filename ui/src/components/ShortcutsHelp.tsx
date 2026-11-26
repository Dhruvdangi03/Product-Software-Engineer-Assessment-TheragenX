const SHORTCUTS: [string[], string][] = [
  [['J', '↓'], 'Next field'],
  [['K', '↑'], 'Previous field'],
  [['A'], 'Accept the selected field'],
  [['R'], 'Reject the selected field'],
  [['E'], 'Edit the selected field'],
  [['U'], 'Undo the review of the selected field'],
  [['['], 'Previous case'],
  [[']'], 'Next case'],
  [['Esc'], 'Close the editor or clear the selection'],
  [['?'], 'Show or hide this list'],
];

export function ShortcutsHelp({ onClose }: { onClose: () => void }) {
  return (
    <aside className="shortcuts" aria-label="Keyboard shortcuts">
      <div className="shortcuts__head">
        <h3>Keyboard shortcuts</h3>
        <button type="button" className="button button--quiet" onClick={onClose}>
          Close
        </button>
      </div>
      <dl>
        {SHORTCUTS.map(([keys, action]) => (
          <div key={action} className="shortcuts__row">
            <dt>
              {keys.map((key) => (
                <kbd key={key}>{key}</kbd>
              ))}
            </dt>
            <dd>{action}</dd>
          </div>
        ))}
      </dl>
    </aside>
  );
}
