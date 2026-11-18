import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { DestroyRef, Injectable, PLATFORM_ID, computed, effect, inject, signal } from '@angular/core';

export type ApexThemePreference = 'system' | 'light' | 'dark';
export type ApexResolvedTheme = 'premium-light' | 'dark-executive';
export type ApexDensityPreference = 'compact' | 'comfortable';

const THEME_STORAGE_KEY = 'apex.pref.theme';
const DENSITY_STORAGE_KEY = 'apex.pref.density';
const LIGHT_THEME_COLOR = '#2563eb';
const DARK_THEME_COLOR = '#070b14';

/**
 * Gerencia o tema global (Premium Gradient Light / Dark Executive) e a densidade
 * de interface com persistencia local e suporte a preferencia do sistema operacional.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly destroyRef = inject(DestroyRef);
  private readonly isBrowser = isPlatformBrowser(this.platformId);

  private readonly systemPrefersDark = signal(this.readSystemDarkPreference());
  readonly preference = signal<ApexThemePreference>(this.readInitialTheme());
  readonly density = signal<ApexDensityPreference>(this.readInitialDensity());

  readonly resolvedTheme = computed<ApexResolvedTheme>(() => {
    const pref = this.preference();
    if (pref === 'dark') {
      return 'dark-executive';
    }
    if (pref === 'light') {
      return 'premium-light';
    }
    return this.systemPrefersDark() ? 'dark-executive' : 'premium-light';
  });

  readonly isDark = computed(() => this.resolvedTheme() === 'dark-executive');

  constructor() {
    this.listenSystemColorScheme();
    effect(() => {
      this.applyToDocument(this.resolvedTheme(), this.density());
    });
  }

  setTheme(preference: ApexThemePreference): void {
    this.preference.set(preference);
    this.writeStorage(THEME_STORAGE_KEY, preference);
  }

  setDensity(density: ApexDensityPreference): void {
    this.density.set(density);
    this.writeStorage(DENSITY_STORAGE_KEY, density);
  }

  toggleLightDark(): void {
    this.setTheme(this.isDark() ? 'light' : 'dark');
  }

  private applyToDocument(resolved: ApexResolvedTheme, density: ApexDensityPreference): void {
    const root = this.document?.documentElement;
    if (!root) {
      return;
    }
    const dark = resolved === 'dark-executive';
    root.setAttribute('data-theme', resolved);
    root.setAttribute('data-density', density);
    root.classList.toggle('ion-palette-dark', dark);

    const themeMeta = this.document.querySelector('meta[name="theme-color"]');
    if (themeMeta) {
      themeMeta.setAttribute('content', dark ? DARK_THEME_COLOR : LIGHT_THEME_COLOR);
    }
  }

  private listenSystemColorScheme(): void {
    if (!this.isBrowser || typeof window.matchMedia !== 'function') {
      return;
    }
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    this.systemPrefersDark.set(mediaQuery.matches);
    const onChange = (event: MediaQueryListEvent): void => {
      this.systemPrefersDark.set(event.matches);
    };
    mediaQuery.addEventListener('change', onChange);
    this.destroyRef.onDestroy(() => mediaQuery.removeEventListener('change', onChange));
  }

  private readSystemDarkPreference(): boolean {
    if (!this.isBrowser || typeof window.matchMedia !== 'function') {
      return false;
    }
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
  }

  private readInitialTheme(): ApexThemePreference {
    const stored = this.readStorage(THEME_STORAGE_KEY);
    if (stored === 'light' || stored === 'dark' || stored === 'system') {
      return stored;
    }
    // Compatibilidade com preferencia legada ('dense' -> claro compacto).
    if (stored === 'dense') {
      this.writeStorage(DENSITY_STORAGE_KEY, 'compact');
      this.writeStorage(THEME_STORAGE_KEY, 'light');
      return 'light';
    }
    return 'system';
  }

  private readInitialDensity(): ApexDensityPreference {
    const stored = this.readStorage(DENSITY_STORAGE_KEY);
    return stored === 'comfortable' ? 'comfortable' : 'compact';
  }

  private readStorage(key: string): string | null {
    if (!this.isBrowser) {
      return null;
    }
    try {
      return globalThis.localStorage?.getItem(key) ?? null;
    } catch {
      return null;
    }
  }

  private writeStorage(key: string, value: string): void {
    if (!this.isBrowser) {
      return;
    }
    try {
      globalThis.localStorage?.setItem(key, value);
    } catch {
      // Storage indisponivel (ex.: contexto restrito); mantem estado em memoria.
    }
  }
}
