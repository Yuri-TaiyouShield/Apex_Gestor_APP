import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { IonicModule } from '@ionic/angular';
import { catchError, of } from 'rxjs';

import { ApexApiService } from '../core/apex-api.service';
import { MOCK_EXPENSES, MOCK_PRODUCTS, MOCK_REPORT, MOCK_USERS } from '../core/mock-data';
import { RelatorioFinanceiro } from '../core/models';
import { SessionService } from '../core/session.service';
import { currency } from '../core/view-utils';

@Component({
  standalone: true,
  imports: [CommonModule, IonicModule],
  template: `
    <ion-content>
      <main class="page">
        <header class="page-header">
          <div>
            <h1>Dashboard {{ session.roleLabel() }}</h1>
            <p>Visão integrada de ERP, PDV, e-commerce, estoque e financeiro.</p>
          </div>
          <ion-badge color="primary">Dados sincronizados por API</ion-badge>
        </header>

        <section class="metric-grid">
          <article class="metric-card">
            <span>Total de vendas</span>
            <strong>{{ money(report().totalVendas) }}</strong>
          </article>
          <article class="metric-card">
            <span>CMV</span>
            <strong>{{ money(report().totalCustosProdutos) }}</strong>
          </article>
          <article class="metric-card">
            <span>Despesas</span>
            <strong>{{ money(report().totalDespesas) }}</strong>
          </article>
          <article class="metric-card">
            <span>Lucro líquido</span>
            <strong>{{ money(report().lucroLiquido) }}</strong>
          </article>
        </section>

        <section class="grid-two dashboard-grid">
          <ion-card class="data-card">
            <ion-card-header>
              <ion-card-title>Performance por canal</ion-card-title>
              <ion-card-subtitle>PDV, e-commerce web e aplicativo cliente</ion-card-subtitle>
            </ion-card-header>
            <ion-card-content>
              <div class="channel-row">
                <span>PDV Desktop</span>
                <strong>54%</strong>
                <div><i style="width: 54%"></i></div>
              </div>
              <div class="channel-row">
                <span>E-commerce Web</span>
                <strong>31%</strong>
                <div><i style="width: 31%"></i></div>
              </div>
              <div class="channel-row">
                <span>Mobile Cliente</span>
                <strong>15%</strong>
                <div><i style="width: 15%"></i></div>
              </div>
            </ion-card-content>
          </ion-card>

          <ion-card class="data-card">
            <ion-card-header>
              <ion-card-title>Alertas operacionais</ion-card-title>
              <ion-card-subtitle>Itens que exigem ação rápida</ion-card-subtitle>
            </ion-card-header>
            <ion-card-content class="stack">
              <ion-item lines="none">
                <ion-icon slot="start" name="cube-outline" color="warning"></ion-icon>
                <ion-label>
                  <strong>{{ lowStockCount }} produtos</strong>
                  <p>abaixo do ponto de reposição</p>
                </ion-label>
              </ion-item>
              <ion-item lines="none">
                <ion-icon slot="start" name="receipt-outline" color="danger"></ion-icon>
                <ion-label>
                  <strong>{{ expenseCount }} despesas</strong>
                  <p>pendentes no período</p>
                </ion-label>
              </ion-item>
              <ion-item lines="none">
                <ion-icon slot="start" name="people-outline" color="primary"></ion-icon>
                <ion-label>
                  <strong>{{ staffCount }} usuários</strong>
                  <p>ativos para RBAC interno</p>
                </ion-label>
              </ion-item>
            </ion-card-content>
          </ion-card>
        </section>
      </main>
    </ion-content>
  `,
  styles: [`
    .dashboard-grid {
      margin-top: 16px;
    }

    .channel-row {
      display: grid;
      gap: 8px;
      grid-template-columns: 1fr auto;
      margin-bottom: 18px;
    }

    .channel-row div {
      background: #e2e8f0;
      border-radius: 999px;
      grid-column: 1 / -1;
      height: 10px;
      overflow: hidden;
    }

    .channel-row i {
      background: linear-gradient(90deg, #0f766e, #2563eb);
      border-radius: inherit;
      display: block;
      height: 100%;
    }
  `]
})
export class DashboardPage {
  private readonly api = inject(ApexApiService);
  readonly session = inject(SessionService);
  readonly report = signal<RelatorioFinanceiro>(MOCK_REPORT);
  readonly money = currency;
  readonly lowStockCount = MOCK_PRODUCTS.filter((product) => product.quantidadeEstoque <= (product.estoqueMinimo ?? 0)).length;
  readonly expenseCount = MOCK_EXPENSES.length;
  readonly staffCount = MOCK_USERS.length;

  constructor() {
    const now = new Date();
    const start = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10);
    const end = new Date(now.getFullYear(), now.getMonth() + 1, 0).toISOString().slice(0, 10);
    this.api.financialReport(start, end).pipe(catchError(() => of(MOCK_REPORT))).subscribe((report) => this.report.set(report));
  }
}
