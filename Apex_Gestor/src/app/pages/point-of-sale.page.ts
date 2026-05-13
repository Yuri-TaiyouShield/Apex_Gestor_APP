import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { catchError, of } from 'rxjs';

import { ApexApiService } from '../core/apex-api.service';
import { MOCK_CLIENTS, MOCK_PAYMENT_METHODS, MOCK_PRODUCTS } from '../core/mock-data';
import { CartItem, Cliente, FormaPagamento, Produto } from '../core/models';
import { currency } from '../core/view-utils';

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule],
  template: `
    <ion-content>
      <main class="page pos-page">
        <header class="page-header">
          <div>
            <h1>PDV / Caixa</h1>
            <p>Venda rápida para desktop com layout produtivo e pronto para atalhos.</p>
          </div>
          <ion-badge color="primary">Desktop + Mobile Staff</ion-badge>
        </header>

        <section class="pos-grid">
          <ion-card class="data-card">
            <ion-card-header>
              <ion-card-title>Produtos</ion-card-title>
            </ion-card-header>
            <ion-card-content>
              <ion-searchbar [ngModel]="search()" (ngModelChange)="search.set($event)" placeholder="Buscar por nome ou código de barras" debounce="100"></ion-searchbar>
              <div class="product-pick-list">
                @for (product of filteredProducts(); track product.idProduto) {
                  <button type="button" (click)="addItem(product)">
                    <span>{{ product.descricao }}</span>
                    <strong>{{ money(product.valorVenda) }}</strong>
                  </button>
                }
              </div>
            </ion-card-content>
          </ion-card>

          <ion-card class="data-card">
            <ion-card-header>
              <ion-card-title>Itens da venda</ion-card-title>
            </ion-card-header>
            <ion-card-content>
              @for (item of items(); track item.produto.idProduto) {
                <ion-item>
                  <ion-label>
                    <strong>{{ item.produto.descricao }}</strong>
                    <p>{{ item.quantidade }}x {{ money(item.produto.valorVenda) }}</p>
                  </ion-label>
                  <ion-button fill="clear" (click)="updateQty(item, item.quantidade - 1)">-</ion-button>
                  <ion-badge>{{ item.quantidade }}</ion-badge>
                  <ion-button fill="clear" (click)="updateQty(item, item.quantidade + 1)">+</ion-button>
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
      grid-template-columns: minmax(280px, 0.8fr) minmax(0, 1.2fr) minmax(320px, 0.7fr);
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
      background: #f8fafc;
      border: 1px solid var(--apex-border);
      border-radius: 8px;
      color: #0f172a;
      display: flex;
      justify-content: space-between;
      min-height: 52px;
      padding: 10px 12px;
      text-align: left;
    }

    .total-box {
      background: #0f766e;
      border-radius: 8px;
      color: #ffffff;
      display: flex;
      justify-content: space-between;
      padding: 18px;
    }

    .total-box strong {
      font-size: 1.6rem;
    }

    @media (max-width: 1180px) {
      .pos-grid {
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
  clienteId = MOCK_CLIENTS[0]?.idCliente;
  formaPagamentoId = MOCK_PAYMENT_METHODS[0]?.idFormaPagamento;
  readonly desconto = signal(0);

  readonly filteredProducts = computed(() => {
    const query = this.search().trim().toLowerCase();
    return this.products().filter((product) => !query || [product.descricao, product.codigoBarras].some((value) => value?.toLowerCase().includes(query)));
  });

  readonly total = computed(() =>
    Math.max(0, this.items().reduce((sum, item) => sum + item.produto.valorVenda * item.quantidade, 0) - Number(this.desconto() || 0))
  );

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
  }

  updateQty(item: CartItem, quantidade: number): void {
    this.items.update((items) => quantidade <= 0
      ? items.filter((current) => current.produto.idProduto !== item.produto.idProduto)
      : items.map((current) => current.produto.idProduto === item.produto.idProduto ? { ...current, quantidade } : current)
    );
  }

  setDiscount(value: string | number | null | undefined): void {
    this.desconto.set(Number(value || 0));
  }

  finishSale(): void {
    if (!this.clienteId || !this.formaPagamentoId) {
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
    }).pipe(catchError(() => of({ offline: true }))).subscribe(() => {
      this.items.set([]);
      this.desconto.set(0);
      this.message.set('Venda finalizada. Estoque atualizado pela API quando o backend estiver conectado.');
    });
  }
}
