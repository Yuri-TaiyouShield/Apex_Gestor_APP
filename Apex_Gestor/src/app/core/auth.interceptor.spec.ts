import { HttpErrorResponse, HttpEvent, HttpRequest } from '@angular/common/http';
import { Router } from '@angular/router';
import { EnvironmentInjector, runInInjectionContext } from '@angular/core';
import { TestBed } from '@angular/core/testing';import { Observable, of, throwError } from 'rxjs';

import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { LicenseContextService } from './license-context.service';
import { SessionService } from './session.service';

describe('authInterceptor', () => {
  let captured: HttpRequest<unknown> | undefined;
  let backend: Observable<HttpEvent<unknown>>;

  function configureBackend(behavior: 'ok' | 'unauthorized' | 'forbidden'): Observable<HttpEvent<unknown>> {
    if (behavior === 'ok') {
      backend = of({ body: [] } as HttpEvent<unknown>);
      return backend;
    }
    const failing = throwError(() => new HttpErrorResponse({ status: behavior === 'forbidden' ? 403 : 401 }));
    backend = failing;
    return backend;
  }

  /** Executa o interceptor em contexto de DI (exigência de `inject()` em function interceptors). */
  function runInterceptor(request: HttpRequest<unknown>): Observable<HttpEvent<unknown>> {
    const next = (req: HttpRequest<unknown>): Observable<HttpEvent<unknown>> => {
      captured = req;
      return backend;
    };
    return runInInjectionContext(TestBed.inject(EnvironmentInjector), () => authInterceptor(request, next));
  }

  function configureTestBed(options: { accessToken?: string | null } = {}): {
    logout: jasmine.Spy;
    setAccessIssue: jasmine.Spy;
    navigate: jasmine.Spy;
  } {
    const logout = jasmine.createSpy('logout');
    const setAccessIssue = jasmine.createSpy('setAccessIssue');
    const navigate = jasmine.createSpy('navigate').and.resolveTo(true);
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { accessToken: () => options.accessToken ?? null, logout } },
        { provide: LicenseContextService, useValue: { licenseHeaders: () => Promise.resolve({ 'X-License-Tenant': 'hml' }) } },
        { provide: SessionService, useValue: { setAccessIssue } },
        { provide: Router, useValue: { navigate, url: '/pos' } }
      ]
    });
    return { logout, setAccessIssue, navigate };
  }  it('anexa plataforma, licença e Authorization nas chamadas da API', async () => {
    configureTestBed({ accessToken: 'token-secret' });
    configureBackend('ok');

    await runInterceptor(new HttpRequest('GET', 'http://127.0.0.1:8080/api/produtos')).toPromise();

    expect(captured?.headers.get('Authorization')).toBe('Bearer token-secret');
    expect(captured?.headers.get('X-Client-Platform')).toBe('web');
    expect(captured?.headers.get('X-License-Tenant')).toBe('hml');
  });

  it('repasa chamadas fora da API sem cabeçalhos sensíveis', async () => {
    configureTestBed({ accessToken: 'token-secret' });
    configureBackend('ok');

    await runInterceptor(new HttpRequest('GET', 'assets/config.json')).toPromise();

    expect(captured?.headers.get('Authorization')).toBeNull();
    expect(captured?.headers.get('X-License-Tenant')).toBeNull();
  });

  it('encerra a sessão e redireciona no 401', async () => {
    const spies = configureTestBed({ accessToken: 'token-secret' });
    configureBackend('unauthorized');

    let capturedError: unknown;
    try {
      await runInterceptor(new HttpRequest('GET', 'http://127.0.0.1:8080/api/produtos')).toPromise();
    } catch (error) {
      capturedError = error;
    }

    expect(spies.logout).toHaveBeenCalledWith('expired');
    expect(spies.setAccessIssue).toHaveBeenCalledWith('Sessão expirada. Entre novamente para continuar.');
    expect(spies.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { reason: 'session-expired', redirect: '/pos' }
    });
    expect(capturedError).toBeInstanceOf(HttpErrorResponse);
  });

  it('avisa acesso negado no 403 sem encerrar a sessão', async () => {
    const spies = configureTestBed({ accessToken: 'token-secret' });
    configureBackend('forbidden');

    let capturedError: unknown;
    try {
      await runInterceptor(new HttpRequest('GET', 'http://127.0.0.1:8080/api/financeiro/auditoria')).toPromise();
    } catch (error) {
      capturedError = error;
    }

    expect(spies.setAccessIssue).toHaveBeenCalledWith('Acesso negado para este perfil.');
    expect(spies.logout).not.toHaveBeenCalled();
    expect(spies.navigate).not.toHaveBeenCalled();
    expect(capturedError).toBeInstanceOf(HttpErrorResponse);
  });

  it('declara X-Client-Platform electron quando executando dentro do Electron', async () => {
    (window as unknown as { apexDesktop?: { isDesktop: boolean } }).apexDesktop = { isDesktop: true };
    try {
      configureTestBed({ accessToken: 'token-secret' });
      configureBackend('ok');

      await runInterceptor(new HttpRequest('GET', 'http://127.0.0.1:8080/api/produtos')).toPromise();

      expect(captured?.headers.get('X-Client-Platform')).toBe('electron');
    } finally {
      delete (window as unknown as { apexDesktop?: unknown }).apexDesktop;
    }
  });
});
