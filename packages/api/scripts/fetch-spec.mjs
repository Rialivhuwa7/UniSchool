// Downloads the OpenAPI spec from a running backend into openapi.json.
// Usage: npm run api:fetch   (backend on http://localhost:8081, or set API_URL)
import { writeFile } from 'node:fs/promises';

const base = process.env.API_URL ?? 'http://localhost:8081';
const res = await fetch(`${base}/v3/api-docs`);
if (!res.ok) {
  console.error(`Could not fetch ${base}/v3/api-docs (HTTP ${res.status}). Is the backend running?`);
  process.exit(1);
}
const spec = await res.json();
// The server URL depends on where the backend ran; the client sets its own base URL.
delete spec.servers;
await writeFile(new URL('../openapi.json', import.meta.url), JSON.stringify(spec, null, 2) + '\n');
console.log(`Saved openapi.json (${Object.keys(spec.paths).length} paths). Now run: npm run api:generate`);
