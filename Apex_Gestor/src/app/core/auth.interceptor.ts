import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const token = auth.accessToken();
  const isApiRequest = request.url.includes('/api/');

  if (!token || !isApiRequest) {
    return next(request);
  }

  return next(request.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`,
      'X-Client-Platform': platformLabel()
    }
  }));
};

function platformLabel(): string {
  const protocol = globalThis.location?.protocol ?? 'http:';
  if (protocol === 'app:') {
    return 'electron';
  }
  if (protocol === 'capacitor:' || protocol === 'ionic:') {
    return 'mobile';
  }
  return 'web';
}
