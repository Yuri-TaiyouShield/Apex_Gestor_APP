import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { ApexApiService } from '../core/apex-api.service';
import { AuthService } from '../core/auth.service';
import {
  BaixaTituloFinanceiroRequest,
  CentroCustoResponse,
  ConciliacaoItemResponse,
  ConciliacaoManualRequest,
  ConciliacaoResumoResponse,
  ContaFinanceiraRequest,
  FluxoCaixaCenarioProjecao,
  FluxoCaixaPainelResponse,
  FluxoCaixaPrevistoItem,
  FluxoCaixaQueryParams,
  FluxoCaixaSimulacaoRequest,
  GerarParcelasRequest,
  ImportacaoExtratoRequest,
  MovimentacaoFinanceiraResponse,
  PlanoContasResponse,
  StatusConciliacaoFinanceira,
  TransferenciaInternaRequest
} from '../core/models';

type CashFlowTab = 'realizado' | 'previsto' | 'projetado' | 'calendario' | 'conciliacao';

@Component({
  selector: 'app-finance-cash-flow-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="cf-shell">
      <header class="cf-hero">
        <div class="cf-hero__main">
          <div class="cf-hero__badges">
            <span class="badge badge--accent">Lote 3 • Regime de Caixa & Tesouraria</span>
            <span class="badge badge--outline">
              {{ painel()?.consolidadoEmpresa ? 'Consolidado Multiloja' : painel()?.filialNome || 'Escopo Filial Ativa' }}
            </span>
            @if (simulacaoAtiva()) {
              <span class="badge badge--simulado">Modo Simulação Ativo (Somente Leitura)</span>
            }
          </div>
          <h1>Central de Fluxo de Caixa, Liquidez e Conciliação</h1>
          <p class="cf-hero__subtitle">
            Visão integrada do caixa realizado, obrigações e recebíveis previstos, projeções de 7 a 90 dias,
            simulador de liquidez e conciliação bancária/cartões conectada à DRE Gerencial.
          </p>
        </div>

        <div class="cf-hero__actions">
          <a routerLink="/finance/dre" class="btn btn--ghost">DRE Gerencial</a>
          <a routerLink="/finance/foundation" class="btn btn--ghost">Fundação Financeira</a>
          <button type="button" class="btn btn--primary" (click)="recarregarTudo()" [disabled]="carregando()">
            {{ carregando() ? 'Atualizando...' : 'Atualizar Caixa' }}
          </button>
        </div>
      </header>

      @if (mensagemSucesso()) {
        <div class="alert alert--success" role="status">
          <span>{{ mensagemSucesso() }}</span>
          <button type="button" class="alert__close" (click)="mensagemSucesso.set(null)">×</button>
        </div>
      }

      @if (erro()) {
        <div class="alert alert--error" role="alert">
          <span>{{ erro() }}</span>
          <button type="button" class="alert__close" (click)="erro.set(null)">×</button>
        </div>
      }

      <!-- Filtros Globais -->
      <section class="cf-filters card" aria-label="Filtros do fluxo de caixa">
        <div class="cf-filters__grid">
          <label class="field">
            <span>Data Inicial</span>
            <input type="date" [(ngModel)]="filtroDataInicio" (change)="aplicarFiltros()" />
          </label>

          <label class="field">
            <span>Data Final</span>
            <input type="date" [(ngModel)]="filtroDataFim" (change)="aplicarFiltros()" />
          </label>

          <label class="field">
            <span>Conta / Caixa / Adquirente</span>
            <select [(ngModel)]="filtroContaFinanceiraId" (change)="aplicarFiltros()">
              <option [ngValue]="null">Todas as contas da empresa/filial</option>
              @for (conta of painel()?.contasFinanceiras ?? []; track conta.idContaFinanceira) {
                <option [ngValue]="conta.idContaFinanceira">
                  {{ conta.codigo }} — {{ conta.nome }} ({{ formatCurrency(conta.saldoAtual) }})
                </option>
              }
            </select>
          </label>

          <label class="field">
            <span>Centro de Custo</span>
            <select [(ngModel)]="filtroCentroCustoId" (change)="aplicarFiltros()">
              <option [ngValue]="null">Todos os centros de custo</option>
              @for (cc of centrosCusto(); track cc.idCentroCusto) {
                <option [ngValue]="cc.idCentroCusto">{{ cc.codigo }} — {{ cc.nome }}</option>
              }
            </select>
          </label>

          <label class="field field--checkbox">
            <input type="checkbox" [(ngModel)]="filtroConsolidarFiliais" (change)="aplicarFiltros()" />
            <span>Consolidar todas as filiais da empresa</span>
          </label>
        </div>
      </section>

      @if (painel(); as p) {
        <!-- Cards Executivos de Liquidez -->
        <section class="cf-kpis" aria-label="Indicadores de disponibilidade e fluxo de caixa">
          <article class="kpi-card kpi-card--highlight">
            <span class="kpi-card__label">Disponibilidade Imediata (Saldo Atual)</span>
            <strong class="kpi-card__value" [class.text-danger]="p.saldoFinalRealizado < 0">
              {{ formatCurrency(p.saldoFinalRealizado) }}
            </strong>
            <small class="kpi-card__meta">
              Saldo inicial realizado: {{ formatCurrency(p.saldoInicialRealizado) }}
            </small>
          </article>

          <article class="kpi-card">
            <span class="kpi-card__label">Entradas Realizadas no Período</span>
            <strong class="kpi-card__value text-success">{{ formatCurrency(p.entradasRealizadasPeriodo) }}</strong>
            <small class="kpi-card__meta">
              Saídas realizadas: {{ formatCurrency(p.saidasRealizadasPeriodo) }}
            </small>
          </article>

          <article class="kpi-card">
            <span class="kpi-card__label">Geração Líquida de Caixa (Realizado)</span>
            <strong
              class="kpi-card__value"
              [class.text-success]="p.geracaoLiquidaCaixaRealizado >= 0"
              [class.text-danger]="p.geracaoLiquidaCaixaRealizado < 0"
            >
              {{ formatCurrency(p.geracaoLiquidaCaixaRealizado) }}
            </strong>
            <small class="kpi-card__meta">
              Transf. internas neutras: {{ formatCurrency(p.transferenciasInternasVolume) }} • Estornos:
              {{ formatCurrency(p.totalEstornosPeriodo) }}
            </small>
          </article>

          <article class="kpi-card">
            <span class="kpi-card__label">Recebíveis Previstos (A Vencer / Vencidos)</span>
            <strong class="kpi-card__value">{{ formatCurrency(p.entradasPrevistasPendentes) }}</strong>
            <small class="kpi-card__meta text-danger">
              Inadimplência vencida: {{ formatCurrency(p.entradasPrevistasVencidas) }}
            </small>
          </article>

          <article class="kpi-card">
            <span class="kpi-card__label">Obrigações Previstas (A Vencer / Vencidas)</span>
            <strong class="kpi-card__value">{{ formatCurrency(p.saidasPrevistasPendentes) }}</strong>
            <small class="kpi-card__meta text-danger">
              Obrigações em atraso: {{ formatCurrency(p.saidasPrevistasVencidas) }}
            </small>
          </article>

          <article class="kpi-card">
            <span class="kpi-card__label">Repasses de Cartão Previstos (Líquido)</span>
            <strong class="kpi-card__value">
              {{ formatCurrency(p.diagnosticoLiquidez.repassesCartaoPendentesLiquido) }}
            </strong>
            <small class="kpi-card__meta">
              Bruto {{ formatCurrency(p.diagnosticoLiquidez.repassesCartaoPendentesBruto) }} • Taxas
              {{ formatCurrency(p.diagnosticoLiquidez.taxasCartaoPrevistas) }}
            </small>
          </article>
        </section>

        <!-- Painel de Diagnóstico de Liquidez & Conexão com DRE -->
        <section class="cf-diagnostico card" aria-label="Diagnóstico de liquidez e conexão com DRE">
          <div class="cf-diagnostico__header">
            <div>
              <span
                class="status-pill"
                [class.status-pill--saudavel]="p.diagnosticoLiquidez.nivelSaudeLiquidez === 'SAUDAVEL'"
                [class.status-pill--atencao]="p.diagnosticoLiquidez.nivelSaudeLiquidez === 'ATENCAO'"
                [class.status-pill--critico]="p.diagnosticoLiquidez.nivelSaudeLiquidez === 'CRITICO'"
              >
                Saúde de Liquidez: {{ p.diagnosticoLiquidez.nivelSaudeLiquidez }}
              </span>
              <h2>{{ p.diagnosticoLiquidez.mancheteExecutiva }}</h2>
            </div>
            <div class="cf-diagnostico__metrics">
              <div>
                <small>Entradas 30d</small>
                <strong>{{ formatCurrency(p.diagnosticoLiquidez.entradasPrevistas30Dias) }}</strong>
              </div>
              <div>
                <small>Obrigações 30d</small>
                <strong>{{ formatCurrency(p.diagnosticoLiquidez.obrigacoesPrevistas30Dias) }}</strong>
              </div>
              <div>
                <small>Inadimplência</small>
                <strong>{{ p.diagnosticoLiquidez.taxaInadimplenciaPercentual | number:'1.1-2' }}%</strong>
              </div>
              <div>
                <small>Margem Segura Investimento</small>
                <strong class="text-success">{{ formatCurrency(p.diagnosticoLiquidez.margemSegurancaInvestimento) }}</strong>
              </div>
            </div>
          </div>

          <div class="cf-diagnostico__dre-bridge">
            <div class="dre-bridge__box">
              <span>Resultado Econômico DRE (Competência)</span>
              <strong [class.text-danger]="p.diagnosticoLiquidez.resultadoEconomicoDrePeriodo < 0">
                {{ formatCurrency(p.diagnosticoLiquidez.resultadoEconomicoDrePeriodo) }}
              </strong>
            </div>
            <div class="dre-bridge__vs">×</div>
            <div class="dre-bridge__box">
              <span>Geração Operacional de Caixa (Realizado)</span>
              <strong [class.text-danger]="p.diagnosticoLiquidez.geracaoCaixaOperacionalPeriodo < 0">
                {{ formatCurrency(p.diagnosticoLiquidez.geracaoCaixaOperacionalPeriodo) }}
              </strong>
            </div>
            <p class="dre-bridge__explanation">
              {{ p.diagnosticoLiquidez.explicacaoDreVsCaixa }}
            </p>
          </div>

          <div class="cf-diagnostico__columns">
            <div>
              <h3>Alertas de Liquidez</h3>
              <ul class="clean-list">
                @for (alerta of p.diagnosticoLiquidez.alertasRiscoLiquidez; track alerta) {
                  <li>⚠️ {{ alerta }}</li>
                } @empty {
                  <li>✅ Nenhum alerta crítico de ruptura de liquidez identificado no horizonte atual.</li>
                }
              </ul>
            </div>

            <div>
              <h3>Contas que Mais Pressionam o Caixa</h3>
              <ul class="clean-list">
                @for (conta of p.diagnosticoLiquidez.contasComprometendoCaixa; track conta) {
                  <li>📌 {{ conta }}</li>
                } @empty {
                  <li>Sem concentração atípica de saídas no período.</li>
                }
              </ul>
            </div>

            <div>
              <h3>Recomendações Baseadas em Dados</h3>
              <ul class="clean-list">
                @for (rec of p.diagnosticoLiquidez.recomendacoesGerenciais; track rec) {
                  <li>💡 {{ rec }}</li>
                }
              </ul>
            </div>
          </div>
        </section>

        <!-- Navegação por Abas dos 5 Pilares -->
        <nav class="cf-tabs" role="tablist" aria-label="Visões do Fluxo de Caixa">
          <button
            type="button"
            role="tab"
            [class.active]="abaAtiva() === 'realizado'"
            (click)="abaAtiva.set('realizado')"
          >
            1. Caixa Realizado & Contas ({{ p.movimentacoesRealizadas.length }})
          </button>
          <button
            type="button"
            role="tab"
            [class.active]="abaAtiva() === 'previsto'"
            (click)="abaAtiva.set('previsto')"
          >
            2. Caixa Previsto & Parcelas ({{ p.itensPrevistos.length }})
          </button>
          <button
            type="button"
            role="tab"
            [class.active]="abaAtiva() === 'projetado'"
            (click)="abaAtiva.set('projetado')"
          >
            3. Caixa Projetado (7–90d) & Simulador
          </button>
          <button
            type="button"
            role="tab"
            [class.active]="abaAtiva() === 'calendario'"
            (click)="abaAtiva.set('calendario')"
          >
            4. Calendário Financeiro ({{ p.calendarioFinanceiro.length }} dias)
          </button>
          <button
            type="button"
            role="tab"
            [class.active]="abaAtiva() === 'conciliacao'"
            (click)="selecionarAbaConciliacao()"
          >
            5. Conciliação Bancária, Caixa e Cartões
          </button>
        </nav>

        <!-- ABA 1: CAIXA REALIZADO & CONTAS FINANCEIRAS -->
        @if (abaAtiva() === 'realizado') {
          <section class="cf-tab-panel">
            <div class="cf-grid-2">
              <div class="card">
                <div class="card__header">
                  <div>
                    <h3>Contas Financeiras, Caixas e Adquirentes</h3>
                    <p>Saldos individualizados por conta, cofre e carteira de adquirente.</p>
                  </div>
                  @if (podeEditar()) {
                    <button type="button" class="btn btn--outline" (click)="executarBootstrapContas()">
                      Inicializar Contas Padrão
                    </button>
                  }
                </div>

                <div class="table-responsive">
                  <table class="cf-table">
                    <thead>
                      <tr>
                        <th>Código</th>
                        <th>Conta</th>
                        <th>Tipo</th>
                        <th class="text-right">Saldo Inicial</th>
                        <th class="text-right">Saldo Atual</th>
                      </tr>
                    </thead>
                    <tbody>
                      @for (c of p.contasFinanceiras; track c.idContaFinanceira) {
                        <tr>
                          <td><code>{{ c.codigo }}</code></td>
                          <td>
                            <strong>{{ c.nome }}</strong>
                            @if (c.bancoNome) {
                              <small class="d-block text-muted">{{ c.bancoNome }} {{ c.numeroConta }}</small>
                            }
                          </td>
                          <td><span class="badge badge--outline">{{ c.tipoConta }}</span></td>
                          <td class="text-right">{{ formatCurrency(c.saldoInicial) }}</td>
                          <td class="text-right">
                            <strong [class.text-danger]="c.saldoAtual < 0">{{ formatCurrency(c.saldoAtual) }}</strong>
                          </td>
                        </tr>
                      } @empty {
                        <tr>
                          <td colspan="5" class="text-center text-muted">Nenhuma conta financeira cadastrada.</td>
                        </tr>
                      }
                    </tbody>
                  </table>
                </div>

                @if (podeEditar()) {
                  <form class="cf-inline-form" (ngSubmit)="criarNovaContaFinanceira()">
                    <h4>Nova Conta Financeira</h4>
                    <div class="cf-form-row">
                      <input
                        type="text"
                        placeholder="Código (ex.: BANCO-ITAU)"
                        [(ngModel)]="novaContaForm.codigo"
                        name="contaCodigo"
                        required
                      />
                      <input
                        type="text"
                        placeholder="Nome da Conta"
                        [(ngModel)]="novaContaForm.nome"
                        name="contaNome"
                        required
                      />
                      <select [(ngModel)]="novaContaForm.tipoConta" name="contaTipo">
                        <option value="CONTA_CORRENTE">Conta Corrente</option>
                        <option value="CAIXA_FISICO">Caixa Físico</option>
                        <option value="CONTA_INVESTIMENTO">Conta Investimento</option>
                        <option value="CARTEIRA_DIGITAL">Carteira Digital</option>
                        <option value="OPERADORA_CARTAO">Operadora / Adquirente Cartão</option>
                      </select>
                      <input
                        type="number"
                        step="0.01"
                        placeholder="Saldo Inicial (R$)"
                        [(ngModel)]="novaContaForm.saldoInicial"
                        name="contaSaldoInicial"
                      />
                      <button type="submit" class="btn btn--primary">Adicionar Conta</button>
                    </div>
                  </form>
                }
              </div>

              @if (podeEditar()) {
                <div class="card">
                  <div class="card__header">
                    <div>
                      <h3>Transferência entre Contas Próprias</h3>
                      <p>
                        Movimenta saldo entre caixa físico e bancos sem inflar receitas ou despesas operacionais da
                        empresa.
                      </p>
                    </div>
                  </div>

                  <form class="cf-stack-form" (ngSubmit)="realizarTransferencia()">
                    <div class="cf-form-row">
                      <label class="field">
                        <span>Conta de Origem (Saída)</span>
                        <select [(ngModel)]="transferenciaForm.contaOrigemId" name="contaOrigemId" required>
                          <option [ngValue]="0">Selecione...</option>
                          @for (c of p.contasFinanceiras; track c.idContaFinanceira) {
                            <option [ngValue]="c.idContaFinanceira">{{ c.nome }} ({{ formatCurrency(c.saldoAtual) }})</option>
                          }
                        </select>
                      </label>

                      <label class="field">
                        <span>Conta de Destino (Entrada)</span>
                        <select [(ngModel)]="transferenciaForm.contaDestinoId" name="contaDestinoId" required>
                          <option [ngValue]="0">Selecione...</option>
                          @for (c of p.contasFinanceiras; track c.idContaFinanceira) {
                            <option [ngValue]="c.idContaFinanceira">{{ c.nome }} ({{ formatCurrency(c.saldoAtual) }})</option>
                          }
                        </select>
                      </label>
                    </div>

                    <div class="cf-form-row">
                      <label class="field">
                        <span>Valor (R$)</span>
                        <input
                          type="number"
                          step="0.01"
                          min="0.01"
                          [(ngModel)]="transferenciaForm.valor"
                          name="valorTransf"
                          required
                        />
                      </label>

                      <label class="field">
                        <span>Data da Transferência</span>
                        <input type="date" [(ngModel)]="transferenciaForm.dataTransferencia" name="dataTransf" />
                      </label>

                      <label class="field">
                        <span>Documento / Comprovante</span>
                        <input
                          type="text"
                          placeholder="Ex.: TED-001"
                          [(ngModel)]="transferenciaForm.documentoReferencia"
                          name="docTransf"
                        />
                      </label>
                    </div>

                    <label class="field">
                      <span>Descrição / Observação</span>
                      <input
                        type="text"
                        placeholder="Ex.: Depósito de sangria do caixa da loja no banco"
                        [(ngModel)]="transferenciaForm.descricao"
                        name="descTransf"
                      />
                    </label>

                    <button type="submit" class="btn btn--primary">Registrar Transferência Interna</button>
                  </form>
                </div>
              }
            </div>

            <!-- Tabela de Movimentações Realizadas -->
            <div class="card">
              <div class="card__header">
                <div>
                  <h3>Extrato Consolidado de Movimentações Realizadas</h3>
                  <p>Entradas, saídas, liquidações de cartão, transferências internas e estornos auditáveis.</p>
                </div>
              </div>

              <div class="table-responsive">
                <table class="cf-table">
                  <thead>
                    <tr>
                      <th>Data</th>
                      <th>Conta</th>
                      <th>Tipo / Origem</th>
                      <th>Descrição / Documento</th>
                      <th>Plano de Contas</th>
                      <th class="text-right">Bruto</th>
                      <th class="text-right">Taxa</th>
                      <th class="text-right">Líquido</th>
                      <th>Ações</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (mov of p.movimentacoesRealizadas; track mov.idMovimentacaoFinanceira) {
                      <tr [class.row-estornado]="mov.estornado">
                        <td>{{ mov.dataMovimentacao }}</td>
                        <td>{{ mov.contaFinanceiraNome }}</td>
                        <td>
                          <span
                            class="badge"
                            [class.badge--success]="mov.tipoMovimentacao === 'ENTRADA'"
                            [class.badge--danger]="mov.tipoMovimentacao === 'SAIDA'"
                          >
                            {{ mov.tipoMovimentacao }}
                          </span>
                          <small class="d-block text-muted">{{ mov.origemTipo }}</small>
                        </td>
                        <td>
                          <strong>{{ mov.descricao }}</strong>
                          @if (mov.documentoReferencia) {
                            <small class="d-block text-muted">Ref: {{ mov.documentoReferencia }}</small>
                          }
                        </td>
                        <td>
                          @if (mov.planoContaNome) {
                            {{ mov.planoContaNome }}
                          } @else {
                            <span class="text-muted">Neutro / Transferência</span>
                          }
                        </td>
                        <td class="text-right">{{ formatCurrency(mov.valorBruto) }}</td>
                        <td class="text-right">{{ formatCurrency(mov.valorTaxa) }}</td>
                        <td class="text-right">
                          <strong>{{ formatCurrency(mov.valorLiquido) }}</strong>
                        </td>
                        <td>
                          @if (mov.estornado) {
                            <span class="badge badge--outline">Estornado</span>
                          } @else if (podeEditar() && mov.idMovimentacaoFinanceira > 0 && mov.tipoMovimentacao !== 'ESTORNO') {
                            <button
                              type="button"
                              class="btn btn--small btn--ghost"
                              (click)="estornarMovimentacao(mov)"
                            >
                              Estornar
                            </button>
                          } @else {
                            <span class="text-muted">—</span>
                          }
                        </td>
                      </tr>
                    } @empty {
                      <tr>
                        <td colspan="9" class="text-center text-muted">
                          Nenhuma movimentação realizada no período selecionado.
                        </td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        }

        <!-- ABA 2: CAIXA PREVISTO (RECEBÍVEIS, OBRIGAÇÕES, PARCELAS E CARTÕES) -->
        @if (abaAtiva() === 'previsto') {
          <section class="cf-tab-panel">
            @if (podeEditar()) {
              <div class="cf-grid-2">
                <!-- Gerador de Parcelas / Compromissos Recorrentes -->
                <div class="card">
                  <div class="card__header">
                    <div>
                      <h3>Gerar Parcelas ou Compromissos Recorrentes</h3>
                      <p>Gera títulos a pagar ou a receber (incluindo cartões com taxa e prazo de repasse).</p>
                    </div>
                  </div>

                  <form class="cf-stack-form" (ngSubmit)="gerarParcelas()">
                    <div class="cf-form-row">
                      <label class="field">
                        <span>Natureza</span>
                        <select [(ngModel)]="parcelasForm.natureza" name="parcNatureza">
                          <option value="RECEBER">Contas a Receber (Entrada Futura)</option>
                          <option value="PAGAR">Contas a Pagar (Obrigação Futura)</option>
                        </select>
                      </label>

                      <label class="field">
                        <span>Modalidade / Meio</span>
                        <select [(ngModel)]="parcelasForm.modalidadeRecebivel" name="parcModalidade">
                          <option value="BOLETO">Boleto</option>
                          <option value="CARTAO_CREDITO">Cartão de Crédito</option>
                          <option value="CARTAO_DEBITO">Cartão de Débito</option>
                          <option value="PIX">PIX</option>
                          <option value="CREDIARIO">Crediário Loja</option>
                          <option value="DIRETO">Direto / Carteira</option>
                        </select>
                      </label>

                      <label class="field">
                        <span>Intervalo (Dias)</span>
                        <input
                          type="number"
                          min="1"
                          [(ngModel)]="parcelasForm.intervaloDias"
                          name="parcIntervalo"
                          required
                        />
                      </label>
                    </div>

                    <div class="cf-form-row">
                      <label class="field">
                        <span>Descrição</span>
                        <input
                          type="text"
                          placeholder="Ex.: Compra fornecedor / Venda parcelada"
                          [(ngModel)]="parcelasForm.descricao"
                          name="parcDesc"
                          required
                        />
                      </label>

                      <label class="field">
                        <span>Valor Total (R$)</span>
                        <input
                          type="number"
                          step="0.01"
                          min="0.01"
                          [(ngModel)]="parcelasForm.valorTotal"
                          name="parcValorTotal"
                          required
                        />
                      </label>

                      <label class="field">
                        <span>Nº Parcelas</span>
                        <input
                          type="number"
                          min="1"
                          max="60"
                          [(ngModel)]="parcelasForm.quantidadeParcelas"
                          name="parcQtd"
                          required
                        />
                      </label>
                    </div>

                    <div class="cf-form-row">
                      <label class="field">
                        <span>1º Vencimento</span>
                        <input
                          type="date"
                          [(ngModel)]="parcelasForm.primeiroVencimento"
                          name="parcPrimeiroVenc"
                          required
                        />
                      </label>

                      <label class="field">
                        <span>Taxa Operadora (%)</span>
                        <input
                          type="number"
                          step="0.01"
                          min="0"
                          [(ngModel)]="parcelasForm.taxaPercentualCartao"
                          name="parcTaxa"
                        />
                      </label>

                      <label class="field">
                        <span>Conta Prevista</span>
                        <select [(ngModel)]="parcelasForm.contaFinanceiraId" name="parcContaId">
                          <option [ngValue]="null">Conta padrão</option>
                          @for (c of p.contasFinanceiras; track c.idContaFinanceira) {
                            <option [ngValue]="c.idContaFinanceira">{{ c.nome }}</option>
                          }
                        </select>
                      </label>
                    </div>

                    <button type="submit" class="btn btn--primary">Gerar Títulos Parcelados</button>
                  </form>
                </div>

                <!-- Formulário de Baixa Parcial / Integral / Antecipação -->
                <div class="card">
                  <div class="card__header">
                    <div>
                      <h3>Baixa de Título (Parcial, Integral ou Antecipação de Cartão)</h3>
                      <p>
                        @if (itemSelecionadoBaixa(); as sel) {
                          Selecionado: <strong>#{{ sel.registroId }}</strong> — {{ sel.descricao }} (Saldo líquido:
                          {{ formatCurrency(sel.saldoAbertoLiquido) }})
                        } @else {
                          Selecione um título na tabela abaixo em "Baixar / Liquidar" para registrar o pagamento ou
                          recebimento.
                        }
                      </p>
                    </div>
                  </div>

                  @if (itemSelecionadoBaixa(); as sel) {
                    <form class="cf-stack-form" (ngSubmit)="confirmarBaixaTitulo()">
                      <div class="cf-form-row">
                        <label class="field">
                          <span>Valor Bruto a Liquidar (R$)</span>
                          <input
                            type="number"
                            step="0.01"
                            min="0.01"
                            [max]="sel.valorBruto"
                            [(ngModel)]="baixaForm.valorLiquidado"
                            name="baixaValor"
                            required
                          />
                        </label>

                        <label class="field">
                          <span>Data da Liquidação</span>
                          <input type="date" [(ngModel)]="baixaForm.dataLiquidacao" name="baixaData" required />
                        </label>

                        <label class="field">
                          <span>Conta Financeira</span>
                          <select [(ngModel)]="baixaForm.contaFinanceiraId" name="baixaConta">
                            @for (c of p.contasFinanceiras; track c.idContaFinanceira) {
                              <option [ngValue]="c.idContaFinanceira">{{ c.nome }}</option>
                            }
                          </select>
                        </label>
                      </div>

                      @if (sel.origemTipo === 'CONTA_RECEBER' || sel.origemTipo === 'REPASSE_CARTAO') {
                        <div class="cf-form-row">
                          <label class="field">
                            <span>Taxa da Operadora (R$)</span>
                            <input
                              type="number"
                              step="0.01"
                              min="0"
                              [(ngModel)]="baixaForm.valorTaxa"
                              name="baixaTaxaOp"
                            />
                          </label>

                          <label class="field">
                            <span>Taxa de Antecipação (R$)</span>
                            <input
                              type="number"
                              step="0.01"
                              min="0"
                              [(ngModel)]="baixaForm.taxaAntecipacao"
                              name="baixaTaxaAnt"
                            />
                          </label>

                          <label class="field field--checkbox">
                            <input
                              type="checkbox"
                              [(ngModel)]="baixaForm.antecipacaoRecebivel"
                              name="baixaAntecipacao"
                            />
                            <span>Antecipação de recebível</span>
                          </label>
                        </div>
                      }

                      <div class="cf-form-actions">
                        <button type="submit" class="btn btn--primary">Confirmar Liquidação no Caixa</button>
                        <button type="button" class="btn btn--ghost" (click)="itemSelecionadoBaixa.set(null)">
                          Cancelar
                        </button>
                      </div>
                    </form>
                  }
                </div>
              </div>
            }

            <!-- Filtros Rápidos do Caixa Previsto -->
            <div class="card">
              <div class="card__header">
                <div>
                  <h3>Títulos Abertos, Parcelas, Cartões e Obrigações Futuras</h3>
                  <p>Separação clara entre títulos a vencer, vencendo hoje e inadimplentes/vencidos.</p>
                </div>

                <div class="cf-inline-filters">
                  <select [ngModel]="filtroNaturezaPrevisto()" (ngModelChange)="filtroNaturezaPrevisto.set($event)">
                    <option value="TODOS">Entradas e Saídas</option>
                    <option value="ENTRADA">Apenas Entradas (Recebíveis)</option>
                    <option value="SAIDA">Apenas Saídas (Obrigações)</option>
                  </select>

                  <select [ngModel]="filtroPrazoPrevisto()" (ngModelChange)="filtroPrazoPrevisto.set($event)">
                    <option value="TODOS">Todos os Prazos</option>
                    <option value="A_VENCER">A Vencer / Vence Hoje</option>
                    <option value="VENCIDO">Apenas Vencidos (Atraso)</option>
                  </select>
                </div>
              </div>

              <div class="table-responsive">
                <table class="cf-table">
                  <thead>
                    <tr>
                      <th>Previsão Caixa</th>
                      <th>Situação</th>
                      <th>Natureza / Origem</th>
                      <th>Parcela</th>
                      <th>Descrição / Parte</th>
                      <th>Modalidade</th>
                      <th class="text-right">Bruto</th>
                      <th class="text-right">Pago</th>
                      <th class="text-right">Taxa Prev.</th>
                      <th class="text-right">Saldo Líquido</th>
                      <th>Ação</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (item of itensPrevistosFiltrados(); track item.origemTipo + '-' + item.registroId + '-' + item.dataPrevistaLiquidacao) {
                      <tr>
                        <td>
                          <strong>{{ item.dataPrevistaLiquidacao }}</strong>
                          <small class="d-block text-muted">Venc: {{ item.dataVencimento }}</small>
                        </td>
                        <td>
                          <span
                            class="badge"
                            [class.badge--danger]="item.situacaoVencimento === 'VENCIDO'"
                            [class.badge--warning]="item.situacaoVencimento === 'VENCE_HOJE'"
                            [class.badge--outline]="item.situacaoVencimento === 'A_VENCER'"
                          >
                            {{ item.situacaoVencimento }}
                          </span>
                        </td>
                        <td>
                          <span
                            class="badge"
                            [class.badge--success]="item.natureza === 'ENTRADA'"
                            [class.badge--danger]="item.natureza === 'SAIDA'"
                          >
                            {{ item.natureza }}
                          </span>
                          <small class="d-block text-muted">{{ item.origemTipo }}</small>
                        </td>
                        <td>
                          <code>#{{ item.registroId }}</code>
                          <small class="d-block text-muted">
                            Parcela {{ item.numeroParcela }}/{{ item.totalParcelas }}
                            @if (item.recorrente) {
                              • Recorrente
                            }
                          </small>
                        </td>
                        <td>
                          <strong>{{ item.descricao }}</strong>
                          @if (item.parteRelacionada) {
                            <small class="d-block text-muted">{{ item.parteRelacionada }}</small>
                          }
                        </td>
                        <td>{{ item.modalidade }}</td>
                        <td class="text-right">{{ formatCurrency(item.valorBruto) }}</td>
                        <td class="text-right">{{ formatCurrency(item.valorPagoAcumulado) }}</td>
                        <td class="text-right">{{ formatCurrency(item.valorTaxaPrevista) }}</td>
                        <td class="text-right">
                          <strong>{{ formatCurrency(item.saldoAbertoLiquido) }}</strong>
                        </td>
                        <td>
                          @if (
                            podeEditar() &&
                            (item.origemTipo === 'CONTA_RECEBER' ||
                              item.origemTipo === 'REPASSE_CARTAO' ||
                              item.origemTipo === 'CONTA_PAGAR') &&
                            item.registroId > 0
                          ) {
                            <button
                              type="button"
                              class="btn btn--small btn--outline"
                              (click)="prepararBaixaTitulo(item)"
                            >
                              Baixar / Liquidar
                            </button>
                          } @else {
                            <span class="text-muted">Projeção</span>
                          }
                        </td>
                      </tr>
                    } @empty {
                      <tr>
                        <td colspan="11" class="text-center text-muted">
                          Nenhum compromisso ou recebível previsto para o filtro selecionado.
                        </td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        }

        <!-- ABA 3: CAIXA PROJETADO (7, 15, 30, 60, 90 DIAS) & SIMULADOR DE LIQUIDEZ -->
        @if (abaAtiva() === 'projetado') {
          <section class="cf-tab-panel">
            <!-- Simulador de Liquidez -->
            <div class="card">
              <div class="card__header">
                <div>
                  <h3>Simulador de Liquidez e Disponibilidade (Sem Alterar o Caixa Realizado)</h3>
                  <p>
                    Simule o impacto de uma nova despesa, investimento, entrada adicional ou antecipação de
                    recebíveis de cartão sobre os cenários de 7 a 90 dias.
                  </p>
                </div>
                @if (simulacaoAtiva()) {
                  <button type="button" class="btn btn--outline" (click)="limparSimulacao()">
                    Sair do Modo Simulação
                  </button>
                }
              </div>

              <form class="cf-stack-form" (ngSubmit)="executarSimulacao()">
                <div class="cf-form-row">
                  <label class="field">
                    <span>Saída / Despesa Simulada (R$)</span>
                    <input
                      type="number"
                      step="0.01"
                      min="0"
                      [(ngModel)]="simulacaoForm.saidaSimuladaAdicional"
                      name="simDespValor"
                    />
                  </label>

                  <label class="field">
                    <span>Investimento Planejado (R$)</span>
                    <input
                      type="number"
                      step="0.01"
                      min="0"
                      [(ngModel)]="simulacaoForm.investimentoPlanejado"
                      name="simInvValor"
                    />
                  </label>

                  <label class="field">
                    <span>Entrada Simulada Adicional (R$)</span>
                    <input
                      type="number"
                      step="0.01"
                      min="0"
                      [(ngModel)]="simulacaoForm.entradaSimuladaAdicional"
                      name="simEntValor"
                    />
                  </label>

                  <label class="field">
                    <span>Data Base da Simulação</span>
                    <input type="date" [(ngModel)]="simulacaoForm.dataSimulacao" name="simData" />
                  </label>
                </div>

                <div class="cf-form-row">
                  <label class="field">
                    <span>Taxa de Antecipação Simulada (%)</span>
                    <input
                      type="number"
                      step="0.1"
                      min="0"
                      max="30"
                      [(ngModel)]="simulacaoForm.taxaAntecipacaoSimuladaPercentual"
                      name="simTaxaAnt"
                    />
                  </label>

                  <label class="field field--checkbox">
                    <input
                      type="checkbox"
                      [(ngModel)]="simulacaoForm.simularAntecipacaoCartao"
                      name="simAnteciparCartao"
                    />
                    <span>Simular antecipação de todos os recebíveis de cartão abertos</span>
                  </label>

                  <button type="submit" class="btn btn--primary">Simular Impacto no Caixa</button>
                </div>
              </form>
            </div>

            <!-- Tabela Comparativa de Cenários 7, 15, 30, 60, 90 dias -->
            <div class="card">
              <div class="card__header">
                <div>
                  <h3>Projeção de Liquidez por Horizonte (7, 15, 30, 60 e 90 dias) e Cenário</h3>
                  <p>
                    Comparação entre os cenários <strong>Otimista</strong>, <strong>Realista</strong> e
                    <strong>Pessimista</strong>.
                  </p>
                </div>

                <div class="cf-inline-filters">
                  <select [ngModel]="filtroCenarioProjecao()" (ngModelChange)="filtroCenarioProjecao.set($event)">
                    <option value="TODOS">Todos os Cenários</option>
                    <option value="REALISTA">Cenário Realista</option>
                    <option value="OTIMISTA">Cenário Otimista</option>
                    <option value="PESSIMISTA">Cenário Pessimista</option>
                  </select>
                </div>
              </div>

              <div class="table-responsive">
                <table class="cf-table">
                  <thead>
                    <tr>
                      <th>Cenário</th>
                      <th class="text-right">Partida</th>
                      <th class="text-right">7 dias</th>
                      <th class="text-right">15 dias</th>
                      <th class="text-right">30 dias</th>
                      <th class="text-right">60 dias</th>
                      <th class="text-right">90 dias</th>
                      <th class="text-right">Menor Saldo (Vale)</th>
                      <th class="text-right">Capital de Giro Adicional</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (c of cenariosFiltrados(); track c.codigoCenario) {
                      <tr [class.row-risco]="c.riscoInsuficienciaCaixa">
                        <td>
                          <span
                            class="badge"
                            [class.badge--success]="c.codigoCenario === 'OTIMISTA'"
                            [class.badge--accent]="c.codigoCenario === 'REALISTA'"
                            [class.badge--warning]="c.codigoCenario === 'PESSIMISTA'"
                          >
                            {{ c.nomeCenario }}
                          </span>
                          <small class="d-block text-muted">{{ c.premissaExplicacao }}</small>
                        </td>
                        <td class="text-right">{{ formatCurrency(c.saldoAtualPartida) }}</td>
                        <td class="text-right" [class.text-danger]="c.saldoProjetado7Dias < 0">
                          {{ formatCurrency(c.saldoProjetado7Dias) }}
                        </td>
                        <td class="text-right" [class.text-danger]="c.saldoProjetado15Dias < 0">
                          {{ formatCurrency(c.saldoProjetado15Dias) }}
                        </td>
                        <td class="text-right">
                          <strong [class.text-danger]="c.saldoProjetado30Dias < 0">
                            {{ formatCurrency(c.saldoProjetado30Dias) }}
                          </strong>
                        </td>
                        <td class="text-right" [class.text-danger]="c.saldoProjetado60Dias < 0">
                          {{ formatCurrency(c.saldoProjetado60Dias) }}
                        </td>
                        <td class="text-right" [class.text-danger]="c.saldoProjetado90Dias < 0">
                          {{ formatCurrency(c.saldoProjetado90Dias) }}
                        </td>
                        <td class="text-right">
                          <span [class.text-danger]="c.menorSaldoProjetado < 0">
                            {{ formatCurrency(c.menorSaldoProjetado) }}
                          </span>
                          @if (c.dataMenorSaldo) {
                            <small class="d-block text-muted">em {{ c.dataMenorSaldo }}</small>
                          }
                        </td>
                        <td class="text-right">
                          @if (c.necessidadeCapitalGiroAdicional > 0) {
                            <strong class="text-danger">
                              {{ formatCurrency(c.necessidadeCapitalGiroAdicional) }}
                            </strong>
                          } @else {
                            <span class="text-success">R$ 0,00 (Suficiente)</span>
                          }
                        </td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        }

        <!-- ABA 4: CALENDÁRIO FINANCEIRO DIÁRIO -->
        @if (abaAtiva() === 'calendario') {
          <section class="cf-tab-panel">
            <div class="card">
              <div class="card__header">
                <div>
                  <h3>Calendário Diário de Disponibilidade (Realizado vs Previsto/Projetado)</h3>
                  <p>Evolução diária do saldo acumulado com identificação automática de dias com risco de descoberto.</p>
                </div>
              </div>

              <div class="table-responsive">
                <table class="cf-table">
                  <thead>
                    <tr>
                      <th>Data</th>
                      <th>Natureza do Dia</th>
                      <th class="text-right">Entradas Realizadas</th>
                      <th class="text-right">Saídas Realizadas</th>
                      <th class="text-right">Entradas Previstas</th>
                      <th class="text-right">Saídas Previstas</th>
                      <th class="text-right">Saldo Líquido Dia</th>
                      <th class="text-right">Saldo Acumulado</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (dia of p.calendarioFinanceiro; track dia.data) {
                      <tr [class.row-risco]="dia.riscoSaldoNegativo">
                        <td><strong>{{ dia.data }}</strong></td>
                        <td>
                          <span class="badge badge--outline">{{ dia.tipoDado }}</span>
                        </td>
                        <td class="text-right text-success">{{ formatCurrency(dia.entradasRealizadas) }}</td>
                        <td class="text-right text-danger">{{ formatCurrency(dia.saidasRealizadas) }}</td>
                        <td class="text-right">{{ formatCurrency(dia.entradasPrevistas) }}</td>
                        <td class="text-right">{{ formatCurrency(dia.saidasPrevistas) }}</td>
                        <td
                          class="text-right"
                          [class.text-success]="dia.saldoLiquidoDia >= 0"
                          [class.text-danger]="dia.saldoLiquidoDia < 0"
                        >
                          {{ formatCurrency(dia.saldoLiquidoDia) }}
                        </td>
                        <td class="text-right">
                          <strong [class.text-danger]="dia.riscoSaldoNegativo">
                            {{ formatCurrency(dia.saldoAcumuladoProjetado) }}
                          </strong>
                        </td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        }

        <!-- ABA 5: CONCILIAÇÃO FINANCEIRA (BANCOS OFX/CSV, CAIXA E CARTÕES) -->
        @if (abaAtiva() === 'conciliacao') {
          <section class="cf-tab-panel">
            @if (conciliacao(); as conc) {
              <div class="cf-kpis">
                <article class="kpi-card">
                  <span class="kpi-card__label">Conciliados Exatos</span>
                  <strong class="kpi-card__value text-success">{{ conc.conciliadosExatos }}</strong>
                  <small class="kpi-card__meta">Manuais: {{ conc.conciliadosManuais }}</small>
                </article>

                <article class="kpi-card">
                  <span class="kpi-card__label">Pendentes de Vínculo</span>
                  <strong class="kpi-card__value">{{ conc.pendentes }}</strong>
                  <small class="kpi-card__meta">Total itens: {{ conc.totalItensExtrato }}</small>
                </article>

                <article class="kpi-card">
                  <span class="kpi-card__label">Divergências de Valor / Taxa</span>
                  <strong class="kpi-card__value text-danger">{{ conc.divergentes }}</strong>
                  <small class="kpi-card__meta">
                    Diferença acumulada: {{ formatCurrency(conc.valorTotalDivergente) }}
                  </small>
                </article>

                <article class="kpi-card">
                  <span class="kpi-card__label">Duplicidades Suspeitas</span>
                  <strong class="kpi-card__value text-danger">{{ conc.duplicidadesSuspeitas }}</strong>
                  <small class="kpi-card__meta">
                    Sessões PDV c/ diferença: {{ conc.sessoesCaixaComDiferencaFechamento }} ({{
                      formatCurrency(conc.diferencaTotalFechamentoCaixa)
                    }})
                  </small>
                </article>
              </div>
            }

            @if (podeEditar()) {
              <div class="card">
                <div class="card__header">
                  <div>
                    <h3>Importar Extrato Bancário ou Borderô de Cartão (OFX / CSV)</h3>
                    <p>
                      Pareamento automático por data, valor e identificador externo, com detecção de divergência de
                      taxa e duplicidade suspeita.
                    </p>
                  </div>
                </div>

                <form class="cf-stack-form" (ngSubmit)="importarExtrato()">
                  <div class="cf-form-row">
                    <label class="field">
                      <span>Conta Financeira</span>
                      <select [(ngModel)]="importacaoForm.contaFinanceiraId" name="impContaId" required>
                        @for (c of p.contasFinanceiras; track c.idContaFinanceira) {
                          <option [ngValue]="c.idContaFinanceira">{{ c.nome }}</option>
                        }
                      </select>
                    </label>

                    <label class="field">
                      <span>Tipo de Conciliação</span>
                      <select [(ngModel)]="importacaoForm.tipoConciliacao" name="impTipo">
                        <option value="BANCARIA">Conciliação Bancária</option>
                        <option value="CARTAO">Conciliação de Adquirente / Cartão</option>
                        <option value="CAIXA">Conciliação de Caixa</option>
                      </select>
                    </label>

                    <label class="field">
                      <span>Formato do Extrato</span>
                      <select [(ngModel)]="importacaoForm.formatoOrigem" name="impFormato">
                        <option value="CSV">CSV (data;descricao;valor;natureza;id)</option>
                        <option value="OFX">OFX (Open Financial Exchange)</option>
                      </select>
                    </label>
                  </div>

                  <label class="field">
                    <span>Conteúdo do Extrato (CSV ou OFX)</span>
                    <textarea
                      rows="4"
                      placeholder="Exemplo CSV: 2026-11-17;PIX RECEBIDO PEDIDO 101;350.00;CREDITO;FIT-001"
                      [(ngModel)]="importacaoForm.conteudoArquivo"
                      name="impConteudo"
                      required
                    ></textarea>
                  </label>

                  <button type="submit" class="btn btn--primary">Processar e Conciliar Extrato</button>
                </form>
              </div>
            }

            <div class="card">
              <div class="card__header">
                <div>
                  <h3>Itens de Conciliação</h3>
                  <p>Histórico de itens importados e status de conferência com os lançamentos do sistema.</p>
                </div>
              </div>

              <div class="table-responsive">
                <table class="cf-table">
                  <thead>
                    <tr>
                      <th>Data</th>
                      <th>Conta / Tipo</th>
                      <th>ID Extrato</th>
                      <th>Descrição no Extrato</th>
                      <th>Natureza</th>
                      <th class="text-right">Valor Extrato</th>
                      <th class="text-right">Diferença</th>
                      <th>Status</th>
                      <th>Ações</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (item of conciliacao()?.itens ?? []; track item.idConciliacaoItem) {
                      <tr>
                        <td>{{ item.dataExtrato }}</td>
                        <td>
                          <strong>{{ item.contaFinanceiraNome }}</strong>
                          <small class="d-block text-muted">{{ item.tipoConciliacao }}</small>
                        </td>
                        <td><code>{{ item.identificadorExtrato || '—' }}</code></td>
                        <td>
                          <strong>{{ item.descricaoExtrato }}</strong>
                          @if (item.observacao) {
                            <small class="d-block text-muted">{{ item.observacao }}</small>
                          }
                        </td>
                        <td>{{ item.naturezaExtrato }}</td>
                        <td class="text-right">{{ formatCurrency(item.valorExtrato) }}</td>
                        <td class="text-right" [class.text-danger]="item.diferencaValor !== 0">
                          {{ formatCurrency(item.diferencaValor) }}
                        </td>
                        <td>
                          <span
                            class="badge"
                            [class.badge--success]="
                              item.statusConciliacao === 'CONCILIADO_EXATO' ||
                              item.statusConciliacao === 'CONCILIADO_MANUAL'
                            "
                            [class.badge--danger]="
                              item.statusConciliacao === 'DIVERGENTE' ||
                              item.statusConciliacao === 'DUPLICIDADE_SUSPEITA'
                            "
                            [class.badge--warning]="item.statusConciliacao === 'PENDENTE'"
                          >
                            {{ item.statusConciliacao }}
                          </span>
                        </td>
                        <td>
                          @if (
                            podeEditar() &&
                            item.statusConciliacao !== 'CONCILIADO_EXATO' &&
                            item.statusConciliacao !== 'CONCILIADO_MANUAL'
                          ) {
                            <div class="cf-btn-group">
                              <button
                                type="button"
                                class="btn btn--small btn--outline"
                                (click)="resolverConciliacaoManual(item, 'CONCILIADO_MANUAL')"
                              >
                                Conciliar
                              </button>
                              <button
                                type="button"
                                class="btn btn--small btn--ghost"
                                (click)="resolverConciliacaoManual(item, 'IGNORADO')"
                              >
                                Ignorar
                              </button>
                            </div>
                          } @else {
                            <span class="text-muted">{{ item.conciliadoPor || 'Automático' }}</span>
                          }
                        </td>
                      </tr>
                    } @empty {
                      <tr>
                        <td colspan="9" class="text-center text-muted">
                          Nenhum item de conciliação importado até o momento.
                        </td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        }
      }
    </div>
  `,
  styles: [
    `
      .cf-shell {
        display: flex;
        flex-direction: column;
        gap: 1.25rem;
        padding: 1.5rem;
        max-width: 1480px;
        margin: 0 auto;
      }
      .cf-hero {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 1rem;
        flex-wrap: wrap;
        background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
        color: #f8fafc;
        padding: 1.5rem;
        border-radius: 1rem;
      }
      .cf-hero__badges {
        display: flex;
        gap: 0.5rem;
        flex-wrap: wrap;
        margin-bottom: 0.5rem;
      }
      .cf-hero h1 {
        margin: 0 0 0.35rem;
        font-size: 1.5rem;
      }
      .cf-hero__subtitle {
        margin: 0;
        color: #cbd5e1;
        max-width: 820px;
        font-size: 0.92rem;
      }
      .cf-hero__actions {
        display: flex;
        gap: 0.5rem;
        flex-wrap: wrap;
      }
      .card {
        background: #ffffff;
        border: 1px solid #e2e8f0;
        border-radius: 0.85rem;
        padding: 1.25rem;
        box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
      }
      .card__header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 1rem;
        flex-wrap: wrap;
        margin-bottom: 1rem;
      }
      .card__header h3 {
        margin: 0 0 0.25rem;
        font-size: 1.05rem;
      }
      .card__header p {
        margin: 0;
        color: #64748b;
        font-size: 0.85rem;
      }
      .cf-filters__grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
        gap: 0.85rem;
        align-items: end;
      }
      .field {
        display: flex;
        flex-direction: column;
        gap: 0.35rem;
        font-size: 0.84rem;
        color: #334155;
      }
      .field input,
      .field select,
      .field textarea {
        padding: 0.5rem 0.65rem;
        border: 1px solid #cbd5e1;
        border-radius: 0.5rem;
        font-size: 0.88rem;
      }
      .field--checkbox {
        flex-direction: row;
        align-items: center;
        gap: 0.5rem;
        padding-bottom: 0.35rem;
      }
      .cf-kpis {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
        gap: 0.85rem;
      }
      .kpi-card {
        background: #ffffff;
        border: 1px solid #e2e8f0;
        border-radius: 0.85rem;
        padding: 1rem;
        display: flex;
        flex-direction: column;
        gap: 0.3rem;
      }
      .kpi-card--highlight {
        border-color: #0ea5e9;
        background: #f0f9ff;
      }
      .kpi-card__label {
        font-size: 0.78rem;
        color: #64748b;
        text-transform: uppercase;
        letter-spacing: 0.03em;
      }
      .kpi-card__value {
        font-size: 1.3rem;
        color: #0f172a;
      }
      .kpi-card__meta {
        font-size: 0.78rem;
        color: #64748b;
      }
      .cf-diagnostico__header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 1rem;
        flex-wrap: wrap;
        margin-bottom: 1rem;
      }
      .cf-diagnostico__header h2 {
        margin: 0.4rem 0 0;
        font-size: 1.15rem;
      }
      .cf-diagnostico__metrics {
        display: flex;
        gap: 1.25rem;
        flex-wrap: wrap;
      }
      .cf-diagnostico__metrics div {
        display: flex;
        flex-direction: column;
        background: #f8fafc;
        padding: 0.5rem 0.85rem;
        border-radius: 0.5rem;
        border: 1px solid #e2e8f0;
      }
      .cf-diagnostico__dre-bridge {
        display: flex;
        align-items: center;
        gap: 1rem;
        flex-wrap: wrap;
        background: #f8fafc;
        padding: 0.85rem 1rem;
        border-radius: 0.65rem;
        margin-bottom: 1rem;
      }
      .dre-bridge__box {
        display: flex;
        flex-direction: column;
      }
      .dre-bridge__explanation {
        margin: 0;
        flex: 1;
        min-width: 260px;
        font-size: 0.86rem;
        color: #334155;
      }
      .cf-diagnostico__columns {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
        gap: 1rem;
      }
      .cf-diagnostico__columns h3 {
        font-size: 0.92rem;
        margin: 0 0 0.5rem;
      }
      .clean-list {
        list-style: none;
        padding: 0;
        margin: 0;
        display: flex;
        flex-direction: column;
        gap: 0.4rem;
        font-size: 0.85rem;
      }
      .cf-tabs {
        display: flex;
        gap: 0.5rem;
        flex-wrap: wrap;
        border-bottom: 1px solid #cbd5e1;
        padding-bottom: 0.5rem;
      }
      .cf-tabs button {
        padding: 0.55rem 0.95rem;
        border-radius: 0.5rem;
        border: 1px solid transparent;
        background: transparent;
        cursor: pointer;
        font-weight: 600;
        font-size: 0.86rem;
        color: #475569;
      }
      .cf-tabs button.active {
        background: #0f172a;
        color: #ffffff;
      }
      .cf-tab-panel {
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }
      .cf-grid-2 {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(420px, 1fr));
        gap: 1rem;
      }
      .cf-table {
        width: 100%;
        border-collapse: collapse;
        font-size: 0.85rem;
      }
      .cf-table th,
      .cf-table td {
        padding: 0.6rem 0.65rem;
        border-bottom: 1px solid #e2e8f0;
        text-align: left;
      }
      .cf-table th.text-right,
      .cf-table td.text-right {
        text-align: right;
      }
      .row-risco {
        background: #fef2f2;
      }
      .row-estornado {
        opacity: 0.6;
        text-decoration: line-through;
      }
      .cf-form-row {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
        gap: 0.65rem;
        margin-bottom: 0.65rem;
      }
      .cf-inline-form {
        margin-top: 1rem;
        padding-top: 0.85rem;
        border-top: 1px solid #e2e8f0;
      }
      .cf-inline-filters {
        display: flex;
        gap: 0.5rem;
        flex-wrap: wrap;
      }
      .btn {
        padding: 0.5rem 0.9rem;
        border-radius: 0.5rem;
        border: 1px solid transparent;
        cursor: pointer;
        font-weight: 600;
        font-size: 0.85rem;
        text-decoration: none;
        display: inline-flex;
        align-items: center;
        justify-content: center;
      }
      .btn--primary {
        background: #0284c7;
        color: #ffffff;
      }
      .btn--outline {
        border-color: #cbd5e1;
        background: #ffffff;
        color: #0f172a;
      }
      .btn--ghost {
        background: rgba(255, 255, 255, 0.12);
        color: inherit;
        border-color: rgba(148, 163, 184, 0.35);
      }
      .btn--small {
        padding: 0.25rem 0.55rem;
        font-size: 0.78rem;
      }
      .badge {
        display: inline-block;
        padding: 0.18rem 0.5rem;
        border-radius: 999px;
        font-size: 0.74rem;
        font-weight: 600;
      }
      .badge--accent {
        background: #0284c7;
        color: #ffffff;
      }
      .badge--simulado {
        background: #7c3aed;
        color: #ffffff;
      }
      .badge--outline {
        border: 1px solid #cbd5e1;
        color: #475569;
      }
      .badge--success {
        background: #dcfce7;
        color: #166534;
      }
      .badge--danger {
        background: #fee2e2;
        color: #991b1b;
      }
      .badge--warning {
        background: #fef3c7;
        color: #92400e;
      }
      .status-pill {
        display: inline-block;
        padding: 0.22rem 0.65rem;
        border-radius: 999px;
        font-size: 0.76rem;
        font-weight: 700;
      }
      .status-pill--saudavel {
        background: #dcfce7;
        color: #166534;
      }
      .status-pill--atencao {
        background: #fef3c7;
        color: #92400e;
      }
      .status-pill--critico {
        background: #fee2e2;
        color: #991b1b;
      }
      .text-success {
        color: #15803d;
      }
      .text-danger {
        color: #b91c1c;
      }
      .text-muted {
        color: #64748b;
      }
      .d-block {
        display: block;
      }
      .alert {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 0.75rem 1rem;
        border-radius: 0.65rem;
        font-size: 0.88rem;
      }
      .alert--success {
        background: #dcfce7;
        color: #166534;
      }
      .alert--error {
        background: #fee2e2;
        color: #991b1b;
      }
      .alert__close {
        background: none;
        border: none;
        font-size: 1.1rem;
        cursor: pointer;
      }
    `
  ]
})
export class FinanceCashFlowPage implements OnInit {
  private readonly api = inject(ApexApiService);
  private readonly auth = inject(AuthService);

  readonly carregando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly mensagemSucesso = signal<string | null>(null);

  readonly abaAtiva = signal<CashFlowTab>('realizado');
  readonly painel = signal<FluxoCaixaPainelResponse | null>(null);
  readonly conciliacao = signal<ConciliacaoResumoResponse | null>(null);
  readonly centrosCusto = signal<CentroCustoResponse[]>([]);
  readonly planoContas = signal<PlanoContasResponse[]>([]);
  readonly simulacaoAtiva = signal(false);

  filtroDataInicio = this.defaultStartDate();
  filtroDataFim = this.defaultEndDate();
  filtroContaFinanceiraId: number | null = null;
  filtroCentroCustoId: number | null = null;
  filtroConsolidarFiliais = false;

  readonly filtroNaturezaPrevisto = signal<'TODOS' | 'ENTRADA' | 'SAIDA'>('TODOS');
  readonly filtroPrazoPrevisto = signal<'TODOS' | 'A_VENCER' | 'VENCIDO'>('TODOS');
  readonly filtroCenarioProjecao = signal<'TODOS' | 'OTIMISTA' | 'REALISTA' | 'PESSIMISTA'>('TODOS');

  readonly itemSelecionadoBaixa = signal<FluxoCaixaPrevistoItem | null>(null);

  novaContaForm: ContaFinanceiraRequest = {
    codigo: '',
    nome: '',
    tipoConta: 'CONTA_CORRENTE',
    saldoInicial: 0,
    padrao: false,
    ativo: true
  };

  transferenciaForm: TransferenciaInternaRequest = {
    contaOrigemId: 0,
    contaDestinoId: 0,
    valor: 0,
    dataTransferencia: this.todayIso(),
    descricao: '',
    documentoReferencia: ''
  };

  parcelasForm: GerarParcelasRequest = {
    natureza: 'RECEBER',
    descricao: '',
    valorTotal: 0,
    quantidadeParcelas: 2,
    intervaloDias: 30,
    primeiroVencimento: this.todayIso(),
    modalidadeRecebivel: 'BOLETO',
    taxaPercentualCartao: 0,
    contaFinanceiraId: null,
    recorrente: false
  };

  baixaForm: BaixaTituloFinanceiroRequest = {
    valorLiquidado: 0,
    dataLiquidacao: this.todayIso(),
    contaFinanceiraId: null,
    valorTaxa: 0,
    taxaAntecipacao: 0,
    valorJurosDesconto: 0,
    antecipacaoRecebivel: false,
    observacao: ''
  };

  simulacaoForm: FluxoCaixaSimulacaoRequest = {
    horizonteDias: 90,
    saidaSimuladaAdicional: 0,
    investimentoPlanejado: 0,
    entradaSimuladaAdicional: 0,
    dataSimulacao: this.todayIso(),
    simularAntecipacaoCartao: false,
    taxaAntecipacaoSimuladaPercentual: 2.5
  };

  importacaoForm: ImportacaoExtratoRequest = {
    contaFinanceiraId: 0,
    tipoConciliacao: 'BANCARIA',
    formatoOrigem: 'CSV',
    conteudoArquivo: ''
  };

  readonly podeEditar = computed(() => {
    const roles = this.auth.user()?.roles ?? [];
    return roles.some((r) =>
      [
        'ROLE_SYSADMIN',
        'ROLE_ADMIN',
        'ROLE_ADMINISTRACAO',
        'ROLE_DONO',
        'ROLE_DONO_GERENTE',
        'ROLE_GERENTE',
        'ROLE_GESTOR',
        'ROLE_FINANCEIRO',
        'ROLE_FINANCAS'
      ].includes(r)
    );
  });

  readonly itensPrevistosFiltrados = computed(() => {
    const lista = this.painel()?.itensPrevistos ?? [];
    const nat = this.filtroNaturezaPrevisto();
    const prazo = this.filtroPrazoPrevisto();
    return lista.filter((item) => {
      if (nat !== 'TODOS' && item.natureza !== nat) {
        return false;
      }
      if (prazo === 'VENCIDO' && item.situacaoVencimento !== 'VENCIDO') {
        return false;
      }
      if (prazo === 'A_VENCER' && item.situacaoVencimento === 'VENCIDO') {
        return false;
      }
      return true;
    });
  });

  readonly cenariosFiltrados = computed<FluxoCaixaCenarioProjecao[]>(() => {
    const lista = this.painel()?.cenariosProjecao ?? [];
    const cenario = this.filtroCenarioProjecao();
    if (cenario === 'TODOS') {
      return lista;
    }
    return lista.filter((c) => c.codigoCenario === cenario);
  });

  ngOnInit(): void {
    this.recarregarTudo();
  }

  recarregarTudo(): void {
    this.carregando.set(true);
    this.erro.set(null);

    forkJoin({
      painel: this.api.getFluxoCaixaPainel(this.buildQueryParams()),
      centros: this.api.listCentrosCusto(true),
      contas: this.api.listPlanoContas(true),
      conciliacao: this.api.getConciliacaoFinanceira(this.filtroContaFinanceiraId)
    }).subscribe({
      next: ({ painel, centros, contas, conciliacao }) => {
        this.painel.set(painel);
        this.simulacaoAtiva.set(false);
        this.centrosCusto.set(centros);
        this.planoContas.set(contas);
        this.conciliacao.set(conciliacao);
        if (!this.importacaoForm.contaFinanceiraId && painel.contasFinanceiras.length > 0) {
          this.importacaoForm.contaFinanceiraId = painel.contasFinanceiras[0].idContaFinanceira;
        }
        this.carregando.set(false);
      },
      error: () => {
        this.erro.set('Não foi possível carregar o painel de Fluxo de Caixa.');
        this.carregando.set(false);
      }
    });
  }

  aplicarFiltros(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.api.getFluxoCaixaPainel(this.buildQueryParams()).subscribe({
      next: (res) => {
        this.painel.set(res);
        if (res.resumoConciliacao) {
          this.conciliacao.set(res.resumoConciliacao);
        }
        this.simulacaoAtiva.set(false);
        if (!this.importacaoForm.contaFinanceiraId && res.contasFinanceiras.length > 0) {
          this.importacaoForm.contaFinanceiraId = res.contasFinanceiras[0].idContaFinanceira;
        }
        this.carregando.set(false);
      },
      error: () => {
        this.erro.set('Falha ao atualizar os filtros do Fluxo de Caixa.');
        this.carregando.set(false);
      }
    });
  }

  selecionarAbaConciliacao(): void {
    this.abaAtiva.set('conciliacao');
    this.carregarConciliacao();
  }

  carregarConciliacao(): void {
    this.api.getConciliacaoFinanceira(this.filtroContaFinanceiraId).subscribe({
      next: (res) => this.conciliacao.set(res),
      error: () => this.erro.set('Falha ao consultar itens de conciliação financeira.')
    });
  }

  executarBootstrapContas(): void {
    this.api.bootstrapContasFinanceiras().subscribe({
      next: () => {
        this.mensagemSucesso.set('Contas financeiras padrão inicializadas com sucesso.');
        this.aplicarFiltros();
      },
      error: () => this.erro.set('Não foi possível inicializar as contas financeiras padrão.')
    });
  }

  criarNovaContaFinanceira(): void {
    if (!this.novaContaForm.codigo.trim() || !this.novaContaForm.nome.trim()) {
      this.erro.set('Informe o código e o nome da conta financeira.');
      return;
    }
    this.api.createContaFinanceira(this.novaContaForm).subscribe({
      next: () => {
        this.mensagemSucesso.set('Conta financeira cadastrada com sucesso.');
        this.novaContaForm = {
          codigo: '',
          nome: '',
          tipoConta: 'CONTA_CORRENTE',
          saldoInicial: 0,
          padrao: false,
          ativo: true
        };
        this.aplicarFiltros();
      },
      error: () => this.erro.set('Erro ao criar conta financeira.')
    });
  }

  realizarTransferencia(): void {
    if (
      !this.transferenciaForm.contaOrigemId ||
      !this.transferenciaForm.contaDestinoId ||
      this.transferenciaForm.contaOrigemId === this.transferenciaForm.contaDestinoId ||
      this.transferenciaForm.valor <= 0
    ) {
      this.erro.set('Selecione contas de origem e destino distintas e um valor positivo.');
      return;
    }
    this.api.transferirEntreContas(this.transferenciaForm).subscribe({
      next: () => {
        this.mensagemSucesso.set('Transferência entre contas próprias registrada sem impactar a DRE.');
        this.transferenciaForm.valor = 0;
        this.aplicarFiltros();
      },
      error: () => this.erro.set('Não foi possível registrar a transferência interna.')
    });
  }

  estornarMovimentacao(mov: MovimentacaoFinanceiraResponse): void {
    this.api
      .estornarMovimentacaoFinanceira(mov.idMovimentacaoFinanceira, {
        motivo: `Estorno solicitado no painel para ${mov.descricao}`,
        dataEstorno: this.todayIso()
      })
      .subscribe({
        next: () => {
          this.mensagemSucesso.set('Movimentação estornada com trilha de auditoria preservada.');
          this.aplicarFiltros();
        },
        error: () => this.erro.set('Não foi possível estornar a movimentação selecionada.')
      });
  }

  gerarParcelas(): void {
    if (!this.parcelasForm.descricao.trim() || this.parcelasForm.valorTotal <= 0) {
      this.erro.set('Preencha a descrição e um valor total maior que zero para gerar as parcelas.');
      return;
    }
    const payload: GerarParcelasRequest = {
      ...this.parcelasForm,
      intervaloDias: this.parcelasForm.intervaloDias && this.parcelasForm.intervaloDias >= 1 ? this.parcelasForm.intervaloDias : 30
    };
    this.api.gerarParcelasFluxoCaixa(payload).subscribe({
      next: (geradas) => {
        this.mensagemSucesso.set(`${geradas.length} parcela(s) gerada(s) no fluxo de caixa previsto.`);
        this.aplicarFiltros();
      },
      error: () => this.erro.set('Erro ao gerar parcelas no fluxo de caixa.')
    });
  }

  prepararBaixaTitulo(item: FluxoCaixaPrevistoItem): void {
    this.itemSelecionadoBaixa.set(item);
    const defaultContaId = this.painel()?.contasFinanceiras?.[0]?.idContaFinanceira ?? null;
    const saldoBrutoRestante = Math.max(0, Number(item.valorBruto ?? 0) - Number(item.valorPagoAcumulado ?? 0));
    this.baixaForm = {
      valorLiquidado: saldoBrutoRestante > 0 ? saldoBrutoRestante : item.saldoAbertoLiquido,
      dataLiquidacao: this.todayIso(),
      contaFinanceiraId: defaultContaId,
      valorTaxa: item.valorTaxaPrevista ?? 0,
      taxaAntecipacao: 0,
      valorJurosDesconto: 0,
      antecipacaoRecebivel: false,
      observacao: `Baixa do título #${item.registroId}`
    };
  }

  confirmarBaixaTitulo(): void {
    const sel = this.itemSelecionadoBaixa();
    if (!sel || this.baixaForm.valorLiquidado <= 0) {
      return;
    }
    const isReceber = sel.origemTipo === 'CONTA_RECEBER' || sel.origemTipo === 'REPASSE_CARTAO';
    const obs = isReceber
      ? this.api.baixarContaReceber(sel.registroId, this.baixaForm)
      : this.api.baixarContaPagar(sel.registroId, this.baixaForm);

    obs.subscribe({
      next: () => {
        this.mensagemSucesso.set(`Liquidação registrada com sucesso para #${sel.registroId}.`);
        this.itemSelecionadoBaixa.set(null);
        this.aplicarFiltros();
      },
      error: () => this.erro.set('Falha ao registrar a baixa do título financeiro.')
    });
  }

  executarSimulacao(): void {
    this.carregando.set(true);
    this.api.simularFluxoCaixa(this.simulacaoForm, this.buildQueryParams()).subscribe({
      next: (cenariosSimulados) => {
        const atual = this.painel();
        if (atual) {
          this.painel.set({
            ...atual,
            cenariosProjecao: cenariosSimulados
          });
        }
        this.simulacaoAtiva.set(true);
        this.carregando.set(false);
        this.mensagemSucesso.set('Simulação de liquidez aplicada (nenhum dado real foi alterado).');
      },
      error: () => {
        this.erro.set('Não foi possível executar a simulação de fluxo de caixa.');
        this.carregando.set(false);
      }
    });
  }

  limparSimulacao(): void {
    this.aplicarFiltros();
  }

  importarExtrato(): void {
    const contaId =
      this.importacaoForm.contaFinanceiraId ||
      this.painel()?.contasFinanceiras?.[0]?.idContaFinanceira ||
      0;
    if (!contaId || !this.importacaoForm.conteudoArquivo?.trim()) {
      this.erro.set('Selecione uma conta financeira e cole o conteúdo do extrato CSV ou OFX.');
      return;
    }
    const payload: ImportacaoExtratoRequest = {
      ...this.importacaoForm,
      contaFinanceiraId: contaId
    };
    this.api.importarExtratoConciliacao(payload).subscribe({
      next: (res) => {
        this.conciliacao.set(res);
        this.mensagemSucesso.set('Extrato processado e conciliado com sucesso.');
        this.importacaoForm.conteudoArquivo = '';
        this.aplicarFiltros();
      },
      error: () => this.erro.set('Falha ao importar o extrato para conciliação.')
    });
  }

  resolverConciliacaoManual(
    item: ConciliacaoItemResponse,
    novoStatus: StatusConciliacaoFinanceira
  ): void {
    const req: ConciliacaoManualRequest = {
      conciliacaoItemId: item.idConciliacaoItem,
      novoStatus,
      observacao: `Resolução manual (${novoStatus}) pelo painel de conciliação`
    };
    this.api.conciliarManualmente(req).subscribe({
      next: () => {
        this.mensagemSucesso.set(`Item de conciliação atualizado (${novoStatus}).`);
        this.carregarConciliacao();
      },
      error: () => this.erro.set('Erro ao atualizar o item de conciliação.')
    });
  }

  formatCurrency(value: number | null | undefined): string {
    const num = Number(value ?? 0);
    return num.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  private buildQueryParams(): FluxoCaixaQueryParams {
    return {
      dataInicio: this.filtroDataInicio,
      dataFim: this.filtroDataFim,
      contaFinanceiraId: this.filtroContaFinanceiraId,
      centroCustoId: this.filtroCentroCustoId,
      consolidarFiliais: this.filtroConsolidarFiliais
    };
  }

  private todayIso(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private defaultStartDate(): string {
    const now = new Date();
    const first = new Date(now.getFullYear(), now.getMonth(), 1);
    return first.toISOString().slice(0, 10);
  }

  private defaultEndDate(): string {
    const now = new Date();
    const last = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    return last.toISOString().slice(0, 10);
  }
}
