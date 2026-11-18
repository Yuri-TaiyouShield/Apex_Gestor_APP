import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IonContent, IonCard, IonCardHeader, IonCardTitle, IonCardSubtitle, IonCardContent, IonItem, IonInput, IonButton, IonNote } from '@ionic/angular/standalone';
import { catchError, of } from 'rxjs';

import { ApexApiService } from '../core/apex-api.service';
import { Identifiable } from '../core/models';
import { currency } from '../core/view-utils';

interface InvoiceDraft {
  numero: string;
  serie: string;
  valorTotal: number;
  fornecedorId: number | null;
  itens: Array<{ produtoId: number; quantidade: number; valorCustoUnitario: number }>;
}

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule, IonContent, IonCard, IonCardHeader, IonCardTitle, IonCardSubtitle, IonCardContent, IonItem, IonInput, IonButton, IonNote],
  template: `
    <ion-content>
      <main class="page">
        <header class="page-header">
          <div>
            <h1>Entrada de Mercadorias por XML</h1>
            <p>Importação de NF-e para alimentar compras, custo e estoque.</p>
          </div>
        </header>

        <section class="grid-two">
          <ion-card class="data-card">
            <ion-card-header>
              <ion-card-title>XML da Nota Fiscal</ion-card-title>
              <ion-card-subtitle>Selecione um XML de NF-e para pré-conferência. A efetivação exige fornecedor e itens conferidos.</ion-card-subtitle>
            </ion-card-header>
            <ion-card-content class="stack">
              <input type="file" accept=".xml,text/xml" (change)="onFileSelected($event)" />
              <ion-item>
                <ion-input label="Número" label-placement="stacked" [(ngModel)]="draft.numero"></ion-input>
              </ion-item>
              <ion-item>
                <ion-input label="Série" label-placement="stacked" [(ngModel)]="draft.serie"></ion-input>
              </ion-item>
              <ion-item>
                <ion-input type="number" label="Valor total" label-placement="stacked" [(ngModel)]="draft.valorTotal"></ion-input>
              </ion-item>
              <ion-button expand="block" (click)="submit()">Registrar entrada</ion-button>
              @if (message()) {
                <ion-note color="primary">{{ message() }}</ion-note>
              }
            </ion-card-content>
          </ion-card>

          <ion-card class="data-card">
            <ion-card-header>
              <ion-card-title>Conferência</ion-card-title>
              <ion-card-subtitle>Resumo extraído do XML.</ion-card-subtitle>
            </ion-card-header>
            <ion-card-content class="stack">
              <div class="nf-summary">
                <span>Número</span>
                <strong>{{ draft.numero || '-' }}</strong>
              </div>
              <div class="nf-summary">
                <span>Série</span>
                <strong>{{ draft.serie || '-' }}</strong>
              </div>
              <div class="nf-summary">
                <span>Total</span>
                <strong>{{ money(draft.valorTotal || 0) }}</strong>
              </div>
              <ion-note>
                O XML atualmente fornece apenas dados de cabeçalho. A entrada fica bloqueada até a tela permitir selecionar o fornecedor e mapear cada item para um produto do tenant.
              </ion-note>
            </ion-card-content>
          </ion-card>
        </section>
      </main>
    </ion-content>
  `,
  styles: [`
    input[type="file"] {
      background: var(--apex-bg-input);
      color: var(--apex-text-primary);
      border: 1px dashed var(--apex-border);
      border-radius: 8px;
      padding: 16px;
      width: 100%;
    }

    .nf-summary {
      display: flex;
      justify-content: space-between;
    }
  `]
})
export class InvoiceEntryPage {
  private readonly api = inject(ApexApiService);
  readonly money = currency;
  readonly message = signal('');
  draft: InvoiceDraft = {
    numero: '',
    serie: '',
    valorTotal: 0,
    fornecedorId: null,
    itens: []
  };

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      const xml = String(reader.result ?? '');
      const doc = new DOMParser().parseFromString(xml, 'text/xml');
      this.draft = {
        numero: doc.querySelector('nNF')?.textContent ?? '',
        serie: doc.querySelector('serie')?.textContent ?? '',
        valorTotal: Number(doc.querySelector('vNF')?.textContent ?? 0),
        fornecedorId: null,
        itens: []
      };
      this.message.set('XML lido. Pré-conferência concluída. O registro está bloqueado até fornecedor e itens serem conferidos.');
    };
    reader.readAsText(file);
  }

  submit(): void {
    if (!this.draft.numero || !this.draft.serie || Number(this.draft.valorTotal) <= 0) {
      this.message.set('Informe número, série e valor total válidos antes de continuar.');
      return;
    }
    if (!this.draft.fornecedorId || this.draft.itens.length === 0) {
      this.message.set('Entrada não efetivada: selecione o fornecedor e mapeie os itens para produtos do tenant.');
      return;
    }
    const payload: Identifiable = {
      numero: this.draft.numero,
      serie: this.draft.serie,
      valorTotal: Number(this.draft.valorTotal ?? 0),
      dataEmissao: new Date().toISOString(),
      fornecedorId: this.draft.fornecedorId,
      itens: this.draft.itens
    };
    this.api.invoiceEntry(payload).pipe(catchError((error) => { this.message.set(error?.error?.detail || 'Não foi possível registrar a entrada com a API.'); return of(null); })).subscribe((response) => {
      if (!response) return;
      this.message.set('Entrada registrada e enviada para atualização de custos e estoque.');
    });
  }
}
