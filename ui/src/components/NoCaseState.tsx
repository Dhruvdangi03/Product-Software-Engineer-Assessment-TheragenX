import { flagDescription, NO_CASE_FLAGS } from '../lib/flags';

/**
 * Shown instead of the field panel when a document holds no case. It says why, from the
 * pipeline's flags, so an empty document reads as a decision and not as a failure.
 */
export function NoCaseState({ flags }: { flags: string[] }) {
  const reasons = flags.filter((flag) => NO_CASE_FLAGS.includes(flag));
  return (
    <div className="no-case">
      <p className="no-case__eyebrow">No case</p>
      <h3 className="no-case__title">This document does not describe a reportable case</h3>
      <p className="no-case__lead">A case needs an identifiable patient, a reporter, a suspect drug and an adverse event.</p>
      {reasons.length > 0 && (
        <ul className="no-case__reasons">
          {reasons.map((flag) => (
            <li key={flag}>{flagDescription(flag)}</li>
          ))}
        </ul>
      )}
    </div>
  );
}
