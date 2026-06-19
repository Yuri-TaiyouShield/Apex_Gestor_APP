import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { NavigationEnd, Router } from '@angular/router';
import { Subject } from 'rxjs';

import { AuthService } from './auth.service';
import { SessionTimeoutService } from './session-timeout.service';

describe('SessionTimeoutService', () => {
  const events = new Subject<NavigationEnd>();
  let auth: { isAuthenticated: jasmine.Spy; logout: jasmine.Spy };
  let router: { events: Subject<NavigationEnd>; navigate: jasmine.Spy };

  beforeEach(() => {
    localStorage.setItem('apex.session.timeoutMinutes', '5');
    auth = {
      isAuthenticated: jasmine.createSpy('isAuthenticated').and.returnValue(true),
      logout: jasmine.createSpy('logout')
    };
    router = {
      events,
      navigate: jasmine.createSpy('navigate').and.resolveTo(true)
    };
    spyOn(window, 'confirm').and.returnValue(false);

    TestBed.configureTestingModule({
      providers: [
        SessionTimeoutService,
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router }
      ]
    });
  });

  afterEach(() => localStorage.removeItem('apex.session.timeoutMinutes'));

  it('logs out after inactivity and redirects with Angular Router inside the current host', fakeAsync(() => {
    const originalOrigin = window.location.origin;
    TestBed.inject(SessionTimeoutService).start();

    tick(5 * 60_000);

    expect(auth.logout).toHaveBeenCalledWith('idle');
    expect(router.navigate).toHaveBeenCalledWith(['/login'], {
      replaceUrl: true,
      queryParams: { reason: 'inactivity' }
    });
    expect(window.location.origin).toBe(originalOrigin);
  }));

  it('treats internal navigation as activity and postpones logout', fakeAsync(() => {
    TestBed.inject(SessionTimeoutService).start();
    tick(4 * 60_000 - 1);
    events.next(new NavigationEnd(1, '/pos', '/pos'));
    tick(60_001);

    expect(auth.logout).not.toHaveBeenCalled();
  }));
});
