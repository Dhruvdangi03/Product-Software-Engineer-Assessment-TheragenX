import type { DocumentSummary } from '../api/types';
import { FlagList } from './FlagList';
import { ProgressBar } from './ProgressBar';

type Props = {
  documents: DocumentSummary[];
  selectedId?: string;
  onSelect: (id: string) => void;
};

export function DocumentList({ documents, selectedId, onSelect }: Props) {
  return (
    <nav aria-label="Documents">
      <ul className="document-list">
        {documents.map((document) => (
          <li key={document.id}>
            <button
              type="button"
              className={`document-list__item ${document.id === selectedId ? 'is-selected' : ''}`}
              aria-current={document.id === selectedId ? 'page' : undefined}
              onClick={() => onSelect(document.id)}
            >
              <span className="document-list__head">
                <span className="document-list__id">{document.id}</span>
                <span className="document-list__cases">{caseCountLabel(document)}</span>
              </span>
              <span className="document-list__title">{document.title}</span>
              <FlagList flags={document.flags} />
              {document.review_progress.total > 0 && <ProgressBar progress={document.review_progress} compact />}
            </button>
          </li>
        ))}
      </ul>
    </nav>
  );
}

function caseCountLabel(document: DocumentSummary): string {
  if (!document.processed) {
    return 'not processed';
  }
  if (document.case_count === 0) {
    return 'no case';
  }
  return document.case_count === 1 ? '1 case' : `${document.case_count} cases`;
}
