import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { catchError, of } from 'rxjs';

import { ApexApiService } from '../core/apex-api.service';
import { MOCK_CLIENTS, MOCK_PAYMENT_METHODS, MOCK_PRODUCTS } from '../core/mock-data';
import { CartItem, Cliente, FormaPagamento, Produto } from '../core/models';
import { currency } from '../core/view-utils';

interface LastReceipt {
  id: string;
  total: number;
  payment: string;
  offline: boolean;
}

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule],
  template: `
    <ion-content>
      <main class="page pos-page">
        <header class="page-header">
          <div>
            <span class="page-kicker">Venda assistida</span>
            <h1>PDV</h1>
            <p>Venda rápida para desktop com desconto, pagamento, cancelamento e integração com caixa aberto quando aplicável.</p>
          </div>
          <ion-badge [color]="apiOffline() ? 'warning' : 'primary'">{{ apiOffline() ? 'Modo offline assistido' : 'PDV conectado' }}</ion-badge>
        </header>

        <section class="pos-grid">
          <ion-card class="data-card">
            <ion-card-header>
              <ion-card-title>Produtos</ion-card-title>
              <ion-card-subtitle>Busca por nome, marca ou código, com alerta de estoque</ion-card-subtitle>
            </ion-card-header>
            <ion-card-content>
              <ion-searchbar [ngModel]="search()" (ngModelChange)="search.set($event)" placeholder="Buscar por nome ou código de barras" debounce="100"></ion-searchbar>
              <div class="product-pick-list">
                @for (product of filteredProducts(); track product.idProduto) {
                  <button type="button" (click)="addItem(product)" [class.low-stock]="isLowStock(product)" [disabled]="product.quantidadeEstoque <= 0">
                    <span>
                      <strong>{{ product.descricao }}</strong>
                      <small>{{ product.marca || 'Apex' }} | {{ product.quantidadeEstoque }} {{ product.unidadeMedida || 'UN' }} em estoque | mín. {{ product.estoqueMinimo || 0 }}</small>
                    </span>
                    <b>{{ money(product.valorVenda) }}</b>
                  </button>
                } @empty {
                  <div class="empty-state">Nenhum produto encontrado para a busca.</div>
                }
              </div>
            </ion-card-content>
          </ion-card>

          <ion-card class="data-card">
            <ion-card-header>
              <ion-card-title>Itens da venda</ion-card-title>
              <ion-card-subtitle>{{ items().length }} itens no cupom</ion-card-subtitle>
            </ion-card-header>
            <ion-card-content>
              @for (item of items(); track item.produto.idProduto) {
                <ion-item class="cart-line">
                  <ion-label>
                    <strong>{{ item.produto.descricao }}</strong>
                    <p>{{ item.quantidade }}x {{ money(item.produto.valorVenda) }}</p>
                  </ion-label>
                  <ion-button fill="clear" (click)="updateQty(item, item.quantidade - 1)" aria-label="Remover uma unidade" title="Remover">
                    <ion-icon slot="icon-only" name="remove-outline"></ion-icon>
                  </ion-button>
                  <ion-badge>{{ item.quantidade }}</ion-badge>
                  <ion-button fill="clear" (click)="updateQty(item, item.quantidade + 1)" aria-label="Adicionar uma unidade" title="Adicionar">
                    <ion-icon slot="icon-only" name="add-outline"></ion-icon>
                  </ion-button>
                </ion-item>
              } @empty {
                <div class="empty-state">Adicione itens para iniciar a venda.</div>
              }
            </ion-card-content>
          </ion-card>

          <ion-card class="data-card checkout-panel">
            <ion-card-header>
              <ion-card-title>Fechamento</ion-card-title>
            </ion-card-header>
            <ion-card-content class="stack">
              <ion-item>
                <ion-select label="Cliente" label-placement="stacked" [(ngModel)]="clienteId">
                  @for (client of clients(); track client.idCliente) {
                    <ion-select-option [value]="client.idCliente">{{ client.nomeRazao }}</ion-select-option>
                  }
                </ion-select>
              </ion-item>
              <ion-item>
                <ion-select label="Forma de pagamento" label-placement="stacked" [(ngModel)]="formaPagamentoId">
                  @for (method of paymentMethods(); track method.idFormaPagamento) {
                    <ion-select-option [value]="method.idFormaPagamento">{{ method.nome }}</ion-select-option>
                  }
                </ion-select>
              </ion-item>
              <ion-item>
                <ion-input type="number" label="Desconto" label-placement="stacked" [ngModel]="desconto()" (ngModelChange)="setDiscount($event)"></ion-input>
              </ion-item>
              <ion-item>
                <ion-input type="number" label="Valor recebido" label-placement="stacked" [ngModel]="paidAmount()" (ngModelChange)="setPaidAmount($event)"></ion-input>
              </ion-item>
              <div class="checkout-lines">
                <span>
                  <small>Subtotal</small>
                  <strong>{{ money(subtotal()) }}</strong>
                </span>
                <span>
                  <small>Desconto</small>
                  <strong>{{ money(desconto()) }}</strong>
                </span>
                <span>
                  <small>Recebido</small>
                  <strong>{{ money(paidAmount()) }}</strong>
                </span>
                <span>
                  <small>Troco</small>
                  <strong>{{ money(changeDue()) }}</strong>
                </span>
              </div>
              <div class="total-box">
                <span>Total</span>
                <strong>{{ money(total()) }}</strong>
              </div>
              @if (message()) {
                <ion-note color="primary">{{ message() }}</ion-note>
              }
              <ion-button expand="block" size="large" (click)="finishSale()" [disabled]="items().length === 0 || !clienteId || !formaPagamentoId">
                Finalizar venda
              </ion-button>
              <ion-button expand="block" fill="outline" color="danger" (click)="cancelCurrentSale()" [disabled]="items().length === 0">
                Cancelar venda
              </ion-button>
            </ion-card-content>
          </ion-card>

          <ion-card class="data-card activity-panel">
            <ion-card-header>
              <ion-card-title>Turno e auditoria</ion-card-title>
              <ion-card-subtitle>Último recibo e resumo operacional da venda.</ion-card-subtitle>
            </ion-card-header>
            <ion-card-content>
              @if (lastReceipt()) {
                <div class="receipt-box">
                  <span>Última venda</span>
                  <strong>#{{ lastReceipt()?.id }} - {{ money(lastReceipt()?.total || 0) }}</strong>
                  <small>{{ lastReceipt()?.payment }} {{ lastReceipt()?.offline ? '| pendente de sincronização' : '| confirmado na API' }}</small>
                </div>
              } @else {
                <div class="empty-state">Finalize uma venda para gerar recibo do turno.</div>
              }

            </ion-card-content>
          </ion-card>
        </section>
      </main>
    </ion-content>
  `,
  styles: [`
    .pos-grid {
      display: grid;
      gap: 16px;
      grid-template-columns: minmax(300px, 0.86fr) minmax(0, 1.14fr) minmax(330px, 0.72fr);
    }

    ion-searchbar {
      padding: 0 0 12px;
    }

    .product-pick-list {
      display: grid;
      gap: 8px;
      max-height: 62vh;
      overflow: auto;
    }

    .product-pick-list button {
      align-items: center;
      background: var(--apex-card-muted);
      border: 1px solid var(--apex-border);
      border-radius: 8px;
      color: var(--apex-ink);
      display: flex;
      gap: 14px;
      justify-content: space-between;
      min-height: 64px;
      padding: 11px 12px;
      text-align: left;
      transition: border-color 120ms ease, transform 120ms ease, background 120ms ease;
      width: 100%;
    }

    .product-pick-list button:hover {
      background: var(--apex-primary-faint);
      border-color: rgba(15, 118, 110, 0.32);
      transform: translateY(-1px);
    }

    .product-pick-list button.low-stock {
      border-color: rgba(180, 105, 14, 0.42);
    }

    .product-pick-list button:disabled {
      cursor: not-allowed;
      opacity: 0.58;
      transform: none;
    }

    .product-pick-list span,
    .product-pick-list strong,
    .product-pick-list small,
    .product-pick-list b {
      display: block;
    }

    .product-pick-list span {
      min-width: 0;
    }

    .product-pick-list strong {
      font-size: 0.9rem;
      line-height: 1.22;
    }

    .product-pick-list small {
      color: var(--apex-muted);
      font-size: 0.76rem;
      margin-top: 5px;
    }

    .product-pick-list b {
      color: var(--apex-primary-strong);
      flex: 0 0 auto;
      font-size: 0.94rem;
      font-weight: 850;
    }

    .cart-line {
      --background: var(--apex-card-muted);
      --border-radius: 8px;
      --inner-padding-end: 10px;
      border: 1px solid var(--apex-border);
      border-radius: 8px;
      margin-bottom: 8px;
    }

    .cart-line ion-button {
      min-height: 36px;
      width: 36px;
    }

    .checkout-panel ion-item {
      --background: var(--apex-card-muted);
      --border-radius: 8px;
      --inner-padding-end: 12px;
      border: 1px solid var(--apex-border);
      border-radius: 8px;
    }

    .checkout-lines {
      display: grid;
      gap: 8px;
      grid-template-columns: 1fr 1fr;
    }

    .checkout-lines span {
      background: var(--apex-card-muted);
      border: 1px solid var(--apex-border);
      border-radius: 8px;
      padding: 10px;
    }

    .checkout-lines small,
    .checkout-lines strong {
      display: block;
    }

    .checkout-lines small {
      color: var(--apex-muted);
      font-size: 0.74rem;
      margin-bottom: 3px;
    }

    .checkout-lines strong {
      color: var(--apex-ink);
      font-size: 0.98rem;
    }

    .total-box {
      background: var(--apex-primary);
      border-radius: 8px;
      color: #ffffff;
      display: flex;
      justify-content: space-between;
      padding: 18px;
    }

    .total-box strong {
      font-size: 1.6rem;
    }

    .activity-panel {
      grid-column: 1 / -1;
    }

    .activity-panel ion-card-content {
      display: grid;
      gap: 12px;
      grid-template-columns: minmax(260px, 0.45fr) minmax(0, 1fr);
    }

    .receipt-box,
    .movement-row {
      background: var(--apex-card-muted);
      border: 1px solid var(--apex-border);
      border-radius: 8px;
      padding: 12px;
    }

    .receipt-box span,
    .receipt-box strong,
    .receipt-box small,
    .movement-row strong,
    .movement-row small {
      display: block;
    }

    .receipt-box span,
    .receipt-box small,
    .movement-row small {
      color: var(--apex-muted);
      font-size: 0.78rem;
    }

    .receipt-box strong {
      color: var(--apex-ink);
      margin: 4px 0;
    }

    .movement-list {
      display: grid;
      gap: 8px;
    }

    .movement-row {
      align-items: center;
      display: flex;
      gap: 12px;
      justify-content: space-between;
    }

    .movement-row b {
      color: var(--apex-primary-strong);
      white-space: nowrap;
    }

    @media (max-width: 1180px) {
      .pos-grid {
        grid-template-columns: 1fr;
      }

      .activity-panel ion-card-content {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class PointOfSalePage {
  private readonly api = inject(ApexApiService);
  readonly money = currency;
  readonly products = signal<Produto[]>(MOCK_PRODUCTS);
  readonly clients = signal<Cliente[]>(MOCK_CLIENTS);
  readonly paymentMethods = signal<FormaPagamento[]>(MOCK_PAYMENT_METHODS);
  readonly items = signal<CartItem[]>([]);
  readonly search = signal('');
  readonly message = signal('');
  readonly apiOffline = signal(false);
  readonly paidAmount = signal(0);
  readonly shiftSalesCount = signal(0);
  readonly shiftSalesTotal = signal(0);
  readonly lastReceipt = signal<LastReceipt | null>(null);
  clienteId = MOCK_CLIENTS[0]?.idCliente;
  formaPagamentoId = MOCK_PAYMENT_METHODS[0]?.idFormaPagamento;
  readonly desconto = signal(0);

  readonly subtotal = computed(() => this.items().reduce((sum, item) => sum + item.produto.valorVenda * item.quantidade, 0));
  readonly filteredProducts = computed(() => {
    const query = this.search().trim().toLowerCase();
    return this.products().filter((product) => !query || [product.descricao, product.codigoBarras].some((value) => value?.toLowerCase().includes(query)));
  });

  readonly total = computed(() => Math.max(0, this.subtotal() - Number(this.desconto() || 0)));
  readonly changeDue = computed(() => Math.max(0, Number(this.paidAmount() || 0) - this.total()));

  constructor() {
    this.api.products().pipe(catchError(() => of(MOCK_PRODUCTS))).subscribe((products) => this.products.set(products));
    this.api.clients().pipe(catchError(() => of(MOCK_CLIENTS))).subscribe((clients) => this.clients.set(clients));
    this.api.paymentMethods().pipe(catchError(() => of(MOCK_PAYMENT_METHODS))).subscribe((methods) => this.paymentMethods.set(methods));
  }

  addItem(product: Produto): void {
    this.items.update((items) => {
      const existing = items.find((item) => item.produto.idProduto === product.idProduto);
      if (existing) {
        return items.map((item) => item.produto.idProduto === product.idProduto ? { ...item, quantidade: item.quantidade + 1 } : item);
      }
      return [...items, { produto: product, quantidade: 1 }];
    });
    this.syncPaidAmount();
  }

  updateQty(item: CartItem, quantidade: number): void {
    this.items.update((items) => quantidade <= 0
      ? items.filter((current) => current.produto.idProduto !== item.produto.idProduto)
      : items.map((current) => current.produto.idProduto === item.produto.idProduto ? { ...current, quantidade } : current)
    );
    this.syncPaidAmount();
  }

  setDiscount(value: string | number | null | undefined): void {
    this.desconto.set(Math.max(0, Number(value || 0)));
    this.syncPaidAmount();
  }

  setPaidAmount(value: string | number | null | undefined): void {
    this.paidAmount.set(Math.max(0, Number(value || 0)));
  }

  isLowStock(product: Produto): boolean {
    return product.quantidadeEstoque <= Number(product.estoqueMinimo ?? 0);
  }

  finishSale(): void {
    if (!this.clienteId || !this.formaPagamentoId) {
      return;
    }
    if (this.paidAmount() < this.total()) {
      this.message.set('Valor recebido menor que o total da venda.');
      return;
    }
    const method = this.paymentMethods().find((payment) => payment.idFormaPagamento === this.formaPagamentoId);
    this.api.checkoutVenda({
      clienteId: this.clienteId,
      usuarioId: 1,
      desconto: Number(this.desconto() || 0),
      itens: this.items().map((item) => ({ produtoId: item.produto.idProduto ?? 0, quantidade: item.quantidade })).filter((item) => item.produtoId > 0),
      pagamentos: [{ formaPagamentoId: this.formaPagamentoId, nome: method?.nome ?? 'Pagamento', valorPago: this.total() }],
      observacao: 'Venda realizada no PDV Apex Gestor'
    }).pipe(catchError((error) => {
      if (error?.status && error.status !== 0) {
        this.message.set(error?.error?.detail || 'Venda bloqueada pela regra operacional. Verifique caixa aberto, permissão ou desconto.');
        return of(null);
      }
      this.apiOffline.set(true);
      return of({ offline: true });
    })).subscribe((response) => {
      if (!response) {
        return;
      }
      const offline = Boolean((response as { offline?: boolean }).offline);
      const receipt: LastReceipt = {
        id: offline ? `OFF-${Date.now()}` : String((response as { idVenda?: unknown; id?: unknown }).idVenda ?? (response as { id?: unknown }).id ?? Date.now()),
        total: this.total(),
        payment: method?.nome ?? 'Pagamento',
        offline
      };
      this.lastReceipt.set(receipt);
      this.shiftSalesCount.update((value) => value + 1);
      this.shiftSalesTotal.update((value) => value + receipt.total);
      this.items.set([]);
      this.desconto.set(0);
      this.paidAmount.set(0);
      this.message.set(offline ? 'Venda simulada offline. Sincronize antes do fechamento definitivo.' : 'Venda finalizada. Estoque e auditoria atualizados pela API.');
    });
  }

  cancelCurrentSale(): void {
    if (!window.confirm('Cancelar a venda atual? Nenhum item será faturado ou movimentado.')) {
      return;
    }
    this.items.set([]);
    this.desconto.set(0);
    this.paidAmount.set(0);
    this.message.set('Venda cancelada antes da finalização. Nenhum estoque foi movimentado.');
  }

  private syncPaidAmount(): void {
    const total = this.total();
    if (this.items().length === 0) {
      this.paidAmount.set(0);
      return;
    }
    if (this.paidAmount() < total) {
      this.paidAmount.set(total);
    }
  }
}
