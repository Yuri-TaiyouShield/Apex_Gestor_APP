import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, from, switchMap, tap } from 'rxjs';

import { ApiConfigService } from './api-config.service';
import { ApiReadinessService } from './api-readiness.service';
import { LicenseValidationResponse } from './models';
import { SecureStorageService } from './secure-storage.service';

const DEVICE_KEY = 'apex.deviceFingerprint';
const LICENSE_KEY = 'apex.licenseKey';
const LICENSE_STATUS_KEY = 'apex.licenseStatus';

interface DeviceInfo {
  deviceFingerprint: string;
  deviceLabel: string;
  platform: string;
}

@Injectable({ providedIn: 'root' })
export class LicenseService {
  private readonly http = inject(HttpClient);
  private readonly apiConfig = inject(ApiConfigService);
  private readonly readiness = inject(ApiReadinessService);
  private readonly secureStorage = inject(SecureStorageService);

  validate(licenseKey: string): Observable<LicenseValidationResponse> {
    return from(this.deviceInfo()).pipe(
      switchMap((device) => this.readiness.waitForApi().pipe(
        switchMap(() => this.http.post<LicenseValidationResponse>(this.apiConfig.apiUrl('/api/licenses/validate'), {
          licenseKey,
          deviceFingerprint: device.deviceFingerprint,
          deviceLabel: device.deviceLabel,
          platform: device.platform,
          appVersion: '3.1.0'
        }))
      )),
      tap((response) => {
        localStorage.setItem(LICENSE_STATUS_KEY, JSON.stringify(response));
        if (response.valid) {
          void this.secureStorage.setItem(LICENSE_KEY, licenseKey);
        }
      })
    );
  }

  async readLicenseKey(): Promise<string> {
    return (await this.secureStorage.getItem(LICENSE_KEY)) ?? '';
  }

  readCachedStatus(): LicenseValidationResponse | null {
    const cached = localStorage.getItem(LICENSE_STATUS_KEY);
    if (!cached) {
      return null;
    }
    try {
      return JSON.parse(cached) as LicenseValidationResponse;
    } catch {
      return null;
    }
  }

  async deviceInfo(): Promise<DeviceInfo> {
    if (window.apexDesktop?.getDeviceInfo) {
      const info = await window.apexDesktop.getDeviceInfo();
      return {
        deviceFingerprint: info.deviceFingerprint,
        deviceLabel: info.deviceLabel,
        platform: info.platform
      };
    }

    let fingerprint = localStorage.getItem(DEVICE_KEY);
    if (!fingerprint) {
      fingerprint = globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random()}`;
      localStorage.setItem(DEVICE_KEY, fingerprint);
    }

    const protocol = globalThis.location?.protocol ?? 'http:';
    return {
      deviceFingerprint: fingerprint,
      deviceLabel: protocol === 'capacitor:' || protocol === 'ionic:' ? 'Apex Mobile' : 'Apex Web',
      platform: protocol.replace(':', '') || 'web'
    };
  }
}
