import { HttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ApiConfigService } from './api-config.service';
import { ApiReadinessService } from './api-readiness.service';
import { AuthService } from './auth.service';
import { SecureStorageService } from './secure-storage.service';
import { SessionService } from './session.service';

describe('AuthService logout', () => {
  let http: jasmine.SpyObj<HttpClient>;
  let secureStorage: jasmine.SpyObj<SecureStorageService>;
  let session: jasmine.SpyObj<SessionService>;

  beforeEach(() => {
    sessionStorage.setItem('apex.accessToken', 'access-secret');
    localStorage.setItem('apex.refreshToken', 'refresh-secret');
    localStorage.setItem('apex.authUser', JSON.stringify({ login: 'v5_vendedor', roles: ['ROLE_VENDEDOR'] }));
    localStorage.setItem('apex.activeEmpresaId', '1');
    localStorage.setItem('apex.activeFilialId', '1');
    localStorage.setItem('apex.licenseStatus', JSON.stringify({ tenantCode: 'hml' }));

    http = jasmine.createSpyObj<HttpClient>('HttpClient', ['post']);
    http.post.and.returnValue(of(null));
    secureStorage = jasmine.createSpyObj<SecureStorageService>('SecureStorageService', ['getItem', 'setItem', 'removeItem']);
    secureStorage.removeItem.and.resolveTo();
    session = jasmine.createSpyObj<SessionService>('SessionService', [
      'clearAuthContext', 'setRoles', 'setPersona', 'setAccessIssue', 'normalizeRole'
    ]);

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        { provide: HttpClient, useValue: http },
        { provide: ApiConfigService, useValue: { apiUrl: (path: string) => `http://127.0.0.1:8080${path}` } },
        { provide: ApiReadinessService, useValue: {} },
        { provide: SecureStorageService, useValue: secureStorage },
        { provide: SessionService, useValue: session }
      ]
    });
  });

  afterEach(() => {
    sessionStorage.clear();
    localStorage.clear();
  });

  it('clears session, user, permissions and tenant-sensitive cache on inactivity', () => {
    const service = TestBed.inject(AuthService);

    service.logout('idle');

    expect(service.accessToken()).toBeNull();
    expect(service.user()).toBeNull();
    expect(sessionStorage.getItem('apex.accessToken')).toBeNull();
    expect(localStorage.getItem('apex.refreshToken')).toBeNull();
    expect(localStorage.getItem('apex.authUser')).toBeNull();
    expect(localStorage.getItem('apex.activeEmpresaId')).toBeNull();
    expect(localStorage.getItem('apex.activeFilialId')).toBeNull();
    expect(localStorage.getItem('apex.licenseStatus')).toBeNull();
    expect(session.clearAuthContext).toHaveBeenCalled();
    expect(secureStorage.removeItem).toHaveBeenCalledWith('apex.refreshToken');
  });

  it('audits only the logout reason and never copies tokens to the request body', () => {
    const service = TestBed.inject(AuthService);

    service.logout('idle');

    const body = http.post.calls.mostRecent().args[1] as Record<string, unknown>;
    expect(body).toEqual({ reason: 'idle' });
    expect(JSON.stringify(body)).not.toContain('secret');
    expect(JSON.stringify(body)).not.toContain('token');
  });
});
