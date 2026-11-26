/** What each document flag from the pipeline means to a reviewer. Unknown flags are shown as they are. */
const FLAG_DESCRIPTIONS: Record<string, string> = {
  unsupported_format: 'The document has no text layer (image-only PDF), so it was not processed.',
  no_adverse_event: 'No adverse event: nobody had a reaction to a medicine.',
  no_identifiable_patient: 'No identifiable patient: no initials, age or sex of a patient who took the medicine.',
  no_reporter: 'No identifiable reporter.',
  no_suspect_drug: 'No suspect drug: the reporter does not blame a medicine.',
  multiple_patients: 'More than one patient: one case per patient.',
  possible_duplicate: 'Another document seems to describe the same case.',
  embedded_instructions_ignored: 'The document contains text addressed to automated systems. It was ignored.',
  model_response_unreadable: 'The model answer could not be read. Process the document again.',
};

export function flagLabel(flag: string): string {
  return flag.replaceAll('_', ' ');
}

export function flagDescription(flag: string): string {
  return FLAG_DESCRIPTIONS[flag] ?? flagLabel(flag);
}

/** Flags that explain why a document has no case. */
export const NO_CASE_FLAGS = ['unsupported_format', 'no_adverse_event', 'no_identifiable_patient', 'no_reporter', 'no_suspect_drug', 'model_response_unreadable'];
