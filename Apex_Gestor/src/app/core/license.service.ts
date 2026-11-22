import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, from, switchMap, tap } from 'rxjs';

import { ApiConfigService } from './api-config.service';
import { ApiReadinessService } from './api-readiness.service';
import { LicenseContextService, LICENSE_STATUS_KEY } from './license-context.service';
import { LicenseValidationResponse, TenantEntitlementResponse } from './models';
import { TenantFeatureService } from './tenant-feature.service';

@Injectable({ providedIn: 'root' })
export class LicenseService {
  private readonly http = inject(HttpClient);
  private readonly apiConfig = inject(ApiConfigService);
  private readonly readiness = inject(ApiReadinessService);
  private readonly context = inject(LicenseContextService);
  private readonly tenantFeatures = inject(TenantFeatureService);

  validate(licenseKey: string): Observable<LicenseValidationResponse> {
    return from(this.deviceInfo()).pipe(
      switchMap((device) => this.readiness.waitForApi().pipe(
        switchMap(() => this.http.post<LicenseValidationResponse>(this.apiConfig.apiUrl('/api/licenses/validate'), {
          licenseKey,
          deviceFingerprint: device.deviceFingerprint,
          deviceLabel: device.deviceLabel,
          platform: device.platform,
          appVersion: '3.1.0',
          appId: device.appId,
          tenantCode: this.context.tenantCode()
        }))
      )),
      tap((response) => {
        localStorage.setItem(LICENSE_STATUS_KEY, JSON.stringify(response));
        this.context.storeTenantCode(response.tenantCode ?? this.context.tenantCode());
        this.tenantFeatures.updateFromLicense(response);
        if (response.valid) {
          void this.context.storeLicenseKey(licenseKey);
        }
      })
    );
  }

  loadEntitlement(): Observable<TenantEntitlementResponse> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.get<TenantEntitlementResponse>(this.apiConfig.apiUrl('/api/licenses/entitlement'))),
      tap((entitlement) => {
        this.context.storeTenantCode(entitlement.tenantCode ?? this.context.tenantCode());
        this.tenantFeatures.updateFromEntitlement(entitlement);
        const cached = this.readCachedStatus();
        const syncedCache: LicenseValidationResponse = {
          ...(cached ?? { valid: true, status: entitlement.status, message: 'Entitlement sincronizado.' }),
          tenantCode: entitlement.tenantCode,
          tenantName: entitlement.tenantName,
          subscriptionTier: entitlement.subscriptionTier,
          features: entitlement.features,
          contingenciaAte: entitlement.contingenciaAte,
          degradedMode: entitlement.emGracePeriod
        };
        localStorage.setItem(LICENSE_STATUS_KEY, JSON.stringify(syncedCache));
      })
    );
  }

  async readLicenseKey(): Promise<string> {
    return this.context.readLicenseKey();
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

  deviceInfo() {
    return this.context.deviceInfo();
  }
}
