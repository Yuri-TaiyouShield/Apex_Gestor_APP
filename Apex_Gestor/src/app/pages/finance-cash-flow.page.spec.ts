import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { ApexApiService } from '../core/apex-api.service';
import { AuthService } from '../core/auth.service';
import {
  ConciliacaoResumoResponse,
  FluxoCaixaPainelResponse
} from '../core/models';
import { FinanceCashFlowPage } from './finance-cash-flow.page';

describe('FinanceCashFlowPage (Lote 3)', () => {
  let fixture: ComponentFixture<FinanceCashFlowPage>;
  let component: FinanceCashFlowPage;
  let apiSpy: jasmine.SpyObj<ApexApiService>;
  let currentRoles: string[];

  const mockConciliacao: ConciliacaoResumoResponse = {
    totalItensExtrato: 2,
    conciliadosExatos: 1,
    conciliadosManuais: 0,
    pendentes: 1,
    divergentes: 0,
    duplicidadesSuspeitas: 0,
    valorTotalDivergente: 45,
    sessoesCaixaComDiferencaFechamento: 0,
    diferencaTotalFechamentoCaixa: 0,
    itens: [
      {
        idConciliacaoItem: 901,
        contaFinanceiraId: 2,
        contaFinanceiraNome: 'Banco Itaú Movimento',
        tipoConciliacao: 'BANCARIA',
        formatoOrigem: 'CSV',
        identificadorExtrato: 'FIT-901',
        dataExtrato: '2026-11-17',
        descricaoExtrato: 'TARIFA PACOTE EMPRESARIAL',
        valorExtrato: 45,
        naturezaExtrato: 'DEBITO',
        diferencaValor: 45,
        statusConciliacao: 'PENDENTE'
      }
    ]
  };

  const mockPainel: FluxoCaixaPainelResponse = {
    dataInicio: '2026-11-01',
    dataFim: '2026-11-30',
    empresaId: 1,
    filialId: 1,
    filialNome: 'Filial Matriz',
    consolidadoEmpresa: false,
    saldoInicialRealizado: 5000,
    entradasRealizadasPeriodo: 12000,
    saidasRealizadasPeriodo: 8500,
    geracaoLiquidaCaixaRealizado: 3500,
    saldoFinalRealizado: 8500,
    transferenciasInternasVolume: 500,
    totalTaxasCartaoDescontadas: 30,
    totalEstornosPeriodo: 0,
    entradasPrevistasPendentes: 4200,
    entradasPrevistasVencidas: 600,
    saidasPrevistasPendentes: 3100,
    saidasPrevistasVencidas: 200,
    saldoPrevistoLiquido30Dias: 8899,
    contasFinanceiras: [
      {
        idContaFinanceira: 1,
        empresaId: 1,
        filialId: 1,
        filialNome: 'Filial Matriz',
        codigo: 'CX-LOJA',
        nome: 'Caixa Físico Principal',
        tipoConta: 'CAIXA_FISICO',
        saldoInicial: 1000,
        saldoAtual: 2500,
        padrao: true,
        ativo: true
      },
      {
        idContaFinanceira: 2,
        empresaId: 1,
        filialId: 1,
        filialNome: 'Filial Matriz',
        codigo: 'BANCO-ITAU',
        nome: 'Banco Itaú Movimento',
        tipoConta: 'CONTA_CORRENTE',
        bancoNome: 'Itaú',
        agencia: '0001',
        numeroConta: '12345-6',
        saldoInicial: 4000,
        saldoAtual: 6000,
        padrao: false,
        ativo: true
      }
    ],
    movimentacoesRealizadas: [
      {
        idMovimentacaoFinanceira: 101,
        empresaId: 1,
        filialId: 1,
        contaFinanceiraId: 2,
        contaFinanceiraNome: 'Banco Itaú Movimento',
        tipoMovimentacao: 'ENTRADA',
        origemTipo: 'CONTA_RECEBER',
        origemId: 50,
        dataMovimentacao: '2026-11-17',
        descricao: 'Recebimento título CR-50',
        documentoReferencia: 'CR-50',
        valorBruto: 1000,
        valorTaxa: 30,
        valorJurosDesconto: 0,
        valorLiquido: 970,
        planoContaId: 1,
        planoContaNome: 'Receita de Vendas',
        estornado: false,
        conciliado: true,
        criadoPor: 'dono'
      }
    ],
    itensPrevistos: [
      {
        origemTipo: 'REPASSE_CARTAO',
        registroId: 51,
        natureza: 'ENTRADA',
        descricao: 'Parcela Cartão 2/2',
        parteRelacionada: 'Cliente Teste',
        dataCompetencia: '2026-11-10',
        dataVencimento: '2026-11-25',
        dataPrevistaLiquidacao: '2026-11-25',
        numeroParcela: 2,
        totalParcelas: 2,
        modalidade: 'CARTAO_CREDITO',
        situacaoVencimento: 'A_VENCER',
        diasAtrasoOuPrazo: 8,
        valorBruto: 1000,
        valorPagoAcumulado: 0,
        valorTaxaPrevista: 30,
        saldoAbertoLiquido: 970,
        recorrente: false,
        antecipado: false,
        filialId: 1
      },
      {
        origemTipo: 'CONTA_PAGAR',
        registroId: 61,
        natureza: 'SAIDA',
        descricao: 'Fornecedor Atrasado',
        parteRelacionada: 'Fornecedor Alfa',
        dataCompetencia: '2026-11-01',
        dataVencimento: '2026-11-10',
        dataPrevistaLiquidacao: '2026-11-10',
        numeroParcela: 1,
        totalParcelas: 1,
        modalidade: 'BOLETO',
        situacaoVencimento: 'VENCIDO',
        diasAtrasoOuPrazo: -7,
        valorBruto: 200,
        valorPagoAcumulado: 0,
        valorTaxaPrevista: 0,
        saldoAbertoLiquido: 200,
        recorrente: false,
        antecipado: false,
        filialId: 1
      }
    ],
    calendarioFinanceiro: [
      {
        data: '2026-11-17',
        tipoDado: 'HOJE_MISTO',
        entradasRealizadas: 970,
        saidasRealizadas: 0,
        entradasPrevistas: 0,
        saidasPrevistas: 0,
        saldoLiquidoDia: 970,
        saldoAcumuladoProjetado: 8500,
        quantidadeTitulosVencendo: 0,
        riscoSaldoNegativo: false
      }
    ],
    cenariosProjecao: [
      {
        codigoCenario: 'REALISTA',
        nomeCenario: 'Cenário Realista (Base)',
        premissaExplicacao: '100% repasses cartão + 92% boletos',
        percentualRealizacaoRecebiveis: 92,
        percentualVariacaoSaidas: 0,
        saldoAtualPartida: 8500,
        saldoProjetado7Dias: 8500,
        saldoProjetado15Dias: 9470,
        saldoProjetado30Dias: 8899,
        saldoProjetado60Dias: 8899,
        saldoProjetado90Dias: 8899,
        menorSaldoProjetado: 7900,
        dataMenorSaldo: '2026-11-22',
        riscoInsuficienciaCaixa: false,
        necessidadeCapitalGiroAdicional: 0,
        serieHorizonte: []
      }
    ],
    diagnosticoLiquidez: {
      nivelSaudeLiquidez: 'SAUDAVEL',
      mancheteExecutiva: 'Liquidez confortável para os próximos 30 dias.',
      disponibilidadeImediataTotal: 8500,
      entradasPrevistas30Dias: 4140,
      obrigacoesPrevistas30Dias: 3100,
      obrigacoesVencidasTotais: 200,
      recebiveisInadimplentesTotais: 600,
      taxaInadimplenciaPercentual: 12.5,
      comprometimentoFornecedoresTotal: 2500,
      repassesCartaoPendentesBruto: 2000,
      taxasCartaoPrevistas: 60,
      repassesCartaoPendentesLiquido: 1940,
      dependenciaAntecipacaoPercentual: 0,
      geracaoCaixaOperacionalPeriodo: 3500,
      resultadoEconomicoDrePeriodo: 4100,
      diferencaDreVsCaixaOperacional: 600,
      explicacaoDreVsCaixa: 'O lucro econômico na DRE superou a geração de caixa devido a vendas parceladas.',
      riscoInsuficienciaCaixa: false,
      capacidadeSustentarOperacao: true,
      disponibilidadeParaInvestimento: true,
      margemSegurancaInvestimento: 4550,
      contasComprometendoCaixa: ['Fornecedores: R$ 2500.00'],
      alertasRiscoLiquidez: ['Existem títulos vencidos a pagar (R$ 200,00).'],
      recomendacoesGerenciais: ['Regularizar obrigações vencidas com saldo disponível.']
    },
    resumoConciliacao: mockConciliacao
  };

  beforeEach(async () => {
    currentRoles = ['ROLE_DONO_GERENTE'];
    apiSpy = jasmine.createSpyObj<ApexApiService>('ApexApiService', [
      'getFluxoCaixaPainel',
      'simularFluxoCaixa',
      'listCentrosCusto',
      'listPlanoContas',
      'getConciliacaoFinanceira',
      'bootstrapContasFinanceiras',
      'createContaFinanceira',
      'transferirEntreContas',
      'estornarMovimentacaoFinanceira',
      'gerarParcelasFluxoCaixa',
      'baixarContaReceber',
      'baixarContaPagar',
      'importarExtratoConciliacao',
      'conciliarManualmente'
    ]);

    apiSpy.getFluxoCaixaPainel.and.returnValue(of(mockPainel));
    apiSpy.simularFluxoCaixa.and.returnValue(of(mockPainel.cenariosProjecao));
    apiSpy.listCentrosCusto.and.returnValue(of([]));
    apiSpy.listPlanoContas.and.returnValue(of([]));
    apiSpy.getConciliacaoFinanceira.and.returnValue(of(mockConciliacao));
    apiSpy.bootstrapContasFinanceiras.and.returnValue(of(mockPainel.contasFinanceiras));
    apiSpy.createContaFinanceira.and.returnValue(of(mockPainel.contasFinanceiras[0]));
    apiSpy.transferirEntreContas.and.returnValue(of(mockPainel.movimentacoesRealizadas));
    apiSpy.estornarMovimentacaoFinanceira.and.returnValue(of(mockPainel.movimentacoesRealizadas[0]));
    apiSpy.gerarParcelasFluxoCaixa.and.returnValue(of(mockPainel.itensPrevistos));
    apiSpy.baixarContaReceber.and.returnValue(of(mockPainel.movimentacoesRealizadas[0]));
    apiSpy.baixarContaPagar.and.returnValue(of(mockPainel.movimentacoesRealizadas[0]));
    apiSpy.importarExtratoConciliacao.and.returnValue(of(mockConciliacao));
    apiSpy.conciliarManualmente.and.returnValue(of(mockConciliacao.itens[0]));

    await TestBed.configureTestingModule({
      imports: [FinanceCashFlowPage],
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

    fixture = TestBed.createComponent(FinanceCashFlowPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('carrega o painel de fluxo de caixa, diagnóstico de liquidez e conexão DRE na inicialização', () => {
    expect(apiSpy.getFluxoCaixaPainel).toHaveBeenCalled();
    expect(component.painel()?.saldoFinalRealizado).toBe(8500);
    expect(component.painel()?.diagnosticoLiquidez.nivelSaudeLiquidez).toBe('SAUDAVEL');
    expect(fixture.nativeElement.textContent).toContain('Central de Fluxo de Caixa, Liquidez e Conciliação');
    expect(fixture.nativeElement.textContent).toContain('Liquidez confortável para os próximos 30 dias.');
  });

  it('filtra itens previstos entre vencidos e a vencer e permite liquidar repasse de cartão', () => {
    component.abaAtiva.set('previsto');
    fixture.detectChanges();

    expect(component.itensPrevistosFiltrados().length).toBe(2);

    component.filtroPrazoPrevisto.set('VENCIDO');
    expect(component.itensPrevistosFiltrados().length).toBe(1);
    expect(component.itensPrevistosFiltrados()[0].registroId).toBe(61);

    component.filtroPrazoPrevisto.set('A_VENCER');
    expect(component.itensPrevistosFiltrados().length).toBe(1);
    expect(component.itensPrevistosFiltrados()[0].registroId).toBe(51);

    component.prepararBaixaTitulo(mockPainel.itensPrevistos[0]);
    expect(component.itemSelecionadoBaixa()?.registroId).toBe(51);

    component.confirmarBaixaTitulo();
    expect(apiSpy.baixarContaReceber).toHaveBeenCalledWith(
      51,
      jasmine.objectContaining({ valorLiquidado: 1000, valorTaxa: 30 })
    );
  });

  it('executa transferência entre contas próprias, geração de parcelas, simulação e conciliação', () => {
    component.transferenciaForm = {
      contaOrigemId: 2,
      contaDestinoId: 1,
      valor: 400,
      dataTransferencia: '2026-11-17',
      descricao: 'Suprimento caixa',
      documentoReferencia: 'TR-10'
    };
    component.realizarTransferencia();
    expect(apiSpy.transferirEntreContas).toHaveBeenCalled();

    component.parcelasForm = {
      natureza: 'PAGAR',
      descricao: 'Compra parcelada',
      valorTotal: 1500,
      quantidadeParcelas: 3,
      intervaloDias: 30,
      primeiroVencimento: '2026-12-05'
    };
    component.gerarParcelas();
    expect(apiSpy.gerarParcelasFluxoCaixa).toHaveBeenCalled();

    component.executarSimulacao();
    expect(apiSpy.simularFluxoCaixa).toHaveBeenCalled();
    expect(component.simulacaoAtiva()).toBeTrue();

    component.importacaoForm.contaFinanceiraId = 2;
    component.importacaoForm.conteudoArquivo = '2026-11-17;PIX;100.00;CREDITO;FIT-1';
    component.importarExtrato();
    expect(apiSpy.importarExtratoConciliacao).toHaveBeenCalled();

    component.resolverConciliacaoManual(mockConciliacao.itens[0], 'CONCILIADO_MANUAL');
    expect(apiSpy.conciliarManualmente).toHaveBeenCalledWith(
      jasmine.objectContaining({ conciliacaoItemId: 901, novoStatus: 'CONCILIADO_MANUAL' })
    );
  });

  it('respeita perfil somente leitura (ROLE_AUDITOR) ocultando ações de mutação no caixa', () => {
    currentRoles = ['ROLE_AUDITOR'];
    const readOnlyFixture = TestBed.createComponent(FinanceCashFlowPage);
    readOnlyFixture.detectChanges();
    expect(readOnlyFixture.componentInstance.podeEditar()).toBeFalse();
  });
});
