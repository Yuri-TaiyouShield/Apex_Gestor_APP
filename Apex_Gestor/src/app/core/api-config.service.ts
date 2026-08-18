import { Injectable, signal } from '@angular/core';
import { Capacitor } from '@capacitor/core';

const STORAGE_KEY = 'apex.apiBaseUrl';

@Injectable({ providedIn: 'root' })
export class ApiConfigService {
  readonly baseUrl = signal(this.readInitialBaseUrl());

  setBaseUrl(value: string): void {
    const normalized = value.trim().replace(/\/$/, '');
    localStorage.setItem(STORAGE_KEY, normalized);
    this.baseUrl.set(normalized);
  }

  reset(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.baseUrl.set(this.defaultBaseUrl());
  }

  apiUrl(path: string): string {
    const normalizedPath = path.startsWith('/') ? path : `/${path}`;
    const base = this.baseUrl();
    return `${base}${normalizedPath}`;
  }

  healthUrl(): string {
    return this.apiUrl('/actuator/health');
  }

  private readInitialBaseUrl(): string {
    return localStorage.getItem(STORAGE_KEY) ?? this.defaultBaseUrl();
  }

  private defaultBaseUrl(): string {
    const protocol = globalThis.location?.protocol ?? 'http:';
    const desktopApiUrl = globalThis.window?.apexDesktop?.defaultApiUrl;
    if (desktopApiUrl) {
      return desktopApiUrl;
    }

    if (Capacitor.isNativePlatform() || protocol === 'capacitor:' || protocol === 'ionic:' || protocol === 'app:') {
      return Capacitor.getPlatform() === 'android' ? 'http://10.0.2.2:8080' : 'http://localhost:8080';
    }

    const host = globalThis.location?.hostname ?? '';
    if (host === 'localhost' || host === '127.0.0.1') {
      return 'http://localhost:8080';
    }

    // Hosted web deployments can keep the API on the same origin behind a reverse proxy.
    return '';
  }
}
