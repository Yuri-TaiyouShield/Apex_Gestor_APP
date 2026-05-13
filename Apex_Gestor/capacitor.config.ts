import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'br.com.apexgestor.app',
  appName: 'Apex Gestor',
  webDir: 'dist/apex-gestor/browser',
  bundledWebRuntime: false,
  server: {
    androidScheme: 'https'
  }
};

export default config;
