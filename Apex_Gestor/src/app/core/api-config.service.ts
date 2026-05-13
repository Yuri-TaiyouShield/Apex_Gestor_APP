import { Injectable, signal } from '@angular/core';

const STORAGE_KEY = 'apex.apiBaseUrl';

declare global {
  interface Window {
    apexDesktop?: {
      defaultApiUrl?: string;
      isDesktop?: boolean;
      platform?: string;
      getDeviceInfo?: () => Promise<{
        defaultApiUrl: string;
        deviceFingerprint: string;
        deviceLabel: string;
        isDesktop: boolean;
        platform: string;
      }>;
      secureStorage?: {
        getItem: (key: string) => Promise<string | null>;
        setItem: (key: string, value: string) => Promise<boolean>;
        removeItem: (key: string) => Promise<boolean>;
      };
    };
  }
}

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
