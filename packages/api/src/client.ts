import createClient, { type Middleware } from 'openapi-fetch';
import type { paths } from './schema';

/** Error thrown for any non-2xx answer. `message` is the backend's human-readable detail. */
export class ApiError extends Error {
  readonly status: number;
  /** Field errors from bean validation (400), keyed by field name. */
  readonly fieldErrors: Record<string, string>;

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

export interface ApiClientOptions {
  /** Where the API lives. Use '' for same-origin (the web app proxies /api in dev). */
  baseUrl: string;
  /** Returns the current JWT, or null when signed out. */
  getToken: () => string | null;
  /** Called when the backend answers 401 to a signed-in request (token expired or revoked). */
  onUnauthorized?: () => void;
}

/** Spring's ProblemDetail body. */
interface Problem {
  detail?: string;
  errors?: Record<string, string>;
}

const FALLBACK: Record<number, string> = {
  400: 'Please check what you entered.',
  401: 'Please sign in again.',
  403: "Your current role can't do this.",
  404: "We couldn't find that.",
  409: 'Someone else changed this. Refresh and try again.',
  422: "That isn't allowed.",
};

/**
 * Typed client for every UniSchool endpoint. Paths, parameters and bodies are checked against
 * the backend's OpenAPI spec, so a renamed field on the Java side becomes a TypeScript error here.
 * Any non-2xx answer throws ApiError; wrap calls in ok() to get the body:
 *
 *   useQuery({ queryKey: ['wallet'], queryFn: () => ok(api.GET('/api/wallet')) })
 */
export function createApiClient({ baseUrl, getToken, onUnauthorized }: ApiClientOptions) {
  const client = createClient<paths>({ baseUrl });

  const auth: Middleware = {
    onRequest({ request }) {
      const token = getToken();
      if (token) request.headers.set('Authorization', `Bearer ${token}`);
      return request;
    },
    async onResponse({ request, response }) {
      if (response.ok) return response;
      let problem: Problem = {};
      try {
        problem = (await response.clone().json()) as Problem;
      } catch {
        // not JSON (e.g. a proxy error page)
      }
      if (response.status === 401 && request.headers.has('Authorization')) onUnauthorized?.();
      const message = problem.detail || FALLBACK[response.status] || `Something went wrong (HTTP ${response.status}).`;
      throw new ApiError(response.status, message, problem.errors ?? {});
    },
  };
  client.use(auth);
  return client;
}

export type ApiClient = ReturnType<typeof createApiClient>;

/**
 * Unwraps an openapi-fetch call to its response body. Failures have already thrown ApiError
 * in the client middleware, so this only has to pick out `data` (undefined for empty answers).
 */
export async function ok<T>(call: Promise<{ data?: T }>): Promise<T> {
  const { data } = await call;
  return data as T;
}
