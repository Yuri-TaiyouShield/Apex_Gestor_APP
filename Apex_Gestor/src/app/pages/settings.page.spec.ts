import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { addIcons } from 'ionicons';
import { logOutOutline } from 'ionicons/icons';
import { of } from 'rxjs';

import { ApiConfigService } from '../core/api-config.service';
import { AuthService } from '../core/auth.service';
import { LicenseService } from '../core/license.service';
import { SessionService } from '../core/session.service';
import { ThemeService } from '../core/ui/theme.service';
import { ToastService } from '../core/ui/toast.service';
import { SettingsPage } from './settings.page';

describe('SettingsPage', () => {
  let fixture: ComponentFixture<SettingsPage>;
  let themeService: ThemeService;
  let toastService: ToastService;

  beforeAll(() => {
    addIcons({ 'log-out-outline': logOutOutline });
  });

  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [SettingsPage],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { data: of({ mode: 'personal' }) }
        },
        {
          provide: Router,
          useValue: { navigate: jasmine.createSpy('navigate').and.resolveTo(true) }
        },
        {
          provide: ApiConfigService,
          useValue: {
            baseUrl: jasmine.createSpy('baseUrl').and.returnValue('http://localhost:8080/api'),
            setBaseUrl: jasmine.createSpy('setBaseUrl'),
            reset: jasmine.createSpy('reset')
          }
        },
        {
          provide: LicenseService,
          useValue: {
            readCachedStatus: () => null,
            readLicenseKey: () => Promise.resolve('DEMO-KEY'),
            deviceInfo: () => Promise.resolve({
              appId: 'apex-gestor-web',
              deviceLabel: 'Chrome Desktop',
              deviceFingerprint: 'abcdef1234567890'
            }),
            validate: () => of({ valid: true, status: 'VALID', message: 'ok' })
          }
        },
        {
          provide: SessionService,
          useValue: {
            roleLabel: () => 'Gerente',
            hasAnyRole: () => false
          }
        },
        {
          provide: AuthService,
          useValue: {
            user: () => ({ login: 'v5_gerente', nome: 'Gerente HML' }),
            logout: jasmine.createSpy('logout')
          }
        }
      ]
    }).compileComponents();

    themeService = TestBed.inject(ThemeService);
    toastService = TestBed.inject(ToastService);
    fixture = TestBed.createComponent(SettingsPage);
    fixture.detectChanges();
  });

  afterEach(() => {
    toastService.clear();
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    document.documentElement.removeAttribute('data-density');
    document.documentElement.classList.remove('ion-palette-dark');
  });

  it('mantém preferências sincronizadas com alternância global no header e salva com toast', () => {
    themeService.setTheme('light');
    TestBed.flushEffects();
    fixture.detectChanges();

    themeService.toggleLightDark();
    TestBed.flushEffects();
    fixture.detectChanges();

    expect(fixture.componentInstance.theme).toBe('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark-executive');

    fixture.componentInstance.onDensityChange('comfortable');
    fixture.componentInstance.savePreferences();
    TestBed.flushEffects();
    fixture.detectChanges();

    expect(fixture.componentInstance.theme).toBe('dark');
    expect(themeService.preference()).toBe('dark');
    expect(themeService.density()).toBe('comfortable');
    expect(toastService.items().length).toBe(1);
    expect(toastService.items()[0].tone).toBe('success');
  });
});
