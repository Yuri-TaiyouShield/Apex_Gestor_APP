import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { catchError, finalize, of } from 'rxjs';

import { ApexApiService } from '../core/apex-api.service';
import { CaixaSessao } from '../core/models';
import { currency, date } from '../core/view-utils';

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule],
  template: `
    <ion-content>
      <main class="page cashier-page">
        <header class="page-header">
          <div>
            <span class="page-kicker">Operação financeira diária</span>
            <h1>Caixa</h1>
            <p>Abertura, suprimento, sangria, movimentações e fechamento do caixa do operador.</p>
          </div>
          <div class="page-actions">
            <ion-button fill="outline" (click)="load()">
              <ion-icon slot="start" name="sync-outline"></ion-icon>
              Atualizar
            </ion-button>
            @if (!cashierOpen()) {
              <ion-button (click)="openCashier()" [disabled]="loading()">
                <ion-icon slot="start" name="lock-open-outline"></ion-icon>
                Abrir caixa
              </ion-button>
            }
          </div>
        </header>

        @if (message()) {
          <div class="state-line" [class.warning]="stateTone() === 'warning'" [class.success]="stateTone() === 'success'">
            <ion-icon [name]="stateTone() === 'warning' ? 'warning-outline' : 'checkmark-circle-outline'" aria-hidden="true"></ion-icon>
            {{ message() }}
          </div>
        }

        <section class="metric-grid">
          <article class="metric-card">
            <header><span>Status</span><ion-icon name="radio-button-on-outline"></ion-icon></header>
            <strong>{{ cashier()?.status || 'FECHADO' }}</strong>
            <small>{{ cashier()?.operadorLogin || 'sem caixa aberto' }}</small>
          </article>
          <article class="metric-card success">
            <header><span>Saldo atual</span><ion-icon name="cash-outline"></ion-icon></header>
            <strong>{{ money(cashier()?.saldoAtual || 0) }}</strong>
            <small>previsto para fechamento</small>
          </article>
          <article class="metric-card indigo">
            <header><span>Vendas</span><ion-icon name="cart-outline"></ion-icon></header>
            <strong>{{ money(cashier()?.totalVendas || 0) }}</strong>
            <small>integrado ao PDV</small>
          </article>
          <article class="metric-card warning">
            <header><span>Sangrias</span><ion-icon name="remove-circle-outline"></ion-icon></header>
            <strong>{{ money(cashier()?.totalSangrias || 0) }}</strong>
            <small>retiradas auditadas</small>
          </article>
        </section>

        <section class="grid-two cashier-grid">
          <ion-card class="data-card">
            <ion-card-header>
              <ion-card-title>Operações do caixa</ion-card-title>
              <ion-card-subtitle>{{ cashierOpen() ? 'Caixa aberto: operações financeiras liberadas.' : 'Abra o caixa para registrar movimentações.' }}</ion-card-subtitle>
            </ion-card-header>
            <ion-card-content class="stack">
              <ion-item>
                <ion-input type="number" label="Valor" label-placement="stacked" [(ngModel)]="amount"></ion-input>
              </ion-item>
              <ion-item>
                <ion-input label="Observação" label-placement="stacked" [(ngModel)]="note"></ion-input>
              </ion-item>
              <div class="action-grid">
                <ion-button fill="outline" (click)="supply()" [disabled]="!cashierOpen() || loading()">
                  <ion-icon slot="start" name="add-circle-outline"></ion-icon>
                  Suprimento
                </ion-button>
                <ion-button fill="outline" color="warning" (click)="withdraw()" [disabled]="!cashierOpen() || loading()">
                  <ion-icon slot="start" name="remove-circle-outline"></ion-icon>
                  Sangria
                </ion-button>
                <ion-button color="danger" (click)="close()" [disabled]="!cashierOpen() || loading()">
                  <ion-icon slot="start" name="lock-closed-outline"></ion-icon>
                  Fechar caixa
                </ion-button>
              </div>
            </ion-card-content>
          </ion-card>

          <ion-card class="data-card">
            <ion-card-header>
              <ion-card-title>Resumo de fechamento</ion-card-title>
              <ion-card-subtitle>Conciliação operacional inicial.</ion-card-subtitle>
            </ion-card-header>
            <ion-card-content>
              <div class="summary-list">
                <span><small>Saldo inicial</small><strong>{{ money(cashier()?.saldoInicial || 0) }}</strong></span>
                <span><small>Suprimentos</small><strong>{{ money(cashier()?.totalSuprimentos || 0) }}</strong></span>
                <span><small>Vendas recebidas</small><strong>{{ money(cashier()?.totalVendas || 0) }}</strong></span>
                <span><small>Sangrias</small><strong>{{ money(cashier()?.totalSangrias || 0) }}</strong></span>
                <span class="total"><small>Saldo previsto</small><strong>{{ money(cashier()?.saldoAtual || 0) }}</strong></span>
              </div>
            </ion-card-content>
          </ion-card>
        </section>

        <ion-card class="data-card">
          <ion-card-header>
            <ion-card-title>Movimentações</ion-card-title>
            <ion-card-subtitle>{{ movementCount() }} registros auditáveis no caixa atual.</ion-card-subtitle>
          </ion-card-header>
          <ion-card-content>
            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Tipo</th>
                    <th>Valor</th>
                    <th>Referência</th>
                    <th>Observação</th>
                    <th>Data</th>
                  </tr>
                </thead>
                <tbody>
                  @for (movement of cashier()?.movimentacoes || []; track movement.idCaixaMovimentacao || movement.criadoEm) {
                    <tr>
                      <td><span class="status-pill active">{{ movement.tipo }}</span></td>
                      <td>{{ money(movement.valor) }}</td>
                      <td>{{ movement.referenciaTipo || '-' }} {{ movement.referenciaId || '' }}</td>
                      <td>{{ movement.observacao || '-' }}</td>
                      <td>{{ asDate(movement.criadoEm) }}</td>
                    </tr>
                  } @empty {
                    <tr>
                      <td colspan="5">
                        <div class="empty-state">
                          <span><strong>Nenhuma movimentação</strong>Abra o caixa ou finalize uma venda no PDV.</span>
                        </div>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </ion-card-content>
        </ion-card>
      </main>
    </ion-content>
  `,
  styles: [`
    .cashier-page {
      max-width: 1500px;
    }

    .cashier-grid {
      align-items: start;
      margin: 12px 0;
    }

    .action-grid {
      display: grid;
      gap: 8px;
      grid-template-columns: repeat(3, minmax(0, 1fr));
    }

    .summary-list {
      display: grid;
      gap: 8px;
    }

    .summary-list span {
      align-items: center;
      background: var(--apex-card-muted);
      border: 1px solid var(--apex-border);
      border-radius: 8px;
      display: flex;
      justify-content: space-between;
      min-height: 42px;
      padding: 9px 11px;
    }

    .summary-list small {
      color: var(--apex-muted);
      font-size: 0.78rem;
    }

    .summary-list strong {
      color: var(--apex-ink);
    }

    .summary-list .total {
      background: var(--apex-primary);
      border-color: var(--apex-primary);
      color: #fff;
    }

    .summary-list .total small,
    .summary-list .total strong {
      color: #fff;
    }

    @media (max-width: 760px) {
      .action-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class CashierPage {
  private readonly api = inject(ApexApiService);
  readonly money = currency;
  readonly asDate = date;
  readonly cashier = signal<CaixaSessao | null>(null);
  readonly loading = signal(false);
  readonly message = signal('');
  readonly stateTone = signal<'success' | 'warning'>('success');
  readonly movementCount = computed(() => this.cashier()?.movimentacoes?.length ?? 0);
  readonly cashierOpen = computed(() => this.cashier()?.status === 'ABERTO');
  amount = 100;
  note = '';

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.api.currentCashier().pipe(
      catchError(() => {
        this.message.set('Não foi possível consultar o caixa. Verifique a API ou sua permissão.');
        this.stateTone.set('warning');
        return of(null);
      }),
      finalize(() => this.loading.set(false))
    ).subscribe((cashier) => {
      this.cashier.set(cashier);
      if (!cashier) {
        this.message.set('Caixa fechado. Abra o caixa para receber vendas e movimentações.');
        this.stateTone.set('warning');
      }
    });
  }

  openCashier(): void {
    this.run(() => this.api.openCashier(this.safeAmount()), 'Caixa aberto com sucesso.');
  }

  supply(): void {
    const cashier = this.cashier();
    if (!cashier || !this.cashierOpen()) {
      return;
    }
    this.run(() => this.api.cashierSupply(cashier.idCaixaSessao, this.safeAmount(), this.note || 'Suprimento operacional'), 'Suprimento registrado.');
  }

  withdraw(): void {
    const cashier = this.cashier();
    if (!cashier || !this.cashierOpen() || !window.confirm(`Registrar sangria de ${this.money(this.safeAmount())}?`)) {
      return;
    }
    this.run(() => this.api.cashierWithdraw(cashier.idCaixaSessao, this.safeAmount(), this.note || 'Sangria operacional'), 'Sangria registrada.');
  }

  close(): void {
    const cashier = this.cashier();
    if (!cashier || !this.cashierOpen() || !window.confirm(`Fechar o caixa com saldo previsto de ${this.money(cashier.saldoAtual)}?`)) {
      return;
    }
    this.run(() => this.api.closeCashier(cashier.idCaixaSessao), 'Caixa fechado e auditado.');
  }

  private run(action: () => ReturnType<ApexApiService['openCashier']>, successMessage: string): void {
    this.loading.set(true);
    action().pipe(
      catchError((error) => {
        this.message.set(error?.error?.detail || 'Operação de caixa não concluída.');
        this.stateTone.set('warning');
        return of(null);
      }),
      finalize(() => this.loading.set(false))
    ).subscribe((cashier) => {
      if (!cashier) {
        return;
      }
      this.cashier.set(cashier);
      this.message.set(successMessage);
      this.stateTone.set('success');
      this.note = '';
    });
  }

  private safeAmount(): number {
    return Math.max(0, Number(this.amount || 0));
  }
}
