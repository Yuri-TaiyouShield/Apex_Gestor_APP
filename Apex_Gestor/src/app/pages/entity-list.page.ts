import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { catchError, of } from 'rxjs';

import { ApexApiService } from '../core/apex-api.service';
import { ENTITY_CONFIGS } from '../core/entity-config';
import { MOCK_CLIENTS, MOCK_EXPENSES, MOCK_EXPENSE_TYPES, MOCK_PRODUCTS, MOCK_USERS } from '../core/mock-data';
import { EntityConfig } from '../core/models';
import { currency, date, getNestedValue } from '../core/view-utils';

type Row = Record<string, unknown>;

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule],
  template: `
    <ion-content>
      <main class="page">
        <header class="page-header">
          <div>
            <h1>{{ config().title }}</h1>
            <p>{{ config().subtitle }}</p>
          </div>
          <ion-button (click)="startNew()">
            <ion-icon slot="start" name="add"></ion-icon>
            Novo
          </ion-button>
        </header>

        <ion-card class="toolbar-card">
          <ion-card-content>
            <ion-searchbar [ngModel]="search()" (ngModelChange)="search.set($event)" placeholder="Buscar registros..." debounce="150"></ion-searchbar>
          </ion-card-content>
        </ion-card>

        <section class="grid-two entity-grid">
          <ion-card class="data-card">
            <ion-card-content>
              <div class="table-wrap">
                <table>
                  <thead>
                    <tr>
                      @for (column of config().columns; track column.key) {
                        <th>{{ column.label }}</th>
                      }
                      <th>Ações</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (row of filteredRows(); track row[config().idKey] || $index) {
                      <tr>
                        @for (column of config().columns; track column.key) {
                          <td>
                            @if (column.format === 'status') {
                              <span class="status-pill" [class.active]="isActive(row, column.key)" [class.inactive]="!isActive(row, column.key)">
                                {{ isActive(row, column.key) ? 'Ativo' : 'Inativo' }}
                              </span>
                            } @else {
                              {{ display(row, column.key, column.format) }}
                            }
                          </td>
                        }
                        <td>
                          <ion-button size="small" fill="clear" (click)="edit(row)">Editar</ion-button>
                          <ion-button size="small" fill="clear" color="danger" (click)="deactivate(row)" [disabled]="!config().deactivatePath">Desativar</ion-button>
                        </td>
                      </tr>
                    } @empty {
                      <tr>
                        <td [attr.colspan]="config().columns.length + 1">
                          <div class="empty-state">Nenhum registro encontrado.</div>
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
              <ion-card-title>{{ editingId() ? 'Editar registro' : 'Novo registro' }}</ion-card-title>
              <ion-card-subtitle>{{ formMessage() || 'Preencha os campos principais.' }}</ion-card-subtitle>
            </ion-card-header>
            <ion-card-content class="stack">
              @for (field of config().fields; track field.key) {
                <ion-item>
                  <ion-input
                    [type]="field.type"
                    [label]="field.label"
                    label-placement="stacked"
                    [(ngModel)]="formModel[field.key]"
                    [required]="field.required || false"
                  ></ion-input>
                </ion-item>
              }
              <ion-button expand="block" (click)="save()">
                {{ editingId() ? 'Salvar alterações' : 'Criar registro' }}
              </ion-button>
              <ion-button expand="block" fill="outline" (click)="startNew()">Limpar</ion-button>
            </ion-card-content>
          </ion-card>
        </section>
      </main>
    </ion-content>
  `,
  styles: [`
    .toolbar-card {
      margin-bottom: 16px;
    }

    .toolbar-card ion-searchbar {
      padding: 0;
    }

    .entity-grid {
      align-items: start;
      grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.6fr);
    }
  `]
})
export class EntityListPage {
  private readonly api = inject(ApexApiService);
  private readonly route = inject(ActivatedRoute);
  readonly search = signal('');
  readonly rows = signal<Row[]>([]);
  readonly editingId = signal<number | string | null>(null);
  readonly formMessage = signal('');
  formModel: Row = {};

  readonly config = computed<EntityConfig>(() => {
    const key = String(this.route.snapshot.data['entity'] ?? 'products');
    return ENTITY_CONFIGS[key] ?? ENTITY_CONFIGS['products'];
  });

  readonly filteredRows = computed(() => {
    const term = this.search().trim().toLowerCase();
    if (!term) {
      return this.rows();
    }
    return this.rows().filter((row) =>
      this.config().searchKeys.some((key) => String(getNestedValue(row, key) ?? '').toLowerCase().includes(term))
    );
  });

  constructor() {
    this.load();
  }

  load(): void {
    const config = this.config();
    this.api.list<Row>(config.endpoint).pipe(catchError(() => of(this.mockRows(config.key)))).subscribe((rows) => this.rows.set(rows));
  }

  startNew(): void {
    this.formModel = {};
    this.editingId.set(null);
    this.formMessage.set('');
  }

  edit(row: Row): void {
    this.editingId.set(row[this.config().idKey] as number | string);
    this.formModel = { ...row };
    this.formMessage.set('Edição carregada.');
  }

  save(): void {
    const config = this.config();
    const payload = this.normalizePayload(config, this.formModel);
    const request = this.editingId()
      ? this.api.update<Row>(config.endpoint, this.editingId()!, payload)
      : this.api.create<Row>(config.endpoint, payload);

    request.pipe(catchError(() => of({ ...payload, [config.idKey]: this.editingId() ?? Date.now() }))).subscribe((saved) => {
      const id = saved[config.idKey] ?? this.editingId();
      if (this.editingId()) {
        this.rows.update((rows) => rows.map((row) => (row[config.idKey] === this.editingId() ? { ...row, ...saved } : row)));
      } else {
        this.rows.update((rows) => [{ ...saved, [config.idKey]: id }, ...rows]);
      }
      this.formMessage.set('Registro salvo.');
      this.startNew();
    });
  }

  deactivate(row: Row): void {
    const config = this.config();
    const id = row[config.idKey] as number | string | undefined;
    if (!id || !config.deactivatePath) {
      return;
    }
    this.api.patch<Row>(config.deactivatePath(id)).pipe(catchError(() => of({ ...row, status: 0 }))).subscribe((updated) => {
      this.rows.update((rows) => rows.map((item) => (item[config.idKey] === id ? { ...item, ...updated, status: updated['status'] ?? 0 } : item)));
    });
  }

  display(row: Row, key: string, format?: string): string {
    const value = getNestedValue(row, key);
    if (format === 'currency') {
      return currency(value);
    }
    if (format === 'date') {
      return date(value);
    }
    if (format === 'status') {
      return this.isActive(row, key) ? 'Ativo' : 'Inativo';
    }
    return String(value ?? '-');
  }

  isActive(row: Row, key: string): boolean {
    return Number(getNestedValue(row, key) ?? 1) === 1;
  }

  private normalizePayload(config: EntityConfig, model: Row): Row {
    const payload: Row = { ...model, status: Number(model['status'] ?? 1) };
    if (config.key === 'products') {
      payload['quantidadeEstoque'] = Number(payload['quantidadeEstoque'] ?? 0);
      payload['estoqueMinimo'] = Number(payload['estoqueMinimo'] ?? 0);
      payload['valorVenda'] = Number(payload['valorVenda'] ?? 0);
      payload['custo'] = Number(payload['custo'] ?? 0);
      payload['categoria'] = payload['categoria'] ?? { idCategoria: 1 };
      payload['fornecedor'] = payload['fornecedor'] ?? { idFornecedor: 1 };
    }
    if (config.key === 'suppliers') {
      payload['endereco'] = payload['endereco'] ?? {
        cep: '00000-000',
        uf: 'SP',
        cidade: 'São Paulo',
        logradouro: 'Endereço não informado'
      };
      payload['dataCadastro'] = payload['dataCadastro'] ?? new Date().toISOString();
    }
    if (config.key === 'users') {
      payload['perfil'] = payload['perfil'] ?? { idPerfil: 1 };
      payload['dataNascimento'] = payload['dataNascimento'] ?? '1990-01-01';
    }
    if (config.key === 'expenses') {
      payload['valor'] = Number(payload['valor'] ?? 0);
      payload['tipoDespesa'] = payload['tipoDespesa'] ?? { idTipoDespesa: 1 };
    }
    return payload;
  }

  private mockRows(key: string): Row[] {
    const suppliers = [
      { idFornecedor: 1, razaoSocial: 'Distribuidora Norte LTDA', nomeFantasia: 'Norte Materiais', cnpj: '12.345.678/0001-90', telefone: '(11) 3333-1010', status: 1 }
    ];
    const map: Record<string, Row[]> = {
      products: MOCK_PRODUCTS as unknown as Row[],
      clients: MOCK_CLIENTS as unknown as Row[],
      suppliers,
      users: MOCK_USERS as unknown as Row[],
      expenses: MOCK_EXPENSES as unknown as Row[],
      expenseTypes: MOCK_EXPENSE_TYPES as unknown as Row[]
    };
    return map[key] ?? [];
  }
}
