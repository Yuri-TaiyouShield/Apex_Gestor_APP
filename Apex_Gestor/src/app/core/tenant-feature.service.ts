import { Injectable, computed, effect, signal } from '@angular/core';

import { APP_VARIANT } from './app-variant';
import { ApexFeatureKey } from './feature-menu.config';
import { LICENSE_STATUS_KEY } from './license-context.service';
import { LicenseValidationResponse, TenantBranding } from './models';

interface TenantFeatureState {
  tenantCode: string;
  tenantName: string;
  subscriptionTier: string;
  features: ApexFeatureKey[];
  branding: TenantBranding | null;
}

const ESSENTIAL_STATE: TenantFeatureState = {
  tenantCode: 'apex-demo',
  tenantName: 'Apex Demo',
  subscriptionTier: 'Essential',
  features: ['BASIC_PRICING', 'FINANCIAL_CORE'],
  branding: {
    primaryColor: '#0b3a42',
    secondaryColor: '#1e3a8a',
    storefrontName: 'Apex Store'
  }
};

@Injectable({ providedIn: 'root' })
export class TenantFeatureService {
  private readonly state = signal<TenantFeatureState>(this.readFromCache());

  readonly context = this.state.asReadonly();
  readonly enabledFeatures = computed(() => new Set(this.state().features));
  readonly isWhiteLabelB2C = computed(() => {
    const b2cVariant = APP_VARIANT === 'client' || APP_VARIANT === 'web-client';
    return b2cVariant && this.hasFeature('B2C_WHITE_LABEL');
  });

  constructor() {
    effect(() => {
      this.applyBranding(this.state());
    });
  }

  updateFromLicense(response: LicenseValidationResponse): void {
    this.state.set(this.fromLicense(response));
  }

  refreshFromCache(): void {
    this.state.set(this.readFromCache());
  }

  hasFeature(feature: ApexFeatureKey): boolean {
    return this.enabledFeatures().has(feature);
  }

  hasAll(features: ApexFeatureKey[] = []): boolean {
    return features.every((feature) => this.hasFeature(feature));
  }

  private readFromCache(): TenantFeatureState {
    const cached = localStorage.getItem(LICENSE_STATUS_KEY);
    if (!cached) {
      return ESSENTIAL_STATE;
    }
    try {
      return this.fromLicense(JSON.parse(cached) as LicenseValidationResponse);
    } catch {
      return ESSENTIAL_STATE;
    }
  }

  private fromLicense(response: LicenseValidationResponse): TenantFeatureState {
    const features = (response.features ?? ESSENTIAL_STATE.features)
      .filter((feature): feature is ApexFeatureKey => isApexFeature(feature));

    return {
      tenantCode: response.tenantCode ?? ESSENTIAL_STATE.tenantCode,
      tenantName: response.tenantName ?? ESSENTIAL_STATE.tenantName,
      subscriptionTier: response.subscriptionTier ?? response.licensePlan ?? ESSENTIAL_STATE.subscriptionTier,
      features,
      branding: response.branding ?? ESSENTIAL_STATE.branding
    };
  }

  private applyBranding(state: TenantFeatureState): void {
    if (!this.isWhiteLabelB2C() || !state.branding) {
      return;
    }

    const root = document.documentElement;
    root.style.setProperty('--ion-color-primary', state.branding.primaryColor);
    root.style.setProperty('--apex-primary', state.branding.primaryColor);
    root.style.setProperty('--ion-color-secondary', state.branding.secondaryColor);
  }
}

function isApexFeature(feature: string): feature is ApexFeatureKey {
  return [
    'BASIC_PRICING',
    'FINANCIAL_CORE',
    'ADVANCED_FINANCE',
    'COMMISSION_OMNICHANNEL',
    'B2C_WHITE_LABEL',
    'MULTI_STORE_ROUTING',
    'SOFT_STOCK_ALLOCATION',
    'HIERARCHICAL_REPORTS'
  ].includes(feature);
}
