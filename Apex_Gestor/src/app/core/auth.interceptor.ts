import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, from, switchMap, throwError } from 'rxjs';

import { AuthService } from './auth.service';
import { LicenseContextService } from './license-context.service';
import { SessionService } from './session.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const licenseContext = inject(LicenseContextService);
  const session = inject(SessionService);
  const token = auth.accessToken();
  const isApiRequest = request.url.includes('/api/');

  if (!isApiRequest) {
    return next(request);
  }

  const baseHeaders: Record<string, string> = {
    'X-Client-Platform': platformLabel()
  };

  if (token) {
    baseHeaders['Authorization'] = `Bearer ${token}`;
  }
  return from(licenseContext.licenseHeaders()).pipe(
    switchMap((licenseHeaders) => next(request.clone({
      setHeaders: {
        ...baseHeaders,
        ...licenseHeaders
      }
    })).pipe(
      catchError((error) => {
        if (error?.status === 401) {
          auth.logout('expired');
          session.setAccessIssue('Sessão expirada. Entre novamente para continuar.');
          void router.navigate(['/login'], { queryParams: { reason: 'session-expired', redirect: router.url } });
        } else if (error?.status === 403) {
          const code = error?.error?.code;
          if (code === 'FEATURE_NOT_LICENSED') {
            session.setAccessIssue('Funcionalidade não contratada para o plano desta empresa.');
          } else if (code === 'SOD_VIOLATION') {
            session.setAccessIssue(error?.error?.detail ?? 'Operação bloqueada por segregação de funções (SoD).');
          } else {
            session.setAccessIssue('Acesso negado para este perfil.');
          }
        }
        return throwError(() => error);
      })
    ))
  );
};

function platformLabel(): string {
  const protocol = globalThis.location?.protocol ?? 'http:';
  if (window.apexDesktop?.isDesktop || protocol === 'app:') {
    return 'electron';
  }
  if (protocol === 'capacitor:' || protocol === 'ionic:') {
    return 'mobile';
  }
  return 'web';
}
