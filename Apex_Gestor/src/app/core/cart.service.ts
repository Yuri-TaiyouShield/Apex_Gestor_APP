import { Injectable, computed, signal } from '@angular/core';

import { CartItem, Produto } from './models';

@Injectable({ providedIn: 'root' })
export class CartService {
  readonly items = signal<CartItem[]>([]);
  readonly itemCount = computed(() => this.items().reduce((total, item) => total + item.quantidade, 0));
  readonly subtotal = computed(() => this.items().reduce((total, item) => total + item.produto.valorVenda * item.quantidade, 0));

  addProduct(produto: Produto, quantidade = 1): void {
    this.items.update((items) => {
      const existing = items.find((item) => item.produto.idProduto === produto.idProduto);
      if (existing) {
        return items.map((item) =>
          item.produto.idProduto === produto.idProduto ? { ...item, quantidade: item.quantidade + quantidade } : item
        );
      }
      return [...items, { produto, quantidade }];
    });
  }

  updateQuantity(produtoId: number | undefined, quantidade: number): void {
    if (!produtoId) {
      return;
    }
    if (quantidade <= 0) {
      this.removeProduct(produtoId);
      return;
    }
    this.items.update((items) => items.map((item) => (item.produto.idProduto === produtoId ? { ...item, quantidade } : item)));
  }

  removeProduct(produtoId: number): void {
    this.items.update((items) => items.filter((item) => item.produto.idProduto !== produtoId));
  }

  clear(): void {
    this.items.set([]);
  }
}
