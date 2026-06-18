import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { catchError, of } from 'rxjs';

import { ApexApiService } from '../core/apex-api.service';
import { MOCK_CLIENTS, MOCK_PRODUCTS } from '../core/mock-data';
import { CartItem, Cliente, CondicaoPagamento, Produto } from '../core/models';
import { currency } from '../core/view-utils';

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule],
  template: `
    <ion-content>
      <main class="page pos-page">
        <header class="page-header">
          <div><span class="page-kicker">Venda assistida</span><h1>PDV</h1>
            <p>Monte a venda, informe a condição pretendida e envie para recebimento pelo Caixa.</p></div>
          <ion-badge [color]="apiOffline() ? 'warning' : 'primary'">{{ apiOffline() ? 'API indisponível' : 'PDV conectado' }}</ion-badge>
        </header>

        <section class="pos-grid">
          <ion-card class="data-card">
            <ion-card-header><ion-card-title>Produtos</ion-card-title>
              <ion-card-subtitle>Busca por ID, código interno, código de barras ou descrição</ion-card-subtitle></ion-card-header>
            <ion-card-content>
              <ion-searchbar [ngModel]="search()" (ngModelChange)="search.set($event)" placeholder="ID, código ou descrição" debounce="100"></ion-searchbar>
              <div class="product-list">
                @for (product of filteredProducts(); track product.idProduto) {
                  <button type="button" (click)="addItem(product)" [disabled]="product.quantidadeEstoque <= 0">
                    <span><strong>{{ product.descricao }}</strong>
                      <small>#{{ product.idProduto }} · {{ product.codigoInterno || 'sem cód. interno' }} · {{ product.codigoBarras || 'sem código de barras' }}</small>
                      <small>{{ product.quantidadeEstoque }} {{ product.unidadeMedida || 'UN' }} em estoque</small></span>
                    <b>{{ money(product.valorVenda) }}</b>
                  </button>
                } @empty { <div class="empty-state">Nenhum produto encontrado.</div> }
              </div>
            </ion-card-content>
          </ion-card>

          <ion-card class="data-card">
            <ion-card-header><ion-card-title>Itens da venda</ion-card-title><ion-card-subtitle>{{ items().length }} produtos</ion-card-subtitle></ion-card-header>
            <ion-card-content>
              @for (item of items(); track item.produto.idProduto) {
                <ion-item class="cart-line">
                  <ion-label><strong>{{ item.produto.descricao }}</strong><p>{{ item.quantidade }} × {{ money(item.produto.valorVenda) }}</p></ion-label>
                  <ion-button fill="clear" (click)="updateQty(item, item.quantidade - 1)" aria-label="Remover unidade"><ion-icon slot="icon-only" name="remove-outline"></ion-icon></ion-button>
                  <ion-badge>{{ item.quantidade }}</ion-badge>
                  <ion-button fill="clear" (click)="updateQty(item, item.quantidade + 1)" [disabled]="item.quantidade >= item.produto.quantidadeEstoque" aria-label="Adicionar unidade"><ion-icon slot="icon-only" name="add-outline"></ion-icon></ion-button>
                </ion-item>
              } @empty { <div class="empty-state">Adicione itens para iniciar a venda.</div> }
            </ion-card-content>
          </ion-card>

          <ion-card class="data-card checkout-panel">
            <ion-card-header><ion-card-title>Envio ao Caixa</ion-card-title></ion-card-header>
            <ion-card-content class="stack">
              <ion-item><ion-select label="Cliente" label-placement="stacked" [(ngModel)]="clienteId">
                @for (client of clients(); track client.idCliente) { <ion-select-option [value]="client.idCliente">{{ client.nomeRazao }}</ion-select-option> }
              </ion-select></ion-item>
              <ion-item><ion-select label="Condição de pagamento pretendida" label-placement="stacked" [(ngModel)]="condicaoPagamentoId">
                @for (condition of paymentConditions(); track condition.idCondicaoPagamento) { <ion-select-option [value]="condition.idCondicaoPagamento">{{ condition.nome }}</ion-select-option> }
              </ion-select></ion-item>
              <ion-item><ion-input type="number" label="Desconto" label-placement="stacked" [ngModel]="desconto()" (ngModelChange)="setDiscount($event)"></ion-input></ion-item>
              <div class="summary"><span>Subtotal <strong>{{ money(subtotal()) }}</strong></span><span>Desconto <strong>{{ money(desconto()) }}</strong></span></div>
              <div class="total"><span>Total</span><strong>{{ money(total()) }}</strong></div>
              <ion-note>Status ao enviar: <strong>AGUARDANDO_CAIXA</strong>. A baixa de estoque ocorrerá na finalização.</ion-note>
              @if (message()) { <div class="state-line" [class.warning]="apiOffline()">{{ message() }}</div> }
              <ion-button expand="block" size="large" (click)="sendToCashier()" [disabled]="sending() || items().length === 0 || !clienteId || !condicaoPagamentoId">Enviar para o Caixa</ion-button>
              <ion-button expand="block" fill="outline" color="danger" (click)="cancelCurrentSale()" [disabled]="items().length === 0">Limpar venda</ion-button>
            </ion-card-content>
          </ion-card>
        </section>
      </main>
    </ion-content>
  `,
  styles: [`
    .pos-grid{display:grid;gap:14px;grid-template-columns:minmax(300px,.9fr) minmax(0,1.1fr) minmax(320px,.72fr)}
    ion-searchbar{padding:0 0 10px}.product-list{display:grid;gap:8px;max-height:62vh;overflow:auto}
    .product-list button{align-items:center;background:var(--apex-card-muted);border:1px solid var(--apex-border);border-radius:8px;color:var(--apex-ink);display:flex;gap:12px;justify-content:space-between;padding:11px;text-align:left;width:100%}
    .product-list button:hover{border-color:var(--apex-primary)}.product-list button:disabled{opacity:.55}.product-list span,.product-list strong,.product-list small{display:block}.product-list small{color:var(--apex-muted);font-size:.74rem;margin-top:4px}.product-list b{color:var(--apex-primary);white-space:nowrap}
    .cart-line,.checkout-panel ion-item{--background:var(--apex-card-muted);--border-radius:8px;border:1px solid var(--apex-border);border-radius:8px;margin-bottom:8px}
    .summary{display:grid;gap:8px;grid-template-columns:1fr 1fr}.summary span{background:var(--apex-card-muted);border:1px solid var(--apex-border);border-radius:8px;padding:10px}.summary strong{display:block;margin-top:4px}.total{background:var(--apex-primary);border-radius:8px;color:#fff;display:flex;justify-content:space-between;padding:16px}.total strong{font-size:1.5rem}
    @media(max-width:1180px){.pos-grid{grid-template-columns:1fr}}
  `]
})
export class PointOfSalePage {
  private readonly api = inject(ApexApiService);
  readonly money = currency;
  readonly products = signal<Produto[]>(MOCK_PRODUCTS);
  readonly clients = signal<Cliente[]>(MOCK_CLIENTS);
  readonly paymentConditions = signal<CondicaoPagamento[]>([]);
  readonly items = signal<CartItem[]>([]);
  readonly search = signal('');
  readonly desconto = signal(0);
  readonly message = signal('');
  readonly apiOffline = signal(false);
  readonly sending = signal(false);
  clienteId = MOCK_CLIENTS[0]?.idCliente;
  condicaoPagamentoId?: number;

  readonly subtotal = computed(() => this.items().reduce((sum, item) => sum + item.produto.valorVenda * item.quantidade, 0));
  readonly total = computed(() => Math.max(0, this.subtotal() - this.desconto()));
  readonly filteredProducts = computed(() => {
    const q = this.search().trim().toLowerCase();
    return this.products().filter((product) => !q || [String(product.idProduto ?? ''), product.codigoInterno, product.codigoBarras, product.descricao]
      .some((value) => value?.toLowerCase().includes(q)));
  });

  constructor() {
    this.api.products().pipe(catchError(() => { this.apiOffline.set(true); return of(MOCK_PRODUCTS); })).subscribe((value) => this.products.set(value));
    this.api.clients().pipe(catchError(() => of(MOCK_CLIENTS))).subscribe((value) => { this.clients.set(value); this.clienteId ??= value[0]?.idCliente; });
    this.api.paymentConditions().pipe(catchError(() => of([]))).subscribe((value) => { this.paymentConditions.set(value); this.condicaoPagamentoId ??= value[0]?.idCondicaoPagamento; });
  }

  addItem(product: Produto): void {
    if (!product.codigoBarras) { this.message.set('Produto vendável exige código de barras.'); return; }
    this.items.update((items) => {
      const current = items.find((item) => item.produto.idProduto === product.idProduto);
      if (current) return current.quantidade >= product.quantidadeEstoque ? items : items.map((item) => item === current ? { ...item, quantidade: item.quantidade + 1 } : item);
      return [...items, { produto: product, quantidade: 1 }];
    });
  }
  updateQty(item: CartItem, quantidade: number): void { this.items.update((items) => quantidade <= 0 ? items.filter((x) => x !== item) : items.map((x) => x === item ? { ...x, quantidade: Math.min(quantidade, item.produto.quantidadeEstoque) } : x)); }
  setDiscount(value: string | number | null | undefined): void { this.desconto.set(Math.max(0, Number(value || 0))); }

  sendToCashier(): void {
    if (!this.clienteId || !this.condicaoPagamentoId || this.items().length === 0) return;
    this.sending.set(true); this.message.set(''); this.apiOffline.set(false);
    this.api.checkoutVenda({ clienteId: this.clienteId, usuarioId: 0, desconto: this.desconto(), condicaoPagamentoId: this.condicaoPagamentoId,
      itens: this.items().map((item) => ({ produtoId: item.produto.idProduto ?? 0, quantidade: item.quantidade })), pagamentos: [], observacao: 'Venda enviada ao Caixa pelo PDV Apex Gestor' })
      .pipe(catchError((error) => { this.message.set(error?.error?.detail || 'Não foi possível enviar a venda ao Caixa.'); this.apiOffline.set(true); return of(null); }))
      .subscribe((response) => { this.sending.set(false); if (!response) return; this.items.set([]); this.desconto.set(0); this.message.set(`Venda #${String((response as { idVenda?: number }).idVenda ?? '')} enviada com status AGUARDANDO_CAIXA.`); });
  }
  cancelCurrentSale(): void { if (window.confirm('Limpar a venda atual?')) { this.items.set([]); this.desconto.set(0); this.message.set('Venda descartada sem movimentar estoque.'); } }
}
