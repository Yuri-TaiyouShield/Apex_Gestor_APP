import { EnvironmentInjector, runInInjectionContext } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';

import { AuthService } from './auth.service';
import { requireAuthGuard } from './auth.guard';

describe('requireAuthGuard', () => {
  it('requires a new login for a protected route after session cleanup', () => {
    const expected = {} as UrlTree;
    const router = jasmine.createSpyObj<Router>('Router', ['createUrlTree']);
    router.createUrlTree.and.returnValue(expected);
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { isAuthenticated: () => false } },
        { provide: Router, useValue: router }
      ]
    });

    const result = runInInjectionContext(TestBed.inject(EnvironmentInjector), () =>
      requireAuthGuard({} as ActivatedRouteSnapshot, { url: '/cashier' } as RouterStateSnapshot)
    );

    expect(result).toBe(expected);
    expect(router.createUrlTree).toHaveBeenCalledWith(['/login'], {
      queryParams: { redirect: '/cashier' }
    });
  });
});
