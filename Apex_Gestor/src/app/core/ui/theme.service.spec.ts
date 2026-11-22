import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';

import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  const originalMatchMedia = window.matchMedia;

  function mockMatchMedia(matchesDark: boolean): {
    dispatch: (nextDark: boolean) => void;
    removeListenerSpy: jasmine.Spy;
  } {
    const listeners = new Set<(event: MediaQueryListEvent) => void>();
    let currentMatches = matchesDark;
    const removeListenerSpy = jasmine.createSpy('removeEventListener').and.callFake(
      (_type: string, cb: (event: MediaQueryListEvent) => void) => {
        listeners.delete(cb);
      }
    );

    spyOn(window, 'matchMedia').and.callFake((query: string) => ({
      matches: currentMatches,
      media: query,
      onchange: null,
      addEventListener: (_type: string, cb: (event: MediaQueryListEvent) => void) => listeners.add(cb),
      removeEventListener: removeListenerSpy,
      addListener: () => undefined,
      removeListener: () => undefined,
      dispatchEvent: () => true
    }) as unknown as MediaQueryList);

    return {
      removeListenerSpy,
      dispatch: (nextDark: boolean) => {
        currentMatches = nextDark;
        listeners.forEach((cb) => cb({ matches: nextDark } as MediaQueryListEvent));
      }
    };
  }

  function ensureThemeMeta(doc: Document): HTMLMetaElement {
    let meta = doc.querySelector('meta[name="theme-color"]') as HTMLMetaElement | null;
    if (!meta) {
      meta = doc.createElement('meta');
      meta.name = 'theme-color';
      meta.content = '#2563eb';
      doc.head.appendChild(meta);
    }
    return meta;
  }

  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    document.documentElement.removeAttribute('data-density');
    document.documentElement.classList.remove('ion-palette-dark');
    window.matchMedia = originalMatchMedia;
  });

  it('inicia em modo system e reage à preferência do sistema operacional', () => {
    const media = mockMatchMedia(false);
    const doc = TestBed.inject(DOCUMENT);
    const meta = ensureThemeMeta(doc);
    const service = TestBed.inject(ThemeService);

    TestBed.flushEffects();

    expect(service.preference()).toBe('system');
    expect(service.resolvedTheme()).toBe('premium-light');
    expect(service.isDark()).toBeFalse();
    expect(doc.documentElement.getAttribute('data-theme')).toBe('premium-light');
    expect(doc.documentElement.classList.contains('ion-palette-dark')).toBeFalse();
    expect(meta.getAttribute('content')).toBe('#2563eb');

    media.dispatch(true);
    TestBed.flushEffects();

    expect(service.resolvedTheme()).toBe('dark-executive');
    expect(service.isDark()).toBeTrue();
    expect(doc.documentElement.getAttribute('data-theme')).toBe('dark-executive');
    expect(doc.documentElement.classList.contains('ion-palette-dark')).toBeTrue();
    expect(meta.getAttribute('content')).toBe('#070b14');
  });

  it('persiste tema dark/light e alterna via toggleLightDark()', () => {
    mockMatchMedia(false);
    const doc = TestBed.inject(DOCUMENT);
    const service = TestBed.inject(ThemeService);

    service.setTheme('dark');
    TestBed.flushEffects();

    expect(localStorage.getItem('apex.pref.theme')).toBe('dark');
    expect(doc.documentElement.getAttribute('data-theme')).toBe('dark-executive');
    expect(doc.documentElement.classList.contains('ion-palette-dark')).toBeTrue();

    service.toggleLightDark();
    TestBed.flushEffects();

    expect(service.preference()).toBe('light');
    expect(localStorage.getItem('apex.pref.theme')).toBe('light');
    expect(doc.documentElement.getAttribute('data-theme')).toBe('premium-light');
    expect(doc.documentElement.classList.contains('ion-palette-dark')).toBeFalse();
  });

  it('migra preferência legada dense para tema light e densidade compact', () => {
    mockMatchMedia(true);
    localStorage.setItem('apex.pref.theme', 'dense');
    const doc = TestBed.inject(DOCUMENT);

    const service = TestBed.inject(ThemeService);
    TestBed.flushEffects();

    expect(service.preference()).toBe('light');
    expect(service.density()).toBe('compact');
    expect(localStorage.getItem('apex.pref.theme')).toBe('light');
    expect(localStorage.getItem('apex.pref.density')).toBe('compact');
    expect(doc.documentElement.getAttribute('data-theme')).toBe('premium-light');
    expect(doc.documentElement.getAttribute('data-density')).toBe('compact');
  });

  it('atualiza e persiste densidade compact/comfortable no documento', () => {
    mockMatchMedia(false);
    const doc = TestBed.inject(DOCUMENT);
    const service = TestBed.inject(ThemeService);

    service.setDensity('comfortable');
    TestBed.flushEffects();

    expect(service.density()).toBe('comfortable');
    expect(localStorage.getItem('apex.pref.density')).toBe('comfortable');
    expect(doc.documentElement.getAttribute('data-density')).toBe('comfortable');
  });
});
