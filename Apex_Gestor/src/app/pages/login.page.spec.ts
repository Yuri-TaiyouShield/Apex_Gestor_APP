import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from '../core/auth.service';
import { AuthResponse, Persona } from '../core/models';
import { SessionService } from '../core/session.service';
import { LoginPage } from './login.page';

function buildAuthResponse(roles: string[], overrides: Partial<AuthResponse> = {}): AuthResponse {
  return {
    accessToken: 'access-token',
    refreshToken: 'refresh-token',
    expiresAt: String(Date.now() + 60_000),
    tokenType: 'Bearer',
    usuarioId: 10,
    nome: 'Operador Teste',
    login: 'v5_gerente',
    roles,
    ...overrides
  };
}

describe('LoginPage', () => {
  let currentPersona: Persona;
  let queryParams: Record<string, string>;
  let auth: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;
  let session: { persona: jasmine.Spy<() => Persona>; normalizeRole: jasmine.Spy<(role: string) => string> };

  function createComponent(options: {
    persona?: Persona;
    query?: Record<string, string>;
    demoMode?: boolean;
  } = {}): { fixture: ComponentFixture<LoginPage>; component: LoginPage } {
    currentPersona = options.persona ?? 'gerente';
    queryParams = options.query ?? {};

    TestBed.resetTestingModule();
    auth = jasmine.createSpyObj<AuthService>('AuthService', [
      'login',
      'logout',
      'registerConsent',
      'enterDemoSession'
    ]);
    auth.registerConsent.and.returnValue(of({}));
    auth.login.and.returnValue(of(buildAuthResponse(['ROLE_GERENTE'])));
    auth.enterDemoSession.and.returnValue(true);

    router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);
    router.navigateByUrl.and.resolveTo(true);

    session = {
      persona: jasmine.createSpy('persona').and.callFake(() => currentPersona),
      normalizeRole: jasmine.createSpy('normalizeRole').and.callFake((role: string) => (
        role.startsWith('ROLE_') ? role : `ROLE_${role}`
      ))
    };

    TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router },
        { provide: SessionService, useValue: session },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap(queryParams)
            }
          }
        }
      ]
    });

    const fixture = TestBed.createComponent(LoginPage);
    const component = fixture.componentInstance;
    if (options.demoMode !== undefined) {
      (component as unknown as { demoMode: boolean }).demoMode = options.demoMode;
    }
    fixture.detectChanges();
    return { fixture, component };
  }

  it('expõe os 5 presets de staff e não inclui Cliente/v5_cliente nos presets nem no modo demo', () => {
    const { fixture, component } = createComponent({ demoMode: true });

    expect(component.loginPresets).toEqual([
      { label: 'Dono/Gerente', login: 'v5_dono' },
      { label: 'Sysadmin', login: 'v5_sysadmin' },
      { label: 'Gerente', login: 'v5_gerente' },
      { label: 'Caixa', login: 'v5_caixa' },
      { label: 'Vendedor', login: 'v5_vendedor' }
    ]);
    expect(component.loginPresets.some((p) => p.login === 'v5_cliente' || p.label === 'Cliente')).toBeFalse();

    const host = fixture.nativeElement as HTMLElement;
    const presetLabels = Array.from(host.querySelectorAll('.preset-grid button')).map((b) => b.textContent?.trim());
    expect(presetLabels).toEqual(['Dono/Gerente', 'Sysadmin', 'Gerente', 'Caixa', 'Vendedor']);
    expect(presetLabels).not.toContain('Cliente');

    const demoButtons = Array.from(host.querySelectorAll('.demo-details .segmented-control button')).map((b) => b.textContent?.trim());
    expect(demoButtons).toEqual(['Caixa', 'Vendedor', 'Gerente', 'Admin']);
    expect(demoButtons).not.toContain('Cliente');
  });

  it('inicializa demoPersona a partir da sessão quando válida ou cai em gerente para cliente', () => {
    expect(createComponent({ persona: 'cliente' }).component.demoPersona).toBe('gerente');
    expect(createComponent({ persona: 'vendedor' }).component.demoPersona).toBe('vendedor');
    expect(createComponent({ persona: 'gerente' }).component.demoPersona).toBe('gerente');
    expect(createComponent({ persona: 'admin' }).component.demoPersona).toBe('admin');
  });

  it('preenche credenciais ao usar preset apenas quando demoMode está ativo', () => {
    const prod = createComponent({ demoMode: false }).component;
    prod.usePreset('v5_caixa');
    expect(prod.login).toBe('');

    const demo = createComponent({ demoMode: true }).component;
    demo.usePreset('v5_caixa');
    expect(demo.login).toBe('v5_caixa');
    expect(demo.acceptedPrivacyTerms).toBeTrue();
    expect(demo.message()).toContain('v5_caixa');
  });

  it('autentica modo demo para caixa, vendedor, gerente e admin e navega ao destino', () => {
    const cases: Array<{ persona: 'caixa' | 'vendedor' | 'gerente' | 'admin'; roles: string[] }> = [
      { persona: 'caixa', roles: ['ROLE_CAIXA'] },
      { persona: 'vendedor', roles: ['ROLE_VENDEDOR'] },
      { persona: 'gerente', roles: ['ROLE_GERENTE'] },
      { persona: 'admin', roles: ['ROLE_ADMIN'] }
    ];

    for (const item of cases) {
      const { component } = createComponent({ demoMode: true, persona: 'cliente' });
      component.demoPersona = item.persona;

      component.enterDemo();

      expect(auth.enterDemoSession).toHaveBeenCalledWith(item.roles);
      expect(router.navigateByUrl).toHaveBeenCalledWith('/');
    }
  });

  it('não navega no modo demo se enterDemoSession retornar false ou demoMode for falso', () => {
    const prod = createComponent({ demoMode: false }).component;
    prod.enterDemo();
    expect(auth.enterDemoSession).not.toHaveBeenCalled();
    expect(router.navigateByUrl).not.toHaveBeenCalled();

    const blocked = createComponent({ demoMode: true }).component;
    auth.enterDemoSession.and.returnValue(false);
    blocked.enterDemo();
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('bloqueia login bem-sucedido quando resposta possui apenas ROLE_CLIENTE_B2C', () => {
    const { component } = createComponent({ persona: 'cliente', query: { redirect: '/cashier' } });
    auth.login.and.returnValue(of(buildAuthResponse(['ROLE_CLIENTE_B2C'], { login: 'v5_cliente' })));
    component.login = 'v5_cliente';
    component.senha = 'Apex@2026';
    component.acceptedPrivacyTerms = true;

    component.enter();

    expect(auth.logout).toHaveBeenCalledWith('forbidden');
    expect(component.messageColor()).toBe('danger');
    expect(component.message()).toBe('Acesso de login para perfil cliente foi desativado.');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('permite login de staff e redireciona para redirect ou raiz sem usar /store', () => {
    const defaultRedirect = createComponent({ persona: 'cliente' }).component;
    auth.login.and.returnValue(of(buildAuthResponse(['ROLE_CAIXA'], { login: 'v5_caixa' })));
    defaultRedirect.login = ' v5_caixa ';
    defaultRedirect.senha = 'Apex@2026';
    defaultRedirect.acceptedPrivacyTerms = true;

    defaultRedirect.enter();

    expect(auth.login).toHaveBeenCalledWith(jasmine.objectContaining({ login: 'v5_caixa' }));
    expect(defaultRedirect.messageColor()).toBe('primary');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/');

    const customRedirect = createComponent({ query: { redirect: '/pos' } }).component;
    auth.login.and.returnValue(of(buildAuthResponse(['ROLE_VENDEDOR'], { login: 'v5_vendedor' })));
    customRedirect.login = 'v5_vendedor';
    customRedirect.senha = 'Apex@2026';
    customRedirect.acceptedPrivacyTerms = true;

    customRedirect.enter();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/pos');
  });

  it('exibe mensagens amigáveis para erros 401, 403, 402 e 0', () => {
    const cases = [
      { status: 401, expected: 'Login ou senha inválidos.' },
      { status: 403, expected: 'Acesso negado para este perfil.' },
      { status: 402, expected: 'APEX-DEMO-2026' },
      { status: 0, expected: 'http://localhost:8080/actuator/health' }
    ];

    for (const item of cases) {
      const { component } = createComponent({ demoMode: false });
      auth.login.and.returnValue(throwError(() => new HttpErrorResponse({ status: item.status })));
      component.login = 'v5_gerente';
      component.senha = 'wrong';
      component.acceptedPrivacyTerms = true;

      component.enter();

      expect(component.messageColor()).toBe('danger');
      expect(component.message()).toContain(item.expected);
      if (item.status === 402) {
        expect(component.message()).not.toContain('APEX-DEMO-ALL');
      }
      expect(router.navigateByUrl).not.toHaveBeenCalled();
    }
  });
});
