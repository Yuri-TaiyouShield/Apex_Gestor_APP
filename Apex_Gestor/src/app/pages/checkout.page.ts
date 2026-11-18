import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { IonContent, IonCard, IonCardHeader, IonCardTitle, IonCardContent, IonItem, IonSelect, IonSelectOption, IonTextarea, IonNote, IonButton } from '@ionic/angular/standalone';
import { catchError, of, switchMap } from 'rxjs';

import { ApexApiService } from '../core/apex-api.service';
import { CartService } from '../core/cart.service';
import { Cliente, FormaPagamento } from '../core/models';
import { currency } from '../core/view-utils';

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule, IonContent, IonCard, IonCardHeader, IonCardTitle, IonCardContent, IonItem, IonSelect, IonSelectOption, IonTextarea, IonNote, IonButton, RouterLink],
  template: `
    <ion-content>
      <main class="page">
        <header class="page-header">
          <div>
            <h1>Checkout simplificado</h1>
            <p>Pedido B2C convertido em venda e integrado ao estoque do ERP.</p>
          </div>
        </header>

        <section class="grid-two">
          <ion-card class="data-card">
            <ion-card-header>
              <ion-card-title>Dados da compra</ion-card-title>
            </ion-card-header>
            <ion-card-content class="stack">
              <ion-item>
                <ion-select label="Cliente" label-placement="stacked" [(ngModel)]="clienteId" interface="popover">
                  @for (client of clients(); track client.idCliente) {
                    <ion-select-option [value]="client.idCliente">{{ client.nomeRazao }}</ion-select-option>
                  }
                </ion-select>
              </ion-item>
              <ion-item>
                <ion-select label="Forma de pagamento" label-placement="stacked" [(ngModel)]="formaPagamentoId" interface="popover">
                  @for (method of paymentMethods(); track method.idFormaPagamento) {
                    <ion-select-option [value]="method.idFormaPagamento">{{ method.nome }}</ion-select-option>
                  }
                </ion-select>
              </ion-item>
              <ion-item>
                <ion-textarea label="Observacao" label-placement="stacked" [(ngModel)]="observacao" placeholder="Retirada, entrega ou instrucoes do pedido"></ion-textarea>
              </ion-item>
              @if (message()) {
                <ion-note color="primary">{{ message() }}</ion-note>
              }
              <ion-button expand="block" (click)="finish()" [disabled]="cart.items().length === 0 || !clienteId || !formaPagamentoId">
                Confirmar pedido
              </ion-button>
            </ion-card-content>
          </ion-card>

          <ion-card class="data-card">
            <ion-card-header>
              <ion-card-title>Resumo do pedido</ion-card-title>
            </ion-card-header>
            <ion-card-content class="stack">
              @for (item of cart.items(); track item.produto.idProduto) {
                <div class="checkout-line">
                  <span>{{ item.quantidade }}x {{ item.produto.descricao }}</span>
                  <strong>{{ money(item.produto.valorVenda * item.quantidade) }}</strong>
                </div>
              }
              <div class="checkout-total">
                <span>Total</span>
                <strong>{{ money(cart.subtotal()) }}</strong>
              </div>
              <ion-button fill="outline" expand="block" routerLink="/cart">Editar carrinho</ion-button>
            </ion-card-content>
          </ion-card>
        </section>
      </main>
    </ion-content>
  `,
  styles: [`
    .checkout-line,
    .checkout-total {
      align-items: center;
      display: flex;
      justify-content: space-between;
    }

    .checkout-total {
      border-top: 1px solid var(--apex-border);
      font-size: 1.2rem;
      padding-top: 14px;
    }
  `]
})
export class CheckoutPage {
  private readonly api = inject(ApexApiService);
  private readonly router = inject(Router);
  readonly cart = inject(CartService);
  readonly money = currency;
  readonly clients = signal<Cliente[]>([]);
  readonly paymentMethods = signal<FormaPagamento[]>([]);
  readonly message = signal('');
  clienteId?: number;
  formaPagamentoId?: number;
  observacao = '';

  constructor() {
    this.api.clients().pipe(catchError(() => of([] as Cliente[]))).subscribe((clients) => { this.clients.set(clients); this.clienteId ??= clients[0]?.idCliente; });
    this.api.paymentMethods().pipe(catchError(() => of([] as FormaPagamento[]))).subscribe((methods) => { this.paymentMethods.set(methods); this.formaPagamentoId ??= methods[0]?.idFormaPagamento; });
  }

  finish(): void {
    if (!this.clienteId || !this.formaPagamentoId) {
      return;
    }
    const selectedMethod = this.paymentMethods().find((method) => method.idFormaPagamento === this.formaPagamentoId);
    const itens = this.cart.items()
      .map((item) => ({ produtoId: item.produto.idProduto ?? 0, quantidade: item.quantidade }))
      .filter((item) => item.produtoId > 0);

    this.api.mergeB2cCart({
      clienteId: this.clienteId,
      itens
    }).pipe(
      switchMap(() => this.api.checkoutVenda({
        clienteId: this.clienteId!,
        usuarioId: 1,
        desconto: 0,
        observacao: this.observacao,
        itens,
        pagamentos: [
          {
            formaPagamentoId: this.formaPagamentoId!,
            nome: selectedMethod?.nome ?? 'Pagamento',
            valorPago: this.cart.subtotal()
          }
        ]
      })),
      catchError((error) => { this.message.set(error?.error?.detail || 'Não foi possível confirmar o pedido com a API.'); return of(null); })
    ).subscribe((result) => {
      if (!result) return;
      this.message.set('Pedido confirmado. O estoque sera reservado e baixado pela venda.');
      this.cart.clear();
      setTimeout(() => this.router.navigateByUrl('/store'), 900);
    });
  }
}
