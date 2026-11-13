import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // In development the browser talks only to Vite; Vite forwards /api calls to Spring Boot.
    proxy: {
      '/api': 'http://localhost:8080',
      '/actuator/health': 'http://localhost:8080',
    },
  },
})
