import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IonContent, IonSearchbar, IonSelect, IonSelectOption, IonButton, IonCard, IonCardContent, IonCardHeader, IonCardTitle, IonCardSubtitle } from '@ionic/angular/standalone';
import { catchError, finalize, of } from 'rxjs';

import { ApexApiService } from '../core/apex-api.service';
import { SessionService } from '../core/session.service';
import { Venda, VendaDevolucaoRequest, VendaStatus } from '../core/models';
import { currency, date } from '../core/view-utils';

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule, IonContent, IonSearchbar, IonSelect, IonSelectOption, IonButton, IonCard, IonCardContent, IonCardHeader, IonCardTitle, IonCardSubtitle],
  template: `
    <ion-content><main class="page history-page">
      <header class="page-header"><div><span class="page-kicker">Pós-venda auditável</span><h1>Histórico de Vendas</h1>
        <p>Localize por ID, número, cliente, vendedor, produto, código interno ou código de barras.</p></div></header>
      <section class="filter-bar"><ion-searchbar [(ngModel)]="query" placeholder="ID, cliente, produto ou código de barras" (keyup.enter)="load()"></ion-searchbar>
        <ion-select [(ngModel)]="status" placeholder="Todos os status"> <ion-select-option value="">Todos</ion-select-option>@for (item of statuses; track item) { <ion-select-option [value]="item">{{ item }}</ion-select-option> }</ion-select>
        <ion-button (click)="load()" [disabled]="loading()">Buscar</ion-button></section>
      @if (message()) { <div class="state-line" [class.warning]="error()">{{ message() }}</div> }
      <ion-card class="data-card"><ion-card-content><div class="table-wrap"><table><thead><tr><th>ID</th><th>Status</th><th>Cliente / vendedor</th><th>Itens</th><th>Total</th><th>Data</th><th>Ações</th></tr></thead><tbody>
        @for (sale of sales(); track sale.idVenda) { <tr><td><strong>#{{ sale.idVenda }}</strong><small>{{ sale.numeroVenda }}</small></td><td><span class="status-pill" [class.active]="sale.status === 'FINALIZADA'">{{ sale.status }}</span>@if (sale.motivoRecusa) { <small>Motivo: {{ sale.motivoRecusa }}</small> }</td><td>{{ sale.clienteNome }}<small>{{ sale.usuarioNome }}</small></td><td>{{ sale.itens.length }}<small>@for (item of sale.itens; track item.produtoId) { {{ item.quantidade }}× {{ item.descricao }} · }</small></td><td>{{ money(sale.valorTotal) }}</td><td>{{ asDate(sale.dataVenda) }}</td><td><ion-button size="small" fill="outline" (click)="select(sale)">Detalhes</ion-button></td></tr> }
        @empty { <tr><td colspan="7"><div class="empty-state">Nenhuma venda encontrada para os filtros.</div></td></tr> }
      </tbody></table></div></ion-card-content></ion-card>

      @if (selected(); as sale) { <ion-card class="data-card detail-card"><ion-card-header><ion-card-title>{{ sale.numeroVenda || '#' + sale.idVenda }}</ion-card-title><ion-card-subtitle>{{ sale.status }} · empresa {{ sale.empresaId }} / filial {{ sale.filialId }}</ion-card-subtitle></ion-card-header><ion-card-content>
        <div class="detail-grid"><span><small>Subtotal</small><strong>{{ money(sale.valorTotal + sale.desconto) }}</strong></span><span><small>Desconto</small><strong>{{ money(sale.desconto) }}</strong></span><span><small>Total</small><strong>{{ money(sale.valorTotal) }}</strong></span><span><small>Caixa</small><strong>{{ sale.caixaSessaoId || '-' }}</strong></span></div>
        @if (canReturn() && (sale.status === 'FINALIZADA' || sale.status === 'TROCA_PARCIAL')) { <div class="return-actions"><ion-button color="warning" fill="outline" (click)="operateReturn(sale, 'PARCIAL')">Devolução parcial</ion-button><ion-button color="danger" fill="outline" (click)="operateReturn(sale, 'TOTAL')">Devolução total</ion-button><ion-button fill="outline" (click)="operateReturn(sale, 'TROCA')">Trocar item</ion-button></div> }
      </ion-card-content></ion-card> }
    </main></ion-content>
  `,
  styles: [`
    .filter-bar{align-items:center;background:var(--apex-bg-surface);border:1px solid var(--apex-border);border-radius:8px;display:grid;gap:8px;grid-template-columns:minmax(260px,1fr) 220px auto;margin-bottom:12px;padding:8px}.filter-bar ion-searchbar{padding:0}td small{color:var(--apex-muted);display:block;font-size:.74rem;margin-top:3px}.detail-card{margin-top:12px}.detail-grid{display:grid;gap:8px;grid-template-columns:repeat(4,1fr)}.detail-grid span{background:var(--apex-card-muted);border:1px solid var(--apex-border);border-radius:8px;padding:10px}.detail-grid small,.detail-grid strong{display:block}.detail-grid small{color:var(--apex-muted)}.return-actions{display:flex;flex-wrap:wrap;gap:8px;margin-top:12px}@media(max-width:800px){.filter-bar,.detail-grid{grid-template-columns:1fr}}
  `]
})
export class SalesHistoryPage {
  private readonly api = inject(ApexApiService); private readonly session = inject(SessionService);
  readonly sales = signal<Venda[]>([]); readonly selected = signal<Venda | null>(null); readonly loading = signal(false); readonly message = signal(''); readonly error = signal(false);
  readonly money = currency; readonly asDate = date; query = ''; status = '';
  readonly statuses: VendaStatus[] = ['EM_ATENDIMENTO', 'AGUARDANDO_CAIXA', 'EM_PAGAMENTO', 'PAGA', 'FINALIZADA', 'CANCELADA', 'DEVOLVIDA', 'TROCA_PARCIAL', 'TROCA_TOTAL'];
  readonly canReturn = computed(() => this.session.hasAnyRole(['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_ADMIN', 'ROLE_GERENTE']));
  constructor() { this.load(); }
  ionViewWillEnter(): void { this.load(); }
  load(): void { this.loading.set(true); this.api.salesHistory(this.query, this.status).pipe(catchError((e) => { this.message.set(e?.error?.detail || 'Não foi possível consultar o histórico.'); this.error.set(true); return of([]); }), finalize(() => this.loading.set(false))).subscribe((value) => { this.sales.set(value); if (!this.error()) this.message.set(`${value.length} venda(s) encontrada(s).`); }); }
  select(sale: Venda): void { this.selected.set(sale); }
  operateReturn(sale: Venda, tipo: VendaDevolucaoRequest['tipo']): void {
    const motivo = window.prompt('Informe o motivo obrigatório:')?.trim(); if (!motivo) { this.message.set('Motivo obrigatório.'); this.error.set(true); return; }
    let itens: VendaDevolucaoRequest['itens'];
    if (tipo === 'TOTAL') itens = sale.itens.map((item) => ({ produtoOriginalId: item.produtoId, quantidade: item.quantidade }));
    else { const produtoId = Number(window.prompt('ID do produto original:', String(sale.itens[0]?.produtoId ?? ''))); const quantidade = Number(window.prompt('Quantidade:', '1')); if (!produtoId || quantidade < 1) return; const produtoNovoId = tipo === 'TROCA' ? Number(window.prompt('ID do novo produto:')) : undefined; if (tipo === 'TROCA' && !produtoNovoId) return; itens = [{ produtoOriginalId: produtoId, produtoNovoId, quantidade }]; }
    this.loading.set(true); this.api.returnSale(sale.idVenda, { tipo, motivo, itens }).pipe(catchError((e) => { this.message.set(e?.error?.detail || 'Troca/devolução não concluída.'); this.error.set(true); return of(null); }), finalize(() => this.loading.set(false))).subscribe((result) => { if (!result) return; this.message.set(`${tipo} registrada com vínculo à venda original.`); this.error.set(false); this.selected.set(null); this.load(); });
  }
}
