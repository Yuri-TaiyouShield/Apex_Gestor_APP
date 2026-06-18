import { Injectable, inject } from '@angular/core';

import { APP_VARIANT } from './app-variant';
import { ApexAppVariant } from './app-variant.model';
import { SecureStorageService } from './secure-storage.service';

export const DEVICE_KEY = 'apex.deviceFingerprint';
export const LICENSE_KEY = 'apex.licenseKey';
export const LICENSE_STATUS_KEY = 'apex.licenseStatus';
export const TENANT_CODE_KEY = 'apex.tenantCode';

export type ApexLicenseAppId = 'desktop' | 'mobile-staff' | 'mobile-client' | 'web-client';

export interface DeviceInfo {
  deviceFingerprint: string;
  deviceLabel: string;
  platform: string;
  appId: ApexLicenseAppId;
}

@Injectable({ providedIn: 'root' })
export class LicenseContextService {
  private readonly secureStorage = inject(SecureStorageService);

  appId(): ApexLicenseAppId {
    if (window.apexDesktop?.isDesktop || window.apexDesktop?.getDeviceInfo) {
      return 'desktop';
    }
    return variantToAppId(APP_VARIANT);
  }

  async readLicenseKey(): Promise<string> {
    return (await this.secureStorage.getItem(LICENSE_KEY)) ?? '';
  }

  async storeLicenseKey(licenseKey: string): Promise<void> {
    await this.secureStorage.setItem(LICENSE_KEY, licenseKey);
  }

  tenantCode(): string {
    const cachedStatus = localStorage.getItem(LICENSE_STATUS_KEY);
    if (cachedStatus) {
      try {
        const parsed = JSON.parse(cachedStatus) as { tenantCode?: string };
        if (parsed.tenantCode) {
          return parsed.tenantCode;
        }
      } catch {
        // Ignore cache corruption and fall back to the configured tenant key.
      }
    }
    return localStorage.getItem(TENANT_CODE_KEY) ?? 'apex-demo';
  }

  storeTenantCode(tenantCode: string): void {
    localStorage.setItem(TENANT_CODE_KEY, tenantCode || 'apex-demo');
  }

  async deviceInfo(): Promise<DeviceInfo> {
    const appId = this.appId();
    if (window.apexDesktop?.getDeviceInfo) {
      const info = await window.apexDesktop.getDeviceInfo();
      return {
        deviceFingerprint: info.deviceFingerprint,
        deviceLabel: info.deviceLabel,
        platform: info.platform,
        appId
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
      deviceLabel: deviceLabel(appId),
      platform: protocol.replace(':', '') || 'web',
      appId
    };
  }

  async licenseHeaders(): Promise<Record<string, string>> {
    const licenseKey = await this.readLicenseKey();
    if (!licenseKey) {
      return {};
    }

    const device = await this.deviceInfo();
    return {
      'X-Apex-License-Key': licenseKey,
      'X-Apex-Device-Fingerprint': device.deviceFingerprint,
      'X-Apex-App-Id': device.appId,
      'X-Apex-Tenant-Code': this.tenantCode()
    };
  }
}

function variantToAppId(variant: ApexAppVariant): ApexLicenseAppId {
  if (variant === 'client') {
    return 'mobile-client';
  }
  if (variant === 'web-client') {
    return 'web-client';
  }
  return 'mobile-staff';
}

function deviceLabel(appId: ApexLicenseAppId): string {
  const labels: Record<ApexLicenseAppId, string> = {
    desktop: 'Apex Desktop',
    'mobile-staff': 'Apex Mobile Empresa',
    'mobile-client': 'Apex Mobile Cliente',
    'web-client': 'Apex Site Web'
  };
  return labels[appId];
}
