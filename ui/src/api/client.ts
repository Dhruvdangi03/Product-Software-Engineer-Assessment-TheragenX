import type {
  Catalog,
  DocumentDetail,
  DocumentReview,
  DocumentSummary,
  DuplicateView,
  FieldValue,
  ReviewAction,
} from './types';

/** An API call that failed. The message is safe to show to the reviewer. */
export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(path, init);
  } catch {
    throw new ApiError(0, 'The server cannot be reached. Is the backend running?');
  }
  if (!response.ok) {
    throw new ApiError(response.status, await errorMessage(response));
  }
  return (await response.json()) as T;
}

/** The backend answers errors as RFC 7807 problem details; fall back to the status text. */
async function errorMessage(response: Response): Promise<string> {
  try {
    const problem = (await response.json()) as { detail?: string };
    if (problem.detail) {
      return problem.detail;
    }
  } catch {
    // The body was not JSON; the status line is all we have.
  }
  return `${response.status} ${response.statusText}`;
}

export const api = {
  catalog: () => request<Catalog>('/api/catalog'),

  documents: () => request<DocumentSummary[]>('/api/documents'),

  document: (id: string) => request<DocumentDetail>(`/api/documents/${encodeURIComponent(id)}`),

  duplicates: () => request<DuplicateView[]>('/api/duplicates'),

  reviewField: (
    documentId: string,
    caseIndex: number,
    fieldId: string,
    action: ReviewAction,
    value?: FieldValue,
  ) =>
    request<DocumentReview>(
      `/api/documents/${encodeURIComponent(documentId)}/cases/${caseIndex}/fields/${encodeURIComponent(fieldId)}`,
      {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ action, value }),
      },
    ),
};
