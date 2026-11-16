import { TestBed } from '@angular/core/testing';

import {
  DEVICE_KEY,
  LICENSE_KEY,
  LICENSE_STATUS_KEY,
  TENANT_CODE_KEY,
  LicenseContextService
} from './license-context.service';
import { SecureStorageService } from './secure-storage.service';

describe('LicenseContextService', () => {
  let secureStorage: jasmine.SpyObj<SecureStorageService>;
  let service: LicenseContextService;

  beforeEach(() => {
    localStorage.clear();
    delete window.apexDesktop;

    secureStorage = jasmine.createSpyObj<SecureStorageService>('SecureStorageService', ['getItem', 'setItem', 'removeItem']);
    secureStorage.getItem.and.resolveTo(null);
    secureStorage.setItem.and.resolveTo();
    secureStorage.removeItem.and.resolveTo();

    TestBed.configureTestingModule({
      providers: [
        LicenseContextService,
        { provide: SecureStorageService, useValue: secureStorage }
      ]
    });
    service = TestBed.inject(LicenseContextService);
  });

  afterEach(() => {
    delete window.apexDesktop;
    localStorage.clear();
  });

  it('identifica desktop quando a bridge apexDesktop está presente', () => {
    window.apexDesktop = { isDesktop: true };

    expect(service.appId()).toBe('desktop');
  });

  it('mapeia a variante staff para o app id mobile-staff', () => {
    expect(service.appId()).toBe('mobile-staff');
  });

  it('persiste e lê a chave de licença via armazenamento seguro', async () => {
    secureStorage.getItem.and.resolveTo('lic-123');

    expect(await service.readLicenseKey()).toBe('lic-123');

    await service.storeLicenseKey('lic-456');

    expect(secureStorage.setItem).toHaveBeenCalledWith(LICENSE_KEY, 'lic-456');
  });

  it('prioriza o tenant do status de licença em cache', () => {
    localStorage.setItem(LICENSE_STATUS_KEY, JSON.stringify({ tenantCode: 'hml-retail' }));

    expect(service.tenantCode()).toBe('hml-retail');
  });

  it('ignora cache corrompido e cai no tenant configurado', () => {
    localStorage.setItem(LICENSE_STATUS_KEY, 'not-json');
    localStorage.setItem(TENANT_CODE_KEY, 'loja-01');

    expect(service.tenantCode()).toBe('loja-01');
  });

  it('usa texto padrão quando não há tenant configurado', () => {
    expect(service.tenantCode()).toBe('apex-demo');

    service.storeTenantCode('');

    expect(service.tenantCode()).toBe('apex-demo');

    service.storeTenantCode('loja-02');

    expect(service.tenantCode()).toBe('loja-02');
  });

  it('monta cabeçalhos de licença quando a chave existe', async () => {
    localStorage.setItem(DEVICE_KEY, 'device-abc');
    localStorage.setItem(TENANT_CODE_KEY, 'loja-01');
    secureStorage.getItem.and.resolveTo('lic-123');

    const headers = await service.licenseHeaders();

    expect(headers).toEqual({
      'X-Apex-License-Key': 'lic-123',
      'X-Apex-Device-Fingerprint': 'device-abc',
      'X-Apex-App-Id': 'mobile-staff',
      'X-Apex-Tenant-Code': 'loja-01'
    });
  });

  it('não envia cabeçalhos de licença sem chave armazenada', async () => {
    const headers = await service.licenseHeaders();

    expect(headers).toEqual({});
    expect(secureStorage.getItem).toHaveBeenCalledWith(LICENSE_KEY);
  });

  it('conserva o fingerprint do dispositivo entre chamadas', async () => {
    const first = await service.deviceInfo();
    const second = await service.deviceInfo();

    expect(second.deviceFingerprint).toBe(first.deviceFingerprint);
    expect(second.deviceFingerprint).not.toBe('');
    expect(second.deviceLabel).toBe('Apex Mobile Empresa');
    expect(localStorage.getItem(DEVICE_KEY)).toBe(second.deviceFingerprint);
  });

  it('usa a bridge desktop para informações do dispositivo', async () => {
    window.apexDesktop = {
      isDesktop: true,
      getDeviceInfo: jasmine.createSpy('getDeviceInfo').and.resolveTo({
        defaultApiUrl: 'http://localhost:8080',
        deviceFingerprint: 'desk-fp',
        deviceLabel: 'PDV Caixa',
        isDesktop: true,
        platform: 'win32'
      })
    };

    const info = await service.deviceInfo();

    expect(info.appId).toBe('desktop');
    expect(info.deviceFingerprint).toBe('desk-fp');
    expect(info.platform).toBe('win32');
    expect(info.deviceLabel).toBe('PDV Caixa');
  });
});
