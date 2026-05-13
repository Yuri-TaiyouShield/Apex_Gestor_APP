import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IonicModule } from '@ionic/angular';

import { CartService } from '../core/cart.service';
import { currency } from '../core/view-utils';

@Component({
  standalone: true,
  imports: [CommonModule, IonicModule, RouterLink],
  template: `
    <ion-content>
      <main class="page">
        <header class="page-header">
          <div>
            <h1>Carrinho</h1>
            <p>Itens reservados para checkout e baixa de estoque após confirmação.</p>
          </div>
          <ion-button routerLink="/checkout" [disabled]="cart.items().length === 0">Finalizar compra</ion-button>
        </header>

        <section class="grid-two">
          <ion-card class="data-card">
            <ion-card-content>
              @if (cart.items().length === 0) {
                <div class="empty-state">Seu carrinho está vazio.</div>
              } @else {
                @for (item of cart.items(); track item.produto.idProduto) {
                  <ion-item>
                    <ion-icon slot="start" name="cube-outline"></ion-icon>
                    <ion-label>
                      <strong>{{ item.produto.descricao }}</strong>
                      <p>{{ money(item.produto.valorVenda) }} cada</p>
                    </ion-label>
                    <ion-button fill="clear" (click)="cart.updateQuantity(item.produto.idProduto, item.quantidade - 1)">-</ion-button>
                    <ion-badge color="primary">{{ item.quantidade }}</ion-badge>
                    <ion-button fill="clear" (click)="cart.updateQuantity(item.produto.idProduto, item.quantidade + 1)">+</ion-button>
                    <ion-button fill="clear" color="danger" (click)="cart.removeProduct(item.produto.idProduto!)">
                      <ion-icon name="trash-outline"></ion-icon>
                    </ion-button>
                  </ion-item>
                }
              }
            </ion-card-content>
          </ion-card>

          <ion-card class="data-card">
            <ion-card-header>
              <ion-card-title>Resumo</ion-card-title>
            </ion-card-header>
            <ion-card-content class="stack">
              <div class="summary-row">
                <span>Itens</span>
                <strong>{{ cart.itemCount() }}</strong>
              </div>
              <div class="summary-row">
                <span>Subtotal</span>
                <strong>{{ money(cart.subtotal()) }}</strong>
              </div>
              <ion-button expand="block" routerLink="/checkout" [disabled]="cart.items().length === 0">Ir para checkout</ion-button>
              <ion-button expand="block" fill="outline" routerLink="/store">Continuar comprando</ion-button>
            </ion-card-content>
          </ion-card>
        </section>
      </main>
    </ion-content>
  `,
  styles: [`
    .summary-row {
      align-items: center;
      display: flex;
      justify-content: space-between;
    }
  `]
})
export class CartPage {
  readonly cart = inject(CartService);
  readonly money = currency;
}
