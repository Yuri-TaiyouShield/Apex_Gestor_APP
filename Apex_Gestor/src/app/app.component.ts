import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map, startWith } from 'rxjs';
import { IonicModule, MenuController } from '@ionic/angular';

import { DEFAULT_HOME_ROUTE } from './core/app-variant';
import { SessionService } from './core/session.service';

interface NavItem {
  label: string;
  path: string;
  icon: string;
  audience: 'cliente' | 'staff' | 'both';
}

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

  readonly title = computed(() => (this.isCommerce() ? 'Apex Store' : 'Apex Gestor'));

  readonly navItems: NavItem[] = [
    { label: 'Loja Online', path: '/store', icon: 'storefront-outline', audience: 'both' },
    { label: 'Carrinho', path: '/cart', icon: 'cart-outline', audience: 'cliente' },
    { label: 'Dashboard', path: '/', icon: 'bar-chart-outline', audience: 'staff' },
    { label: 'PDV / Caixa', path: '/pos', icon: 'cash-outline', audience: 'staff' },
    { label: 'Produtos', path: '/products', icon: 'cube-outline', audience: 'staff' },
    { label: 'Clientes CRM', path: '/clients', icon: 'people-outline', audience: 'staff' },
    { label: 'Fornecedores', path: '/suppliers', icon: 'business-outline', audience: 'staff' },
    { label: 'Funcionários', path: '/users', icon: 'person-circle-outline', audience: 'staff' },
    { label: 'Entrada XML NF', path: '/invoice-entry', icon: 'document-text-outline', audience: 'staff' },
    { label: 'Despesas', path: '/expenses', icon: 'receipt-outline', audience: 'staff' },
    { label: 'Tipos de Despesa', path: '/expense-types', icon: 'pricetags-outline', audience: 'staff' },
    { label: 'Privacidade LGPD', path: '/privacy', icon: 'shield-checkmark-outline', audience: 'both' },
    { label: 'Configurações', path: '/settings', icon: 'settings-outline', audience: 'staff' }
  ];

  readonly visibleNavItems = computed(() => {
    const persona = this.session.persona();
    return this.navItems.filter((item) => item.audience === 'both' || item.audience === persona || persona !== 'cliente');
  });

  ngOnInit(): void {
    if (DEFAULT_HOME_ROUTE !== '/' && this.router.url === '/') {
      this.router.navigateByUrl(DEFAULT_HOME_ROUTE, { replaceUrl: true });
    }
  }

  async closeMenu(): Promise<void> {
    await this.menu.close('main-menu');
  }
}
