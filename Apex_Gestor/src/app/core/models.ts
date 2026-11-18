export type Persona = 'cliente' | 'vendedor' | 'gerente' | 'admin';

export type ApexRole =
  | 'ROLE_SYSADMIN'
  | 'ROLE_DONO_GERENTE'
  | 'ROLE_DONO'
  | 'ROLE_FINANCEIRO'
  | 'ROLE_VENDEDOR'
  | 'ROLE_CAIXA'
  | 'ROLE_DESPACHANTE'
  | 'ROLE_CLIENTE_B2C'
  | 'ROLE_ADMIN'
  | 'ROLE_ADMIN_MASTER'
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
  dataCompetencia?: string;
  dataEmissao?: string;
  status?: number;
  tipoDespesaId?: number;
  tipoDespesa?: TipoDespesa;
  planoContaId?: number | null;
  planoContaCodigo?: string;
  planoContaNome?: string;
  centroCustoId?: number | null;
  centroCustoCodigo?: string;
  centroCustoNome?: string;
}

export interface TipoDespesa {
  idTipoDespesa?: number;
  nome: string;
  status?: number;
  naturezaDespesa?: string;
  planoContaId?: number | null;
  planoContaCodigo?: string;
  planoContaNome?: string;
  centroCustoId?: number | null;
  centroCustoCodigo?: string;
  centroCustoNome?: string;
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
  escalaPercentual?: 'AUTO' | 'DECIMAL' | 'PERCENTUAL';
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
  tipoTitular?: string;
  tipo: 'EXPORTACAO' | 'EXCLUSAO' | 'ANONIMIZACAO';
  justificativa: string;
}

export interface LgpdSimulacaoAnonimizacao {
  clienteId: number;
  elegivelAnonimizacaoPessoal: boolean;
  bloqueioOperacionalAtivo: boolean;
  bloqueioExclusaoFisicaPorRetencaoLegal: boolean;
  vendasVinculadas: number;
  contasReceberEmAberto: number;
  creditosAtivos: number;
  obrigacoesRetencao: string[];
  camposAnonimizados: string[];
  camposPreservadosPorObrigacaoLegal: string[];
  exigeAprovacaoSegundoResponsavel: boolean;
  responsaveisElegiveisEmpresa: number;
  simulacaoHash: string;
}

export interface LgpdConfirmacaoAnonimizacaoRequest {
  confirmadoExplicito: boolean;
  justificativa: string;
  simulacaoHash: string;
  solicitacaoId?: number;
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
  contingenciaAte?: string;
  degradedMode?: boolean;
}

export interface TenantEntitlementResponse {
  empresaId: number;
  tenantCode: string;
  tenantName: string;
  subscriptionTier: string;
  features: string[];
  status: string;
  validoAte?: string;
  ultimaValidacaoEm?: string;
  contingenciaAte?: string;
  gracePeriodHoras: number;
  emGracePeriod: boolean;
}

export type TipoContaGerencial =
  | 'RECEITA'
  | 'DEDUCAO'
  | 'CMV'
  | 'DESPESA'
  | 'FINANCEIRO'
  | 'OUTRAS'
  | 'TRIBUTO_LUCRO'
  | 'INVESTIMENTO';

export type NaturezaContaGerencial = 'CREDITO' | 'DEBITO';

export type ClassificacaoCusto = 'FIXO' | 'VARIAVEL' | 'NAO_APLICAVEL';

export type GrupoDreGerencial =
  | 'RECEITA_BRUTA'
  | 'DEVOLUCOES_CANCELAMENTOS'
  | 'DESCONTOS_CONCEDIDOS'
  | 'TRIBUTOS_SOBRE_VENDAS'
  | 'CMV'
  | 'DESPESA_COMERCIAL'
  | 'DESPESA_ADMINISTRATIVA'
  | 'DESPESA_OPERACIONAL'
  | 'DESPESA_PESSOAL'
  | 'DESPESA_INFRAESTRUTURA'
  | 'DESPESA_FINANCEIRA'
  | 'RECEITA_FINANCEIRA'
  | 'OUTRAS_RECEITAS'
  | 'OUTRAS_DESPESAS'
  | 'TRIBUTOS_SOBRE_LUCRO'
  | 'NAO_APLICAVEL_DRE';

export interface PlanoContasRequest {
  codigo: string;
  nome: string;
  descricao?: string;
  contaPaiId?: number | null;
  tipoConta: TipoContaGerencial;
  natureza: NaturezaContaGerencial;
  classificacaoCusto: ClassificacaoCusto;
  grupoDre: GrupoDreGerencial;
  ordemExibicao?: number;
  aceitaLancamento?: boolean;
  ativo?: boolean;
}

export interface PlanoContasResponse {
  idPlanoConta: number;
  contaPaiId?: number | null;
  codigo: string;
  nome: string;
  descricao?: string;
  tipoConta: TipoContaGerencial;
  natureza: NaturezaContaGerencial;
  classificacaoCusto: ClassificacaoCusto;
  grupoDre: GrupoDreGerencial;
  ordemExibicao: number;
  aceitaLancamento: boolean;
  sistemaPadrao: boolean;
  ativo: boolean;
  criadoEm?: string;
  atualizadoEm?: string;
  filhas?: PlanoContasResponse[];
}

export interface PlanoContasBootstrapResponse {
  contasCriadas: number;
  contasJaExistentes: number;
  totalContas: number;
  contas: PlanoContasResponse[];
}

export type TipoCentroCusto =
  | 'LOJA'
  | 'ADMINISTRATIVO'
  | 'COMERCIAL'
  | 'LOGISTICA'
  | 'EXPANSAO'
  | 'GERAL';

export interface CentroCustoRequest {
  codigo: string;
  nome: string;
  descricao?: string;
  tipoOperacional: TipoCentroCusto;
  responsavelUsuarioId?: number | null;
  ativo?: boolean;
}

export interface CentroCustoResponse {
  idCentroCusto: number;
  codigo: string;
  nome: string;
  descricao?: string;
  tipoOperacional: TipoCentroCusto;
  responsavelUsuarioId?: number | null;
  ativo: boolean;
  criadoEm?: string;
  atualizadoEm?: string;
}

export interface CentroCustoBootstrapResponse {
  centrosCriados?: number;
  centrosJaExistentes?: number;
  totalCentros?: number;
  centros?: CentroCustoResponse[];
}

export type OrigemClassificacaoFinanceira =
  | 'VENDA'
  | 'DEVOLUCAO'
  | 'DESPESA'
  | 'CONTA_PAGAR'
  | 'CONTA_RECEBER'
  | 'NFE_ENTRADA'
  | 'CAIXA_MOVIMENTACAO';

export interface RegraClassificacaoRequest {
  nome: string;
  origemTipo: OrigemClassificacaoFinanceira;
  tipoDespesaId?: number | null;
  categoriaId?: number | null;
  formaPagamentoId?: number | null;
  fornecedorId?: number | null;
  palavraChave?: string;
  planoContaId: number;
  centroCustoId?: number | null;
  prioridade?: number;
  ativo?: boolean;
}

export interface RegraClassificacaoResponse {
  idRegraClassificacao: number;
  nome: string;
  origemTipo: OrigemClassificacaoFinanceira;
  tipoDespesaId?: number | null;
  categoriaId?: number | null;
  formaPagamentoId?: number | null;
  fornecedorId?: number | null;
  palavraChave?: string;
  planoContaId: number;
  planoContaCodigo?: string;
  planoContaNome?: string;
  centroCustoId?: number | null;
  centroCustoCodigo?: string;
  centroCustoNome?: string;
  prioridade: number;
  ativo: boolean;
  criadoEm?: string;
  atualizadoEm?: string;
}

export interface ItemPendenteClassificacao {
  origemTipo: OrigemClassificacaoFinanceira;
  registroId: number;
  descricao: string;
  valor: number;
  dataCompetencia?: string;
  sugestaoPlanoContaId?: number | null;
  sugestaoPlanoContaNome?: string;
  sugestaoCentroCustoId?: number | null;
}

export interface PendenciaClassificacaoResponse {
  totalPendentes: number;
  despesasPendentes: number;
  contasPagarPendentes: number;
  contasReceberPendentes: number;
  vendasPendentes: number;
  itens: ItemPendenteClassificacao[];
}

export interface ReclassificacaoManualRequest {
  origemTipo: OrigemClassificacaoFinanceira;
  registroId: number;
  planoContaId: number;
  centroCustoId?: number | null;
  motivo?: string;
}

export interface ResultadoClassificacaoLote {
  processados: number;
  classificados: number;
  naoClassificados: number;
  observacoes: string[];
}

export interface ResumoCompetenciaItem {
  planoContaId: number | null;
  planoContaCodigo?: string;
  planoContaNome?: string;
  grupoDre?: GrupoDreGerencial | string;
  classificacaoCusto?: ClassificacaoCusto | string;
  centroCustoId: number | null;
  centroCustoCodigo?: string;
  centroCustoNome?: string;
  valorCompetencia: number;
  valorRealizadoCaixa: number;
  quantidadeLancamentos: number;
}

export interface ConsolidacaoCompetenciaResponse {
  competenciaAnoMes: string;
  inicioPeriodo: string;
  fimPeriodo: string;
  receitasCompetencia: number;
  cmvCompetencia: number;
  despesasCompetencia: number;
  resultadoCompetencia: number;
  entradasRealizadasCaixa: number;
  saidasRealizadasCaixa: number;
  saldoRealizadoCaixa: number;
  lancamentosSemPlanoConta: number;
  valorSemPlanoConta: number;
  avisosLimitacao: string[];
  linhas: ResumoCompetenciaItem[];
}

export type DreTipoPeriodo = 'MENSAL' | 'TRIMESTRAL' | 'SEMESTRAL' | 'ANUAL' | 'PERSONALIZADO';

export interface DreContaDetalhe {
  idPlanoConta: number | null;
  contaPaiId?: number | null;
  codigo: string;
  nome: string;
  tipoConta?: TipoContaGerencial | string | null;
  natureza?: NaturezaContaGerencial | string | null;
  classificacaoCusto?: ClassificacaoCusto | string | null;
  grupoDre?: GrupoDreGerencial | string | null;
  sintetica: boolean;
  valor: number;
  analiseVerticalPercentual?: number | null;
  valorComparacao?: number | null;
  variacaoAbsoluta?: number | null;
  analiseHorizontalPercentual?: number | null;
  quantidadeLancamentos: number;
  filhas: DreContaDetalhe[];
}

export interface DreLinha {
  numeroLinha: number;
  codigoLinha: string;
  titulo: string;
  tipoLinha: 'RECEITA' | 'DEDUCAO' | 'CUSTO' | 'DESPESA' | 'SUBTOTAL' | 'RESULTADO' | string;
  sinalDemonstracao: '+' | '-' | '=' | '+/-' | string;
  gruposAssociados: (GrupoDreGerencial | string)[];
  valor: number;
  analiseVerticalPercentual?: number | null;
  valorComparacao?: number | null;
  variacaoAbsoluta?: number | null;
  analiseHorizontalPercentual?: number | null;
  statusVariacao?: 'FAVORAVEL' | 'DESFAVORAVEL' | 'ESTAVEL' | 'SEM_BASE' | string;
  quantidadeLancamentos: number;
  explicacaoGerencial: string;
  contas: DreContaDetalhe[];
}

export interface DreIndicadoresResumo {
  receitaBruta: number;
  deducoesTotais: number;
  receitaLiquida: number;
  cmv: number;
  lucroBruto: number;
  margemBrutaPercentual: number;
  despesasOperacionaisTotais: number;
  despesasFixas: number;
  despesasVariaveis: number;
  custosVariaveisTotais: number;
  margemContribuicaoValor: number;
  margemContribuicaoPercentual: number;
  pontoEquilibrioContabilReceita: number;
  resultadoOperacional: number;
  margemOperacionalPercentual: number;
  resultadoFinanceiroLiquido?: number;
  resultadoAntesTributos: number;
  tributosSobreLucro: number;
  resultadoLiquidoGerencial: number;
  margemLiquidaPercentual: number;
  entradasRealizadasCaixa: number;
  saidasRealizadasCaixa: number;
  saldoRealizadoCaixa: number;
  diferencaCompetenciaVsCaixa: number;
}

export interface DreFatorRelevante {
  titulo: string;
  categoria: string;
  valor: number;
  percentualReceitaLiquida?: number | null;
  variacaoPercentual?: number | null;
  impacto: 'POSITIVO' | 'NEGATIVO' | 'ATENCAO' | 'NEUTRO' | string;
  explicacao: string;
}

export interface DreDiagnosticoGerencial {
  situacaoPeriodo: 'LUCRO_SAUDAVEL' | 'LUCRO_APERTADO' | 'EQUILIBRIO' | 'PREJUIZO' | 'SEM_MOVIMENTO' | string;
  mancheteExplicativa: string;
  explicacaoResultado: string;
  explicacaoCompetenciaVsCaixa: string;
  explicacaoPontoEquilibrio: string;
  principaisFatores: DreFatorRelevante[];
  recomendacoesGerenciais: string[];
  avisosLimitacao: string[];
}

export interface DreGerencialResponse {
  tipoPeriodo: DreTipoPeriodo | string;
  rotuloPeriodo: string;
  dataInicio: string;
  dataFim: string;
  comparacaoAtiva: boolean;
  rotuloPeriodoComparacao?: string | null;
  comparacaoInicio?: string | null;
  comparacaoFim?: string | null;
  empresaId: number;
  filialId?: number | null;
  filialNome: string;
  consolidadoEmpresa: boolean;
  centroCustoId?: number | null;
  centroCustoNome?: string | null;
  baseAnaliseVertical: string;
  lancamentosSemClassificacao: number;
  valorSemClassificacao: number;
  indicadores: DreIndicadoresResumo;
  diagnostico: DreDiagnosticoGerencial;
  linhas: DreLinha[];
}

export interface DreLancamentoOrigem {
  origemTipo: 'VENDA' | 'DEVOLUCAO' | 'DESCONTO_VENDA' | 'CMV_VENDA' | 'CMV_DEVOLUCAO' | 'DESPESA' | 'CONTA_PAGAR' | 'CONTA_RECEBER' | string;
  registroId: number;
  documentoReferencia: string;
  descricao: string;
  dataCompetencia: string;
  dataEmissao?: string | null;
  dataLiquidacaoCaixa?: string | null;
  statusOperacional: string;
  valorCompetencia: number;
  impactoDre: 'POSITIVO' | 'NEGATIVO' | string;
  numeroLinhaDre: number;
  codigoLinhaDre?: string;
  tituloLinhaDre: string;
  planoContaId?: number | null;
  planoContaCodigo: string;
  planoContaNome: string;
  grupoDre: GrupoDreGerencial | string;
  centroCustoId?: number | null;
  centroCustoCodigo?: string | null;
  centroCustoNome?: string | null;
  filialId: number;
  rotaDetalheFrontend: string;
}

export interface DreDrillDownResponse {
  rotuloPeriodo: string;
  dataInicio: string;
  dataFim: string;
  numeroLinhaFiltro?: number | null;
  codigoLinhaFiltro?: string | null;
  tituloLinhaFiltro?: string | null;
  planoContaIdFiltro?: number | null;
  planoContaNomeFiltro?: string | null;
  grupoDreFiltro?: GrupoDreGerencial | string | null;
  centroCustoIdFiltro?: number | null;
  filialIdFiltro?: number | null;
  consolidadoEmpresa: boolean;
  totalLancamentos: number;
  somaValorCompetencia: number;
  lancamentos: DreLancamentoOrigem[];
}

export interface DreEvolucaoPonto {
  competencia: string;
  rotulo: string;
  receitaBruta: number;
  receitaLiquida: number;
  cmv: number;
  lucroBruto: number;
  despesasTotais: number;
  resultadoOperacional: number;
  resultadoLiquidoGerencial: number;
  margemBrutaPercentual: number;
  margemOperacionalPercentual: number;
  margemLiquidaPercentual: number;
  saldoRealizadoCaixa: number;
}

export interface DreEvolucaoResponse {
  competenciaInicio: string;
  competenciaFim: string;
  quantidadeMeses: number;
  empresaId: number;
  filialId?: number | null;
  consolidadoEmpresa: boolean;
  centroCustoId?: number | null;
  receitaLiquidaAcumulada: number;
  lucroBrutoAcumulado: number;
  resultadoLiquidoAcumulado: number;
  margemLiquidaMediaPercentual: number;
  tendenciaReceita: 'CRESCIMENTO' | 'QUEDA' | 'ESTAVEL' | string;
  tendenciaResultado: 'CRESCIMENTO' | 'QUEDA' | 'ESTAVEL' | string;
  serie: DreEvolucaoPonto[];
}

export interface DreFilialColuna {
  filialId: number;
  filialCodigo: string;
  filialNome: string;
  receitaBruta: number;
  receitaLiquida: number;
  cmv: number;
  lucroBruto: number;
  despesasTotais: number;
  resultadoOperacional: number;
  resultadoLiquidoGerencial: number;
  margemBrutaPercentual: number;
  margemLiquidaPercentual: number;
  participacaoReceitaLiquidaPercentual: number;
  participacaoResultadoPercentual: number;
  linhas: DreLinha[];
}

export interface DreComparativoFiliaisResponse {
  tipoPeriodo: string;
  rotuloPeriodo: string;
  dataInicio: string;
  dataFim: string;
  empresaId: number;
  centroCustoId?: number | null;
  consolidadoEmpresa: DreIndicadoresResumo;
  filiais: DreFilialColuna[];
  destaquesComparativos: string[];
}

export interface DreQueryParams {
  tipoPeriodo?: DreTipoPeriodo | string;
  competencia?: string;
  ano?: number;
  trimestre?: number;
  semestre?: number;
  dataInicio?: string;
  dataFim?: string;
  compararPeriodoAnterior?: boolean;
  competenciaComparacao?: string;
  comparacaoInicio?: string;
  comparacaoFim?: string;
  centroCustoId?: number | null;
  filialFiltroId?: number | null;
  consolidarFiliais?: boolean;
}

export type TipoContaFinanceira =
  | 'CAIXA_FISICO'
  | 'CONTA_CORRENTE'
  | 'CONTA_INVESTIMENTO'
  | 'OPERADORA_CARTAO'
  | 'CARTEIRA_DIGITAL';

export type TipoMovimentacaoFinanceira =
  | 'ENTRADA'
  | 'SAIDA'
  | 'TRANSFERENCIA_SAIDA'
  | 'TRANSFERENCIA_ENTRADA'
  | 'ESTORNO';

export type OrigemMovimentacaoFinanceira =
  | 'CAIXA_PDV'
  | 'VENDA_AVISTA'
  | 'CONTA_RECEBER'
  | 'CONTA_PAGAR'
  | 'DESPESA'
  | 'TRANSFERENCIA_INTERNA'
  | 'ESTORNO_MOVIMENTACAO'
  | 'ANTECIPACAO_RECEBIVEL'
  | 'LIQUIDACAO_CARTAO'
  | 'LANCAMENTO_MANUAL'
  | 'AJUSTE_CONCILIACAO';

export type ModalidadeRecebivel =
  | 'DIRETO'
  | 'CARTAO_CREDITO'
  | 'CARTAO_DEBITO'
  | 'PIX'
  | 'BOLETO'
  | 'CREDIARIO';

export type StatusConciliacaoFinanceira =
  | 'PENDENTE'
  | 'CONCILIADO_EXATO'
  | 'CONCILIADO_MANUAL'
  | 'DIVERGENTE'
  | 'DUPLICIDADE_SUSPEITA'
  | 'IGNORADO';

export interface ContaFinanceiraRequest {
  codigo: string;
  nome: string;
  tipoConta: TipoContaFinanceira;
  bancoCodigo?: string | null;
  bancoNome?: string | null;
  agencia?: string | null;
  numeroConta?: string | null;
  saldoInicial?: number | null;
  dataSaldoInicial?: string | null;
  padrao?: boolean | null;
  ativo?: boolean | null;
}

export interface ContaFinanceiraResponse {
  idContaFinanceira: number;
  empresaId: number;
  filialId: number;
  filialNome?: string | null;
  codigo: string;
  nome: string;
  tipoConta: TipoContaFinanceira | string;
  bancoCodigo?: string | null;
  bancoNome?: string | null;
  agencia?: string | null;
  numeroConta?: string | null;
  saldoInicial: number;
  saldoAtual: number;
  dataSaldoInicial?: string | null;
  padrao: boolean;
  ativo: boolean;
  criadoEm?: string | null;
  atualizadoEm?: string | null;
}

export interface TransferenciaInternaRequest {
  contaOrigemId: number;
  contaDestinoId: number;
  valor: number;
  dataTransferencia?: string | null;
  descricao?: string | null;
  documentoReferencia?: string | null;
  idempotencyKey?: string | null;
}

export interface EstornoMovimentacaoRequest {
  motivo: string;
  dataEstorno?: string | null;
}

export interface BaixaTituloFinanceiroRequest {
  valorLiquidado: number;
  contaFinanceiraId?: number | null;
  dataLiquidacao?: string | null;
  valorTaxa?: number | null;
  taxaAntecipacao?: number | null;
  valorJurosDesconto?: number | null;
  antecipacaoRecebivel?: boolean | null;
  observacao?: string | null;
  idempotencyKey?: string | null;
}

export interface GerarParcelasRequest {
  natureza: 'PAGAR' | 'RECEBER';
  descricao: string;
  valorTotal: number;
  quantidadeParcelas: number;
  intervaloDias?: number | null;
  primeiroVencimento: string;
  dataCompetencia?: string | null;
  vendaId?: number | null;
  clienteId?: number | null;
  fornecedorId?: number | null;
  categoriaId?: number | null;
  formaPagamentoId?: number | null;
  planoContaId?: number | null;
  centroCustoId?: number | null;
  contaFinanceiraId?: number | null;
  modalidadeRecebivel?: ModalidadeRecebivel | string | null;
  taxaPercentualCartao?: number | null;
  prazoRepasseDias?: number | null;
  recorrente?: boolean | null;
  periodicidade?: 'MENSAL' | 'QUINZENAL' | 'SEMANAL' | 'ANUAL' | string | null;
}

export interface MovimentacaoFinanceiraResponse {
  idMovimentacaoFinanceira: number;
  empresaId: number;
  filialId: number;
  contaFinanceiraId?: number | null;
  contaFinanceiraNome: string;
  tipoMovimentacao: TipoMovimentacaoFinanceira | string;
  origemTipo: OrigemMovimentacaoFinanceira | string;
  origemId?: number | null;
  descricao: string;
  documentoReferencia?: string | null;
  dataMovimentacao: string;
  dataHoraRegistro?: string | null;
  valorBruto: number;
  valorTaxa: number;
  valorJurosDesconto: number;
  valorLiquido: number;
  planoContaId?: number | null;
  planoContaNome?: string | null;
  centroCustoId?: number | null;
  centroCustoNome?: string | null;
  transferenciaParId?: number | null;
  movimentacaoEstornadaId?: number | null;
  estornado: boolean;
  conciliado: boolean;
  conciliacaoItemId?: number | null;
  criadoPor?: string | null;
}

export interface FluxoCaixaPrevistoItem {
  origemTipo: 'CONTA_RECEBER' | 'REPASSE_CARTAO' | 'CONTA_PAGAR' | 'DESPESA' | 'OBRIGACAO_RECORRENTE' | string;
  registroId: number;
  natureza: 'ENTRADA' | 'SAIDA' | string;
  descricao: string;
  parteRelacionada?: string | null;
  dataCompetencia?: string | null;
  dataVencimento: string;
  dataPrevistaLiquidacao: string;
  numeroParcela: number;
  totalParcelas: number;
  modalidade: ModalidadeRecebivel | string;
  situacaoVencimento: 'A_VENCER' | 'VENCE_HOJE' | 'VENCIDO' | string;
  diasAtrasoOuPrazo: number;
  valorBruto: number;
  valorPagoAcumulado: number;
  valorTaxaPrevista: number;
  saldoAbertoLiquido: number;
  recorrente: boolean;
  antecipado: boolean;
  planoContaId?: number | null;
  planoContaNome?: string | null;
  centroCustoId?: number | null;
  centroCustoNome?: string | null;
  filialId: number;
}

export interface FluxoCaixaDiaCalendario {
  data: string;
  tipoDado: 'REALIZADO' | 'HOJE_MISTO' | 'PREVISTO_PROJETADO' | 'PROJETADO' | 'SIMULADO' | string;
  entradasRealizadas: number;
  saidasRealizadas: number;
  entradasPrevistas: number;
  saidasPrevistas: number;
  saldoLiquidoDia: number;
  saldoAcumuladoProjetado: number;
  quantidadeTitulosVencendo: number;
  riscoSaldoNegativo: boolean;
}

export interface FluxoCaixaCenarioProjecao {
  codigoCenario: 'OTIMISTA' | 'REALISTA' | 'PESSIMISTA' | string;
  nomeCenario: string;
  premissaExplicacao: string;
  percentualRealizacaoRecebiveis: number;
  percentualVariacaoSaidas: number;
  saldoAtualPartida: number;
  saldoProjetado7Dias: number;
  saldoProjetado15Dias: number;
  saldoProjetado30Dias: number;
  saldoProjetado60Dias: number;
  saldoProjetado90Dias: number;
  menorSaldoProjetado: number;
  dataMenorSaldo?: string | null;
  riscoInsuficienciaCaixa: boolean;
  necessidadeCapitalGiroAdicional: number;
  serieHorizonte?: FluxoCaixaDiaCalendario[];
}

export interface ItemSimulacaoManual {
  descricao: string;
  natureza: 'ENTRADA' | 'SAIDA' | string;
  valor: number;
  dataPrevista?: string | null;
}

export interface FluxoCaixaSimulacaoRequest {
  horizonteDias?: number | null;
  entradaSimuladaAdicional?: number | null;
  saidaSimuladaAdicional?: number | null;
  investimentoPlanejado?: number | null;
  dataSimulacao?: string | null;
  simularAntecipacaoCartao?: boolean | null;
  taxaAntecipacaoSimuladaPercentual?: number | null;
  itensSimulados?: ItemSimulacaoManual[] | null;
}

export interface DiagnosticoLiquidez {
  nivelSaudeLiquidez: 'SAUDAVEL' | 'ATENCAO' | 'CRITICO' | string;
  mancheteExecutiva: string;
  disponibilidadeImediataTotal: number;
  entradasPrevistas30Dias: number;
  obrigacoesPrevistas30Dias: number;
  obrigacoesVencidasTotais: number;
  recebiveisInadimplentesTotais: number;
  taxaInadimplenciaPercentual: number;
  comprometimentoFornecedoresTotal: number;
  repassesCartaoPendentesBruto: number;
  taxasCartaoPrevistas: number;
  repassesCartaoPendentesLiquido: number;
  dependenciaAntecipacaoPercentual: number;
  geracaoCaixaOperacionalPeriodo: number;
  resultadoEconomicoDrePeriodo: number;
  diferencaDreVsCaixaOperacional: number;
  explicacaoDreVsCaixa: string;
  riscoInsuficienciaCaixa: boolean;
  capacidadeSustentarOperacao: boolean;
  disponibilidadeParaInvestimento: boolean;
  margemSegurancaInvestimento: number;
  contasComprometendoCaixa: string[];
  alertasRiscoLiquidez: string[];
  recomendacoesGerenciais: string[];
}

export interface ConciliacaoItemResponse {
  idConciliacaoItem: number;
  contaFinanceiraId?: number | null;
  contaFinanceiraNome: string;
  tipoConciliacao: string;
  formatoOrigem: string;
  identificadorExtrato?: string | null;
  dataExtrato: string;
  descricaoExtrato: string;
  valorExtrato: number;
  naturezaExtrato: 'CREDITO' | 'DEBITO' | string;
  statusConciliacao: StatusConciliacaoFinanceira | string;
  movimentacaoVinculadaId?: number | null;
  documentoOrigemTipo?: string | null;
  documentoOrigemId?: number | null;
  diferencaValor: number;
  observacao?: string | null;
  conciliadoPor?: string | null;
  conciliadoEm?: string | null;
}

export interface ConciliacaoResumoResponse {
  totalItensExtrato: number;
  conciliadosExatos: number;
  conciliadosManuais: number;
  pendentes: number;
  divergentes: number;
  duplicidadesSuspeitas: number;
  valorTotalDivergente: number;
  sessoesCaixaComDiferencaFechamento: number;
  diferencaTotalFechamentoCaixa: number;
  itens: ConciliacaoItemResponse[];
}

export interface FluxoCaixaPainelResponse {
  dataInicio: string;
  dataFim: string;
  empresaId: number;
  filialId?: number | null;
  filialNome: string;
  consolidadoEmpresa: boolean;
  contaFinanceiraFiltroId?: number | null;
  centroCustoFiltroId?: number | null;
  saldoInicialRealizado: number;
  entradasRealizadasPeriodo: number;
  saidasRealizadasPeriodo: number;
  geracaoLiquidaCaixaRealizado: number;
  saldoFinalRealizado: number;
  transferenciasInternasVolume: number;
  totalTaxasCartaoDescontadas: number;
  totalEstornosPeriodo: number;
  entradasPrevistasPendentes: number;
  entradasPrevistasVencidas: number;
  saidasPrevistasPendentes: number;
  saidasPrevistasVencidas: number;
  saldoPrevistoLiquido30Dias: number;
  contasFinanceiras: ContaFinanceiraResponse[];
  movimentacoesRealizadas: MovimentacaoFinanceiraResponse[];
  itensPrevistos: FluxoCaixaPrevistoItem[];
  calendarioFinanceiro: FluxoCaixaDiaCalendario[];
  cenariosProjecao: FluxoCaixaCenarioProjecao[];
  diagnosticoLiquidez: DiagnosticoLiquidez;
  resumoConciliacao: ConciliacaoResumoResponse;
}

export interface LinhaExtratoManual {
  identificadorExtrato?: string | null;
  dataExtrato: string;
  descricaoExtrato: string;
  valorExtrato: number;
  naturezaExtrato: 'CREDITO' | 'DEBITO' | string;
}

export interface ImportacaoExtratoRequest {
  contaFinanceiraId: number;
  tipoConciliacao: 'BANCARIA' | 'CAIXA' | 'CARTAO' | string;
  formatoOrigem: 'OFX' | 'CSV' | 'MANUAL' | string;
  conteudoArquivo?: string | null;
  linhas?: LinhaExtratoManual[] | null;
}

export interface ConciliacaoManualRequest {
  conciliacaoItemId: number;
  movimentacaoVinculadaId?: number | null;
  documentoOrigemTipo?: string | null;
  documentoOrigemId?: number | null;
  novoStatus?: StatusConciliacaoFinanceira | string | null;
  observacao?: string | null;
}

export interface FluxoCaixaQueryParams {
  dataInicio?: string;
  dataFim?: string;
  contaFinanceiraId?: number | null;
  centroCustoId?: number | null;
  filialFiltroId?: number | null;
  consolidarFiliais?: boolean;
}

// ============================================================================
// Lote 4: Cálculos Administrativos, Motor de Indicadores e Inteligência Financeira
// ============================================================================

export type StatusConfiabilidadeIndicador =
  | 'CALCULADO'
  | 'DADOS_INSUFICIENTES'
  | 'DIVISAO_POR_ZERO'
  | 'DADOS_DESATUALIZADOS'
  | 'SIMULADO_OU_PROJETADO'
  | 'INDISPONIVEL';

export interface IndicadorDetalhado {
  codigo: string;
  nome: string;
  categoriaIndicador:
    | 'RENTABILIDADE'
    | 'PONTO_EQUILIBRIO'
    | 'PRECIFICACAO'
    | 'CAPITAL_GIRO'
    | 'PRAZOS_CICLOS'
    | 'LIQUIDEZ'
    | 'ENDIVIDAMENTO'
    | 'INVESTIMENTO'
    | string;
  prioridade: 'P0' | 'P1' | 'P2' | string;
  descricao: string;
  formula: string;
  variaveis: Record<string, number | null>;
  resultado: number | null;
  unidadeMedida: 'R$' | '%' | 'DIAS' | 'INDICE' | 'UNIDADES' | string;
  periodo: string;
  fonteDados: string;
  dataAtualizacao: string;
  interpretacaoGerencial: string;
  limitacoes: string;
  statusConfiabilidade: StatusConfiabilidadeIndicador | string;
  dependenciasFaltantes: string[];
}

export interface MargemContribuicaoItem {
  tipoAgrupamento: 'PRODUTO' | 'CATEGORIA' | 'FILIAL' | 'OPERACAO' | string;
  referenciaId: number;
  codigo: string;
  nome: string;
  categoriaNome: string;
  quantidadeVendidaLiquida: number;
  receitaBruta: number;
  deducoesTributosDescontos: number;
  receitaLiquida: number;
  cmvTotal: number;
  taxasCartaoAlocadas: number;
  comissoesFretesVariaveis: number;
  outrasDespesasVariaveis: number;
  custosDespesasVariaveisTotais: number;
  margemContribuicaoValor: number;
  margemContribuicaoPercentual: number;
  margemContribuicaoUnitariaMedia: number;
  participacaoFaturamentoPercentual: number;
  participacaoMargemTotalPercentual: number;
  classificacaoRentabilidade:
    | 'ESTRELA_ALTA_CONTRIBUICAO'
    | 'VOLUME_ALTO_BAIXA_MARGEM'
    | 'MARGEM_SAUDAVEL_BAIXO_VOLUME'
    | 'MARGEM_CRITICA_OU_NEGATIVA'
    | 'SEM_MOVIMENTACAO'
    | string;
  recomendacaoItem: string;
}

export interface RentabilidadePainel {
  receitaBrutaTotal: number;
  deducoesTributosVendasTotal: number;
  receitaLiquidaTotal: number;
  cmvTotal: number;
  despesasVariaveisDre: number;
  taxasCartaoAntecipacaoPeriodo: number;
  custosDespesasVariaveisTotais: number;
  margemContribuicaoTotalValor: number;
  margemContribuicaoTotalPercentual: number;
  custosDespesasFixasTotais: number;
  resultadoOperacionalDre: number;
  resultadoLiquidoDre: number;
  margemLiquidaPercentual: number;
  porProduto: MargemContribuicaoItem[];
  porCategoria: MargemContribuicaoItem[];
  porFilial: MargemContribuicaoItem[];
  alertasAltoVolumeBaixaMargem: MargemContribuicaoItem[];
}

export interface PontoEquilibrioFilialCategoria {
  tipoEscopo: 'FILIAL' | 'CATEGORIA' | string;
  escopoId: number;
  escopoNome: string;
  receitaLiquidaAtual: number;
  margemContribuicaoValor: number;
  indiceMargemContribuicaoPercentual: number;
  custosDespesasFixasAlocadas: number;
  pontoEquilibrioContabilReceita: number | null;
  pontoEquilibrioFinanceiroReceita: number | null;
  pontoEquilibrioEconomicoReceita: number | null;
  margemSegurancaPercentual: number | null;
  situacaoEquilibrio: 'ACIMA_DO_EQUILIBRIO' | 'NO_EQUILIBRIO' | 'ABAIXO_DO_EQUILIBRIO' | 'DADOS_INSUFICIENTES' | string;
}

export interface PontoEquilibrioPainel {
  receitaLiquidaAtual: number;
  margemContribuicaoTotal: number;
  indiceMargemContribuicaoDecimal: number | null;
  indiceMargemContribuicaoPercentual: number;
  quantidadeTotalVendidaMix: number;
  precoMedioPonderadoMix: number | null;
  margemContribuicaoUnitariaMediaMix: number | null;
  custosDespesasFixasContabeis: number;
  despesasNaoDesembolsaveisDepreciacao: number;
  amortizacaoPrincipalDividasPeriodo: number;
  desembolsosFixosFinanceiros: number;
  capitalInvestidoBase: number;
  taxaRetornoMinimoPeriodoPercentual: number;
  remuneracaoMinimaCapitalValor: number;
  pontoEquilibrioContabilReceita: number | null;
  pontoEquilibrioContabilUnidadesMix: number | null;
  pontoEquilibrioFinanceiroReceita: number | null;
  pontoEquilibrioFinanceiroUnidadesMix: number | null;
  pontoEquilibrioEconomicoReceita: number | null;
  pontoEquilibrioEconomicoUnidadesMix: number | null;
  margemSegurancaContabilValor: number | null;
  margemSegurancaContabilPercentual: number | null;
  grauAlavancagemOperacional: number | null;
  metodologiaExplicita: string;
  statusConfiabilidade: StatusConfiabilidadeIndicador | string;
  porFilial: PontoEquilibrioFilialCategoria[];
  porCategoria: PontoEquilibrioFilialCategoria[];
}

export interface SimulacaoPrecificacaoRequest {
  produtoId?: number | null;
  custoAquisicaoBase?: number | null;
  freteCompraValor?: number | null;
  seguroCompraValor?: number | null;
  impostosNaoRecuperaveisValor?: number | null;
  despesasAcessoriasValor?: number | null;
  aliquotaTributosVendaPercentual?: number | null;
  taxaCartaoPercentual?: number | null;
  comissaoVendaPercentual?: number | null;
  freteVendaVariavelPercentual?: number | null;
  outrasDespesasVariaveisPercentual?: number | null;
  margemLucroDesejadaPercentual?: number | null;
  precoPraticadoAtual?: number | null;
  descontoPromocionalSimuladoPercentual?: number | null;
  variacaoCustoSimuladaPercentual?: number | null;
}

export interface SimulacaoPrecificacaoResponse {
  produtoId?: number | null;
  produtoDescricao: string;
  custoAquisicaoBase: number;
  custoAquisicaoEfetivoTotal: number;
  percentualDespesasVariaveisTotal: number;
  margemLucroDesejadaPercentual: number;
  markupDivisor: number;
  markupMultiplicador: number;
  precoMinimoEquilibrio: number;
  precoRecomendado: number;
  precoPraticadoAtual: number;
  margemRealPrecoAtualValor: number;
  margemRealPrecoAtualPercentual: number;
  markupRealPrecoAtual: number;
  precoPromocionalSimulado: number;
  margemPromocionalValor: number;
  margemPromocionalPercentual: number;
  aumentoVolumeNecessarioParaMesmoLucroPercentual: number | null;
  custoComReajusteSimulado: number;
  precoRecomendadoAposReajusteCusto: number;
  margemSeMantiverPrecoAtualAposReajustePercentual: number;
  situacaoPrecoAtual:
    | 'ADEQUADO_ACIMA_RECOMENDADO'
    | 'ABAIXO_MARGEM_DESEJADA'
    | 'ABAIXO_PRECO_MINIMO_PREJUIZO'
    | string;
  diferencaMarkupVsMargemExplicacao: string;
  recomendacoesPrecificacao: string[];
  simulacaoReadOnly: boolean;
}

export interface AplicarPrecoProdutoRequest {
  produtoId: number;
  novoValorVenda: number;
  novaMargemPercentual?: number | null;
  justificativa: string;
}

export interface CapitalGiroPainel {
  disponibilidadesCaixaBancos: number;
  contasReceberOperacionais: number;
  estoquesOperacionais: number;
  adiantamentosDespesasAntecipadas: number;
  ativoCirculanteOperacional: number;
  ativoCirculanteTotal: number;
  fornecedoresContasPagarOperacionais: number;
  obrigacoesTributariasTrabalhistasOperacionais: number;
  passivoCirculanteOperacional: number;
  emprestimosFinanciamentosCurtoPrazo: number;
  passivoCirculanteTotal: number;
  capitalGiroLiquidoCgl: number;
  necessidadeCapitalGiroNcg: number;
  saldoTesourariaSt: number;
  ncgPeriodoAnterior: number | null;
  variacaoNcgValor: number | null;
  variacaoNcgPercentual: number | null;
  percentualCapitalPresoEstoque: number;
  percentualCapitalPresoClientes: number;
  percentualFinanciadoFornecedores: number;
  riscoEfeitoTesoura: boolean;
  perfilEstruturaFinanceira:
    | 'SOLIDA_SUPERAVIT_TESOURARIA'
    | 'EQUILIBRADA_FINANCIAMENTO_OPERACIONAL'
    | 'TENSAO_DEPENDENCIA_CAPITAL_TERCEIROS'
    | 'EFEITO_TESOURA_CRITICO'
    | string;
  statusConfiabilidadeCgl: StatusConfiabilidadeIndicador | string;
  statusConfiabilidadeNcg: StatusConfiabilidadeIndicador | string;
  interpretacaoCapitalGiro: string;
  fatoresPressaoCapitalGiro: string[];
}

export interface PrazosCiclosEvolucaoPonto {
  competencia: string;
  pmeDias: number | null;
  pmrDias: number | null;
  pmpDias: number | null;
  cicloOperacionalDias: number | null;
  cicloFinanceiroDias: number | null;
}

export interface PrazosCiclosPainel {
  diasPeriodoAnalisado: number;
  estoqueInicialPeriodo: number;
  estoqueFinalPeriodo: number;
  estoqueMedioPeriodo: number;
  cmvPeriodo: number;
  pmePrazoMedioEstocagemDias: number | null;
  giroEstoqueVezesPeriodo: number | null;
  contasReceberInicialPeriodo: number;
  contasReceberFinalPeriodo: number;
  contasReceberMediaPeriodo: number;
  vendasPrazoPeriodo: number;
  pmrPrazoMedioRecebimentoDias: number | null;
  fornecedoresInicialPeriodo: number;
  fornecedoresFinalPeriodo: number;
  fornecedoresMediaPeriodo: number;
  comprasPrazoPeriodo: number;
  pmpPrazoMedioPagamentoDias: number | null;
  cicloOperacionalDias: number | null;
  cicloFinanceiroDias: number | null;
  pagaFornecedorAntesDeReceber: boolean;
  descompassoCaixaDias: number;
  statusConfiabilidadePme: StatusConfiabilidadeIndicador | string;
  statusConfiabilidadePmr: StatusConfiabilidadeIndicador | string;
  statusConfiabilidadePmp: StatusConfiabilidadeIndicador | string;
  statusConfiabilidadeCicloFinanceiro: StatusConfiabilidadeIndicador | string;
  tendenciaCicloFinanceiro: string;
  interpretacaoPrazosCiclos: string;
  evolucaoMensal: PrazosCiclosEvolucaoPonto[];
}

export interface BalancoPatrimonialGerencialRequest {
  competenciaAnoMes: string;
  dataReferencia?: string | null;
  disponibilidadesCaixaBancos?: number | null;
  contasReceberOperacionais?: number | null;
  estoquesOperacionais?: number | null;
  adiantamentosOperacionais?: number | null;
  despesasAntecipadas?: number | null;
  outrosAtivosCirculantes?: number | null;
  realizavelLongoPrazo?: number | null;
  investimentosLongoPrazo?: number | null;
  imobilizadoBruto?: number | null;
  depreciacaoAcumulada?: number | null;
  intangivel?: number | null;
  fornecedoresOperacionaisCp?: number | null;
  obrigacoesTributariasTrabalhistasCp?: number | null;
  outrasObrigacoesOperacionaisCp?: number | null;
  emprestimosFinanciamentosCp?: number | null;
  emprestimosFinanciamentosLp?: number | null;
  outrasObrigacoesLp?: number | null;
  capitalSocial?: number | null;
  reservasCapitalLucros?: number | null;
  lucrosPrejuizosAcumulados?: number | null;
  depreciacaoAmortizacaoPeriodo?: number | null;
  amortizacaoPrincipalDividaPeriodo?: number | null;
  usarSaldosOperacionaisErp?: boolean | null;
  confirmadoGestor?: boolean | null;
  observacoes?: string | null;
}

export interface BalancoPatrimonialGerencialResponse {
  idBalancoPatrimonial?: number | null;
  empresaId: number;
  filialId: number;
  competenciaAnoMes: string;
  dataReferencia: string;
  disponibilidadesCaixaBancos: number;
  contasReceberOperacionais: number;
  estoquesOperacionais: number;
  adiantamentosOperacionais: number;
  despesasAntecipadas: number;
  outrosAtivosCirculantes: number;
  ativoCirculanteTotal: number;
  realizavelLongoPrazo: number;
  investimentosLongoPrazo: number;
  imobilizadoBruto: number;
  depreciacaoAcumulada: number;
  imobilizadoLiquido: number;
  intangivel: number;
  ativoNaoCirculanteTotal: number;
  ativoTotal: number;
  fornecedoresOperacionaisCp: number;
  obrigacoesTributariasTrabalhistasCp: number;
  outrasObrigacoesOperacionaisCp: number;
  emprestimosFinanciamentosCp: number;
  passivoCirculanteTotal: number;
  emprestimosFinanciamentosLp: number;
  outrasObrigacoesLp: number;
  passivoNaoCirculanteTotal: number;
  passivoExigivelTotal: number;
  capitalSocial: number;
  reservasCapitalLucros: number;
  lucrosPrejuizosAcumulados: number;
  patrimonioLiquidoTotal: number;
  passivoMaisPatrimonioLiquido: number;
  diferencaEquilibrioPatrimonial: number;
  depreciacaoAmortizacaoPeriodo: number;
  amortizacaoPrincipalDividaPeriodo: number;
  sincronizadoOperacional: boolean;
  confirmadoGestor: boolean;
  consistenteContabilmente: boolean;
  observacoes?: string | null;
  atualizadoPor?: string | null;
  atualizadoEm?: string | null;
}

export interface LiquidezEndividamentoPainel {
  balancoPatrimonialConfirmado: boolean;
  competenciaBalancoUtilizado?: string | null;
  disponibilidadesImediatas: number;
  ativoCirculante: number;
  estoques: number;
  despesasAntecipadas: number;
  realizavelLongoPrazo: number | null;
  ativoNaoCirculanteTotal: number | null;
  ativoTotal: number | null;
  passivoCirculante: number;
  passivoNaoCirculante: number | null;
  passivoExigivelTotal: number | null;
  dividaFinanceiraTotal: number | null;
  patrimonioLiquido: number | null;
  liquidezImediata: number | null;
  liquidezSeca: number | null;
  liquidezCorrente: number | null;
  liquidezGeral: number | null;
  endividamentoGeralPercentual: number | null;
  participacaoCapitalTerceirosPercentual: number | null;
  composicaoEndividamentoCurtoPrazoPercentual: number | null;
  relacaoDividaPatrimonioLiquidoPercentual: number | null;
  coberturaObrigacoesCurtoPrazo: number | null;
  statusConfiabilidadeLiquidezImediata: StatusConfiabilidadeIndicador | string;
  statusConfiabilidadePatrimonial: StatusConfiabilidadeIndicador | string;
  dependenciasPatrimoniaisFaltantes: string[];
  interpretacaoLiquidezEndividamento: string;
}

export interface SimulacaoInvestimentoRequest {
  nomeProjeto?: string | null;
  investimentoInicial: number;
  ganhoEsperadoTotal?: number | null;
  taxaDescontoPeriodicaPercentual?: number | null;
  fluxosCaixaProjetados?: number[] | null;
}

export interface InvestimentosRetornoPainel {
  resultadoOperacionalPeriodo: number;
  resultadoLiquidoPeriodo: number;
  ativoTotalMedio: number | null;
  patrimonioLiquidoMedio: number | null;
  capitalInvestidoOperacional: number | null;
  roaRetornoSobreAtivoPercentual: number | null;
  roeRetornoSobrePatrimonioPercentual: number | null;
  roicRetornoSobreCapitalInvestidoPercentual: number | null;
  roiSimuladoPercentual: number | null;
  paybackSimplesPeriodos: number | null;
  paybackDescontadoPeriodos: number | null;
  vplValorPresenteLiquido: number | null;
  tirTaxaInternaRetornoPercentual: number | null;
  investimentoViavelEconomicamente: boolean;
  capacidadeFinanceiraCaixaParaInvestir: boolean;
  disponibilidadeCaixaAposReserva: number;
  statusConfiabilidadeRoaRoe: StatusConfiabilidadeIndicador | string;
  statusConfiabilidadeSimulacao: StatusConfiabilidadeIndicador | string;
  separacaoContabilVsCaixaExplicacao: string;
  alertasInvestimento: string[];
}

export interface DiagnosticoAdministrativoItem {
  codigoDiagnostico: string;
  severidade: 'CRITICO' | 'ALERTA' | 'OPORTUNIDADE' | 'SAUDAVEL' | string;
  indicadorRelacionado: string;
  resultadoFormatado: string;
  periodo: string;
  fonteDados: string;
  interpretacao: string;
  possivelConsequencia: string;
  recomendacaoGerencial: string;
  grauConfiabilidade: StatusConfiabilidadeIndicador | string;
}

export interface ParametroInteligenciaAdministrativa {
  idParametroInteligencia?: number | null;
  empresaId?: number | null;
  filialId?: number | null;
  segmentoNegocio: string;
  retornoMinimoMensalPercentual: number;
  capitalInvestidoReferencia: number;
  taxaDescontoAnualPercentual: number;
  margemContribuicaoMinimaPercentual: number;
  margemLiquidaMinimaPercentual: number;
  markupMargemDesejadaPadrao: number;
  aliquotaTributosVendaPadrao: number;
  taxaCartaoMediaPadrao: number;
  comissaoVendaPadrao: number;
  freteVariavelVendaPadrao: number;
  cicloFinanceiroMaximoDias: number;
  pmeMaximoDias: number;
  liquidezCorrenteMinima: number;
  liquidezSecaMinima: number;
  liquidezImediataMinima: number;
  endividamentoGeralMaximoPercentual: number;
  atualizadoPor?: string | null;
  atualizadoEm?: string | null;
}

export interface PainelInteligenciaAdministrativaResponse {
  competencia: string;
  dataInicio: string;
  dataFim: string;
  empresaId: number;
  filialId?: number | null;
  filialNome: string;
  consolidadoEmpresa: boolean;
  geradoEm: string;
  parametrosVigentes: ParametroInteligenciaAdministrativa;
  balancoPatrimonial: BalancoPatrimonialGerencialResponse;
  rentabilidade: RentabilidadePainel;
  pontoEquilibrio: PontoEquilibrioPainel;
  precificacaoPadrao: SimulacaoPrecificacaoResponse;
  capitalGiro: CapitalGiroPainel;
  prazosCiclos: PrazosCiclosPainel;
  liquidezEndividamento: LiquidezEndividamentoPainel;
  investimentosRetorno: InvestimentosRetornoPainel;
  catalogoIndicadores: IndicadorDetalhado[];
  diagnosticosExplicaveis: DiagnosticoAdministrativoItem[];
}

export interface IndicadoresQueryParams {
  competencia?: string;
  dataInicio?: string;
  dataFim?: string;
  filialId?: number | null;
  consolidarFiliais?: boolean;
}


