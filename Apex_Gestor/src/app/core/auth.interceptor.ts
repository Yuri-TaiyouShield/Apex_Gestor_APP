import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap } from 'rxjs';

import { AuthService } from './auth.service';
import { LicenseContextService } from './license-context.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const licenseContext = inject(LicenseContextService);
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
    })))
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
