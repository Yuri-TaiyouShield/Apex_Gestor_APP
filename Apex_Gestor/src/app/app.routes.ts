import { Routes } from '@angular/router';

import { requireAuthGuard, roleGuard } from './core/auth.guard';

const STAFF_DASHBOARD_ROLES = ['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_FINANCEIRO', 'ROLE_VENDEDOR', 'ROLE_CAIXA', 'ROLE_DESPACHANTE', 'ROLE_ADMIN', 'ROLE_GERENTE', 'ROLE_GESTOR'];
const MANAGER_ROLES = ['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_ADMIN', 'ROLE_GERENTE'];
const FINANCE_ROLES = ['ROLE_DONO_GERENTE', 'ROLE_FINANCEIRO', 'ROLE_AUDITOR', 'ROLE_CONTADOR', 'ROLE_ADVOGADO', 'ROLE_ADMIN', 'ROLE_GERENTE'];

export const routes: Routes = [
  {
    path: '',
    title: 'Dashboard',
    canActivate: [roleGuard],
    data: { roles: STAFF_DASHBOARD_ROLES },
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
    canActivate: [requireAuthGuard],
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
    canActivate: [roleGuard],
    data: { entity: 'products', roles: ['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_FINANCEIRO', 'ROLE_VENDEDOR', 'ROLE_CAIXA', 'ROLE_ADMIN', 'ROLE_GERENTE'] },
    loadComponent: () => import('./pages/entity-list.page').then((m) => m.EntityListPage)
  },
  {
    path: 'clients',
    title: 'Clientes',
    canActivate: [roleGuard],
    data: { entity: 'clients', roles: ['ROLE_DONO_GERENTE', 'ROLE_VENDEDOR', 'ROLE_CAIXA', 'ROLE_ADMIN', 'ROLE_GERENTE'] },
    loadComponent: () => import('./pages/entity-list.page').then((m) => m.EntityListPage)
  },
  {
    path: 'suppliers',
    title: 'Fornecedores',
    canActivate: [roleGuard],
    data: { entity: 'suppliers', roles: ['ROLE_DONO_GERENTE', 'ROLE_FINANCEIRO', 'ROLE_ADMIN', 'ROLE_GERENTE'] },
    loadComponent: () => import('./pages/entity-list.page').then((m) => m.EntityListPage)
  },
  {
    path: 'users',
    title: 'Funcionarios',
    canActivate: [roleGuard],
    data: { entity: 'users', roles: MANAGER_ROLES },
    loadComponent: () => import('./pages/entity-list.page').then((m) => m.EntityListPage)
  },
  {
    path: 'pos',
    title: 'PDV',
    canActivate: [roleGuard],
    data: { roles: ['ROLE_DONO_GERENTE', 'ROLE_VENDEDOR', 'ROLE_CAIXA', 'ROLE_ADMIN', 'ROLE_GERENTE'] },
    loadComponent: () => import('./pages/point-of-sale.page').then((m) => m.PointOfSalePage)
  },
  {
    path: 'invoice-entry',
    title: 'Entrada de NF',
    canActivate: [roleGuard],
    data: { roles: ['ROLE_DONO_GERENTE', 'ROLE_FINANCEIRO', 'ROLE_DESPACHANTE', 'ROLE_ADMIN', 'ROLE_GERENTE'] },
    loadComponent: () => import('./pages/invoice-entry.page').then((m) => m.InvoiceEntryPage)
  },
  {
    path: 'expenses',
    title: 'Despesas',
    canActivate: [roleGuard],
    data: { entity: 'expenses', roles: ['ROLE_DONO_GERENTE', 'ROLE_FINANCEIRO', 'ROLE_ADMIN', 'ROLE_GERENTE'] },
    loadComponent: () => import('./pages/entity-list.page').then((m) => m.EntityListPage)
  },
  {
    path: 'finance',
    title: 'Financeiro inteligente',
    canActivate: [roleGuard],
    data: { roles: FINANCE_ROLES },
    loadComponent: () => import('./pages/finance-compliance.page').then((m) => m.FinanceCompliancePage)
  },
  {
    path: 'expense-types',
    title: 'Tipos de Despesa',
    canActivate: [roleGuard],
    data: { entity: 'expenseTypes', roles: ['ROLE_DONO_GERENTE', 'ROLE_FINANCEIRO', 'ROLE_ADMIN', 'ROLE_GERENTE'] },
    loadComponent: () => import('./pages/entity-list.page').then((m) => m.EntityListPage)
  },
  {
    path: 'settings',
    title: 'Configuracoes',
    canActivate: [roleGuard],
    data: { roles: MANAGER_ROLES },
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
