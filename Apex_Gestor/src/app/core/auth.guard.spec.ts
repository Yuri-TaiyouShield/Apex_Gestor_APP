import { EnvironmentInjector, runInInjectionContext } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot, UrlTree } from '@angular/router';

import { AuthService } from './auth.service';
import { requireAuthGuard, roleGuard } from './auth.guard';
import { SessionService } from './session.service';

type GuardOutcome = ReturnType<CanActivateFn>;

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

  it('allows an authenticated user through a protected route', () => {
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { isAuthenticated: () => true } },
        { provide: Router, useValue: jasmine.createSpyObj<Router>('Router', ['createUrlTree']) }
      ]
    });

    const result = runInInjectionContext(TestBed.inject(EnvironmentInjector), () =>
      requireAuthGuard({} as ActivatedRouteSnapshot, { url: '/cashier' } as RouterStateSnapshot)
    );

    expect(result).toBeTrue();
  });
});

describe('roleGuard', () => {
  function configure(options: { authenticated: boolean; hasAnyRole?: (roles: readonly string[] | undefined) => boolean }): { router: jasmine.SpyObj<Router> } {
    const router = jasmine.createSpyObj<Router>('Router', ['createUrlTree']);
    router.createUrlTree.and.returnValue({} as UrlTree);
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { isAuthenticated: () => options.authenticated } },
        { provide: SessionService, useValue: { hasAnyRole: options.hasAnyRole ?? ((roles: readonly string[] | undefined) => !roles || roles.length === 0) } },
        { provide: Router, useValue: router }
      ]
    });
    return { router };
  }

  function run(routes: Record<string, unknown>, url: string): GuardOutcome {
    const route = { data: routes } as ActivatedRouteSnapshot;
    const state = { url } as RouterStateSnapshot;
    return runInInjectionContext(TestBed.inject(EnvironmentInjector), () => roleGuard(route, state));
  }

  it('redirects unauthenticated users to login keeping the target route', () => {
    const { router } = configure({ authenticated: false });

    const result = run({ roles: ['ROLE_CAIXA'] }, '/cashier');

    expect(result).toEqual({} as UrlTree);
    expect(router.createUrlTree).toHaveBeenCalledWith(['/login'], {
      queryParams: { redirect: '/cashier' }
    });
  });

  it('rejects authenticated users without the required role and flags the reason', () => {
    const { router } = configure({ authenticated: true });

    const result = run({ roles: ['ROLE_CAIXA'] }, '/cashier');

    expect(result).toEqual({} as UrlTree);
    expect(router.createUrlTree).toHaveBeenCalledWith(['/login'], {
      queryParams: { redirect: '/cashier', reason: 'forbidden' }
    });
  });

  it('allows authenticated users holding one of the required roles', () => {
    configure({ authenticated: true, hasAnyRole: (roles) => roles?.includes('ROLE_CAIXA') === true });

    expect(run({ roles: ['ROLE_CAIXA', 'ROLE_GERENTE'] }, '/cashier')).toBeTrue();
  });

  it('treats routes without role metadata as open for authenticated users', () => {
    configure({ authenticated: true });

    expect(run({}, '/store')).toBeTrue();
  });
});
