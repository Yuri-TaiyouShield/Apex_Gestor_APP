import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { catchError, of } from 'rxjs';

import { ApexApiService } from '../core/apex-api.service';
import { MOCK_EXPENSES, MOCK_PRODUCTS, MOCK_REPORT, MOCK_USERS } from '../core/mock-data';
import { DashboardVendedor, RelatorioFinanceiro } from '../core/models';
import { SessionService } from '../core/session.service';
import { currency } from '../core/view-utils';

@Component({
  standalone: true,
  imports: [CommonModule, IonicModule, RouterLink],
  template: `
    <ion-content>
      <main class="page">
        <header class="page-header">
          <div>
            <span class="page-kicker">Comando operacional</span>
            <h1>{{ isSellerDashboard() ? 'Dashboard do Vendedor' : 'Painel executivo ' + session.roleLabel() }}</h1>
            <p>{{ isSellerDashboard()
              ? 'Metas, vendas e fila de caixa do vendedor autenticado, sem financeiro gerencial.'
              : 'Indicadores de venda, estoque, caixa e rotina financeira em leitura rapida para gestao.' }}</p>
          </div>
          <div class="header-tools">
            <span class="date-pill">{{ currentMonthLabel }}</span>
            <ion-badge [color]="apiOffline() ? 'warning' : 'primary'">{{ apiOffline() ? 'Dados locais' : 'API sincronizada' }}</ion-badge>
          </div>
        </header>

        <section class="command-bar">
          @if (canOpenPdv()) {
            <ion-button routerLink="/pos">
              <ion-icon slot="start" name="cash-outline"></ion-icon>
              Abrir PDV
            </ion-button>
          }
          @if (canViewStock()) {
            <ion-button fill="outline" routerLink="/products">
              <ion-icon slot="start" name="cube-outline"></ion-icon>
              Estoque critico
            </ion-button>
          }
          @if (canViewFinance()) {
            <ion-button fill="outline" routerLink="/finance-operations">
              <ion-icon slot="start" name="receipt-outline"></ion-icon>
              Financeiro
            </ion-button>
          }
          @if (canViewReports()) {
            <ion-button fill="outline" routerLink="/reports">
              <ion-icon slot="start" name="bar-chart-outline"></ion-icon>
              Relatorios
            </ion-button>
          }
        </section>

        @if (isSellerDashboard()) {
          <section class="metric-grid">
            <article class="metric-card revenue">
              <header><span>Meta do mes</span><ion-icon name="flag-outline" aria-hidden="true"></ion-icon></header>
              <strong>{{ money(sellerGoal()) }}</strong>
              <small>{{ sellerGoalConfigured() ? 'Meta individual da filial ativa' : 'Meta ainda não configurada' }}</small>
            </article>
            <article class="metric-card profit">
              <header><span>Vendido hoje</span><ion-icon name="calendar-outline" aria-hidden="true"></ion-icon></header>
              <strong>{{ money(sellerSoldToday()) }}</strong>
              <small>{{ sellerSalesToday() }} vendas no dia</small>
            </article>
            <article class="metric-card cost">
              <header><span>Vendido no mes</span><ion-icon name="trending-up-outline" aria-hidden="true"></ion-icon></header>
              <strong>{{ money(sellerSoldMonth()) }}</strong>
              <small>{{ sellerSalesMonth() }} vendas no mes</small>
            </article>
            <article class="metric-card expenses">
              <header><span>Faltante para meta</span><ion-icon name="speedometer-outline" aria-hidden="true"></ion-icon></header>
              <strong>{{ money(sellerMissingGoal()) }}</strong>
              <small>{{ sellerGoalPercent() }} da meta atingida</small>
            </article>
          </section>

          <section class="ops-strip">
            <div class="surface-row">
              <ion-icon name="bag-check-outline" aria-hidden="true"></ion-icon>
              <div><strong>{{ sellerItemsToday() }} itens hoje</strong><span>{{ sellerItemsMonth() }} itens no mes</span></div>
            </div>
            <div class="surface-row">
              <ion-icon name="receipt-outline" aria-hidden="true"></ion-icon>
              <div><strong>{{ money(sellerAverageTicket()) }}</strong><span>Ticket medio individual</span></div>
            </div>
            <div class="surface-row">
              <ion-icon name="time-outline" aria-hidden="true"></ion-icon>
              <div><strong>{{ sellerWaitingCashier() }} aguardando caixa</strong><span>{{ sellerPaidByCashier() }} finalizadas pelo caixa</span></div>
            </div>
          </section>

          <section class="grid-two dashboard-grid">
            <ion-card class="data-card">
              <ion-card-header>
                <ion-card-title>Ultimas vendas</ion-card-title>
                <ion-card-subtitle>Somente vendas do vendedor autenticado</ion-card-subtitle>
              </ion-card-header>
              <ion-card-content class="seller-list">
                @for (sale of sellerLastSales(); track sale.id) {
                  <div class="seller-row">
                    <strong>#{{ sale.id }} {{ sale.status }}</strong>
                    <span>{{ sale.when }} - {{ money(sale.total) }}</span>
                  </div>
                }
              </ion-card-content>
            </ion-card>

            <ion-card class="data-card">
              <ion-card-header>
                <ion-card-title>Fila do caixa</ion-card-title>
                <ion-card-subtitle>Venda enviada ao caixa nao libera acesso ao caixa</ion-card-subtitle>
              </ion-card-header>
              <ion-card-content class="action-list">
                <ion-item lines="none">
                  <span class="action-icon warning"><ion-icon name="hourglass-outline" aria-hidden="true"></ion-icon></span>
                  <ion-label><strong>{{ sellerWaitingCashier() }} aguardando recebimento</strong><p>Acompanhe pelo historico de vendas.</p></ion-label>
                </ion-item>
                <ion-item lines="none">
                  <span class="action-icon"><ion-icon name="checkmark-done-outline" aria-hidden="true"></ion-icon></span>
                  <ion-label><strong>{{ sellerPaidByCashier() }} finalizadas</strong><p>Recebidas e confirmadas pelo operador de caixa.</p></ion-label>
                </ion-item>
                <ion-item lines="none">
                  <span class="action-icon warning"><ion-icon name="return-down-back-outline" aria-hidden="true"></ion-icon></span>
                  <ion-label><strong>{{ sellerRejectedByCashier() }} devolvidas para ajuste</strong><p>Corrija e reenvie pelo histórico de vendas.</p></ion-label>
                </ion-item>
              </ion-card-content>
            </ion-card>
          </section>
        } @else {
          <section class="metric-grid">
            <article class="metric-card revenue">
              <header><span>Total de vendas</span><ion-icon name="trending-up-outline" aria-hidden="true"></ion-icon></header>
              <strong>{{ money(report().totalVendas) }}</strong>
              <small class="positive">Periodo atual</small>
            </article>
            <article class="metric-card cost">
              <header><span>CMV</span><ion-icon name="layers-outline" aria-hidden="true"></ion-icon></header>
              <strong>{{ money(report().totalCustosProdutos) }}</strong>
              <small>{{ costRate() }} da receita</small>
            </article>
            <article class="metric-card expenses">
              <header><span>Despesas</span><ion-icon name="receipt-outline" aria-hidden="true"></ion-icon></header>
              <strong>{{ money(report().totalDespesas) }}</strong>
              <small class="attention">{{ expenseCount }} lancamentos no ciclo</small>
            </article>
            <article class="metric-card profit">
              <header><span>Lucro liquido</span><ion-icon name="wallet-outline" aria-hidden="true"></ion-icon></header>
              <strong>{{ money(report().lucroLiquido) }}</strong>
              <small class="positive">Margem {{ profitMargin() }}</small>
            </article>
          </section>

          <section class="ops-strip">
            <div class="surface-row">
              <ion-icon name="cube-outline" aria-hidden="true"></ion-icon>
              <div><strong>{{ lowStockCount }} produtos criticos</strong><span>Reposicao abaixo do minimo</span></div>
            </div>
            <div class="surface-row">
              <ion-icon name="cash-outline" aria-hidden="true"></ion-icon>
              <div><strong>{{ money(report().saldoCaixa) }}</strong><span>Saldo de caixa projetado</span></div>
            </div>
            <div class="surface-row">
              <ion-icon name="shield-checkmark-outline" aria-hidden="true"></ion-icon>
              <div><strong>{{ staffCount }} usuarios ativos</strong><span>RBAC e auditoria interna</span></div>
            </div>
          </section>

          <section class="grid-two dashboard-grid">
            <ion-card class="data-card">
              <ion-card-header>
                <ion-card-title>Performance por canal</ion-card-title>
                <ion-card-subtitle>Participacao de venda por frente comercial</ion-card-subtitle>
              </ion-card-header>
              <ion-card-content>
                <div class="channel-list">
                  @for (channel of channelMix; track channel.label) {
                    <div class="channel-row" [class.accent]="channel.tone === 'accent'" [class.indigo]="channel.tone === 'indigo'">
                      <span class="channel-icon"><ion-icon [name]="channel.icon" aria-hidden="true"></ion-icon></span>
                      <div class="channel-copy"><span>{{ channel.label }}</span><strong>{{ channel.amount }}</strong></div>
                      <b>{{ channel.share }}%</b>
                      <div class="progress-track"><i [style.width.%]="channel.share"></i></div>
                    </div>
                  }
                </div>
              </ion-card-content>
            </ion-card>

            <ion-card class="data-card">
              <ion-card-header>
                <ion-card-title>Prioridades do dia</ion-card-title>
                <ion-card-subtitle>Fila objetiva para gerente, caixa e estoque</ion-card-subtitle>
              </ion-card-header>
              <ion-card-content class="action-list">
                @for (item of actionItems; track item.label) {
                  <ion-item lines="none">
                    <span class="action-icon" [class.warning]="item.color === 'warning'" [class.danger]="item.color === 'danger'">
                      <ion-icon [name]="item.icon" aria-hidden="true"></ion-icon>
                    </span>
                    <ion-label><strong>{{ item.value }}</strong><p>{{ item.label }}</p></ion-label>
                  </ion-item>
                }
              </ion-card-content>
            </ion-card>
          </section>
        }
      </main>
    </ion-content>
  `,
  styles: [`
    .header-tools { align-items: center; display: flex; flex-wrap: wrap; gap: 10px; justify-content: flex-end; }
    .date-pill { background: var(--apex-card); border: 1px solid var(--apex-border); border-radius: 8px; color: var(--apex-muted); display: inline-flex; font-size: 0.82rem; font-weight: 750; padding: 8px 10px; }
    .command-bar { background: #ffffff; border: 1px solid var(--apex-border); border-radius: 8px; display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; padding: 10px; }
    .dashboard-grid { margin-top: 16px; }
    .ops-strip { display: grid; gap: 12px; grid-template-columns: repeat(3, minmax(0, 1fr)); margin-top: 12px; }
    .ops-strip .surface-row { justify-content: flex-start; }
    .ops-strip ion-icon, .channel-icon, .action-icon { align-items: center; background: var(--apex-primary-faint); border-radius: 8px; color: var(--apex-primary); display: flex; justify-content: center; }
    .ops-strip ion-icon { flex: 0 0 auto; font-size: 1.1rem; padding: 9px; }
    .metric-card.cost ion-icon { background: var(--apex-indigo-soft); border-color: rgba(79, 70, 229, 0.18); color: var(--apex-indigo); }
    .metric-card.expenses ion-icon { background: var(--apex-accent-soft); border-color: rgba(199, 116, 18, 0.2); color: var(--apex-accent); }
    .metric-card.profit ion-icon { background: rgba(21, 128, 61, 0.1); border-color: rgba(21, 128, 61, 0.18); color: var(--apex-success); }
    .channel-list, .action-list, .seller-list { display: grid; gap: 12px; }
    .channel-row { align-items: center; display: grid; gap: 10px 12px; grid-template-columns: 42px 1fr auto; }
    .channel-icon { border: 1px solid rgba(15, 118, 110, 0.16); height: 42px; width: 42px; }
    .channel-row.accent .channel-icon { background: var(--apex-accent-soft); color: var(--apex-accent); }
    .channel-row.indigo .channel-icon { background: var(--apex-indigo-soft); color: var(--apex-indigo); }
    .channel-copy span, .channel-copy strong { display: block; }
    .channel-copy span { color: var(--apex-muted); font-size: 0.82rem; margin-bottom: 2px; }
    .channel-copy strong, .channel-row b { color: var(--apex-ink); font-weight: 850; }
    .progress-track { background: #e6eee9; border-radius: 999px; grid-column: 1 / -1; height: 8px; overflow: hidden; }
    .progress-track i { background: var(--apex-primary); border-radius: inherit; display: block; height: 100%; }
    .channel-row.accent .progress-track i { background: var(--apex-accent); }
    .channel-row.indigo .progress-track i { background: var(--apex-indigo); }
    .action-list ion-item { --background: var(--apex-card-muted); --border-radius: 8px; --inner-padding-end: 12px; --min-height: 72px; border: 1px solid var(--apex-border); border-radius: 8px; }
    .action-icon { height: 38px; margin-right: 12px; width: 38px; }
    .action-icon.warning { background: var(--apex-accent-soft); color: var(--apex-accent); }
    .action-icon.danger { background: var(--apex-coral-soft); color: var(--apex-coral); }
    .action-list strong, .seller-row strong { color: var(--apex-ink); display: block; font-size: 0.98rem; }
    .action-list p { color: var(--apex-muted); margin: 3px 0 0; }
    .seller-row { background: var(--apex-card-muted); border: 1px solid var(--apex-border); border-radius: 8px; display: grid; gap: 4px; padding: 12px; }
    .seller-row span { color: var(--apex-muted); font-size: 0.85rem; }
    @media (max-width: 991px) { .header-tools { justify-content: flex-start; } .ops-strip { grid-template-columns: 1fr; } }
  `]
})
export class DashboardPage {
  private readonly api = inject(ApexApiService);
  readonly session = inject(SessionService);
  readonly report = signal<RelatorioFinanceiro>(MOCK_REPORT);
  readonly apiOffline = signal(false);
  readonly money = currency;
  readonly canOpenPdv = computed(() => this.session.hasAnyRole(['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_VENDEDOR', 'ROLE_CAIXA', 'ROLE_ADMIN', 'ROLE_GERENTE']));
  readonly canViewStock = computed(() => this.session.hasAnyRole(['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_FINANCEIRO', 'ROLE_VENDEDOR', 'ROLE_CAIXA', 'ROLE_ADMIN', 'ROLE_GERENTE']));
  readonly canViewFinance = computed(() => this.session.hasAnyRole(['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_FINANCEIRO', 'ROLE_ADMIN', 'ROLE_GERENTE']));
  readonly canViewReports = computed(() => this.session.hasAnyRole(['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_FINANCEIRO', 'ROLE_ADMIN', 'ROLE_GERENTE', 'ROLE_GESTOR']));
  readonly isSellerDashboard = computed(() => this.session.hasAnyRole(['ROLE_VENDEDOR']) && !this.session.hasAnyRole(['ROLE_CAIXA', 'ROLE_GERENTE', 'ROLE_DONO_GERENTE', 'ROLE_SYSADMIN', 'ROLE_ADMIN']));
  readonly currentMonthLabel = new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' }).format(new Date());
  readonly lowStockCount = MOCK_PRODUCTS.filter((product) => product.quantidadeEstoque <= (product.estoqueMinimo ?? 0)).length;
  readonly expenseCount = MOCK_EXPENSES.length;
  readonly staffCount = MOCK_USERS.length;
  readonly profitMargin = computed(() => this.percent(this.report().lucroLiquido, this.report().totalVendas));
  readonly costRate = computed(() => this.percent(this.report().totalCustosProdutos, this.report().totalVendas));
  readonly channelMix = [
    { label: 'PDV Desktop', share: 54, amount: 'R$ 48,4 mil', icon: 'desktop-outline', tone: 'primary' },
    { label: 'E-commerce web', share: 31, amount: 'R$ 27,8 mil', icon: 'storefront-outline', tone: 'accent' },
    { label: 'Mobile cliente', share: 15, amount: 'R$ 13,4 mil', icon: 'phone-portrait-outline', tone: 'indigo' }
  ];
  readonly actionItems = [
    { icon: 'cube-outline', color: 'warning', value: `${this.lowStockCount} produtos`, label: 'abaixo do ponto de reposicao' },
    { icon: 'receipt-outline', color: 'danger', value: `${this.expenseCount} despesas`, label: 'pendentes no periodo' },
    { icon: 'people-outline', color: 'primary', value: `${this.staffCount} usuarios`, label: 'com acesso ativo no RBAC' }
  ];
  readonly sellerGoal = signal(0);
  readonly sellerGoalConfigured = signal(false);
  readonly sellerSoldToday = signal(0);
  readonly sellerSoldMonth = signal(0);
  readonly sellerSalesToday = signal(0);
  readonly sellerSalesMonth = signal(0);
  readonly sellerItemsToday = signal(0);
  readonly sellerItemsMonth = signal(0);
  readonly sellerWaitingCashier = signal(0);
  readonly sellerPaidByCashier = signal(0);
  readonly sellerRejectedByCashier = signal(0);
  readonly sellerMissingGoal = computed(() => Math.max(this.sellerGoal() - this.sellerSoldMonth(), 0));
  readonly sellerGoalPercent = computed(() => this.percent(this.sellerSoldMonth(), this.sellerGoal()));
  readonly sellerAverageTicket = computed(() => this.sellerSalesMonth() ? this.sellerSoldMonth() / this.sellerSalesMonth() : 0);
  readonly sellerLastSales = signal([
    { id: 'sem-vendas', status: 'Nenhuma venda no periodo', when: 'Meta aguardando operacao', total: 0 }
  ]);

  constructor() {
    if (this.isSellerDashboard()) {
      this.loadSellerDashboard();
      return;
    }
    const now = new Date();
    const start = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10);
    const end = new Date(now.getFullYear(), now.getMonth() + 1, 0).toISOString().slice(0, 10);
    this.api.financialReport(start, end).pipe(catchError(() => {
      this.apiOffline.set(true);
      return of(MOCK_REPORT);
    })).subscribe((report) => this.report.set(report));
  }

  ionViewWillEnter(): void {
    if (this.isSellerDashboard()) {
      this.loadSellerDashboard();
    }
  }

  private loadSellerDashboard(): void {
    this.api.sellerDashboard().pipe(catchError(() => {
      this.apiOffline.set(true);
      return of(null);
    })).subscribe((data) => {
      if (!data) return;
      this.applySellerDashboard(data);
      this.apiOffline.set(false);
    });
  }

  private applySellerDashboard(data: DashboardVendedor): void {
    this.sellerGoal.set(data.meta);
    this.sellerGoalConfigured.set(data.metaConfigurada);
    this.sellerSoldToday.set(data.vendidoHoje);
    this.sellerSoldMonth.set(data.vendidoMes);
    this.sellerSalesToday.set(data.vendasHoje);
    this.sellerSalesMonth.set(data.vendasMes);
    this.sellerItemsToday.set(data.itensHoje);
    this.sellerItemsMonth.set(data.itensMes);
    this.sellerWaitingCashier.set(data.aguardandoCaixa);
    this.sellerPaidByCashier.set(data.finalizadasCaixa);
    this.sellerRejectedByCashier.set(data.recusadasCaixa);
    this.sellerLastSales.set(data.ultimasVendas.length ? data.ultimasVendas.map((sale) => ({
      id: String(sale.numeroVenda || sale.idVenda),
      status: sale.status,
      when: new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(sale.dataVenda)),
      total: sale.total
    })) : [{ id: 'sem-vendas', status: 'Nenhuma venda no período', when: 'Aguardando operação', total: 0 }]);
  }

  private percent(value: number, total: number): string {
    if (!total) {
      return '0,0%';
    }
    return `${((value / total) * 100).toLocaleString('pt-BR', { maximumFractionDigits: 1, minimumFractionDigits: 1 })}%`;
  }
}
