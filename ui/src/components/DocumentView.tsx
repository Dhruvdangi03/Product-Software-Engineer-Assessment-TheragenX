import { api } from '../api/client';
import type { Catalog, DocumentReview } from '../api/types';
import { useAsync } from '../lib/useAsync';
import { ReviewScreen } from './ReviewScreen';
import { StateMessage } from './StateMessage';

type Props = {
  catalog: Catalog;
  documentId: string;
  onReviewSaved: () => void;
};

/** Loads one document and hands it to the review screen. */
export function DocumentView({ catalog, documentId, onReviewSaved }: Props) {
  const [state, reload, replace] = useAsync(() => api.document(documentId), [documentId]);

  if (state.status === 'loading') {
    return <StateMessage tone="loading" title={`Loading ${documentId}…`} />;
  }
  if (state.status === 'error') {
    return (
      <StateMessage tone="error" title={`Could not load ${documentId}`} onRetry={reload}>
        {state.error.message}
      </StateMessage>
    );
  }

  const saveReview = (review: DocumentReview) => {
    replace({ ...state.data, review });
    onReviewSaved();
  };
  return <ReviewScreen key={documentId} catalog={catalog} detail={state.data} onReviewSaved={saveReview} />;
}
