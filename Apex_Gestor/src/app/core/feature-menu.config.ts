import { ApexRole } from './models';

export type ApexFeatureKey =
  | 'BASIC_PRICING'
  | 'FINANCIAL_CORE'
  | 'ADVANCED_FINANCE'
  | 'COMMISSION_OMNICHANNEL'
  | 'B2C_WHITE_LABEL'
  | 'MULTI_STORE_ROUTING'
  | 'SOFT_STOCK_ALLOCATION'
  | 'HIERARCHICAL_REPORTS';

export interface ApexNavItem {
  label: string;
  path: string;
  icon: string;
  audience: 'cliente' | 'staff' | 'both';
  group?: 'principal' | 'operacao' | 'gestao' | 'admin';
  allowedRoles?: ApexRole[];
  requiredFeatures?: ApexFeatureKey[];
  minimumTier?: 'Essential' | 'Growth' | 'Premium';
}

const STAFF_ROLES: ApexRole[] = ['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_FINANCEIRO', 'ROLE_VENDEDOR', 'ROLE_CAIXA', 'ROLE_DESPACHANTE', 'ROLE_ADMIN', 'ROLE_GERENTE', 'ROLE_GESTOR'];
const SALES_ROLES: ApexRole[] = ['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_VENDEDOR', 'ROLE_CAIXA', 'ROLE_ADMIN', 'ROLE_GERENTE'];
const CASHIER_ROLES: ApexRole[] = ['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_CAIXA', 'ROLE_ADMIN', 'ROLE_GERENTE'];
const MANAGER_ROLES: ApexRole[] = ['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_ADMIN', 'ROLE_GERENTE'];
const FINANCE_ROLES: ApexRole[] = ['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_FINANCEIRO', 'ROLE_ADMIN', 'ROLE_GERENTE'];

export const APEX_NAV_ITEMS: ApexNavItem[] = [
  { label: 'Loja Online', path: '/store', icon: 'storefront-outline', audience: 'cliente', group: 'principal', requiredFeatures: ['B2C_WHITE_LABEL'] },
  { label: 'Carrinho', path: '/cart', icon: 'cart-outline', audience: 'cliente', group: 'principal', requiredFeatures: ['B2C_WHITE_LABEL'] },
  { label: 'Dashboard', path: '/', icon: 'bar-chart-outline', audience: 'staff', group: 'principal', allowedRoles: STAFF_ROLES },
  { label: 'PDV', path: '/pos', icon: 'cart-outline', audience: 'staff', group: 'principal', allowedRoles: SALES_ROLES },
  { label: 'Caixa', path: '/cashier', icon: 'cash-outline', audience: 'staff', group: 'principal', allowedRoles: CASHIER_ROLES },
  { label: 'Histórico de Vendas', path: '/sales-history', icon: 'receipt-outline', audience: 'staff', group: 'operacao', allowedRoles: SALES_ROLES },
  { label: 'Produtos / Estoque', path: '/products', icon: 'cube-outline', audience: 'staff', group: 'operacao', allowedRoles: SALES_ROLES },
  { label: 'Clientes CRM', path: '/clients', icon: 'people-outline', audience: 'staff', group: 'operacao', allowedRoles: SALES_ROLES },
  { label: 'Fornecedores', path: '/suppliers', icon: 'business-outline', audience: 'staff', group: 'operacao', allowedRoles: FINANCE_ROLES },
  { label: 'Compras / NF', path: '/invoice-entry', icon: 'document-text-outline', audience: 'staff', group: 'operacao', allowedRoles: FINANCE_ROLES },
  { label: 'Financeiro', path: '/finance-operations', icon: 'receipt-outline', audience: 'staff', group: 'gestao', allowedRoles: FINANCE_ROLES, requiredFeatures: ['FINANCIAL_CORE'] },
  { label: 'Condições de Pagamento', path: '/payment-conditions', icon: 'card-outline', audience: 'staff', group: 'gestao', allowedRoles: FINANCE_ROLES },
  { label: 'Relatórios', path: '/reports', icon: 'bar-chart-outline', audience: 'staff', group: 'gestao', allowedRoles: [...FINANCE_ROLES, 'ROLE_GESTOR'] },
  { label: 'Funcionários / Ponto', path: '/users', icon: 'person-circle-outline', audience: 'staff', group: 'admin', allowedRoles: MANAGER_ROLES },
  { label: 'Administração', path: '/admin-settings', icon: 'shield-checkmark-outline', audience: 'staff', group: 'admin', allowedRoles: MANAGER_ROLES },
  { label: 'Preferências', path: '/settings', icon: 'settings-outline', audience: 'both', group: 'admin' }
];
