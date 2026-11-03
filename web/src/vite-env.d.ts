/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Where the API lives when it isn't served from the same origin. Empty in dev (Vite proxies /api). */
  readonly VITE_API_URL?: string;
}
