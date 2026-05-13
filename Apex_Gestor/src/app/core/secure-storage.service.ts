import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class SecureStorageService {
  async getItem(key: string): Promise<string | null> {
    if (window.apexDesktop?.secureStorage) {
      return window.apexDesktop.secureStorage.getItem(key);
    }
    return localStorage.getItem(key);
  }

  async setItem(key: string, value: string): Promise<void> {
    if (window.apexDesktop?.secureStorage) {
      await window.apexDesktop.secureStorage.setItem(key, value);
      return;
    }
    localStorage.setItem(key, value);
  }

  async removeItem(key: string): Promise<void> {
    if (window.apexDesktop?.secureStorage) {
      await window.apexDesktop.secureStorage.removeItem(key);
      return;
    }
    localStorage.removeItem(key);
  }
}
