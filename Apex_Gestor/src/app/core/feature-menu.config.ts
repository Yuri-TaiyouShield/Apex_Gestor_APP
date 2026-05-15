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
  allowedRoles?: ApexRole[];
  requiredFeatures?: ApexFeatureKey[];
  minimumTier?: 'Essential' | 'Growth' | 'Premium';
}

export const APEX_NAV_ITEMS: ApexNavItem[] = [
  { label: 'Loja Online', path: '/store', icon: 'storefront-outline', audience: 'both', requiredFeatures: ['B2C_WHITE_LABEL'] },
  { label: 'Carrinho', path: '/cart', icon: 'cart-outline', audience: 'cliente', requiredFeatures: ['B2C_WHITE_LABEL'] },
  { label: 'Dashboard', path: '/', icon: 'bar-chart-outline', audience: 'staff', allowedRoles: ['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_FINANCEIRO', 'ROLE_VENDEDOR', 'ROLE_CAIXA', 'ROLE_DESPACHANTE', 'ROLE_ADMIN', 'ROLE_GERENTE', 'ROLE_GESTOR'] },
  { label: 'PDV / Caixa', path: '/pos', icon: 'cash-outline', audience: 'staff', allowedRoles: ['ROLE_DONO_GERENTE', 'ROLE_VENDEDOR', 'ROLE_CAIXA', 'ROLE_ADMIN', 'ROLE_GERENTE'] },
  { label: 'Produtos', path: '/products', icon: 'cube-outline', audience: 'staff', allowedRoles: ['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_FINANCEIRO', 'ROLE_VENDEDOR', 'ROLE_CAIXA', 'ROLE_ADMIN', 'ROLE_GERENTE'] },
  { label: 'Clientes CRM', path: '/clients', icon: 'people-outline', audience: 'staff', allowedRoles: ['ROLE_DONO_GERENTE', 'ROLE_VENDEDOR', 'ROLE_CAIXA', 'ROLE_ADMIN', 'ROLE_GERENTE'] },
  { label: 'Fornecedores', path: '/suppliers', icon: 'business-outline', audience: 'staff', allowedRoles: ['ROLE_DONO_GERENTE', 'ROLE_FINANCEIRO', 'ROLE_ADMIN', 'ROLE_GERENTE'] },
  { label: 'Funcionarios', path: '/users', icon: 'person-circle-outline', audience: 'staff', allowedRoles: ['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_ADMIN', 'ROLE_GERENTE'] },
  { label: 'Entrada XML NF', path: '/invoice-entry', icon: 'document-text-outline', audience: 'staff', allowedRoles: ['ROLE_DONO_GERENTE', 'ROLE_FINANCEIRO', 'ROLE_ADMIN', 'ROLE_GERENTE'] },
  { label: 'Despesas', path: '/expenses', icon: 'receipt-outline', audience: 'staff', allowedRoles: ['ROLE_DONO_GERENTE', 'ROLE_FINANCEIRO', 'ROLE_ADMIN', 'ROLE_GERENTE'], requiredFeatures: ['FINANCIAL_CORE'] },
  { label: 'Financeiro Pro', path: '/finance', icon: 'calculator-outline', audience: 'staff', allowedRoles: ['ROLE_DONO_GERENTE', 'ROLE_FINANCEIRO', 'ROLE_AUDITOR', 'ROLE_CONTADOR', 'ROLE_ADVOGADO', 'ROLE_ADMIN', 'ROLE_GERENTE'], requiredFeatures: ['ADVANCED_FINANCE'] },
  { label: 'Comissoes Omnichannel', path: '/finance', icon: 'git-network-outline', audience: 'staff', allowedRoles: ['ROLE_DONO_GERENTE', 'ROLE_FINANCEIRO', 'ROLE_VENDEDOR', 'ROLE_ADMIN', 'ROLE_GERENTE'], requiredFeatures: ['COMMISSION_OMNICHANNEL'] },
  { label: 'Roteamento Multi-Loja', path: '/invoice-entry', icon: 'trail-sign-outline', audience: 'staff', allowedRoles: ['ROLE_DONO_GERENTE', 'ROLE_DESPACHANTE', 'ROLE_ADMIN', 'ROLE_GERENTE'], requiredFeatures: ['MULTI_STORE_ROUTING'], minimumTier: 'Premium' },
  { label: 'Relatorios da Rede', path: '/', icon: 'analytics-outline', audience: 'staff', allowedRoles: ['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_ADMIN', 'ROLE_GERENTE'], requiredFeatures: ['HIERARCHICAL_REPORTS'], minimumTier: 'Premium' },
  { label: 'Tipos de Despesa', path: '/expense-types', icon: 'pricetags-outline', audience: 'staff', allowedRoles: ['ROLE_DONO_GERENTE', 'ROLE_FINANCEIRO', 'ROLE_ADMIN', 'ROLE_GERENTE'], requiredFeatures: ['FINANCIAL_CORE'] },
  { label: 'Privacidade LGPD', path: '/privacy', icon: 'shield-checkmark-outline', audience: 'both' },
  { label: 'Configuracoes', path: '/settings', icon: 'settings-outline', audience: 'staff', allowedRoles: ['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_ADMIN', 'ROLE_GERENTE'] }
];
