import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IonContent, IonButton, IonIcon, IonSegment, IonSegmentButton, IonLabel, IonCard, IonCardHeader, IonCardTitle, IonCardSubtitle, IonCardContent, IonItem, IonSelect, IonSelectOption, IonInput } from '@ionic/angular/standalone';
import { catchError, finalize, forkJoin, of } from 'rxjs';

import { ApexApiService } from '../core/apex-api.service';
import { CaixaSessao, Categoria, ContaPagar, ContaReceber, FormaPagamento, Fornecedor } from '../core/models';

type FinanceTab = 'receber' | 'pagar' | 'caixas';

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule, IonContent, IonButton, IonIcon, IonSegment, IonSegmentButton, IonLabel, IonCard, IonCardHeader, IonCardTitle, IonCardSubtitle, IonCardContent, IonItem, IonSelect, IonSelectOption, IonInput],
  template: `
    <ion-content>
      <main class="page finance-page">
        <header class="page-header">
          <div>
            <span class="page-kicker">Financeiro operacional v1</span>
            <h1>Contas e conciliação</h1>
            <p>Rastro financeiro das vendas, compromissos e fechamentos de caixa por empresa e filial.</p>
          </div>
          <ion-button fill="outline" (click)="load()" [disabled]="loading()">
            <ion-icon slot="start" name="sync-outline"></ion-icon>Atualizar
          </ion-button>
        </header>

        @if (message()) {
          <div class="state-line" [class.warning]="error()">
            <ion-icon [name]="error() ? 'warning-outline' : 'checkmark-circle-outline'"></ion-icon>
            {{ message() }}
          </div>
        }

        <section class="metric-grid">
          <article class="metric-card"><span>A receber</span><strong>{{ receivableOpen() | currency:'BRL' }}</strong><small>{{ receivable().length }} títulos</small></article>
          <article class="metric-card"><span>A pagar</span><strong>{{ payableOpen() | currency:'BRL' }}</strong><small>{{ payable().length }} títulos</small></article>
          <article class="metric-card"><span>Caixas conciliados</span><strong>{{ closedCashiers() }}</strong><small>{{ cashiers().length }} sessões</small></article>
        </section>

        <ion-segment [value]="tab()" (ionChange)="tab.set($any($event.detail.value))">
          <ion-segment-button value="receber"><ion-label>Contas a receber</ion-label></ion-segment-button>
          <ion-segment-button value="pagar"><ion-label>Contas a pagar</ion-label></ion-segment-button>
          <ion-segment-button value="caixas"><ion-label>Conciliação de caixa</ion-label></ion-segment-button>
        </ion-segment>

        @if (tab() === 'receber') {
          <ion-card class="data-card">
            <ion-card-header><ion-card-title>Contas a receber</ion-card-title><ion-card-subtitle>Valores originados nas vendas e recebimentos do Caixa.</ion-card-subtitle></ion-card-header>
            <ion-card-content><div class="table-wrap"><table>
              <thead><tr><th>ID</th><th>Cliente / venda</th><th>Bruto</th><th>Desconto</th><th>Líquido</th><th>Pago</th><th>Saldo</th><th>Vencimento</th><th>Forma</th><th>Status</th><th>Escopo</th></tr></thead>
              <tbody>
                @for (row of receivable(); track row.idContaReceber) {
                  <tr><td>#{{ row.idContaReceber }}</td><td><strong>{{ row.clienteNome || 'Cliente #' + row.clienteId }}</strong><small>{{ row.numeroVenda || 'Venda #' + row.vendaId }}</small></td><td>{{ row.valorBruto | currency:'BRL' }}</td><td>{{ row.desconto | currency:'BRL' }}</td><td>{{ row.valorLiquido | currency:'BRL' }}</td><td>{{ row.valorPago | currency:'BRL' }}</td><td>{{ row.saldo | currency:'BRL' }}</td><td>{{ row.vencimento | date:'dd/MM/yyyy' }}</td><td>{{ row.formaPagamentoNome || 'Misto' }}</td><td><span class="status-pill active">{{ row.status }}</span></td><td>E{{ row.empresaId }} / F{{ row.filialId }}</td></tr>
                } @empty { <tr><td colspan="11">Nenhuma conta a receber no escopo autorizado.</td></tr> }
              </tbody>
            </table></div></ion-card-content>
          </ion-card>
        }

        @if (tab() === 'pagar') {
          <section class="split-grid">
            <ion-card class="data-card">
              <ion-card-header><ion-card-title>Contas a pagar</ion-card-title><ion-card-subtitle>Compromissos da empresa e filial autorizadas.</ion-card-subtitle></ion-card-header>
              <ion-card-content><div class="table-wrap"><table>
                <thead><tr><th>ID</th><th>Fornecedor</th><th>Categoria / descrição</th><th>Valor</th><th>Vencimento</th><th>Forma prevista</th><th>Status</th><th>Escopo</th></tr></thead>
                <tbody>
                  @for (row of payable(); track row.idContaPagar) {
                    <tr><td>#{{ row.idContaPagar }}</td><td>{{ row.fornecedorNome || 'Não informado' }}</td><td><strong>{{ row.categoriaNome || 'Geral' }}</strong><small>{{ row.descricao }}</small></td><td>{{ row.valor | currency:'BRL' }}</td><td>{{ row.vencimento | date:'dd/MM/yyyy' }}</td><td>{{ row.formaPagamentoPrevistaNome || 'A definir' }}</td><td><span class="status-pill active">{{ row.status }}</span></td><td>E{{ row.empresaId }} / F{{ row.filialId }}</td></tr>
                  } @empty { <tr><td colspan="8">Nenhuma conta a pagar no escopo autorizado.</td></tr> }
                </tbody>
              </table></div></ion-card-content>
            </ion-card>

            <ion-card class="data-card form-card">
              <ion-card-header><ion-card-title>Novo compromisso</ion-card-title><ion-card-subtitle>Cadastro mínimo com auditoria.</ion-card-subtitle></ion-card-header>
              <ion-card-content class="stack">
                <ion-item><ion-select label="Fornecedor" label-placement="stacked" [(ngModel)]="payableForm.fornecedorId"><ion-select-option [value]="undefined">Não informado</ion-select-option>@for (row of suppliers(); track row.idFornecedor) { <ion-select-option [value]="row.idFornecedor">{{ row.razaoSocial }}</ion-select-option> }</ion-select></ion-item>
                <ion-item><ion-select label="Categoria" label-placement="stacked" [(ngModel)]="payableForm.categoriaId"><ion-select-option [value]="undefined">Geral</ion-select-option>@for (row of categories(); track row.idCategoria) { <ion-select-option [value]="row.idCategoria">{{ row.descricao }}</ion-select-option> }</ion-select></ion-item>
                <ion-item><ion-input label="Descrição" label-placement="stacked" [(ngModel)]="payableForm.descricao"></ion-input></ion-item>
                <ion-item><ion-input type="number" min="0.01" step="0.01" label="Valor" label-placement="stacked" [(ngModel)]="payableForm.valor"></ion-input></ion-item>
                <ion-item><ion-input type="date" label="Vencimento" label-placement="stacked" [(ngModel)]="payableForm.vencimento"></ion-input></ion-item>
                <ion-item><ion-select label="Forma prevista" label-placement="stacked" [(ngModel)]="payableForm.formaPagamentoPrevistaId"><ion-select-option [value]="undefined">A definir</ion-select-option>@for (row of paymentMethods(); track row.idFormaPagamento) { <ion-select-option [value]="row.idFormaPagamento">{{ row.nome }}</ion-select-option> }</ion-select></ion-item>
                <ion-button expand="block" (click)="savePayable()" [disabled]="saving()">Salvar conta a pagar</ion-button>
              </ion-card-content>
            </ion-card>
          </section>
        }

        @if (tab() === 'caixas') {
          <section class="cashier-grid">
            @for (cashier of cashiers(); track cashier.idCaixaSessao) {
              <ion-card class="data-card">
                <ion-card-header><ion-card-title>Caixa #{{ cashier.idCaixaSessao }}</ion-card-title><ion-card-subtitle>{{ cashier.operadorLogin }} · E{{ cashier.empresaId }} / F{{ cashier.filialId }}</ion-card-subtitle></ion-card-header>
                <ion-card-content>
                  <dl><div><dt>Abertura</dt><dd>{{ cashier.abertoEm | date:'dd/MM/yyyy HH:mm' }}</dd></div><div><dt>Fechamento</dt><dd>{{ cashier.fechadoEm ? (cashier.fechadoEm | date:'dd/MM/yyyy HH:mm') : 'Em operação' }}</dd></div><div><dt>Total esperado</dt><dd>{{ cashier.saldoFinal ?? cashier.saldoAtual | currency:'BRL' }}</dd></div><div><dt>Total informado</dt><dd>{{ cashier.saldoInformado ?? 0 | currency:'BRL' }}</dd></div><div><dt>Diferença</dt><dd>{{ cashier.diferencaFechamento ?? 0 | currency:'BRL' }}</dd></div><div><dt>Vendas</dt><dd>{{ cashier.totalVendas | currency:'BRL' }}</dd></div><div><dt>Suprimentos</dt><dd>{{ cashier.totalSuprimentos | currency:'BRL' }}</dd></div><div><dt>Sangrias</dt><dd>{{ cashier.totalSangrias | currency:'BRL' }}</dd></div></dl>
                  <h3>Totais por forma de pagamento</h3>
                  <ul>@for (item of cashier.resumoPagamentos; track item.formaPagamentoId) { <li><span>{{ item.nome }} ({{ item.quantidadePagamentos }})</span><strong>{{ item.total | currency:'BRL' }}</strong></li> } @empty { <li>Nenhum recebimento nesta sessão.</li> }</ul>
                  <p class="audit-note"><ion-icon name="shield-checkmark-outline"></ion-icon>{{ cashier.movimentacoes.length }} movimentações no histórico auditável.</p>
                </ion-card-content>
              </ion-card>
            } @empty { <div class="empty-state"><strong>Nenhuma sessão de caixa</strong>Abra o Caixa operacional para iniciar a conciliação.</div> }
          </section>
        }
      </main>
    </ion-content>
  `,
  styles: [`
    .finance-page { max-width: 1600px; }
    .metric-grid { display:grid; gap:12px; grid-template-columns:repeat(3,minmax(0,1fr)); margin-bottom:16px; }
    .metric-card { background:var(--apex-surface); border:1px solid var(--apex-border); border-radius:16px; display:grid; gap:4px; padding:18px; }
    .metric-card span,.metric-card small { color:var(--apex-text-muted); } .metric-card strong { font-size:1.55rem; }
    ion-segment { margin-bottom:14px; }
    .split-grid { align-items:start; display:grid; gap:14px; grid-template-columns:minmax(0,1.5fr) minmax(320px,.5fr); }
    td small { color:var(--apex-text-muted); display:block; margin-top:3px; }
    .cashier-grid { display:grid; gap:14px; grid-template-columns:repeat(2,minmax(0,1fr)); }
    dl { display:grid; gap:8px; grid-template-columns:repeat(2,minmax(0,1fr)); } dl div { border-bottom:1px solid var(--apex-border); padding:8px 0; } dt { color:var(--apex-text-muted); font-size:.78rem; } dd { font-weight:700; margin:3px 0 0; }
    ul { list-style:none; margin:0; padding:0; } li { display:flex; justify-content:space-between; padding:7px 0; }
    .audit-note { align-items:center; color:var(--apex-text-muted); display:flex; gap:6px; margin-bottom:0; }
    @media (max-width:1000px) { .metric-grid,.split-grid,.cashier-grid { grid-template-columns:1fr; } }
  `]
})
export class FinanceOperationsPage {
  private readonly api = inject(ApexApiService);
  readonly tab = signal<FinanceTab>('receber');
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal(false);
  readonly message = signal('');
  readonly receivable = signal<ContaReceber[]>([]);
  readonly payable = signal<ContaPagar[]>([]);
  readonly cashiers = signal<CaixaSessao[]>([]);
  readonly suppliers = signal<Fornecedor[]>([]);
  readonly categories = signal<Categoria[]>([]);
  readonly paymentMethods = signal<FormaPagamento[]>([]);
  payableForm: Partial<ContaPagar> = this.emptyPayable();

  readonly receivableOpen = computed(() => this.receivable().reduce((sum, row) => sum + Number(row.saldo || 0), 0));
  readonly payableOpen = computed(() => this.payable().filter((row) => row.status !== 'PAGO' && row.status !== 'CANCELADO').reduce((sum, row) => sum + Number(row.valor || 0), 0));
  readonly closedCashiers = computed(() => this.cashiers().filter((row) => row.status === 'FECHADO').length);

  constructor() { this.load(); }

  load(): void {
    this.loading.set(true); this.error.set(false);
    forkJoin({
      receivable: this.api.accountsReceivable(), payable: this.api.accountsPayable(), cashiers: this.api.cashierHistory(),
      suppliers: this.api.list<Fornecedor>('/api/fornecedores'), categories: this.api.list<Categoria>('/api/categorias'), paymentMethods: this.api.paymentMethods()
    }).pipe(catchError(() => { this.error.set(true); this.message.set('Não foi possível carregar todo o financeiro no escopo autorizado.'); return of(null); }), finalize(() => this.loading.set(false)))
      .subscribe((data) => { if (!data) return; this.receivable.set(data.receivable); this.payable.set(data.payable); this.cashiers.set(data.cashiers); this.suppliers.set(data.suppliers); this.categories.set(data.categories); this.paymentMethods.set(data.paymentMethods); this.message.set('Financeiro atualizado com dados auditáveis.'); });
  }

  savePayable(): void {
    if (!this.payableForm.descricao?.trim() || Number(this.payableForm.valor) <= 0 || !this.payableForm.vencimento) { this.error.set(true); this.message.set('Informe descrição, valor e vencimento.'); return; }
    this.saving.set(true); this.api.createAccountPayable(this.payableForm).pipe(catchError((e) => { this.error.set(true); this.message.set(e?.error?.detail || 'Não foi possível salvar a conta.'); return of(null); }), finalize(() => this.saving.set(false)))
      .subscribe((saved) => { if (!saved) return; this.payable.update((rows) => [saved, ...rows]); this.payableForm = this.emptyPayable(); this.error.set(false); this.message.set(`Conta a pagar #${saved.idContaPagar} criada.`); });
  }

  private emptyPayable(): Partial<ContaPagar> { return { descricao: '', valor: 0, vencimento: new Date().toISOString().slice(0, 10) }; }
}
