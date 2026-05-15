import { Injectable, signal } from '@angular/core';

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
    if (window.apexDesktop?.defaultApiUrl) {
      return window.apexDesktop.defaultApiUrl;
    }
    if (protocol === 'capacitor:' || protocol === 'ionic:' || protocol === 'app:') {
      return 'http://localhost:8080';
    }
    return '';
  }
}
