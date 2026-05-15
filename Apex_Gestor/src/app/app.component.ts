import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map, startWith } from 'rxjs';
import { IonicModule, MenuController } from '@ionic/angular';

import { DEFAULT_HOME_ROUTE } from './core/app-variant';
import { APEX_NAV_ITEMS } from './core/feature-menu.config';
import { SessionService } from './core/session.service';
import { TenantFeatureService } from './core/tenant-feature.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, IonicModule, RouterLink, RouterLinkActive],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly menu = inject(MenuController);
  readonly session = inject(SessionService);
  readonly tenantFeatures = inject(TenantFeatureService);

  private readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map((event) => event.urlAfterRedirects),
      startWith(this.router.url)
    ),
    { initialValue: '/' }
  );

  readonly isCommerce = computed(() => ['/store', '/cart', '/checkout'].some((path) => this.currentUrl().startsWith(path)));

  readonly platformLabel = computed(() => {
    const protocol = globalThis.location?.protocol ?? 'http:';
    if (protocol === 'app:') {
      return 'Desktop';
    }
    if (protocol === 'capacitor:' || protocol === 'ionic:') {
      return 'Mobile';
    }
    return 'Web';
  });

  readonly title = computed(() => {
    if (this.isCommerce() && this.tenantFeatures.isWhiteLabelB2C()) {
      return this.tenantFeatures.context().branding?.storefrontName ?? this.tenantFeatures.context().tenantName;
    }
    return this.isCommerce() ? 'Apex Store' : 'Apex Gestor';
  });

  readonly visibleNavItems = computed(() => {
    const persona = this.session.persona();
    return APEX_NAV_ITEMS.filter((item) => {
      const audienceAllowed = item.audience === 'both' || item.audience === persona || persona !== 'cliente';
      const roleAllowed = this.session.hasAnyRole(item.allowedRoles);
      return audienceAllowed && roleAllowed && this.tenantFeatures.hasAll(item.requiredFeatures);
    });
  });

  ngOnInit(): void {
    this.tenantFeatures.refreshFromCache();
    if (DEFAULT_HOME_ROUTE !== '/' && this.router.url === '/') {
      this.router.navigateByUrl(DEFAULT_HOME_ROUTE, { replaceUrl: true });
    }
  }

  async closeMenu(): Promise<void> {
    await this.menu.close('main-menu');
  }
}
