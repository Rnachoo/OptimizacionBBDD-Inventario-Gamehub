import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// El CORS del microservicio solo permite los puertos 5173 y 3000,
// por eso el puerto queda fijo: si estuviera ocupado, Vite fallaría en vez de cambiarlo.
export default defineConfig({
  plugins: [react()],
  server: { port: 5173, strictPort: true }
})
