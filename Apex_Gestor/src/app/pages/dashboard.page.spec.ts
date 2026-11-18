import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { addIcons } from 'ionicons';
import {
  bagCheckOutline,
  barChartOutline,
  calendarOutline,
  cashOutline,
  checkmarkDoneOutline,
  cubeOutline,
  flagOutline,
  hourglassOutline,
  layersOutline,
  peopleOutline,
  radioButtonOnOutline,
  receiptOutline,
  returnDownBackOutline,
  shieldCheckmarkOutline,
  speedometerOutline,
  timeOutline,
  trendingUpOutline,
  walletOutline
} from 'ionicons/icons';
import { of, throwError } from 'rxjs';

import { ApexApiService } from '../core/apex-api.service';
import { ApexRole, CaixaSessao, DashboardVendedor, RelatorioFinanceiro, Venda } from '../core/models';
import { SessionService } from '../core/session.service';
import { DashboardPage } from './dashboard.page';

describe('DashboardPage', () => {
  let roles: ApexRole[];
  let roleLabel: string;
  let api: jasmine.SpyObj<ApexApiService>;

  beforeAll(() => {
    addIcons({
      'bag-check-outline': bagCheckOutline,
      'bar-chart-outline': barChartOutline,
      'calendar-outline': calendarOutline,
      'cash-outline': cashOutline,
      'checkmark-done-outline': checkmarkDoneOutline,
      'cube-outline': cubeOutline,
      'flag-outline': flagOutline,
      'hourglass-outline': hourglassOutline,
      'layers-outline': layersOutline,
      'people-outline': peopleOutline,
      'radio-button-on-outline': radioButtonOnOutline,
      'receipt-outline': receiptOutline,
      'return-down-back-outline': returnDownBackOutline,
      'shield-checkmark-outline': shieldCheckmarkOutline,
      'speedometer-outline': speedometerOutline,
      'time-outline': timeOutline,
      'trending-up-outline': trendingUpOutline,
      'wallet-outline': walletOutline
    });
  });

  function createDashboard(userRoles: ApexRole[], label: string): {
    fixture: ComponentFixture<DashboardPage>;
    component: DashboardPage;
  } {
    roles = userRoles;
    roleLabel = label;

    const cashierMock: CaixaSessao = {
      idCaixaSessao: 42,
      operadorLogin: 'v5_caixa',
      status: 'ABERTO',
      saldoInicial: 150,
      totalVendas: 820.5,
      totalSuprimentos: 50,
      totalSangrias: 0,
      saldoAtual: 1020.5,
      abertoEm: '2026-11-16T08:00:00',
      movimentacoes: [],
      resumoPagamentos: []
    };

    const pendingSalesMock: Venda[] = [
      {
        idVenda: 101,
        numeroVenda: 'V-101',
        clienteId: 1,
        clienteNome: 'Cliente Balcão',
        usuarioId: 2,
        usuarioNome: 'v5_vendedor',
        status: 'AGUARDANDO_CAIXA',
        valorTotal: 199.9,
        desconto: 0,
        empresaId: 1,
        filialId: 1,
        dataVenda: '2026-11-16T10:00:00',
        itens: [],
        pagamentos: []
      },
      {
        idVenda: 102,
        numeroVenda: 'V-102',
        clienteId: 2,
        clienteNome: 'Maria',
        usuarioId: 2,
        usuarioNome: 'v5_vendedor',
        status: 'AGUARDANDO_CAIXA',
        valorTotal: 75,
        desconto: 0,
        empresaId: 1,
        filialId: 1,
        dataVenda: '2026-11-16T10:15:00',
        itens: [],
        pagamentos: []
      }
    ];

    const sellerMock: DashboardVendedor = {
      meta: 15000,
      metaConfigurada: true,
      vendidoHoje: 640,
      vendidoMes: 5200,
      vendasHoje: 3,
      vendasMes: 22,
      itensHoje: 7,
      itensMes: 48,
      faltanteMeta: 9800,
      percentualMeta: 34.7,
      ticketMedio: 236.36,
      aguardandoCaixa: 2,
      finalizadasCaixa: 20,
      recusadasCaixa: 0,
      canceladasOuDevolvidas: 0,
      ultimasVendas: []
    };

    const financeMock: RelatorioFinanceiro = {
      totalVendas: 45000,
      totalCustosProdutos: 21000,
      totalDespesas: 6000,
      lucroLiquido: 18000,
      saldoCaixa: 12500
    };

    api = jasmine.createSpyObj<ApexApiService>('ApexApiService', [
      'currentCashier',
      'pendingCashierSales',
      'sellerDashboard',
      'financialReport'
    ]);
    api.currentCashier.and.returnValue(of(cashierMock));
    api.pendingCashierSales.and.returnValue(of(pendingSalesMock));
    api.sellerDashboard.and.returnValue(of(sellerMock));
    api.financialReport.and.returnValue(of(financeMock));

    const sessionStub = {
      roleLabel: () => roleLabel,
      hasAnyRole: (required?: readonly ApexRole[]) => {
        if (!required || required.length === 0) {
          return true;
        }
        const active = new Set(roles);
        return required.some((role) => active.has(role));
      }
    };

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [DashboardPage],
      providers: [
        provideRouter([]),
        { provide: ApexApiService, useValue: api },
        { provide: SessionService, useValue: sessionStub }
      ]
    });

    const fixture = TestBed.createComponent(DashboardPage);
    fixture.detectChanges();
    return { fixture, component: fixture.componentInstance };
  }

  it('para ROLE_CAIXA carrega apenas dados do caixa sem chamar financialReport e oculta indicadores gerenciais', () => {
    const { fixture, component } = createDashboard(['ROLE_CAIXA'], 'Caixa');

    expect(component.isCashierDashboard()).toBeTrue();
    expect(component.canOpenCashier()).toBeTrue();
    expect(api.financialReport).not.toHaveBeenCalled();
    expect(api.sellerDashboard).not.toHaveBeenCalled();
    expect(api.currentCashier).toHaveBeenCalledTimes(1);
    expect(api.pendingCashierSales).toHaveBeenCalledTimes(1);
    expect(component.cashierSession()?.status).toBe('ABERTO');
    expect(component.cashierPendingCount()).toBe(2);
    expect(component.apiOffline()).toBeFalse();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Painel operacional do Caixa');
    expect(text).toContain('Abrir Caixa');
    expect(text).toContain('Status do Caixa');
    expect(text).toContain('ABERTO');
    expect(text).toContain('Saldo atual');
    expect(text).toContain('Fila aguardando recebimento');
    expect(text).toContain('Vendas recebidas na sessão');
    expect(text).not.toContain('CMV');
    expect(text).not.toContain('Lucro liquido');

    const links = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('ion-button')).map(
      (btn) => btn.getAttribute('routerLink') ?? btn.getAttribute('ng-reflect-router-link')
    );
    expect(links).toContain('/cashier');
    expect(links).toContain('/pos');
  });

  it('para ROLE_CAIXA sinaliza offline se chamada do caixa falhar e exibe FECHADO quando não há sessão aberta', () => {
    const { fixture, component } = createDashboard(['ROLE_CAIXA'], 'Caixa');
    api.currentCashier.and.returnValue(of(null));
    api.pendingCashierSales.and.returnValue(throwError(() => new Error('offline')));

    component.ionViewWillEnter();
    fixture.detectChanges();

    expect(api.financialReport).not.toHaveBeenCalled();
    expect(component.cashierSession()).toBeNull();
    expect(component.cashierPendingCount()).toBe(0);
    expect(component.apiOffline()).toBeTrue();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('FECHADO');
  });

  it('para ROLE_VENDEDOR carrega sellerDashboard sem chamar financialReport nem caixa', () => {
    const { fixture, component } = createDashboard(['ROLE_VENDEDOR'], 'Vendedor');

    expect(component.isSellerDashboard()).toBeTrue();
    expect(component.isCashierDashboard()).toBeFalse();
    expect(component.canOpenCashier()).toBeFalse();
    expect(api.sellerDashboard).toHaveBeenCalledTimes(1);
    expect(api.financialReport).not.toHaveBeenCalled();
    expect(api.currentCashier).not.toHaveBeenCalled();
    expect(component.sellerGoal()).toBe(15000);

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Dashboard do Vendedor');
    expect(text).not.toContain('Abrir Caixa');
  });

  it('para ROLE_GERENTE carrega financialReport e exibe atalho Abrir Caixa e indicadores executivos', () => {
    const { fixture, component } = createDashboard(['ROLE_GERENTE'], 'Gerente');

    expect(component.canViewFinance()).toBeTrue();
    expect(component.canOpenCashier()).toBeTrue();
    expect(component.isCashierDashboard()).toBeFalse();
    expect(api.financialReport).toHaveBeenCalledTimes(1);
    expect(api.sellerDashboard).not.toHaveBeenCalled();
    expect(api.currentCashier).not.toHaveBeenCalled();
    expect(component.report().lucroLiquido).toBe(18000);

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Painel executivo Gerente');
    expect(text).toContain('Abrir Caixa');
    expect(text).toContain('CMV');
    expect(text).toContain('Lucro liquido');
  });

  it('para perfil sem permissão financeira (ROLE_GESTOR) não dispara chamada de financialReport', () => {
    createDashboard(['ROLE_GESTOR'], 'Gestor');

    expect(api.financialReport).not.toHaveBeenCalled();
    expect(api.sellerDashboard).not.toHaveBeenCalled();
    expect(api.currentCashier).not.toHaveBeenCalled();
  });
});
