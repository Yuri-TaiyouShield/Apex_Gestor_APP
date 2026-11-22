import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of, shareReplay, switchMap, tap, throwError } from 'rxjs';

import { ApiConfigService } from './api-config.service';
import { ApiReadinessService } from './api-readiness.service';
import { AuthService } from './auth.service';
import {
  AdmCalcRequest,
  AplicarPrecoProdutoRequest,
  B2cCartMergeRequest,
  B2cCartMergeResponse,
  BaixaTituloFinanceiroRequest,
  BalancoPatrimonialGerencialRequest,
  BalancoPatrimonialGerencialResponse,
  CaixaSessao,
  CentroCustoBootstrapResponse,
  CentroCustoRequest,
  CentroCustoResponse,
  ConciliacaoItemResponse,
  ConciliacaoManualRequest,
  ConciliacaoResumoResponse,
  ConsolidacaoCompetenciaResponse,
  ContaFinanceiraRequest,
  ContaFinanceiraResponse,
  ContaPagar,
  ContaReceber,
  CreditoCliente,
  DashboardVendedor,
  CheckoutPayment,
  Cliente,
  CondicaoPagamento,
  Despesa,
  DreComparativoFiliaisResponse,
  DreDrillDownResponse,
  DreEvolucaoResponse,
  DreGerencialResponse,
  DreQueryParams,
  EstornoMovimentacaoRequest,
  FinancialAuditEvent,
  FinancialCalculationResponse,
  FinancialDocument,
  FinancialDocumentRequest,
  FluxoCaixaCenarioProjecao,
  FluxoCaixaPainelResponse,
  FluxoCaixaPrevistoItem,
  FluxoCaixaQueryParams,
  FluxoCaixaSimulacaoRequest,
  FormaPagamento,
  GerarParcelasRequest,
  Identifiable,
  ImportacaoExtratoRequest,
  IndicadoresQueryParams,
  InvestimentosRetornoPainel,
  LaborCalculationRequest,
  MovimentacaoFinanceiraResponse,
  OrigemClassificacaoFinanceira,
  PainelInteligenciaAdministrativaResponse,
  ParametroInteligenciaAdministrativa,
  PendenciaClassificacaoResponse,
  PlanoContasBootstrapResponse,
  PlanoContasRequest,
  PlanoContasResponse,
  RelatorioOperacional,
  PricingCalculationRequest,
  PricingCalculationResponse,
  Produto,
  ReclassificacaoManualRequest,
  RegraClassificacaoRequest,
  RegraClassificacaoResponse,
  RelatorioFinanceiro,
  RecebimentoPagamento,
  ResultadoClassificacaoLote,
  SignFinancialDocumentRequest,
  SimulacaoInvestimentoRequest,
  SimulacaoPrecificacaoRequest,
  SimulacaoPrecificacaoResponse,
  TaxCalculationRequest,
  TipoDespesa,
  TransferenciaInternaRequest,
  Usuario,
  Venda,
  VendaDevolucaoRequest
} from './models';

@Injectable({ providedIn: 'root' })
export class ApexApiService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(ApiConfigService);
  private readonly readiness = inject(ApiReadinessService);
  private readonly auth = inject(AuthService);
  private readonly listCache = new Map<string, Observable<unknown>>();

  constructor() {
    window.addEventListener('apex-auth-cleared', () => this.listCache.clear());
  }

  list<T>(endpoint: string, options: { refresh?: boolean } = {}): Observable<T[]> {
    const cacheKey = this.cacheKey(endpoint);
    if (options.refresh) {
      this.listCache.delete(cacheKey);
    }
    const cached = this.listCache.get(cacheKey) as Observable<T[]> | undefined;
    if (cached) {
      return cached;
    }

    const request = this.readiness.waitForApi().pipe(
      switchMap(() => this.http.get<T[]>(this.config.apiUrl(endpoint))),
      catchError((error) => {
        this.listCache.delete(cacheKey);
        return throwError(() => error);
      }),
      shareReplay({ bufferSize: 1, refCount: false })
    );
    this.listCache.set(cacheKey, request);
    return request;
  }

  create<T>(endpoint: string, payload: Partial<T>): Observable<T> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post<T>(this.config.apiUrl(endpoint), payload)),
      tap(() => this.invalidate(endpoint))
    );
  }

  update<T>(endpoint: string, id: number | string, payload: Partial<T>): Observable<T> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.put<T>(this.config.apiUrl(`${endpoint}/${id}`), payload)),
      tap(() => this.invalidate(endpoint))
    );
  }

  patch<T>(path: string, payload: object = {}): Observable<T> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.patch<T>(this.config.apiUrl(path), payload)),
      tap(() => this.invalidate(path))
    );
  }

  products(): Observable<Produto[]> {
    return this.list<Produto>('/api/produtos');
  }

  clients(): Observable<Cliente[]> {
    return this.list<Cliente>('/api/clientes');
  }

  users(): Observable<Usuario[]> {
    return this.list<Usuario>('/api/usuarios');
  }

  paymentMethods(): Observable<FormaPagamento[]> {
    return this.list<FormaPagamento>('/api/formaspagamento');
  }

  paymentConditions(): Observable<CondicaoPagamento[]> {
    return this.list<CondicaoPagamento>('/api/condicoes-pagamento');
  }

  currentCashier(): Observable<CaixaSessao | null> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.get<CaixaSessao>(this.config.apiUrl('/api/caixa/atual'))),
      catchError((error) => {
        if (error?.status === 204 || error?.status === 404) {
          return of(null);
        }
        return throwError(() => error);
      })
    );
  }

  openCashier(valor: number): Observable<CaixaSessao> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post<CaixaSessao>(this.config.apiUrl('/api/caixa/abrir'), {
        valor,
        observacao: 'Abertura de caixa pelo Apex Gestor'
      })),
      tap(() => this.invalidate('/api/caixa'))
    );
  }

  cashierSupply(id: number, valor: number, observacao = 'Suprimento operacional'): Observable<CaixaSessao> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post<CaixaSessao>(this.config.apiUrl(`/api/caixa/${id}/suprimento`), { valor, observacao })),
      tap(() => this.invalidate('/api/caixa'))
    );
  }

  cashierWithdraw(id: number, valor: number, observacao = 'Sangria operacional'): Observable<CaixaSessao> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post<CaixaSessao>(this.config.apiUrl(`/api/caixa/${id}/sangria`), { valor, observacao })),
      tap(() => this.invalidate('/api/caixa'))
    );
  }

  closeCashier(id: number, saldoInformado?: number): Observable<CaixaSessao> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post<CaixaSessao>(this.config.apiUrl(`/api/caixa/${id}/fechar`), { valor: saldoInformado })),
      tap(() => this.invalidate('/api/caixa'))
    );
  }

  pendingCashierSales(): Observable<Venda[]> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.get<Venda[]>(this.config.apiUrl('/api/caixa/vendas-pendentes')))
    );
  }

  receiveCashierSale(id: number, pagamentos: RecebimentoPagamento[], idempotencyKey = this.newIdempotencyKey()): Observable<Venda> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post<Venda>(this.config.apiUrl(`/api/caixa/vendas/${id}/receber`), { pagamentos }, {
        headers: new HttpHeaders({ 'Idempotency-Key': idempotencyKey })
      })),
      tap(() => this.invalidateOperationalSaleData())
    );
  }

  cashierCredits(clienteId: number): Observable<CreditoCliente[]> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.get<CreditoCliente[]>(this.config.apiUrl(`/api/caixa/clientes/${clienteId}/creditos`)))
    );
  }

  cashierHistory(): Observable<CaixaSessao[]> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.get<CaixaSessao[]>(this.config.apiUrl('/api/caixa/historico')))
    );
  }

  accountsReceivable(): Observable<ContaReceber[]> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.get<ContaReceber[]>(this.config.apiUrl('/api/financeiro/operacional/contas-receber')))
    );
  }

  accountsPayable(): Observable<ContaPagar[]> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.get<ContaPagar[]>(this.config.apiUrl('/api/financeiro/operacional/contas-pagar')))
    );
  }

  createAccountPayable(payload: Partial<ContaPagar>): Observable<ContaPagar> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post<ContaPagar>(this.config.apiUrl('/api/financeiro/operacional/contas-pagar'), payload)),
      tap(() => this.invalidate('/api/financeiro'))
    );
  }

  rejectCashierSale(id: number, motivo: string): Observable<Venda> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post<Venda>(this.config.apiUrl(`/api/caixa/vendas/${id}/recusar`), { motivo })),
      tap(() => this.invalidateOperationalSaleData())
    );
  }

  sellerDashboard(): Observable<DashboardVendedor> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.get<DashboardVendedor>(this.config.apiUrl('/api/dashboard/vendedor')))
    );
  }

  salesHistory(q = '', status = ''): Observable<Venda[]> {
    let params = new HttpParams();
    if (q.trim()) params = params.set('q', q.trim());
    if (status) params = params.set('status', status);
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.get<Venda[]>(this.config.apiUrl('/api/vendas/historico'), { params }))
    );
  }

  returnSale(id: number, payload: VendaDevolucaoRequest): Observable<unknown> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post(this.config.apiUrl(`/api/vendas/${id}/devolucoes`), payload)),
      tap(() => this.invalidateOperationalSaleData())
    );
  }

  expenses(): Observable<Despesa[]> {
    return this.list<Despesa>('/api/despesas');
  }

  expenseTypes(): Observable<TipoDespesa[]> {
    return this.list<TipoDespesa>('/api/despesas/tipos');
  }

  listTiposDespesa(options: { refresh?: boolean } = {}): Observable<TipoDespesa[]> {
    return this.list<TipoDespesa>('/api/despesas/tipos', options);
  }

  createTipoDespesa(payload: Partial<TipoDespesa>): Observable<TipoDespesa> {
    return this.create<TipoDespesa>('/api/despesas/tipos', payload);
  }

  updateTipoDespesa(id: number, payload: Partial<TipoDespesa>): Observable<TipoDespesa> {
    return this.update<TipoDespesa>('/api/despesas/tipos', id, payload);
  }

  pagarDespesa(id: number, dataPagamento?: string): Observable<Despesa> {
    return this.patch<Despesa>(
      `/api/despesas/${id}/pagar`,
      dataPagamento ? { dataPagamento } : {}
    ).pipe(tap(() => this.invalidate('/api/financeiro')));
  }

  // --- Lote 1: Fundação Financeira ---

  listPlanoContas(somenteAtivos?: boolean): Observable<PlanoContasResponse[]> {
    const params = this.buildParams({ somenteAtivos });
    return this.getWithOptionalParams<PlanoContasResponse[]>('/api/financeiro/plano-contas', params);
  }

  getPlanoContasTree(somenteAtivos?: boolean): Observable<PlanoContasResponse[]> {
    const params = this.buildParams({ somenteAtivos });
    return this.getWithOptionalParams<PlanoContasResponse[]>('/api/financeiro/plano-contas/arvore', params);
  }

  bootstrapPlanoContasPadrao(): Observable<PlanoContasBootstrapResponse> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post<PlanoContasBootstrapResponse>(this.config.apiUrl('/api/financeiro/plano-contas/inicializar-padrao'), {})),
      tap(() => this.invalidate('/api/financeiro'))
    );
  }

  createPlanoConta(payload: PlanoContasRequest): Observable<PlanoContasResponse> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post<PlanoContasResponse>(this.config.apiUrl('/api/financeiro/plano-contas'), payload)),
      tap(() => this.invalidate('/api/financeiro'))
    );
  }

  updatePlanoConta(id: number, payload: PlanoContasRequest): Observable<PlanoContasResponse> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.put<PlanoContasResponse>(this.config.apiUrl(`/api/financeiro/plano-contas/${id}`), payload)),
      tap(() => this.invalidate('/api/financeiro'))
    );
  }

  togglePlanoContaStatus(id: number, ativo: boolean): Observable<PlanoContasResponse> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.patch<PlanoContasResponse>(this.config.apiUrl(`/api/financeiro/plano-contas/${id}/status?ativo=${ativo}`), {})),
      tap(() => this.invalidate('/api/financeiro'))
    );
  }

  listCentrosCusto(somenteAtivos?: boolean): Observable<CentroCustoResponse[]> {
    const params = this.buildParams({ somenteAtivos });
    return this.getWithOptionalParams<CentroCustoResponse[]>('/api/financeiro/centros-custo', params);
  }

  bootstrapCentrosCustoPadrao(): Observable<CentroCustoBootstrapResponse | CentroCustoResponse[]> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post<CentroCustoBootstrapResponse | CentroCustoResponse[]>(
        this.config.apiUrl('/api/financeiro/centros-custo/inicializar-padrao'),
        {}
      )),
      tap(() => this.invalidate('/api/financeiro'))
    );
  }

  createCentroCusto(payload: CentroCustoRequest): Observable<CentroCustoResponse> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post<CentroCustoResponse>(this.config.apiUrl('/api/financeiro/centros-custo'), payload)),
      tap(() => this.invalidate('/api/financeiro'))
    );
  }

  updateCentroCusto(id: number, payload: CentroCustoRequest): Observable<CentroCustoResponse> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.put<CentroCustoResponse>(this.config.apiUrl(`/api/financeiro/centros-custo/${id}`), payload)),
      tap(() => this.invalidate('/api/financeiro'))
    );
  }

  toggleCentroCustoStatus(id: number, ativo: boolean): Observable<CentroCustoResponse> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.patch<CentroCustoResponse>(this.config.apiUrl(`/api/financeiro/centros-custo/${id}/status?ativo=${ativo}`), {})),
      tap(() => this.invalidate('/api/financeiro'))
    );
  }

  listRegrasClassificacao(origemTipo?: string, somenteAtivas?: boolean): Observable<RegraClassificacaoResponse[]> {
    const params = this.buildParams({ origemTipo, somenteAtivas });
    return this.getWithOptionalParams<RegraClassificacaoResponse[]>('/api/financeiro/classificacoes/regras', params);
  }

  createRegraClassificacao(payload: RegraClassificacaoRequest): Observable<RegraClassificacaoResponse> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post<RegraClassificacaoResponse>(this.config.apiUrl('/api/financeiro/classificacoes/regras'), payload)),
      tap(() => this.invalidate('/api/financeiro'))
    );
  }

  updateRegraClassificacao(id: number, payload: RegraClassificacaoRequest): Observable<RegraClassificacaoResponse> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.put<RegraClassificacaoResponse>(this.config.apiUrl(`/api/financeiro/classificacoes/regras/${id}`), payload)),
      tap(() => this.invalidate('/api/financeiro'))
    );
  }

  toggleRegraClassificacaoStatus(id: number, ativo: boolean): Observable<RegraClassificacaoResponse> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.patch<RegraClassificacaoResponse>(this.config.apiUrl(`/api/financeiro/classificacoes/regras/${id}/status?ativo=${ativo}`), {})),
      tap(() => this.invalidate('/api/financeiro'))
    );
  }

  getPendenciasClassificacao(origemTipo?: OrigemClassificacaoFinanceira | string): Observable<PendenciaClassificacaoResponse> {
    const params = this.buildParams({ origemTipo });
    return this.getWithOptionalParams<PendenciaClassificacaoResponse>('/api/financeiro/classificacoes/pendentes', params);
  }

  aplicarClassificacaoAutomatica(origemTipo?: string): Observable<ResultadoClassificacaoLote> {
    const params = this.buildParams({ origemTipo });
    return this.postWithOptionalParams<ResultadoClassificacaoLote>(
      '/api/financeiro/classificacoes/aplicar-automatico',
      {},
      params
    ).pipe(
      tap(() => {
        this.invalidate('/api/financeiro');
        this.invalidate('/api/despesas');
      })
    );
  }

  reclassificarManualmente(payload: ReclassificacaoManualRequest): Observable<unknown> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post(this.config.apiUrl('/api/financeiro/classificacoes/manual'), payload)),
      tap(() => {
        this.invalidate('/api/financeiro');
        this.invalidate('/api/despesas');
      })
    );
  }

  getConsolidacaoCompetencia(competencia?: string): Observable<ConsolidacaoCompetenciaResponse> {
    const params = this.buildParams({ competencia });
    return this.getWithOptionalParams<ConsolidacaoCompetenciaResponse>('/api/financeiro/consolidacao/competencia', params);
  }

  recalcularConsolidacaoCompetencia(competencia?: string): Observable<ConsolidacaoCompetenciaResponse> {
    const params = this.buildParams({ competencia });
    return this.postWithOptionalParams<ConsolidacaoCompetenciaResponse>(
      '/api/financeiro/consolidacao/competencia/recalcular',
      {},
      params
    ).pipe(tap(() => this.invalidate('/api/financeiro')));
  }

  financialReport(inicio: string, fim: string): Observable<RelatorioFinanceiro> {
    const params = new HttpParams().set('inicio', inicio).set('fim', fim);
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.get<RelatorioFinanceiro>(this.config.apiUrl('/api/relatorios/financeiro'), { params }))
    );
  }

  operationalReport(kind: 'clientes' | 'estoque' | 'funcionarios' | 'vendas' | 'fornecedores'): Observable<RelatorioOperacional> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.get<RelatorioOperacional>(this.config.apiUrl(`/api/relatorios/${kind}`)))
    );
  }

  laborCalculation(payload: LaborCalculationRequest): Observable<FinancialCalculationResponse> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post<FinancialCalculationResponse>(this.config.apiUrl('/api/financeiro/calculos/trabalhista'), payload))
    );
  }

  taxCalculation(payload: TaxCalculationRequest): Observable<FinancialCalculationResponse> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post<FinancialCalculationResponse>(this.config.apiUrl('/api/financeiro/calculos/tributario'), payload))
    );
  }

  admCalcCalculation(payload: AdmCalcRequest): Observable<FinancialCalculationResponse> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post<FinancialCalculationResponse>(this.config.apiUrl('/api/financeiro/calculos/admcalc'), payload))
    );
  }

  pricingCalculation(payload: PricingCalculationRequest): Observable<PricingCalculationResponse> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post<PricingCalculationResponse>(this.config.apiUrl('/api/financeiro/precificacao'), payload))
    );
  }

  mergeB2cCart(payload: B2cCartMergeRequest): Observable<B2cCartMergeResponse> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post<B2cCartMergeResponse>(this.config.apiUrl('/api/b2c/cart/merge'), payload))
    );
  }

  financialDocuments(): Observable<FinancialDocument[]> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.get<FinancialDocument[]>(this.config.apiUrl('/api/financeiro/documentos')))
    );
  }

  createFinancialDocument(payload: FinancialDocumentRequest): Observable<FinancialDocument> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post<FinancialDocument>(this.config.apiUrl('/api/financeiro/documentos'), payload))
    );
  }

  signFinancialDocument(id: number, payload: SignFinancialDocumentRequest): Observable<FinancialDocument> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post<FinancialDocument>(this.config.apiUrl(`/api/financeiro/documentos/${id}/assinaturas`), payload))
    );
  }

  financialAudit(): Observable<FinancialAuditEvent[]> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.get<FinancialAuditEvent[]>(this.config.apiUrl('/api/financeiro/auditoria')))
    );
  }

  getDreGerencial(query: DreQueryParams = {}): Observable<DreGerencialResponse> {
    const params = this.buildDreParams(query);
    return this.getWithOptionalParams<DreGerencialResponse>('/api/financeiro/dre', params);
  }

  getDreLancamentos(
    query: DreQueryParams & {
      numeroLinha?: number | null;
      codigoLinha?: string | null;
      grupoDre?: string | null;
      planoContaId?: number | null;
    } = {}
  ): Observable<DreDrillDownResponse> {
    const params = this.buildDreParams({
      ...query,
      numeroLinha: query.numeroLinha !== undefined && query.numeroLinha !== null ? String(query.numeroLinha) : undefined,
      codigoLinha: query.codigoLinha ?? undefined,
      grupoDre: query.grupoDre ?? undefined,
      planoContaId: query.planoContaId !== undefined && query.planoContaId !== null ? String(query.planoContaId) : undefined
    });
    return this.getWithOptionalParams<DreDrillDownResponse>('/api/financeiro/dre/lancamentos', params);
  }

  getDreEvolucao(query: {
    competenciaInicio?: string;
    competenciaFim?: string;
    meses?: number;
    centroCustoId?: number | null;
    filialFiltroId?: number | null;
    consolidarFiliais?: boolean;
  } = {}): Observable<DreEvolucaoResponse> {
    const params = this.buildParams({
      competenciaInicio: query.competenciaInicio,
      competenciaFim: query.competenciaFim,
      meses: query.meses !== undefined ? String(query.meses) : undefined,
      centroCustoId: query.centroCustoId !== undefined && query.centroCustoId !== null ? String(query.centroCustoId) : undefined,
      filialFiltroId: query.filialFiltroId !== undefined && query.filialFiltroId !== null ? String(query.filialFiltroId) : undefined,
      consolidarFiliais: query.consolidarFiliais
    });
    return this.getWithOptionalParams<DreEvolucaoResponse>('/api/financeiro/dre/evolucao', params);
  }

  getDreComparativoFiliais(query: DreQueryParams = {}): Observable<DreComparativoFiliaisResponse> {
    const params = this.buildDreParams(query);
    return this.getWithOptionalParams<DreComparativoFiliaisResponse>('/api/financeiro/dre/comparativo-filiais', params);
  }

  exportarDreCsv(query: DreQueryParams = {}): Observable<string> {
    let params = this.buildDreParams(query) ?? new HttpParams();
    params = params.set('formato', 'CSV');
    return this.readiness.waitForApi().pipe(
      switchMap(() =>
        this.http.get(this.config.apiUrl('/api/financeiro/dre/exportar'), {
          params,
          responseType: 'text'
        })
      )
    );
  }

  exportarDreJson(query: DreQueryParams = {}): Observable<DreGerencialResponse> {
    let params = this.buildDreParams(query) ?? new HttpParams();
    params = params.set('formato', 'JSON');
    return this.getWithOptionalParams<DreGerencialResponse>('/api/financeiro/dre/exportar', params);
  }

  getFluxoCaixaPainel(query: FluxoCaixaQueryParams = {}): Observable<FluxoCaixaPainelResponse> {
    const params = this.buildFluxoCaixaParams(query);
    return this.getWithOptionalParams<FluxoCaixaPainelResponse>('/api/financeiro/fluxo-caixa', params);
  }

  simularFluxoCaixa(
    payload: FluxoCaixaSimulacaoRequest,
    query: FluxoCaixaQueryParams = {}
  ): Observable<FluxoCaixaCenarioProjecao[]> {
    const params = this.buildFluxoCaixaParams(query);
    return this.postWithOptionalParams<FluxoCaixaCenarioProjecao[]>(
      '/api/financeiro/fluxo-caixa/simular',
      payload,
      params
    );
  }

  listContasFinanceiras(consolidarFiliais = false): Observable<ContaFinanceiraResponse[]> {
    const params = this.buildParams({
      consolidarFiliais: consolidarFiliais ? true : undefined
    });
    return this.getWithOptionalParams<ContaFinanceiraResponse[]>('/api/financeiro/fluxo-caixa/contas', params);
  }

  bootstrapContasFinanceiras(): Observable<ContaFinanceiraResponse[]> {
    return this.readiness.waitForApi().pipe(
      switchMap(() =>
        this.http.post<ContaFinanceiraResponse[]>(
          this.config.apiUrl('/api/financeiro/fluxo-caixa/contas/bootstrap'),
          {}
        )
      ),
      tap(() => this.invalidate('/api/financeiro'))
    );
  }

  createContaFinanceira(payload: ContaFinanceiraRequest): Observable<ContaFinanceiraResponse> {
    return this.readiness.waitForApi().pipe(
      switchMap(() =>
        this.http.post<ContaFinanceiraResponse>(this.config.apiUrl('/api/financeiro/fluxo-caixa/contas'), payload)
      ),
      tap(() => this.invalidate('/api/financeiro'))
    );
  }

  updateContaFinanceira(id: number, payload: ContaFinanceiraRequest): Observable<ContaFinanceiraResponse> {
    return this.readiness.waitForApi().pipe(
      switchMap(() =>
        this.http.put<ContaFinanceiraResponse>(
          this.config.apiUrl(`/api/financeiro/fluxo-caixa/contas/${id}`),
          payload
        )
      ),
      tap(() => this.invalidate('/api/financeiro'))
    );
  }

  gerarParcelasFluxoCaixa(
    payload: GerarParcelasRequest,
    idempotencyKey = this.newIdempotencyKey()
  ): Observable<FluxoCaixaPrevistoItem[]> {
    return this.readiness.waitForApi().pipe(
      switchMap(() =>
        this.http.post<FluxoCaixaPrevistoItem[]>(
          this.config.apiUrl('/api/financeiro/fluxo-caixa/parcelas/gerar'),
          payload,
          { headers: new HttpHeaders({ 'Idempotency-Key': idempotencyKey }) }
        )
      ),
      tap(() => this.invalidate('/api/financeiro'))
    );
  }

  baixarContaReceber(
    id: number,
    payload: BaixaTituloFinanceiroRequest,
    idempotencyKey = this.newIdempotencyKey()
  ): Observable<MovimentacaoFinanceiraResponse> {
    return this.readiness.waitForApi().pipe(
      switchMap(() =>
        this.http.post<MovimentacaoFinanceiraResponse>(
          this.config.apiUrl(`/api/financeiro/fluxo-caixa/contas-receber/${id}/baixar`),
          payload,
          { headers: new HttpHeaders({ 'Idempotency-Key': idempotencyKey }) }
        )
      ),
      tap(() => {
        this.invalidate('/api/financeiro');
        this.invalidate('/api/despesas');
      })
    );
  }

  baixarContaPagar(
    id: number,
    payload: BaixaTituloFinanceiroRequest,
    idempotencyKey = this.newIdempotencyKey()
  ): Observable<MovimentacaoFinanceiraResponse> {
    return this.readiness.waitForApi().pipe(
      switchMap(() =>
        this.http.post<MovimentacaoFinanceiraResponse>(
          this.config.apiUrl(`/api/financeiro/fluxo-caixa/contas-pagar/${id}/baixar`),
          payload,
          { headers: new HttpHeaders({ 'Idempotency-Key': idempotencyKey }) }
        )
      ),
      tap(() => this.invalidate('/api/financeiro'))
    );
  }

  transferirEntreContas(
    payload: TransferenciaInternaRequest,
    idempotencyKey = this.newIdempotencyKey()
  ): Observable<MovimentacaoFinanceiraResponse[]> {
    return this.readiness.waitForApi().pipe(
      switchMap(() =>
        this.http.post<MovimentacaoFinanceiraResponse[]>(
          this.config.apiUrl('/api/financeiro/fluxo-caixa/transferencias'),
          payload,
          { headers: new HttpHeaders({ 'Idempotency-Key': idempotencyKey }) }
        )
      ),
      tap(() => this.invalidate('/api/financeiro'))
    );
  }

  estornarMovimentacaoFinanceira(
    id: number,
    payload: EstornoMovimentacaoRequest,
    idempotencyKey = this.newIdempotencyKey()
  ): Observable<MovimentacaoFinanceiraResponse> {
    return this.readiness.waitForApi().pipe(
      switchMap(() =>
        this.http.post<MovimentacaoFinanceiraResponse>(
          this.config.apiUrl(`/api/financeiro/fluxo-caixa/movimentacoes/${id}/estornar`),
          payload,
          { headers: new HttpHeaders({ 'Idempotency-Key': idempotencyKey }) }
        )
      ),
      tap(() => this.invalidate('/api/financeiro'))
    );
  }

  getConciliacaoFinanceira(contaFinanceiraId?: number | null): Observable<ConciliacaoResumoResponse> {
    const params = this.buildParams({
      contaFinanceiraId:
        contaFinanceiraId !== undefined && contaFinanceiraId !== null ? String(contaFinanceiraId) : undefined
    });
    return this.getWithOptionalParams<ConciliacaoResumoResponse>('/api/financeiro/fluxo-caixa/conciliacao', params);
  }

  importarExtratoConciliacao(
    payload: ImportacaoExtratoRequest,
    idempotencyKey = this.newIdempotencyKey()
  ): Observable<ConciliacaoResumoResponse> {
    return this.readiness.waitForApi().pipe(
      switchMap(() =>
        this.http.post<ConciliacaoResumoResponse>(
          this.config.apiUrl('/api/financeiro/fluxo-caixa/conciliacao/importar'),
          payload,
          { headers: new HttpHeaders({ 'Idempotency-Key': idempotencyKey }) }
        )
      ),
      tap(() => this.invalidate('/api/financeiro'))
    );
  }

  conciliarManualmente(payload: ConciliacaoManualRequest): Observable<ConciliacaoItemResponse> {
    return this.readiness.waitForApi().pipe(
      switchMap(() =>
        this.http.post<ConciliacaoItemResponse>(
          this.config.apiUrl('/api/financeiro/fluxo-caixa/conciliacao/conciliar'),
          payload
        )
      ),
      tap(() => this.invalidate('/api/financeiro'))
    );
  }

  getIndicadoresPainel(query: IndicadoresQueryParams = {}): Observable<PainelInteligenciaAdministrativaResponse> {
    const params = this.buildParams({
      competencia: query.competencia,
      dataInicio: query.dataInicio,
      dataFim: query.dataFim,
      filialId: query.filialId !== undefined && query.filialId !== null ? String(query.filialId) : undefined,
      consolidarFiliais: query.consolidarFiliais
    });
    return this.getWithOptionalParams<PainelInteligenciaAdministrativaResponse>('/api/financeiro/indicadores', params);
  }

  getIndicadoresParametros(): Observable<ParametroInteligenciaAdministrativa> {
    return this.getWithOptionalParams<ParametroInteligenciaAdministrativa>('/api/financeiro/indicadores/parametros');
  }

  salvarIndicadoresParametros(
    payload: ParametroInteligenciaAdministrativa
  ): Observable<ParametroInteligenciaAdministrativa> {
    return this.readiness.waitForApi().pipe(
      switchMap(() =>
        this.http.put<ParametroInteligenciaAdministrativa>(
          this.config.apiUrl('/api/financeiro/indicadores/parametros'),
          payload
        )
      ),
      tap(() => this.invalidate('/api/financeiro'))
    );
  }

  getBalancoPatrimonialGerencial(competencia?: string): Observable<BalancoPatrimonialGerencialResponse> {
    const params = this.buildParams({ competencia });
    return this.getWithOptionalParams<BalancoPatrimonialGerencialResponse>(
      '/api/financeiro/indicadores/balanco-patrimonial',
      params
    );
  }

  salvarBalancoPatrimonialGerencial(
    payload: BalancoPatrimonialGerencialRequest
  ): Observable<BalancoPatrimonialGerencialResponse> {
    return this.readiness.waitForApi().pipe(
      switchMap(() =>
        this.http.put<BalancoPatrimonialGerencialResponse>(
          this.config.apiUrl('/api/financeiro/indicadores/balanco-patrimonial'),
          payload
        )
      ),
      tap(() => this.invalidate('/api/financeiro'))
    );
  }

  simularPrecificacaoInteligente(
    payload: SimulacaoPrecificacaoRequest
  ): Observable<SimulacaoPrecificacaoResponse> {
    return this.postWithOptionalParams<SimulacaoPrecificacaoResponse>(
      '/api/financeiro/indicadores/precificacao/simular',
      payload
    );
  }

  aplicarPrecoProdutoAutorizado(payload: AplicarPrecoProdutoRequest): Observable<Produto> {
    return this.readiness.waitForApi().pipe(
      switchMap(() =>
        this.http.post<Produto>(
          this.config.apiUrl('/api/financeiro/indicadores/precificacao/aplicar-preco'),
          payload
        )
      ),
      tap(() => {
        this.invalidate('/api/produtos');
        this.invalidate('/api/financeiro');
      })
    );
  }

  simularInvestimentoIndicadores(
    payload: SimulacaoInvestimentoRequest,
    competencia?: string
  ): Observable<InvestimentosRetornoPainel> {
    const params = this.buildParams({ competencia });
    return this.postWithOptionalParams<InvestimentosRetornoPainel>(
      '/api/financeiro/indicadores/investimentos/simular',
      payload,
      params
    );
  }

  private buildFluxoCaixaParams(query: FluxoCaixaQueryParams): HttpParams | undefined {
    return this.buildParams({
      dataInicio: query.dataInicio,
      dataFim: query.dataFim,
      contaFinanceiraId:
        query.contaFinanceiraId !== undefined && query.contaFinanceiraId !== null
          ? String(query.contaFinanceiraId)
          : undefined,
      centroCustoId:
        query.centroCustoId !== undefined && query.centroCustoId !== null ? String(query.centroCustoId) : undefined,
      filialFiltroId:
        query.filialFiltroId !== undefined && query.filialFiltroId !== null
          ? String(query.filialFiltroId)
          : undefined,
      consolidarFiliais: query.consolidarFiliais
    });
  }

  private buildDreParams(
    query: DreQueryParams & {
      numeroLinha?: string | number | null;
      codigoLinha?: string | null;
      grupoDre?: string | null;
      planoContaId?: string | number | null;
    }
  ): HttpParams | undefined {
    return this.buildParams({
      tipoPeriodo: query.tipoPeriodo,
      competencia: query.competencia,
      ano: query.ano !== undefined && query.ano !== null ? String(query.ano) : undefined,
      trimestre: query.trimestre !== undefined && query.trimestre !== null ? String(query.trimestre) : undefined,
      semestre: query.semestre !== undefined && query.semestre !== null ? String(query.semestre) : undefined,
      dataInicio: query.dataInicio,
      dataFim: query.dataFim,
      compararPeriodoAnterior: query.compararPeriodoAnterior,
      competenciaComparacao: query.competenciaComparacao,
      comparacaoInicio: query.comparacaoInicio,
      comparacaoFim: query.comparacaoFim,
      centroCustoId: query.centroCustoId !== undefined && query.centroCustoId !== null ? String(query.centroCustoId) : undefined,
      filialFiltroId: query.filialFiltroId !== undefined && query.filialFiltroId !== null ? String(query.filialFiltroId) : undefined,
      consolidarFiliais: query.consolidarFiliais,
      numeroLinha: query.numeroLinha !== undefined && query.numeroLinha !== null ? String(query.numeroLinha) : undefined,
      codigoLinha: query.codigoLinha !== undefined && query.codigoLinha !== null ? String(query.codigoLinha) : undefined,
      grupoDre: query.grupoDre !== undefined && query.grupoDre !== null ? String(query.grupoDre) : undefined,
      planoContaId: query.planoContaId !== undefined && query.planoContaId !== null ? String(query.planoContaId) : undefined
    });
  }

  checkoutVenda(payload: {
    clienteId: number;
    usuarioId: number;
    desconto: number;
    itens: Array<{ produtoId: number; quantidade: number }>;
    pagamentos?: CheckoutPayment[];
    observacao?: string;
    caixaSessaoId?: number;
    condicaoPagamentoId?: number;
    idempotencyKey?: string;
  }): Observable<Identifiable> {
    const body = {
      clienteId: payload.clienteId,
      usuarioId: this.auth.user()?.usuarioId ?? payload.usuarioId,
      desconto: payload.desconto,
      observacao: payload.observacao,
      caixaSessaoId: payload.caixaSessaoId,
      condicaoPagamentoId: payload.condicaoPagamentoId,
      itens: payload.itens.map((item) => ({
        produtoId: item.produtoId,
        quantidade: item.quantidade
      })),
      pagamentos: (payload.pagamentos ?? []).map((payment) => ({
        formaPagamentoId: payment.formaPagamentoId,
        valorPago: payment.valorPago
      }))
    };
    const idempotencyKey = payload.idempotencyKey ?? this.newIdempotencyKey();
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post<Identifiable>(this.config.apiUrl('/api/vendas'), body, {
        headers: new HttpHeaders({ 'Idempotency-Key': idempotencyKey })
      })),
      tap(() => {
        this.invalidate('/api/vendas');
        this.invalidate('/api/caixa');
        this.invalidate('/api/despesas');
      })
    );
  }

  private invalidateOperationalSaleData(): void {
    this.invalidate('/api/vendas');
    this.invalidate('/api/caixa');
    this.invalidate('/api/dashboard');
    this.invalidate('/api/financeiro');
    this.invalidate('/api/produtos');
  }

  invoiceEntry(payload: Identifiable, idempotencyKey = this.newIdempotencyKey()): Observable<Identifiable> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post<Identifiable>(this.config.apiUrl('/api/nfs/entrada'), payload, {
        headers: new HttpHeaders({ 'Idempotency-Key': idempotencyKey })
      })),
      tap(() => this.invalidateOperationalSaleData())
    );
  }

  private buildParams(entries: Record<string, string | boolean | undefined>): HttpParams | undefined {
    let params = new HttpParams();
    let hasEntries = false;
    for (const [key, value] of Object.entries(entries)) {
      if (value !== undefined && String(value).trim() !== '') {
        params = params.set(key, typeof value === 'string' ? value.trim() : String(value));
        hasEntries = true;
      }
    }
    return hasEntries ? params : undefined;
  }

  private getWithOptionalParams<T>(path: string, params?: HttpParams): Observable<T> {
    return this.readiness.waitForApi().pipe(
      switchMap(() =>
        params
          ? this.http.get<T>(this.config.apiUrl(path), { params })
          : this.http.get<T>(this.config.apiUrl(path))
      )
    );
  }

  private postWithOptionalParams<T>(path: string, body: object, params?: HttpParams): Observable<T> {
    return this.readiness.waitForApi().pipe(
      switchMap(() =>
        params
          ? this.http.post<T>(this.config.apiUrl(path), body, { params })
          : this.http.post<T>(this.config.apiUrl(path), body)
      )
    );
  }

  private invalidate(path: string): void {
    for (const key of this.listCache.keys()) {
      const endpoint = key.split('|')[0] ?? key;
      if (this.endpointMatches(endpoint, path)) {
        this.listCache.delete(key);
      }
    }
  }

  private endpointMatches(endpoint: string, invalidatedPath: string): boolean {
    const normalize = (value: string) => (value.length > 1 && value.endsWith('/') ? value.slice(0, -1) : value);
    const normalizedEndpoint = normalize(endpoint);
    const normalizedPath = normalize(invalidatedPath);
    return normalizedEndpoint === normalizedPath
      || normalizedEndpoint.startsWith(`${normalizedPath}/`)
      || normalizedPath.startsWith(`${normalizedEndpoint}/`);
  }

  private cacheKey(endpoint: string): string {
    const user = this.auth.user();
    const empresa = safeLocalStorage('apex.activeEmpresaId') ?? '1';
    const filial = safeLocalStorage('apex.activeFilialId') ?? '1';
    const roles = user?.roles?.join(',') ?? 'publico';
    return `${endpoint}|${user?.login ?? 'anon'}|${roles}|empresa:${empresa}|filial:${filial}`;
  }

  private newIdempotencyKey(): string {
    const browserCrypto = globalThis.crypto as Crypto & { randomUUID?: () => string };
    if (typeof browserCrypto?.randomUUID === 'function') {
      return browserCrypto.randomUUID();
    }
    const bytes = new Uint8Array(16);
    if (typeof browserCrypto?.getRandomValues === 'function') {
      browserCrypto.getRandomValues(bytes);
      return Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');
    }
    return `${Date.now()}-${performance.now()}`;
  }
}

function safeLocalStorage(key: string): string | null {
  try {
    return globalThis.localStorage?.getItem(key) ?? null;
  } catch {
    return null;
  }
}
