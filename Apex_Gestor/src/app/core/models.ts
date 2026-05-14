export type Persona = 'cliente' | 'vendedor' | 'gerente' | 'admin';

export interface Identifiable {
  [key: string]: unknown;
}

export interface Categoria {
  idCategoria?: number;
  descricao: string;
  status?: number;
}

export interface Fornecedor {
  idFornecedor?: number;
  status?: number;
  razaoSocial: string;
  nomeFantasia?: string;
  cnpj: string;
  telefone?: string;
}

export interface Produto {
  idProduto?: number;
  descricao: string;
  custo?: number;
  codigoBarras?: string;
  marca?: string;
  unidadeMedida?: string;
  quantidadeEstoque: number;
  estoqueMinimo?: number;
  valorVenda: number;
  status?: number;
  categoria?: Categoria;
  fornecedor?: Fornecedor;
}

export interface Cliente {
  idCliente?: number;
  nomeRazao: string;
  telefone?: string;
  tipoDocumento?: number;
  cpfCnpj?: string;
  status?: number;
}

export interface Usuario {
  idUsuario?: number;
  nome: string;
  login: string;
  status?: number;
  perfil?: {
    idPerfil?: number;
    nome?: string;
  };
}

export interface FormaPagamento {
  idFormaPagamento?: number;
  nome: string;
  descricao?: string;
  tipoPagamento?: number;
  status?: number;
}

export interface Despesa {
  idDespesa?: number;
  descricao: string;
  valor: number;
  dataVencimento?: string;
  dataPagamento?: string;
  status?: number;
  tipoDespesa?: TipoDespesa;
}

export interface TipoDespesa {
  idTipoDespesa?: number;
  nome: string;
  status?: number;
}

export interface RelatorioFinanceiro {
  totalVendas: number;
  totalCustosProdutos: number;
  totalDespesas: number;
  lucroLiquido: number;
  saldoCaixa: number;
}

export interface CartItem {
  produto: Produto;
  quantidade: number;
}

export interface CheckoutPayment {
  formaPagamentoId: number;
  nome: string;
  valorPago: number;
}

export interface EntityField {
  key: string;
  label: string;
  type: 'text' | 'number' | 'date';
  required?: boolean;
}

export interface EntityConfig {
  key: string;
  title: string;
  subtitle: string;
  endpoint: string;
  idKey: string;
  searchKeys: string[];
  columns: Array<{
    key: string;
    label: string;
    format?: 'currency' | 'status' | 'date' | 'nested';
  }>;
  fields: EntityField[];
  deactivatePath?: (id: number | string) => string;
}

export interface LoginRequest {
  login: string;
  senha: string;
  totpCode?: string;
  consentVersion?: string;
  acceptedPrivacyTerms?: boolean;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresAt: string;
  tokenType: 'Bearer';
  usuarioId: number;
  nome: string;
  login: string;
  roles: string[];
}

export interface ConsentRequest {
  titularId?: string;
  tipoTitular: string;
  documento?: string;
  versao: string;
  canal: string;
}

export interface PrivacyRequestPayload {
  titularId: string;
  tipo: 'EXPORTACAO' | 'EXCLUSAO';
}

export interface LicenseValidationRequest {
  licenseKey: string;
  deviceFingerprint: string;
  deviceLabel: string;
  platform: string;
  appVersion: string;
  appId: string;
}

export interface LicenseValidationResponse {
  valid: boolean;
  status: string;
  message: string;
  expiresAt?: string;
  remainingActivations?: number;
  deviceHash?: string;
  appId?: string;
  licensePlan?: string;
  allowedApps?: string[];
  activatedApps?: string[];
}
