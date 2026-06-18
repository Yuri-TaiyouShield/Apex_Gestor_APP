import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of, shareReplay, switchMap, tap, throwError } from 'rxjs';

import { ApiConfigService } from './api-config.service';
import { ApiReadinessService } from './api-readiness.service';
import { AuthService } from './auth.service';
import {
  AdmCalcRequest,
  B2cCartMergeRequest,
  B2cCartMergeResponse,
  CaixaSessao,
  CheckoutPayment,
  Cliente,
  CondicaoPagamento,
  Despesa,
  FinancialAuditEvent,
  FinancialCalculationResponse,
  FinancialDocument,
  FinancialDocumentRequest,
  FormaPagamento,
  Identifiable,
  LaborCalculationRequest,
  RelatorioOperacional,
  PricingCalculationRequest,
  PricingCalculationResponse,
  Produto,
  RelatorioFinanceiro,
  SignFinancialDocumentRequest,
  TaxCalculationRequest,
  TipoDespesa,
  Usuario
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

  patch<T>(path: string): Observable<T> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.patch<T>(this.config.apiUrl(path), {})),
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

  closeCashier(id: number): Observable<CaixaSessao> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post<CaixaSessao>(this.config.apiUrl(`/api/caixa/${id}/fechar`), {})),
      tap(() => this.invalidate('/api/caixa'))
    );
  }

  expenses(): Observable<Despesa[]> {
    return this.list<Despesa>('/api/despesas');
  }

  expenseTypes(): Observable<TipoDespesa[]> {
    return this.list<TipoDespesa>('/api/despesas/tipos');
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

  checkoutVenda(payload: {
    clienteId: number;
    usuarioId: number;
    desconto: number;
    itens: Array<{ produtoId: number; quantidade: number }>;
    pagamentos: CheckoutPayment[];
    observacao?: string;
    caixaSessaoId?: number;
  }): Observable<Identifiable> {
    const body = {
      clienteId: payload.clienteId,
      usuarioId: payload.usuarioId,
      desconto: payload.desconto,
      observacao: payload.observacao,
      caixaSessaoId: payload.caixaSessaoId,
      itens: payload.itens.map((item) => ({
        produtoId: item.produtoId,
        quantidade: item.quantidade
      })),
      pagamentos: payload.pagamentos.map((payment) => ({
        formaPagamentoId: payment.formaPagamentoId,
        valorPago: payment.valorPago
      }))
    };
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post<Identifiable>(this.config.apiUrl('/api/vendas'), body)),
      tap(() => {
        this.invalidate('/api/vendas');
        this.invalidate('/api/caixa');
        this.invalidate('/api/despesas');
      })
    );
  }

  invoiceEntry(payload: Identifiable): Observable<Identifiable> {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post<Identifiable>(this.config.apiUrl('/api/nfs/entrada'), payload))
    );
  }

  private invalidate(path: string): void {
    for (const key of this.listCache.keys()) {
      const endpoint = key.split('|')[0] ?? key;
      if (path.startsWith(endpoint) || endpoint.startsWith(path)) {
        this.listCache.delete(key);
      }
    }
  }

  private cacheKey(endpoint: string): string {
    const user = this.auth.user();
    const empresa = safeLocalStorage('apex.activeEmpresaId') ?? '1';
    const filial = safeLocalStorage('apex.activeFilialId') ?? '1';
    const roles = user?.roles?.join(',') ?? 'publico';
    return `${endpoint}|${user?.login ?? 'anon'}|${roles}|empresa:${empresa}|filial:${filial}`;
  }
}

function safeLocalStorage(key: string): string | null {
  try {
    return globalThis.localStorage?.getItem(key) ?? null;
  } catch {
    return null;
  }
}
