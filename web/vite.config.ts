import { defineConfig, type Plugin } from 'vitest/config';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import QRCode from 'qrcode';

// In dev the browser calls /api on the Vite server, which forwards to Spring Boot.
// Same origin means no CORS setup, and phones only need to reach this one server.
const backend = process.env.API_URL ?? 'http://localhost:8081';

/** Home and office Wi-Fi addresses (192.168.x, 10.x, 172.16-31.x), not VPNs like Tailscale (100.x). */
const isWifiAddress = (url: string) => /^http:\/\/(192\.168\.|10\.|172\.(1[6-9]|2\d|3[01])\.)/.test(url);

/** After the Local and Network lines, prints a QR code of the Wi-Fi address to scan with a phone. */
function phoneQrCode(): Plugin {
  return {
    name: 'unischool-phone-qr',
    apply: 'serve',
    configureServer(server) {
      const printUrls = server.printUrls.bind(server);
      server.printUrls = () => {
        printUrls();
        const network = server.resolvedUrls?.network ?? [];
        const url = network.find(isWifiAddress) ?? network[0];
        if (!url) return;
        QRCode.toString(url, { type: 'terminal', small: true })
          .then((qr) => console.log(`\n  Scan with your phone's camera (same Wi-Fi) to open ${url}\n\n${qr}`))
          .catch(() => {});
      };
    },
  };
}

export default defineConfig({
  plugins: [react(), tailwindcss(), phoneQrCode()],
  server: {
    // A fixed port (5173 is often taken by other projects), so the phone and Tailscale addresses never change.
    port: 5174,
    strictPort: true,
    // Listen on the Wi-Fi network too, so `npm run dev` prints a Network address to open on a phone.
    host: true,
    // Also answer through Tailscale (https://<computer>.<tailnet>.ts.net), for people outside the Wi-Fi.
    allowedHosts: ['.ts.net'],
    proxy: { '/api': backend },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
  },
});
