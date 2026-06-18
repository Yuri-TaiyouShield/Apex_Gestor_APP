export type Persona = 'cliente' | 'vendedor' | 'gerente' | 'admin';

export type ApexRole =
  | 'ROLE_SYSADMIN'
  | 'ROLE_DONO_GERENTE'
  | 'ROLE_FINANCEIRO'
  | 'ROLE_VENDEDOR'
  | 'ROLE_CAIXA'
  | 'ROLE_DESPACHANTE'
  | 'ROLE_CLIENTE_B2C'
  | 'ROLE_ADMIN'
  | 'ROLE_GERENTE'
  | 'ROLE_GESTOR'
  | 'ROLE_FINANCAS'
  | 'ROLE_ADMINISTRACAO'
  | 'ROLE_AUDITOR'
  | 'ROLE_CONTADOR'
  | 'ROLE_ADVOGADO';

export interface Identifiable {
  [key: string]: unknown;
}

export interface Categoria {
  idCategoria?: number;
  descricao: string;
  status?: number;
}

export interface GrupoProduto {
  idGrupoProduto?: number;
  nome: string;
  descricao?: string;
  status?: number;
}

export interface Fornecedor {
  idFornecedor?: number;
  status?: number;
  razaoSocial: string;
  nomeFantasia?: string;
  cnpj: string;
  telefone?: string;
  celular?: string;
  email?: string;
  inscricaoEstadual?: string;
  situacaoFiscal?: string;
  condicoesPagamento?: string;
  dadosBancarios?: string;
  observacoes?: string;
  empresaId?: number;
  filialId?: number;
  dataCadastro?: string;
  endereco?: Endereco;
}

export interface Produto {
  idProduto?: number;
  descricao: string;
  custo?: number;
  codigoBarras?: string;
  codigoInterno?: string;
  codigoOriginal?: string;
  marca?: string;
  grupo?: string;
  subgrupo?: string;
  modelo?: string;
  imagemUrl?: string;
  imagemMimeType?: string;
  imagemAtualizadaEm?: string;
  enriquecimentoCatalogoStatus?: string;
  unidadeMedida?: string;
  embalagem?: string;
  quantidadeEstoque: number;
  estoqueMinimo?: number;
  valorVenda: number;
  margem?: number;
  localizacao?: string;
  situacaoFiscal?: string;
  ncm?: string;
  cfop?: string;
  icms?: number;
  ipi?: number;
  pis?: number;
  cofins?: number;
  observacoes?: string;
  empresaId?: number;
  filialId?: number;
  status?: number;
  categoria?: Categoria;
  fornecedor?: Fornecedor;
}

export interface Cliente {
  idCliente?: number;
  nomeRazao: string;
  nomeFantasia?: string;
  telefone?: string;
  celular?: string;
  email?: string;
  tipoDocumento?: number;
  cpfCnpj?: string;
  rgIe?: string;
  situacaoFiscal?: string;
  observacoes?: string;
  empresaId?: number;
  filialId?: number;
  dataCadastro?: string;
  status?: number;
  enderecos?: Endereco[];
}

export interface Endereco {
  idEndereco?: number;
  estado?: string;
  cidade?: string;
  cep: string;
  bairro?: string;
  logradouro?: string;
  uf?: string;
  numero?: string;
  complemento?: string;
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

export interface Menu {
  idMenu?: number;
  nome: string;
  link: string;
  icone?: string;
  exibir?: number;
  perfis?: Perfil[];
}

export interface Perfil {
  idPerfil?: number;
  nome: string;
  status?: number;
  menus?: Menu[];
}

export interface FormaPagamento {
  idFormaPagamento?: number;
  nome: string;
  descricao?: string;
  tipoPagamento?: number;
  status?: number;
}

export interface CondicaoPagamento {
  idCondicaoPagamento?: number;
  nome: string;
  tipoPagamento: string;
  quantidadeParcelas: number;
  prazoDias: number;
  percentual: number;
  restricoes?: string;
  status?: number;
  empresaId?: number;
  filialId?: number;
}

export interface CaixaMovimentacao {
  idCaixaMovimentacao?: number;
  tipo: string;
  valor: number;
  observacao?: string;
  referenciaTipo?: string;
  referenciaId?: number;
  criadoEm: string;
}

export interface CaixaSessao {
  idCaixaSessao: number;
  operadorLogin: string;
  empresaId?: number;
  filialId?: number;
  status: 'ABERTO' | 'FECHADO';
  saldoInicial: number;
  totalVendas: number;
  totalSuprimentos: number;
  totalSangrias: number;
  saldoAtual: number;
  saldoFinal?: number;
  saldoInformado?: number;
  diferencaFechamento?: number;
  abertoEm: string;
  fechadoEm?: string;
  movimentacoes: CaixaMovimentacao[];
  resumoPagamentos: Array<{ formaPagamentoId: number; nome: string; total: number; quantidadePagamentos: number }>;
}

export type VendaStatus = 'RASCUNHO' | 'EM_ATENDIMENTO' | 'AGUARDANDO_CAIXA' | 'EM_PAGAMENTO' |
  'PAGA' | 'FINALIZADA' | 'CANCELADA' | 'DEVOLVIDA' | 'TROCA_PARCIAL' | 'TROCA_TOTAL';

export interface VendaItem {
  produtoId: number;
  descricao: string;
  quantidade: number;
  precoUnitario: number;
  precoTotal: number;
}

export interface Venda {
  idVenda: number;
  numeroVenda?: string;
  clienteId: number;
  clienteNome: string;
  usuarioId: number;
  usuarioNome: string;
  status: VendaStatus;
  valorTotal: number;
  desconto: number;
  observacao?: string;
  empresaId: number;
  filialId: number;
  caixaSessaoId?: number;
  condicaoPagamentoId?: number;
  formaPagamentoRealId?: number;
  valorRecebido?: number;
  troco?: number;
  diferenca?: number;
  dataVenda: string;
  enviadoCaixaEm?: string;
  recebidoCaixaEm?: string;
  finalizadoEm?: string;
  motivoRecusa?: string;
  itens: VendaItem[];
  pagamentos: Array<{ formaPagamentoId: number; nome: string; valorPago: number; numeroParcelas: number; creditoId?: number }>;
}

export interface RecebimentoPagamento {
  formaPagamentoId: number;
  valor: number;
  numeroParcelas?: number;
  creditoId?: number;
}

export interface CreditoCliente {
  idCredito: number;
  clienteId: number;
  vendaOrigemId: number;
  devolucaoId: number;
  valorOriginal: number;
  saldo: number;
  status: string;
  empresaId: number;
  filialId: number;
  criadoEm: string;
}

export interface ContaReceber {
  idContaReceber: number;
  vendaId: number;
  numeroVenda?: string;
  clienteId?: number;
  clienteNome?: string;
  formaPagamentoId?: number;
  formaPagamentoNome?: string;
  condicaoPagamentoId?: number;
  caixaSessaoId?: number;
  valorBruto: number;
  desconto: number;
  valorLiquido: number;
  valorPago: number;
  saldo: number;
  vencimento: string;
  status: string;
  empresaId: number;
  filialId: number;
}

export interface ContaPagar {
  idContaPagar: number;
  fornecedorId?: number;
  fornecedorNome?: string;
  categoriaId?: number;
  categoriaNome?: string;
  descricao: string;
  valor: number;
  vencimento: string;
  formaPagamentoPrevistaId?: number;
  formaPagamentoPrevistaNome?: string;
  status: string;
  empresaId: number;
  filialId: number;
}

export interface DashboardVendedor {
  meta: number;
  metaConfigurada: boolean;
  vendidoHoje: number;
  vendidoMes: number;
  vendasHoje: number;
  vendasMes: number;
  itensHoje: number;
  itensMes: number;
  faltanteMeta: number;
  percentualMeta: number;
  ticketMedio: number;
  aguardandoCaixa: number;
  finalizadasCaixa: number;
  recusadasCaixa: number;
  canceladasOuDevolvidas: number;
  ultimasVendas: Array<{ idVenda: number; numeroVenda?: string; status: VendaStatus; total: number; dataVenda: string }>;
}

export interface VendaDevolucaoRequest {
  tipo: 'TOTAL' | 'PARCIAL' | 'TROCA';
  motivo: string;
  itens: Array<{ produtoOriginalId: number; produtoNovoId?: number; quantidade: number }>;
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

export interface RelatorioOperacional {
  tipo: string;
  totalRegistros: number;
  ativos: number;
  inativos: number;
  valorTotal: number;
  alertas: string[];
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

export interface PricingCalculationRequest {
  productCost: number;
  fixedExpensePercent?: number;
  taxPercent?: number;
  desiredMarginPercent?: number;
  sellerCommissionPercent?: number;
  onlineSale?: boolean;
  branchCode?: string;
  orderId?: string;
  grossAmount?: number;
}

export interface PricingCalculationResponse {
  tenantCode: string;
  planCode: string;
  productCost: number;
  fixedExpensePercent: number;
  taxPercent: number;
  desiredMarginPercent: number;
  sellerCommissionPercent: number;
  suggestedPrice: number;
  expectedProfit: number;
  commissionPoolId?: number;
  message: string;
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
  placeholder?: string;
  group?: string;
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

export interface B2cCartMergeItem {
  produtoId: number;
  quantidade: number;
}

export interface B2cCartMergeRequest {
  clienteId: number;
  itens: B2cCartMergeItem[];
}

export interface B2cCartMergeResponse {
  cartId: number;
  clienteId: number;
  totalItems: number;
  subtotal: number;
  status: string;
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
  tenantCode?: string;
}

export interface TenantBranding {
  primaryColor: string;
  secondaryColor: string;
  logoUrl?: string;
  storefrontName: string;
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
  tenantCode?: string;
  tenantName?: string;
  subscriptionTier?: string;
  features?: string[];
  branding?: TenantBranding | null;
}
