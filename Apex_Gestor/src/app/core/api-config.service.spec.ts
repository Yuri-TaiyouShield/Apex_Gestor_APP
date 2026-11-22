import { TestBed } from '@angular/core/testing';
import { Capacitor } from '@capacitor/core';

import { ApiConfigService } from './api-config.service';

describe('ApiConfigService', () => {
  beforeEach(() => {
    localStorage.clear();
    delete window.apexDesktop;
  });

  afterEach(() => {
    localStorage.clear();
    delete window.apexDesktop;
  });

  function freshService(): ApiConfigService {
    TestBed.resetTestingModule();
    return TestBed.inject(ApiConfigService);
  }

  it('usa localhost:8080 como base no ambiente web de execução local', () => {
    const service = freshService();

    expect(service.baseUrl()).toBe('http://localhost:8080');
  });

  it('prefere a base informada pela bridge desktop', () => {
    window.apexDesktop = { defaultApiUrl: 'http://127.0.0.1:9090' };

    const service = freshService();

    expect(service.baseUrl()).toBe('http://127.0.0.1:9090');
  });

  it('persiste a base configurada sem barra final', () => {
    const service = freshService();

    service.setBaseUrl(' http://api.local:8080/ ');

    expect(service.baseUrl()).toBe('http://api.local:8080');
    expect(localStorage.getItem('apex.apiBaseUrl')).toBe('http://api.local:8080');
  });

  it('recupera a base persistida ao recriar o serviço', () => {
    freshService().setBaseUrl('http://api.persistida:8080');

    const restored = freshService();

    expect(restored.baseUrl()).toBe('http://api.persistida:8080');
  });

  it('reset volta à base padrão e limpa o storage', () => {
    const service = freshService();
    service.setBaseUrl('http://api.local:8080');

    service.reset();

    expect(service.baseUrl()).toBe('http://localhost:8080');
    expect(localStorage.getItem('apex.apiBaseUrl')).toBeNull();
  });

  it('normaliza paths na URL final da API e da saúde', () => {
    const service = freshService();

    expect(service.apiUrl('api/produtos')).toBe('http://localhost:8080/api/produtos');
    expect(service.apiUrl('/api/produtos')).toBe('http://localhost:8080/api/produtos');
    expect(service.healthUrl()).toBe('http://localhost:8080/actuator/health');
  });

  it('usa 10.0.2.2:8080 para emulador Android nativo', () => {
    spyOn(Capacitor, 'isNativePlatform').and.returnValue(true);
    spyOn(Capacitor, 'getPlatform').and.returnValue('android');

    const service = freshService();

    expect(service.baseUrl()).toBe('http://10.0.2.2:8080');
  });

  it('usa localhost:8080 para plataformas nativas não-Android', () => {
    spyOn(Capacitor, 'isNativePlatform').and.returnValue(true);
    spyOn(Capacitor, 'getPlatform').and.returnValue('ios');

    const service = freshService();

    expect(service.baseUrl()).toBe('http://localhost:8080');
  });

  it('ignora cache legado na porta 8085 e usa http://localhost:8080', () => {
    localStorage.setItem('apex.apiBaseUrl', 'http://localhost:8085');

    const service = freshService();

    expect(service.baseUrl()).toBe('http://localhost:8080');
  });
});
