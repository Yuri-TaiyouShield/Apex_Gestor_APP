import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { ApexApiService } from '../core/apex-api.service';
import { AuthService } from '../core/auth.service';
import {
  PainelInteligenciaAdministrativaResponse,
  Produto,
  SimulacaoPrecificacaoResponse
} from '../core/models';
import { FinanceIndicatorsPage } from './finance-indicators.page';

describe('FinanceIndicatorsPage (Lote 4)', () => {
  let fixture: ComponentFixture<FinanceIndicatorsPage>;
  let component: FinanceIndicatorsPage;
  let apiSpy: jasmine.SpyObj<ApexApiService>;
  let currentRoles: string[];

  const mockPrecificacao: SimulacaoPrecificacaoResponse = {
    produtoId: 10,
    produtoDescricao: 'Fone ANC Pro',
    custoAquisicaoBase: 90,
    custoAquisicaoEfetivoTotal: 100,
    percentualDespesasVariaveisTotal: 15,
    margemLucroDesejadaPercentual: 25,
    markupDivisor: 0.6,
    markupMultiplicador: 1.6667,
    precoMinimoEquilibrio: 117.65,
    precoRecomendado: 166.67,
    precoPraticadoAtual: 150,
    margemRealPrecoAtualValor: 27.5,
    margemRealPrecoAtualPercentual: 18.33,
    markupRealPrecoAtual: 1.5,
    precoPromocionalSimulado: 135,
    margemPromocionalValor: 14.75,
    margemPromocionalPercentual: 10.93,
    aumentoVolumeNecessarioParaMesmoLucroPercentual: 86.44,
    custoComReajusteSimulado: 105,
    precoRecomendadoAposReajusteCusto: 175,
    margemSeMantiverPrecoAtualAposReajustePercentual: 15,
    situacaoPrecoAtual: 'ABAIXO_MARGEM_DESEJADA',
    diferencaMarkupVsMargemExplicacao: 'Markup incide sobre o custo; margem mede o lucro sobre o preço final.',
    recomendacoesPrecificacao: ['Ajustar preço para R$ 166,67 para atingir a meta de 25%.'],
    simulacaoReadOnly: true
  };

  const mockPainel: PainelInteligenciaAdministrativaResponse = {
    competencia: '2026-11',
    dataInicio: '2026-11-01',
    dataFim: '2026-11-30',
    empresaId: 1,
    filialId: 1,
    filialNome: 'Filial Matriz',
    consolidadoEmpresa: false,
    geradoEm: '2026-11-18T01:30:00',
    parametrosVigentes: {
      segmentoNegocio: 'VAREJO_GERAL',
      retornoMinimoMensalPercentual: 1.5,
      capitalInvestidoReferencia: 20000,
      taxaDescontoAnualPercentual: 12,
      margemContribuicaoMinimaPercentual: 25,
      margemLiquidaMinimaPercentual: 8,
      markupMargemDesejadaPadrao: 25,
      aliquotaTributosVendaPadrao: 10,
      taxaCartaoMediaPadrao: 3,
      comissaoVendaPadrao: 2,
      freteVariavelVendaPadrao: 0,
      cicloFinanceiroMaximoDias: 45,
      pmeMaximoDias: 60,
      liquidezCorrenteMinima: 1.2,
      liquidezSecaMinima: 0.9,
      liquidezImediataMinima: 0.3,
      endividamentoGeralMaximoPercentual: 60
    },
    balancoPatrimonial: {
      empresaId: 1,
      filialId: 1,
      competenciaAnoMes: '2026-11',
      dataReferencia: '2026-11-30',
      disponibilidadesCaixaBancos: 15000,
      contasReceberOperacionais: 20000,
      estoquesOperacionais: 20000,
      adiantamentosOperacionais: 0,
      despesasAntecipadas: 0,
      outrosAtivosCirculantes: 0,
      ativoCirculanteTotal: 55000,
      realizavelLongoPrazo: 0,
      investimentosLongoPrazo: 0,
      imobilizadoBruto: 0,
      depreciacaoAcumulada: 0,
      imobilizadoLiquido: 0,
      intangivel: 0,
      ativoNaoCirculanteTotal: 0,
      ativoTotal: 55000,
      fornecedoresOperacionaisCp: 15000,
      obrigacoesTributariasTrabalhistasCp: 0,
      outrasObrigacoesOperacionaisCp: 0,
      emprestimosFinanciamentosCp: 0,
      passivoCirculanteTotal: 15000,
      emprestimosFinanciamentosLp: 0,
      outrasObrigacoesLp: 0,
      passivoNaoCirculanteTotal: 0,
      passivoExigivelTotal: 15000,
      capitalSocial: 0,
      reservasCapitalLucros: 0,
      lucrosPrejuizosAcumulados: 0,
      patrimonioLiquidoTotal: 0,
      passivoMaisPatrimonioLiquido: 15000,
      diferencaEquilibrioPatrimonial: 40000,
      depreciacaoAmortizacaoPeriodo: 0,
      amortizacaoPrincipalDividaPeriodo: 0,
      sincronizadoOperacional: true,
      confirmadoGestor: false,
      consistenteContabilmente: false
    },
    rentabilidade: {
      receitaBrutaTotal: 3000,
      deducoesTributosVendasTotal: 300,
      receitaLiquidaTotal: 2700,
      cmvTotal: 1620,
      despesasVariaveisDre: 180,
      taxasCartaoAntecipacaoPeriodo: 90,
      custosDespesasVariaveisTotais: 1800,
      margemContribuicaoTotalValor: 900,
      margemContribuicaoTotalPercentual: 33.33,
      custosDespesasFixasTotais: 450,
      resultadoOperacionalDre: 450,
      resultadoLiquidoDre: 450,
      margemLiquidaPercentual: 16.67,
      porProduto: [
        {
          tipoAgrupamento: 'PRODUTO',
          referenciaId: 10,
          codigo: 'PRD-10',
          nome: 'Fone ANC Pro',
          categoriaNome: 'Eletrônicos',
          quantidadeVendidaLiquida: 10,
          receitaBruta: 2000,
          deducoesTributosDescontos: 200,
          receitaLiquida: 1800,
          cmvTotal: 800,
          taxasCartaoAlocadas: 60,
          comissoesFretesVariaveis: 60,
          outrasDespesasVariaveis: 0,
          custosDespesasVariaveisTotais: 920,
          margemContribuicaoValor: 880,
          margemContribuicaoPercentual: 48.89,
          margemContribuicaoUnitariaMedia: 88,
          participacaoFaturamentoPercentual: 66.67,
          participacaoMargemTotalPercentual: 97.78,
          classificacaoRentabilidade: 'ESTRELA_ALTA_CONTRIBUICAO',
          recomendacaoItem: 'Manter estoque.'
        }
      ],
      porCategoria: [],
      porFilial: [],
      alertasAltoVolumeBaixaMargem: []
    },
    pontoEquilibrio: {
      receitaLiquidaAtual: 2700,
      margemContribuicaoTotal: 900,
      indiceMargemContribuicaoDecimal: 0.3333,
      indiceMargemContribuicaoPercentual: 33.33,
      quantidadeTotalVendidaMix: 20,
      precoMedioPonderadoMix: 135,
      margemContribuicaoUnitariaMediaMix: 45,
      custosDespesasFixasContabeis: 450,
      despesasNaoDesembolsaveisDepreciacao: 90,
      amortizacaoPrincipalDividasPeriodo: 180,
      desembolsosFixosFinanceiros: 540,
      capitalInvestidoBase: 20000,
      taxaRetornoMinimoPeriodoPercentual: 0.9,
      remuneracaoMinimaCapitalValor: 180,
      pontoEquilibrioContabilReceita: 1350,
      pontoEquilibrioContabilUnidadesMix: 10,
      pontoEquilibrioFinanceiroReceita: 1620,
      pontoEquilibrioFinanceiroUnidadesMix: 12,
      pontoEquilibrioEconomicoReceita: 1890,
      pontoEquilibrioEconomicoUnidadesMix: 14,
      margemSegurancaContabilValor: 1350,
      margemSegurancaContabilPercentual: 50,
      grauAlavancagemOperacional: 2,
      metodologiaExplicita: 'PEC = CF / IMC',
      statusConfiabilidade: 'CALCULADO',
      porFilial: [],
      porCategoria: []
    },
    precificacaoPadrao: mockPrecificacao,
    capitalGiro: {
      disponibilidadesCaixaBancos: 15000,
      contasReceberOperacionais: 20000,
      estoquesOperacionais: 20000,
      adiantamentosDespesasAntecipadas: 0,
      ativoCirculanteOperacional: 40000,
      ativoCirculanteTotal: 55000,
      fornecedoresContasPagarOperacionais: 15000,
      obrigacoesTributariasTrabalhistasOperacionais: 0,
      passivoCirculanteOperacional: 15000,
      emprestimosFinanciamentosCurtoPrazo: 0,
      passivoCirculanteTotal: 15000,
      capitalGiroLiquidoCgl: 40000,
      necessidadeCapitalGiroNcg: 25000,
      saldoTesourariaSt: 15000,
      ncgPeriodoAnterior: null,
      variacaoNcgValor: null,
      variacaoNcgPercentual: null,
      percentualCapitalPresoEstoque: 50,
      percentualCapitalPresoClientes: 50,
      percentualFinanciadoFornecedores: 37.5,
      riscoEfeitoTesoura: false,
      perfilEstruturaFinanceira: 'SOLIDA_SUPERAVIT_TESOURARIA',
      statusConfiabilidadeCgl: 'CALCULADO',
      statusConfiabilidadeNcg: 'CALCULADO',
      interpretacaoCapitalGiro: 'CGL cobre a NCG com folga.',
      fatoresPressaoCapitalGiro: []
    },
    prazosCiclos: {
      diasPeriodoAnalisado: 30,
      estoqueInicialPeriodo: 20000,
      estoqueFinalPeriodo: 20000,
      estoqueMedioPeriodo: 20000,
      cmvPeriodo: 10000,
      pmePrazoMedioEstocagemDias: 60,
      giroEstoqueVezesPeriodo: 0.5,
      contasReceberInicialPeriodo: 0,
      contasReceberFinalPeriodo: 20000,
      contasReceberMediaPeriodo: 10000,
      vendasPrazoPeriodo: 20000,
      pmrPrazoMedioRecebimentoDias: 15,
      fornecedoresInicialPeriodo: 0,
      fornecedoresFinalPeriodo: 15000,
      fornecedoresMediaPeriodo: 7500,
      comprasPrazoPeriodo: 15000,
      pmpPrazoMedioPagamentoDias: 15,
      cicloOperacionalDias: 75,
      cicloFinanceiroDias: 60,
      pagaFornecedorAntesDeReceber: true,
      descompassoCaixaDias: 60,
      statusConfiabilidadePme: 'CALCULADO',
      statusConfiabilidadePmr: 'CALCULADO',
      statusConfiabilidadePmp: 'CALCULADO',
      statusConfiabilidadeCicloFinanceiro: 'CALCULADO',
      tendenciaCicloFinanceiro: 'ESTAVEL',
      interpretacaoPrazosCiclos: 'Descompasso de 60 dias.',
      evolucaoMensal: []
    },
    liquidezEndividamento: {
      balancoPatrimonialConfirmado: false,
      competenciaBalancoUtilizado: '2026-11',
      disponibilidadesImediatas: 15000,
      ativoCirculante: 55000,
      estoques: 20000,
      despesasAntecipadas: 0,
      realizavelLongoPrazo: null,
      ativoNaoCirculanteTotal: null,
      ativoTotal: null,
      passivoCirculante: 15000,
      passivoNaoCirculante: null,
      passivoExigivelTotal: null,
      dividaFinanceiraTotal: null,
      patrimonioLiquido: null,
      liquidezImediata: 1,
      liquidezSeca: null,
      liquidezCorrente: null,
      liquidezGeral: null,
      endividamentoGeralPercentual: null,
      participacaoCapitalTerceirosPercentual: null,
      composicaoEndividamentoCurtoPrazoPercentual: null,
      relacaoDividaPatrimonioLiquidoPercentual: null,
      coberturaObrigacoesCurtoPrazo: 1,
      statusConfiabilidadeLiquidezImediata: 'CALCULADO',
      statusConfiabilidadePatrimonial: 'DADOS_INSUFICIENTES',
      dependenciasPatrimoniaisFaltantes: ['Balanço Patrimonial Gerencial não confirmado'],
      interpretacaoLiquidezEndividamento: 'Aguardando confirmação patrimonial.'
    },
    investimentosRetorno: {
      resultadoOperacionalPeriodo: 450,
      resultadoLiquidoPeriodo: 450,
      ativoTotalMedio: null,
      patrimonioLiquidoMedio: null,
      capitalInvestidoOperacional: null,
      roaRetornoSobreAtivoPercentual: null,
      roeRetornoSobrePatrimonioPercentual: null,
      roicRetornoSobreCapitalInvestidoPercentual: null,
      roiSimuladoPercentual: 50,
      paybackSimplesPeriodos: 2.5,
      paybackDescontadoPeriodos: 2.7,
      vplValorPresenteLiquido: 4307.07,
      tirTaxaInternaRetornoPercentual: 19.2,
      investimentoViavelEconomicamente: true,
      capacidadeFinanceiraCaixaParaInvestir: true,
      disponibilidadeCaixaAposReserva: 8000,
      statusConfiabilidadeRoaRoe: 'DADOS_INSUFICIENTES',
      statusConfiabilidadeSimulacao: 'SIMULADO_OU_PROJETADO',
      separacaoContabilVsCaixaExplicacao: 'ROA/ROE é contábil; VPL/TIR é caixa descontado.',
      alertasInvestimento: ['Monitorar reserva de caixa para o desembolso inicial.']
    },
    catalogoIndicadores: [
      {
        codigo: 'LIQUIDEZ_CORRENTE',
        nome: 'Liquidez Corrente',
        categoriaIndicador: 'LIQUIDEZ',
        prioridade: 'P2',
        descricao: 'Capacidade de pagamento no curto prazo.',
        formula: 'AC / PC',
        variaveis: { ativoCirculante: 55000, passivoCirculante: 15000 },
        resultado: null,
        unidadeMedida: 'INDICE',
        periodo: '2026-11',
        fonteDados: 'Balanço Patrimonial Gerencial',
        dataAtualizacao: '2026-11-18T01:30:00',
        interpretacaoGerencial: 'Bloqueado até confirmação do balanço.',
        limitacoes: 'Requer Balanço confirmado.',
        statusConfiabilidade: 'DADOS_INSUFICIENTES',
        dependenciasFaltantes: ['Balanço Patrimonial Gerencial não confirmado']
      }
    ],
    diagnosticosExplicaveis: [
      {
        codigoDiagnostico: 'DEPENDENCIA_BALANCO_PATRIMONIAL',
        severidade: 'ALERTA',
        indicadorRelacionado: 'Balanço Patrimonial Gerencial',
        resultadoFormatado: 'DADOS_INSUFICIENTES',
        periodo: '2026-11',
        fonteDados: 'Estrutura Patrimonial (Lote 4.6)',
        interpretacao: 'Indicadores patrimoniais bloqueados até confirmação.',
        possivelConsequencia: 'Evita números improvisados.',
        recomendacaoGerencial: 'Confirmar balanço gerencial.',
        grauConfiabilidade: 'DADOS_INSUFICIENTES'
      }
    ]
  };

  beforeEach(async () => {
    currentRoles = ['ROLE_DONO_GERENTE'];
    apiSpy = jasmine.createSpyObj<ApexApiService>('ApexApiService', [
      'products',
      'getIndicadoresPainel',
      'simularPrecificacaoInteligente',
      'aplicarPrecoProdutoAutorizado',
      'salvarBalancoPatrimonialGerencial',
      'simularInvestimentoIndicadores',
      'salvarIndicadoresParametros'
    ]);

    apiSpy.products.and.returnValue(
      of([
        {
          idProduto: 10,
          descricao: 'Fone ANC Pro',
          custo: 90,
          valorVenda: 150,
          quantidadeEstoque: 15,
          icms: 10,
          status: 1
        } as Produto
      ])
    );
    apiSpy.getIndicadoresPainel.and.returnValue(of(mockPainel));
    apiSpy.simularPrecificacaoInteligente.and.returnValue(of(mockPrecificacao));
    apiSpy.aplicarPrecoProdutoAutorizado.and.returnValue(of({ idProduto: 10 } as Produto));
    apiSpy.salvarBalancoPatrimonialGerencial.and.returnValue(of(mockPainel.balancoPatrimonial));
    apiSpy.simularInvestimentoIndicadores.and.returnValue(of(mockPainel.investimentosRetorno));
    apiSpy.salvarIndicadoresParametros.and.returnValue(of(mockPainel.parametrosVigentes));

    await TestBed.configureTestingModule({
      imports: [FinanceIndicatorsPage],
      providers: [
        provideRouter([]),
        { provide: ApexApiService, useValue: apiSpy },
        {
          provide: AuthService,
          useValue: {
            user: () => ({ usuarioId: 1, login: 'dono', nome: 'Dono', roles: currentRoles })
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(FinanceIndicatorsPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('carrega o painel de inteligência administrativa e exibe estado DADOS_INSUFICIENTES para índices patrimoniais sem balanço', () => {
    expect(apiSpy.products).toHaveBeenCalled();
    expect(apiSpy.getIndicadoresPainel).toHaveBeenCalled();
    expect(component.produtosCatalogo().length).toBe(1);
    expect(component.precifForm.produtoId).toBe(10);
    expect(component.painel()?.rentabilidade.margemContribuicaoTotalValor).toBe(900);
    expect(component.painel()?.liquidezEndividamento.statusConfiabilidadePatrimonial).toBe('DADOS_INSUFICIENTES');
    expect(fixture.nativeElement.textContent).toContain('Cálculos Administrativos, Margens, Capital de Giro');
    expect(fixture.nativeElement.textContent).toContain('DADOS_INSUFICIENTES');
  });

  it('permite selecionar produto, simular precificação sem persistir e aplicar preço autorizado mediante justificativa', () => {
    component.abaAtiva.set('precificacao');
    fixture.detectChanges();

    component.onSelecionarProdutoPrecificacao(10);
    expect(component.precifForm.custoAquisicaoBase).toBe(90);
    expect(component.precifForm.precoPraticadoAtual).toBe(150);
    expect(apiSpy.simularPrecificacaoInteligente).toHaveBeenCalled();
    expect(apiSpy.aplicarPrecoProdutoAutorizado).not.toHaveBeenCalled();

    component.aplicarPrecoJustificativa = 'Ajuste autorizado para meta 25%';
    component.aplicarPrecoAutorizado(10);
    expect(apiSpy.aplicarPrecoProdutoAutorizado).toHaveBeenCalledWith(
      jasmine.objectContaining({
        produtoId: 10,
        novoValorVenda: 166.67,
        justificativa: 'Ajuste autorizado para meta 25%'
      })
    );
  });

  it('salva balanço patrimonial respeitando sincronização ERP ou edição manual, salva parâmetros e exibe alertas de investimento', () => {
    component.balancoForm.usarSaldosOperacionaisErp = true;
    component.salvarBalancoPatrimonial();
    expect(apiSpy.salvarBalancoPatrimonialGerencial).toHaveBeenCalledWith(
      jasmine.objectContaining({
        usarSaldosOperacionaisErp: true,
        disponibilidadesCaixaBancos: null,
        contasReceberOperacionais: null,
        estoquesOperacionais: null,
        fornecedoresOperacionaisCp: null
      })
    );

    component.balancoForm.usarSaldosOperacionaisErp = false;
    component.balancoForm.disponibilidadesCaixaBancos = 18000;
    component.salvarBalancoPatrimonial();
    expect(apiSpy.salvarBalancoPatrimonialGerencial).toHaveBeenCalledWith(
      jasmine.objectContaining({
        usarSaldosOperacionaisErp: false,
        disponibilidadesCaixaBancos: 18000,
        estoquesOperacionais: 20000
      })
    );

    component.salvarParametros();
    expect(apiSpy.salvarIndicadoresParametros).toHaveBeenCalled();

    component.abaAtiva.set('investimentos');
    component.fluxosProjetoTexto = '4000, 4000, 4000, 3000';
    component.simularInvestimento();
    fixture.detectChanges();

    expect(apiSpy.simularInvestimentoIndicadores).toHaveBeenCalledWith(
      jasmine.objectContaining({ fluxosCaixaProjetados: [4000, 4000, 4000, 3000] }),
      jasmine.any(String)
    );
    expect(fixture.nativeElement.textContent).toContain('VIÁVEL');
    expect(fixture.nativeElement.textContent).toContain('COM FOLGA');
    expect(fixture.nativeElement.textContent).toContain('Monitorar reserva de caixa para o desembolso inicial.');
  });

  it('oculta ações de escrita quando perfil é somente leitura (ROLE_AUDITOR)', () => {
    currentRoles = ['ROLE_AUDITOR'];
    const readOnlyFixture = TestBed.createComponent(FinanceIndicatorsPage);
    readOnlyFixture.detectChanges();
    expect(readOnlyFixture.componentInstance.podeEditar()).toBeFalse();
  });
});
