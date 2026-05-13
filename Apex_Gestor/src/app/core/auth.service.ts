import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, from, switchMap, tap } from 'rxjs';

import { ApiConfigService } from './api-config.service';
import { AuthResponse, ConsentRequest, LoginRequest, Persona } from './models';
import { SecureStorageService } from './secure-storage.service';
import { SessionService } from './session.service';

const ACCESS_TOKEN_KEY = 'apex.accessToken';
const REFRESH_TOKEN_KEY = 'apex.refreshToken';
const USER_KEY = 'apex.authUser';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiConfig = inject(ApiConfigService);
  private readonly secureStorage = inject(SecureStorageService);
  private readonly session = inject(SessionService);

  readonly accessToken = signal(sessionStorage.getItem(ACCESS_TOKEN_KEY));
  readonly user = signal<AuthResponse | null>(this.readStoredUser());
  readonly isAuthenticated = computed(() => Boolean(this.accessToken()));

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(this.apiConfig.apiUrl('/api/auth/login'), request).pipe(
      tap((response) => this.storeSession(response))
    );
  }

  refresh(): Observable<AuthResponse> {
    return from(this.secureStorage.getItem(REFRESH_TOKEN_KEY)).pipe(
      switchMap((refreshToken) => this.http.post<AuthResponse>(this.apiConfig.apiUrl('/api/auth/refresh'), {
        refreshToken: refreshToken ?? ''
      })),
      tap((response) => this.storeSession(response))
    );
  }

  logout(): void {
    void this.secureStorage.getItem(REFRESH_TOKEN_KEY).then((refreshToken) => {
      if (refreshToken) {
        this.http.post(this.apiConfig.apiUrl('/api/auth/logout'), { refreshToken }).subscribe({ error: () => undefined });
      }
    });
    sessionStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    void this.secureStorage.removeItem(REFRESH_TOKEN_KEY);
    this.accessToken.set(null);
    this.user.set(null);
  }

  registerConsent(request: ConsentRequest): Observable<unknown> {
    return this.http.post(this.apiConfig.apiUrl('/api/privacy/consents'), request);
  }

  personaFromRoles(roles: string[]): Persona {
    if (roles.includes('ADMIN')) {
      return 'admin';
    }
    if (roles.includes('GERENTE')) {
      return 'gerente';
    }
    if (roles.includes('VENDEDOR')) {
      return 'vendedor';
    }
    return 'cliente';
  }

  private storeSession(response: AuthResponse): void {
    sessionStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    void this.secureStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify({ ...response, refreshToken: '' }));
    this.accessToken.set(response.accessToken);
    this.user.set(response);
    this.session.setPersona(this.personaFromRoles(response.roles));
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
