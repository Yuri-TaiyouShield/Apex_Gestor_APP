import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { catchError, finalize, of } from 'rxjs';

import { ApexApiService } from '../core/apex-api.service';
import { CondicaoPagamento } from '../core/models';

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule],
  template: `
    <ion-content>
      <main class="page payment-page">
        <header class="page-header">
          <div>
            <span class="page-kicker">Financeiro v1</span>
            <h1>Condições de Pagamento</h1>
            <p>Regras de parcelas, prazo e tipo usadas no PDV, vendas e financeiro.</p>
          </div>
          <div class="page-actions">
            <ion-button fill="outline" (click)="load()">
              <ion-icon slot="start" name="sync-outline"></ion-icon>
              Atualizar
            </ion-button>
            <ion-button (click)="startNew()">
              <ion-icon slot="start" name="add-outline"></ion-icon>
              Novo
            </ion-button>
          </div>
        </header>

        @if (message()) {
          <div class="state-line" [class.warning]="offline()">
            <ion-icon [name]="offline() ? 'warning-outline' : 'checkmark-circle-outline'" aria-hidden="true"></ion-icon>
            {{ message() }}
          </div>
        }

        <section class="payment-grid">
          <ion-card class="data-card">
            <ion-card-header>
              <ion-card-title>Condições cadastradas</ion-card-title>
              <ion-card-subtitle>{{ filtered().length }} registros no filtro atual.</ion-card-subtitle>
            </ion-card-header>
            <ion-card-content>
              <ion-searchbar [ngModel]="searchTerm()" (ngModelChange)="searchTerm.set($event)" placeholder="Pesquisar por nome, tipo ou restrição" debounce="180"></ion-searchbar>
              <div class="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Nome</th>
                      <th>Tipo</th>
                      <th>Parcelas</th>
                      <th>Prazo</th>
                      <th>%</th>
                      <th>Status</th>
                      <th>Ações</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (condition of filtered(); track condition.idCondicaoPagamento || condition.nome) {
                      <tr [class.selected]="selected()?.idCondicaoPagamento === condition.idCondicaoPagamento">
                        <td>{{ condition.nome }}</td>
                        <td>{{ condition.tipoPagamento }}</td>
                        <td>{{ condition.quantidadeParcelas }}</td>
                        <td>{{ condition.prazoDias }} dias</td>
                        <td>{{ condition.percentual }}</td>
                        <td>
                          <span class="status-pill" [class.active]="condition.status !== 0" [class.inactive]="condition.status === 0">
                            {{ condition.status === 0 ? 'Inativa' : 'Ativa' }}
                          </span>
                        </td>
                        <td>
                          <div class="table-actions">
                            <ion-button fill="clear" size="small" (click)="edit(condition)" title="Editar" aria-label="Editar condição">
                              <ion-icon slot="icon-only" name="create-outline"></ion-icon>
                            </ion-button>
                            <ion-button fill="clear" size="small" color="danger" (click)="deactivate(condition)" title="Inativar" aria-label="Inativar condição">
                              <ion-icon slot="icon-only" name="ban-outline"></ion-icon>
                            </ion-button>
                          </div>
                        </td>
                      </tr>
                    } @empty {
                      <tr>
                        <td colspan="7">
                          <div class="empty-state">
                            <span><strong>Nenhuma condição encontrada</strong>Cadastre uma condição para usar no PDV.</span>
                          </div>
                        </td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            </ion-card-content>
          </ion-card>

          <ion-card class="data-card">
            <ion-card-header>
              <ion-card-title>{{ selected()?.idCondicaoPagamento ? 'Editar condição' : 'Nova condição' }}</ion-card-title>
              <ion-card-subtitle>Detalhes e regras de parcelamento.</ion-card-subtitle>
            </ion-card-header>
            <ion-card-content class="stack">
              <ion-item>
                <ion-input label="Nome" label-placement="stacked" [(ngModel)]="form.nome"></ion-input>
              </ion-item>
              <ion-item>
                <ion-select label="Tipo de pagamento" label-placement="stacked" [(ngModel)]="form.tipoPagamento">
                  <ion-select-option value="DINHEIRO">Dinheiro</ion-select-option>
                  <ion-select-option value="PIX">PIX</ion-select-option>
                  <ion-select-option value="CARTAO">Cartão</ion-select-option>
                  <ion-select-option value="BOLETO">Boleto</ion-select-option>
                  <ion-select-option value="CHEQUE">Cheque</ion-select-option>
                  <ion-select-option value="TRANSFERENCIA">Transferência</ion-select-option>
                </ion-select>
              </ion-item>
              <div class="form-row">
                <ion-item>
                  <ion-input type="number" label="Parcelas" label-placement="stacked" [(ngModel)]="form.quantidadeParcelas"></ion-input>
                </ion-item>
                <ion-item>
                  <ion-input type="number" label="Prazo em dias" label-placement="stacked" [(ngModel)]="form.prazoDias"></ion-input>
                </ion-item>
                <ion-item>
                  <ion-input type="number" label="Percentual" label-placement="stacked" [(ngModel)]="form.percentual"></ion-input>
                </ion-item>
              </div>
              <ion-item>
                <ion-input label="Restrições" label-placement="stacked" [(ngModel)]="form.restricoes"></ion-input>
              </ion-item>
              <div class="form-actions">
                <ion-button (click)="save()" [disabled]="loading()">Salvar</ion-button>
                <ion-button fill="outline" (click)="startNew()">Cancelar</ion-button>
              </div>
            </ion-card-content>
          </ion-card>
        </section>
      </main>
    </ion-content>
  `,
  styles: [`
    .payment-page {
      max-width: 1500px;
    }

    .payment-grid {
      align-items: start;
      display: grid;
      gap: 14px;
      grid-template-columns: minmax(0, 1.45fr) minmax(360px, 0.55fr);
    }

    tr.selected td {
      background: var(--apex-primary-faint);
    }

    .form-row {
      display: grid;
      gap: 8px;
      grid-template-columns: repeat(3, minmax(0, 1fr));
    }

    .form-actions {
      display: flex;
      gap: 8px;
    }

    @media (max-width: 1080px) {
      .payment-grid,
      .form-row {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class PaymentConditionsPage {
  private readonly api = inject(ApexApiService);
  readonly rows = signal<CondicaoPagamento[]>([]);
  readonly selected = signal<CondicaoPagamento | null>(null);
  readonly loading = signal(false);
  readonly offline = signal(false);
  readonly message = signal('');
  readonly searchTerm = signal('');
  form: CondicaoPagamento = this.emptyForm();

  readonly filtered = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    return this.rows().filter((item) => !term || [item.nome, item.tipoPagamento, item.restricoes]
      .some((value) => String(value ?? '').toLowerCase().includes(term)));
  });

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.api.paymentConditions().pipe(
      catchError(() => {
        this.offline.set(true);
        this.message.set('API indisponível ou permissão insuficiente para condições de pagamento.');
        return of([]);
      }),
      finalize(() => this.loading.set(false))
    ).subscribe((rows) => {
      this.rows.set(rows);
      if (!this.offline()) {
        this.message.set('Condições carregadas.');
      }
    });
  }

  startNew(): void {
    this.selected.set(null);
    this.form = this.emptyForm();
  }

  edit(condition: CondicaoPagamento): void {
    this.selected.set(condition);
    this.form = { ...condition };
  }

  save(): void {
    if (!this.form.nome || !this.form.tipoPagamento) {
      this.message.set('Nome e tipo de pagamento são obrigatórios.');
      return;
    }
    this.loading.set(true);
    const action = this.selected()?.idCondicaoPagamento
      ? this.api.update<CondicaoPagamento>('/api/condicoes-pagamento', this.selected()!.idCondicaoPagamento!, this.form)
      : this.api.create<CondicaoPagamento>('/api/condicoes-pagamento', this.form);
    action.pipe(
      catchError((error) => {
        this.message.set(error?.error?.detail || 'Não foi possível salvar a condição.');
        this.offline.set(true);
        return of(null);
      }),
      finalize(() => this.loading.set(false))
    ).subscribe((saved) => {
      if (!saved) {
        return;
      }
      this.rows.update((rows) => {
        const id = saved.idCondicaoPagamento;
        return rows.some((row) => row.idCondicaoPagamento === id)
          ? rows.map((row) => row.idCondicaoPagamento === id ? saved : row)
          : [saved, ...rows];
      });
      this.message.set('Condição salva com auditoria.');
      this.startNew();
    });
  }

  deactivate(condition: CondicaoPagamento): void {
    if (!condition.idCondicaoPagamento || !window.confirm(`Inativar "${condition.nome}"?`)) {
      return;
    }
    this.api.patch<CondicaoPagamento>(`/api/condicoes-pagamento/${condition.idCondicaoPagamento}/inativar`)
      .pipe(catchError(() => of({ ...condition, status: 0 })))
      .subscribe((updated) => {
        this.rows.update((rows) => rows.map((row) => row.idCondicaoPagamento === updated.idCondicaoPagamento ? updated : row));
        this.message.set('Condição inativada.');
      });
  }

  private emptyForm(): CondicaoPagamento {
    return {
      nome: '',
      tipoPagamento: 'DINHEIRO',
      quantidadeParcelas: 1,
      prazoDias: 0,
      percentual: 100,
      restricoes: '',
      status: 1
    };
  }
}
