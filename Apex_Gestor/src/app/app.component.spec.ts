import { addIcons } from 'ionicons';
import {
  barChartOutline,
  businessOutline,
  cardOutline,
  cartOutline,
  cashOutline,
  cubeOutline,
  documentTextOutline,
  layersOutline,
  moonOutline,
  peopleOutline,
  personCircleOutline,
  receiptOutline,
  settingsOutline,
  shieldCheckmarkOutline,
  storefrontOutline,
  sunnyOutline,
  warningOutline
} from 'ionicons/icons';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { MenuController } from '@ionic/angular/standalone';
import { Subject } from 'rxjs';

import { AppComponent } from './app.component';
import { SessionService } from './core/session.service';
import { SessionTimeoutService } from './core/session-timeout.service';
import { TenantFeatureService } from './core/tenant-feature.service';
import { Persona } from './core/models';

describe('AppComponent shell', () => {
  let events: Subject<NavigationEnd>;
  let whiteLabel: boolean;
  let persona: Persona;
  let session: {
    persona: jasmine.Spy<() => Persona>;
    hasAnyRole: jasmine.Spy<(roles?: readonly string[]) => boolean>;
    roleLabel: jasmine.Spy<() => string>;
    accessIssue: jasmine.Spy<() => string | null>;
    setAccessIssue: jasmine.Spy<(issue: string | null) => void>;
  };
  let tenantFeatures: {
    refreshFromCache: jasmine.Spy;
    isWhiteLabelB2C: jasmine.Spy<() => boolean>;
    context: () => { tenantName: string; subscriptionTier?: string; branding: { storefrontName?: string } | null };
    hasAll: jasmine.Spy<() => boolean>;
  };
  let sessionTimeout: { start: jasmine.Spy };
  let menu: { close: jasmine.Spy };
  let routerMock: { events: Subject<NavigationEnd>; url: string; createUrlTree: jasmine.Spy; serializeUrl: jasmine.Spy; navigateByUrl: jasmine.Spy };
  let fixture: ComponentFixture<AppComponent>;

  beforeAll(() => {
    addIcons({
      'bar-chart-outline': barChartOutline,
      'business-outline': businessOutline,
      'card-outline': cardOutline,
      'cart-outline': cartOutline,
      'cash-outline': cashOutline,
      'cube-outline': cubeOutline,
      'document-text-outline': documentTextOutline,
      'layers-outline': layersOutline,
      'moon-outline': moonOutline,
      'people-outline': peopleOutline,
      'person-circle-outline': personCircleOutline,
      'receipt-outline': receiptOutline,
      'settings-outline': settingsOutline,
      'shield-checkmark-outline': shieldCheckmarkOutline,
      'storefront-outline': storefrontOutline,
      'sunny-outline': sunnyOutline,
      'warning-outline': warningOutline
    });
  });

  beforeEach(() => {
    events = new Subject<NavigationEnd>();
    whiteLabel = false;
    persona = 'gerente';
    routerMock = {
      events,
      url: '/',
      createUrlTree: jasmine.createSpy('createUrlTree').and.returnValue({}),
      serializeUrl: jasmine.createSpy('serializeUrl').and.returnValue('/mock'),
      navigateByUrl: jasmine.createSpy('navigateByUrl').and.resolveTo(true)
    };

    session = {
      persona: jasmine.createSpy('persona').and.callFake(() => persona),
      hasAnyRole: jasmine.createSpy('hasAnyRole').and.returnValue(true),
      roleLabel: jasmine.createSpy('roleLabel').and.returnValue('Gerente'),
      accessIssue: jasmine.createSpy('accessIssue').and.returnValue(null),
      setAccessIssue: jasmine.createSpy('setAccessIssue')
    };
    tenantFeatures = {
      refreshFromCache: jasmine.createSpy('refreshFromCache'),
      isWhiteLabelB2C: jasmine.createSpy('isWhiteLabelB2C').and.callFake(() => whiteLabel),
      context: jasmine.createSpy('context').and.callFake(() => ({
        tenantName: 'HML Retail',
        subscriptionTier: 'Growth',
        branding: whiteLabel ? { storefrontName: 'B2C Storefront' } : null
      })),
      hasAll: jasmine.createSpy('hasAll').and.returnValue(true)
    };
    sessionTimeout = { start: jasmine.createSpy('start') };
    menu = { close: jasmine.createSpy('close').and.resolveTo() };

    TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        { provide: ActivatedRoute, useValue: {} },
        { provide: Router, useValue: routerMock },
        { provide: MenuController, useValue: menu },
        { provide: SessionService, useValue: session },
        { provide: SessionTimeoutService, useValue: sessionTimeout },
        { provide: TenantFeatureService, useValue: tenantFeatures }
      ]
    });
    fixture = TestBed.createComponent(AppComponent);
  });

  afterEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    document.documentElement.removeAttribute('data-density');
    document.documentElement.classList.remove('ion-palette-dark');
  });

  it('exibe Apex Gestor para o quadro de funcionários fora do comércio', () => {
    expect(fixture.componentInstance.title()).toBe('Apex Gestor');
    expect(fixture.componentInstance.isCommerce()).toBeFalse();
  });

  it('troca o título para a loja white-label em rotas de comércio B2C', () => {
    whiteLabel = true;

    events.next(new NavigationEnd(2, '/store', '/store'));

    expect(fixture.componentInstance.isCommerce()).toBeTrue();
    expect(fixture.componentInstance.title()).toBe('B2C Storefront');
  });

  it('usa Apex Store em rotas de comércio sem white-label', () => {
    events.next(new NavigationEnd(3, '/cart', '/cart'));

    expect(fixture.componentInstance.title()).toBe('Apex Store');
  });

  it('declara plataforma web em execução no navegador', () => {
    expect(fixture.componentInstance.platformLabel()).toBe('Web');
  });

  it('declara plataforma Desktop quando a bridge apexDesktop está ativa', () => {
    (window as unknown as { apexDesktop?: { isDesktop: boolean } }).apexDesktop = { isDesktop: true };
    try {
      expect(fixture.componentInstance.platformLabel()).toBe('Desktop');
    } finally {
      delete (window as unknown as { apexDesktop?: unknown }).apexDesktop;
    }
  });

  it('usa tenantName como fallback quando branding não possui storefrontName', () => {
    whiteLabel = true;
    tenantFeatures.context = jasmine.createSpy('context').and.returnValue({
      tenantName: 'Fallback Tenant Name',
      branding: {} as { storefrontName: string }
    });
    events.next(new NavigationEnd(4, '/store', '/store'));

    expect(fixture.componentInstance.isCommerce()).toBeTrue();
    expect(fixture.componentInstance.title()).toBe('Fallback Tenant Name');
  });

  it('libera o PDV para o quadro de funcionários com permissão', () => {
    persona = 'gerente';
    session.hasAnyRole.and.returnValue(true);

    expect(fixture.componentInstance.canOpenPdv()).toBeTrue();
  });

  it('esconde o PDV do cliente B2C mesmo com papéis técnicos válidos', () => {
    persona = 'cliente';
    session.hasAnyRole.and.returnValue(true);

    expect(fixture.componentInstance.canOpenPdv()).toBeFalse();
  });

  it('agrupa itens visíveis em seções com rótulo operacional', () => {
    persona = 'gerente';
    session.hasAnyRole.and.returnValue(true);

    const groups = fixture.componentInstance.visibleNavGroups();

    expect(groups.map((section) => section.label)).toEqual(['Principal', 'Operação', 'Gestão', 'Administração']);
    expect(groups[0]?.items.map((item) => item.path)).toEqual(jasmine.arrayContaining(['/', '/pos', '/cashier']));
  });

  it('remapeia item com papéis proibidos para pessoa sem permissão', () => {
    persona = 'vendedor';
    session.hasAnyRole.and.callFake((roles?: readonly string[]) =>
      !roles || (roles as readonly string[]).includes('ROLE_VENDEDOR'));

    const visible = fixture.componentInstance.visibleNavItems().map((item) => item.path);

    expect(visible).toContain('/pos');
    expect(visible).not.toContain('/cashier');
    expect(visible).not.toContain('/finance-operations');
  });

  it('inicializa o shell recarregando licença e timer de inatividade', () => {
    fixture.componentInstance.ngOnInit();

    expect(tenantFeatures.refreshFromCache).toHaveBeenCalledOnceWith();
    expect(sessionTimeout.start).toHaveBeenCalledOnceWith();
  });

  it('fecha o menu lateral pelo controlador do Ionic', async () => {
    await fixture.componentInstance.closeMenu();

    expect(menu.close).toHaveBeenCalledOnceWith('main-menu');
  });

  it('alterna tema global claro/escuro pelo botão de alternância no shell', () => {
    fixture.detectChanges();
    const initialDark = fixture.componentInstance.theme.isDark();

    fixture.componentInstance.theme.toggleLightDark();
    fixture.detectChanges();

    expect(fixture.componentInstance.theme.isDark()).toBe(!initialDark);
    expect(document.documentElement.getAttribute('data-theme')).toBe(initialDark ? 'premium-light' : 'dark-executive');
  });
});
