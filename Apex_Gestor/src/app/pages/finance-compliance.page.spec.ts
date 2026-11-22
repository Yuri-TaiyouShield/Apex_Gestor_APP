import { ComponentFixture, TestBed } from '@angular/core/testing';
import { addIcons } from 'ionicons';
import { warningOutline } from 'ionicons/icons';
import { of, throwError } from 'rxjs';

import { ApexApiService } from '../core/apex-api.service';
import { FinancialCalculationResponse, FinancialDocument } from '../core/models';
import { FinanceCompliancePage } from './finance-compliance.page';

describe('FinanceCompliancePage', () => {
  let api: jasmine.SpyObj<ApexApiService>;
  let fixture: ComponentFixture<FinanceCompliancePage>;
  let component: FinanceCompliancePage;

  beforeAll(() => {
    addIcons({
      'warning-outline': warningOutline
    });
  });

  beforeEach(() => {
    const calcResponse: FinancialCalculationResponse = {
      calculationId: 101,
      tipo: 'ADMCALC',
      calculadoEm: '2026-11-17T10:00:00',
      resultados: {
        faturamento: 4990,
        lucroBruto: 4000,
        lucroLiquido: 2000,
        margemLucroBruta: 40,
        margemLucroLiquida: 20,
        margemContribuicao: 6000,
        margemContribuicaoPercentual: 60,
        pontoEquilibrio: 120,
        pontoEquilibrioReceita: 5000,
        pontoEquilibrioUnidades: 120,
        ativoTotal: 200000,
        passivoTotal: 70000,
        patrimonioLiquido: 130000,
        capitalGiro: 50000,
        liquidezCorrente: 1.85,
        liquidezSeca: 1.2,
        depreciacaoAnual: 4000,
        depreciacaoMensal: 333.33,
        custoTotal: 7000,
        custoUnitario: 20,
        precoMarkup: 10000,
        markupMultiplicador: 1.43,
        roi: 50,
        valorPresente: 8000,
        valorFuturo: 12000,
        vpl: 550.25,
        tir: 18.25,
        fatorR: 30,
        fatorReducaoNoturna: 11.43
      },
      alertas: []
    };

    const docsMock: FinancialDocument[] = [];

    api = jasmine.createSpyObj<ApexApiService>('ApexApiService', [
      'financialDocuments',
      'laborCalculation',
      'taxCalculation',
      'admCalcCalculation',
      'createFinancialDocument',
      'signFinancialDocument'
    ]);
    api.financialDocuments.and.returnValue(of(docsMock));
    api.laborCalculation.and.returnValue(of(calcResponse));
    api.taxCalculation.and.returnValue(of(calcResponse));
    api.admCalcCalculation.and.returnValue(of(calcResponse));
    api.createFinancialDocument.and.returnValue(
      of({
        idDocumento: 88,
        tipoDocumento: 'Holerite',
        funcionarioNome: 'Colaborador',
        funcionarioEmail: 'c@apex.com',
        status: 'RETIDO',
        cargoAssinanteObrigatorio: 'CONTADOR',
        geradoPor: 'financeiro',
        geradoEm: '2026-11-17T10:00:00'
      })
    );
    api.signFinancialDocument.and.returnValue(
      of({
        idDocumento: 88,
        tipoDocumento: 'Holerite',
        funcionarioNome: 'Colaborador',
        funcionarioEmail: 'c@apex.com',
        status: 'ASSINADO',
        cargoAssinanteObrigatorio: 'CONTADOR',
        geradoPor: 'financeiro',
        geradoEm: '2026-11-17T10:00:00'
      })
    );

    TestBed.configureTestingModule({
      imports: [FinanceCompliancePage],
      providers: [{ provide: ApexApiService, useValue: api }]
    });

    fixture = TestBed.createComponent(FinanceCompliancePage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('usa escalaPercentual PERCENTUAL por padrão e envia os valores reais do formulário AdmCalc sem overrides fixos', () => {
    expect(component.admCalcForm.controls.escalaPercentual.value).toBe('PERCENTUAL');

    component.admCalcForm.patchValue({
      escalaPercentual: 'DECIMAL',
      precoVenda: 120,
      quantidade: 40,
      receitaTotal: 4800,
      cmv: 2000,
      despesas: 500,
      impostos: 300,
      custosFixos: 1500,
      custosVariaveis: 1800,
      margemContribuicao: 75,
      ativoCirculante: 90000,
      ativoNaoCirculante: 45000,
      passivoCirculante: 35000,
      passivoNaoCirculante: 15000,
      estoque: 12500,
      custoBem: 30000,
      valorResidual: 3000,
      vidaUtil: 6,
      custoTotal: 3300,
      quantidadeProduzida: 110,
      margemDesejada: 0.25,
      ganhoInvestimento: 20000,
      custoInvestimento: 12000,
      valorPresente: 8000,
      valorFuturo: 12000,
      taxaDesconto: 0.12,
      periodo: 4,
      fluxosDeCaixaTexto: '-2500, 900, 1100, 1400'
    });

    component.calculateAdmCalc();

    expect(api.admCalcCalculation).toHaveBeenCalledTimes(1);
    const payload = api.admCalcCalculation.calls.mostRecent().args[0];
    expect(payload).toEqual({
      escalaPercentual: 'DECIMAL',
      precoVenda: 120,
      quantidade: 40,
      receitaTotal: 4800,
      cmv: 2000,
      despesas: 500,
      impostos: 300,
      custosFixos: 1500,
      custosVariaveis: 1800,
      margemContribuicao: 75,
      ativoCirculante: 90000,
      ativoNaoCirculante: 45000,
      passivoCirculante: 35000,
      passivoNaoCirculante: 15000,
      estoque: 12500,
      custoBem: 30000,
      valorResidual: 3000,
      vidaUtil: 6,
      custoTotal: 3300,
      quantidadeProduzida: 110,
      margemDesejada: 0.25,
      ganhoInvestimento: 20000,
      custoInvestimento: 12000,
      valorPresente: 8000,
      valorFuturo: 12000,
      taxaDesconto: 0.12,
      periodo: 4,
      fluxosDeCaixa: [-2500, 900, 1100, 1400]
    });
  });

  it('omite fluxosDeCaixa quando campo está vazio e bloqueia texto inválido sem chamar API', () => {
    component.admCalcForm.patchValue({ fluxosDeCaixaTexto: '   ' });
    component.calculateAdmCalc();

    expect(api.admCalcCalculation).toHaveBeenCalledTimes(1);
    expect(api.admCalcCalculation.calls.mostRecent().args[0].fluxosDeCaixa).toBeUndefined();

    api.admCalcCalculation.calls.reset();
    component.admCalcForm.patchValue({ fluxosDeCaixaTexto: '-1000, abc, 500' });
    component.calculateAdmCalc();

    expect(api.admCalcCalculation).not.toHaveBeenCalled();
    expect(component.error()).toContain('Fluxos de caixa');
  });

  it('não injeta valores ocultos fabricados em calculateLabor nem calculateTaxes quando opcionais não são informados', () => {
    component.calculateLabor();

    expect(api.laborCalculation).toHaveBeenCalledTimes(1);
    const laborPayload = api.laborCalculation.calls.mostRecent().args[0];
    expect(laborPayload).toEqual({
      salarioMensal: 3000,
      jornadaMensal: 220
    });
    expect(laborPayload.verbasIncontroversas).toBeUndefined();
    expect(laborPayload.salariosAteTerminoContrato).toBeUndefined();
    expect(laborPayload.tabelaInss).toBeUndefined();

    component.laborForm.patchValue({
      horasRelogioNoturnas: 12,
      usarTabelaInssReferencia: true
    });
    component.calculateLabor();
    const laborWithOptIn = api.laborCalculation.calls.mostRecent().args[0];
    expect(laborWithOptIn.horasRelogioNoturnas).toBe(12);
    expect(laborWithOptIn.tabelaInss?.length).toBe(2);

    component.calculateTaxes();
    expect(api.taxCalculation).toHaveBeenCalledTimes(1);
    const taxPayload = api.taxCalculation.calls.mostRecent().args[0];
    expect(taxPayload).toEqual({});
    expect(taxPayload.limiteAdicionalIrpj).toBeUndefined();
    expect(taxPayload.tabelaIrrf).toBeUndefined();

    component.taxForm.patchValue({
      receitaLucroPresumido: 120000,
      limiteAdicionalIrpj: 25000,
      usarTabelaIrrfReferencia: true
    });
    component.calculateTaxes();
    const taxWithOptIn = api.taxCalculation.calls.mostRecent().args[0];
    expect(taxWithOptIn.receitaLucroPresumido).toBe(120000);
    expect(taxWithOptIn.limiteAdicionalIrpj).toBe(25000);
    expect(taxWithOptIn.tabelaIrrf?.length).toBe(3);
  });

  it('exige fingerprint e dados reais do assinante ao assinar documento sem pré-preencher certificado falso', () => {
    expect(component.signForm.controls.certificadoFingerprint.value).toBe('');
    expect(component.signForm.controls.nomeAssinante.value).toBe('');

    component.selectDocument(88);
    component.signDocument();
    expect(api.signFinancialDocument).not.toHaveBeenCalled();
    expect(component.error()).toContain('fingerprint');

    component.signForm.patchValue({
      nomeAssinante: 'Maria Contadora',
      cargoAssinante: 'CONTADOR',
      certificadoFingerprint: 'SHA256-ICP-9988',
      aprovado: false,
      observacao: 'Divergência nos encargos'
    });
    component.signDocument();

    expect(api.signFinancialDocument).toHaveBeenCalledWith(88, {
      nomeAssinante: 'Maria Contadora',
      cargoAssinante: 'CONTADOR',
      certificadoFingerprint: 'SHA256-ICP-9988',
      aprovado: false,
      observacao: 'Divergência nos encargos'
    });
  });

  it('formata corretamente os 27 indicadores do AdmCalc e chaves trabalhistas/tributárias sem confundir margemContribuicao com %', () => {
    const normalize = (value: string) => value.replace(/\u00a0/g, ' ');

    // Moeda BRL (incluindo margemContribuicao absoluta)
    expect(normalize(component.formatResult('margemContribuicao', 6000))).toBe('R$ 6.000,00');
    expect(normalize(component.formatResult('faturamento', 4990))).toBe('R$ 4.990,00');
    expect(normalize(component.formatResult('pontoEquilibrioReceita', 5000))).toBe('R$ 5.000,00');
    expect(normalize(component.formatResult('lucroBruto', 4000))).toBe('R$ 4.000,00');
    expect(normalize(component.formatResult('lucroLiquido', 2000))).toBe('R$ 2.000,00');
    expect(normalize(component.formatResult('vpl', 550.25))).toBe('R$ 550,25');
    expect(normalize(component.formatResult('precoMarkup', 10000))).toBe('R$ 10.000,00');
    expect(normalize(component.formatResult('depreciacaoAnual', 4000))).toBe('R$ 4.000,00');
    expect(normalize(component.formatResult('depreciacaoMensal', 333.33))).toBe('R$ 333,33');
    expect(normalize(component.formatResult('capitalGiro', 50000))).toBe('R$ 50.000,00');
    expect(normalize(component.formatResult('custoUnitario', 20))).toBe('R$ 20,00');
    expect(normalize(component.formatResult('custoTotal', 7000))).toBe('R$ 7.000,00');

    // Percentuais
    expect(component.formatResult('margemContribuicaoPercentual', 60)).toBe('60,00%');
    expect(component.formatResult('margemLucroBruta', 40)).toBe('40,00%');
    expect(component.formatResult('margemLucroLiquida', 20)).toBe('20,00%');
    expect(component.formatResult('roi', 50)).toBe('50,00%');
    expect(component.formatResult('tir', 18.25)).toBe('18,25%');
    expect(component.formatResult('fatorR', 30)).toBe('30,00%');

    // Unidades e horas
    expect(component.formatResult('pontoEquilibrio', 120)).toBe('120,00 un');
    expect(component.formatResult('pontoEquilibrioUnidades', 120)).toBe('120,00 un');
    expect(component.formatResult('fatorReducaoNoturna', 11.43)).toBe('11,43 h');

    // Multiplicadores / índices (nunca R$)
    expect(component.formatResult('liquidezCorrente', 1.85)).toBe('1,85 x');
    expect(component.formatResult('liquidezSeca', 1.2)).toBe('1,20 x');
    expect(component.formatResult('markupMultiplicador', 1.43)).toBe('1,43 x');
  });

  it('exibe mensagem de erro vinda da API ou de restrição de acesso', () => {
    api.admCalcCalculation.and.returnValue(
      throwError(() => ({ status: 400, error: { detail: 'margemDesejada deve ser menor que 100%' } }))
    );

    component.setTab('admcalc');
    component.calculateAdmCalc();
    expect(component.error()).toBe('margemDesejada deve ser menor que 100%');

    api.admCalcCalculation.and.returnValue(throwError(() => ({ status: 403 })));
    component.calculateAdmCalc();
    expect(component.error()).toContain('Acesso restrito');
  });
});
