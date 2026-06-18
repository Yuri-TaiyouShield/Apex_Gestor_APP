import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { Observable, catchError, finalize, of } from 'rxjs';

import { ApexApiService } from '../core/apex-api.service';
import { CaixaSessao, CreditoCliente, FormaPagamento, RecebimentoPagamento, Venda } from '../core/models';
import { currency, date } from '../core/view-utils';

interface PaymentLine extends RecebimentoPagamento { key: number; }

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule],
  template: `
    <ion-content><main class="page cashier-page">
      <header class="page-header"><div><span class="page-kicker">Recebimento operacional</span><h1>Caixa</h1>
        <p>Fila de vendas, pagamento misto, crédito rastreável, movimentações e conciliação.</p></div>
        <div class="page-actions"><ion-button fill="outline" (click)="load()"><ion-icon slot="start" name="sync-outline"></ion-icon>Atualizar</ion-button>
          @if (!cashierOpen()) { <ion-button (click)="openCashier()" [disabled]="loading()">Abrir caixa</ion-button> }</div></header>
      @if (message()) { <div class="state-line" [class.warning]="tone() === 'warning'">{{ message() }}</div> }

      <section class="metric-grid">
        <article class="metric-card"><header><span>Status</span><ion-icon name="radio-button-on-outline"></ion-icon></header><strong>{{ cashier()?.status || 'FECHADO' }}</strong><small>{{ cashier()?.operadorLogin || 'sem caixa aberto' }}</small></article>
        <article class="metric-card success"><header><span>Saldo atual</span><ion-icon name="cash-outline"></ion-icon></header><strong>{{ money(cashier()?.saldoAtual || 0) }}</strong><small>saldo esperado</small></article>
        <article class="metric-card indigo"><header><span>Fila</span><ion-icon name="hourglass-outline"></ion-icon></header><strong>{{ pendingSales().length }}</strong><small>aguardando recebimento</small></article>
        <article class="metric-card warning"><header><span>Vendas recebidas</span><ion-icon name="cart-outline"></ion-icon></header><strong>{{ money(cashier()?.totalVendas || 0) }}</strong><small>no caixa atual</small></article>
      </section>

      <ion-card class="data-card queue-card"><ion-card-header><ion-card-title>Vendas aguardando Caixa</ion-card-title>
        <ion-card-subtitle>Confira vendedor, cliente, itens, desconto e total antes de receber.</ion-card-subtitle></ion-card-header>
        <ion-card-content>
          <div class="queue-list">
            @for (sale of pendingSales(); track sale.idVenda) {
              <article class="queue-row" [class.selected]="selectedSale()?.idVenda === sale.idVenda">
                <button type="button" (click)="selectSale(sale)"><span><strong>#{{ sale.idVenda }} · {{ sale.numeroVenda }}</strong><small>{{ sale.clienteNome }} · vendedor {{ sale.usuarioNome }}</small><small>{{ sale.itens.length }} itens · desconto {{ money(sale.desconto) }}</small></span><b>{{ money(sale.valorTotal) }}</b></button>
              </article>
            } @empty { <div class="empty-state">Nenhuma venda aguardando recebimento.</div> }
          </div>
          @if (selectedSale(); as sale) {
            <section class="receive-panel">
              <div class="items-review">@for (item of sale.itens; track item.produtoId) { <span><strong>{{ item.quantidade }}× {{ item.descricao }}</strong><small>{{ money(item.precoTotal) }}</small></span> }</div>
              <div class="payment-header"><strong>Formas de pagamento</strong><ion-button size="small" fill="outline" (click)="addPayment()">Adicionar forma</ion-button></div>
              <div class="payment-list">
                @for (line of paymentLines(); track line.key; let i = $index) {
                  <div class="payment-row">
                    <ion-item><ion-select label="Forma" label-placement="stacked" [ngModel]="line.formaPagamentoId" (ngModelChange)="updateLine(i, 'formaPagamentoId', $event)">@for (method of paymentMethods(); track method.idFormaPagamento) { <ion-select-option [value]="method.idFormaPagamento">{{ method.nome }}</ion-select-option> }</ion-select></ion-item>
                    <ion-item><ion-input type="number" label="Valor" label-placement="stacked" [ngModel]="line.valor" (ngModelChange)="updateLine(i, 'valor', $event)"></ion-input></ion-item>
                    @if (isCreditMethod(line.formaPagamentoId)) {
                      <ion-item><ion-select label="Crédito com origem" label-placement="stacked" [ngModel]="line.creditoId" (ngModelChange)="updateLine(i, 'creditoId', $event)">@for (credit of credits(); track credit.idCredito) { <ion-select-option [value]="credit.idCredito">#{{ credit.idCredito }} · {{ money(credit.saldo) }} · venda {{ credit.vendaOrigemId }}</ion-select-option> }</ion-select></ion-item>
                    }
                    <ion-button fill="clear" color="danger" (click)="removePayment(i)" [disabled]="paymentLines().length === 1" aria-label="Remover forma"><ion-icon slot="icon-only" name="trash-outline"></ion-icon></ion-button>
                  </div>
                }
              </div>
              <div class="receive-summary"><span>Total <strong>{{ money(sale.valorTotal) }}</strong></span><span>Informado <strong>{{ money(paidTotal()) }}</strong></span><span>Restante <strong>{{ money(remaining()) }}</strong></span><span>Troco previsto <strong>{{ money(changeDue()) }}</strong></span></div>
              <div class="receive-actions"><ion-button (click)="receiveSelected()" [disabled]="!canReceive()">Confirmar recebimento</ion-button>
                <ion-button fill="outline" color="warning" (click)="rejectSelected()" [disabled]="loading()">Devolver para ajuste</ion-button></div>
              @if (!cashierOpen()) { <ion-note color="danger">Caixa fechado bloqueia o recebimento.</ion-note> }
              @if (hasCreditWithoutOrigin()) { <ion-note color="danger">Crédito/vale exige saldo com origem vinculada ao cliente.</ion-note> }
            </section>
          }
        </ion-card-content></ion-card>

      <section class="grid-two cashier-grid">
        <ion-card class="data-card"><ion-card-header><ion-card-title>Operações do caixa</ion-card-title></ion-card-header><ion-card-content class="stack">
          <ion-item><ion-input type="number" label="Valor" label-placement="stacked" [(ngModel)]="amount"></ion-input></ion-item>
          <ion-item><ion-input label="Observação" label-placement="stacked" [(ngModel)]="note"></ion-input></ion-item>
          <div class="action-grid"><ion-button fill="outline" (click)="supply()" [disabled]="!cashierOpen() || loading()">Suprimento</ion-button><ion-button fill="outline" color="warning" (click)="withdraw()" [disabled]="!cashierOpen() || loading()">Sangria</ion-button></div>
        </ion-card-content></ion-card>
        <ion-card class="data-card"><ion-card-header><ion-card-title>Conciliação de fechamento</ion-card-title></ion-card-header><ion-card-content class="stack">
          <div class="summary-list"><span><small>Saldo esperado</small><strong>{{ money(cashier()?.saldoAtual || 0) }}</strong></span><span><small>Vendas</small><strong>{{ money(cashier()?.totalVendas || 0) }}</strong></span><span><small>Diferença apurada</small><strong>{{ money(cashier()?.diferencaFechamento || 0) }}</strong></span></div>
          @for (summary of cashier()?.resumoPagamentos || []; track summary.formaPagamentoId) { <div class="payment-summary"><span>{{ summary.nome }}</span><strong>{{ money(summary.total) }}</strong></div> }
          <ion-item><ion-input type="number" label="Saldo contado" label-placement="stacked" [(ngModel)]="countedBalance"></ion-input></ion-item>
          <ion-button color="danger" (click)="close()" [disabled]="!cashierOpen() || loading()">Fechar e conciliar</ion-button>
        </ion-card-content></ion-card>
      </section>

      <ion-card class="data-card"><ion-card-header><ion-card-title>Movimentações</ion-card-title></ion-card-header><ion-card-content><div class="table-wrap"><table><thead><tr><th>Tipo</th><th>Valor</th><th>Referência</th><th>Data</th></tr></thead><tbody>
        @for (movement of cashier()?.movimentacoes || []; track movement.idCaixaMovimentacao) { <tr><td>{{ movement.tipo }}</td><td>{{ money(movement.valor) }}</td><td>{{ movement.referenciaTipo }} {{ movement.referenciaId || '' }}</td><td>{{ asDate(movement.criadoEm) }}</td></tr> }
      </tbody></table></div></ion-card-content></ion-card>
    </main></ion-content>
  `,
  styles: [`
    .cashier-page{max-width:1500px}.queue-card{margin-top:12px}.queue-list{display:grid;gap:8px}.queue-row button{align-items:center;background:var(--apex-card-muted);border:1px solid var(--apex-border);border-radius:8px;color:var(--apex-ink);display:flex;justify-content:space-between;padding:12px;text-align:left;width:100%}.queue-row.selected button{border-color:var(--apex-primary);box-shadow:0 0 0 2px var(--apex-primary-faint)}.queue-row span,.queue-row strong,.queue-row small{display:block}.queue-row small{color:var(--apex-muted);margin-top:3px}.queue-row b{font-size:1.05rem}.receive-panel{background:var(--apex-card-muted);border:1px solid var(--apex-border);border-radius:8px;display:grid;gap:10px;margin-top:12px;padding:12px}.items-review{display:grid;gap:6px}.items-review span,.receive-summary span{align-items:center;display:flex;justify-content:space-between}.items-review small{color:var(--apex-muted)}.payment-header{align-items:center;display:flex;justify-content:space-between}.payment-list{display:grid;gap:8px}.payment-row{align-items:end;display:grid;gap:8px;grid-template-columns:minmax(160px,1fr) minmax(120px,.6fr) minmax(190px,1fr) auto}.payment-row ion-item{--background:#fff;border:1px solid var(--apex-border);border-radius:8px}.receive-summary{display:grid;gap:8px;grid-template-columns:repeat(4,1fr)}.receive-summary span{background:#fff;border:1px solid var(--apex-border);border-radius:8px;padding:10px}.receive-actions,.action-grid{display:grid;gap:8px;grid-template-columns:1fr 1fr}.cashier-grid{align-items:start;margin:12px 0}.summary-list{display:grid;gap:8px}.summary-list span,.payment-summary{display:flex;justify-content:space-between}.summary-list small{color:var(--apex-muted)}.payment-summary{background:var(--apex-card-muted);border-radius:8px;padding:8px 10px}@media(max-width:900px){.payment-row,.receive-summary{grid-template-columns:1fr 1fr}}@media(max-width:760px){.receive-actions,.action-grid,.payment-row,.receive-summary{grid-template-columns:1fr}}
  `]
})
export class CashierPage {
  private readonly api = inject(ApexApiService);
  readonly money = currency; readonly asDate = date;
  readonly cashier = signal<CaixaSessao | null>(null); readonly pendingSales = signal<Venda[]>([]); readonly paymentMethods = signal<FormaPagamento[]>([]); readonly selectedSale = signal<Venda | null>(null);
  readonly credits = signal<CreditoCliente[]>([]); readonly paymentLines = signal<PaymentLine[]>([]);
  readonly loading = signal(false); readonly message = signal(''); readonly tone = signal<'success' | 'warning'>('success');
  readonly cashierOpen = computed(() => this.cashier()?.status === 'ABERTO');
  readonly paidTotal = computed(() => this.paymentLines().reduce((sum, line) => sum + Math.max(0, Number(line.valor || 0)), 0));
  readonly remaining = computed(() => Math.max(0, Number(this.selectedSale()?.valorTotal || 0) - this.paidTotal()));
  readonly changeDue = computed(() => Math.max(0, this.paidTotal() - Number(this.selectedSale()?.valorTotal || 0)));
  readonly hasCreditWithoutOrigin = computed(() => this.paymentLines().some((line) => this.isCreditMethod(line.formaPagamentoId) && !line.creditoId));
  amount = 100; note = ''; countedBalance = 0; private lineKey = 0;

  constructor() { this.load(); }
  ionViewWillEnter(): void { this.load(); }
  load(): void {
    this.loading.set(true);
    this.api.currentCashier().pipe(catchError(() => of(null)), finalize(() => this.loading.set(false))).subscribe((value) => { this.cashier.set(value); this.countedBalance = value?.saldoAtual ?? 0; if (!value) this.warn('Caixa fechado. Abra o caixa para receber vendas.'); });
    this.api.pendingCashierSales().pipe(catchError(() => of([]))).subscribe((value) => { this.pendingSales.set(value); const current = this.selectedSale(); if (current) this.selectedSale.set(value.find((x) => x.idVenda === current.idVenda) ?? null); });
    this.api.paymentMethods().pipe(catchError(() => of([]))).subscribe((value) => this.paymentMethods.set(value));
  }
  selectSale(sale: Venda): void {
    this.selectedSale.set(sale); this.credits.set([]);
    const defaultMethod = this.paymentMethods().find((m) => this.normalized(m.nome).includes('dinheiro')) ?? this.paymentMethods()[0];
    this.paymentLines.set(defaultMethod?.idFormaPagamento ? [{ key: ++this.lineKey, formaPagamentoId: defaultMethod.idFormaPagamento, valor: sale.valorTotal, numeroParcelas: 1 }] : []);
    this.api.cashierCredits(sale.clienteId).pipe(catchError(() => of([]))).subscribe((value) => this.credits.set(value));
  }
  addPayment(): void { const used = new Set(this.paymentLines().map((line) => line.formaPagamentoId)); const method = this.paymentMethods().find((item) => !used.has(item.idFormaPagamento ?? 0)); if (!method?.idFormaPagamento) { this.warn('Todas as formas disponíveis já foram adicionadas.'); return; } this.paymentLines.update((lines) => [...lines, { key: ++this.lineKey, formaPagamentoId: method.idFormaPagamento!, valor: this.remaining(), numeroParcelas: 1 }]); }
  removePayment(index: number): void { this.paymentLines.update((lines) => lines.filter((_, i) => i !== index)); }
  updateLine(index: number, field: 'formaPagamentoId' | 'valor' | 'creditoId', value: number | string | null): void { this.paymentLines.update((lines) => lines.map((line, i) => i === index ? { ...line, [field]: field === 'valor' ? Math.max(0, Number(value || 0)) : Number(value || 0) || undefined } : line)); }
  isCreditMethod(id?: number): boolean { const method = this.paymentMethods().find((item) => item.idFormaPagamento === id); const name = this.normalized(method?.nome || ''); return name.includes('credito') || name.includes('vale'); }
  canReceive(): boolean { return Boolean(this.cashierOpen() && !this.loading() && this.selectedSale() && this.paymentLines().length && this.remaining() === 0 && !this.hasCreditWithoutOrigin()); }
  receiveSelected(): void { const sale = this.selectedSale(); if (!sale || !this.canReceive()) return; const payments = this.paymentLines().map(({ key: _key, ...line }) => line); this.loading.set(true); this.api.receiveCashierSale(sale.idVenda, payments).pipe(catchError((e) => { this.warn(e?.error?.detail || 'Recebimento não concluído.'); return of(null); }), finalize(() => this.loading.set(false))).subscribe((result) => { if (!result) return; this.message.set(`Venda ${result.numeroVenda || result.idVenda} finalizada. Troco: ${this.money(result.troco || 0)}.`); this.tone.set('success'); this.selectedSale.set(null); this.paymentLines.set([]); this.load(); }); }
  rejectSelected(): void { const sale = this.selectedSale(); if (!sale) return; const motivo = window.prompt('Motivo obrigatório para devolver a venda ao Vendedor:')?.trim(); if (!motivo) { this.warn('Informe o motivo da recusa.'); return; } this.loading.set(true); this.api.rejectCashierSale(sale.idVenda, motivo).pipe(catchError((e) => { this.warn(e?.error?.detail || 'Recusa não registrada.'); return of(null); }), finalize(() => this.loading.set(false))).subscribe((result) => { if (!result) return; this.message.set('Venda devolvida ao Vendedor para ajuste.'); this.tone.set('success'); this.selectedSale.set(null); this.load(); }); }
  openCashier(): void { this.run(this.api.openCashier(Math.max(0, Number(this.amount || 0))), 'Caixa aberto.'); }
  supply(): void { const c = this.cashier(); if (c) this.run(this.api.cashierSupply(c.idCaixaSessao, this.safeAmount(), this.note), 'Suprimento registrado.'); }
  withdraw(): void { const c = this.cashier(); if (c && window.confirm(`Registrar sangria de ${this.money(this.safeAmount())}?`)) this.run(this.api.cashierWithdraw(c.idCaixaSessao, this.safeAmount(), this.note), 'Sangria registrada.'); }
  close(): void { const c = this.cashier(); if (c && window.confirm('Fechar e conciliar este caixa?')) this.run(this.api.closeCashier(c.idCaixaSessao, Number(this.countedBalance || 0)), 'Caixa fechado e conciliado.'); }
  private run(action: Observable<CaixaSessao>, success: string): void { this.loading.set(true); action.pipe(catchError((e) => { this.warn(e?.error?.detail || 'Operação de caixa não concluída.'); return of(null); }), finalize(() => this.loading.set(false))).subscribe((value) => { if (!value) return; this.cashier.set(value); this.message.set(success); this.tone.set('success'); this.load(); }); }
  private warn(value: string): void { this.message.set(value); this.tone.set('warning'); }
  private safeAmount(): number { return Math.max(0, Number(this.amount || 0)); }
  private normalized(value: string): string { return value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase(); }
}
