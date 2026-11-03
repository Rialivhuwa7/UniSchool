import { defineConfig, devices } from '@playwright/test';

/*
 * End-to-end tests drive the real app against a running backend with the dev demo data:
 *   ./mvnw spring-boot:run        (backend on :8081)
 *   npm run e2e --workspace web   (starts Vite on :5180 if it isn't running)
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  use: {
    baseURL: 'http://localhost:5180',
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    // Its own port, so the tests never hit another project's dev server on 5173.
    command: 'npm run dev -- --port 5180 --strictPort',
    url: 'http://localhost:5180',
    reuseExistingServer: true,
  },
});
