import { TestBed } from '@angular/core/testing';

import { DEFAULT_PERSONA } from './app-variant';
import { SessionService } from './session.service';

describe('SessionService', () => {
  let service: SessionService;

  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
    service = TestBed.inject(SessionService);
  });

  afterEach(() => {
    sessionStorage.clear();
    localStorage.clear();
  });

  it('normaliza e persiste os papéis da sessão', () => {
    service.setRoles(['vendedor', 'ROLE_CAIXA']);

    expect(service.roles().map((role) => String(role))).toEqual(['ROLE_vendedor', 'ROLE_CAIXA']);
    expect(JSON.parse(localStorage.getItem('apex.roles') ?? '[]')).toEqual(['ROLE_vendedor', 'ROLE_CAIXA']);
  });

  it('verifica permissões com hasAnyRole incluindo lista vazia', () => {
    expect(service.hasAnyRole(['ROLE_CAIXA'])).toBeFalse();
    expect(service.hasAnyRole(undefined)).toBeTrue();
    expect(service.hasAnyRole([])).toBeTrue();

    service.setRoles(['ROLE_CAIXA']);
    expect(service.hasAnyRole(['ROLE_CAIXA', 'ROLE_GERENTE'])).toBeTrue();
    expect(service.hasAnyRole(['ROLE_VENDEDOR'])).toBeFalse();
  });

  it('limpa contexto autenticado e volta para a persona padrão', () => {
    service.setRoles(['ROLE_CAIXA']);
    service.setPersona('vendedor');
    service.setAccessIssue('Sessão expirada. Entre novamente para continuar.');

    service.clearAuthContext();

    expect(service.roles()).toEqual([]);
    expect(service.persona()).toBe(DEFAULT_PERSONA);
    expect(service.accessIssue()).toBeNull();
    expect(localStorage.getItem('apex.roles')).toBeNull();
    expect(localStorage.getItem('apex.persona')).toBeNull();
  });

  it('prioriza o papel primário no rótulo exibido', () => {
    service.setRoles(['ROLE_CAIXA']);
    expect(service.roleLabel()).toBe('Caixa');

    service.clearAuthContext();
    expect(service.roleLabel()).toBe('Gerente');
  });

  it('restaura persona e papéis persistidos ao iniciar', () => {
    TestBed.resetTestingModule();
    localStorage.setItem('apex.persona', 'vendedor');
    localStorage.setItem('apex.roles', JSON.stringify(['ROLE_VENDEDOR']));

    const restored = TestBed.inject(SessionService);

    expect(restored.persona()).toBe('vendedor');
    expect(restored.roles()).toEqual(['ROLE_VENDEDOR']);
  });

  it('ignora papéis corrompidos no storage', () => {
    TestBed.resetTestingModule();
    localStorage.setItem('apex.roles', 'not-json');

    const restored = TestBed.inject(SessionService);

    expect(restored.roles()).toEqual([]);
  });
});
