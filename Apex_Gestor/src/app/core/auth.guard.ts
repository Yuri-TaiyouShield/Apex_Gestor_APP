import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from './auth.service';
import { ApexRole } from './models';
import { SessionService } from './session.service';

export const requireAuthGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(['/login'], {
    queryParams: {
      redirect: state.url
    }
  });
};

export const roleGuard: CanActivateFn = (route, state) => {
  const session = inject(SessionService);
  const router = inject(Router);
  const requiredRoles = route.data?.['roles'] as ApexRole[] | undefined;

  if (session.hasAnyRole(requiredRoles)) {
    return true;
  }

  return router.createUrlTree(['/login'], {
    queryParams: {
      redirect: state.url,
      reason: 'forbidden'
    }
  });
};
