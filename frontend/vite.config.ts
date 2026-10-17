import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', 'VITE_')
  return {
    base: env.VITE_BASE_PATH || '/',
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        '/api': 'http://localhost:8080',
        '/oauth2': 'http://localhost:8080',
        '/login': 'http://localhost:8080',
        '/logout': 'http://localhost:8080',
      },
    },
  }
})
