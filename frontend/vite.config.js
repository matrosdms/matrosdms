import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    proxy: {
      '/api': {
        // FIX: Load API URL from env or fallback to localhost. Avoids hardcoding for Docker/Prod.
        target: process.env.VITE_API_URL || 'http://127.0.0.1:9090', 
        changeOrigin: true,
        // Increase timeout for long-running backend jobs
        timeout: 120000,
        proxyTimeout: 120000,
        
        // TRANSPARENT ERROR HANDLING
        // Instead of crashing with 500, return 503 so the Client knows the Backend is down.
        configure: (proxy, _options) => {
          proxy.on('error', (err, _req, res) => {
            console.log('[Proxy] Connection Error:', err.code);
            
            // Prevent double-response if headers already sent
            if (res.headersSent) return;

            // Map connection refusal to 503 Service Unavailable
            if (err.code === 'ECONNREFUSED' || err.code === 'ECONNRESET') {
                res.writeHead(503, {
                  'Content-Type': 'application/json',
                  'X-Proxy-Error': 'Backend-Unreachable'
                });
                res.end(JSON.stringify({ 
                    error: 'Service Unavailable', 
                    message: 'The backend service is currently unreachable.' 
                }));
            } else {
                // Generic fallback
                res.writeHead(500, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: 'Proxy Error', details: err.message }));
            }
          });
        }
      }
    }
  }
})