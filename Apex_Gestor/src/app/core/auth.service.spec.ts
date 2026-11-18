import { HttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of, throwError } from 'rxjs';

import { ApiConfigService } from './api-config.service';
import { ApiReadinessService } from './api-readiness.service';
import { ApexRole, AuthResponse, LoginRequest } from './models';
import { AuthService } from './auth.service';
import { SecureStorageService } from './secure-storage.service';
import { SessionService } from './session.service';

const ACCESS_TOKEN_KEY = 'apex.accessToken';
const EXPIRES_AT_KEY = 'apex.accessTokenExpiresAt';
const REFRESH_TOKEN_KEY = 'apex.refreshToken';
const USER_KEY = 'apex.authUser';

interface StorageSeed {
  accessToken?: string;
  expiresAt?: string;
  refreshToken?: string;
  user?: Record<string, unknown>;
}

/** Payload conforme o contrato atual do backend (AuthResponse). */
function authResponse(expiresInSecondsAhead: number | null, overrides: Partial<AuthResponse> = {}): AuthResponse {
  return {
    accessToken: 'access-secret',
    refreshToken: 'refresh-secret',
    expiresAt: expiresInSecondsAhead === null ? '' : String(Math.floor(Date.now() / 1000) + expiresInSecondsAhead),
    tokenType: 'Bearer',
    usuarioId: 7,
    nome: 'Dono Teste',
    login: 'v5_dono',
    roles: ['ROLE_VENDEDOR'],
    ...overrides
  };
}

function configureAuthService(seed: StorageSeed = {}): {
  service: AuthService;
  http: jasmine.SpyObj<HttpClient>;
  secureStorage: jasmine.SpyObj<SecureStorageService>;
  session: jasmine.SpyObj<SessionService>;
} {
  TestBed.resetTestingModule();
  sessionStorage.clear();
  localStorage.clear();
  if (seed.accessToken !== undefined) {
    sessionStorage.setItem(ACCESS_TOKEN_KEY, seed.accessToken);
  }
  if (seed.expiresAt !== undefined) {
    sessionStorage.setItem(EXPIRES_AT_KEY, seed.expiresAt);
  }
  if (seed.refreshToken !== undefined) {
    localStorage.setItem(REFRESH_TOKEN_KEY, seed.refreshToken);
  }
  if (seed.user !== undefined) {
    localStorage.setItem(USER_KEY, JSON.stringify(seed.user));
  }
  localStorage.setItem('apex.activeEmpresaId', '1');
  localStorage.setItem('apex.activeFilialId', '1');
  localStorage.setItem('apex.licenseStatus', JSON.stringify({ tenantCode: 'hml' }));

  const http = jasmine.createSpyObj<HttpClient>('HttpClient', ['get', 'post', 'put', 'patch', 'delete']);
  http.post.and.returnValue(of(null));
  const secureStorage = jasmine.createSpyObj<SecureStorageService>('SecureStorageService', ['getItem', 'setItem', 'removeItem']);
  secureStorage.getItem.and.resolveTo(seed.refreshToken ?? null);
  secureStorage.setItem.and.resolveTo();
  secureStorage.removeItem.and.resolveTo();
  const session = jasmine.createSpyObj<SessionService>('SessionService', [
    'clearAuthContext', 'setRoles', 'setPersona', 'setAccessIssue', 'normalizeRole'
  ]);
  session.normalizeRole.and.callFake((role: string) => (role.startsWith('ROLE_') ? role : `ROLE_${role}`) as ApexRole);

  TestBed.configureTestingModule({
    providers: [
      AuthService,
      { provide: HttpClient, useValue: http },
      { provide: ApiConfigService, useValue: { apiUrl: (path: string) => `http://127.0.0.1:8080${path}` } },
      { provide: ApiReadinessService, useValue: { waitForApi: () => of(undefined) } },
      { provide: SecureStorageService, useValue: secureStorage },
      { provide: SessionService, useValue: session }
    ]
  });
  return { service: TestBed.inject(AuthService), http, secureStorage, session };
}

describe('AuthService session lifecycle', () => {
  describe('login', () => {
    it('stores access token, expiry metadata and a sanitized user copy', () => {
      const response = authResponse(3600);
      const { service, http, secureStorage, session } = configureAuthService();
      http.post.and.returnValue(of(response));

      const request: LoginRequest = { login: 'v5_dono', senha: 'Apex@2026' };
      const responseEvents: AuthResponse[] = [];
      service.login(request).subscribe((result) => responseEvents.push(result));

      expect(responseEvents).toContain(response);
      expect(service.accessToken()).toBe('access-secret');
      expect(service.user()).toEqual({ ...response, refreshToken: '' });
      expect(sessionStorage.getItem(ACCESS_TOKEN_KEY)).toBe('access-secret');
      const storedExpiry = Number(sessionStorage.getItem(EXPIRES_AT_KEY));
      expect(storedExpiry).toBeGreaterThan(Date.now() + 30 * 60_000);
      expect(storedExpiry).toBeLessThan(Date.now() + 90 * 60_000);
      const storedUser = JSON.parse(localStorage.getItem(USER_KEY) ?? '{}') as Record<string, unknown>;
      expect(storedUser['login']).toBe('v5_dono');
      expect(JSON.stringify(localStorage.getItem(USER_KEY) ?? '')).not.toContain('refresh-secret');
      expect(secureStorage.setItem).toHaveBeenCalledWith(REFRESH_TOKEN_KEY, 'refresh-secret');
      expect(session.setRoles).toHaveBeenCalledWith(['ROLE_VENDEDOR']);
      expect(session.setPersona).toHaveBeenCalledWith('vendedor');
      expect(session.setAccessIssue).toHaveBeenCalledWith(null);
    });
  });

  describe('refresh', () => {
    it('reuses the stored refresh token and persists the renewed session', async () => {
      const { service, http, secureStorage } = configureAuthService({ refreshToken: 'refresh-secret' });
      http.post.and.returnValue(of(authResponse(3600, { accessToken: 'access-renewed' })));

      const renewed = await firstValueFrom(service.refresh());

      expect(http.post).toHaveBeenCalledWith('http://127.0.0.1:8080/api/auth/refresh', { refreshToken: 'refresh-secret' });
      expect(renewed.accessToken).toBe('access-renewed');
      expect(service.accessToken()).toBe('access-renewed');
      expect(secureStorage.setItem).toHaveBeenCalledWith(REFRESH_TOKEN_KEY, 'refresh-secret');
    });

    it('propagates refresh failures without writing a fresh session', async () => {
      const { service, http, session } = configureAuthService();
      http.post.and.returnValue(throwError(() => new Error('refresh-failed')));
      const clearCallsBeforeFailure = session.clearAuthContext.calls.count();

      let capturedError: unknown;
      try {
        await firstValueFrom(service.refresh());
      } catch (error) {
        capturedError = error;
      }

      expect(capturedError).toBeInstanceOf(Error);
      expect(capturedError instanceof Error ? capturedError.message : '').toBe('refresh-failed');
      expect(service.accessToken()).toBeNull();
      expect(service.user()).toBeNull();
      expect(session.clearAuthContext.calls.count()).toBe(clearCallsBeforeFailure);
      expect(session.setRoles).not.toHaveBeenCalled();
    });
  });

  describe('reconcileStoredSession', () => {
    it('discards an expired token and its sensitive context at startup', () => {
      const { service, session, secureStorage } = configureAuthService({
        accessToken: 'access-secret',
        expiresAt: String(Date.now() - 1000),
        refreshToken: 'refresh-secret',
        user: { login: 'v5_dono', roles: ['ROLE_VENDEDOR'], usuarioId: 7 }
      });

      expect(service.accessToken()).toBeNull();
      expect(service.user()).toBeNull();
      expect(sessionStorage.getItem(ACCESS_TOKEN_KEY)).toBeNull();
      expect(sessionStorage.getItem(EXPIRES_AT_KEY)).toBeNull();
      expect(localStorage.getItem(REFRESH_TOKEN_KEY)).toBeNull();
      expect(localStorage.getItem(USER_KEY)).toBeNull();
      expect(localStorage.getItem('apex.activeEmpresaId')).toBeNull();
      expect(localStorage.getItem('apex.activeFilialId')).toBeNull();
      expect(localStorage.getItem('apex.licenseStatus')).toBeNull();
      expect(secureStorage.removeItem).toHaveBeenCalledWith(REFRESH_TOKEN_KEY);
      expect(session.clearAuthContext).toHaveBeenCalled();
    });

    it('treats a token inside the expiry grace window as expired', () => {
      const { service } = configureAuthService({
        accessToken: 'access-secret',
        expiresAt: String(Date.now() + 10_000)
      });

      expect(service.isAuthenticated()).toBeFalse();
      expect(service.accessToken()).toBeNull();
    });

    it('keeps a legacy token without expiry metadata usable', () => {
      const { service, session } = configureAuthService({
        accessToken: 'access-secret',
        user: { login: 'v5_dono', roles: ['ROLE_VENDEDOR'], usuarioId: 7 }
      });

      expect(service.isAuthenticated()).toBeTrue();
      expect(sessionStorage.getItem(ACCESS_TOKEN_KEY)).toBe('access-secret');
      expect(session.setRoles).toHaveBeenCalledWith(['ROLE_VENDEDOR']);
    });
  });

  describe('personaFromRoles', () => {
    it('maps normalized role sets to the operating persona', () => {
      const { service } = configureAuthService();

      expect(service.personaFromRoles(['ROLE_ADMIN'])).toBe('admin');
      expect(service.personaFromRoles(['ROLE_SYSADMIN'])).toBe('admin');
      expect(service.personaFromRoles(['ROLE_DONO_GERENTE'])).toBe('gerente');
      expect(service.personaFromRoles(['ROLE_CAIXA'])).toBe('gerente');
      expect(service.personaFromRoles(['VENDEDOR'])).toBe('vendedor');
      expect(service.personaFromRoles(['ROLE_CLIENTE_B2C'])).toBe('cliente');
      expect(service.personaFromRoles(['ROLE_UNKNOWN'])).toBe('cliente');
    });
  });

  describe('logout', () => {
    it('clears session, user, permissions and tenant-sensitive cache on inactivity', () => {
      const { service, secureStorage, session } = configureAuthService({
        accessToken: 'access-secret',
        refreshToken: 'refresh-secret',
        user: { login: 'v5_vendedor', roles: ['ROLE_VENDEDOR'] }
      });

      let clearedDetail: unknown;
      const listener = (event: Event): void => { clearedDetail = (event as CustomEvent).detail; };
      window.addEventListener('apex-auth-cleared', listener);
      try {
        service.logout('idle');
      } finally {
        window.removeEventListener('apex-auth-cleared', listener);
      }

      expect(service.accessToken()).toBeNull();
      expect(service.user()).toBeNull();
      expect(sessionStorage.getItem(ACCESS_TOKEN_KEY)).toBeNull();
      expect(sessionStorage.getItem(EXPIRES_AT_KEY)).toBeNull();
      expect(localStorage.getItem(REFRESH_TOKEN_KEY)).toBeNull();
      expect(localStorage.getItem(USER_KEY)).toBeNull();
      expect(localStorage.getItem('apex.activeEmpresaId')).toBeNull();
      expect(localStorage.getItem('apex.activeFilialId')).toBeNull();
      expect(localStorage.getItem('apex.licenseStatus')).toBeNull();
      expect(session.clearAuthContext).toHaveBeenCalled();
      expect(secureStorage.removeItem).toHaveBeenCalledWith(REFRESH_TOKEN_KEY);
      expect(clearedDetail).toEqual({ reason: 'idle' });
    });

    it('audits only the logout reason and never copies tokens to the request body', () => {
      const { service, http } = configureAuthService({
        accessToken: 'access-secret',
        refreshToken: 'refresh-secret'
      });

      service.logout('idle');

      const body = (http.post.calls.mostRecent().args[1] ?? {}) as Record<string, string>;
      expect(body).toEqual({ reason: 'idle' });
      expect(JSON.stringify(body)).not.toContain('secret');
      expect(JSON.stringify(body)).not.toContain('token');
    });

    it('armazena sessão com expiração em milissegundos, formato ISO e data inválida', async () => {
      const now = Date.now();
      const { service, http } = configureAuthService();

      http.post.and.returnValue(of(authResponse(null, {
        expiresAt: new Date(now + 120_000).toISOString()
      })));
      await firstValueFrom(service.login({ login: 'v5_dono', senha: '123' }));
      expect(service.isAuthenticated()).toBeTrue();

      http.post.and.returnValue(of(authResponse(null, {
        expiresAt: String(now + 120_000)
      })));
      await firstValueFrom(service.login({ login: 'v5_dono', senha: '123' }));
      expect(service.isAuthenticated()).toBeTrue();

      http.post.and.returnValue(of(authResponse(null, {
        expiresAt: 'invalid-date-format'
      })));
      await firstValueFrom(service.login({ login: 'v5_dono', senha: '123' }));
      expect(service.isAuthenticated()).toBeTrue();
    });

    it('trata expiração nula ou inválida no storage como sem bloqueio', () => {
      const { service } = configureAuthService({
        accessToken: 'token-sem-exp'
      });
      expect(service.isAuthenticated()).toBeTrue();

      const { service: serviceInvalid } = configureAuthService({
        accessToken: 'token-invalido',
        expiresAt: 'not-a-number'
      });
      expect(serviceInvalid.isAuthenticated()).toBeTrue();
    });
  });

  describe('enterDemoSession', () => {
    it('recusa sessão demo quando DEMO_MODE está desativado sem alterar estado ou tokens existentes', () => {
      const { service, session } = configureAuthService({
        accessToken: 'access-secret',
        expiresAt: String(Date.now() + 120_000),
        refreshToken: 'refresh-secret',
        user: { login: 'v5_vendedor', roles: ['ROLE_VENDEDOR'], usuarioId: 7 }
      });
      session.setRoles.calls.reset();
      session.setPersona.calls.reset();
      session.setAccessIssue.calls.reset();

      const entered = service.enterDemoSession(['ROLE_GERENTE']);

      expect(entered).toBeFalse();
      expect(service.accessToken()).toBe('access-secret');
      expect(service.isAuthenticated()).toBeTrue();
      expect(session.setRoles).not.toHaveBeenCalled();
      expect(session.setPersona).not.toHaveBeenCalled();
      expect(session.setAccessIssue).not.toHaveBeenCalled();
    });

    it('mantém isAuthenticated falso quando não há token e DEMO_MODE está desativado', () => {
      const { service, session } = configureAuthService();

      expect(service.enterDemoSession(['ROLE_CAIXA'])).toBeFalse();
      expect(service.isAuthenticated()).toBeFalse();
      expect(session.setRoles).not.toHaveBeenCalled();
    });
  });
});
