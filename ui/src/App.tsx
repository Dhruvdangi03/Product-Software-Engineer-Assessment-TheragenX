import { useEffect, useState } from 'react';
import { api } from './api/client';
import type { DocumentSummary } from './api/types';
import { DocumentList } from './components/DocumentList';
import { DocumentView } from './components/DocumentView';
import { DuplicatesView } from './components/DuplicatesView';
import { StateMessage } from './components/StateMessage';
import { useAsync } from './lib/useAsync';

/** What the main area shows. Kept in the URL hash, so a link or a reload opens the same view. */
type Route = { view: 'document'; id: string } | { view: 'duplicates' } | { view: 'home' };

const DUPLICATES_HASH = '#/duplicates';
const DOCUMENT_HASH_PREFIX = '#/documents/';

function routeFromHash(hash: string): Route {
  if (hash === DUPLICATES_HASH) {
    return { view: 'duplicates' };
  }
  if (hash.startsWith(DOCUMENT_HASH_PREFIX)) {
    return { view: 'document', id: decodeURIComponent(hash.slice(DOCUMENT_HASH_PREFIX.length)) };
  }
  return { view: 'home' };
}

function useHashRoute(): Route {
  const [route, setRoute] = useState(() => routeFromHash(window.location.hash));
  useEffect(() => {
    const onChange = () => setRoute(routeFromHash(window.location.hash));
    window.addEventListener('hashchange', onChange);
    return () => window.removeEventListener('hashchange', onChange);
  }, []);
  return route;
}

const openDocument = (id: string) => {
  window.location.hash = DOCUMENT_HASH_PREFIX + encodeURIComponent(id);
};

export function App() {
  const route = useHashRoute();
  const [catalogState, reloadCatalog] = useAsync(() => api.catalog(), []);
  const [documentsState, reloadDocuments] = useAsync(() => api.documents(), []);
  const documents = documentsState.status === 'ready' ? documentsState.data : [];

  // With no document chosen, open the first one: the reviewer's work starts there.
  useEffect(() => {
    if (route.view === 'home' && documents[0]) {
      window.location.replace(DOCUMENT_HASH_PREFIX + encodeURIComponent(documents[0].id));
    }
  }, [route.view, documents]);

  return (
    <div className="app">
      <header className="topbar">
        <h1 className="topbar__title">Case Intake Review</h1>
        {catalogState.status === 'ready' && <span className="topbar__meta">Catalog {catalogState.data.catalog_version}</span>}
      </header>

      <aside className="sidebar">
        <a className={`sidebar__link ${route.view === 'duplicates' ? 'is-selected' : ''}`} href={DUPLICATES_HASH}>
          Duplicates
          <span className="sidebar__count">{duplicateCount(documents)}</span>
        </a>
        <h2 className="sidebar__heading">Documents</h2>
        {documentsState.status === 'loading' && <StateMessage tone="loading" title="Loading documents…" />}
        {documentsState.status === 'error' && (
          <StateMessage tone="error" title="Could not load documents" onRetry={reloadDocuments}>
            {documentsState.error.message}
          </StateMessage>
        )}
        {documentsState.status === 'ready' && documents.length === 0 && (
          <StateMessage tone="empty" title="No documents">
            Put .txt files in the documents folder and run the pipeline.
          </StateMessage>
        )}
        <DocumentList documents={documents} selectedId={route.view === 'document' ? route.id : undefined} onSelect={openDocument} />
      </aside>

      <main className="main">
        {catalogState.status === 'loading' && <StateMessage tone="loading" title="Loading catalog…" />}
        {catalogState.status === 'error' && (
          <StateMessage tone="error" title="Could not load the field catalog" onRetry={reloadCatalog}>
            {catalogState.error.message}
          </StateMessage>
        )}
        {catalogState.status === 'ready' && route.view === 'document' && (
          <DocumentView key={route.id} catalog={catalogState.data} documentId={route.id} onReviewSaved={reloadDocuments} />
        )}
        {catalogState.status === 'ready' && route.view === 'duplicates' && <DuplicatesView catalog={catalogState.data} />}
      </main>
    </div>
  );
}

function duplicateCount(documents: DocumentSummary[]): number {
  const flagged = documents.filter((document) => document.flags.includes('possible_duplicate')).length;
  return Math.floor(flagged / 2);
}
