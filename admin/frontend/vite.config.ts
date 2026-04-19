import { defineConfig } from 'vite';
import preact from '@preact/preset-vite';

const backendPort = process.env.PARSEBOT_ADMIN_PORT ?? '8080';

export default defineConfig({
  plugins: [preact()],
  base: './',
  build: {
    outDir: 'dist',
    emptyOutDir: true,
    rollupOptions: {
      output: {
        entryFileNames: 'assets/[name].js',
        chunkFileNames: 'assets/[name].js',
        assetFileNames: 'assets/[name][extname]'
      }
    }
  },
  server: {
    proxy: {
      '/api': `http://127.0.0.1:${backendPort}`
    }
  }
});
