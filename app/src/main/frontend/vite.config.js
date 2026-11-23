import { defineConfig } from 'vite';

export default defineConfig({
  test: {
    // Lit components need a DOM
    environment: 'happy-dom'
  },
  build: {
    outDir: '../../../build/frontend',
    emptyOutDir: true,
    rollupOptions: {
      input: { app: 'src/main.js' },
      output: {
        entryFileNames: '[name].js',
        assetFileNames: '[name][extname]'
      }
    }
  }
});