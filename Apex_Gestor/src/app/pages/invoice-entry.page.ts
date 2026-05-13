import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { catchError, of } from 'rxjs';

import { ApexApiService } from '../core/apex-api.service';
import { Identifiable } from '../core/models';
import { currency } from '../core/view-utils';

interface InvoiceDraft {
  numero: string;
  serie: string;
  valorTotal: number;
}

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule],
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
              <ion-card-subtitle>Selecione um XML de NF-e para pré-conferência.</ion-card-subtitle>
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
                A etapa completa deve validar fornecedor, itens, CFOP, custos e divergências antes de efetivar estoque.
              </ion-note>
            </ion-card-content>
          </ion-card>
        </section>
      </main>
    </ion-content>
  `,
  styles: [`
    input[type="file"] {
      background: #f8fafc;
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
    valorTotal: 0
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
        valorTotal: Number(doc.querySelector('vNF')?.textContent ?? 0)
      };
      this.message.set('XML lido. Confira os dados antes de registrar.');
    };
    reader.readAsText(file);
  }

  submit(): void {
    const payload: Identifiable = {
      numero: this.draft.numero,
      serie: this.draft.serie,
      valorTotal: Number(this.draft.valorTotal ?? 0),
      dataEmissao: new Date().toISOString(),
      dataEntrada: new Date().toISOString(),
      fornecedor: { idFornecedor: 1 },
      itens: []
    };
    this.api.invoiceEntry(payload).pipe(catchError(() => of({ offline: true }))).subscribe(() => {
      this.message.set('Entrada registrada ou simulada. A próxima etapa é conferir os itens para atualizar custos e estoque.');
    });
  }
}
