import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    title: 'Dashboard',
    loadComponent: () => import('./pages/dashboard.page').then((m) => m.DashboardPage)
  },
  {
    path: 'store',
    title: 'Loja online',
    loadComponent: () => import('./pages/storefront.page').then((m) => m.StorefrontPage)
  },
  {
    path: 'cart',
    title: 'Carrinho',
    loadComponent: () => import('./pages/cart.page').then((m) => m.CartPage)
  },
  {
    path: 'checkout',
    title: 'Checkout',
    loadComponent: () => import('./pages/checkout.page').then((m) => m.CheckoutPage)
  },
  {
    path: 'login',
    title: 'Acesso',
    loadComponent: () => import('./pages/login.page').then((m) => m.LoginPage)
  },
  {
    path: 'products',
    title: 'Produtos',
    data: { entity: 'products' },
    loadComponent: () => import('./pages/entity-list.page').then((m) => m.EntityListPage)
  },
  {
    path: 'clients',
    title: 'Clientes',
    data: { entity: 'clients' },
    loadComponent: () => import('./pages/entity-list.page').then((m) => m.EntityListPage)
  },
  {
    path: 'suppliers',
    title: 'Fornecedores',
    data: { entity: 'suppliers' },
    loadComponent: () => import('./pages/entity-list.page').then((m) => m.EntityListPage)
  },
  {
    path: 'users',
    title: 'Funcionários',
    data: { entity: 'users' },
    loadComponent: () => import('./pages/entity-list.page').then((m) => m.EntityListPage)
  },
  {
    path: 'pos',
    title: 'PDV',
    loadComponent: () => import('./pages/point-of-sale.page').then((m) => m.PointOfSalePage)
  },
  {
    path: 'invoice-entry',
    title: 'Entrada de NF',
    loadComponent: () => import('./pages/invoice-entry.page').then((m) => m.InvoiceEntryPage)
  },
  {
    path: 'expenses',
    title: 'Despesas',
    data: { entity: 'expenses' },
    loadComponent: () => import('./pages/entity-list.page').then((m) => m.EntityListPage)
  },
  {
    path: 'finance',
    title: 'Financeiro inteligente',
    loadComponent: () => import('./pages/finance-compliance.page').then((m) => m.FinanceCompliancePage)
  },
  {
    path: 'expense-types',
    title: 'Tipos de Despesa',
    data: { entity: 'expenseTypes' },
    loadComponent: () => import('./pages/entity-list.page').then((m) => m.EntityListPage)
  },
  {
    path: 'settings',
    title: 'Configurações',
    loadComponent: () => import('./pages/settings.page').then((m) => m.SettingsPage)
  },
  {
    path: 'privacy',
    title: 'Privacidade',
    loadComponent: () => import('./pages/privacy.page').then((m) => m.PrivacyPage)
  },
  {
    path: '**',
    redirectTo: ''
  }
];
