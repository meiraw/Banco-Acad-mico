import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// O proxy encaminha /api/* para o Spring Boot (porta 8080), evitando CORS em desenvolvimento.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true, rewrite: p => p.replace(/^\/api/, '') } },
  },
})
