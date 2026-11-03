import { QueryClient } from '@tanstack/react-query';
import { ApiError, createApiClient } from '@unischool/api';

/*
 * The signed-in token lives in sessionStorage: it survives a page refresh but not closing the
 * tab, which suits shared school-office PCs. Tokens expire after 8 hours on the server anyway.
 */
const TOKEN_KEY = 'unischool.token';

let unauthorizedHandler: (() => void) | null = null;

export const tokenStore = {
  get: () => sessionStorage.getItem(TOKEN_KEY),
  set: (token: string) => sessionStorage.setItem(TOKEN_KEY, token),
  clear: () => sessionStorage.removeItem(TOKEN_KEY),
  /** The auth provider registers this so an expired token signs the user out. */
  onUnauthorized: (handler: () => void) => {
    unauthorizedHandler = handler;
  },
};

/** Typed client for the whole UniSchool API. In dev, Vite proxies /api to Spring Boot. */
export const api = createApiClient({
  baseUrl: import.meta.env.VITE_API_URL ?? '',
  getToken: tokenStore.get,
  onUnauthorized: () => unauthorizedHandler?.(),
});

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      // Don't retry answers the backend gave on purpose (not found, forbidden, rule broken).
      retry: (count, error) => !(error instanceof ApiError && error.status < 500) && count < 2,
    },
  },
});

/** Text to show for any error thrown by a query or mutation. */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof TypeError) return "Can't reach UniSchool. Check that the backend is running.";
  return 'Something went wrong.';
}
