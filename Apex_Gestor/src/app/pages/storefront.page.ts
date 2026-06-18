import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { catchError, of } from 'rxjs';

import { ApexApiService } from '../core/apex-api.service';
import { ApiConfigService } from '../core/api-config.service';
import { CartService } from '../core/cart.service';
import { MOCK_PRODUCTS } from '../core/mock-data';
import { Produto } from '../core/models';
import { currency } from '../core/view-utils';

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule, RouterLink],
  template: `
    <ion-content>
      <main class="store-page">
        <section class="store-hero">
          <div>
            <strong>Apex Store</strong>
            <h1>Materiais, ferramentas e ofertas para sua obra.</h1>
            <p>Experiência B2C com busca rápida, filtros e checkout conectado ao estoque do ERP.</p>
            <div class="hero-actions">
              <ion-searchbar [ngModel]="query()" (ngModelChange)="query.set($event)" placeholder="Buscar cimento, tinta, furadeira..." debounce="150"></ion-searchbar>
              <ion-button routerLink="/cart">
                <ion-icon slot="start" name="cart-outline"></ion-icon>
                Carrinho ({{ cart.itemCount() }})
              </ion-button>
            </div>
          </div>
        </section>

        <section class="quick-filters">
          @for (category of categories(); track category) {
            <ion-chip [outline]="selectedCategory() !== category" (click)="selectedCategory.set(category)">
              <ion-label>{{ category }}</ion-label>
            </ion-chip>
          }
        </section>

        <section class="product-grid">
          @for (product of filteredProducts(); track product.idProduto) {
            <article class="commerce-card product-card">
              <div class="product-media">
                @if (product.imagemUrl) {
                  <img [src]="productImageUrl(product)" [alt]="product.descricao" loading="lazy">
                } @else {
                  <ion-icon name="cube-outline"></ion-icon>
                }
              </div>
              <div class="product-body">
                <ion-badge color="success" *ngIf="product.quantidadeEstoque > 0">Disponível</ion-badge>
                <h2>{{ product.descricao }}</h2>
                <p>{{ product.marca || 'Apex' }} | {{ product.categoria?.descricao || 'Geral' }}</p>
                <strong>{{ money(product.valorVenda) }}</strong>
                <small>{{ product.quantidadeEstoque }} {{ product.unidadeMedida || 'UN' }} em estoque</small>
                <ion-button expand="block" (click)="cart.addProduct(product)">
                  <ion-icon slot="start" name="cart-outline"></ion-icon>
                  Comprar
                </ion-button>
              </div>
            </article>
          }
        </section>
      </main>
    </ion-content>
  `,
  styles: [`
    .store-page {
      margin: 0 auto;
      max-width: 1500px;
      padding: 18px;
    }

    .store-hero {
      background: linear-gradient(135deg, rgba(15, 118, 110, 0.96), rgba(37, 99, 235, 0.92));
      border-radius: 8px;
      color: #ffffff;
      min-height: 310px;
      padding: clamp(24px, 5vw, 58px);
    }

    .store-hero h1 {
      font-size: clamp(2rem, 5vw, 4.4rem);
      letter-spacing: 0;
      line-height: 0.98;
      margin: 8px 0 14px;
      max-width: 860px;
    }

    .store-hero p {
      color: rgba(255, 255, 255, 0.88);
      font-size: 1.05rem;
      max-width: 650px;
    }

    .hero-actions {
      align-items: center;
      display: grid;
      gap: 12px;
      grid-template-columns: minmax(0, 560px) auto;
      margin-top: 26px;
    }

    .hero-actions ion-searchbar {
      --background: #ffffff;
      --border-radius: 8px;
      padding: 0;
    }

    .quick-filters {
      display: flex;
      gap: 8px;
      margin: 16px 0;
      overflow-x: auto;
    }

    .product-grid {
      display: grid;
      gap: 16px;
      grid-template-columns: repeat(4, minmax(0, 1fr));
    }

    .product-card {
      display: grid;
      overflow: hidden;
    }

    .product-media {
      align-items: center;
      background: linear-gradient(135deg, #f1f5f9, #dbeafe);
      display: flex;
      height: 160px;
      justify-content: center;
    }

    .product-media img {
      height: 100%;
      object-fit: contain;
      padding: 12px;
      width: 100%;
    }

    .product-media ion-icon {
      color: #0f766e;
      font-size: 52px;
    }

    .product-body {
      display: grid;
      gap: 9px;
      padding: 14px;
    }

    .product-body h2 {
      font-size: 1rem;
      line-height: 1.25;
      margin: 0;
      min-height: 42px;
    }

    .product-body p,
    .product-body small {
      color: #64748b;
      margin: 0;
    }

    .product-body strong {
      font-size: 1.32rem;
    }

    @media (max-width: 1100px) {
      .product-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }
    }

    @media (max-width: 680px) {
      .hero-actions,
      .product-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class StorefrontPage {
  private readonly api = inject(ApexApiService);
  private readonly apiConfig = inject(ApiConfigService);
  readonly cart = inject(CartService);
  readonly money = currency;
  readonly products = signal<Produto[]>(MOCK_PRODUCTS);
  readonly query = signal('');
  readonly selectedCategory = signal('Todos');

  readonly categories = computed(() => {
    return ['Todos', ...new Set(this.products().map((product) => product.categoria?.descricao ?? 'Geral'))];
  });

  readonly filteredProducts = computed(() => {
    const query = this.query().trim().toLowerCase();
    const category = this.selectedCategory();
    return this.products().filter((product) => {
      const matchesQuery = !query || [product.descricao, product.marca, product.codigoBarras].some((value) => value?.toLowerCase().includes(query));
      const matchesCategory = category === 'Todos' || (product.categoria?.descricao ?? 'Geral') === category;
      return matchesQuery && matchesCategory;
    });
  });

  constructor() {
    this.api.products().pipe(catchError(() => of(MOCK_PRODUCTS))).subscribe((products) => this.products.set(products));
  }

  productImageUrl(product: Produto): string {
    if (!product.imagemUrl) {
      return '';
    }
    return product.imagemUrl.startsWith('/') ? this.apiConfig.apiUrl(product.imagemUrl) : product.imagemUrl;
  }
}
