import { CommonModule } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { Subscription, catchError, finalize, of } from 'rxjs';

import { ApexApiService } from '../core/apex-api.service';
import { ENTITY_CONFIGS } from '../core/entity-config';
import { MOCK_CLIENTS, MOCK_EXPENSES, MOCK_EXPENSE_TYPES, MOCK_PAYMENT_METHODS, MOCK_PRODUCTS, MOCK_USERS } from '../core/mock-data';
import { EntityConfig } from '../core/models';
import { SessionService } from '../core/session.service';
import { currency, date, getNestedValue } from '../core/view-utils';

type Row = Record<string, unknown>;
type StatusFilter = 'all' | 'active' | 'inactive';
type SaveState = 'idle' | 'loading' | 'success' | 'error';

interface OperationalMetric {
  label: string;
  value: string;
  hint: string;
  icon: string;
  tone: 'primary' | 'success' | 'warning' | 'danger' | 'indigo';
}

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule],
  template: `
    <ion-content>
      <main class="page entity-page">
        <header class="page-header">
          <div>
            <span class="page-kicker">Operação v1</span>
            <h1>{{ config().title }}</h1>
            <p>{{ config().subtitle }}</p>
          </div>
          <div class="page-actions">
            <ion-button fill="outline" (click)="load()">
              <ion-icon slot="start" name="sync-outline"></ion-icon>
              Atualizar
            </ion-button>
            <ion-button (click)="startNew()" [disabled]="!canManageEntity()">
              <ion-icon slot="start" name="add"></ion-icon>
              Novo
            </ion-button>
          </div>
        </header>

        <section class="metric-grid entity-metrics">
          @for (metric of metrics(); track metric.label) {
            <article class="metric-card" [class.success]="metric.tone === 'success'" [class.warning]="metric.tone === 'warning'" [class.danger]="metric.tone === 'danger'" [class.indigo]="metric.tone === 'indigo'">
              <header>
                <span>{{ metric.label }}</span>
                <ion-icon [name]="metric.icon" aria-hidden="true"></ion-icon>
              </header>
              <strong>{{ metric.value }}</strong>
              <small>{{ metric.hint }}</small>
            </article>
          }
        </section>

        <ion-card class="toolbar-card">
          <ion-card-content>
            <div class="toolbar-row">
              <ion-searchbar [ngModel]="search()" (ngModelChange)="setSearch($event)" placeholder="Buscar por nome, documento, código ou descrição" debounce="150"></ion-searchbar>
              <ion-segment [value]="statusFilter()" (ionChange)="setStatusFilter($event.detail.value)">
                <ion-segment-button value="all">
                  <ion-label>Todos</ion-label>
                </ion-segment-button>
                <ion-segment-button value="active">
                  <ion-label>Ativos</ion-label>
                </ion-segment-button>
                <ion-segment-button value="inactive">
                  <ion-label>Inativos</ion-label>
                </ion-segment-button>
              </ion-segment>
              <div class="toolbar-stats" aria-label="Resumo dos registros">
                <span>
                  <strong>{{ filteredRows().length }}</strong>
                  <small>filtrados</small>
                </span>
                <span>
                  <strong>{{ activeRows() }}</strong>
                  <small>ativos</small>
                </span>
              </div>
            </div>
            @if (loading()) {
              <div class="state-line">
                <ion-icon name="sync-outline" aria-hidden="true"></ion-icon>
                Sincronizando com a API...
              </div>
            } @else if (offline()) {
              <div class="state-line warning">
                <ion-icon name="cloud-offline-outline" aria-hidden="true"></ion-icon>
                API indisponível. Operando com dados locais de demonstração para evitar tela vazia.
              </div>
            } @else {
              <div class="state-line success">
                <ion-icon name="checkmark-circle-outline" aria-hidden="true"></ion-icon>
                Dados carregados e prontos para operação.
              </div>
            }
          </ion-card-content>
        </ion-card>

        <section class="grid-two entity-grid">
          <ion-card class="data-card table-card">
            <ion-card-header>
              <ion-card-title>{{ config().title }}</ion-card-title>
              <ion-card-subtitle>Página {{ safePage() }} de {{ totalPages() }} com {{ filteredRows().length }} registros no filtro atual.</ion-card-subtitle>
            </ion-card-header>
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
                    @for (row of pagedRows(); track row[config().idKey] || $index) {
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
                          <div class="table-actions">
                            <ion-button size="small" fill="clear" (click)="edit(row)" [disabled]="!canManageEntity()" aria-label="Editar registro" title="Editar">
                              <ion-icon slot="icon-only" name="create-outline"></ion-icon>
                            </ion-button>
                            <ion-button size="small" fill="clear" color="danger" (click)="deactivate(row)" [disabled]="!config().deactivatePath || !canManageEntity()" aria-label="Desativar registro" title="Desativar">
                              <ion-icon slot="icon-only" name="ban-outline"></ion-icon>
                            </ion-button>
                          </div>
                        </td>
                      </tr>
                    } @empty {
                      <tr>
                        <td [attr.colspan]="config().columns.length + 1">
                          <div class="empty-state">
                            <span>
                              <strong>Nenhum registro encontrado</strong>
                              Ajuste a busca, troque o filtro ou cadastre um novo item.
                            </span>
                          </div>
                        </td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>

              <footer class="table-footer">
                <ion-button size="small" fill="outline" (click)="previousPage()" [disabled]="safePage() <= 1">
                  <ion-icon slot="icon-only" name="chevron-back-outline"></ion-icon>
                </ion-button>
                <span>{{ pageStart() }}-{{ pageEnd() }} de {{ filteredRows().length }}</span>
                <ion-button size="small" fill="outline" (click)="nextPage()" [disabled]="safePage() >= totalPages()">
                  <ion-icon slot="icon-only" name="chevron-forward-outline"></ion-icon>
                </ion-button>
              </footer>
            </ion-card-content>
          </ion-card>

          <aside class="side-stack">
            <ion-card class="data-card">
              <ion-card-header>
                <ion-card-title>{{ editingId() ? 'Editar registro' : 'Novo registro' }}</ion-card-title>
                <ion-card-subtitle>{{ formMessage() || 'Campos essenciais para uso operacional.' }}</ion-card-subtitle>
              </ion-card-header>
              <ion-card-content class="stack">
                @if (formTabs().length > 1) {
                  <ion-segment class="form-tabs" [value]="currentFormTab()" (ionChange)="setFormTab($event.detail.value)">
                    @for (tab of formTabs(); track tab) {
                      <ion-segment-button [value]="tab">
                        <ion-label>{{ tab }}</ion-label>
                      </ion-segment-button>
                    }
                  </ion-segment>
                }
                @for (field of visibleFields(); track field.key) {
                  <ion-item class="form-field" [class.invalid]="saveState() === 'error' && isRequired(field.key, field.required || false) && isEmpty(fieldValue(field.key))">
                    <ion-input
                      [type]="field.type"
                      [label]="field.label"
                      label-placement="stacked"
                      [ngModel]="fieldValue(field.key)"
                      (ngModelChange)="setFieldValue(field.key, $event)"
                      [required]="isRequired(field.key, field.required || false)"
                      [placeholder]="field.placeholder || ''"
                    ></ion-input>
                  </ion-item>
                }
                <div class="form-actions">
                  <ion-button expand="block" (click)="save()" [disabled]="saveState() === 'loading' || !canManageEntity()">
                    {{ editingId() ? 'Salvar alterações' : 'Criar registro' }}
                  </ion-button>
                  <ion-button expand="block" fill="outline" (click)="startNew()">Limpar</ion-button>
                </div>
                @if (!canManageEntity()) {
                  <ion-note color="warning">Seu perfil pode consultar esta tela, mas não pode criar, editar ou desativar registros.</ion-note>
                }
              </ion-card-content>
            </ion-card>

            <ion-card class="data-card workflow-card">
              <ion-card-header>
                <ion-card-title>Fluxo do módulo</ion-card-title>
                <ion-card-subtitle>Como esta tela deve evoluir sem virar submenu solto.</ion-card-subtitle>
              </ion-card-header>
              <ion-card-content>
                <div class="workflow-list">
                  @for (item of workflow(); track item) {
                    <span>
                      <ion-icon name="checkmark-circle-outline" aria-hidden="true"></ion-icon>
                      {{ item }}
                    </span>
                  }
                </div>
              </ion-card-content>
            </ion-card>
          </aside>
        </section>
      </main>
    </ion-content>
  `,
  styles: [`
    .entity-page {
      max-width: 1600px;
    }

    .page-actions,
    .form-actions {
      display: flex;
      gap: 8px;
    }

    .entity-metrics {
      margin-bottom: 10px;
    }

    .metric-card.success ion-icon {
      background: rgba(22, 115, 66, 0.1);
      color: var(--apex-success);
    }

    .metric-card.warning ion-icon {
      background: var(--apex-accent-soft);
      color: var(--apex-warning);
    }

    .metric-card.danger ion-icon {
      background: var(--apex-coral-soft);
      color: var(--apex-danger);
    }

    .metric-card.indigo ion-icon {
      background: var(--apex-indigo-soft);
      color: var(--apex-indigo);
    }

    .toolbar-card {
      margin-bottom: 10px;
    }

    .toolbar-row {
      align-items: center;
      display: grid;
      gap: 10px;
      grid-template-columns: minmax(320px, 1fr) minmax(250px, 320px) auto;
    }

    .toolbar-row ion-segment {
      background: var(--apex-card-muted);
      border: 1px solid var(--apex-border);
      border-radius: 8px;
      min-height: 42px;
      padding: 3px;
    }

    .toolbar-stats {
      display: flex;
      gap: 8px;
    }

    .toolbar-stats span {
      background: var(--apex-card-muted);
      border: 1px solid var(--apex-border);
      border-radius: 8px;
      display: grid;
      min-width: 78px;
      padding: 7px 9px;
    }

    .toolbar-stats strong {
      color: var(--apex-ink);
      font-size: 1rem;
      line-height: 1;
    }

    .toolbar-stats small {
      color: var(--apex-muted);
      font-size: 0.72rem;
      margin-top: 3px;
    }

    .state-line {
      align-items: center;
      border-top: 1px solid var(--apex-border);
      color: var(--apex-muted);
      display: flex;
      font-size: 0.84rem;
      gap: 8px;
      margin-top: 10px;
      padding-top: 9px;
    }

    .state-line.success {
      color: var(--apex-success);
    }

    .state-line.warning {
      color: var(--apex-warning);
    }

    .entity-grid {
      align-items: start;
      grid-template-columns: minmax(0, 1.6fr) minmax(310px, 0.5fr);
    }

    .table-card ion-card-content {
      padding-top: 0;
    }

    .form-field {
      --background: var(--apex-card-muted);
      --border-color: var(--apex-border);
      --border-radius: 8px;
      --inner-padding-end: 12px;
      border: 1px solid var(--apex-border);
      border-radius: 8px;
    }

    .form-field.invalid {
      border-color: var(--apex-danger);
    }

    .form-tabs {
      background: var(--apex-card-muted);
      border: 1px solid var(--apex-border);
      border-radius: 8px;
      min-height: 38px;
      overflow: auto;
      padding: 3px;
    }

    .form-tabs ion-segment-button {
      min-height: 32px;
      min-width: max-content;
      --indicator-color: var(--apex-primary);
      --color-checked: var(--apex-primary-strong);
    }

    .side-stack {
      display: grid;
      gap: 10px;
      max-height: calc(100vh - 174px);
      overflow: auto;
      padding-right: 2px;
    }

    .workflow-list {
      display: grid;
      gap: 8px;
    }

    .workflow-list span {
      align-items: center;
      background: var(--apex-card-muted);
      border: 1px solid var(--apex-border);
      border-radius: 8px;
      color: var(--apex-ink);
      display: flex;
      font-size: 0.86rem;
      gap: 9px;
      min-height: 38px;
      padding: 9px 10px;
    }

    .workflow-list ion-icon {
      color: var(--apex-success);
      flex: 0 0 auto;
    }

    .table-footer {
      align-items: center;
      border-top: 1px solid var(--apex-border);
      color: var(--apex-muted);
      display: flex;
      gap: 10px;
      justify-content: flex-end;
      padding-top: 10px;
    }

    .empty-state span,
    .empty-state strong {
      display: block;
    }

    .empty-state strong {
      color: var(--apex-ink);
      margin-bottom: 4px;
    }

    @media (max-width: 1120px) {
      .toolbar-row {
        grid-template-columns: 1fr;
      }

      .toolbar-stats {
        flex-wrap: wrap;
      }
    }

    @media (max-width: 720px) {
      .page-actions,
      .form-actions {
        flex-direction: column;
        width: 100%;
      }
    }
  `]
})
export class EntityListPage {
  private readonly api = inject(ApexApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly session = inject(SessionService);
  private readonly destroyRef = inject(DestroyRef);
  private loadSubscription?: Subscription;
  private activeLoadKey = '';
  readonly entityKey = signal('products');
  readonly search = signal('');
  readonly rows = signal<Row[]>([]);
  readonly editingId = signal<number | string | null>(null);
  readonly formMessage = signal('');
  readonly statusFilter = signal<StatusFilter>('all');
  readonly page = signal(1);
  readonly pageSize = signal(8);
  readonly loading = signal(false);
  readonly offline = signal(false);
  readonly saveState = signal<SaveState>('idle');
  readonly activeFormTab = signal('');
  formModel: Row = {};

  readonly config = computed<EntityConfig>(() => {
    const key = this.entityKey();
    return ENTITY_CONFIGS[key] ?? ENTITY_CONFIGS['products'];
  });

  readonly filteredRows = computed(() => {
    const term = this.search().trim().toLowerCase();
    const status = this.statusFilter();
    return this.rows().filter((row) => {
      const matchesSearch = !term || this.config().searchKeys.some((key) => String(getNestedValue(row, key) ?? '').toLowerCase().includes(term));
      const active = this.isActive(row, this.statusKey());
      const matchesStatus = status === 'all' || (status === 'active' && active) || (status === 'inactive' && !active);
      return matchesSearch && matchesStatus;
    });
  });
  readonly activeRows = computed(() => this.rows().filter((row) => this.isActive(row, this.statusKey())).length);
  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.filteredRows().length / this.pageSize())));
  readonly safePage = computed(() => Math.min(this.page(), this.totalPages()));
  readonly pageStart = computed(() => this.filteredRows().length === 0 ? 0 : (this.safePage() - 1) * this.pageSize() + 1);
  readonly pageEnd = computed(() => Math.min(this.safePage() * this.pageSize(), this.filteredRows().length));
  readonly pagedRows = computed(() => {
    const start = (this.safePage() - 1) * this.pageSize();
    return this.filteredRows().slice(start, start + this.pageSize());
  });
  readonly metrics = computed(() => this.metricCards(this.config().key, this.rows()));
  readonly workflow = computed(() => this.workflowFor(this.config().key));
  readonly formTabs = computed(() => Array.from(new Set(this.config().fields.map((field) => field.group || 'Dados'))));
  readonly visibleFields = computed(() => {
    const active = this.currentFormTab();
    return this.config().fields.filter((field) => (field.group || 'Dados') === active);
  });

  constructor() {
    this.route.data.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((data) => {
      const nextKey = String(data['entity'] ?? 'products');
      if (nextKey !== this.entityKey()) {
        this.entityKey.set(nextKey);
        this.resetViewState();
      }
      this.load({ refresh: true });
    });
  }

  load(options: { refresh?: boolean } = {}): void {
    const config = this.config();
    const loadKey = `${config.key}:${Date.now()}`;
    this.activeLoadKey = loadKey;
    this.loadSubscription?.unsubscribe();
    this.rows.set(this.mockRows(config.key));
    this.loading.set(true);
    this.offline.set(false);
    this.loadSubscription = this.api.list<Row>(config.endpoint, { refresh: options.refresh }).pipe(
      catchError(() => {
        this.offline.set(true);
        return of(this.mockRows(config.key));
      }),
      finalize(() => {
        if (this.activeLoadKey === loadKey) {
          this.loading.set(false);
        }
      })
    ).subscribe((rows) => {
      if (this.activeLoadKey !== loadKey) {
        return;
      }
      this.rows.set(Array.isArray(rows) ? rows : this.mockRows(config.key));
      this.page.set(1);
    });
  }

  setSearch(value: string): void {
    this.search.set(value);
    this.page.set(1);
  }

  setStatusFilter(value: unknown): void {
    if (value === 'all' || value === 'active' || value === 'inactive') {
      this.statusFilter.set(value);
      this.page.set(1);
    }
  }

  startNew(): void {
    this.formModel = {};
    this.editingId.set(null);
    this.formMessage.set('');
    this.saveState.set('idle');
    this.activeFormTab.set(this.formTabs()[0] ?? 'Dados');
  }

  edit(row: Row): void {
    this.editingId.set(row[this.config().idKey] as number | string);
    this.formModel = this.formModelFromRow(this.config(), row);
    this.formMessage.set('Edição carregada. Revise e salve para auditar a alteração.');
    this.saveState.set('idle');
  }

  currentFormTab(): string {
    const tabs = this.formTabs();
    const active = this.activeFormTab();
    return tabs.includes(active) ? active : tabs[0] ?? 'Dados';
  }

  setFormTab(value: unknown): void {
    if (typeof value === 'string' && this.formTabs().includes(value)) {
      this.activeFormTab.set(value);
    }
  }

  save(): void {
    if (!this.canManageEntity()) {
      this.formMessage.set('Acesso negado: seu perfil nao permite alterar este cadastro.');
      return;
    }
    const config = this.config();
    const missing = config.fields.filter((field) => field.required
      && !(config.key === 'users' && field.key === 'senha' && this.editingId())
      && this.isEmpty(this.fieldValue(field.key)));
    if (missing.length) {
      this.saveState.set('error');
      this.formMessage.set(`Preencha os campos obrigatórios: ${missing.map((field) => field.label).join(', ')}.`);
      return;
    }

    this.saveState.set('loading');
    const payload = this.normalizePayload(config, this.formModel);
    const request = this.editingId()
      ? this.api.update<Row>(config.endpoint, this.editingId()!, payload)
      : this.api.create<Row>(config.endpoint, payload);

    request.pipe(
      catchError(() => {
        this.offline.set(true);
        return of({ ...payload, [config.idKey]: this.editingId() ?? Date.now() });
      }),
      finalize(() => {
        if (this.saveState() === 'loading') {
          this.saveState.set('idle');
        }
      })
    ).subscribe((saved) => {
      const id = saved[config.idKey] ?? this.editingId();
      if (this.editingId()) {
        this.rows.update((rows) => rows.map((row) => (row[config.idKey] === this.editingId() ? { ...row, ...saved } : row)));
      } else {
        this.rows.update((rows) => [{ ...saved, [config.idKey]: id }, ...rows]);
      }
      this.formModel = {};
      this.editingId.set(null);
      this.saveState.set('success');
      this.formMessage.set(this.offline() ? 'Registro salvo localmente porque a API está indisponível.' : 'Registro salvo com sucesso.');
      if (!this.offline()) {
        this.load({ refresh: true });
      }
    });
  }

  deactivate(row: Row): void {
    if (!this.canManageEntity()) {
      this.formMessage.set('Acesso negado: seu perfil nao permite desativar este cadastro.');
      return;
    }
    const config = this.config();
    const id = row[config.idKey] as number | string | undefined;
    if (!id || !config.deactivatePath) {
      return;
    }
    const label = String(row['nomeRazao'] ?? row['razaoSocial'] ?? row['descricao'] ?? row['nome'] ?? id);
    if (!window.confirm(`Desativar "${label}"? A ação deve preservar histórico e auditoria.`)) {
      return;
    }
    this.api.patch<Row>(config.deactivatePath(id)).pipe(catchError(() => {
      this.offline.set(true);
      return of({ ...row, [this.statusKey()]: 0 });
    })).subscribe((updated) => {
      this.rows.update((rows) => rows.map((item) => (item[config.idKey] === id ? { ...item, ...updated, [this.statusKey()]: updated[this.statusKey()] ?? 0 } : item)));
      this.formMessage.set('Registro desativado. Ação crítica deve gerar auditoria no backend.');
      if (!this.offline()) {
        this.load({ refresh: true });
      }
    });
  }

  previousPage(): void {
    this.page.update((value) => Math.max(1, value - 1));
  }

  nextPage(): void {
    this.page.update((value) => Math.min(this.totalPages(), value + 1));
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

  isEmpty(value: unknown): boolean {
    return value === null || value === undefined || String(value).trim() === '';
  }

  isRequired(key: string, required: boolean): boolean {
    return required && !(this.config().key === 'users' && key === 'senha' && Boolean(this.editingId()));
  }

  canManageEntity(): boolean {
    const key = this.config().key;
    if (key === 'products') {
      return this.session.hasAnyRole(['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_ADMIN', 'ROLE_GERENTE']);
    }
    if (key === 'clients' || key === 'suppliers') {
      return this.session.hasAnyRole(['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_ADMIN', 'ROLE_GERENTE']);
    }
    if (key === 'users' || key === 'profiles' || key === 'menus') {
      return this.session.hasAnyRole(['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_ADMIN', 'ROLE_GERENTE']);
    }
    if (key === 'expenses' || key === 'expenseTypes' || key === 'paymentMethods') {
      return this.session.hasAnyRole(['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_FINANCEIRO', 'ROLE_ADMIN', 'ROLE_GERENTE']);
    }
    return !this.session.hasAnyRole(['ROLE_CLIENTE_B2C']);
  }

  fieldValue(key: string): unknown {
    return getNestedValue(this.formModel, key);
  }

  setFieldValue(key: string, value: unknown): void {
    const segments = key.split('.');
    let current: Row = this.formModel;
    segments.forEach((segment, index) => {
      const isLast = index === segments.length - 1;
      if (isLast) {
        current[segment] = value;
        return;
      }
      const nextSegment = segments[index + 1];
      const shouldCreateArray = /^\d+$/.test(nextSegment);
      if (!current[segment] || typeof current[segment] !== 'object') {
        current[segment] = shouldCreateArray ? [] : {};
      }
      current = current[segment] as Row;
    });
  }

  private statusKey(): string {
    return this.config().key === 'menus' ? 'exibir' : 'status';
  }

  private resetViewState(): void {
    this.search.set('');
    this.statusFilter.set('all');
    this.page.set(1);
    this.editingId.set(null);
    this.formModel = {};
    this.formMessage.set('');
    this.saveState.set('idle');
    this.offline.set(false);
    this.activeFormTab.set(this.formTabs()[0] ?? 'Dados');
  }

  private normalizePayload(config: EntityConfig, model: Row): Row {
    const statusKey = this.statusKey();
    const payload: Row = { ...model, [statusKey]: Number(model[statusKey] ?? 1) };
    if (config.key === 'products') {
      payload['quantidadeEstoque'] = Number(payload['quantidadeEstoque'] ?? 0);
      payload['estoqueMinimo'] = Number(payload['estoqueMinimo'] ?? 0);
      payload['valorVenda'] = Number(payload['valorVenda'] ?? 0);
      payload['custo'] = Number(payload['custo'] ?? 0);
      const categoria = payload['categoria'] as Row | undefined;
      const fornecedor = payload['fornecedor'] as Row | undefined;
      if (categoria?.['idCategoria']) {
        categoria['idCategoria'] = Number(categoria['idCategoria']);
      }
      if (fornecedor?.['idFornecedor']) {
        fornecedor['idFornecedor'] = Number(fornecedor['idFornecedor']);
      }
    }
    if (config.key === 'clients') {
      payload['tipoDocumento'] = Number(payload['tipoDocumento'] ?? 1);
      payload['dataCadastro'] = payload['dataCadastro'] ?? new Date().toISOString();
      const endereco = payload['endereco'] as Row | undefined;
      if (endereco && !payload['enderecos']) {
        payload['enderecos'] = [endereco];
      }
      delete payload['endereco'];
    }
    if (config.key === 'suppliers') {
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
    if (config.key === 'paymentMethods') {
      payload['tipoPagamento'] = Number(payload['tipoPagamento'] ?? 1);
    }
    if (config.key === 'menus') {
      payload['exibir'] = Number(payload['exibir'] ?? 1);
    }
    return payload;
  }

  private metricCards(key: string, rows: Row[]): OperationalMetric[] {
    const active = rows.filter((row) => this.isActive(row, this.statusKey())).length;
    const inactive = Math.max(0, rows.length - active);
    if (key === 'products') {
      const critical = rows.filter((row) => Number(row['quantidadeEstoque'] ?? 0) <= Number(row['estoqueMinimo'] ?? 0)).length;
      const stockValue = rows.reduce((sum, row) => sum + Number(row['quantidadeEstoque'] ?? 0) * Number(row['custo'] ?? 0), 0);
      return [
        { label: 'Produtos', value: String(rows.length), hint: 'catálogo operacional', icon: 'cube-outline', tone: 'primary' },
        { label: 'Estoque crítico', value: String(critical), hint: 'abaixo do mínimo', icon: 'warning-outline', tone: critical ? 'warning' : 'success' },
        { label: 'Valor em estoque', value: currency(stockValue), hint: 'custo estimado', icon: 'layers-outline', tone: 'indigo' },
        { label: 'Ativos', value: String(active), hint: `${inactive} inativos`, icon: 'checkmark-circle-outline', tone: 'success' }
      ];
    }
    if (key === 'expenses') {
      const total = rows.reduce((sum, row) => sum + Number(row['valor'] ?? 0), 0);
      return [
        { label: 'Lançamentos', value: String(rows.length), hint: 'contas e despesas', icon: 'receipt-outline', tone: 'primary' },
        { label: 'Valor aberto', value: currency(total), hint: 'fluxo previsto', icon: 'cash-outline', tone: 'warning' },
        { label: 'Ativos', value: String(active), hint: 'pendentes ou válidos', icon: 'checkmark-circle-outline', tone: 'success' },
        { label: 'Cancelados', value: String(inactive), hint: 'revisar auditoria', icon: 'ban-outline', tone: inactive ? 'danger' : 'indigo' }
      ];
    }
    return [
      { label: 'Total', value: String(rows.length), hint: 'registros carregados', icon: 'albums-outline', tone: 'primary' },
      { label: 'Ativos', value: String(active), hint: 'em uso na operação', icon: 'checkmark-circle-outline', tone: 'success' },
      { label: 'Inativos', value: String(inactive), hint: 'soft delete lógico', icon: 'ban-outline', tone: inactive ? 'warning' : 'indigo' },
      { label: 'Filtro atual', value: String(this.filteredRows().length), hint: 'resultado da busca', icon: 'filter-outline', tone: 'indigo' }
    ];
  }

  private workflowFor(key: string): string[] {
    const flows: Record<string, string[]> = {
      products: ['Dados do produto', 'Preço e custo', 'Estoque mínimo', 'Movimentações e auditoria', 'Transferência por filial'],
      clients: ['Dados gerais', 'Contatos', 'Endereços dentro do cliente', 'Histórico de compras', 'Financeiro e observações'],
      suppliers: ['Dados fiscais', 'Contato comercial', 'Entrada de NF', 'Compras vinculadas', 'Avaliação de reposição'],
      users: ['Dados do funcionário', 'Perfil RBAC', 'Registro de ponto', 'Justificativas', 'Histórico de alterações'],
      expenses: ['Conta a pagar/receber', 'Vencimento', 'Conciliação', 'Sangria/suprimento', 'Fechamento diário'],
      profiles: ['Perfil', 'Permissões', 'Menus vinculados', 'Unidade/filial', 'Auditoria de acesso'],
      menus: ['Rota', 'Ícone', 'Exibição', 'Perfis vinculados', 'Ordem operacional']
    };
    return flows[key] ?? ['Cadastro', 'Validação', 'Ativação', 'Auditoria', 'Relatórios'];
  }

  private mockRows(key: string): Row[] {
    const suppliers = [
      { idFornecedor: 1, razaoSocial: 'Distribuidora Norte LTDA', nomeFantasia: 'Norte Materiais', cnpj: '12.345.678/0001-90', telefone: '(11) 3333-1010', status: 1 },
      { idFornecedor: 2, razaoSocial: 'Acabamentos Paulista SA', nomeFantasia: 'Paulista Acabamentos', cnpj: '98.765.432/0001-10', telefone: '(11) 3333-2020', status: 1 }
    ];
    const categories = [
      { idCategoria: 1, descricao: 'Construção', status: 1 },
      { idCategoria: 2, descricao: 'Ferramentas', status: 1 },
      { idCategoria: 3, descricao: 'Pintura', status: 1 }
    ];
    const productGroups = [
      { idGrupoProduto: 1, nome: 'Materiais básicos', descricao: 'Cimento, areia, tijolos e agregados', status: 1 },
      { idGrupoProduto: 2, nome: 'Acabamento', descricao: 'Tintas, revestimentos e acessórios', status: 1 }
    ];
    const menus = [
      { idMenu: 1, nome: 'Dashboard', link: '/', icone: 'bar-chart-outline', exibir: 1 },
      { idMenu: 2, nome: 'PDV', link: '/pos', icone: 'cart-outline', exibir: 1 },
      { idMenu: 3, nome: 'Caixa', link: '/cashier', icone: 'cash-outline', exibir: 1 },
      { idMenu: 4, nome: 'Financeiro', link: '/expenses', icone: 'receipt-outline', exibir: 1 }
    ];
    const profiles = [
      { idPerfil: 1, nome: 'Administrador', status: 1 },
      { idPerfil: 2, nome: 'Gerente', status: 1 },
      { idPerfil: 3, nome: 'Operador de Caixa', status: 1 }
    ];
    const map: Record<string, Row[]> = {
      products: MOCK_PRODUCTS as unknown as Row[],
      clients: MOCK_CLIENTS as unknown as Row[],
      suppliers,
      users: MOCK_USERS as unknown as Row[],
      expenses: MOCK_EXPENSES as unknown as Row[],
      expenseTypes: MOCK_EXPENSE_TYPES as unknown as Row[],
      paymentMethods: MOCK_PAYMENT_METHODS as unknown as Row[],
      categories,
      productGroups,
      menus,
      profiles
    };
    return map[key] ?? [];
  }

  private formModelFromRow(config: EntityConfig, row: Row): Row {
    const clone = structuredClone(row) as Row;
    if (config.key === 'clients') {
      const enderecos = clone['enderecos'] as Row[] | undefined;
      clone['endereco'] = enderecos?.[0] ?? {};
    }
    return clone;
  }
}
