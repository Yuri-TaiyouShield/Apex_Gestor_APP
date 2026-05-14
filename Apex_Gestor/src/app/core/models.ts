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

export interface TaxBracket {
  limite?: number | null;
  aliquota: number;
  parcelaDeduzir?: number;
}

export interface LaborCalculationRequest {
  salarioMensal: number;
  jornadaMensal: number;
  percentualHoraExtra?: number;
  horasRelogioNoturnas?: number;
  totalVerbasVariaveis?: number;
  diasUteisMes?: number;
  domingosFeriadosMes?: number;
  salarioMinimo?: number;
  percentualInsalubridade?: number;
  salarioBase?: number;
  custoRealPassagens?: number;
  quantidadeFilhosElegiveis?: number;
  cotaSalarioFamilia?: number;
  mediaAdicionais?: number;
  mediaDsr?: number;
  mesesTrabalhados13?: number;
  diasFalta?: number;
  dsrSemanaFalta?: number;
  salarioContribuicao?: number;
  tabelaInss?: TaxBracket[];
  anosCompletosTrabalhados?: number;
  saldoFgts?: number;
  percentualMultaFgts?: number;
  maiorRemuneracao?: number;
  verbasIncontroversas?: number;
  salariosAteTerminoContrato?: number;
  mesesRestantesEstabilidade?: number;
  valorNominal?: number;
  fatorIpcaEAcumulado?: number;
  taxaSelicAcumulada?: number;
}

export interface TaxCalculationRequest {
  salarioBruto?: number;
  inssEmpregado?: number;
  dependentes?: number;
  deducaoLegalDependente?: number;
  pensao?: number;
  tabelaIrrf?: TaxBracket[];
  folhaSalarios?: number;
  proLabore?: number;
  receitaBruta?: number;
  receitaLucroPresumido?: number;
  limiteAdicionalIrpj?: number;
}

export interface AdmCalcRequest {
  precoVenda?: number;
  quantidade?: number;
  receitaTotal?: number;
  cmv?: number;
  despesas?: number;
  impostos?: number;
  custosFixos?: number;
  custosVariaveis?: number;
  margemContribuicao?: number;
  ativoCirculante?: number;
  ativoNaoCirculante?: number;
  passivoCirculante?: number;
  passivoNaoCirculante?: number;
  estoque?: number;
  custoBem?: number;
  valorResidual?: number;
  vidaUtil?: number;
  custoTotal?: number;
  quantidadeProduzida?: number;
  margemDesejada?: number;
  ganhoInvestimento?: number;
  custoInvestimento?: number;
  valorPresente?: number;
  valorFuturo?: number;
  taxaDesconto?: number;
  periodo?: number;
  fluxosDeCaixa?: number[];
}

export interface FinancialCalculationResponse {
  calculationId: number;
  tipo: string;
  calculadoEm: string;
  resultados: Record<string, number>;
  alertas: string[];
}

export interface FinancialDocumentRequest {
  tipoDocumento: string;
  funcionarioNome: string;
  funcionarioEmail: string;
  cargoAssinanteObrigatorio?: string;
  conteudo: string;
  referencia?: string;
}

export interface SignFinancialDocumentRequest {
  nomeAssinante: string;
  cargoAssinante: string;
  emailAssinante?: string;
  certificadoFingerprint?: string;
  aprovado: boolean;
  observacao?: string;
}

export interface FinancialDocument {
  idDocumento: number;
  tipoDocumento: string;
  funcionarioNome: string;
  funcionarioEmail: string;
  status: string;
  cargoAssinanteObrigatorio: string;
  geradoPor: string;
  geradoEm: string;
  assinadoPor?: string;
  cargoAssinante?: string;
  assinadoEm?: string;
  enviadoEm?: string;
  assinaturaDigitalHash?: string;
  assuntoEmail?: string;
  mensagemEmail?: string;
  referencia?: string;
}

export interface FinancialAuditEvent {
  idEvento: number;
  tipoEvento: string;
  alvoTipo: string;
  alvoId?: number;
  atorLogin: string;
  valorAnterior?: string;
  valorNovo?: string;
  metadados?: string;
  criadoEm: string;
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
