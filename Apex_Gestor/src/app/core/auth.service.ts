import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, from, switchMap, tap } from 'rxjs';

import { ApiConfigService } from './api-config.service';
import { ApiReadinessService } from './api-readiness.service';
import { AuthResponse, ConsentRequest, LoginRequest, Persona } from './models';
import { SecureStorageService } from './secure-storage.service';
import { SessionService } from './session.service';

const ACCESS_TOKEN_KEY = 'apex.accessToken';
const REFRESH_TOKEN_KEY = 'apex.refreshToken';
const USER_KEY = 'apex.authUser';
const SENSITIVE_CONTEXT_KEYS = [
  'apex.activeEmpresaId',
  'apex.activeFilialId',
  'apex.licenseStatus'
];

export type LogoutReason = 'manual' | 'expired' | 'forbidden' | 'idle';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiConfig = inject(ApiConfigService);
  private readonly readiness = inject(ApiReadinessService);
  private readonly secureStorage = inject(SecureStorageService);
  private readonly session = inject(SessionService);

  readonly accessToken = signal(sessionStorage.getItem(ACCESS_TOKEN_KEY));
  readonly user = signal<AuthResponse | null>(this.readStoredUser());
  readonly isAuthenticated = computed(() => Boolean(this.accessToken()));

  constructor() {
    this.reconcileStoredSession();
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post<AuthResponse>(this.apiConfig.apiUrl('/api/auth/login'), request)),
      tap((response) => this.storeSession(response))
    );
  }

  refresh(): Observable<AuthResponse> {
    return from(this.secureStorage.getItem(REFRESH_TOKEN_KEY)).pipe(
      switchMap((refreshToken) => this.readiness.waitForApi().pipe(
        switchMap(() => this.http.post<AuthResponse>(this.apiConfig.apiUrl('/api/auth/refresh'), {
          refreshToken: refreshToken ?? ''
        }))
      )),
      tap((response) => this.storeSession(response))
    );
  }

  logout(reason: LogoutReason = 'manual'): void {
    // The request is subscribed before the local token is removed so the backend can
    // identify the actor. No token or secret is copied into the audit payload.
    this.http.post(this.apiConfig.apiUrl('/api/auth/logout'), { reason }).subscribe({ error: () => undefined });
    sessionStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    SENSITIVE_CONTEXT_KEYS.forEach((key) => localStorage.removeItem(key));
    void this.secureStorage.removeItem(REFRESH_TOKEN_KEY);
    this.accessToken.set(null);
    this.user.set(null);
    this.session.clearAuthContext();
    window.dispatchEvent(new CustomEvent('apex-auth-cleared', { detail: { reason } }));
  }

  registerConsent(request: ConsentRequest): Observable<unknown> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post(this.apiConfig.apiUrl('/api/privacy/consents'), request))
    );
  }

  personaFromRoles(roles: string[]): Persona {
    const normalized = roles.map((role) => this.session.normalizeRole(role));
    if (normalized.some((role) => ['ROLE_SYSADMIN', 'ROLE_ADMIN'].includes(role))) {
      return 'admin';
    }
    if (normalized.some((role) => ['ROLE_DONO_GERENTE', 'ROLE_GERENTE', 'ROLE_GESTOR', 'ROLE_FINANCEIRO', 'ROLE_CAIXA', 'ROLE_DESPACHANTE'].includes(role))) {
      return 'gerente';
    }
    if (normalized.includes('ROLE_VENDEDOR')) {
      return 'vendedor';
    }
    return 'cliente';
  }

  private storeSession(response: AuthResponse): void {
    const safeUser = { ...response, refreshToken: '' };
    sessionStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    void this.secureStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(safeUser));
    this.accessToken.set(response.accessToken);
    this.user.set(safeUser);
    this.session.setRoles(response.roles);
    this.session.setPersona(this.personaFromRoles(response.roles));
    this.session.setAccessIssue(null);
  }

  private reconcileStoredSession(): void {
    const token = this.accessToken();
    const storedUser = this.user();
    if (!token) {
      localStorage.removeItem(REFRESH_TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      void this.secureStorage.removeItem(REFRESH_TOKEN_KEY);
      this.user.set(null);
      this.session.clearAuthContext();
      return;
    }
    if (storedUser) {
      this.session.setRoles(storedUser.roles);
      this.session.setPersona(this.personaFromRoles(storedUser.roles));
    }
  }

  private readStoredUser(): AuthResponse | null {
    const value = localStorage.getItem(USER_KEY);
    if (!value) {
      return null;
    }
    try {
      return JSON.parse(value) as AuthResponse;
    } catch {
      return null;
    }
  }
}
