import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { addIcons } from 'ionicons';
import {
  addOutline,
  albumsOutline,
  banOutline,
  businessOutline,
  calendarOutline,
  cashOutline,
  checkmarkCircleOutline,
  createOutline,
  filterOutline,
  layersOutline,
  receiptOutline,
  syncOutline,
  trendingUpOutline,
  warningOutline
} from 'ionicons/icons';
import { of } from 'rxjs';

import { ApexApiService } from '../core/apex-api.service';
import {
  ApexRole,
  CentroCustoResponse,
  ConsolidacaoCompetenciaResponse,
  PendenciaClassificacaoResponse,
  PlanoContasResponse,
  RegraClassificacaoResponse
} from '../core/models';
import { SessionService } from '../core/session.service';
import { FinanceFoundationPage } from './finance-foundation.page';

registerLocaleData(localePt);

describe('FinanceFoundationPage', () => {
  let activeRoles: ApexRole[];
  let api: jasmine.SpyObj<ApexApiService>;
  let fixture: ComponentFixture<FinanceFoundationPage>;
  let component: FinanceFoundationPage;

  const mockContas: PlanoContasResponse[] = [
    {
      idPlanoConta: 1,
      contaPaiId: null,
      codigo: '4.1',
      nome: 'Despesas Operacionais',
      tipoConta: 'DESPESA',
      natureza: 'DEBITO',
      classificacaoCusto: 'FIXO',
      grupoDre: 'DESPESA_OPERACIONAL',
      ordemExibicao: 1,
      aceitaLancamento: false,
      sistemaPadrao: true,
      ativo: true,
      filhas: [
        {
          idPlanoConta: 2,
          contaPaiId: 1,
          codigo: '4.1.01',
          nome: 'Aluguel Loja',
          tipoConta: 'DESPESA',
          natureza: 'DEBITO',
          classificacaoCusto: 'FIXO',
          grupoDre: 'DESPESA_INFRAESTRUTURA',
          ordemExibicao: 2,
          aceitaLancamento: true,
          sistemaPadrao: true,
          ativo: true
        }
      ]
    },
    {
      idPlanoConta: 2,
      contaPaiId: 1,
      codigo: '4.1.01',
      nome: 'Aluguel Loja',
      tipoConta: 'DESPESA',
      natureza: 'DEBITO',
      classificacaoCusto: 'FIXO',
      grupoDre: 'DESPESA_INFRAESTRUTURA',
      ordemExibicao: 2,
      aceitaLancamento: true,
      sistemaPadrao: true,
      ativo: true
    }
  ];

  const mockCentros: CentroCustoResponse[] = [
    {
      idCentroCusto: 10,
      codigo: 'CC-LOJA',
      nome: 'Loja Matriz',
      tipoOperacional: 'LOJA',
      ativo: true
    }
  ];

  const mockRegras: RegraClassificacaoResponse[] = [
    {
      idRegraClassificacao: 50,
      nome: 'Aluguel Matriz',
      origemTipo: 'DESPESA',
      palavraChave: 'ALUGUEL',
      planoContaId: 2,
      planoContaCodigo: '4.1.01',
      planoContaNome: 'Aluguel Loja',
      centroCustoId: 10,
      centroCustoNome: 'Loja Matriz',
      prioridade: 10,
      ativo: true
    }
  ];

  const mockPendencias: PendenciaClassificacaoResponse = {
    totalPendentes: 2,
    despesasPendentes: 1,
    contasPagarPendentes: 1,
    contasReceberPendentes: 0,
    vendasPendentes: 0,
    itens: [
      {
        origemTipo: 'DESPESA',
        registroId: 700,
        descricao: 'Aluguel Novembro',
        valor: 2500,
        dataCompetencia: '2026-11-05',
        sugestaoPlanoContaId: 2,
        sugestaoPlanoContaNome: '4.1.01 - Aluguel Loja',
        sugestaoCentroCustoId: 10
      }
    ]
  };

  const mockConsolidacao: ConsolidacaoCompetenciaResponse = {
    competenciaAnoMes: '2026-11',
    inicioPeriodo: '2026-11-01',
    fimPeriodo: '2026-11-30',
    receitasCompetencia: 30000,
    cmvCompetencia: 14000,
    despesasCompetencia: 6000,
    resultadoCompetencia: 10000,
    entradasRealizadasCaixa: 24000,
    saidasRealizadasCaixa: 5500,
    saldoRealizadoCaixa: 18500,
    lancamentosSemPlanoConta: 1,
    valorSemPlanoConta: 2500,
    avisosLimitacao: ['Valores sem conta gerencial não entram no DRE agrupado.'],
    linhas: [
      {
        planoContaId: 2,
        planoContaCodigo: '4.1.01',
        planoContaNome: 'Aluguel Loja',
        grupoDre: 'DESPESA_INFRAESTRUTURA',
        classificacaoCusto: 'FIXO',
        centroCustoId: 10,
        centroCustoCodigo: 'CC-LOJA',
        centroCustoNome: 'Loja Matriz',
        valorCompetencia: 2500,
        valorRealizadoCaixa: 2500,
        quantidadeLancamentos: 1
      }
    ]
  };

  beforeAll(() => {
    addIcons({
      'add-outline': addOutline,
      'albums-outline': albumsOutline,
      'ban-outline': banOutline,
      'business-outline': businessOutline,
      'calendar-outline': calendarOutline,
      'cash-outline': cashOutline,
      'checkmark-circle-outline': checkmarkCircleOutline,
      'create-outline': createOutline,
      'filter-outline': filterOutline,
      'layers-outline': layersOutline,
      'receipt-outline': receiptOutline,
      'sync-outline': syncOutline,
      'trending-up-outline': trendingUpOutline,
      'warning-outline': warningOutline
    });
  });

  function createPage(roles: ApexRole[] = ['ROLE_FINANCEIRO']): void {
    activeRoles = roles;
    api = jasmine.createSpyObj<ApexApiService>('ApexApiService', [
      'listPlanoContas',
      'getPlanoContasTree',
      'bootstrapPlanoContasPadrao',
      'createPlanoConta',
      'updatePlanoConta',
      'togglePlanoContaStatus',
      'listCentrosCusto',
      'bootstrapCentrosCustoPadrao',
      'createCentroCusto',
      'updateCentroCusto',
      'toggleCentroCustoStatus',
      'listRegrasClassificacao',
      'createRegraClassificacao',
      'updateRegraClassificacao',
      'toggleRegraClassificacaoStatus',
      'getPendenciasClassificacao',
      'aplicarClassificacaoAutomatica',
      'reclassificarManualmente',
      'getConsolidacaoCompetencia',
      'recalcularConsolidacaoCompetencia',
      'listTiposDespesa',
      'paymentMethods',
      'list'
    ]);

    api.listPlanoContas.and.returnValue(of(mockContas));
    api.getPlanoContasTree.and.returnValue(of([mockContas[0]]));
    api.bootstrapPlanoContasPadrao.and.returnValue(
      of({ contasCriadas: 12, contasJaExistentes: 2, totalContas: 14, contas: mockContas })
    );
    api.createPlanoConta.and.returnValue(of(mockContas[1]));
    api.updatePlanoConta.and.returnValue(of(mockContas[1]));
    api.togglePlanoContaStatus.and.returnValue(of({ ...mockContas[1], ativo: false }));

    api.listCentrosCusto.and.returnValue(of(mockCentros));
    api.bootstrapCentrosCustoPadrao.and.returnValue(of(mockCentros));
    api.createCentroCusto.and.returnValue(of(mockCentros[0]));
    api.updateCentroCusto.and.returnValue(of(mockCentros[0]));
    api.toggleCentroCustoStatus.and.returnValue(of({ ...mockCentros[0], ativo: false }));

    api.listRegrasClassificacao.and.returnValue(of(mockRegras));
    api.createRegraClassificacao.and.returnValue(of(mockRegras[0]));
    api.updateRegraClassificacao.and.returnValue(of(mockRegras[0]));
    api.toggleRegraClassificacaoStatus.and.returnValue(of({ ...mockRegras[0], ativo: false }));
    api.getPendenciasClassificacao.and.returnValue(of(mockPendencias));
    api.aplicarClassificacaoAutomatica.and.returnValue(
      of({ processados: 2, classificados: 2, naoClassificados: 0, observacoes: ['Tudo classificado'] })
    );
    api.reclassificarManualmente.and.returnValue(of({ ok: true }));

    api.getConsolidacaoCompetencia.and.returnValue(of(mockConsolidacao));
    api.recalcularConsolidacaoCompetencia.and.returnValue(of(mockConsolidacao));

    api.listTiposDespesa.and.returnValue(of([]));
    api.paymentMethods.and.returnValue(of([]));
    api.list.and.returnValue(of([]));

    const sessionStub = {
      hasAnyRole: (required?: readonly ApexRole[]) => {
        if (!required || required.length === 0) {
          return true;
        }
        const held = new Set(activeRoles);
        return required.some((role) => held.has(role));
      }
    };

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [FinanceFoundationPage],
      providers: [
        { provide: ApexApiService, useValue: api },
        { provide: SessionService, useValue: sessionStub }
      ]
    });

    fixture = TestBed.createComponent(FinanceFoundationPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('carrega dados iniciais e agrupa Plano de Contas por grupo DRE e modo árvore', () => {
    createPage(['ROLE_FINANCEIRO']);

    expect(component.canMutate()).toBeTrue();
    expect(component.isAuditorReadOnly()).toBeFalse();
    expect(component.groupedPlanoContas().length).toBe(2);
    expect(component.filteredTreeNodes().map((n) => n.depth)).toEqual([0, 1]);

    component.setPlanoViewMode('tree');
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('4.1.01 · Aluguel Loja');
  });

  it('inicializa Plano Padrão, cria/edita conta e alterna status na aba Plano de Contas', () => {
    createPage(['ROLE_GERENTE']);

    component.bootstrapPlanoContas();
    expect(api.bootstrapPlanoContasPadrao).toHaveBeenCalledTimes(1);
    expect(component.feedbackMessage()).toContain('12 criada(s)');

    component.planoForm.patchValue({
      codigo: '4.2.01',
      nome: 'Marketing Digital',
      tipoConta: 'DESPESA',
      natureza: 'DEBITO',
      classificacaoCusto: 'VARIAVEL',
      grupoDre: 'DESPESA_COMERCIAL'
    });
    component.savePlanoConta();
    expect(api.createPlanoConta).toHaveBeenCalledWith(jasmine.objectContaining({ codigo: '4.2.01', nome: 'Marketing Digital' }));

    component.editPlanoConta(mockContas[1]);
    component.savePlanoConta();
    expect(api.updatePlanoConta).toHaveBeenCalledWith(2, jasmine.any(Object));

    component.togglePlanoStatus(mockContas[1]);
    expect(api.togglePlanoContaStatus).toHaveBeenCalledWith(2, false);
  });

  it('gerencia Centros de Custo com bootstrap, criação e alternância de status', () => {
    createPage(['ROLE_ADMIN']);
    component.setTab('centros-custo');
    fixture.detectChanges();

    component.bootstrapCentrosCusto();
    expect(api.bootstrapCentrosCustoPadrao).toHaveBeenCalledTimes(1);

    component.centroForm.patchValue({
      codigo: 'CC-LOG',
      nome: 'Logística',
      tipoOperacional: 'LOGISTICA'
    });
    component.saveCentroCusto();
    expect(api.createCentroCusto).toHaveBeenCalledWith(jasmine.objectContaining({ codigo: 'CC-LOG', tipoOperacional: 'LOGISTICA' }));

    component.toggleCentroStatus(mockCentros[0]);
    expect(api.toggleCentroCustoStatus).toHaveBeenCalledWith(10, false);
  });

  it('exibe pendências, executa classificação automática, reclassifica manualmente e salva regras', () => {
    createPage(['ROLE_CONTADOR']);
    component.setTab('classificacao');
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Aluguel Novembro');
    expect(text).toContain('Aluguel Matriz');

    component.aplicarClassificacaoAutomatica();
    expect(api.aplicarClassificacaoAutomatica).toHaveBeenCalledWith(undefined);
    expect(component.ultimoLoteResultado()?.classificados).toBe(2);

    const pendingItem = mockPendencias.itens[0];
    component.updatePendingDraft(pendingItem, { motivo: 'Conferido no fechamento' });
    component.reclassificarItem(pendingItem);
    expect(api.reclassificarManualmente).toHaveBeenCalledWith({
      origemTipo: 'DESPESA',
      registroId: 700,
      planoContaId: 2,
      centroCustoId: 10,
      motivo: 'Conferido no fechamento'
    });

    component.regraForm.patchValue({
      nome: 'Regra Energia',
      origemTipo: 'DESPESA',
      palavraChave: 'CEMIG',
      planoContaId: 2,
      centroCustoId: 10,
      prioridade: 20
    });
    component.saveRegra();
    expect(api.createRegraClassificacao).toHaveBeenCalledWith(
      jasmine.objectContaining({ nome: 'Regra Energia', planoContaId: 2, centroCustoId: 10 })
    );
  });

  it('exibe consolidação competência vs caixa, banner de não classificados e recalcula competência', () => {
    createPage(['ROLE_FINANCAS']);
    component.setTab('competencia');
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('1 lançamento(s) sem plano de contas');
    expect(text).toContain('Valores sem conta gerencial não entram no DRE agrupado.');
    expect(text).toContain('Regime de Competência (2026-11)');
    expect(text).toContain('Regime de Caixa Realizado');

    component.recalcularCompetencia();
    expect(api.recalcularConsolidacaoCompetencia).toHaveBeenCalledWith(component.competencia());
    expect(component.feedbackMessage()).toContain('2026-11 recalculada');
  });

  it('mantém postura somente leitura para ROLE_AUDITOR ocultando e bloqueando mutações', () => {
    createPage(['ROLE_AUDITOR']);

    expect(component.canMutate()).toBeFalse();
    expect(component.isAuditorReadOnly()).toBeTrue();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Modo Auditor (somente leitura)');
    expect(text).not.toContain('Inicializar Plano Padrão');
    expect(text).not.toContain('Nova Conta Gerencial');

    component.bootstrapPlanoContas();
    component.savePlanoConta();
    component.bootstrapCentrosCusto();
    component.aplicarClassificacaoAutomatica();
    component.recalcularCompetencia();

    expect(api.bootstrapPlanoContasPadrao).not.toHaveBeenCalled();
    expect(api.createPlanoConta).not.toHaveBeenCalled();
    expect(api.bootstrapCentrosCustoPadrao).not.toHaveBeenCalled();
    expect(api.aplicarClassificacaoAutomatica).not.toHaveBeenCalled();
    expect(api.recalcularConsolidacaoCompetencia).not.toHaveBeenCalled();
  });
});
