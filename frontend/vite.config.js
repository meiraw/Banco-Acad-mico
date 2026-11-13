import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { pdfAssets } from './pdf-assets.js'

// O proxy encaminha /api/* para o Spring Boot na porta 8080.
export default defineConfig({
  plugins: [react(), pdfAssets()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true, rewrite: p => p.replace(/^\/api/, '') } },
  },
})
