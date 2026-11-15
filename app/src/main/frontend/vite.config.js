import { defineConfig } from 'vite';

export default defineConfig({
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