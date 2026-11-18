import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { ApexApiService } from '../core/apex-api.service';
import { AuthService } from '../core/auth.service';
import {
  DreComparativoFiliaisResponse,
  DreDrillDownResponse,
  DreEvolucaoResponse,
  DreGerencialResponse
} from '../core/models';
import { FinanceDrePage } from './finance-dre.page';

describe('FinanceDrePage', () => {
  let fixture: ComponentFixture<FinanceDrePage>;
  let component: FinanceDrePage;
  let api: jasmine.SpyObj<ApexApiService>;
  const userSignal = signal<{ login: string; roles: string[] } | null>({
    login: 'gerente',
    roles: ['ROLE_GERENTE']
  });

  const sampleDre: DreGerencialResponse = {
    tipoPeriodo: 'MENSAL',
    rotuloPeriodo: 'Nov/2026',
    dataInicio: '2026-11-01',
    dataFim: '2026-11-30',
    comparacaoAtiva: true,
    rotuloPeriodoComparacao: 'Out/2026',
    comparacaoInicio: '2026-10-01',
    comparacaoFim: '2026-10-31',
    empresaId: 1,
    filialId: 1,
    filialNome: 'Matriz Centro',
    consolidadoEmpresa: false,
    centroCustoId: null,
    centroCustoNome: null,
    baseAnaliseVertical: 'Linha 5 - Receita Liquida (100%)',
    lancamentosSemClassificacao: 1,
    valorSemClassificacao: 50,
    indicadores: {
      receitaBruta: 2000,
      deducoesTotais: 400,
      receitaLiquida: 1600,
      cmv: 720,
      lucroBruto: 880,
      margemBrutaPercentual: 55,
      despesasOperacionaisTotais: 630,
      despesasFixas: 450,
      despesasVariaveis: 180,
      custosVariaveisTotais: 900,
      margemContribuicaoValor: 700,
      margemContribuicaoPercentual: 43.75,
      pontoEquilibrioContabilReceita: 1028.57,
      resultadoOperacional: 260,
      margemOperacionalPercentual: 16.25,
      resultadoFinanceiroLiquido: 20,
      resultadoAntesTributos: 280,
      tributosSobreLucro: 40,
      resultadoLiquidoGerencial: 240,
      margemLiquidaPercentual: 15,
      entradasRealizadasCaixa: 1800,
      saidasRealizadasCaixa: 600,
      saldoRealizadoCaixa: 1200,
      diferencaCompetenciaVsCaixa: -960
    },
    linhas: [
      {
        numeroLinha: 1,
        codigoLinha: 'RECEITA_BRUTA_VENDAS',
        titulo: '1. Receita Bruta de Vendas',
        tipoLinha: 'RECEITA',
        sinalDemonstracao: '+',
        gruposAssociados: ['RECEITA_BRUTA'],
        explicacaoGerencial: 'Total faturado com vendas finalizadas.',
        valor: 2000,
        analiseVerticalPercentual: 125,
        valorComparacao: 1600,
        variacaoAbsoluta: 400,
        analiseHorizontalPercentual: 25,
        statusVariacao: 'FAVORAVEL',
        quantidadeLancamentos: 5,
        contas: [
          {
            idPlanoConta: 10,
            contaPaiId: null,
            codigo: '1.01',
            nome: 'Receita de Vendas de Mercadorias',
            tipoConta: 'RECEITA',
            natureza: 'CREDITO',
            classificacaoCusto: 'NAO_APLICAVEL',
            grupoDre: 'RECEITA_BRUTA',
            sintetica: false,
            valor: 2000,
            analiseVerticalPercentual: 125,
            valorComparacao: 1600,
            variacaoAbsoluta: 400,
            analiseHorizontalPercentual: 25,
            quantidadeLancamentos: 5,
            filhas: []
          }
        ]
      },
      {
        numeroLinha: 5,
        codigoLinha: 'RECEITA_LIQUIDA',
        titulo: '5. (=) Receita Liquida Operacional',
        tipoLinha: 'SUBTOTAL',
        sinalDemonstracao: '=',
        gruposAssociados: [],
        explicacaoGerencial: 'Base 100% da Analise Vertical.',
        valor: 1600,
        analiseVerticalPercentual: 100,
        valorComparacao: 1300,
        variacaoAbsoluta: 300,
        analiseHorizontalPercentual: 23.08,
        statusVariacao: 'FAVORAVEL',
        quantidadeLancamentos: 0,
        contas: []
      }
    ],
    diagnostico: {
      situacaoPeriodo: 'LUCRO_SAUDAVEL',
      mancheteExplicativa: 'A operacao apresentou lucro liquido saudavel de R$ 240,00 (15,00% da Receita Liquida).',
      explicacaoResultado: 'O Lucro Bruto cobriu confortavelmente as despesas fixas e variaveis.',
      explicacaoCompetenciaVsCaixa: 'O saldo de caixa realizado superou o lucro contabil por recebimento de vendas.',
      explicacaoPontoEquilibrio: 'O faturamento superou o ponto de equilibrio contabil de R$ 1.028,57.',
      principaisFatores: [
        {
          categoria: 'CUSTO',
          titulo: 'CMV sob controle',
          valor: 720,
          percentualReceitaLiquida: 45,
          variacaoPercentual: 5,
          impacto: 'POSITIVO',
          explicacao: 'Representou 45% da Receita Liquida.'
        }
      ],
      recomendacoesGerenciais: ['Manter politica atual de margem e classificar pendencias.'],
      avisosLimitacao: ['Existe 1 lancamento pendente de classificacao.']
    }
  };

  const sampleDrillDown: DreDrillDownResponse = {
    rotuloPeriodo: 'Nov/2026',
    dataInicio: '2026-11-01',
    dataFim: '2026-11-30',
    numeroLinhaFiltro: 1,
    codigoLinhaFiltro: 'RECEITA_BRUTA_VENDAS',
    tituloLinhaFiltro: '1. Receita Bruta de Vendas',
    planoContaIdFiltro: 10,
    planoContaNomeFiltro: 'Receita de Vendas de Mercadorias',
    grupoDreFiltro: 'RECEITA_BRUTA',
    centroCustoIdFiltro: null,
    filialIdFiltro: 1,
    consolidadoEmpresa: false,
    totalLancamentos: 2,
    somaValorCompetencia: 2000,
    lancamentos: [
      {
        origemTipo: 'VENDA',
        registroId: 101,
        documentoReferencia: 'Venda V-101',
        descricao: 'Receita bruta de venda finalizada (Venda V-101)',
        dataCompetencia: '2026-11-10',
        dataEmissao: '2026-11-10',
        dataLiquidacaoCaixa: '2026-11-10',
        statusOperacional: 'FINALIZADA',
        valorCompetencia: 1500,
        impactoDre: 'POSITIVO',
        numeroLinhaDre: 1,
        tituloLinhaDre: '1. Receita Bruta de Vendas',
        grupoDre: 'RECEITA_BRUTA',
        planoContaId: 10,
        planoContaCodigo: '1.01',
        planoContaNome: 'Receita de Vendas de Mercadorias',
        centroCustoId: 1,
        centroCustoNome: 'Comercial e Vendas',
        filialId: 1,
        rotaDetalheFrontend: '/sales-history'
      },
      {
        origemTipo: 'DESPESA',
        registroId: 202,
        documentoReferencia: 'Despesa #202',
        descricao: 'Aluguel Loja',
        dataCompetencia: '2026-11-05',
        dataEmissao: '2026-11-05',
        dataLiquidacaoCaixa: '2026-11-05',
        statusOperacional: 'PAGA',
        valorCompetencia: 500,
        impactoDre: 'NEGATIVO',
        numeroLinhaDre: 9,
        tituloLinhaDre: '9. (-) Despesas Administrativas',
        grupoDre: 'DESPESA_ADMINISTRATIVA',
        planoContaId: 20,
        planoContaCodigo: '5.01',
        planoContaNome: 'Aluguel e Condominio',
        centroCustoId: 2,
        centroCustoNome: 'Administrativo',
        filialId: 1,
        rotaDetalheFrontend: '/expenses'
      }
    ]
  };

  const sampleEvolucao: DreEvolucaoResponse = {
    competenciaInicio: '2026-06',
    competenciaFim: '2026-11',
    quantidadeMeses: 2,
    empresaId: 1,
    filialId: 1,
    consolidadoEmpresa: false,
    centroCustoId: null,
    receitaLiquidaAcumulada: 2900,
    lucroBrutoAcumulado: 1580,
    resultadoLiquidoAcumulado: 420,
    margemLiquidaMediaPercentual: 14.48,
    tendenciaReceita: 'CRESCIMENTO',
    tendenciaResultado: 'CRESCIMENTO',
    serie: [
      {
        competencia: '2026-10',
        rotulo: 'Out/2026',
        receitaBruta: 1500,
        receitaLiquida: 1300,
        cmv: 600,
        lucroBruto: 700,
        despesasTotais: 520,
        resultadoOperacional: 180,
        resultadoLiquidoGerencial: 180,
        margemBrutaPercentual: 53.85,
        margemOperacionalPercentual: 13.85,
        margemLiquidaPercentual: 13.85,
        saldoRealizadoCaixa: 900
      },
      {
        competencia: '2026-11',
        rotulo: 'Nov/2026',
        receitaBruta: 2000,
        receitaLiquida: 1600,
        cmv: 720,
        lucroBruto: 880,
        despesasTotais: 630,
        resultadoOperacional: 280,
        resultadoLiquidoGerencial: 240,
        margemBrutaPercentual: 55,
        margemOperacionalPercentual: 17.5,
        margemLiquidaPercentual: 15,
        saldoRealizadoCaixa: 1200
      }
    ]
  };

  const sampleComparativo: DreComparativoFiliaisResponse = {
    tipoPeriodo: 'MENSAL',
    rotuloPeriodo: 'Nov/2026',
    dataInicio: '2026-11-01',
    dataFim: '2026-11-30',
    empresaId: 1,
    centroCustoId: null,
    consolidadoEmpresa: sampleDre.indicadores,
    filiais: [
      {
        filialId: 1,
        filialCodigo: 'F01',
        filialNome: 'Matriz Centro',
        receitaBruta: 2000,
        receitaLiquida: 1600,
        cmv: 720,
        lucroBruto: 880,
        despesasTotais: 630,
        resultadoOperacional: 280,
        resultadoLiquidoGerencial: 240,
        margemBrutaPercentual: 55,
        margemLiquidaPercentual: 15,
        participacaoReceitaLiquidaPercentual: 100,
        participacaoResultadoPercentual: 100,
        linhas: []
      }
    ],
    destaquesComparativos: []
  };

  beforeEach(async () => {
    userSignal.set({ login: 'gerente', roles: ['ROLE_GERENTE'] });
    api = jasmine.createSpyObj<ApexApiService>('ApexApiService', [
      'listCentrosCusto',
      'getDreGerencial',
      'getDreLancamentos',
      'getDreEvolucao',
      'getDreComparativoFiliais',
      'exportarDreCsv',
      'exportarDreJson'
    ]);
    api.listCentrosCusto.and.returnValue(
      of([
        {
          idCentroCusto: 1,
          empresaId: 1,
          filialId: 1,
          codigo: 'CC-LOJA',
          nome: 'Comercial e Vendas',
          tipoOperacional: 'COMERCIAL',
          ativo: true
        }
      ])
    );
    api.getDreGerencial.and.returnValue(of(sampleDre));
    api.getDreLancamentos.and.returnValue(of(sampleDrillDown));
    api.getDreEvolucao.and.returnValue(of(sampleEvolucao));
    api.getDreComparativoFiliais.and.returnValue(of(sampleComparativo));
    api.exportarDreCsv.and.returnValue(of('# CSV DRE'));
    api.exportarDreJson.and.returnValue(of(sampleDre));

    await TestBed.configureTestingModule({
      imports: [FinanceDrePage],
      providers: [
        provideRouter([]),
        { provide: ApexApiService, useValue: api },
        { provide: AuthService, useValue: { user: userSignal } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(FinanceDrePage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('carrega DRE Gerencial, centros de custo, drill-down, evolução e comparativo de filiais na inicialização', () => {
    expect(api.listCentrosCusto).toHaveBeenCalledWith(true);
    expect(api.getDreGerencial).toHaveBeenCalled();
    expect(component.dre()?.indicadores.receitaLiquida).toBe(1600);
    expect(component.dre()?.indicadores.resultadoLiquidoGerencial).toBe(240);
    expect(component.centrosCusto().length).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('DRE Gerencial Configurável (17 Linhas)');
    expect(fixture.nativeElement.textContent).toContain('Diagnóstico Econômico do Período');
  });

  it('permite expandir/recolher linhas e realizar drill-down por linha e por conta gerencial', () => {
    const linha1 = sampleDre.linhas[0];
    const conta1 = linha1.contas[0];

    component.rastrearLinha(linha1);
    expect(component.abaAtiva()).toBe('lancamentos');
    expect(api.getDreLancamentos).toHaveBeenCalledWith(jasmine.objectContaining({ numeroLinha: 1 }));

    component.rastrearConta(linha1, conta1);
    expect(api.getDreLancamentos).toHaveBeenCalledWith(
      jasmine.objectContaining({ numeroLinha: 1, planoContaId: 10 })
    );

    component.filtroOrigemDrillDown.set('VENDA');
    expect(component.lancamentosFiltrados().length).toBe(1);

    component.buscaDrillDown.set('V-101');
    expect(component.lancamentosFiltrados().length).toBe(1);

    component.limparFiltroDrillDown();
    expect(component.filtroOrigemDrillDown()).toBe('');
    expect(component.buscaDrillDown()).toBe('');
  });

  it('alterna períodos (TRIMESTRAL, SEMESTRAL, ANUAL, PERSONALIZADO), evolução e exporta CSV/JSON', () => {
    component.onTipoPeriodoChange('TRIMESTRAL');
    component.ano.set(2026);
    component.trimestre.set(2);
    component.atualizarTudo();

    expect(api.getDreGerencial).toHaveBeenCalledWith(
      jasmine.objectContaining({ tipoPeriodo: 'TRIMESTRAL', ano: 2026, trimestre: 2 })
    );

    component.alterarMesesEvolucao(12);
    expect(api.getDreEvolucao).toHaveBeenCalledWith(jasmine.objectContaining({ meses: 12 }));

    component.exportar('CSV');
    expect(api.exportarDreCsv).toHaveBeenCalled();
    expect(component.mensagemSucesso()).toContain('CSV');

    component.exportar('JSON');
    expect(api.exportarDreJson).toHaveBeenCalled();
    expect(component.mensagemSucesso()).toContain('JSON');
  });

  it('aplica postura somente-leitura para ROLE_AUDITOR bloqueando botões de exportação', () => {
    userSignal.set({ login: 'auditor', roles: ['ROLE_AUDITOR'] });
    fixture.detectChanges();

    expect(component.isReadOnlyAuditor()).toBeTrue();
    expect(component.canExport()).toBeFalse();

    api.exportarDreCsv.calls.reset();
    component.exportar('CSV');
    expect(api.exportarDreCsv).not.toHaveBeenCalled();
  });

  it('exibe mensagem de erro amigável quando a apuração falha e navega para origem do lançamento', () => {
    const router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.returnValue(Promise.resolve(true));

    component.navegarParaOrigem(sampleDrillDown.lancamentos[0]);
    expect(router.navigateByUrl).toHaveBeenCalledWith('/sales-history');

    api.getDreGerencial.and.returnValue(throwError(() => ({ error: { message: 'Competência inválida' } })));
    component.atualizarTudo();
    expect(component.erro()).toBe('Competência inválida');
  });
});
