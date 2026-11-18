import { CommonModule, CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  IonBadge,
  IonButton,
  IonCard,
  IonCardContent,
  IonCardHeader,
  IonCardSubtitle,
  IonCardTitle,
  IonCheckbox,
  IonContent,
  IonIcon,
  IonInput,
  IonItem,
  IonLabel,
  IonSearchbar,
  IonSegment,
  IonSegmentButton,
  IonSelect,
  IonSelectOption
} from '@ionic/angular/standalone';
import { catchError, finalize, forkJoin, of } from 'rxjs';

import { ApexApiService } from '../core/apex-api.service';
import {
  ApexRole,
  Categoria,
  CentroCustoBootstrapResponse,
  CentroCustoRequest,
  CentroCustoResponse,
  ClassificacaoCusto,
  ConsolidacaoCompetenciaResponse,
  FormaPagamento,
  Fornecedor,
  GrupoDreGerencial,
  ItemPendenteClassificacao,
  NaturezaContaGerencial,
  OrigemClassificacaoFinanceira,
  PendenciaClassificacaoResponse,
  PlanoContasRequest,
  PlanoContasResponse,
  RegraClassificacaoRequest,
  RegraClassificacaoResponse,
  ResultadoClassificacaoLote,
  TipoCentroCusto,
  TipoContaGerencial,
  TipoDespesa
} from '../core/models';
import { SessionService } from '../core/session.service';
import { ApexEmptyStateComponent } from '../shared/ui/empty-state/apex-empty-state.component';
import { ApexPageHeaderComponent } from '../shared/ui/page-header/apex-page-header.component';
import { ApexStatusBadgeComponent } from '../shared/ui/status-badge/apex-status-badge.component';

export type FoundationTab = 'plano-contas' | 'centros-custo' | 'classificacao' | 'competencia';
type PlanoViewMode = 'grouped' | 'tree';

interface FlattenedAccountNode {
  account: PlanoContasResponse;
  depth: number;
}

interface PendingDraft {
  planoContaId: number | null;
  centroCustoId: number | null;
  motivo: string;
}

const FOUNDATION_MUTATION_ROLES: readonly ApexRole[] = [
  'ROLE_SYSADMIN',
  'ROLE_DONO_GERENTE',
  'ROLE_DONO',
  'ROLE_FINANCEIRO',
  'ROLE_FINANCAS',
  'ROLE_CONTADOR',
  'ROLE_GERENTE',
  'ROLE_GESTOR',
  'ROLE_ADMIN',
  'ROLE_ADMINISTRACAO'
];

export const TIPO_CONTA_OPTIONS: Array<{ value: TipoContaGerencial; label: string }> = [
  { value: 'RECEITA', label: 'Receita' },
  { value: 'DEDUCAO', label: 'Dedução' },
  { value: 'CMV', label: 'CMV' },
  { value: 'DESPESA', label: 'Despesa' },
  { value: 'FINANCEIRO', label: 'Financeiro' },
  { value: 'OUTRAS', label: 'Outras' },
  { value: 'TRIBUTO_LUCRO', label: 'Tributo sobre Lucro' },
  { value: 'INVESTIMENTO', label: 'Investimento' }
];

export const NATUREZA_OPTIONS: Array<{ value: NaturezaContaGerencial; label: string }> = [
  { value: 'CREDITO', label: 'Crédito' },
  { value: 'DEBITO', label: 'Débito' }
];

export const CLASSIFICACAO_CUSTO_OPTIONS: Array<{ value: ClassificacaoCusto; label: string }> = [
  { value: 'FIXO', label: 'Fixo' },
  { value: 'VARIAVEL', label: 'Variável' },
  { value: 'NAO_APLICAVEL', label: 'Não aplicável' }
];

export const GRUPO_DRE_OPTIONS: Array<{ value: GrupoDreGerencial; label: string }> = [
  { value: 'RECEITA_BRUTA', label: 'Receita Bruta' },
  { value: 'DEVOLUCOES_CANCELAMENTOS', label: 'Devoluções e Cancelamentos' },
  { value: 'DESCONTOS_CONCEDIDOS', label: 'Descontos Concedidos' },
  { value: 'TRIBUTOS_SOBRE_VENDAS', label: 'Tributos sobre Vendas' },
  { value: 'CMV', label: 'CMV' },
  { value: 'DESPESA_COMERCIAL', label: 'Despesa Comercial' },
  { value: 'DESPESA_ADMINISTRATIVA', label: 'Despesa Administrativa' },
  { value: 'DESPESA_OPERACIONAL', label: 'Despesa Operacional' },
  { value: 'DESPESA_PESSOAL', label: 'Despesa de Pessoal' },
  { value: 'DESPESA_INFRAESTRUTURA', label: 'Despesa de Infraestrutura' },
  { value: 'DESPESA_FINANCEIRA', label: 'Despesa Financeira' },
  { value: 'RECEITA_FINANCEIRA', label: 'Receita Financeira' },
  { value: 'OUTRAS_RECEITAS', label: 'Outras Receitas' },
  { value: 'OUTRAS_DESPESAS', label: 'Outras Despesas' },
  { value: 'TRIBUTOS_SOBRE_LUCRO', label: 'Tributos sobre Lucro' },
  { value: 'NAO_APLICAVEL_DRE', label: 'Não Aplicável ao DRE' }
];

export const TIPO_CENTRO_CUSTO_OPTIONS: Array<{ value: TipoCentroCusto; label: string }> = [
  { value: 'LOJA', label: 'Loja' },
  { value: 'ADMINISTRATIVO', label: 'Administrativo' },
  { value: 'COMERCIAL', label: 'Comercial' },
  { value: 'LOGISTICA', label: 'Logística' },
  { value: 'EXPANSAO', label: 'Expansão' },
  { value: 'GERAL', label: 'Geral' }
];

export const ORIGEM_CLASSIFICACAO_OPTIONS: Array<{ value: OrigemClassificacaoFinanceira; label: string }> = [
  { value: 'DESPESA', label: 'Despesa' },
  { value: 'CONTA_PAGAR', label: 'Conta a Pagar' },
  { value: 'CONTA_RECEBER', label: 'Conta a Receber' },
  { value: 'VENDA', label: 'Venda' },
  { value: 'DEVOLUCAO', label: 'Devolução' },
  { value: 'NFE_ENTRADA', label: 'NF-e Entrada' },
  { value: 'CAIXA_MOVIMENTACAO', label: 'Movimentação de Caixa' }
];

const GRUPO_DRE_LABELS: Record<string, string> = Object.fromEntries(
  GRUPO_DRE_OPTIONS.map((item) => [item.value, item.label])
);

const ORIGEM_LABELS: Record<string, string> = Object.fromEntries(
  ORIGEM_CLASSIFICACAO_OPTIONS.map((item) => [item.value, item.label])
);

@Component({
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    CurrencyPipe,
    FormsModule,
    ReactiveFormsModule,
    IonContent,
    IonBadge,
    IonButton,
    IonIcon,
    IonSegment,
    IonSegmentButton,
    IonLabel,
    IonCard,
    IonCardHeader,
    IonCardTitle,
    IonCardSubtitle,
    IonCardContent,
    IonItem,
    IonInput,
    IonSelect,
    IonSelectOption,
    IonSearchbar,
    IonCheckbox,
    ApexPageHeaderComponent,
    ApexStatusBadgeComponent,
    ApexEmptyStateComponent
  ],
  template: `
    <ion-content>
      <main class="page foundation-page">
        <apex-page-header
          kicker="Lote 1 · Fundação Financeira"
          title="Fundação Financeira & Competência"
          subtitle="Plano de contas gerencial, centros de custo, regras de classificação automática e consolidação competência vs caixa."
        >
          @if (isAuditorReadOnly()) {
            <apex-status-badge tone="info">Modo Auditor (somente leitura)</apex-status-badge>
          }
          <ion-button fill="outline" (click)="refreshCurrentTab()" [disabled]="loading()">
            <ion-icon slot="start" name="sync-outline" aria-hidden="true"></ion-icon>
            Atualizar
          </ion-button>
        </apex-page-header>

        <ion-segment [value]="tab()" (ionChange)="setTab($event.detail.value)" aria-label="Seções da Fundação Financeira">
          <ion-segment-button value="plano-contas">
            <ion-label>Plano de Contas</ion-label>
          </ion-segment-button>
          <ion-segment-button value="centros-custo">
            <ion-label>Centros de Custo</ion-label>
          </ion-segment-button>
          <ion-segment-button value="classificacao">
            <ion-label>Classificação &amp; Pendências</ion-label>
          </ion-segment-button>
          <ion-segment-button value="competencia">
            <ion-label>Competência vs Caixa</ion-label>
          </ion-segment-button>
        </ion-segment>

        @if (feedbackMessage()) {
          <div
            class="feedback-banner"
            [class.danger]="feedbackTone() === 'danger'"
            [class.success]="feedbackTone() === 'success'"
            [class.warning]="feedbackTone() === 'warning'"
            role="status"
            aria-live="polite"
          >
            <ion-icon
              [name]="feedbackTone() === 'danger' ? 'warning-outline' : 'checkmark-circle-outline'"
              aria-hidden="true"
            ></ion-icon>
            <span>{{ feedbackMessage() }}</span>
          </div>
        }

        @if (tab() === 'plano-contas') {
          <section class="foundation-toolbar toolbar-card">
            <div class="toolbar-controls">
              <ion-searchbar
                [ngModel]="planoSearch()"
                (ngModelChange)="planoSearch.set($event)"
                placeholder="Filtrar conta por código, nome ou grupo DRE"
                debounce="120"
              ></ion-searchbar>

              <div class="inline-switches">
                <ion-segment [value]="planoViewMode()" (ionChange)="setPlanoViewMode($event.detail.value)">
                  <ion-segment-button value="grouped">
                    <ion-label>Por Grupo DRE</ion-label>
                  </ion-segment-button>
                  <ion-segment-button value="tree">
                    <ion-label>Hierárquica</ion-label>
                  </ion-segment-button>
                </ion-segment>

                <ion-checkbox
                  [checked]="planoSomenteAtivos()"
                  (ionChange)="togglePlanoActiveFilter($event.detail.checked)"
                >
                  Somente ativas
                </ion-checkbox>
              </div>

              @if (canMutate()) {
                <div class="toolbar-actions">
                  <ion-button fill="outline" (click)="bootstrapPlanoContas()" [disabled]="saving()">
                    <ion-icon slot="start" name="layers-outline" aria-hidden="true"></ion-icon>
                    Inicializar Plano Padrão
                  </ion-button>
                  <ion-button (click)="resetPlanoForm()" [disabled]="saving()">
                    <ion-icon slot="start" name="add-outline" aria-hidden="true"></ion-icon>
                    Nova Conta
                  </ion-button>
                </div>
              }
            </div>
          </section>

          <section class="split-layout" [class.full-width]="!canMutate()">
            <ion-card class="data-card">
              <ion-card-header>
                <ion-card-title>Plano de Contas Gerencial ({{ filteredPlanoCount() }})</ion-card-title>
                <ion-card-subtitle>Estrutura gerencial usada no DRE, regras automáticas e consolidação por competência.</ion-card-subtitle>
              </ion-card-header>
              <ion-card-content>
                @if (filteredPlanoCount() === 0) {
                  <apex-empty-state
                    icon="layers-outline"
                    title="Nenhuma conta gerencial encontrada"
                    description="Inicialize o Plano Padrão para criar a estrutura gerencial ou cadastre uma nova conta."
                  >
                    @if (canMutate()) {
                      <ion-button (click)="bootstrapPlanoContas()" [disabled]="saving()">
                        Inicializar Plano Padrão
                      </ion-button>
                    }
                  </apex-empty-state>
                } @else if (planoViewMode() === 'tree') {
                  <div class="table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th>Código &amp; Conta</th>
                          <th>Grupo DRE</th>
                          <th>Tipo / Natureza</th>
                          <th>Custo</th>
                          <th>Lançamento</th>
                          <th>Status</th>
                          @if (canMutate()) {
                            <th>Ações</th>
                          }
                        </tr>
                      </thead>
                      <tbody>
                        @for (node of filteredTreeNodes(); track node.account.idPlanoConta) {
                          <tr>
                            <td>
                              <div class="tree-cell" [style.paddingLeft.px]="node.depth * 18">
                                @if (node.depth > 0) {
                                  <span class="tree-branch" aria-hidden="true">└</span>
                                }
                                <div>
                                  <strong>{{ node.account.codigo }} · {{ node.account.nome }}</strong>
                                  @if (node.account.descricao) {
                                    <small>{{ node.account.descricao }}</small>
                                  }
                                </div>
                              </div>
                            </td>
                            <td>{{ grupoDreLabel(node.account.grupoDre) }}</td>
                            <td>
                              <span>{{ node.account.tipoConta }}</span>
                              <small>{{ node.account.natureza }}</small>
                            </td>
                            <td>{{ node.account.classificacaoCusto }}</td>
                            <td>
                              <apex-status-badge [tone]="node.account.aceitaLancamento ? 'primary' : 'neutral'">
                                {{ node.account.aceitaLancamento ? 'Analítica' : 'Sintética' }}
                              </apex-status-badge>
                            </td>
                            <td>
                              <span class="status-pill" [class.active]="node.account.ativo" [class.inactive]="!node.account.ativo">
                                {{ node.account.ativo ? 'Ativa' : 'Inativa' }}
                              </span>
                            </td>
                            @if (canMutate()) {
                              <td>
                                <div class="table-actions">
                                  <ion-button
                                    size="small"
                                    fill="clear"
                                    (click)="editPlanoConta(node.account)"
                                    aria-label="Editar conta gerencial"
                                    title="Editar"
                                  >
                                    <ion-icon slot="icon-only" name="create-outline"></ion-icon>
                                  </ion-button>
                                  <ion-button
                                    size="small"
                                    fill="clear"
                                    [color]="node.account.ativo ? 'danger' : 'success'"
                                    (click)="togglePlanoStatus(node.account)"
                                    [attr.aria-label]="node.account.ativo ? 'Inativar conta gerencial' : 'Ativar conta gerencial'"
                                    [title]="node.account.ativo ? 'Inativar' : 'Ativar'"
                                  >
                                    <ion-icon
                                      slot="icon-only"
                                      [name]="node.account.ativo ? 'ban-outline' : 'checkmark-circle-outline'"
                                    ></ion-icon>
                                  </ion-button>
                                </div>
                              </td>
                            }
                          </tr>
                        }
                      </tbody>
                    </table>
                  </div>
                } @else {
                  <div class="group-stack">
                    @for (group of groupedPlanoContas(); track group.grupo) {
                      <section class="dre-group">
                        <header class="dre-group-head">
                          <h3>{{ group.label }}</h3>
                          <ion-badge color="medium">{{ group.accounts.length }}</ion-badge>
                        </header>
                        <div class="table-wrap">
                          <table>
                            <thead>
                              <tr>
                                <th>Código</th>
                                <th>Conta</th>
                                <th>Tipo / Natureza</th>
                                <th>Custo</th>
                                <th>Lançamento</th>
                                <th>Status</th>
                                @if (canMutate()) {
                                  <th>Ações</th>
                                }
                              </tr>
                            </thead>
                            <tbody>
                              @for (conta of group.accounts; track conta.idPlanoConta) {
                                <tr>
                                  <td><strong>{{ conta.codigo }}</strong></td>
                                  <td>
                                    <strong>{{ conta.nome }}</strong>
                                    @if (conta.descricao) {
                                      <small>{{ conta.descricao }}</small>
                                    }
                                    @if (conta.sistemaPadrao) {
                                      <small class="padrao-tag">Padrão do sistema</small>
                                    }
                                  </td>
                                  <td>
                                    <span>{{ conta.tipoConta }}</span>
                                    <small>{{ conta.natureza }}</small>
                                  </td>
                                  <td>{{ conta.classificacaoCusto }}</td>
                                  <td>
                                    <apex-status-badge [tone]="conta.aceitaLancamento ? 'primary' : 'neutral'">
                                      {{ conta.aceitaLancamento ? 'Analítica' : 'Sintética' }}
                                    </apex-status-badge>
                                  </td>
                                  <td>
                                    <span class="status-pill" [class.active]="conta.ativo" [class.inactive]="!conta.ativo">
                                      {{ conta.ativo ? 'Ativa' : 'Inativa' }}
                                    </span>
                                  </td>
                                  @if (canMutate()) {
                                    <td>
                                      <div class="table-actions">
                                        <ion-button
                                          size="small"
                                          fill="clear"
                                          (click)="editPlanoConta(conta)"
                                          aria-label="Editar conta gerencial"
                                          title="Editar"
                                        >
                                          <ion-icon slot="icon-only" name="create-outline"></ion-icon>
                                        </ion-button>
                                        <ion-button
                                          size="small"
                                          fill="clear"
                                          [color]="conta.ativo ? 'danger' : 'success'"
                                          (click)="togglePlanoStatus(conta)"
                                          [attr.aria-label]="conta.ativo ? 'Inativar conta gerencial' : 'Ativar conta gerencial'"
                                          [title]="conta.ativo ? 'Inativar' : 'Ativar'"
                                        >
                                          <ion-icon
                                            slot="icon-only"
                                            [name]="conta.ativo ? 'ban-outline' : 'checkmark-circle-outline'"
                                          ></ion-icon>
                                        </ion-button>
                                      </div>
                                    </td>
                                  }
                                </tr>
                              }
                            </tbody>
                          </table>
                        </div>
                      </section>
                    }
                  </div>
                }
              </ion-card-content>
            </ion-card>

            @if (canMutate()) {
              <ion-card class="data-card form-card">
                <ion-card-header>
                  <ion-card-title>{{ editingPlanoId() ? 'Editar Conta Gerencial' : 'Nova Conta Gerencial' }}</ion-card-title>
                  <ion-card-subtitle>Vincule o grupo DRE e se a conta aceita lançamentos diretos.</ion-card-subtitle>
                </ion-card-header>
                <ion-card-content>
                  <form class="stack" [formGroup]="planoForm" (ngSubmit)="savePlanoConta()">
                    <ion-input label="Código" labelPlacement="stacked" formControlName="codigo" placeholder="Ex.: 4.1.01"></ion-input>
                    <ion-input label="Nome" labelPlacement="stacked" formControlName="nome" placeholder="Ex.: Aluguel da Loja"></ion-input>
                    <ion-input label="Descrição" labelPlacement="stacked" formControlName="descricao" placeholder="Opcional"></ion-input>

                    <ion-select label="Conta pai" labelPlacement="stacked" formControlName="contaPaiId">
                      <ion-select-option [value]="null">Nenhuma (conta raiz)</ion-select-option>
                      @for (conta of planoContas(); track conta.idPlanoConta) {
                        @if (conta.idPlanoConta !== editingPlanoId()) {
                          <ion-select-option [value]="conta.idPlanoConta">{{ conta.codigo }} - {{ conta.nome }}</ion-select-option>
                        }
                      }
                    </ion-select>

                    <ion-select label="Tipo da conta" labelPlacement="stacked" formControlName="tipoConta">
                      @for (opt of tipoContaOptions; track opt.value) {
                        <ion-select-option [value]="opt.value">{{ opt.label }}</ion-select-option>
                      }
                    </ion-select>

                    <ion-select label="Natureza" labelPlacement="stacked" formControlName="natureza">
                      @for (opt of naturezaOptions; track opt.value) {
                        <ion-select-option [value]="opt.value">{{ opt.label }}</ion-select-option>
                      }
                    </ion-select>

                    <ion-select label="Classificação de custo" labelPlacement="stacked" formControlName="classificacaoCusto">
                      @for (opt of classificacaoCustoOptions; track opt.value) {
                        <ion-select-option [value]="opt.value">{{ opt.label }}</ion-select-option>
                      }
                    </ion-select>

                    <ion-select label="Grupo DRE" labelPlacement="stacked" formControlName="grupoDre">
                      @for (opt of grupoDreOptions; track opt.value) {
                        <ion-select-option [value]="opt.value">{{ opt.label }}</ion-select-option>
                      }
                    </ion-select>

                    <ion-input label="Ordem de exibição" labelPlacement="stacked" type="number" formControlName="ordemExibicao"></ion-input>

                    <ion-item lines="none" class="checkbox-row">
                      <ion-checkbox formControlName="aceitaLancamento">Aceita lançamento direto (analítica)</ion-checkbox>
                    </ion-item>

                    <div class="form-actions">
                      <ion-button type="submit" expand="block" [disabled]="saving()">
                        {{ editingPlanoId() ? 'Salvar Conta' : 'Criar Conta' }}
                      </ion-button>
                      <ion-button type="button" expand="block" fill="outline" (click)="resetPlanoForm()">
                        Limpar
                      </ion-button>
                    </div>
                  </form>
                </ion-card-content>
              </ion-card>
            }
          </section>
        }

        @if (tab() === 'centros-custo') {
          <section class="foundation-toolbar toolbar-card">
            <div class="toolbar-controls">
              <ion-searchbar
                [ngModel]="centroSearch()"
                (ngModelChange)="centroSearch.set($event)"
                placeholder="Buscar centro de custo por código, nome ou tipo"
                debounce="120"
              ></ion-searchbar>

              <div class="inline-switches">
                <ion-checkbox
                  [checked]="centroSomenteAtivos()"
                  (ionChange)="toggleCentroActiveFilter($event.detail.checked)"
                >
                  Somente ativos
                </ion-checkbox>
              </div>

              @if (canMutate()) {
                <div class="toolbar-actions">
                  <ion-button fill="outline" (click)="bootstrapCentrosCusto()" [disabled]="saving()">
                    <ion-icon slot="start" name="business-outline" aria-hidden="true"></ion-icon>
                    Inicializar Padrões
                  </ion-button>
                  <ion-button (click)="resetCentroForm()" [disabled]="saving()">
                    <ion-icon slot="start" name="add-outline" aria-hidden="true"></ion-icon>
                    Novo Centro
                  </ion-button>
                </div>
              }
            </div>
          </section>

          <section class="split-layout" [class.full-width]="!canMutate()">
            <ion-card class="data-card">
              <ion-card-header>
                <ion-card-title>Centros de Custo ({{ filteredCentros().length }})</ion-card-title>
                <ion-card-subtitle>Unidades operacionais para rateio de receitas, despesas e rentabilidade.</ion-card-subtitle>
              </ion-card-header>
              <ion-card-content>
                @if (filteredCentros().length === 0) {
                  <apex-empty-state
                    icon="business-outline"
                    title="Nenhum centro de custo encontrado"
                    description="Inicialize os centros padrão ou crie um novo centro de custo para a unidade."
                  >
                    @if (canMutate()) {
                      <ion-button (click)="bootstrapCentrosCusto()" [disabled]="saving()">
                        Inicializar Padrões
                      </ion-button>
                    }
                  </apex-empty-state>
                } @else {
                  <div class="table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th>Código</th>
                          <th>Nome</th>
                          <th>Tipo Operacional</th>
                          <th>Responsável (ID)</th>
                          <th>Status</th>
                          @if (canMutate()) {
                            <th>Ações</th>
                          }
                        </tr>
                      </thead>
                      <tbody>
                        @for (cc of filteredCentros(); track cc.idCentroCusto) {
                          <tr>
                            <td><strong>{{ cc.codigo }}</strong></td>
                            <td>
                              <strong>{{ cc.nome }}</strong>
                              @if (cc.descricao) {
                                <small>{{ cc.descricao }}</small>
                              }
                            </td>
                            <td>{{ cc.tipoOperacional }}</td>
                            <td>{{ cc.responsavelUsuarioId ?? '-' }}</td>
                            <td>
                              <span class="status-pill" [class.active]="cc.ativo" [class.inactive]="!cc.ativo">
                                {{ cc.ativo ? 'Ativo' : 'Inativo' }}
                              </span>
                            </td>
                            @if (canMutate()) {
                              <td>
                                <div class="table-actions">
                                  <ion-button
                                    size="small"
                                    fill="clear"
                                    (click)="editCentroCusto(cc)"
                                    aria-label="Editar centro de custo"
                                    title="Editar"
                                  >
                                    <ion-icon slot="icon-only" name="create-outline"></ion-icon>
                                  </ion-button>
                                  <ion-button
                                    size="small"
                                    fill="clear"
                                    [color]="cc.ativo ? 'danger' : 'success'"
                                    (click)="toggleCentroStatus(cc)"
                                    [attr.aria-label]="cc.ativo ? 'Inativar centro de custo' : 'Ativar centro de custo'"
                                    [title]="cc.ativo ? 'Inativar' : 'Ativar'"
                                  >
                                    <ion-icon
                                      slot="icon-only"
                                      [name]="cc.ativo ? 'ban-outline' : 'checkmark-circle-outline'"
                                    ></ion-icon>
                                  </ion-button>
                                </div>
                              </td>
                            }
                          </tr>
                        }
                      </tbody>
                    </table>
                  </div>
                }
              </ion-card-content>
            </ion-card>

            @if (canMutate()) {
              <ion-card class="data-card form-card">
                <ion-card-header>
                  <ion-card-title>{{ editingCentroId() ? 'Editar Centro de Custo' : 'Novo Centro de Custo' }}</ion-card-title>
                  <ion-card-subtitle>Defina código único e tipo operacional.</ion-card-subtitle>
                </ion-card-header>
                <ion-card-content>
                  <form class="stack" [formGroup]="centroForm" (ngSubmit)="saveCentroCusto()">
                    <ion-input label="Código" labelPlacement="stacked" formControlName="codigo" placeholder="Ex.: CC-LOJA-01"></ion-input>
                    <ion-input label="Nome" labelPlacement="stacked" formControlName="nome" placeholder="Ex.: Matriz Comercial"></ion-input>
                    <ion-input label="Descrição" labelPlacement="stacked" formControlName="descricao" placeholder="Opcional"></ion-input>

                    <ion-select label="Tipo operacional" labelPlacement="stacked" formControlName="tipoOperacional">
                      @for (opt of tipoCentroCustoOptions; track opt.value) {
                        <ion-select-option [value]="opt.value">{{ opt.label }}</ion-select-option>
                      }
                    </ion-select>

                    <ion-input
                      label="ID do responsável (opcional)"
                      labelPlacement="stacked"
                      type="number"
                      formControlName="responsavelUsuarioId"
                    ></ion-input>

                    <div class="form-actions">
                      <ion-button type="submit" expand="block" [disabled]="saving()">
                        {{ editingCentroId() ? 'Salvar Centro' : 'Criar Centro' }}
                      </ion-button>
                      <ion-button type="button" expand="block" fill="outline" (click)="resetCentroForm()">
                        Limpar
                      </ion-button>
                    </div>
                  </form>
                </ion-card-content>
              </ion-card>
            }
          </section>
        }

        @if (tab() === 'classificacao') {
          <section class="metric-grid classification-metrics" aria-label="Resumo de pendências de classificação">
            <article class="metric-card">
              <header>
                <span>Total Pendentes</span>
                <ion-icon name="warning-outline" aria-hidden="true"></ion-icon>
              </header>
              <strong class="apex-tabular">{{ pendencias()?.totalPendentes ?? 0 }}</strong>
              <small>lançamentos sem conta gerencial</small>
            </article>
            <article class="metric-card">
              <header>
                <span>Despesas</span>
                <ion-icon name="receipt-outline" aria-hidden="true"></ion-icon>
              </header>
              <strong class="apex-tabular">{{ pendencias()?.despesasPendentes ?? 0 }}</strong>
              <small>pendentes de conta</small>
            </article>
            <article class="metric-card">
              <header>
                <span>Contas a Pagar</span>
                <ion-icon name="cash-outline" aria-hidden="true"></ion-icon>
              </header>
              <strong class="apex-tabular">{{ pendencias()?.contasPagarPendentes ?? 0 }}</strong>
              <small>pendentes de conta</small>
            </article>
            <article class="metric-card">
              <header>
                <span>Contas a Receber / Vendas</span>
                <ion-icon name="trending-up-outline" aria-hidden="true"></ion-icon>
              </header>
              <strong class="apex-tabular">
                {{ (pendencias()?.contasReceberPendentes ?? 0) + (pendencias()?.vendasPendentes ?? 0) }}
              </strong>
              <small>
                {{ pendencias()?.contasReceberPendentes ?? 0 }} receber · {{ pendencias()?.vendasPendentes ?? 0 }} vendas
              </small>
            </article>
          </section>

          <section class="foundation-toolbar toolbar-card">
            <div class="toolbar-controls">
              <ion-select
                label="Filtrar por origem"
                labelPlacement="stacked"
                [ngModel]="selectedOrigem()"
                (ngModelChange)="onOrigemChange($event)"
              >
                <ion-select-option value="">Todas as origens</ion-select-option>
                @for (opt of origemOptions; track opt.value) {
                  <ion-select-option [value]="opt.value">{{ opt.label }}</ion-select-option>
                }
              </ion-select>

              @if (canMutate()) {
                <div class="toolbar-actions">
                  <ion-button (click)="aplicarClassificacaoAutomatica()" [disabled]="saving()">
                    <ion-icon slot="start" name="checkmark-circle-outline" aria-hidden="true"></ion-icon>
                    Classificar Automaticamente
                  </ion-button>
                </div>
              }
            </div>

            @if (ultimoLoteResultado()) {
              <div class="batch-summary" role="status">
                <strong>
                  Lote executado: {{ ultimoLoteResultado()?.classificados }} de {{ ultimoLoteResultado()?.processados }} classificados
                  ({{ ultimoLoteResultado()?.naoClassificados }} não classificados).
                </strong>
                @if ((ultimoLoteResultado()?.observacoes?.length ?? 0) > 0) {
                  <ul>
                    @for (obs of ultimoLoteResultado()?.observacoes ?? []; track obs) {
                      <li>{{ obs }}</li>
                    }
                  </ul>
                }
              </div>
            }
          </section>

          <ion-card class="data-card section-gap">
            <ion-card-header>
              <ion-card-title>Itens Pendentes de Classificação ({{ visiblePendingItems().length }})</ion-card-title>
              <ion-card-subtitle>Classifique manualmente os lançamentos pendentes ou aceite a sugestão automática.</ion-card-subtitle>
            </ion-card-header>
            <ion-card-content>
              @if (visiblePendingItems().length === 0) {
                <apex-empty-state
                  icon="checkmark-circle-outline"
                  title="Nenhuma pendência encontrada"
                  description="Todos os lançamentos filtrados já possuem conta gerencial associada."
                ></apex-empty-state>
              } @else {
                <div class="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>Origem / ID</th>
                        <th>Descrição</th>
                        <th>Competência</th>
                        <th>Valor</th>
                        <th>Sugestão</th>
                        @if (canMutate()) {
                          <th>Plano de Contas</th>
                          <th>Centro de Custo</th>
                          <th>Motivo</th>
                          <th>Ação</th>
                        }
                      </tr>
                    </thead>
                    <tbody>
                      @for (item of visiblePendingItems(); track pendingKey(item)) {
                        <tr>
                          <td>
                            <apex-status-badge tone="warning">{{ origemLabel(item.origemTipo) }}</apex-status-badge>
                            <small>#{{ item.registroId }}</small>
                          </td>
                          <td>{{ item.descricao }}</td>
                          <td>{{ item.dataCompetencia || '-' }}</td>
                          <td class="apex-tabular"><strong>{{ item.valor | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong></td>
                          <td>{{ item.sugestaoPlanoContaNome || '-' }}</td>
                          @if (canMutate()) {
                            <td>
                              <ion-select
                                aria-label="Plano de contas para reclassificação"
                                interface="popover"
                                [ngModel]="pendingDraft(item).planoContaId"
                                (ngModelChange)="updatePendingDraft(item, { planoContaId: $event })"
                              >
                                <ion-select-option [value]="null">Selecionar conta...</ion-select-option>
                                @for (conta of launchableAccounts(); track conta.idPlanoConta) {
                                  <ion-select-option [value]="conta.idPlanoConta">{{ conta.codigo }} - {{ conta.nome }}</ion-select-option>
                                }
                              </ion-select>
                            </td>
                            <td>
                              <ion-select
                                aria-label="Centro de custo para reclassificação"
                                interface="popover"
                                [ngModel]="pendingDraft(item).centroCustoId"
                                (ngModelChange)="updatePendingDraft(item, { centroCustoId: $event })"
                              >
                                <ion-select-option [value]="null">Opcional</ion-select-option>
                                @for (cc of activeCentrosCusto(); track cc.idCentroCusto) {
                                  <ion-select-option [value]="cc.idCentroCusto">{{ cc.codigo }} - {{ cc.nome }}</ion-select-option>
                                }
                              </ion-select>
                            </td>
                            <td>
                              <ion-input
                                aria-label="Motivo da reclassificação"
                                placeholder="Motivo (opcional)"
                                [ngModel]="pendingDraft(item).motivo"
                                (ngModelChange)="updatePendingDraft(item, { motivo: $event })"
                              ></ion-input>
                            </td>
                            <td>
                              <ion-button
                                size="small"
                                (click)="reclassificarItem(item)"
                                [disabled]="saving() || !pendingDraft(item).planoContaId"
                              >
                                Classificar
                              </ion-button>
                            </td>
                          }
                        </tr>
                      }
                    </tbody>
                  </table>
                </div>
              }
            </ion-card-content>
          </ion-card>

          <section class="split-layout" [class.full-width]="!canMutate()">
            <ion-card class="data-card">
              <ion-card-header>
                <ion-card-title>Regras Automáticas de Classificação ({{ regras().length }})</ion-card-title>
                <ion-card-subtitle>Prioridade menor executa primeiro; palavras-chave e vínculos definem conta e centro.</ion-card-subtitle>
              </ion-card-header>
              <ion-card-content>
                @if (regras().length === 0) {
                  <apex-empty-state
                    icon="filter-outline"
                    title="Nenhuma regra cadastrada"
                    description="Crie regras para classificar automaticamente despesas, vendas e títulos."
                  ></apex-empty-state>
                } @else {
                  <div class="table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th>Prioridade</th>
                          <th>Regra &amp; Critérios</th>
                          <th>Origem</th>
                          <th>Conta Destino</th>
                          <th>Centro de Custo</th>
                          <th>Status</th>
                          @if (canMutate()) {
                            <th>Ações</th>
                          }
                        </tr>
                      </thead>
                      <tbody>
                        @for (regra of regras(); track regra.idRegraClassificacao) {
                          <tr>
                            <td class="apex-tabular"><strong>{{ regra.prioridade }}</strong></td>
                            <td>
                              <strong>{{ regra.nome }}</strong>
                              @if (regra.palavraChave) {
                                <small>Palavra-chave: "{{ regra.palavraChave }}"</small>
                              }
                            </td>
                            <td>{{ origemLabel(regra.origemTipo) }}</td>
                            <td>{{ regra.planoContaCodigo ? regra.planoContaCodigo + ' - ' : '' }}{{ regra.planoContaNome || ('#' + regra.planoContaId) }}</td>
                            <td>{{ regra.centroCustoNome || (regra.centroCustoId ? '#' + regra.centroCustoId : '-') }}</td>
                            <td>
                              <span class="status-pill" [class.active]="regra.ativo" [class.inactive]="!regra.ativo">
                                {{ regra.ativo ? 'Ativa' : 'Inativa' }}
                              </span>
                            </td>
                            @if (canMutate()) {
                              <td>
                                <div class="table-actions">
                                  <ion-button
                                    size="small"
                                    fill="clear"
                                    (click)="editRegra(regra)"
                                    aria-label="Editar regra de classificação"
                                    title="Editar"
                                  >
                                    <ion-icon slot="icon-only" name="create-outline"></ion-icon>
                                  </ion-button>
                                  <ion-button
                                    size="small"
                                    fill="clear"
                                    [color]="regra.ativo ? 'danger' : 'success'"
                                    (click)="toggleRegraStatus(regra)"
                                    [attr.aria-label]="regra.ativo ? 'Inativar regra de classificação' : 'Ativar regra de classificação'"
                                    [title]="regra.ativo ? 'Inativar' : 'Ativar'"
                                  >
                                    <ion-icon
                                      slot="icon-only"
                                      [name]="regra.ativo ? 'ban-outline' : 'checkmark-circle-outline'"
                                    ></ion-icon>
                                  </ion-button>
                                </div>
                              </td>
                            }
                          </tr>
                        }
                      </tbody>
                    </table>
                  </div>
                }
              </ion-card-content>
            </ion-card>

            @if (canMutate()) {
              <ion-card class="data-card form-card">
                <ion-card-header>
                  <ion-card-title>{{ editingRegraId() ? 'Editar Regra' : 'Nova Regra Automática' }}</ion-card-title>
                  <ion-card-subtitle>Associe critérios operacionais ao plano de contas e centro de custo.</ion-card-subtitle>
                </ion-card-header>
                <ion-card-content>
                  <form class="stack" [formGroup]="regraForm" (ngSubmit)="saveRegra()">
                    <ion-input label="Nome da regra" labelPlacement="stacked" formControlName="nome" placeholder="Ex.: Energia Elétrica Loja"></ion-input>

                    <ion-select label="Origem" labelPlacement="stacked" formControlName="origemTipo">
                      @for (opt of origemOptions; track opt.value) {
                        <ion-select-option [value]="opt.value">{{ opt.label }}</ion-select-option>
                      }
                    </ion-select>

                    <ion-input label="Palavra-chave na descrição" labelPlacement="stacked" formControlName="palavraChave" placeholder="Ex.: CEMIG, ALUGUEL"></ion-input>

                    <ion-select label="Plano de contas destino" labelPlacement="stacked" formControlName="planoContaId">
                      <ion-select-option [value]="null">Selecione a conta...</ion-select-option>
                      @for (conta of launchableAccounts(); track conta.idPlanoConta) {
                        <ion-select-option [value]="conta.idPlanoConta">{{ conta.codigo }} - {{ conta.nome }}</ion-select-option>
                      }
                    </ion-select>

                    <ion-select label="Centro de custo (opcional)" labelPlacement="stacked" formControlName="centroCustoId">
                      <ion-select-option [value]="null">Nenhum</ion-select-option>
                      @for (cc of activeCentrosCusto(); track cc.idCentroCusto) {
                        <ion-select-option [value]="cc.idCentroCusto">{{ cc.codigo }} - {{ cc.nome }}</ion-select-option>
                      }
                    </ion-select>

                    @if (tiposDespesa().length > 0) {
                      <ion-select label="Tipo de despesa (opcional)" labelPlacement="stacked" formControlName="tipoDespesaId">
                        <ion-select-option [value]="null">Qualquer</ion-select-option>
                        @for (tipo of tiposDespesa(); track tipo.idTipoDespesa) {
                          <ion-select-option [value]="tipo.idTipoDespesa">{{ tipo.nome }}</ion-select-option>
                        }
                      </ion-select>
                    }

                    @if (categorias().length > 0) {
                      <ion-select label="Categoria (opcional)" labelPlacement="stacked" formControlName="categoriaId">
                        <ion-select-option [value]="null">Qualquer</ion-select-option>
                        @for (cat of categorias(); track cat.idCategoria) {
                          <ion-select-option [value]="cat.idCategoria">{{ cat.descricao }}</ion-select-option>
                        }
                      </ion-select>
                    }

                    @if (formasPagamento().length > 0) {
                      <ion-select label="Forma de pagamento (opcional)" labelPlacement="stacked" formControlName="formaPagamentoId">
                        <ion-select-option [value]="null">Qualquer</ion-select-option>
                        @for (fp of formasPagamento(); track fp.idFormaPagamento) {
                          <ion-select-option [value]="fp.idFormaPagamento">{{ fp.nome }}</ion-select-option>
                        }
                      </ion-select>
                    }

                    @if (fornecedores().length > 0) {
                      <ion-select label="Fornecedor (opcional)" labelPlacement="stacked" formControlName="fornecedorId">
                        <ion-select-option [value]="null">Qualquer</ion-select-option>
                        @for (forn of fornecedores(); track forn.idFornecedor) {
                          <ion-select-option [value]="forn.idFornecedor">{{ forn.razaoSocial }}</ion-select-option>
                        }
                      </ion-select>
                    }

                    <ion-input label="Prioridade" labelPlacement="stacked" type="number" formControlName="prioridade"></ion-input>

                    <div class="form-actions">
                      <ion-button type="submit" expand="block" [disabled]="saving()">
                        {{ editingRegraId() ? 'Salvar Regra' : 'Criar Regra' }}
                      </ion-button>
                      <ion-button type="button" expand="block" fill="outline" (click)="resetRegraForm()">
                        Limpar
                      </ion-button>
                    </div>
                  </form>
                </ion-card-content>
              </ion-card>
            }
          </section>
        }

        @if (tab() === 'competencia') {
          <section class="foundation-toolbar toolbar-card">
            <div class="toolbar-controls">
              <ion-input
                label="Competência (AAAA-MM)"
                labelPlacement="stacked"
                type="month"
                [ngModel]="competencia()"
                (ngModelChange)="competencia.set($event)"
              ></ion-input>

              <div class="toolbar-actions">
                <ion-button fill="outline" (click)="loadConsolidacao()" [disabled]="loading()">
                  <ion-icon slot="start" name="calendar-outline" aria-hidden="true"></ion-icon>
                  Consultar Mês
                </ion-button>
                @if (canMutate()) {
                  <ion-button (click)="recalcularCompetencia()" [disabled]="saving()">
                    <ion-icon slot="start" name="sync-outline" aria-hidden="true"></ion-icon>
                    Recalcular Competência
                  </ion-button>
                }
              </div>
            </div>
          </section>

          @if (consolidacao(); as cons) {
            @if (cons.lancamentosSemPlanoConta > 0) {
              <div class="feedback-banner warning" role="status">
                <ion-icon name="warning-outline" aria-hidden="true"></ion-icon>
                <div class="banner-body">
                  <strong>
                    {{ cons.lancamentosSemPlanoConta }} lançamento(s) sem plano de contas
                    ({{ cons.valorSemPlanoConta | currency:'BRL':'symbol':'1.2-2':'pt-BR' }})
                  </strong>
                  <span>Classifique as pendências para garantir acurácia total do DRE gerencial.</span>
                </div>
                <ion-button size="small" fill="outline" color="warning" (click)="setTab('classificacao')">
                  Ver pendências
                </ion-button>
              </div>
            }

            @if (cons.avisosLimitacao.length > 0) {
              <ion-card class="data-card warning-card section-gap">
                <ion-card-header>
                  <ion-card-title>Notas e limitações do período ({{ cons.competenciaAnoMes }})</ion-card-title>
                  <ion-card-subtitle>Período {{ cons.inicioPeriodo }} a {{ cons.fimPeriodo }}</ion-card-subtitle>
                </ion-card-header>
                <ion-card-content>
                  <ul class="limitation-list">
                    @for (aviso of cons.avisosLimitacao; track aviso) {
                      <li>{{ aviso }}</li>
                    }
                  </ul>
                </ion-card-content>
              </ion-card>
            }

            <section class="regime-columns section-gap">
              <div class="regime-block">
                <h2>Regime de Competência ({{ cons.competenciaAnoMes }})</h2>
                <div class="metric-grid two-col">
                  <article class="metric-card">
                    <span>Receitas Competência</span>
                    <strong class="apex-tabular">{{ cons.receitasCompetencia | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong>
                    <small>faturamento reconhecido no mês</small>
                  </article>
                  <article class="metric-card">
                    <span>CMV Competência</span>
                    <strong class="apex-tabular">{{ cons.cmvCompetencia | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong>
                    <small>custo das mercadorias vendidas</small>
                  </article>
                  <article class="metric-card">
                    <span>Despesas Competência</span>
                    <strong class="apex-tabular">{{ cons.despesasCompetencia | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong>
                    <small>despesas reconhecidas no período</small>
                  </article>
                  <article class="metric-card">
                    <span>Resultado Competência</span>
                    <strong class="apex-tabular">{{ cons.resultadoCompetencia | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong>
                    <small [class.positive]="cons.resultadoCompetencia >= 0" [class.danger]="cons.resultadoCompetencia < 0">
                      {{ cons.resultadoCompetencia >= 0 ? 'Superávit gerencial' : 'Déficit gerencial' }}
                    </small>
                  </article>
                </div>
              </div>

              <div class="regime-block">
                <h2>Regime de Caixa Realizado</h2>
                <div class="metric-grid two-col">
                  <article class="metric-card">
                    <span>Entradas Realizadas (Caixa)</span>
                    <strong class="apex-tabular">{{ cons.entradasRealizadasCaixa | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong>
                    <small>recebimentos efetivados no período</small>
                  </article>
                  <article class="metric-card">
                    <span>Saídas Realizadas (Caixa)</span>
                    <strong class="apex-tabular">{{ cons.saidasRealizadasCaixa | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong>
                    <small>pagamentos efetivados no período</small>
                  </article>
                  <article class="metric-card">
                    <span>Saldo Realizado (Caixa)</span>
                    <strong class="apex-tabular">{{ cons.saldoRealizadoCaixa | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong>
                    <small [class.positive]="cons.saldoRealizadoCaixa >= 0" [class.danger]="cons.saldoRealizadoCaixa < 0">
                      {{ cons.saldoRealizadoCaixa >= 0 ? 'Caixa líquido positivo' : 'Caixa líquido negativo' }}
                    </small>
                  </article>
                  <article class="metric-card">
                    <span>Diferença Competência vs Caixa</span>
                    <strong class="apex-tabular">
                      {{ (cons.resultadoCompetencia - cons.saldoRealizadoCaixa) | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}
                    </strong>
                    <small>efeito de prazos e recebíveis</small>
                  </article>
                </div>
              </div>
            </section>

            <ion-card class="data-card">
              <ion-card-header>
                <ion-card-title>Detalhamento por Conta Gerencial e Centro de Custo ({{ cons.linhas.length }})</ion-card-title>
                <ion-card-subtitle>Visão consolidada para DRE gerencial e controle orçamentário.</ion-card-subtitle>
              </ion-card-header>
              <ion-card-content>
                @if (cons.linhas.length === 0) {
                  <apex-empty-state
                    icon="albums-outline"
                    title="Sem movimentações consolidadas no mês"
                    description="Recalcule a competência ou escolha outro mês para visualizar o detalhamento."
                  ></apex-empty-state>
                } @else {
                  <div class="table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th>Conta Gerencial</th>
                          <th>Grupo DRE</th>
                          <th>Custo</th>
                          <th>Centro de Custo</th>
                          <th>Lançamentos</th>
                          <th>Competência</th>
                          <th>Realizado Caixa</th>
                          <th>Diferença</th>
                        </tr>
                      </thead>
                      <tbody>
                        @for (linha of cons.linhas; track (linha.planoContaId ?? 'sem') + '-' + (linha.centroCustoId ?? 'sem') + '-' + $index) {
                          <tr>
                            <td>
                              <strong>
                                {{ linha.planoContaCodigo ? linha.planoContaCodigo + ' · ' : '' }}{{ linha.planoContaNome || 'Sem classificação' }}
                              </strong>
                            </td>
                            <td>{{ linha.grupoDre ? grupoDreLabel(linha.grupoDre) : '-' }}</td>
                            <td>{{ linha.classificacaoCusto || '-' }}</td>
                            <td>
                              {{ linha.centroCustoCodigo ? linha.centroCustoCodigo + ' · ' : '' }}{{ linha.centroCustoNome || 'Sem centro' }}
                            </td>
                            <td class="apex-tabular">{{ linha.quantidadeLancamentos }}</td>
                            <td class="apex-tabular">{{ linha.valorCompetencia | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</td>
                            <td class="apex-tabular">{{ linha.valorRealizadoCaixa | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</td>
                            <td class="apex-tabular">
                              {{ (linha.valorCompetencia - linha.valorRealizadoCaixa) | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}
                            </td>
                          </tr>
                        }
                      </tbody>
                    </table>
                  </div>
                }
              </ion-card-content>
            </ion-card>
          } @else {
            <apex-empty-state
              icon="calendar-outline"
              title="Nenhuma consolidação carregada"
              description="Escolha o mês e clique em Consultar Mês para carregar a comparação competência vs caixa."
            ></apex-empty-state>
          }
        }
      </main>
    </ion-content>
  `,
  styles: [`
    .foundation-page ion-segment {
      margin-bottom: 14px;
    }

    .foundation-toolbar {
      margin-bottom: 14px;
      padding: 14px;
    }

    .toolbar-controls {
      align-items: center;
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      justify-content: space-between;
    }

    .toolbar-controls ion-searchbar {
      flex: 1 1 280px;
      max-width: 460px;
    }

    .toolbar-controls ion-input,
    .toolbar-controls ion-select {
      flex: 0 1 260px;
    }

    .inline-switches {
      align-items: center;
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
    }

    .inline-switches ion-segment {
      margin-bottom: 0;
    }

    .toolbar-actions {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .feedback-banner {
      align-items: center;
      background: var(--apex-status-info-bg);
      border: 1px solid var(--apex-status-info-border);
      border-radius: var(--apex-radius-md);
      color: var(--apex-status-info);
      display: flex;
      flex-wrap: wrap;
      font-size: var(--apex-font-size-sm);
      gap: 10px;
      margin-bottom: 14px;
      padding: 11px 14px;
    }

    .feedback-banner.success {
      background: var(--apex-status-success-bg);
      border-color: var(--apex-status-success-border);
      color: var(--apex-status-success);
    }

    .feedback-banner.warning {
      background: var(--apex-status-warning-bg);
      border-color: var(--apex-status-warning-border);
      color: var(--apex-status-warning);
    }

    .feedback-banner.danger {
      background: var(--apex-status-danger-bg);
      border-color: var(--apex-status-danger-border);
      color: var(--apex-status-danger);
    }

    .banner-body {
      display: grid;
      flex: 1 1 240px;
      gap: 2px;
    }

    .split-layout {
      align-items: start;
      display: grid;
      gap: 16px;
      grid-template-columns: minmax(0, 1.55fr) minmax(320px, 0.75fr);
    }

    .split-layout.full-width {
      grid-template-columns: 1fr;
    }

    .section-gap {
      margin-bottom: 16px;
    }

    .group-stack {
      display: grid;
      gap: 16px;
    }

    .dre-group {
      border: 1px solid var(--apex-border-subtle);
      border-radius: var(--apex-radius-md);
      overflow: hidden;
    }

    .dre-group-head {
      align-items: center;
      background: var(--apex-bg-surface-muted);
      border-bottom: 1px solid var(--apex-border-subtle);
      display: flex;
      justify-content: space-between;
      padding: 10px 14px;
    }

    .dre-group-head h3 {
      color: var(--apex-text-primary);
      font-size: var(--apex-font-size-sm);
      font-weight: 750;
      margin: 0;
    }

    .tree-cell {
      align-items: center;
      display: flex;
      gap: 6px;
    }

    .tree-branch {
      color: var(--apex-text-muted);
      font-family: var(--apex-font-mono);
    }

    td small {
      color: var(--apex-text-muted);
      display: block;
      font-size: var(--apex-font-size-xs);
      margin-top: 2px;
    }

    .padrao-tag {
      color: var(--apex-brand-primary);
      font-weight: 600;
    }

    .form-card ion-input,
    .form-card ion-select {
      --background: var(--apex-bg-input);
      --border-color: var(--apex-border-default);
      --border-radius: 8px;
      --border-style: solid;
      --border-width: 1px;
      --padding-start: 12px;
      --padding-end: 12px;
    }

    .checkbox-row {
      --padding-start: 0;
      --inner-padding-end: 0;
    }

    .form-actions {
      display: flex;
      gap: 8px;
    }

    .classification-metrics {
      margin-bottom: 14px;
    }

    .batch-summary {
      border-top: 1px solid var(--apex-border-subtle);
      color: var(--apex-text-secondary);
      font-size: var(--apex-font-size-sm);
      margin-top: 12px;
      padding-top: 10px;
    }

    .batch-summary ul,
    .limitation-list {
      margin: 6px 0 0;
      padding-left: 18px;
    }

    .regime-columns {
      display: grid;
      gap: 16px;
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .regime-block h2 {
      color: var(--apex-text-primary);
      font-size: var(--apex-font-size-md);
      font-weight: 750;
      margin: 0 0 10px;
    }

    .metric-grid.two-col {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .warning-card {
      border-color: var(--apex-status-warning-border);
    }

    @media (max-width: 1080px) {
      .split-layout,
      .regime-columns {
        grid-template-columns: 1fr;
      }
    }

    @media (max-width: 640px) {
      .metric-grid.two-col {
        grid-template-columns: 1fr;
      }

      .form-actions {
        flex-direction: column;
      }
    }
  `]
})
export class FinanceFoundationPage {
  private readonly api = inject(ApexApiService);
  private readonly fb = inject(FormBuilder);
  private readonly session = inject(SessionService);

  readonly tipoContaOptions = TIPO_CONTA_OPTIONS;
  readonly naturezaOptions = NATUREZA_OPTIONS;
  readonly classificacaoCustoOptions = CLASSIFICACAO_CUSTO_OPTIONS;
  readonly grupoDreOptions = GRUPO_DRE_OPTIONS;
  readonly tipoCentroCustoOptions = TIPO_CENTRO_CUSTO_OPTIONS;
  readonly origemOptions = ORIGEM_CLASSIFICACAO_OPTIONS;

  readonly tab = signal<FoundationTab>('plano-contas');
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly feedbackMessage = signal<string | null>(null);
  readonly feedbackTone = signal<'info' | 'success' | 'warning' | 'danger'>('info');

  readonly canMutate = computed(() => this.session.hasAnyRole(FOUNDATION_MUTATION_ROLES));
  readonly isAuditorReadOnly = computed(() => this.session.hasAnyRole(['ROLE_AUDITOR']) && !this.canMutate());

  // Tab 1: Plano de Contas
  readonly planoContas = signal<PlanoContasResponse[]>([]);
  readonly planoContasTree = signal<PlanoContasResponse[]>([]);
  readonly planoViewMode = signal<PlanoViewMode>('grouped');
  readonly planoSomenteAtivos = signal(false);
  readonly planoSearch = signal('');
  readonly editingPlanoId = signal<number | null>(null);

  // Tab 2: Centros de Custo
  readonly centrosCusto = signal<CentroCustoResponse[]>([]);
  readonly centroSomenteAtivos = signal(false);
  readonly centroSearch = signal('');
  readonly editingCentroId = signal<number | null>(null);

  // Tab 3: Classificação & Pendências
  readonly pendencias = signal<PendenciaClassificacaoResponse | null>(null);
  readonly regras = signal<RegraClassificacaoResponse[]>([]);
  readonly selectedOrigem = signal<'' | OrigemClassificacaoFinanceira>('');
  readonly ultimoLoteResultado = signal<ResultadoClassificacaoLote | null>(null);
  readonly editingRegraId = signal<number | null>(null);
  readonly pendingDrafts = signal<Record<string, PendingDraft>>({});

  readonly tiposDespesa = signal<TipoDespesa[]>([]);
  readonly categorias = signal<Categoria[]>([]);
  readonly formasPagamento = signal<FormaPagamento[]>([]);
  readonly fornecedores = signal<Fornecedor[]>([]);

  // Tab 4: Competência vs Caixa
  readonly competencia = signal(currentYearMonth());
  readonly consolidacao = signal<ConsolidacaoCompetenciaResponse | null>(null);

  readonly planoForm = this.fb.nonNullable.group({
    codigo: ['', Validators.required],
    nome: ['', Validators.required],
    descricao: [''],
    contaPaiId: this.fb.control<number | null>(null),
    tipoConta: ['DESPESA' as TipoContaGerencial, Validators.required],
    natureza: ['DEBITO' as NaturezaContaGerencial, Validators.required],
    classificacaoCusto: ['FIXO' as ClassificacaoCusto, Validators.required],
    grupoDre: ['DESPESA_OPERACIONAL' as GrupoDreGerencial, Validators.required],
    ordemExibicao: [0],
    aceitaLancamento: [true]
  });

  readonly centroForm = this.fb.nonNullable.group({
    codigo: ['', Validators.required],
    nome: ['', Validators.required],
    descricao: [''],
    tipoOperacional: ['LOJA' as TipoCentroCusto, Validators.required],
    responsavelUsuarioId: this.fb.control<number | null>(null)
  });

  readonly regraForm = this.fb.nonNullable.group({
    nome: ['', Validators.required],
    origemTipo: ['DESPESA' as OrigemClassificacaoFinanceira, Validators.required],
    palavraChave: [''],
    tipoDespesaId: this.fb.control<number | null>(null),
    categoriaId: this.fb.control<number | null>(null),
    formaPagamentoId: this.fb.control<number | null>(null),
    fornecedorId: this.fb.control<number | null>(null),
    planoContaId: this.fb.control<number | null>(null, Validators.required),
    centroCustoId: this.fb.control<number | null>(null),
    prioridade: [100]
  });

  readonly filteredPlanoList = computed(() => {
    const term = this.planoSearch().trim().toLowerCase();
    const onlyActive = this.planoSomenteAtivos();
    return this.planoContas().filter((conta) => {
      if (onlyActive && !conta.ativo) {
        return false;
      }
      if (!term) {
        return true;
      }
      return (
        conta.codigo.toLowerCase().includes(term) ||
        conta.nome.toLowerCase().includes(term) ||
        this.grupoDreLabel(conta.grupoDre).toLowerCase().includes(term)
      );
    });
  });

  readonly filteredTreeNodes = computed<FlattenedAccountNode[]>(() => {
    const term = this.planoSearch().trim().toLowerCase();
    const onlyActive = this.planoSomenteAtivos();
    const sourceTree = this.planoContasTree().length > 0 ? this.planoContasTree() : this.planoContas();
    const flattened: FlattenedAccountNode[] = [];

    const visit = (nodes: PlanoContasResponse[], depth: number): void => {
      for (const node of nodes) {
        const matches =
          (!onlyActive || node.ativo) &&
          (!term ||
            node.codigo.toLowerCase().includes(term) ||
            node.nome.toLowerCase().includes(term) ||
            this.grupoDreLabel(node.grupoDre).toLowerCase().includes(term));
        if (matches) {
          flattened.push({ account: node, depth });
        }
        if (node.filhas?.length) {
          visit(node.filhas, depth + 1);
        }
      }
    };
    visit(sourceTree, 0);
    return flattened;
  });

  readonly filteredPlanoCount = computed(() =>
    this.planoViewMode() === 'tree' ? this.filteredTreeNodes().length : this.filteredPlanoList().length
  );

  readonly groupedPlanoContas = computed(() => {
    const byGroup = new Map<string, PlanoContasResponse[]>();
    for (const conta of this.filteredPlanoList()) {
      const current = byGroup.get(conta.grupoDre) ?? [];
      current.push(conta);
      byGroup.set(conta.grupoDre, current);
    }
    const orderedGroups: Array<{ grupo: string; label: string; accounts: PlanoContasResponse[] }> = [];
    for (const opt of GRUPO_DRE_OPTIONS) {
      const accounts = byGroup.get(opt.value);
      if (accounts?.length) {
        orderedGroups.push({ grupo: opt.value, label: opt.label, accounts });
        byGroup.delete(opt.value);
      }
    }
    for (const [grupo, accounts] of byGroup.entries()) {
      orderedGroups.push({ grupo, label: this.grupoDreLabel(grupo), accounts });
    }
    return orderedGroups;
  });

  readonly launchableAccounts = computed(() => {
    const active = this.planoContas().filter((conta) => conta.ativo);
    const analytical = active.filter((conta) => conta.aceitaLancamento);
    return analytical.length > 0 ? analytical : active;
  });

  readonly filteredCentros = computed(() => {
    const term = this.centroSearch().trim().toLowerCase();
    const onlyActive = this.centroSomenteAtivos();
    return this.centrosCusto().filter((cc) => {
      if (onlyActive && !cc.ativo) {
        return false;
      }
      if (!term) {
        return true;
      }
      return (
        cc.codigo.toLowerCase().includes(term) ||
        cc.nome.toLowerCase().includes(term) ||
        cc.tipoOperacional.toLowerCase().includes(term)
      );
    });
  });

  readonly activeCentrosCusto = computed(() => this.centrosCusto().filter((cc) => cc.ativo));

  readonly visiblePendingItems = computed(() => {
    const items = this.pendencias()?.itens ?? [];
    const origin = this.selectedOrigem();
    return origin ? items.filter((item) => item.origemTipo === origin) : items;
  });

  constructor() {
    this.loadInitialData();
  }

  setTab(value: unknown): void {
    if (value === 'plano-contas' || value === 'centros-custo' || value === 'classificacao' || value === 'competencia') {
      this.tab.set(value);
      this.feedbackMessage.set(null);
    }
  }

  setPlanoViewMode(value: unknown): void {
    if (value === 'grouped' || value === 'tree') {
      this.planoViewMode.set(value);
    }
  }

  togglePlanoActiveFilter(checked: boolean): void {
    this.planoSomenteAtivos.set(checked);
    this.loadPlanoContas();
  }

  toggleCentroActiveFilter(checked: boolean): void {
    this.centroSomenteAtivos.set(checked);
    this.loadCentrosCusto();
  }

  refreshCurrentTab(): void {
    const current = this.tab();
    if (current === 'plano-contas') {
      this.loadPlanoContas();
      return;
    }
    if (current === 'centros-custo') {
      this.loadCentrosCusto();
      return;
    }
    if (current === 'classificacao') {
      this.loadClassificacao();
      return;
    }
    this.loadConsolidacao();
  }

  // --- Plano de Contas ---

  bootstrapPlanoContas(): void {
    if (!this.canMutate()) {
      return;
    }
    this.saving.set(true);
    this.feedbackMessage.set(null);
    this.api
      .bootstrapPlanoContasPadrao()
      .pipe(
        catchError((error: unknown) => {
          this.setErrorFeedback(error, 'Não foi possível inicializar o plano de contas padrão.');
          return of(null);
        }),
        finalize(() => this.saving.set(false))
      )
      .subscribe((res) => {
        if (!res) {
          return;
        }
        this.setSuccessFeedback(
          `Plano padrão processado: ${res.contasCriadas} criada(s), ${res.contasJaExistentes} já existente(s) (total: ${res.totalContas}).`
        );
        this.loadPlanoContas();
      });
  }

  savePlanoConta(): void {
    if (!this.canMutate()) {
      return;
    }
    if (this.planoForm.invalid) {
      this.setWarningFeedback('Informe ao menos código, nome, tipo, natureza, custo e grupo DRE da conta.');
      return;
    }
    const raw = this.planoForm.getRawValue();
    const payload: PlanoContasRequest = {
      codigo: raw.codigo.trim(),
      nome: raw.nome.trim(),
      descricao: raw.descricao.trim() || undefined,
      contaPaiId: raw.contaPaiId ?? undefined,
      tipoConta: raw.tipoConta,
      natureza: raw.natureza,
      classificacaoCusto: raw.classificacaoCusto,
      grupoDre: raw.grupoDre,
      ordemExibicao: Number(raw.ordemExibicao ?? 0),
      aceitaLancamento: raw.aceitaLancamento
    };
    const editingId = this.editingPlanoId();
    this.saving.set(true);
    this.feedbackMessage.set(null);

    const request$ = editingId
      ? this.api.updatePlanoConta(editingId, payload)
      : this.api.createPlanoConta(payload);

    request$
      .pipe(
        catchError((error: unknown) => {
          this.setErrorFeedback(error, 'Não foi possível salvar a conta gerencial.');
          return of(null);
        }),
        finalize(() => this.saving.set(false))
      )
      .subscribe((saved) => {
        if (!saved) {
          return;
        }
        this.setSuccessFeedback(editingId ? 'Conta gerencial atualizada com sucesso.' : 'Conta gerencial criada com sucesso.');
        this.resetPlanoForm();
        this.loadPlanoContas();
      });
  }

  editPlanoConta(conta: PlanoContasResponse): void {
    this.editingPlanoId.set(conta.idPlanoConta);
    this.planoForm.patchValue({
      codigo: conta.codigo,
      nome: conta.nome,
      descricao: conta.descricao ?? '',
      contaPaiId: conta.contaPaiId ?? null,
      tipoConta: conta.tipoConta,
      natureza: conta.natureza,
      classificacaoCusto: conta.classificacaoCusto,
      grupoDre: conta.grupoDre,
      ordemExibicao: conta.ordemExibicao ?? 0,
      aceitaLancamento: conta.aceitaLancamento
    });
  }

  resetPlanoForm(): void {
    this.editingPlanoId.set(null);
    this.planoForm.reset({
      codigo: '',
      nome: '',
      descricao: '',
      contaPaiId: null,
      tipoConta: 'DESPESA',
      natureza: 'DEBITO',
      classificacaoCusto: 'FIXO',
      grupoDre: 'DESPESA_OPERACIONAL',
      ordemExibicao: 0,
      aceitaLancamento: true
    });
  }

  togglePlanoStatus(conta: PlanoContasResponse): void {
    if (!this.canMutate()) {
      return;
    }
    this.saving.set(true);
    this.api
      .togglePlanoContaStatus(conta.idPlanoConta, !conta.ativo)
      .pipe(
        catchError((error: unknown) => {
          this.setErrorFeedback(error, 'Não foi possível alterar o status da conta gerencial.');
          return of(null);
        }),
        finalize(() => this.saving.set(false))
      )
      .subscribe((updated) => {
        if (!updated) {
          return;
        }
        this.setSuccessFeedback(`Conta ${updated.codigo} ${updated.ativo ? 'ativada' : 'inativada'}.`);
        this.loadPlanoContas();
      });
  }

  // --- Centros de Custo ---

  bootstrapCentrosCusto(): void {
    if (!this.canMutate()) {
      return;
    }
    this.saving.set(true);
    this.feedbackMessage.set(null);
    this.api
      .bootstrapCentrosCustoPadrao()
      .pipe(
        catchError((error: unknown) => {
          this.setErrorFeedback(error, 'Não foi possível inicializar os centros de custo padrão.');
          return of(null);
        }),
        finalize(() => this.saving.set(false))
      )
      .subscribe((res) => {
        if (!res) {
          return;
        }
        if (Array.isArray(res)) {
          this.setSuccessFeedback(`${res.length} centro(s) de custo padrão sincronizado(s).`);
        } else {
          const info = res as CentroCustoBootstrapResponse;
          this.setSuccessFeedback(
            `Centros padrão inicializados: ${info.centrosCriados ?? 0} criado(s), ${info.centrosJaExistentes ?? 0} já existente(s).`
          );
        }
        this.loadCentrosCusto();
      });
  }

  saveCentroCusto(): void {
    if (!this.canMutate()) {
      return;
    }
    if (this.centroForm.invalid) {
      this.setWarningFeedback('Informe código, nome e tipo operacional do centro de custo.');
      return;
    }
    const raw = this.centroForm.getRawValue();
    const payload: CentroCustoRequest = {
      codigo: raw.codigo.trim(),
      nome: raw.nome.trim(),
      descricao: raw.descricao.trim() || undefined,
      tipoOperacional: raw.tipoOperacional,
      responsavelUsuarioId: raw.responsavelUsuarioId ? Number(raw.responsavelUsuarioId) : undefined
    };
    const editingId = this.editingCentroId();
    this.saving.set(true);
    this.feedbackMessage.set(null);

    const request$ = editingId
      ? this.api.updateCentroCusto(editingId, payload)
      : this.api.createCentroCusto(payload);

    request$
      .pipe(
        catchError((error: unknown) => {
          this.setErrorFeedback(error, 'Não foi possível salvar o centro de custo.');
          return of(null);
        }),
        finalize(() => this.saving.set(false))
      )
      .subscribe((saved) => {
        if (!saved) {
          return;
        }
        this.setSuccessFeedback(editingId ? 'Centro de custo atualizado.' : 'Centro de custo criado.');
        this.resetCentroForm();
        this.loadCentrosCusto();
      });
  }

  editCentroCusto(cc: CentroCustoResponse): void {
    this.editingCentroId.set(cc.idCentroCusto);
    this.centroForm.patchValue({
      codigo: cc.codigo,
      nome: cc.nome,
      descricao: cc.descricao ?? '',
      tipoOperacional: cc.tipoOperacional,
      responsavelUsuarioId: cc.responsavelUsuarioId ?? null
    });
  }

  resetCentroForm(): void {
    this.editingCentroId.set(null);
    this.centroForm.reset({
      codigo: '',
      nome: '',
      descricao: '',
      tipoOperacional: 'LOJA',
      responsavelUsuarioId: null
    });
  }

  toggleCentroStatus(cc: CentroCustoResponse): void {
    if (!this.canMutate()) {
      return;
    }
    this.saving.set(true);
    this.api
      .toggleCentroCustoStatus(cc.idCentroCusto, !cc.ativo)
      .pipe(
        catchError((error: unknown) => {
          this.setErrorFeedback(error, 'Não foi possível atualizar o status do centro de custo.');
          return of(null);
        }),
        finalize(() => this.saving.set(false))
      )
      .subscribe((updated) => {
        if (!updated) {
          return;
        }
        this.setSuccessFeedback(`Centro ${updated.codigo} ${updated.ativo ? 'ativado' : 'inativado'}.`);
        this.loadCentrosCusto();
      });
  }

  // --- Classificação & Pendências ---

  onOrigemChange(value: '' | OrigemClassificacaoFinanceira): void {
    const origem = value || '';
    this.selectedOrigem.set(origem);
    this.loadPendencias(origem || undefined);
    this.loadRegras();
  }

  aplicarClassificacaoAutomatica(): void {
    if (!this.canMutate()) {
      return;
    }
    this.saving.set(true);
    this.feedbackMessage.set(null);
    this.api
      .aplicarClassificacaoAutomatica(this.selectedOrigem() || undefined)
      .pipe(
        catchError((error: unknown) => {
          this.setErrorFeedback(error, 'Não foi possível aplicar a classificação automática.');
          return of(null);
        }),
        finalize(() => this.saving.set(false))
      )
      .subscribe((res) => {
        if (!res) {
          return;
        }
        this.ultimoLoteResultado.set(res);
        this.setSuccessFeedback(`Classificação em lote: ${res.classificados} de ${res.processados} lançamento(s) classificados.`);
        this.loadClassificacao();
        this.loadConsolidacao();
      });
  }

  pendingKey(item: ItemPendenteClassificacao): string {
    return `${item.origemTipo}:${item.registroId}`;
  }

  pendingDraft(item: ItemPendenteClassificacao): PendingDraft {
    const key = this.pendingKey(item);
    return (
      this.pendingDrafts()[key] ?? {
        planoContaId: item.sugestaoPlanoContaId ?? null,
        centroCustoId: item.sugestaoCentroCustoId ?? null,
        motivo: ''
      }
    );
  }

  updatePendingDraft(item: ItemPendenteClassificacao, patch: Partial<PendingDraft>): void {
    const key = this.pendingKey(item);
    const current = this.pendingDraft(item);
    this.pendingDrafts.update((state) => ({
      ...state,
      [key]: { ...current, ...patch }
    }));
  }

  reclassificarItem(item: ItemPendenteClassificacao): void {
    if (!this.canMutate()) {
      return;
    }
    const draft = this.pendingDraft(item);
    if (!draft.planoContaId) {
      this.setWarningFeedback('Selecione uma conta gerencial antes de classificar o lançamento.');
      return;
    }
    this.saving.set(true);
    this.feedbackMessage.set(null);
    this.api
      .reclassificarManualmente({
        origemTipo: item.origemTipo,
        registroId: item.registroId,
        planoContaId: draft.planoContaId,
        centroCustoId: draft.centroCustoId ?? undefined,
        motivo: draft.motivo.trim() || undefined
      })
      .pipe(
        catchError((error: unknown) => {
          this.setErrorFeedback(error, 'Não foi possível reclassificar o lançamento.');
          return of(null);
        }),
        finalize(() => this.saving.set(false))
      )
      .subscribe((res) => {
        if (res === null) {
          return;
        }
        this.setSuccessFeedback(`Lançamento #${item.registroId} (${this.origemLabel(item.origemTipo)}) classificado.`);
        this.loadClassificacao();
        this.loadConsolidacao();
      });
  }

  saveRegra(): void {
    if (!this.canMutate()) {
      return;
    }
    if (this.regraForm.invalid) {
      this.setWarningFeedback('Informe nome, origem e conta gerencial destino para a regra.');
      return;
    }
    const raw = this.regraForm.getRawValue();
    if (!raw.planoContaId) {
      this.setWarningFeedback('Selecione a conta gerencial destino.');
      return;
    }
    const payload: RegraClassificacaoRequest = {
      nome: raw.nome.trim(),
      origemTipo: raw.origemTipo,
      palavraChave: raw.palavraChave.trim() || undefined,
      tipoDespesaId: raw.tipoDespesaId ?? undefined,
      categoriaId: raw.categoriaId ?? undefined,
      formaPagamentoId: raw.formaPagamentoId ?? undefined,
      fornecedorId: raw.fornecedorId ?? undefined,
      planoContaId: raw.planoContaId,
      centroCustoId: raw.centroCustoId ?? undefined,
      prioridade: Number(raw.prioridade ?? 100)
    };
    const editingId = this.editingRegraId();
    this.saving.set(true);
    this.feedbackMessage.set(null);

    const request$ = editingId
      ? this.api.updateRegraClassificacao(editingId, payload)
      : this.api.createRegraClassificacao(payload);

    request$
      .pipe(
        catchError((error: unknown) => {
          this.setErrorFeedback(error, 'Não foi possível salvar a regra de classificação.');
          return of(null);
        }),
        finalize(() => this.saving.set(false))
      )
      .subscribe((saved) => {
        if (!saved) {
          return;
        }
        this.setSuccessFeedback(editingId ? 'Regra de classificação atualizada.' : 'Regra de classificação criada.');
        this.resetRegraForm();
        this.loadRegras();
      });
  }

  editRegra(regra: RegraClassificacaoResponse): void {
    this.editingRegraId.set(regra.idRegraClassificacao);
    this.regraForm.patchValue({
      nome: regra.nome,
      origemTipo: regra.origemTipo,
      palavraChave: regra.palavraChave ?? '',
      tipoDespesaId: regra.tipoDespesaId ?? null,
      categoriaId: regra.categoriaId ?? null,
      formaPagamentoId: regra.formaPagamentoId ?? null,
      fornecedorId: regra.fornecedorId ?? null,
      planoContaId: regra.planoContaId,
      centroCustoId: regra.centroCustoId ?? null,
      prioridade: regra.prioridade ?? 100
    });
  }

  resetRegraForm(): void {
    this.editingRegraId.set(null);
    this.regraForm.reset({
      nome: '',
      origemTipo: 'DESPESA',
      palavraChave: '',
      tipoDespesaId: null,
      categoriaId: null,
      formaPagamentoId: null,
      fornecedorId: null,
      planoContaId: null,
      centroCustoId: null,
      prioridade: 100
    });
  }

  toggleRegraStatus(regra: RegraClassificacaoResponse): void {
    if (!this.canMutate()) {
      return;
    }
    this.saving.set(true);
    this.api
      .toggleRegraClassificacaoStatus(regra.idRegraClassificacao, !regra.ativo)
      .pipe(
        catchError((error: unknown) => {
          this.setErrorFeedback(error, 'Não foi possível alterar o status da regra.');
          return of(null);
        }),
        finalize(() => this.saving.set(false))
      )
      .subscribe((updated) => {
        if (!updated) {
          return;
        }
        this.setSuccessFeedback(`Regra "${updated.nome}" ${updated.ativo ? 'ativada' : 'inativada'}.`);
        this.loadRegras();
      });
  }

  // --- Competência vs Caixa ---

  loadConsolidacao(): void {
    this.loading.set(true);
    this.api
      .getConsolidacaoCompetencia(this.competencia() || undefined)
      .pipe(
        catchError((error: unknown) => {
          this.setErrorFeedback(error, 'Não foi possível carregar a consolidação por competência.');
          return of(null);
        }),
        finalize(() => this.loading.set(false))
      )
      .subscribe((res) => {
        if (res) {
          this.consolidacao.set(res);
        }
      });
  }

  recalcularCompetencia(): void {
    if (!this.canMutate()) {
      return;
    }
    this.saving.set(true);
    this.feedbackMessage.set(null);
    this.api
      .recalcularConsolidacaoCompetencia(this.competencia() || undefined)
      .pipe(
        catchError((error: unknown) => {
          this.setErrorFeedback(error, 'Não foi possível recalcular a competência selecionada.');
          return of(null);
        }),
        finalize(() => this.saving.set(false))
      )
      .subscribe((res) => {
        if (!res) {
          return;
        }
        this.consolidacao.set(res);
        this.setSuccessFeedback(`Competência ${res.competenciaAnoMes} recalculada com sucesso.`);
      });
  }

  grupoDreLabel(grupo: string): string {
    return GRUPO_DRE_LABELS[grupo] ?? grupo;
  }

  origemLabel(origem: string): string {
    return ORIGEM_LABELS[origem] ?? origem;
  }

  private loadInitialData(): void {
    this.loading.set(true);
    let failedCore = false;

    forkJoin({
      contas: this.api.listPlanoContas().pipe(
        catchError(() => {
          failedCore = true;
          return of([] as PlanoContasResponse[]);
        })
      ),
      arvore: this.api.getPlanoContasTree().pipe(catchError(() => of([] as PlanoContasResponse[]))),
      centros: this.api.listCentrosCusto().pipe(
        catchError(() => {
          failedCore = true;
          return of([] as CentroCustoResponse[]);
        })
      ),
      regras: this.api.listRegrasClassificacao().pipe(catchError(() => of([] as RegraClassificacaoResponse[]))),
      pendencias: this.api.getPendenciasClassificacao().pipe(catchError(() => of(null))),
      consolidacao: this.api.getConsolidacaoCompetencia(this.competencia()).pipe(catchError(() => of(null))),
      tiposDespesa: this.api.listTiposDespesa().pipe(catchError(() => of([] as TipoDespesa[]))),
      categorias: this.api.list<Categoria>('/api/categorias').pipe(catchError(() => of([] as Categoria[]))),
      formasPagamento: this.api.paymentMethods().pipe(catchError(() => of([] as FormaPagamento[]))),
      fornecedores: this.api.list<Fornecedor>('/api/fornecedores').pipe(catchError(() => of([] as Fornecedor[])))
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((data) => {
        this.planoContas.set(Array.isArray(data.contas) ? data.contas : []);
        this.planoContasTree.set(Array.isArray(data.arvore) ? data.arvore : []);
        this.centrosCusto.set(Array.isArray(data.centros) ? data.centros : []);
        this.regras.set(Array.isArray(data.regras) ? data.regras : []);
        this.applyPendencias(data.pendencias);
        if (data.consolidacao) {
          this.consolidacao.set(data.consolidacao);
        }
        this.tiposDespesa.set(Array.isArray(data.tiposDespesa) ? data.tiposDespesa : []);
        this.categorias.set(Array.isArray(data.categorias) ? data.categorias : []);
        this.formasPagamento.set(Array.isArray(data.formasPagamento) ? data.formasPagamento : []);
        this.fornecedores.set(Array.isArray(data.fornecedores) ? data.fornecedores : []);

        if (failedCore) {
          this.setWarningFeedback('Alguns módulos financeiros não puderam ser carregados no momento.');
        }
      });
  }

  private loadPlanoContas(): void {
    this.loading.set(true);
    const activeFilter = this.planoSomenteAtivos() ? true : undefined;
    forkJoin({
      contas: this.api.listPlanoContas(activeFilter).pipe(catchError(() => of(null))),
      arvore: this.api.getPlanoContasTree(activeFilter).pipe(catchError(() => of([] as PlanoContasResponse[])))
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe(({ contas, arvore }) => {
        if (contas === null) {
          this.setWarningFeedback('Não foi possível carregar o plano de contas.');
          return;
        }
        this.planoContas.set(Array.isArray(contas) ? contas : []);
        this.planoContasTree.set(Array.isArray(arvore) ? arvore : []);
      });
  }

  private loadCentrosCusto(): void {
    this.loading.set(true);
    const activeFilter = this.centroSomenteAtivos() ? true : undefined;
    this.api
      .listCentrosCusto(activeFilter)
      .pipe(
        catchError((error: unknown) => {
          this.setErrorFeedback(error, 'Não foi possível carregar os centros de custo.');
          return of(null);
        }),
        finalize(() => this.loading.set(false))
      )
      .subscribe((centros) => {
        if (centros) {
          this.centrosCusto.set(Array.isArray(centros) ? centros : []);
        }
      });
  }

  private loadClassificacao(): void {
    this.loading.set(true);
    const origem = this.selectedOrigem() || undefined;
    forkJoin({
      pendencias: this.api.getPendenciasClassificacao(origem).pipe(catchError(() => of(null))),
      regras: this.api.listRegrasClassificacao(origem).pipe(catchError(() => of(null)))
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe(({ pendencias, regras }) => {
        if (pendencias) {
          this.applyPendencias(pendencias);
        }
        if (regras) {
          this.regras.set(Array.isArray(regras) ? regras : []);
        }
      });
  }

  private loadPendencias(origem: OrigemClassificacaoFinanceira | string | undefined = this.selectedOrigem() || undefined): void {
    this.api
      .getPendenciasClassificacao(origem)
      .pipe(
        catchError((error: unknown) => {
          this.setErrorFeedback(error, 'Não foi possível carregar as pendências de classificação.');
          return of(null);
        })
      )
      .subscribe((pendencias) => {
        if (pendencias) {
          this.applyPendencias(pendencias);
        }
      });
  }

  private loadRegras(): void {
    this.api
      .listRegrasClassificacao(this.selectedOrigem() || undefined)
      .pipe(catchError(() => of([] as RegraClassificacaoResponse[])))
      .subscribe((regras) => {
        this.regras.set(Array.isArray(regras) ? regras : []);
      });
  }

  private applyPendencias(response: PendenciaClassificacaoResponse | null): void {
    this.pendencias.set(response);
    if (!response?.itens) {
      return;
    }
    const nextDrafts: Record<string, PendingDraft> = {};
    const previous = this.pendingDrafts();
    for (const item of response.itens) {
      const key = this.pendingKey(item);
      nextDrafts[key] = previous[key] ?? {
        planoContaId: item.sugestaoPlanoContaId ?? null,
        centroCustoId: item.sugestaoCentroCustoId ?? null,
        motivo: ''
      };
    }
    this.pendingDrafts.set(nextDrafts);
  }

  private setSuccessFeedback(message: string): void {
    this.feedbackTone.set('success');
    this.feedbackMessage.set(message);
  }

  private setWarningFeedback(message: string): void {
    this.feedbackTone.set('warning');
    this.feedbackMessage.set(message);
  }

  private setErrorFeedback(error: unknown, fallback: string): void {
    this.feedbackTone.set('danger');
    const detail = extractErrorMessage(error);
    this.feedbackMessage.set(detail ?? fallback);
  }
}

function currentYearMonth(): string {
  return new Date().toISOString().slice(0, 7);
}

function extractErrorMessage(error: unknown): string | null {
  if (typeof error !== 'object' || error === null) {
    return null;
  }
  const status = 'status' in error ? Number((error as { status?: number }).status) : undefined;
  if (status === 401 || status === 403) {
    return 'Seu perfil não possui permissão para alterar dados da Fundação Financeira.';
  }
  const payload = (error as { error?: { detail?: string; message?: string } }).error;
  return payload?.detail ?? payload?.message ?? null;
}
