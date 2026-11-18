import { Routes } from '@angular/router';

import { requireAuthGuard, roleGuard } from './core/auth.guard';

const STAFF_DASHBOARD_ROLES = ['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_FINANCEIRO', 'ROLE_VENDEDOR', 'ROLE_CAIXA', 'ROLE_DESPACHANTE', 'ROLE_ADMIN', 'ROLE_GERENTE', 'ROLE_GESTOR'];
const MANAGER_ROLES = ['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_ADMIN', 'ROLE_GERENTE'];
const FINANCE_ROLES = ['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_FINANCEIRO', 'ROLE_AUDITOR', 'ROLE_CONTADOR', 'ROLE_ADVOGADO', 'ROLE_ADMIN', 'ROLE_GERENTE'];
const FINANCE_FOUNDATION_ROLES = [
  'ROLE_FINANCEIRO',
  'ROLE_FINANCAS',
  'ROLE_AUDITOR',
  'ROLE_CONTADOR',
  'ROLE_GERENTE',
  'ROLE_GESTOR',
  'ROLE_ADMIN',
  'ROLE_ADMINISTRACAO',
  'ROLE_ADMIN_MASTER',
  'ROLE_DONO',
  'ROLE_DONO_GERENTE',
  'ROLE_SYSADMIN'
];
const CASHIER_ROLES = ['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_CAIXA', 'ROLE_ADMIN', 'ROLE_GERENTE'];
const SALES_ROLES = ['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_VENDEDOR', 'ROLE_CAIXA', 'ROLE_ADMIN', 'ROLE_GERENTE'];

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
    data: { entity: 'clients', roles: ['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_VENDEDOR', 'ROLE_CAIXA', 'ROLE_ADMIN', 'ROLE_GERENTE'] },
    loadComponent: () => import('./pages/entity-list.page').then((m) => m.EntityListPage)
  },
  {
    path: 'suppliers',
    title: 'Fornecedores',
    canActivate: [roleGuard],
    data: { entity: 'suppliers', roles: ['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_FINANCEIRO', 'ROLE_ADMIN', 'ROLE_GERENTE'] },
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
    data: { roles: SALES_ROLES },
    loadComponent: () => import('./pages/point-of-sale.page').then((m) => m.PointOfSalePage)
  },
  {
    path: 'cashier',
    title: 'Caixa',
    canActivate: [roleGuard],
    data: { roles: CASHIER_ROLES },
    loadComponent: () => import('./pages/cashier.page').then((m) => m.CashierPage)
  },
  {
    path: 'sales-history',
    title: 'Histórico de Vendas',
    canActivate: [roleGuard],
    data: { roles: SALES_ROLES },
    loadComponent: () => import('./pages/sales-history.page').then((m) => m.SalesHistoryPage)
  },
  {
    path: 'invoice-entry',
    title: 'Entrada de NF',
    canActivate: [roleGuard],
    data: { roles: ['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_FINANCEIRO', 'ROLE_DESPACHANTE', 'ROLE_ADMIN', 'ROLE_GERENTE'] },
    loadComponent: () => import('./pages/invoice-entry.page').then((m) => m.InvoiceEntryPage)
  },
  {
    path: 'expenses',
    title: 'Despesas',
    canActivate: [roleGuard],
    data: { entity: 'expenses', roles: ['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_FINANCEIRO', 'ROLE_ADMIN', 'ROLE_GERENTE'] },
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
    path: 'finance/foundation',
    title: 'Fundação Financeira',
    canActivate: [roleGuard],
    data: { roles: FINANCE_FOUNDATION_ROLES },
    loadComponent: () => import('./pages/finance-foundation.page').then((m) => m.FinanceFoundationPage)
  },
  {
    path: 'finance/dre',
    title: 'DRE Gerencial',
    canActivate: [roleGuard],
    data: { roles: FINANCE_FOUNDATION_ROLES },
    loadComponent: () => import('./pages/finance-dre.page').then((m) => m.FinanceDrePage)
  },
  {
    path: 'finance/cash-flow',
    title: 'Fluxo de Caixa',
    canActivate: [roleGuard],
    data: { roles: FINANCE_FOUNDATION_ROLES },
    loadComponent: () => import('./pages/finance-cash-flow.page').then((m) => m.FinanceCashFlowPage)
  },
  {
    path: 'finance/indicators',
    title: 'Inteligência Administrativa',
    canActivate: [roleGuard],
    data: { roles: FINANCE_FOUNDATION_ROLES },
    loadComponent: () => import('./pages/finance-indicators.page').then((m) => m.FinanceIndicatorsPage)
  },
  {
    path: 'finance-operations',
    title: 'Financeiro operacional',
    canActivate: [roleGuard],
    data: { roles: FINANCE_ROLES },
    loadComponent: () => import('./pages/finance-operations.page').then((m) => m.FinanceOperationsPage)
  },
  {
    path: 'reports',
    title: 'Relatorios',
    canActivate: [roleGuard],
    data: { roles: ['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_FINANCEIRO', 'ROLE_ADMIN', 'ROLE_GERENTE', 'ROLE_GESTOR'] },
    loadComponent: () => import('./pages/reports.page').then((m) => m.ReportsPage)
  },
  {
    path: 'expense-types',
    title: 'Tipos de Despesa',
    canActivate: [roleGuard],
    data: { entity: 'expenseTypes', roles: ['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_FINANCEIRO', 'ROLE_ADMIN', 'ROLE_GERENTE'] },
    loadComponent: () => import('./pages/entity-list.page').then((m) => m.EntityListPage)
  },
  {
    path: 'payment-methods',
    title: 'Formas de Pagamento',
    canActivate: [roleGuard],
    data: { entity: 'paymentMethods', roles: MANAGER_ROLES },
    loadComponent: () => import('./pages/entity-list.page').then((m) => m.EntityListPage)
  },
  {
    path: 'payment-conditions',
    title: 'Condições de Pagamento',
    canActivate: [roleGuard],
    data: { roles: FINANCE_ROLES },
    loadComponent: () => import('./pages/payment-conditions.page').then((m) => m.PaymentConditionsPage)
  },
  {
    path: 'categories',
    title: 'Categorias',
    canActivate: [roleGuard],
    data: { entity: 'categories', roles: MANAGER_ROLES },
    loadComponent: () => import('./pages/entity-list.page').then((m) => m.EntityListPage)
  },
  {
    path: 'product-groups',
    title: 'Grupos de Produtos',
    canActivate: [roleGuard],
    data: { entity: 'productGroups', roles: MANAGER_ROLES },
    loadComponent: () => import('./pages/entity-list.page').then((m) => m.EntityListPage)
  },
  {
    path: 'menus',
    title: 'Menus',
    canActivate: [roleGuard],
    data: { entity: 'menus', roles: MANAGER_ROLES },
    loadComponent: () => import('./pages/entity-list.page').then((m) => m.EntityListPage)
  },
  {
    path: 'profiles',
    title: 'Perfis',
    canActivate: [roleGuard],
    data: { entity: 'profiles', roles: MANAGER_ROLES },
    loadComponent: () => import('./pages/entity-list.page').then((m) => m.EntityListPage)
  },
  {
    path: 'settings',
    title: 'Preferências',
    canActivate: [requireAuthGuard],
    data: { mode: 'personal' },
    loadComponent: () => import('./pages/settings.page').then((m) => m.SettingsPage)
  },
  {
    path: 'admin-settings',
    title: 'Administração',
    canActivate: [roleGuard],
    data: { mode: 'admin', roles: MANAGER_ROLES },
    loadComponent: () => import('./pages/settings.page').then((m) => m.SettingsPage)
  },
  {
    path: '**',
    redirectTo: ''
  }
];
