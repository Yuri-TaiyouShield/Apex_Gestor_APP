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

  it('propõe continuidade 4 minutos antes do encerramento e reinicia o ciclo', fakeAsync(() => {
    (window.confirm as jasmine.Spy).and.returnValue(true);
    TestBed.inject(SessionTimeoutService).start();

    tick(4 * 60_000);

    expect(window.confirm).toHaveBeenCalledWith('Sua sessão será encerrada por inatividade. Deseja continuar conectado?');
    expect(auth.logout).not.toHaveBeenCalled();

    tick(60_000);

    // ciclo reiniciado: encerramento original no quinto minuto foi descartado
    expect(auth.logout).not.toHaveBeenCalled();
  }));

  it('não agenda encerramento enquanto não houver sessão autenticada', fakeAsync(() => {
    auth.isAuthenticated.and.returnValue(false);

    TestBed.inject(SessionTimeoutService).start();
    tick(10 * 60_000);

    expect(auth.logout).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  }));

  it('interrompe o encerramento silenciosamente quando a sessão já caiu', fakeAsync(() => {
    TestBed.inject(SessionTimeoutService).start();
    auth.isAuthenticated.and.returnValue(false);

    tick(5 * 60_000);

    expect(auth.logout).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  }));

  it('usa o prazo padrão de 30 minutos quando a configuração é inválida', fakeAsync(() => {
    localStorage.setItem('apex.session.timeoutMinutes', 'banana');

    TestBed.inject(SessionTimeoutService).start();

    tick(30 * 60_000 - 1);
    expect(auth.logout).not.toHaveBeenCalled();

    tick(1);
    expect(auth.logout).toHaveBeenCalledWith('idle');
  }));

  it('alerta imediato quando o prazo configurado é menor que a antecedência do aviso', fakeAsync(() => {
    localStorage.setItem('apex.session.timeoutMinutes', '0.5');

    TestBed.inject(SessionTimeoutService).start();

    tick();
    expect(window.confirm).toHaveBeenCalled();
    expect(auth.logout).not.toHaveBeenCalled();

    tick(30_000);
    expect(auth.logout).toHaveBeenCalledWith('idle');
  }));

  it('registra ouvintes e agendamento apenas na primeira ativação', fakeAsync(() => {
    const service = TestBed.inject(SessionTimeoutService);
    service.start();
    service.start();

    tick(5 * 60_000);

    expect(auth.logout).toHaveBeenCalledTimes(1);
  }));
});
