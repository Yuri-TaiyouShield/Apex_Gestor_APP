import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { ApexApiService } from './apex-api.service';
import { ApiConfigService } from './api-config.service';
import { ApiReadinessService } from './api-readiness.service';
import { AuthService } from './auth.service';
import {
  AdmCalcRequest,
  B2cCartMergeRequest,
  CaixaSessao,
  FinancialDocumentRequest,
  Identifiable,
  LaborCalculationRequest,
  PricingCalculationRequest,
  RecebimentoPagamento,
  SignFinancialDocumentRequest,
  TaxCalculationRequest
} from './models';

describe('ApexApiService', () => {
  let http: jasmine.SpyObj<HttpClient>;
  let authUser: jasmine.Spy;
  let service: ApexApiService;

  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
    localStorage.setItem('apex.activeEmpresaId', '1');
    localStorage.setItem('apex.activeFilialId', '1');

    http = jasmine.createSpyObj<HttpClient>('HttpClient', ['get', 'post', 'put', 'patch', 'delete']);
    authUser = jasmine.createSpy('user').and.returnValue({
      login: 'v5_dono',
      roles: ['ROLE_DONO_GERENTE'],
      usuarioId: 77,
      accessToken: 'access-secret',
      refreshToken: '',
      expiresAt: '',
      tokenType: 'Bearer',
      nome: 'Dono Teste'
    });
    TestBed.configureTestingModule({
      providers: [
        ApexApiService,
        { provide: HttpClient, useValue: http },
        { provide: ApiConfigService, useValue: { apiUrl: (path: string) => `http://api.test${path}` } },
        { provide: ApiReadinessService, useValue: { waitForApi: () => of(undefined) } },
        { provide: AuthService, useValue: { accessToken: () => 'access-secret', user: authUser } }
      ]
    });
    service = TestBed.inject(ApexApiService);
  });

  afterEach(() => {
    sessionStorage.clear();
    localStorage.clear();
  });

  it('faz cache do list por endpoint e reutiliza entre chamadas', () => {
    http.get.and.returnValue(of([{ id: 1 }]));

    service.list<{ id: number }>('/api/produtos').subscribe();
    service.list<{ id: number }>('/api/produtos').subscribe();

    expect(http.get).toHaveBeenCalledTimes(1);
    expect(http.get).toHaveBeenCalledWith('http://api.test/api/produtos');
  });

  it('força requisição nova quando list é acionado com refresh', () => {
    http.get.and.returnValue(of([]));

    service.list<unknown>('/api/produtos').subscribe();
    service.list<unknown>('/api/produtos', { refresh: true }).subscribe();

    expect(http.get).toHaveBeenCalledTimes(2);
  });

  it('invalida o cache do endpoint após create/update/patch', () => {
    http.get.and.returnValue(of([]));
    http.post.and.returnValue(of({ id: 9 }));
    http.put.and.returnValue(of({ id: 9 }));
    http.patch.and.returnValue(of({ id: 9 }));

    service.list<unknown>('/api/despesas').subscribe();
    service.create<unknown>('/api/despesas', { id: 9 }).subscribe();
    service.list<unknown>('/api/despesas').subscribe();
    service.update<unknown>('/api/despesas', 9, { id: 9 }).subscribe();
    service.list<unknown>('/api/despesas').subscribe();
    service.patch('/api/despesas', {}).subscribe();

    // Cada mutação invalida o cache; as três listagens disparam exatamente três GETs.
    expect(http.get).toHaveBeenCalledTimes(3);
    expect(http.patch).toHaveBeenCalledWith('http://api.test/api/despesas', {});
  });

  it('normaliza caixa inexistente (204/404) para null sem propagar erro', async () => {
    for (const status of [204, 404]) {
      http.get.and.returnValue(throwError(() => Object.assign(new Error('sem-sessao'), { status })));

      const cashier: CaixaSessao | null = (await service.currentCashier().toPromise()) ?? null;

      expect(cashier).toBeNull();
    }
    expect(http.get).toHaveBeenCalledTimes(2);
  });

  it('propaga erro real de caixa que não seja ausência de sessão', async () => {
    http.get.and.returnValue(throwError(() => Object.assign(new Error('operação recusada'), { status: 409 })));

    let error: unknown;
    try {
      await service.currentCashier().toPromise();
    } catch (failure) {
      error = failure;
    }

    expect((error as { status?: number }).status).toBe(409);
  });

  it('envia chave de idempotência explícita no recebimento por caixa', async () => {
    http.post.and.returnValue(of({ id: 3 }));

    const pagamentos: RecebimentoPagamento[] = [{ formaPagamentoId: 1, valor: 120.5, numeroParcelas: 1 }];
    const received = (await service.receiveCashierSale(35, pagamentos, 'chave-pdv-00035').toPromise()) as Identifiable | undefined;

    expect(received?.['id']).toBe(3);
    const recent = http.post.calls.mostRecent();
    expect(recent.args[0]).toBe('http://api.test/api/caixa/vendas/35/receber');
    const body = recent.args[1] as { pagamentos: RecebimentoPagamento[] };
    expect(body.pagamentos).toEqual(pagamentos);
    expect(JSON.stringify(body)).not.toContain('idempot');
    expect((recent.args[2] as { headers: HttpHeaders }).headers.get('Idempotency-Key')).toBe('chave-pdv-00035');
  });

  it('abre caixa com valor e observação padrão, invalidando a listagem em cache', async () => {
    http.get.and.returnValue(of([]));
    http.post.and.returnValue(of({ id: 5 }));

    service.list<unknown>('/api/caixa').subscribe();
    const opened = (await service.openCashier(150).toPromise()) as Identifiable | undefined;

    expect(opened?.['id']).toBe(5);
    const recent = http.post.calls.mostRecent();
    expect(recent.args[0]).toBe('http://api.test/api/caixa/abrir');
    expect(recent.args[1]).toEqual({ valor: 150, observacao: 'Abertura de caixa pelo Apex Gestor' });

    http.get.calls.reset();
    service.list<unknown>('/api/caixa').subscribe();
    expect(http.get).toHaveBeenCalledTimes(1);
  });

  it('executa suprimento e sangria com observação padrão e sobrescrita', async () => {
    http.post.and.returnValue(of({ id: 1 }));

    await service.cashierSupply(5, 200).toPromise();
    await service.cashierWithdraw(5, 80, 'Sangria auditada').toPromise();

    const supply = http.post.calls.first();
    expect(supply.args[0]).toBe('http://api.test/api/caixa/5/suprimento');
    expect(supply.args[1]).toEqual({ valor: 200, observacao: 'Suprimento operacional' });
    const withdraw = http.post.calls.mostRecent();
    expect(withdraw.args[0]).toBe('http://api.test/api/caixa/5/sangria');
    expect(withdraw.args[1]).toEqual({ valor: 80, observacao: 'Sangria auditada' });
  });

  it('fecha caixa com saldo informado e recusa venda com motivo', async () => {
    http.post.and.returnValue(of({ id: 5 }));

    const closed = (await service.closeCashier(5, 950).toPromise()) as Identifiable | undefined;
    const rejected = (await service.rejectCashierSale(77, 'estorno duplicado').toPromise()) as Identifiable | undefined;

    expect(closed?.['id']).toBe(5);
    expect(rejected?.['id']).toBe(5);
    const closeCall = http.post.calls.all()[0];
    expect(closeCall.args[0]).toBe('http://api.test/api/caixa/5/fechar');
    expect(closeCall.args[1]).toEqual({ valor: 950 });
    const rejectCall = http.post.calls.all()[1];
    expect(rejectCall.args[0]).toBe('http://api.test/api/caixa/vendas/77/recusar');
    expect(rejectCall.args[1]).toEqual({ motivo: 'estorno duplicado' });
  });

  it('consulta vendas pendentes, créditos, histórico e contas', async () => {
    http.get.and.returnValue(of([]));

    await service.pendingCashierSales().toPromise();
    await service.cashierCredits(31).toPromise();
    await service.cashierHistory().toPromise();
    await service.accountsReceivable().toPromise();
    await service.accountsPayable().toPromise();

    const urls = http.get.calls.all().map((call) => call.args[0]);
    expect(urls).toEqual([
      'http://api.test/api/caixa/vendas-pendentes',
      'http://api.test/api/caixa/clientes/31/creditos',
      'http://api.test/api/caixa/historico',
      'http://api.test/api/financeiro/operacional/contas-receber',
      'http://api.test/api/financeiro/operacional/contas-pagar'
    ]);
  });

  it('consulta histórico de vendas com trim de filtros e sem filtros', async () => {
    http.get.and.returnValue(of([]));

    await service.salesHistory('  camisa 12 ', 'PENDING').toPromise();
    await service.salesHistory().toPromise();

    const filtered = http.get.calls.first().args[1] as { params: HttpParams };
    expect(filtered.params.get('q')).toBe('camisa 12');
    expect(filtered.params.get('status')).toBe('PENDING');
    const plain = http.get.calls.mostRecent().args[1] as { params: HttpParams };
    expect(plain.params.get('q')).toBeNull();
    expect(plain.params.get('status')).toBeNull();
  });

  it('monta relatórios financeiros e operacionais', async () => {
    http.get.and.returnValue(of({}));

    await service.financialReport('2026-01-01', '2026-01-31').toPromise();
    await service.operationalReport('vendas').toPromise();

    expect(http.get.calls.first().args[0]).toBe('http://api.test/api/relatorios/financeiro');
    const financeOptions = http.get.calls.first().args[1] as { params: HttpParams };
    expect(financeOptions.params.get('inicio')).toBe('2026-01-01');
    expect(financeOptions.params.get('fim')).toBe('2026-01-31');
    expect(http.get.calls.mostRecent().args[0]).toBe('http://api.test/api/relatorios/vendas');
  });

  it('envia cálculos financeiros e precificação para endpoints dedicados', async () => {
    http.post.and.returnValue(of({ total: 10 }));

    await service.laborCalculation({} as LaborCalculationRequest).toPromise();
    await service.taxCalculation({} as TaxCalculationRequest).toPromise();
    await service.admCalcCalculation({} as AdmCalcRequest).toPromise();
    await service.pricingCalculation({} as PricingCalculationRequest).toPromise();

    const urls = http.post.calls.all().map((call) => call.args[0]);
    expect(urls).toEqual([
      'http://api.test/api/financeiro/calculos/trabalhista',
      'http://api.test/api/financeiro/calculos/tributario',
      'http://api.test/api/financeiro/calculos/admcalc',
      'http://api.test/api/financeiro/precificacao'
    ]);
  });

  it('gerencia documentos financeiros, assinaturas, auditoria e carrinho B2C', async () => {
    http.get.and.returnValue(of([]));
    http.post.and.returnValue(of({ id: 2 }));

    await service.financialDocuments().toPromise();
    await service.createFinancialDocument({} as FinancialDocumentRequest).toPromise();
    await service.signFinancialDocument(2, {} as SignFinancialDocumentRequest).toPromise();
    await service.financialAudit().toPromise();
    await service.mergeB2cCart({} as B2cCartMergeRequest).toPromise();

    const getUrls = http.get.calls.all().map((call) => call.args[0]);
    expect(getUrls).toEqual([
      'http://api.test/api/financeiro/documentos',
      'http://api.test/api/financeiro/auditoria'
    ]);
    const postSecond = http.post.calls.all()[1];
    expect(postSecond.args[0]).toBe('http://api.test/api/financeiro/documentos/2/assinaturas');
    expect(http.post.calls.mostRecent().args[0]).toBe('http://api.test/api/b2c/cart/merge');
  });

  it('monta checkout completo com pagamento, caixa e chave de idempotência informadas', async () => {
    authUser.and.returnValue(null);
    http.post.and.returnValue(of({ id: 66 }));

    const created = (await service.checkoutVenda({
      clienteId: 8,
      usuarioId: 12,
      desconto: 5,
      observacao: 'venda balcão',
      caixaSessaoId: 3,
      condicaoPagamentoId: 21,
      idempotencyKey: 'chave-fixa',
      itens: [{ produtoId: 4, quantidade: 2 }],
      pagamentos: [{ formaPagamentoId: 1, valorPago: 30, nome: 'Dinheiro' }]
    }).toPromise()) as Identifiable | undefined;

    expect(created?.['id']).toBe(66);
    const recent = http.post.calls.mostRecent();
    const body = recent.args[1] as Record<string, unknown>;
    expect(body['usuarioId']).toBe(12);
    expect(body['observacao']).toBe('venda balcão');
    expect(body['caixaSessaoId']).toBe(3);
    expect(body['condicaoPagamentoId']).toBe(21);
    expect(body['pagamentos']).toEqual([{ formaPagamentoId: 1, valorPago: 30 }]);
    expect((recent.args[2] as { headers: HttpHeaders }).headers.get('Idempotency-Key')).toBe('chave-fixa');
  });

  it('invalida listas de sub-endpoints quando o endpoint pai muda', () => {
    http.get.and.returnValue(of([]));

    service.list<unknown>('/api/despesas/tipos').subscribe();
    service.list<unknown>('/api/despesas/arquivadas').subscribe();
    expect(http.get).toHaveBeenCalledTimes(2);

    http.post.and.returnValue(of({ id: 1 }));
    service.create<unknown>('/api/despesas', { id: 1 }).subscribe();

    service.list<unknown>('/api/despesas/tipos').subscribe();
    service.list<unknown>('/api/despesas/arquivadas').subscribe();
    expect(http.get).toHaveBeenCalledTimes(4);
  });

  it('descarta todo o cache por usuário quando a sessão é encerrada', () => {
    http.get.and.returnValue(of([]));

    service.list<unknown>('/api/produtos').subscribe();
    expect(http.get).toHaveBeenCalledTimes(1);

    window.dispatchEvent(new Event('apex-auth-cleared'));

    service.list<unknown>('/api/produtos').subscribe();
    expect(http.get).toHaveBeenCalledTimes(2);
  });

  it('propaga falha de listagem e libera tentativa nova depois', async () => {
    http.get.and.returnValue(throwError(() => new Error('boom')));

    let error: unknown;
    try {
      await service.list<unknown>('/api/produtos').toPromise();
    } catch (failure) {
      error = failure;
    }
    expect(error).toBeInstanceOf(Error);

    http.get.and.returnValue(of([{ id: 2 }]));
    const items = await service.list<unknown>('/api/produtos').toPromise();

    expect(items).toEqual([{ id: 2 }]);
    expect(http.get).toHaveBeenCalledTimes(2);
  });

  it('registra entrada de NF com idempotência e invalida caixa em cache', async () => {
    http.get.and.returnValue(of([]));
    http.post.and.returnValue(of({ id: 71 }));

    service.list<unknown>('/api/caixa').subscribe();
    const created = (await service.invoiceEntry({ id: 1 }).toPromise()) as Identifiable | undefined;

    expect(created?.['id']).toBe(71);
    const recent = http.post.calls.mostRecent();
    expect(recent.args[0]).toBe('http://api.test/api/nfs/entrada');
    expect((recent.args[2] as { headers: HttpHeaders }).headers.get('Idempotency-Key')).toBeTruthy();

    http.get.calls.reset();
    service.list<unknown>('/api/caixa').subscribe();
    expect(http.get).toHaveBeenCalledTimes(1);
  });

  it('invalida vendas, caixa, dashboard, financeiro e produtos após operação de venda', async () => {
    http.get.and.returnValue(of([]));
    http.post.and.returnValue(of({ id: 9 }));

    service.list<unknown>('/api/produtos').subscribe();
    service.list<unknown>('/api/vendas').subscribe();
    service.list<unknown>('/api/caixa').subscribe();
    service.list<unknown>('/api/financeiro/operacional/contas-receber').subscribe();
    service.list<unknown>('/api/categorias').subscribe();
    expect(http.get).toHaveBeenCalledTimes(5);

    await service.receiveCashierSale(1, [{ formaPagamentoId: 1, valor: 20 }], 'chave-unitaria').toPromise();

    service.list<unknown>('/api/produtos').subscribe();
    service.list<unknown>('/api/vendas').subscribe();
    expect(http.get).toHaveBeenCalledTimes(7);

    service.list<unknown>('/api/categorias').subscribe();
    expect(http.get).toHaveBeenCalledTimes(7);
  });

  it('constrói checkout com usuário da sessão e chave de idempotência', async () => {
    http.post.and.returnValue(of({ id: 55 }));

    const created = (await service.checkoutVenda({
      clienteId: 3,
      usuarioId: 5,
      desconto: 0,
      itens: [{ produtoId: 1, quantidade: 2 }]
    }).toPromise()) as Identifiable;

    expect(created['id']).toBe(55);
    const recent = http.post.calls.mostRecent();
    expect(recent.args[0]).toBe('http://api.test/api/vendas');
    const body = recent.args[1] as Record<string, unknown>;
    expect(body['usuarioId']).toBe(77);
    const options = recent.args[2] as { headers: HttpHeaders };
    expect(options.headers.get('Idempotency-Key')).toBeTruthy();
  });

  it('executa patch com payload default e invalida cache', async () => {
    http.patch.and.returnValue(of({ ok: true }));
    http.get.and.returnValue(of([]));

    service.list<unknown>('/api/recurso').subscribe();
    expect(http.get).toHaveBeenCalledTimes(1);

    await service.patch('/api/recurso/1').toPromise();
    expect(http.patch).toHaveBeenCalledWith('http://api.test/api/recurso/1', {});

    service.list<unknown>('/api/recurso').subscribe();
    expect(http.get).toHaveBeenCalledTimes(2);
  });

  it('suprimento e sangria usam observações padrão quando omitidas', async () => {
    http.post.and.returnValue(of({ id: 10 } as unknown as CaixaSessao));

    await service.cashierSupply(10, 100).toPromise();
    expect(http.post.calls.mostRecent().args[1]).toEqual({ valor: 100, observacao: 'Suprimento operacional' });

    await service.cashierWithdraw(10, 50).toPromise();
    expect(http.post.calls.mostRecent().args[1]).toEqual({ valor: 50, observacao: 'Sangria operacional' });
  });

  it('recebimento de venda gera chave de idempotência automaticamente se omitida', async () => {
    http.post.and.returnValue(of({ id: 99 } as unknown as Identifiable));

    await service.receiveCashierSale(99, [{ formaPagamentoId: 1, valor: 50 }]).toPromise();

    const recent = http.post.calls.mostRecent();
    const options = recent.args[2] as { headers: HttpHeaders };
    expect(options.headers.get('Idempotency-Key')).toBeTruthy();
  });

  it('gera chave de cache consistente mesmo para usuário anônimo ou sem roles', () => {
    authUser.and.returnValue(null);
    http.get.and.returnValue(of([{ id: 1 }]));

    service.list<unknown>('/api/publico').subscribe();
    expect(http.get).toHaveBeenCalledTimes(1);

    service.list<unknown>('/api/publico').subscribe();
    expect(http.get).toHaveBeenCalledTimes(1);
  });
});
