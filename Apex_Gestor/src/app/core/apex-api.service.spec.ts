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
  CentroCustoRequest,
  FinancialDocumentRequest,
  Identifiable,
  LaborCalculationRequest,
  PlanoContasRequest,
  PricingCalculationRequest,
  RecebimentoPagamento,
  ReclassificacaoManualRequest,
  RegraClassificacaoRequest,
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

  it('gerencia Plano de Contas Gerencial com filtros opcionais, bootstrap, CRUD e toggle de status', async () => {
    http.get.and.returnValue(of([]));
    http.post.and.returnValue(of({ idPlanoConta: 10 }));
    http.put.and.returnValue(of({ idPlanoConta: 10 }));
    http.patch.and.returnValue(of({ idPlanoConta: 10, ativo: false }));

    await service.listPlanoContas().toPromise();
    await service.listPlanoContas(true).toPromise();
    await service.getPlanoContasTree(false).toPromise();
    await service.bootstrapPlanoContasPadrao().toPromise();

    const payload: PlanoContasRequest = {
      codigo: '4.1.01',
      nome: 'Aluguel',
      tipoConta: 'DESPESA',
      natureza: 'DEBITO',
      classificacaoCusto: 'FIXO',
      grupoDre: 'DESPESA_INFRAESTRUTURA',
      aceitaLancamento: true
    };
    await service.createPlanoConta(payload).toPromise();
    await service.updatePlanoConta(10, payload).toPromise();
    await service.togglePlanoContaStatus(10, false).toPromise();

    expect(http.get.calls.all()[0].args).toEqual(['http://api.test/api/financeiro/plano-contas']);
    const activeCall = http.get.calls.all()[1];
    expect(activeCall.args[0]).toBe('http://api.test/api/financeiro/plano-contas');
    expect((activeCall.args[1] as { params: HttpParams }).params.get('somenteAtivos')).toBe('true');

    const treeCall = http.get.calls.all()[2];
    expect(treeCall.args[0]).toBe('http://api.test/api/financeiro/plano-contas/arvore');
    expect((treeCall.args[1] as { params: HttpParams }).params.get('somenteAtivos')).toBe('false');

    expect(http.post.calls.all()[0].args[0]).toBe('http://api.test/api/financeiro/plano-contas/inicializar-padrao');
    expect(http.post.calls.all()[1].args).toEqual(['http://api.test/api/financeiro/plano-contas', payload]);
    expect(http.put.calls.mostRecent().args).toEqual(['http://api.test/api/financeiro/plano-contas/10', payload]);
    expect(http.patch.calls.mostRecent().args).toEqual(['http://api.test/api/financeiro/plano-contas/10/status?ativo=false', {}]);
  });

  it('gerencia Centros de Custo com filtros, bootstrap, CRUD e status', async () => {
    http.get.and.returnValue(of([]));
    http.post.and.returnValue(of([]));
    http.put.and.returnValue(of({ idCentroCusto: 3 }));
    http.patch.and.returnValue(of({ idCentroCusto: 3, ativo: true }));

    await service.listCentrosCusto().toPromise();
    await service.listCentrosCusto(true).toPromise();
    await service.bootstrapCentrosCustoPadrao().toPromise();

    const payload: CentroCustoRequest = {
      codigo: 'CC-LOJA',
      nome: 'Loja Matriz',
      tipoOperacional: 'LOJA'
    };
    await service.createCentroCusto(payload).toPromise();
    await service.updateCentroCusto(3, payload).toPromise();
    await service.toggleCentroCustoStatus(3, true).toPromise();

    expect(http.get.calls.all()[0].args).toEqual(['http://api.test/api/financeiro/centros-custo']);
    expect((http.get.calls.all()[1].args[1] as { params: HttpParams }).params.get('somenteAtivos')).toBe('true');
    expect(http.post.calls.all()[0].args[0]).toBe('http://api.test/api/financeiro/centros-custo/inicializar-padrao');
    expect(http.post.calls.all()[1].args).toEqual(['http://api.test/api/financeiro/centros-custo', payload]);
    expect(http.put.calls.mostRecent().args).toEqual(['http://api.test/api/financeiro/centros-custo/3', payload]);
    expect(http.patch.calls.mostRecent().args).toEqual(['http://api.test/api/financeiro/centros-custo/3/status?ativo=true', {}]);
  });

  it('gerencia regras de classificação, pendências, lote automático e reclassificação manual', async () => {
    http.get.and.returnValue(of([]));
    http.post.and.returnValue(of({ processados: 2, classificados: 2, naoClassificados: 0, observacoes: [] }));
    http.put.and.returnValue(of({ idRegraClassificacao: 7 }));
    http.patch.and.returnValue(of({ idRegraClassificacao: 7, ativo: false }));

    await service.listRegrasClassificacao('DESPESA', true).toPromise();
    await service.getPendenciasClassificacao().toPromise();

    const regraPayload: RegraClassificacaoRequest = {
      nome: 'Energia',
      origemTipo: 'DESPESA',
      palavraChave: 'ENERGIA',
      planoContaId: 5,
      centroCustoId: 2,
      prioridade: 10
    };
    await service.createRegraClassificacao(regraPayload).toPromise();
    await service.updateRegraClassificacao(7, regraPayload).toPromise();
    await service.toggleRegraClassificacaoStatus(7, false).toPromise();
    await service.aplicarClassificacaoAutomatica('DESPESA').toPromise();
    await service.aplicarClassificacaoAutomatica().toPromise();

    const manualPayload: ReclassificacaoManualRequest = {
      origemTipo: 'DESPESA',
      registroId: 99,
      planoContaId: 5,
      centroCustoId: 2,
      motivo: 'Ajuste gerencial'
    };
    await service.reclassificarManualmente(manualPayload).toPromise();

    const regrasCall = http.get.calls.all()[0];
    expect(regrasCall.args[0]).toBe('http://api.test/api/financeiro/classificacoes/regras');
    const regrasParams = (regrasCall.args[1] as { params: HttpParams }).params;
    expect(regrasParams.get('origemTipo')).toBe('DESPESA');
    expect(regrasParams.get('somenteAtivas')).toBe('true');
    expect(http.get.calls.all()[1].args[0]).toBe('http://api.test/api/financeiro/classificacoes/pendentes');

    expect(http.post.calls.all()[0].args).toEqual(['http://api.test/api/financeiro/classificacoes/regras', regraPayload]);
    expect(http.put.calls.mostRecent().args).toEqual(['http://api.test/api/financeiro/classificacoes/regras/7', regraPayload]);
    expect(http.patch.calls.mostRecent().args).toEqual(['http://api.test/api/financeiro/classificacoes/regras/7/status?ativo=false', {}]);

    const autoFiltered = http.post.calls.all()[1];
    expect(autoFiltered.args[0]).toBe('http://api.test/api/financeiro/classificacoes/aplicar-automatico');
    expect((autoFiltered.args[2] as { params: HttpParams }).params.get('origemTipo')).toBe('DESPESA');
    expect(http.post.calls.all()[2].args).toEqual(['http://api.test/api/financeiro/classificacoes/aplicar-automatico', {}]);
    expect(http.post.calls.all()[3].args).toEqual(['http://api.test/api/financeiro/classificacoes/manual', manualPayload]);
  });

  it('invalida cache de /api/despesas e /api/financeiro após aplicarClassificacaoAutomatica', async () => {
    http.get.and.returnValue(of([]));
    http.post.and.returnValue(of({ processados: 1, classificados: 1, naoClassificados: 0, observacoes: [] }));

    service.list<unknown>('/api/despesas').subscribe();
    service.list<unknown>('/api/financeiro/operacional/contas-pagar').subscribe();
    service.list<unknown>('/api/categorias').subscribe();
    expect(http.get).toHaveBeenCalledTimes(3);

    await service.aplicarClassificacaoAutomatica().toPromise();

    service.list<unknown>('/api/despesas').subscribe();
    service.list<unknown>('/api/financeiro/operacional/contas-pagar').subscribe();
    service.list<unknown>('/api/categorias').subscribe();
    expect(http.get).toHaveBeenCalledTimes(5);
  });

  it('consulta e recalcula consolidação por competência com parâmetro opcional', async () => {
    http.get.and.returnValue(of({ competenciaAnoMes: '2026-11' }));
    http.post.and.returnValue(of({ competenciaAnoMes: '2026-11' }));

    await service.getConsolidacaoCompetencia().toPromise();
    await service.getConsolidacaoCompetencia('2026-11').toPromise();
    await service.recalcularConsolidacaoCompetencia('2026-11').toPromise();

    expect(http.get.calls.all()[0].args).toEqual(['http://api.test/api/financeiro/consolidacao/competencia']);
    expect((http.get.calls.all()[1].args[1] as { params: HttpParams }).params.get('competencia')).toBe('2026-11');
    const recalcCall = http.post.calls.mostRecent();
    expect(recalcCall.args[0]).toBe('http://api.test/api/financeiro/consolidacao/competencia/recalcular');
    expect((recalcCall.args[2] as { params: HttpParams }).params.get('competencia')).toBe('2026-11');
  });

  it('gerencia tipos de despesa e pagamento de despesa com invalidação de cache', async () => {
    http.get.and.returnValue(of([]));
    http.post.and.returnValue(of({ idTipoDespesa: 4, nome: 'Energia' }));
    http.put.and.returnValue(of({ idTipoDespesa: 4, nome: 'Energia Elétrica' }));
    http.patch.and.returnValue(of({ idDespesa: 12, dataPagamento: '2026-11-17' }));

    await service.listTiposDespesa().toPromise();
    await service.createTipoDespesa({ nome: 'Energia' }).toPromise();
    await service.updateTipoDespesa(4, { nome: 'Energia Elétrica' }).toPromise();
    await service.pagarDespesa(12, '2026-11-17').toPromise();
    await service.pagarDespesa(13).toPromise();

    expect(http.get.calls.mostRecent().args[0]).toBe('http://api.test/api/despesas/tipos');
    expect(http.post.calls.mostRecent().args).toEqual(['http://api.test/api/despesas/tipos', { nome: 'Energia' }]);
    expect(http.put.calls.mostRecent().args).toEqual(['http://api.test/api/despesas/tipos/4', { nome: 'Energia Elétrica' }]);
    expect(http.patch.calls.all()[0].args).toEqual(['http://api.test/api/despesas/12/pagar', { dataPagamento: '2026-11-17' }]);
    expect(http.patch.calls.all()[1].args).toEqual(['http://api.test/api/despesas/13/pagar', {}]);
  });

  it('consulta DRE Gerencial, lançamentos drill-down, evolução mensal, comparativo de filiais e exportação CSV/JSON', async () => {
    http.get.and.returnValue(of({}));

    await service.getDreGerencial({
      tipoPeriodo: 'MENSAL',
      competencia: '2026-11',
      compararPeriodoAnterior: true,
      centroCustoId: 3,
      consolidarFiliais: false
    }).toPromise();

    await service.getDreLancamentos({
      tipoPeriodo: 'MENSAL',
      competencia: '2026-11',
      numeroLinha: 8,
      planoContaId: 12
    }).toPromise();

    await service.getDreEvolucao({
      competenciaFim: '2026-11',
      meses: 6,
      consolidarFiliais: true
    }).toPromise();

    await service.getDreComparativoFiliais({
      tipoPeriodo: 'TRIMESTRAL',
      ano: 2026,
      trimestre: 4
    }).toPromise();

    await service.exportarDreCsv({ tipoPeriodo: 'MENSAL', competencia: '2026-11' }).toPromise();
    await service.exportarDreJson({ tipoPeriodo: 'MENSAL', competencia: '2026-11' }).toPromise();

    const calls = http.get.calls.all();
    expect(calls[0].args[0]).toBe('http://api.test/api/financeiro/dre');
    expect((calls[0].args[1] as { params: HttpParams }).params.get('competencia')).toBe('2026-11');
    expect((calls[0].args[1] as { params: HttpParams }).params.get('centroCustoId')).toBe('3');

    expect(calls[1].args[0]).toBe('http://api.test/api/financeiro/dre/lancamentos');
    expect((calls[1].args[1] as { params: HttpParams }).params.get('numeroLinha')).toBe('8');
    expect((calls[1].args[1] as { params: HttpParams }).params.get('planoContaId')).toBe('12');

    expect(calls[2].args[0]).toBe('http://api.test/api/financeiro/dre/evolucao');
    expect((calls[2].args[1] as { params: HttpParams }).params.get('meses')).toBe('6');

    expect(calls[3].args[0]).toBe('http://api.test/api/financeiro/dre/comparativo-filiais');
    expect((calls[3].args[1] as { params: HttpParams }).params.get('trimestre')).toBe('4');

    expect(calls[4].args[0]).toBe('http://api.test/api/financeiro/dre/exportar');
    expect((calls[4].args[1] as { params: HttpParams }).params.get('formato')).toBe('CSV');

    expect(calls[5].args[0]).toBe('http://api.test/api/financeiro/dre/exportar');
    expect((calls[5].args[1] as { params: HttpParams }).params.get('formato')).toBe('JSON');
  });

  it('consulta e opera a Central de Fluxo de Caixa, Parcelas, Transferências, Estornos e Conciliação (Lote 3)', async () => {
    http.get.and.returnValue(of({}));
    http.post.and.returnValue(of({}));
    http.put.and.returnValue(of({}));

    await service.getFluxoCaixaPainel({
      dataInicio: '2026-11-01',
      dataFim: '2026-11-30',
      contaFinanceiraId: 2,
      centroCustoId: 4,
      consolidarFiliais: true
    }).toPromise();

    await service.simularFluxoCaixa(
      { saidaSimuladaAdicional: 1200, simularAntecipacaoCartao: true },
      { dataInicio: '2026-11-01', dataFim: '2026-11-30' }
    ).toPromise();

    await service.listContasFinanceiras(true).toPromise();
    await service.bootstrapContasFinanceiras().toPromise();
    await service.createContaFinanceira({
      codigo: 'BANCO-01',
      nome: 'Conta Movimento',
      tipoConta: 'CONTA_CORRENTE',
      saldoInicial: 1000
    }).toPromise();
    await service.updateContaFinanceira(5, {
      codigo: 'BANCO-01',
      nome: 'Conta Movimento Atualizada',
      tipoConta: 'CONTA_CORRENTE'
    }).toPromise();

    await service.gerarParcelasFluxoCaixa(
      {
        natureza: 'RECEBER',
        descricao: 'Venda Cartão',
        valorTotal: 900,
        quantidadeParcelas: 3,
        intervaloDias: 30,
        primeiroVencimento: '2026-12-01'
      },
      'idem-parc-1'
    ).toPromise();

    await service.baixarContaReceber(11, { valorLiquidado: 300, valorTaxa: 9 }, 'idem-rec-11').toPromise();
    await service.baixarContaPagar(22, { valorLiquidado: 450 }, 'idem-pag-22').toPromise();
    await service.transferirEntreContas(
      { contaOrigemId: 1, contaDestinoId: 2, valor: 250 },
      'idem-transf-1'
    ).toPromise();
    await service.estornarMovimentacaoFinanceira(33, { motivo: 'Erro de digitação' }, 'idem-est-33').toPromise();

    await service.getConciliacaoFinanceira(2).toPromise();
    await service.importarExtratoConciliacao(
      {
        contaFinanceiraId: 2,
        tipoConciliacao: 'BANCARIA',
        formatoOrigem: 'CSV',
        conteudoArquivo: '2026-11-17;PIX;100.00;CREDITO;ID-1'
      },
      'idem-conc-1'
    ).toPromise();
    await service.conciliarManualmente({
      conciliacaoItemId: 44,
      novoStatus: 'CONCILIADO_MANUAL'
    }).toPromise();

    const getCalls = http.get.calls.all();
    expect(getCalls[0].args[0]).toBe('http://api.test/api/financeiro/fluxo-caixa');
    expect((getCalls[0].args[1] as { params: HttpParams }).params.get('contaFinanceiraId')).toBe('2');
    expect((getCalls[0].args[1] as { params: HttpParams }).params.get('consolidarFiliais')).toBe('true');
    expect(getCalls[1].args[0]).toBe('http://api.test/api/financeiro/fluxo-caixa/contas');
    expect(getCalls[2].args[0]).toBe('http://api.test/api/financeiro/fluxo-caixa/conciliacao');

    const postUrls = http.post.calls.all().map((c) => c.args[0]);
    expect(postUrls).toEqual([
      'http://api.test/api/financeiro/fluxo-caixa/simular',
      'http://api.test/api/financeiro/fluxo-caixa/contas/bootstrap',
      'http://api.test/api/financeiro/fluxo-caixa/contas',
      'http://api.test/api/financeiro/fluxo-caixa/parcelas/gerar',
      'http://api.test/api/financeiro/fluxo-caixa/contas-receber/11/baixar',
      'http://api.test/api/financeiro/fluxo-caixa/contas-pagar/22/baixar',
      'http://api.test/api/financeiro/fluxo-caixa/transferencias',
      'http://api.test/api/financeiro/fluxo-caixa/movimentacoes/33/estornar',
      'http://api.test/api/financeiro/fluxo-caixa/conciliacao/importar',
      'http://api.test/api/financeiro/fluxo-caixa/conciliacao/conciliar'
    ]);
    expect(http.put.calls.mostRecent().args[0]).toBe('http://api.test/api/financeiro/fluxo-caixa/contas/5');
  });

  it('integra endpoints do Centro de Inteligência Administrativa (Lote 4)', async () => {
    http.get.and.returnValue(of({}));
    http.post.and.returnValue(of({}));
    http.put.and.returnValue(of({}));

    await service.getIndicadoresPainel({ competencia: '2026-11', consolidarFiliais: true }).toPromise();
    await service.getIndicadoresParametros().toPromise();
    await service.getBalancoPatrimonialGerencial('2026-11').toPromise();
    await service.salvarIndicadoresParametros({} as never).toPromise();
    await service.salvarBalancoPatrimonialGerencial({ competenciaAnoMes: '2026-11' }).toPromise();
    await service.simularPrecificacaoInteligente({ custoAquisicaoBase: 100 }).toPromise();
    await service.aplicarPrecoProdutoAutorizado({
      produtoId: 10,
      novoValorVenda: 166.67,
      justificativa: 'Ajuste P0'
    }).toPromise();
    await service.simularInvestimentoIndicadores({ investimentoInicial: 10000 }, '2026-11').toPromise();

    expect(http.get.calls.all().map((c) => c.args[0])).toEqual([
      'http://api.test/api/financeiro/indicadores',
      'http://api.test/api/financeiro/indicadores/parametros',
      'http://api.test/api/financeiro/indicadores/balanco-patrimonial'
    ]);
    expect(http.put.calls.all().map((c) => c.args[0])).toEqual([
      'http://api.test/api/financeiro/indicadores/parametros',
      'http://api.test/api/financeiro/indicadores/balanco-patrimonial'
    ]);
    expect(http.post.calls.all().map((c) => c.args[0])).toEqual([
      'http://api.test/api/financeiro/indicadores/precificacao/simular',
      'http://api.test/api/financeiro/indicadores/precificacao/aplicar-preco',
      'http://api.test/api/financeiro/indicadores/investimentos/simular'
    ]);
  });
});
