import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, from, switchMap, tap } from 'rxjs';

import { ApiConfigService } from './api-config.service';
import { ApiReadinessService } from './api-readiness.service';
import { DEMO_MODE } from './app-variant';
import { AuthResponse, ConsentRequest, LoginRequest, Persona } from './models';
import { SecureStorageService } from './secure-storage.service';
import { SessionService } from './session.service';

const ACCESS_TOKEN_KEY = 'apex.accessToken';
const ACCESS_TOKEN_EXPIRES_AT_KEY = 'apex.accessTokenExpiresAt';
const REFRESH_TOKEN_KEY = 'apex.refreshToken';
const USER_KEY = 'apex.authUser';
const SENSITIVE_CONTEXT_KEYS = [
  'apex.activeEmpresaId',
  'apex.activeFilialId',
  'apex.licenseStatus'
];
const ACCESS_TOKEN_EXPIRY_GRACE_MS = 30_000;

export type LogoutReason = 'manual' | 'expired' | 'forbidden' | 'idle';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiConfig = inject(ApiConfigService);
  private readonly readiness = inject(ApiReadinessService);
  private readonly secureStorage = inject(SecureStorageService);
  private readonly session = inject(SessionService);

  readonly accessToken = signal<string | null>(this.readStoredAccessToken());
  private readonly accessExpiresAt = signal<number | null>(this.readStoredAccessExpiry());
  readonly user = signal<AuthResponse | null>(this.readStoredUser());
  private readonly demoAuthenticated = signal(false);
  readonly isAuthenticated = computed(() => this.isUsableNow() || (DEMO_MODE && this.demoAuthenticated()));

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

  enterDemoSession(roles: string[]): boolean {
    if (!DEMO_MODE) {
      return false;
    }
    this.clearStoredSession();
    this.demoAuthenticated.set(true);
    this.session.setRoles(roles);
    this.session.setPersona(this.personaFromRoles(roles));
    this.session.setAccessIssue(null);
    return true;
  }

  logout(reason: LogoutReason = 'manual'): void {
    // The request is subscribed before the local token is removed so the backend can
    // identify the actor. No token or secret is copied into the audit payload.
    this.http.post(this.apiConfig.apiUrl('/api/auth/logout'), { reason }).subscribe({ error: () => undefined });
    this.clearStoredSession();
    window.dispatchEvent(new CustomEvent('apex-auth-cleared', { detail: { reason } }));
  }

  registerConsent(request: ConsentRequest): Observable<unknown> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post(this.apiConfig.apiUrl('/api/privacy/consents'), request))
    );
  }

  personaFromRoles(roles: string[]): Persona {
    const normalized = roles.map((role) => this.session.normalizeRole(role));
    if (normalized.some((role) => [
      'ROLE_SYSADMIN',
      'ROLE_ADMIN',
      'ROLE_ADMIN_MASTER',
      'ROLE_ADMINISTRACAO'
    ].includes(role))) {
      return 'admin';
    }
    if (normalized.some((role) => [
      'ROLE_DONO',
      'ROLE_DONO_GERENTE',
      'ROLE_GERENTE',
      'ROLE_GESTOR',
      'ROLE_FINANCEIRO',
      'ROLE_FINANCAS',
      'ROLE_CAIXA',
      'ROLE_DESPACHANTE',
      'ROLE_AUDITOR',
      'ROLE_CONTADOR',
      'ROLE_ADVOGADO'
    ].includes(role))) {
      return 'gerente';
    }
    if (normalized.includes('ROLE_VENDEDOR')) {
      return 'vendedor';
    }
    return 'cliente';
  }

  private clearStoredSession(): void {
    sessionStorage.removeItem(ACCESS_TOKEN_KEY);
    sessionStorage.removeItem(ACCESS_TOKEN_EXPIRES_AT_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    SENSITIVE_CONTEXT_KEYS.forEach((key) => localStorage.removeItem(key));
    void this.secureStorage.removeItem(REFRESH_TOKEN_KEY);
    this.accessToken.set(null);
    this.accessExpiresAt.set(null);
    this.user.set(null);
    this.demoAuthenticated.set(false);
    this.session.clearAuthContext();
  }

  private storeSession(response: AuthResponse): void {
    const safeUser = { ...response, refreshToken: '' };
    const expiresAt = this.parseExpiresAt(response.expiresAt);
    this.demoAuthenticated.set(false);
    sessionStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken);
    if (expiresAt === null) {
      sessionStorage.removeItem(ACCESS_TOKEN_EXPIRES_AT_KEY);
    } else {
      sessionStorage.setItem(ACCESS_TOKEN_EXPIRES_AT_KEY, String(expiresAt));
    }
    this.accessToken.set(response.accessToken);
    this.accessExpiresAt.set(expiresAt);
    void this.secureStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(safeUser));
    this.user.set(safeUser);
    this.session.setRoles(response.roles);
    this.session.setPersona(this.personaFromRoles(response.roles));
    this.session.setAccessIssue(null);
  }

  private reconcileStoredSession(): void {
    this.demoAuthenticated.set(false);
    if (!this.isUsableNow()) {
      this.clearStoredSession();
      return;
    }
    const storedUser = this.user();
    if (storedUser) {
      this.session.setRoles(storedUser.roles);
      this.session.setPersona(this.personaFromRoles(storedUser.roles));
    }
  }

  private isUsableNow(): boolean {
    const token = this.accessToken();
    if (!token) {
      return false;
    }
    const expiresAt = this.accessExpiresAt();
    if (expiresAt === null) {
      return true;
    }
    return Date.now() < expiresAt - ACCESS_TOKEN_EXPIRY_GRACE_MS;
  }

  private parseExpiresAt(expiresAt: string | undefined | null): number | null {
    if (!expiresAt) {
      return null;
    }
    const numeric = Number(expiresAt);
    const milliseconds = Number.isFinite(numeric)
      ? (numeric < 1e12 ? numeric * 1000 : numeric)
      : Date.parse(expiresAt);
    return Number.isFinite(milliseconds) ? milliseconds : null;
  }

  private readStoredAccessToken(): string | null {
    return sessionStorage.getItem(ACCESS_TOKEN_KEY);
  }

  private readStoredAccessExpiry(): number | null {
    const raw = sessionStorage.getItem(ACCESS_TOKEN_EXPIRES_AT_KEY);
    const parsed = raw ? Number(raw) : NaN;
    return Number.isFinite(parsed) ? parsed : null;
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
