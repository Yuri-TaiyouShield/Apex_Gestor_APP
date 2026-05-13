import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, shareReplay, tap, throwError } from 'rxjs';

import { ApiConfigService } from './api-config.service';
import { CheckoutPayment, Cliente, Despesa, FormaPagamento, Identifiable, Produto, RelatorioFinanceiro, TipoDespesa, Usuario } from './models';

@Injectable({ providedIn: 'root' })
export class ApexApiService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(ApiConfigService);
  private readonly listCache = new Map<string, Observable<unknown>>();

  list<T>(endpoint: string): Observable<T[]> {
    const cached = this.listCache.get(endpoint) as Observable<T[]> | undefined;
    if (cached) {
      return cached;
    }

    const request = this.http.get<T[]>(this.config.apiUrl(endpoint)).pipe(
      catchError((error) => {
        this.listCache.delete(endpoint);
        return throwError(() => error);
      }),
      shareReplay({ bufferSize: 1, refCount: false })
    );
    this.listCache.set(endpoint, request);
    return request;
  }

  create<T>(endpoint: string, payload: Partial<T>): Observable<T> {
    return this.http.post<T>(this.config.apiUrl(endpoint), payload).pipe(tap(() => this.invalidate(endpoint)));
  }

  update<T>(endpoint: string, id: number | string, payload: Partial<T>): Observable<T> {
    return this.http.put<T>(this.config.apiUrl(`${endpoint}/${id}`), payload).pipe(tap(() => this.invalidate(endpoint)));
  }

  patch<T>(path: string): Observable<T> {
    return this.http.patch<T>(this.config.apiUrl(path), {}).pipe(tap(() => this.invalidate(path)));
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

  expenses(): Observable<Despesa[]> {
    return this.list<Despesa>('/api/despesas');
  }

  expenseTypes(): Observable<TipoDespesa[]> {
    return this.list<TipoDespesa>('/api/despesas/tipos');
  }

  financialReport(inicio: string, fim: string): Observable<RelatorioFinanceiro> {
    const params = new HttpParams().set('inicio', inicio).set('fim', fim);
    return this.http.get<RelatorioFinanceiro>(this.config.apiUrl('/api/relatorios/financeiro'), { params });
  }

  checkoutVenda(payload: {
    clienteId: number;
    usuarioId: number;
    desconto: number;
    itens: Array<{ produtoId: number; quantidade: number }>;
    pagamentos: CheckoutPayment[];
    observacao?: string;
  }): Observable<Identifiable> {
    const body = {
      cliente: { idCliente: payload.clienteId },
      usuario: { idUsuario: payload.usuarioId },
      desconto: payload.desconto,
      observacao: payload.observacao,
      itens: payload.itens.map((item) => ({
        produto: { idProduto: item.produtoId },
        quantidade: item.quantidade
      })),
      pagamentos: payload.pagamentos.map((payment) => ({
        formaPagamento: { idFormaPagamento: payment.formaPagamentoId },
        valorPago: payment.valorPago
      }))
    };
    return this.http.post<Identifiable>(this.config.apiUrl('/api/vendas'), body);
  }

  invoiceEntry(payload: Identifiable): Observable<Identifiable> {
    return this.http.post<Identifiable>(this.config.apiUrl('/api/nfs/entrada'), payload);
  }

  private invalidate(path: string): void {
    for (const key of this.listCache.keys()) {
      if (path.startsWith(key) || key.startsWith(path)) {
        this.listCache.delete(key);
      }
    }
  }
}
