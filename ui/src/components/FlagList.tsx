import { flagDescription, flagLabel } from '../lib/flags';

export function FlagList({ flags }: { flags: string[] }) {
  if (flags.length === 0) {
    return null;
  }
  return (
    <ul className="flag-list" aria-label="Document flags">
      {flags.map((flag) => (
        <li key={flag} className={`flag flag--${flag}`} title={flagDescription(flag)}>
          {flagLabel(flag)}
        </li>
      ))}
    </ul>
  );
}
