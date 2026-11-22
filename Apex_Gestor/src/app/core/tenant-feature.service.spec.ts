import { TestBed } from '@angular/core/testing';

import { LICENSE_STATUS_KEY } from './license-context.service';
import { LicenseValidationResponse } from './models';
import { TenantFeatureService } from './tenant-feature.service';

describe('TenantFeatureService', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  function freshService(): TenantFeatureService {
    TestBed.resetTestingModule();
    return TestBed.inject(TenantFeatureService);
  }

  it('mantém plano Essential com features básicas quando não há licença em cache', () => {
    TestBed.configureTestingModule({});
    const service = freshService();

    expect(service.context().tenantCode).toBe('apex-demo');
    expect(service.context().tenantName).toBe('Apex Demo');
    expect(service.context().subscriptionTier).toBe('Essential');
    expect(service.context().branding?.primaryColor).toBe('#0b3a42');
    expect(service.hasFeature('BASIC_PRICING')).toBeTrue();
    expect(service.hasFeature('FINANCIAL_CORE')).toBeTrue();
    expect(service.hasFeature('ADVANCED_FINANCE')).toBeFalse();
    expect(service.isWhiteLabelB2C()).toBeFalse();
  });

  it('descarta features desconhecidas vindas do cache', () => {
    localStorage.setItem(LICENSE_STATUS_KEY, JSON.stringify({
      tenantCode: 'hml-retail',
      tenantName: 'HML Retail',
      subscriptionTier: 'Pro',
      features: ['FEATURE_NAO_RECONHECIDA', 'ADVANCED_FINANCE', 'FINANCIAL_CORE']
    }));

    TestBed.configureTestingModule({});
    const service = freshService();

    expect(service.context().tenantCode).toBe('hml-retail');
    expect(service.context().features).toEqual(['ADVANCED_FINANCE', 'FINANCIAL_CORE']);
    expect(service.hasFeature('ADVANCED_FINANCE')).toBeTrue();
  });

  it('usa licensePlan quando subscriptionTier não vem na validação', () => {
    localStorage.setItem(LICENSE_STATUS_KEY, JSON.stringify({
      tenantCode: 't1',
      features: ['FINANCIAL_CORE'],
      licensePlan: 'Growth'
    }));

    TestBed.configureTestingModule({});
    const service = freshService();

    expect(service.context().subscriptionTier).toBe('Growth');
    expect(service.context().tenantName).toBe('Apex Demo');
  });

  it('volta ao Essential quando o cache está corrompido', () => {
    localStorage.setItem(LICENSE_STATUS_KEY, '{invalid');

    TestBed.configureTestingModule({});
    const service = freshService();

    expect(service.context().tenantCode).toBe('apex-demo');
    expect(service.context().subscriptionTier).toBe('Essential');
  });

  it('recarrega o cache atualizado via refreshFromCache', () => {
    TestBed.configureTestingModule({});
    const service = freshService();
    expect(service.context().tenantName).toBe('Apex Demo');

    localStorage.setItem(LICENSE_STATUS_KEY, JSON.stringify({
      tenantCode: 'loja-07',
      tenantName: 'Loja 07',
      features: ['FINANCIAL_CORE']
    }));
    service.refreshFromCache();

    expect(service.context().tenantName).toBe('Loja 07');
    expect(service.context().tenantCode).toBe('loja-07');
  });

  it('aplica validação de licença em execução mantendo fallback do plano', () => {
    TestBed.configureTestingModule({});
    const service = freshService();

    const validation: LicenseValidationResponse = {
      valid: true,
      status: 'VALID',
      message: 'ok',
      tenantCode: 'loja-09',
      features: ['FINANCIAL_CORE', 'B2C_WHITE_LABEL']
    };
    service.updateFromLicense(validation);

    expect(service.hasFeature('B2C_WHITE_LABEL')).toBeTrue();
    expect(service.context().tenantCode).toBe('loja-09');
    expect(service.context().subscriptionTier).toBe('Essential');
    expect(service.context().branding?.storefrontName).toBe('Apex Store');
  });

  it('valida conjuntos com hasAll e lista vazia', () => {
    TestBed.configureTestingModule({});
    const service = freshService();

    expect(service.hasAll(['BASIC_PRICING', 'FINANCIAL_CORE'])).toBeTrue();
    expect(service.hasAll(['BASIC_PRICING', 'B2C_WHITE_LABEL'])).toBeFalse();
    expect(service.hasAll()).toBeTrue();
  });

  it('mantém white-label B2C inativo na variante staff mesmo quando a feature existe', () => {
    TestBed.configureTestingModule({});
    const service = freshService();

    const validation: LicenseValidationResponse = {
      valid: true,
      status: 'VALID',
      message: 'ok',
      tenantCode: 'loja-personalizada',
      features: ['B2C_WHITE_LABEL'],
      branding: {
        storefrontName: 'Marca Própria',
        primaryColor: '#123456',
        secondaryColor: '#654321'
      }
    };
    service.updateFromLicense(validation);

    expect(service.hasFeature('B2C_WHITE_LABEL')).toBeTrue();
    expect(service.isWhiteLabelB2C()).toBeFalse();
    expect(document.documentElement.style.getPropertyValue('--apex-primary')).toBe('');
  });

  it('sincroniza entitlement autenticado com ADVANCED_FINANCE e flag de contingencia', () => {
    TestBed.configureTestingModule({});
    const service = freshService();

    service.updateFromEntitlement({
      empresaId: 10,
      tenantCode: 'grupo-alfa',
      tenantName: 'Grupo Alfa',
      subscriptionTier: 'PREMIUM',
      features: ['BASIC_PRICING', 'FINANCIAL_CORE', 'ADVANCED_FINANCE'],
      status: 'CONTINGENCIA',
      gracePeriodHoras: 72,
      emGracePeriod: true
    });

    expect(service.hasFeature('ADVANCED_FINANCE')).toBeTrue();
    expect(service.context().tenantCode).toBe('grupo-alfa');
    expect(service.context().subscriptionTier).toBe('PREMIUM');
    expect(service.context().status).toBe('CONTINGENCIA');
    expect(service.context().degradedMode).toBeTrue();
  });
});
