import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { ApexApiService } from '../core/apex-api.service';
import { AuthService } from '../core/auth.service';
import {
  CentroCustoResponse,
  DreComparativoFiliaisResponse,
  DreContaDetalhe,
  DreDrillDownResponse,
  DreEvolucaoResponse,
  DreGerencialResponse,
  DreLancamentoOrigem,
  DreLinha,
  DreQueryParams,
  DreTipoPeriodo
} from '../core/models';

type DreAba = 'demonstracao' | 'lancamentos' | 'evolucao' | 'filiais';

@Component({
  selector: 'app-finance-dre-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="dre-shell">
      <header class="dre-header">
        <div>
          <span class="eyebrow">Controladoria &amp; Regime de Competência — Lote 2</span>
          <h1>DRE Gerencial Configurável (17 Linhas)</h1>
          <p class="subtitle">
            Demonstração do Resultado do Exercício apurada pelo regime de competência sobre o Plano de Contas Gerencial,
            com análise vertical, análise horizontal, comparativo entre filiais e rastreabilidade até o documento de origem.
          </p>
        </div>

        <div class="header-actions">
          @if (isReadOnlyAuditor()) {
            <span class="badge badge-neutral" title="Perfil Auditor/Consulta: visualização integral sem permissão de exportação.">
              Modo Leitura (Auditor)
            </span>
          }
          <a routerLink="/finance/foundation" class="btn btn-secondary">
            Fundação Financeira
          </a>
          <a routerLink="/finance" class="btn btn-secondary">
            Calculadora ADM
          </a>
          @if (canExport()) {
            <button type="button" class="btn btn-outline" [disabled]="exportando()" (click)="exportar('CSV')">
              {{ exportando() ? 'Exportando...' : 'Exportar CSV' }}
            </button>
            <button type="button" class="btn btn-primary" [disabled]="exportando()" (click)="exportar('JSON')">
              Exportar JSON
            </button>
          }
        </div>
      </header>

      <!-- Barra de Filtros de Apuração -->
      <section class="filter-card" aria-label="Filtros de Apuração da DRE">
        <div class="filter-grid">
          <div class="form-field">
            <label for="dreTipoPeriodo">Regime / Período</label>
            <select id="dreTipoPeriodo" [ngModel]="tipoPeriodo()" (ngModelChange)="onTipoPeriodoChange($event)">
              <option value="MENSAL">Mensal (Competência YYYY-MM)</option>
              <option value="TRIMESTRAL">Trimestral</option>
              <option value="SEMESTRAL">Semestral</option>
              <option value="ANUAL">Anual</option>
              <option value="PERSONALIZADO">Personalizado (Datas)</option>
            </select>
          </div>

          @if (tipoPeriodo() === 'MENSAL') {
            <div class="form-field">
              <label for="dreCompetencia">Competência (YYYY-MM)</label>
              <input
                id="dreCompetencia"
                type="month"
                [ngModel]="competencia()"
                (ngModelChange)="competencia.set($event)"
              />
            </div>
          }

          @if (tipoPeriodo() === 'TRIMESTRAL' || tipoPeriodo() === 'SEMESTRAL' || tipoPeriodo() === 'ANUAL') {
            <div class="form-field">
              <label for="dreAno">Ano</label>
              <input
                id="dreAno"
                type="number"
                min="2020"
                max="2099"
                [ngModel]="ano()"
                (ngModelChange)="ano.set(+$event)"
              />
            </div>
          }

          @if (tipoPeriodo() === 'TRIMESTRAL') {
            <div class="form-field">
              <label for="dreTrimestre">Trimestre</label>
              <select id="dreTrimestre" [ngModel]="trimestre()" (ngModelChange)="trimestre.set(+$event)">
                <option [ngValue]="1">1º Trimestre (Jan–Mar)</option>
                <option [ngValue]="2">2º Trimestre (Abr–Jun)</option>
                <option [ngValue]="3">3º Trimestre (Jul–Set)</option>
                <option [ngValue]="4">4º Trimestre (Out–Dez)</option>
              </select>
            </div>
          }

          @if (tipoPeriodo() === 'SEMESTRAL') {
            <div class="form-field">
              <label for="dreSemestre">Semestre</label>
              <select id="dreSemestre" [ngModel]="semestre()" (ngModelChange)="semestre.set(+$event)">
                <option [ngValue]="1">1º Semestre (Jan–Jun)</option>
                <option [ngValue]="2">2º Semestre (Jul–Dez)</option>
              </select>
            </div>
          }

          @if (tipoPeriodo() === 'PERSONALIZADO') {
            <div class="form-field">
              <label for="dreDataInicio">Data Inicial</label>
              <input id="dreDataInicio" type="date" [ngModel]="dataInicio()" (ngModelChange)="dataInicio.set($event)" />
            </div>
            <div class="form-field">
              <label for="dreDataFim">Data Final</label>
              <input id="dreDataFim" type="date" [ngModel]="dataFim()" (ngModelChange)="dataFim.set($event)" />
            </div>
          }

          <div class="form-field">
            <label for="dreCentroCusto">Centro de Custo</label>
            <select id="dreCentroCusto" [ngModel]="centroCustoId()" (ngModelChange)="centroCustoId.set($event)">
              <option [ngValue]="null">Todos os Centros de Custo</option>
              @for (cc of centrosCusto(); track cc.idCentroCusto) {
                <option [ngValue]="cc.idCentroCusto">{{ cc.codigo }} — {{ cc.nome }}</option>
              }
            </select>
          </div>

          <div class="form-field checkbox-group">
            <label class="checkbox-label">
              <input
                type="checkbox"
                [ngModel]="compararPeriodoAnterior()"
                (ngModelChange)="compararPeriodoAnterior.set($event)"
              />
              Comparar com período anterior (AH%)
            </label>
            <label class="checkbox-label">
              <input
                type="checkbox"
                [ngModel]="consolidarFiliais()"
                (ngModelChange)="consolidarFiliais.set($event)"
              />
              Consolidar todas as filiais da empresa
            </label>
          </div>

          <div class="form-field filter-submit">
            <button type="button" class="btn btn-primary full-width" [disabled]="carregando()" (click)="atualizarTudo()">
              {{ carregando() ? 'Apurando DRE...' : 'Apurar DRE' }}
            </button>
          </div>
        </div>
      </section>

      @if (erro()) {
        <div class="alert alert-danger" role="alert">
          <strong>Falha na apuração:</strong> {{ erro() }}
        </div>
      }

      @if (mensagemSucesso()) {
        <div class="alert alert-success" role="status">
          {{ mensagemSucesso() }}
        </div>
      }

      @if (dre(); as d) {
        <!-- Contexto do Período Apurado -->
        <div class="period-banner">
          <div>
            <strong>Escopo apurado:</strong> {{ d.filialNome }}
            <span class="sep">|</span>
            <strong>Período:</strong> {{ d.rotuloPeriodo }} ({{ d.dataInicio }} a {{ d.dataFim }})
            @if (d.comparacaoAtiva && d.rotuloPeriodoComparacao) {
              <span class="sep">|</span>
              <strong>Comparado com:</strong> {{ d.rotuloPeriodoComparacao }}
            }
            @if (d.centroCustoNome) {
              <span class="sep">|</span>
              <strong>Centro de Custo:</strong> {{ d.centroCustoNome }}
            }
          </div>
          <div class="vertical-base-tag">
            Base Análise Vertical (AV%): <strong>{{ d.baseAnaliseVertical }}</strong>
          </div>
        </div>

        <!-- Cards Executivos de Resultado -->
        <section class="kpi-grid" aria-label="Indicadores Executivos da DRE">
          <article class="kpi-card">
            <span class="kpi-label">1. Receita Bruta</span>
            <strong class="kpi-value">{{ formatCurrency(d.indicadores.receitaBruta) }}</strong>
            <span class="kpi-sub">Deduções: {{ formatCurrency(d.indicadores.deducoesTotais) }}</span>
          </article>

          <article class="kpi-card highlight-neutral">
            <span class="kpi-label">5. Receita Líquida (100% AV)</span>
            <strong class="kpi-value">{{ formatCurrency(d.indicadores.receitaLiquida) }}</strong>
            <span class="kpi-sub">Faturamento líquido de devoluções, descontos e impostos</span>
          </article>

          <article class="kpi-card">
            <span class="kpi-label">6. (-) CMV</span>
            <strong class="kpi-value text-danger">{{ formatCurrency(d.indicadores.cmv) }}</strong>
            <span class="kpi-sub">
              Custo das Mercadorias Vendidas ({{ formatPercent(calcAv(d.indicadores.cmv, d.indicadores.receitaLiquida)) }} da RL)
            </span>
          </article>

          <article class="kpi-card">
            <span class="kpi-label">7. (=) Lucro Bruto</span>
            <strong class="kpi-value" [class.text-success]="d.indicadores.lucroBruto >= 0" [class.text-danger]="d.indicadores.lucroBruto < 0">
              {{ formatCurrency(d.indicadores.lucroBruto) }}
            </strong>
            <span class="kpi-sub">Margem Bruta: <strong>{{ formatPercent(d.indicadores.margemBrutaPercentual) }}</strong></span>
          </article>

          <article class="kpi-card">
            <span class="kpi-label">Despesas Operacionais (8 a 12)</span>
            <strong class="kpi-value text-danger">{{ formatCurrency(d.indicadores.despesasOperacionaisTotais) }}</strong>
            <span class="kpi-sub">
              Fixas: {{ formatCurrency(d.indicadores.despesasFixas) }} | Variáveis: {{ formatCurrency(d.indicadores.despesasVariaveis) }}
            </span>
          </article>

          <article class="kpi-card">
            <span class="kpi-label">13. (=) Resultado Operacional</span>
            <strong class="kpi-value" [class.text-success]="d.indicadores.resultadoOperacional >= 0" [class.text-danger]="d.indicadores.resultadoOperacional < 0">
              {{ formatCurrency(d.indicadores.resultadoOperacional) }}
            </strong>
            <span class="kpi-sub">
              Margem Oper.: <strong>{{ formatPercent(d.indicadores.margemOperacionalPercentual) }}</strong>
              | 14. Fin. Líq.: <strong>{{ formatCurrency(d.indicadores.resultadoFinanceiroLiquido) }}</strong>
            </span>
          </article>

          <article
            class="kpi-card"
            [class.highlight-positive]="d.indicadores.resultadoLiquidoGerencial > 0"
            [class.highlight-negative]="d.indicadores.resultadoLiquidoGerencial < 0"
          >
            <span class="kpi-label">17. (=) Resultado Líquido Gerencial</span>
            <strong class="kpi-value" [class.text-success]="d.indicadores.resultadoLiquidoGerencial >= 0" [class.text-danger]="d.indicadores.resultadoLiquidoGerencial < 0">
              {{ formatCurrency(d.indicadores.resultadoLiquidoGerencial) }}
            </strong>
            <span class="kpi-sub">Margem Líquida: <strong>{{ formatPercent(d.indicadores.margemLiquidaPercentual) }}</strong></span>
          </article>

          <article class="kpi-card">
            <span class="kpi-label">Ponto de Equilíbrio Contábil</span>
            <strong class="kpi-value">{{ formatCurrency(d.indicadores.pontoEquilibrioContabilReceita) }}</strong>
            <span class="kpi-sub">
              Margem de Contribuição: {{ formatCurrency(d.indicadores.margemContribuicaoValor) }} ({{ formatPercent(d.indicadores.margemContribuicaoPercentual) }})
            </span>
          </article>
        </section>

        <!-- Painel de Diagnóstico Gerencial em Linguagem Acessível -->
        <section class="diagnostic-panel" aria-label="Diagnóstico Gerencial da DRE">
          <div class="diagnostic-header">
            <div>
              <span class="eyebrow">Leitura Guiada para Gestão da Loja</span>
              <h2>Diagnóstico Econômico do Período</h2>
            </div>
            <span class="badge" [ngClass]="situacaoBadgeClass(d.diagnostico.situacaoPeriodo)">
              {{ formatSituacao(d.diagnostico.situacaoPeriodo) }}
            </span>
          </div>

          <p class="diagnostic-lead">{{ d.diagnostico.mancheteExplicativa }}</p>

          <div class="diagnostic-columns">
            <div class="diag-box">
              <h3>O que aconteceu com o resultado?</h3>
              <p>{{ d.diagnostico.explicacaoResultado }}</p>
              <p class="mt-2">{{ d.diagnostico.explicacaoPontoEquilibrio }}</p>
            </div>

            <div class="diag-box">
              <h3>Competência (DRE) vs. Caixa Realizado</h3>
              <p>{{ d.diagnostico.explicacaoCompetenciaVsCaixa }}</p>
              <div class="cash-reconciliation">
                <div>
                  <span>Entradas Caixa:</span>
                  <strong>{{ formatCurrency(d.indicadores.entradasRealizadasCaixa) }}</strong>
                </div>
                <div>
                  <span>Saídas Caixa:</span>
                  <strong>{{ formatCurrency(d.indicadores.saidasRealizadasCaixa) }}</strong>
                </div>
                <div>
                  <span>Saldo Caixa Realizado:</span>
                  <strong [class.text-success]="d.indicadores.saldoRealizadoCaixa >= 0" [class.text-danger]="d.indicadores.saldoRealizadoCaixa < 0">
                    {{ formatCurrency(d.indicadores.saldoRealizadoCaixa) }}
                  </strong>
                </div>
              </div>
            </div>
          </div>

          @if (d.diagnostico.principaisFatores.length > 0) {
            <div class="factors-row">
              @for (fator of d.diagnostico.principaisFatores; track fator.titulo) {
                <div class="factor-chip" [ngClass]="'factor-' + fator.impacto.toLowerCase()">
                  <div class="factor-top">
                    <span class="factor-cat">{{ fator.categoria }}</span>
                    <strong>{{ formatCurrency(fator.valor) }}</strong>
                  </div>
                  <div class="factor-title">{{ fator.titulo }}</div>
                  <p class="factor-exp">{{ fator.explicacao }}</p>
                </div>
              }
            </div>
          }

          <div class="diagnostic-columns mt-3">
            <div class="diag-box">
              <h3>Recomendações Práticas</h3>
              <ul class="clean-list">
                @for (rec of d.diagnostico.recomendacoesGerenciais; track rec) {
                  <li>{{ rec }}</li>
                }
              </ul>
            </div>

            <div class="diag-box">
              <h3>Governança Contábil e Limitações</h3>
              <ul class="clean-list warning-list">
                @for (alerta of d.diagnostico.avisosLimitacao; track alerta) {
                  <li>{{ alerta }}</li>
                }
              </ul>
              @if (d.lancamentosSemClassificacao > 0) {
                <div class="unclassified-cta">
                  <span>
                    Existem <strong>{{ d.lancamentosSemClassificacao }}</strong> lançamento(s)
                    ({{ formatCurrency(d.valorSemClassificacao) }}) sem conta analítica vinculada.
                  </span>
                  <a routerLink="/finance/foundation" class="btn btn-outline btn-sm">
                    Classificar na Fundação Financeira
                  </a>
                </div>
              }
            </div>
          </div>
        </section>

        <!-- Abas de Navegação da DRE -->
        <nav class="tabs-bar" aria-label="Visões da DRE Gerencial">
          <button
            type="button"
            class="tab-btn"
            [class.active]="abaAtiva() === 'demonstracao'"
            (click)="selecionarAba('demonstracao')"
          >
            1. Demonstração (17 Linhas)
          </button>
          <button
            type="button"
            class="tab-btn"
            [class.active]="abaAtiva() === 'lancamentos'"
            (click)="selecionarAba('lancamentos')"
          >
            2. Rastreabilidade / Lançamentos
            @if (drillDown(); as dd) {
              <span class="tab-counter">{{ dd.totalLancamentos }}</span>
            }
          </button>
          <button
            type="button"
            class="tab-btn"
            [class.active]="abaAtiva() === 'evolucao'"
            (click)="selecionarAba('evolucao')"
          >
            3. Evolução Mensal
          </button>
          <button
            type="button"
            class="tab-btn"
            [class.active]="abaAtiva() === 'filiais'"
            (click)="selecionarAba('filiais')"
          >
            4. Comparativo entre Filiais
          </button>
        </nav>

        <!-- ABA 1: DEMONSTRAÇÃO HIERÁRQUICA DE 17 LINHAS -->
        @if (abaAtiva() === 'demonstracao') {
          <section class="panel-card" aria-label="Tabela de 17 Linhas da DRE">
            <div class="panel-toolbar">
              <div>
                <h2>Estrutura Gerencial de 17 Linhas</h2>
                <p class="muted">
                  Clique em qualquer linha com contas para expandir as contas sintéticas e analíticas, ou em
                  <strong>Rastrear Origem</strong> para inspecionar os documentos que formam o saldo.
                </p>
              </div>
              <div class="toolbar-actions">
                <button type="button" class="btn btn-sm btn-outline" (click)="expandirTodasLinhas()">
                  Expandir Contas
                </button>
                <button type="button" class="btn btn-sm btn-outline" (click)="recolherTodasLinhas()">
                  Recolher Contas
                </button>
              </div>
            </div>

            <div class="table-responsive">
              <table class="dre-table">
                <thead>
                  <tr>
                    <th class="col-sinal">Sinal</th>
                    <th class="col-titulo">Linha da DRE / Conta Gerencial</th>
                    <th class="text-right">Valor Apurado (R$)</th>
                    <th class="text-right" title="Análise Vertical sobre a Receita Líquida (Linha 5 = 100%)">AV (%)</th>
                    @if (d.comparacaoAtiva) {
                      <th class="text-right">Período Anterior (R$)</th>
                      <th class="text-right">Variação (R$)</th>
                      <th class="text-right" title="Análise Horizontal (%)">AH (%)</th>
                      <th class="text-center">Avaliação</th>
                    }
                    <th class="text-center">Qtd.</th>
                    <th class="text-right">Rastreabilidade</th>
                  </tr>
                </thead>
                <tbody>
                  @for (linha of d.linhas; track linha.numeroLinha) {
                    <tr
                      class="dre-line-row"
                      [class.row-subtotal]="linha.tipoLinha === 'SUBTOTAL' || linha.tipoLinha === 'RESULTADO_PARCIAL'"
                      [class.row-resultado]="linha.tipoLinha === 'RESULTADO' || linha.tipoLinha === 'RESULTADO_FINAL'"
                    >
                      <td class="col-sinal">
                        <span class="signal-pill" [ngClass]="sinalClass(linha.sinalDemonstracao)">{{ linha.sinalDemonstracao }}</span>
                      </td>
                      <td class="col-titulo">
                        <div class="line-title-wrap">
                          @if (linha.contas.length > 0) {
                            <button
                              type="button"
                              class="expand-btn"
                              (click)="toggleLinha(linha.numeroLinha)"
                              [attr.aria-label]="'Expandir linha ' + linha.titulo"
                            >
                              {{ isLinhaExpandida(linha.numeroLinha) ? '▼' : '▶' }}
                            </button>
                          } @else {
                            <span class="expand-placeholder"></span>
                          }
                          <div>
                            <div class="line-title">{{ linha.titulo }}</div>
                            <div class="line-explanation">{{ linha.explicacaoGerencial }}</div>
                          </div>
                        </div>
                      </td>
                      <td class="text-right font-mono" [ngClass]="valorClass(linha)">
                        {{ formatCurrency(linha.valor) }}
                      </td>
                      <td class="text-right font-mono">
                        {{ formatPercent(linha.analiseVerticalPercentual) }}
                      </td>
                      @if (d.comparacaoAtiva) {
                        <td class="text-right font-mono muted">
                          {{ linha.valorComparacao !== null && linha.valorComparacao !== undefined ? formatCurrency(linha.valorComparacao) : '—' }}
                        </td>
                        <td class="text-right font-mono">
                          {{ linha.variacaoAbsoluta !== null && linha.variacaoAbsoluta !== undefined ? formatSignedCurrency(linha.variacaoAbsoluta) : '—' }}
                        </td>
                        <td class="text-right font-mono">
                          {{ formatPercent(linha.analiseHorizontalPercentual) }}
                        </td>
                        <td class="text-center">
                          @if (linha.statusVariacao) {
                            <span class="badge badge-xs" [ngClass]="statusVariacaoClass(linha.statusVariacao)">
                              {{ linha.statusVariacao }}
                            </span>
                          }
                        </td>
                      }
                      <td class="text-center font-mono">
                        {{ linha.quantidadeLancamentos > 0 ? linha.quantidadeLancamentos : '—' }}
                      </td>
                      <td class="text-right">
                        @if (
                          linha.tipoLinha !== 'SUBTOTAL' &&
                          linha.tipoLinha !== 'RESULTADO' &&
                          linha.tipoLinha !== 'RESULTADO_PARCIAL' &&
                          linha.tipoLinha !== 'RESULTADO_FINAL'
                        ) {
                          <button
                            type="button"
                            class="btn btn-xs btn-outline"
                            (click)="rastrearLinha(linha)"
                          >
                            Rastrear Origem
                          </button>
                        }
                      </td>
                    </tr>

                    @if (isLinhaExpandida(linha.numeroLinha) && linha.contas.length > 0) {
                      @for (conta of linha.contas; track conta.codigo) {
                        <tr class="dre-account-row" [class.account-synthetic]="conta.sintetica">
                          <td></td>
                          <td class="account-indent-1">
                            <span class="account-code">{{ conta.codigo }}</span>
                            <span class="account-name">{{ conta.nome }}</span>
                            @if (conta.sintetica) {
                              <span class="badge badge-xs badge-neutral">Sintética</span>
                            } @else if (conta.classificacaoCusto) {
                              <span class="badge badge-xs badge-subtle">{{ conta.classificacaoCusto }}</span>
                            }
                          </td>
                          <td class="text-right font-mono">{{ formatCurrency(conta.valor) }}</td>
                          <td class="text-right font-mono">{{ formatPercent(conta.analiseVerticalPercentual) }}</td>
                          @if (d.comparacaoAtiva) {
                            <td class="text-right font-mono muted">
                              {{ conta.valorComparacao !== null && conta.valorComparacao !== undefined ? formatCurrency(conta.valorComparacao) : '—' }}
                            </td>
                            <td class="text-right font-mono">
                              {{ conta.variacaoAbsoluta !== null && conta.variacaoAbsoluta !== undefined ? formatSignedCurrency(conta.variacaoAbsoluta) : '—' }}
                            </td>
                            <td class="text-right font-mono">{{ formatPercent(conta.analiseHorizontalPercentual) }}</td>
                            <td></td>
                          }
                          <td class="text-center font-mono">{{ conta.quantidadeLancamentos }}</td>
                          <td class="text-right">
                            @if (conta.idPlanoConta !== null) {
                              <button
                                type="button"
                                class="btn btn-xs btn-link"
                                (click)="rastrearConta(linha, conta)"
                              >
                                Ver Lançamentos
                              </button>
                            }
                          </td>
                        </tr>

                        @for (filha of conta.filhas; track filha.codigo) {
                          <tr class="dre-account-row account-analytic">
                            <td></td>
                            <td class="account-indent-2">
                              <span class="account-code">{{ filha.codigo }}</span>
                              <span class="account-name">{{ filha.nome }}</span>
                              @if (filha.classificacaoCusto) {
                                <span class="badge badge-xs badge-subtle">{{ filha.classificacaoCusto }}</span>
                              }
                            </td>
                            <td class="text-right font-mono">{{ formatCurrency(filha.valor) }}</td>
                            <td class="text-right font-mono">{{ formatPercent(filha.analiseVerticalPercentual) }}</td>
                            @if (d.comparacaoAtiva) {
                              <td class="text-right font-mono muted">
                                {{ filha.valorComparacao !== null && filha.valorComparacao !== undefined ? formatCurrency(filha.valorComparacao) : '—' }}
                              </td>
                              <td class="text-right font-mono">
                                {{ filha.variacaoAbsoluta !== null && filha.variacaoAbsoluta !== undefined ? formatSignedCurrency(filha.variacaoAbsoluta) : '—' }}
                              </td>
                              <td class="text-right font-mono">{{ formatPercent(filha.analiseHorizontalPercentual) }}</td>
                              <td></td>
                            }
                            <td class="text-center font-mono">{{ filha.quantidadeLancamentos }}</td>
                            <td class="text-right">
                              @if (filha.idPlanoConta !== null) {
                                <button
                                  type="button"
                                  class="btn btn-xs btn-link"
                                  (click)="rastrearConta(linha, filha)"
                                >
                                  Ver Lançamentos
                                </button>
                              }
                            </td>
                          </tr>
                        }
                      }
                    }
                  }
                </tbody>
              </table>
            </div>
          </section>
        }

        <!-- ABA 2: RASTREABILIDADE / DRILL-DOWN ATÉ OS LANÇAMENTOS DE ORIGEM -->
        @if (abaAtiva() === 'lancamentos') {
          <section class="panel-card" aria-label="Rastreabilidade de Lançamentos da DRE">
            <div class="panel-toolbar">
              <div>
                <h2>Rastreabilidade: DRE → Grupo → Conta → Lançamento → Documento</h2>
                @if (drillDown(); as dd) {
                  <p class="muted">
                    Exibindo <strong>{{ lancamentosFiltrados().length }}</strong> de <strong>{{ dd.totalLancamentos }}</strong>
                    lançamento(s) no período {{ dd.rotuloPeriodo }}
                    @if (dd.tituloLinhaFiltro) {
                      — Linha: <strong>{{ dd.tituloLinhaFiltro }}</strong>
                    }
                    @if (dd.planoContaNomeFiltro) {
                      — Conta: <strong>{{ dd.planoContaNomeFiltro }}</strong>
                    }
                    | Soma do impacto: <strong>{{ formatCurrency(dd.somaValorCompetencia) }}</strong>
                  </p>
                }
              </div>

              <div class="toolbar-actions">
                <select
                  aria-label="Filtrar por tipo de documento de origem"
                  [ngModel]="filtroOrigemDrillDown()"
                  (ngModelChange)="filtroOrigemDrillDown.set($event)"
                >
                  <option value="">Todas as Origens</option>
                  <option value="VENDA">Vendas (Faturamento Bruto)</option>
                  <option value="DEVOLUCAO">Devoluções de Vendas</option>
                  <option value="DESCONTO_VENDA">Descontos Concedidos</option>
                  <option value="CMV_VENDA">CMV de Vendas</option>
                  <option value="CMV_DEVOLUCAO">Reversão de CMV por Devolução</option>
                  <option value="DESPESA">Despesas</option>
                  <option value="CONTA_PAGAR">Contas a Pagar</option>
                  <option value="CONTA_RECEBER">Contas a Receber Avulsas</option>
                </select>
                <input
                  type="search"
                  placeholder="Buscar documento, descrição ou conta..."
                  [ngModel]="buscaDrillDown()"
                  (ngModelChange)="buscaDrillDown.set($event)"
                />
                <button type="button" class="btn btn-sm btn-outline" (click)="limparFiltroDrillDown()">
                  Ver Todos do Período
                </button>
              </div>
            </div>

            <div class="table-responsive">
              <table class="dre-table">
                <thead>
                  <tr>
                    <th>Origem</th>
                    <th>Documento</th>
                    <th>Descrição</th>
                    <th>Competência</th>
                    <th>Liquidação Caixa</th>
                    <th>Linha DRE</th>
                    <th>Conta Gerencial</th>
                    <th>Centro de Custo</th>
                    <th class="text-right">Valor Competência</th>
                    <th class="text-right">Ação</th>
                  </tr>
                </thead>
                <tbody>
                  @for (item of lancamentosFiltrados(); track item.origemTipo + '-' + item.registroId + '-' + item.numeroLinhaDre) {
                    <tr>
                      <td>
                        <span class="badge badge-xs badge-neutral">{{ item.origemTipo }}</span>
                      </td>
                      <td class="font-mono">
                        <strong>{{ item.documentoReferencia }}</strong>
                      </td>
                      <td>{{ item.descricao }}</td>
                      <td class="font-mono">{{ item.dataCompetencia }}</td>
                      <td class="font-mono muted">{{ item.dataLiquidacaoCaixa || 'Regime Competência' }}</td>
                      <td>{{ item.tituloLinhaDre }}</td>
                      <td>
                        <span class="account-code">{{ item.planoContaCodigo }}</span>
                        {{ item.planoContaNome }}
                      </td>
                      <td>{{ item.centroCustoNome || '—' }}</td>
                      <td
                        class="text-right font-mono"
                        [class.text-success]="item.impactoDre === 'POSITIVO'"
                        [class.text-danger]="item.impactoDre === 'NEGATIVO'"
                      >
                        {{ item.impactoDre === 'NEGATIVO' ? '- ' : '+ ' }}{{ formatCurrency(item.valorCompetencia) }}
                      </td>
                      <td class="text-right">
                        <button
                          type="button"
                          class="btn btn-xs btn-outline"
                          (click)="navegarParaOrigem(item)"
                        >
                          Abrir Módulo
                        </button>
                      </td>
                    </tr>
                  } @empty {
                    <tr>
                      <td colspan="10" class="empty-cell">
                        Nenhum lançamento encontrado para os critérios selecionados.
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </section>
        }

        <!-- ABA 3: EVOLUÇÃO MENSAL DOS RESULTADOS -->
        @if (abaAtiva() === 'evolucao') {
          <section class="panel-card" aria-label="Evolução Mensal da DRE">
            <div class="panel-toolbar">
              <div>
                <h2>Evolução Mensal dos Resultados</h2>
                <p class="muted">
                  Acompanhe a trajetória histórica de Receita Líquida, CMV, Lucro Bruto, Despesas Operacionais e Resultado Líquido.
                </p>
              </div>
              <div class="toolbar-actions">
                <label for="mesesEvolucao">Janela:</label>
                <select id="mesesEvolucao" [ngModel]="mesesEvolucao()" (ngModelChange)="alterarMesesEvolucao(+$event)">
                  <option [ngValue]="3">Últimos 3 meses</option>
                  <option [ngValue]="6">Últimos 6 meses</option>
                  <option [ngValue]="12">Últimos 12 meses</option>
                </select>
              </div>
            </div>

            @if (evolucao(); as ev) {
              <div class="kpi-grid mb-3">
                <article class="kpi-card">
                  <span class="kpi-label">Receita Líquida Acumulada ({{ ev.quantidadeMeses }}m)</span>
                  <strong class="kpi-value">{{ formatCurrency(ev.receitaLiquidaAcumulada) }}</strong>
                  <span class="kpi-sub">Tendência de Receita: <strong>{{ ev.tendenciaReceita }}</strong></span>
                </article>
                <article class="kpi-card">
                  <span class="kpi-label">Lucro Bruto Acumulado</span>
                  <strong class="kpi-value">{{ formatCurrency(ev.lucroBrutoAcumulado) }}</strong>
                  <span class="kpi-sub">Período: {{ ev.competenciaInicio }} a {{ ev.competenciaFim }}</span>
                </article>
                <article class="kpi-card">
                  <span class="kpi-label">Resultado Líquido Acumulado</span>
                  <strong class="kpi-value" [class.text-success]="ev.resultadoLiquidoAcumulado >= 0" [class.text-danger]="ev.resultadoLiquidoAcumulado < 0">
                    {{ formatCurrency(ev.resultadoLiquidoAcumulado) }}
                  </strong>
                  <span class="kpi-sub">Tendência de Resultado: <strong>{{ ev.tendenciaResultado }}</strong></span>
                </article>
                <article class="kpi-card">
                  <span class="kpi-label">Margem Líquida Média</span>
                  <strong class="kpi-value">{{ formatPercent(ev.margemLiquidaMediaPercentual) }}</strong>
                  <span class="kpi-sub">Média ponderada no período</span>
                </article>
              </div>

              <div class="table-responsive">
                <table class="dre-table">
                  <thead>
                    <tr>
                      <th>Competência</th>
                      <th class="text-right">Receita Bruta</th>
                      <th class="text-right">Receita Líquida</th>
                      <th class="text-right">(-) CMV</th>
                      <th class="text-right">(=) Lucro Bruto</th>
                      <th class="text-right">Margem Bruta</th>
                      <th class="text-right">(-) Desp. Operacionais</th>
                      <th class="text-right">(=) Resultado Líquido</th>
                      <th class="text-right">Margem Líquida</th>
                      <th class="text-right">Saldo Caixa</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (ponto of ev.serie; track ponto.competencia) {
                      <tr>
                        <td><strong>{{ ponto.rotulo }}</strong> ({{ ponto.competencia }})</td>
                        <td class="text-right font-mono">{{ formatCurrency(ponto.receitaBruta) }}</td>
                        <td class="text-right font-mono">{{ formatCurrency(ponto.receitaLiquida) }}</td>
                        <td class="text-right font-mono text-danger">{{ formatCurrency(ponto.cmv) }}</td>
                        <td class="text-right font-mono">{{ formatCurrency(ponto.lucroBruto) }}</td>
                        <td class="text-right font-mono">{{ formatPercent(ponto.margemBrutaPercentual) }}</td>
                        <td class="text-right font-mono text-danger">{{ formatCurrency(ponto.despesasTotais) }}</td>
                        <td
                          class="text-right font-mono"
                          [class.text-success]="ponto.resultadoLiquidoGerencial >= 0"
                          [class.text-danger]="ponto.resultadoLiquidoGerencial < 0"
                        >
                          <strong>{{ formatCurrency(ponto.resultadoLiquidoGerencial) }}</strong>
                        </td>
                        <td class="text-right font-mono">{{ formatPercent(ponto.margemLiquidaPercentual) }}</td>
                        <td class="text-right font-mono muted">{{ formatCurrency(ponto.saldoRealizadoCaixa) }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            }
          </section>
        }

        <!-- ABA 4: COMPARATIVO ENTRE FILIAIS -->
        @if (abaAtiva() === 'filiais') {
          <section class="panel-card" aria-label="Comparativo entre Filiais">
            <div class="panel-toolbar">
              <div>
                <h2>Comparativo de Desempenho entre Filiais</h2>
                <p class="muted">
                  Compare a contribuição de cada unidade na Receita Líquida, Margem Bruta e Resultado Líquido consolidado da empresa.
                </p>
              </div>
            </div>

            @if (comparativoFiliais(); as cf) {
              <div class="table-responsive">
                <table class="dre-table">
                  <thead>
                    <tr>
                      <th>Unidade / Filial</th>
                      <th class="text-right">Receita Bruta</th>
                      <th class="text-right">Receita Líquida</th>
                      <th class="text-right">(-) CMV</th>
                      <th class="text-right">(=) Lucro Bruto</th>
                      <th class="text-right">Margem Bruta %</th>
                      <th class="text-right">(-) Despesas Oper.</th>
                      <th class="text-right">(=) Resultado Líquido</th>
                      <th class="text-right">Margem Líquida %</th>
                      <th class="text-right">Part. Receita %</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (fil of cf.filiais; track fil.filialId) {
                      <tr>
                        <td>
                          <strong>{{ fil.filialCodigo }} — {{ fil.filialNome }}</strong>
                        </td>
                        <td class="text-right font-mono">{{ formatCurrency(fil.receitaBruta) }}</td>
                        <td class="text-right font-mono">{{ formatCurrency(fil.receitaLiquida) }}</td>
                        <td class="text-right font-mono text-danger">{{ formatCurrency(fil.cmv) }}</td>
                        <td class="text-right font-mono">{{ formatCurrency(fil.lucroBruto) }}</td>
                        <td class="text-right font-mono">{{ formatPercent(fil.margemBrutaPercentual) }}</td>
                        <td class="text-right font-mono text-danger">{{ formatCurrency(fil.despesasTotais) }}</td>
                        <td
                          class="text-right font-mono"
                          [class.text-success]="fil.resultadoLiquidoGerencial >= 0"
                          [class.text-danger]="fil.resultadoLiquidoGerencial < 0"
                        >
                          <strong>{{ formatCurrency(fil.resultadoLiquidoGerencial) }}</strong>
                        </td>
                        <td class="text-right font-mono">{{ formatPercent(fil.margemLiquidaPercentual) }}</td>
                        <td class="text-right font-mono">{{ formatPercent(fil.participacaoReceitaLiquidaPercentual) }}</td>
                      </tr>
                    }
                    <tr class="row-resultado">
                      <td><strong>CONSOLIDADO EMPRESA ({{ cf.filiais.length }} unidade(s))</strong></td>
                      <td class="text-right font-mono">{{ formatCurrency(cf.consolidadoEmpresa.receitaBruta) }}</td>
                      <td class="text-right font-mono">{{ formatCurrency(cf.consolidadoEmpresa.receitaLiquida) }}</td>
                      <td class="text-right font-mono">{{ formatCurrency(cf.consolidadoEmpresa.cmv) }}</td>
                      <td class="text-right font-mono">{{ formatCurrency(cf.consolidadoEmpresa.lucroBruto) }}</td>
                      <td class="text-right font-mono">{{ formatPercent(cf.consolidadoEmpresa.margemBrutaPercentual) }}</td>
                      <td class="text-right font-mono">{{ formatCurrency(cf.consolidadoEmpresa.despesasOperacionaisTotais) }}</td>
                      <td class="text-right font-mono">{{ formatCurrency(cf.consolidadoEmpresa.resultadoLiquidoGerencial) }}</td>
                      <td class="text-right font-mono">{{ formatPercent(cf.consolidadoEmpresa.margemLiquidaPercentual) }}</td>
                      <td class="text-right font-mono">100,00%</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            }
          </section>
        }
      }
    </div>
  `,
  styles: [
    `
      .dre-shell {
        display: flex;
        flex-direction: column;
        gap: 1.25rem;
        padding: 1.5rem;
        max-width: 1480px;
        margin: 0 auto;
      }
      .dre-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 1rem;
        flex-wrap: wrap;
      }
      .eyebrow {
        font-size: 0.75rem;
        text-transform: uppercase;
        letter-spacing: 0.08em;
        color: #0f766e;
        font-weight: 700;
      }
      h1 {
        margin: 0.25rem 0;
        font-size: 1.65rem;
        color: #0f172a;
      }
      .subtitle {
        margin: 0;
        color: #475569;
        max-width: 820px;
        font-size: 0.92rem;
      }
      .header-actions {
        display: flex;
        gap: 0.5rem;
        align-items: center;
        flex-wrap: wrap;
      }
      .filter-card,
      .panel-card,
      .diagnostic-panel {
        background: #ffffff;
        border: 1px solid #e2e8f0;
        border-radius: 12px;
        padding: 1.25rem;
        box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
      }
      .filter-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
        gap: 0.85rem;
        align-items: end;
      }
      .form-field {
        display: flex;
        flex-direction: column;
        gap: 0.35rem;
      }
      .form-field label {
        font-size: 0.8rem;
        font-weight: 600;
        color: #334155;
      }
      .form-field input,
      .form-field select {
        padding: 0.5rem 0.65rem;
        border: 1px solid #cbd5e1;
        border-radius: 8px;
        font-size: 0.88rem;
        background: #fff;
      }
      .checkbox-group {
        justify-content: center;
        gap: 0.45rem;
      }
      .checkbox-label {
        display: flex;
        align-items: center;
        gap: 0.45rem;
        font-size: 0.82rem;
        cursor: pointer;
      }
      .period-banner {
        display: flex;
        justify-content: space-between;
        align-items: center;
        flex-wrap: wrap;
        gap: 0.75rem;
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 10px;
        padding: 0.75rem 1rem;
        font-size: 0.86rem;
        color: #334155;
      }
      .sep {
        margin: 0 0.5rem;
        color: #94a3b8;
      }
      .vertical-base-tag {
        background: #f0fdf4;
        color: #166534;
        padding: 0.25rem 0.65rem;
        border-radius: 6px;
        font-size: 0.8rem;
      }
      .kpi-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
        gap: 0.85rem;
      }
      .kpi-card {
        background: #ffffff;
        border: 1px solid #e2e8f0;
        border-radius: 12px;
        padding: 1rem;
        display: flex;
        flex-direction: column;
        gap: 0.3rem;
      }
      .kpi-card.highlight-neutral {
        border-left: 4px solid #0284c7;
      }
      .kpi-card.highlight-positive {
        border-left: 4px solid #16a34a;
        background: #f0fdf4;
      }
      .kpi-card.highlight-negative {
        border-left: 4px solid #dc2626;
        background: #fef2f2;
      }
      .kpi-label {
        font-size: 0.78rem;
        color: #64748b;
        font-weight: 600;
      }
      .kpi-value {
        font-size: 1.32rem;
        color: #0f172a;
      }
      .kpi-sub {
        font-size: 0.76rem;
        color: #64748b;
      }
      .diagnostic-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 0.65rem;
      }
      .diagnostic-header h2 {
        margin: 0.15rem 0 0;
        font-size: 1.2rem;
      }
      .diagnostic-lead {
        font-size: 0.98rem;
        font-weight: 600;
        color: #0f172a;
        margin: 0 0 1rem;
      }
      .diagnostic-columns {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
        gap: 1rem;
      }
      .diag-box {
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 10px;
        padding: 0.95rem;
      }
      .diag-box h3 {
        margin: 0 0 0.45rem;
        font-size: 0.92rem;
        color: #1e293b;
      }
      .diag-box p {
        margin: 0;
        font-size: 0.86rem;
        color: #475569;
        line-height: 1.45;
      }
      .cash-reconciliation {
        display: flex;
        gap: 1rem;
        flex-wrap: wrap;
        margin-top: 0.75rem;
        padding-top: 0.65rem;
        border-top: 1px dashed #cbd5e1;
        font-size: 0.82rem;
      }
      .cash-reconciliation div {
        display: flex;
        flex-direction: column;
      }
      .factors-row {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
        gap: 0.75rem;
        margin-top: 1rem;
      }
      .factor-chip {
        border: 1px solid #e2e8f0;
        border-radius: 10px;
        padding: 0.8rem;
        background: #fff;
      }
      .factor-top {
        display: flex;
        justify-content: space-between;
        font-size: 0.78rem;
        color: #64748b;
      }
      .factor-title {
        font-weight: 700;
        font-size: 0.9rem;
        margin: 0.25rem 0;
        color: #0f172a;
      }
      .factor-exp {
        margin: 0;
        font-size: 0.8rem;
        color: #475569;
      }
      .clean-list {
        margin: 0;
        padding-left: 1.15rem;
        font-size: 0.84rem;
        color: #334155;
        display: flex;
        flex-direction: column;
        gap: 0.35rem;
      }
      .unclassified-cta {
        margin-top: 0.75rem;
        padding: 0.65rem;
        background: #fffbeb;
        border: 1px solid #fde68a;
        border-radius: 8px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 0.75rem;
        flex-wrap: wrap;
        font-size: 0.82rem;
      }
      .tabs-bar {
        display: flex;
        gap: 0.5rem;
        border-bottom: 2px solid #e2e8f0;
        padding-bottom: 0.25rem;
        flex-wrap: wrap;
      }
      .tab-btn {
        background: transparent;
        border: none;
        padding: 0.6rem 1rem;
        font-size: 0.9rem;
        font-weight: 600;
        color: #64748b;
        cursor: pointer;
        border-radius: 8px 8px 0 0;
      }
      .tab-btn.active {
        color: #0f766e;
        background: #f0fdfa;
        border-bottom: 3px solid #0f766e;
      }
      .tab-counter {
        background: #0f766e;
        color: #fff;
        font-size: 0.72rem;
        padding: 0.1rem 0.45rem;
        border-radius: 999px;
        margin-left: 0.35rem;
      }
      .panel-toolbar {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 1rem;
        flex-wrap: wrap;
        margin-bottom: 1rem;
      }
      .panel-toolbar h2 {
        margin: 0;
        font-size: 1.15rem;
      }
      .toolbar-actions {
        display: flex;
        gap: 0.5rem;
        align-items: center;
        flex-wrap: wrap;
      }
      .toolbar-actions select,
      .toolbar-actions input {
        padding: 0.4rem 0.6rem;
        border: 1px solid #cbd5e1;
        border-radius: 6px;
        font-size: 0.84rem;
      }
      .table-responsive {
        overflow-x: auto;
      }
      .dre-table {
        width: 100%;
        border-collapse: collapse;
        font-size: 0.86rem;
      }
      .dre-table th,
      .dre-table td {
        padding: 0.65rem 0.75rem;
        border-bottom: 1px solid #e2e8f0;
        text-align: left;
      }
      .dre-table th {
        background: #f8fafc;
        font-weight: 700;
        color: #334155;
        font-size: 0.78rem;
        text-transform: uppercase;
      }
      .row-subtotal {
        background: #f8fafc;
        font-weight: 700;
      }
      .row-resultado {
        background: #f0fdfa;
        font-weight: 700;
        font-size: 0.92rem;
      }
      .dre-account-row {
        background: #fcfcfd;
        font-size: 0.82rem;
      }
      .account-indent-1 {
        padding-left: 2.2rem !important;
      }
      .account-indent-2 {
        padding-left: 3.4rem !important;
      }
      .account-code {
        font-family: monospace;
        font-weight: 700;
        color: #0f766e;
        margin-right: 0.45rem;
      }
      .line-title-wrap {
        display: flex;
        align-items: flex-start;
        gap: 0.45rem;
      }
      .expand-btn {
        border: 1px solid #cbd5e1;
        background: #fff;
        border-radius: 4px;
        width: 22px;
        height: 22px;
        font-size: 0.7rem;
        cursor: pointer;
        line-height: 1;
      }
      .expand-placeholder {
        display: inline-block;
        width: 22px;
      }
      .line-title {
        font-weight: 600;
        color: #0f172a;
      }
      .line-explanation {
        font-size: 0.75rem;
        color: #64748b;
      }
      .signal-pill {
        display: inline-block;
        min-width: 28px;
        text-align: center;
        padding: 0.15rem 0.35rem;
        border-radius: 6px;
        font-weight: 700;
        font-size: 0.78rem;
      }
      .sig-plus {
        background: #dcfce7;
        color: #166534;
      }
      .sig-minus {
        background: #fee2e2;
        color: #991b1b;
      }
      .sig-equal {
        background: #e0f2fe;
        color: #075985;
      }
      .text-right {
        text-align: right !important;
      }
      .text-center {
        text-align: center !important;
      }
      .font-mono {
        font-variant-numeric: tabular-nums;
      }
      .text-success {
        color: #15803d !important;
      }
      .text-danger {
        color: #b91c1c !important;
      }
      .muted {
        color: #64748b;
      }
      .btn {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        padding: 0.5rem 0.9rem;
        border-radius: 8px;
        font-weight: 600;
        font-size: 0.85rem;
        cursor: pointer;
        text-decoration: none;
        border: 1px solid transparent;
      }
      .btn-primary {
        background: #0f766e;
        color: #fff;
      }
      .btn-secondary {
        background: #f1f5f9;
        color: #0f172a;
        border-color: #cbd5e1;
      }
      .btn-outline {
        background: #fff;
        color: #0f766e;
        border-color: #0f766e;
      }
      .btn-link {
        background: transparent;
        color: #0284c7;
        text-decoration: underline;
      }
      .btn-sm {
        padding: 0.35rem 0.65rem;
        font-size: 0.78rem;
      }
      .btn-xs {
        padding: 0.2rem 0.5rem;
        font-size: 0.74rem;
      }
      .badge {
        display: inline-block;
        padding: 0.25rem 0.6rem;
        border-radius: 999px;
        font-size: 0.75rem;
        font-weight: 700;
      }
      .badge-xs {
        padding: 0.12rem 0.45rem;
        font-size: 0.7rem;
      }
      .badge-success {
        background: #dcfce7;
        color: #166534;
      }
      .badge-warning {
        background: #fef3c7;
        color: #92400e;
      }
      .badge-danger {
        background: #fee2e2;
        color: #991b1b;
      }
      .badge-neutral {
        background: #f1f5f9;
        color: #334155;
      }
      .badge-subtle {
        background: #e0f2fe;
        color: #0369a1;
        margin-left: 0.35rem;
      }
      .alert {
        padding: 0.85rem 1rem;
        border-radius: 8px;
        font-size: 0.88rem;
      }
      .alert-danger {
        background: #fef2f2;
        border: 1px solid #fecaca;
        color: #991b1b;
      }
      .alert-success {
        background: #f0fdf4;
        border: 1px solid #bbf7d0;
        color: #166534;
      }
      .empty-cell {
        text-align: center !important;
        padding: 2rem !important;
        color: #64748b;
      }
    `
  ]
})
export class FinanceDrePage implements OnInit {
  private readonly api = inject(ApexApiService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly abaAtiva = signal<DreAba>('demonstracao');
  readonly carregando = signal(false);
  readonly exportando = signal(false);
  readonly erro = signal('');
  readonly mensagemSucesso = signal('');

  // Parâmetros de filtro da DRE
  readonly tipoPeriodo = signal<DreTipoPeriodo>('MENSAL');
  readonly competencia = signal<string>(this.defaultYearMonth());
  readonly ano = signal<number>(new Date().getFullYear());
  readonly trimestre = signal<number>(Math.floor(new Date().getMonth() / 3) + 1);
  readonly semestre = signal<number>(new Date().getMonth() < 6 ? 1 : 2);
  readonly dataInicio = signal<string>(this.defaultMonthStart());
  readonly dataFim = signal<string>(this.defaultToday());
  readonly compararPeriodoAnterior = signal<boolean>(true);
  readonly centroCustoId = signal<number | null>(null);
  readonly consolidarFiliais = signal<boolean>(false);
  readonly mesesEvolucao = signal<number>(6);

  // Dados carregados da API
  readonly centrosCusto = signal<CentroCustoResponse[]>([]);
  readonly dre = signal<DreGerencialResponse | null>(null);
  readonly drillDown = signal<DreDrillDownResponse | null>(null);
  readonly evolucao = signal<DreEvolucaoResponse | null>(null);
  readonly comparativoFiliais = signal<DreComparativoFiliaisResponse | null>(null);

  // Estado de UI
  readonly linhasExpandidas = signal<Set<number>>(new Set([1, 6, 8, 9, 10, 11, 12, 14]));
  readonly filtroOrigemDrillDown = signal<string>('');
  readonly buscaDrillDown = signal<string>('');

  readonly isReadOnlyAuditor = computed(() => {
    const roles = this.auth.user()?.roles ?? [];
    return roles.includes('ROLE_AUDITOR') && !this.canExport();
  });

  readonly canExport = computed(() => {
    const roles = this.auth.user()?.roles ?? [];
    return roles.some((r) =>
      [
        'ROLE_SYSADMIN',
        'ROLE_DONO_GERENTE',
        'ROLE_DONO',
        'ROLE_GERENTE',
        'ROLE_FINANCEIRO',
        'ROLE_FINANCAS',
        'ROLE_CONTADOR',
        'ROLE_ADMIN',
        'ROLE_ADMINISTRACAO'
      ].includes(r)
    );
  });

  readonly lancamentosFiltrados = computed(() => {
    const lista = this.drillDown()?.lancamentos ?? [];
    const origem = this.filtroOrigemDrillDown().trim().toUpperCase();
    const termo = this.buscaDrillDown().trim().toLowerCase();
    return lista.filter((item) => {
      if (origem && item.origemTipo !== origem) {
        return false;
      }
      if (!termo) {
        return true;
      }
      return (
        item.documentoReferencia.toLowerCase().includes(termo) ||
        item.descricao.toLowerCase().includes(termo) ||
        item.planoContaNome.toLowerCase().includes(termo) ||
        item.planoContaCodigo.toLowerCase().includes(termo)
      );
    });
  });

  ngOnInit(): void {
    this.carregarCentrosCusto();
    this.atualizarTudo();
  }

  onTipoPeriodoChange(novoTipo: DreTipoPeriodo): void {
    this.tipoPeriodo.set(novoTipo);
  }

  selecionarAba(aba: DreAba): void {
    this.abaAtiva.set(aba);
    if (aba === 'lancamentos' && !this.drillDown()) {
      this.carregarDrillDown({});
    } else if (aba === 'evolucao' && !this.evolucao()) {
      this.carregarEvolucao();
    } else if (aba === 'filiais' && !this.comparativoFiliais()) {
      this.carregarComparativoFiliais();
    }
  }

  atualizarTudo(): void {
    this.erro.set('');
    this.mensagemSucesso.set('');
    this.carregando.set(true);

    const query = this.buildQueryParams();
    this.api.getDreGerencial(query).subscribe({
      next: (resp) => {
        this.dre.set(resp);
        this.carregando.set(false);
        this.carregarDrillDown({});
        this.carregarEvolucao();
        this.carregarComparativoFiliais();
      },
      error: (err) => {
        this.carregando.set(false);
        this.erro.set(err?.error?.message ?? err?.message ?? 'Não foi possível apurar a DRE Gerencial.');
      }
    });
  }

  rastrearLinha(linha: DreLinha): void {
    this.abaAtiva.set('lancamentos');
    this.carregarDrillDown({ numeroLinha: linha.numeroLinha });
  }

  rastrearConta(linha: DreLinha, conta: DreContaDetalhe): void {
    this.abaAtiva.set('lancamentos');
    this.carregarDrillDown({
      numeroLinha: linha.numeroLinha,
      planoContaId: conta.idPlanoConta ?? undefined
    });
  }

  limparFiltroDrillDown(): void {
    this.filtroOrigemDrillDown.set('');
    this.buscaDrillDown.set('');
    this.carregarDrillDown({});
  }

  alterarMesesEvolucao(meses: number): void {
    this.mesesEvolucao.set(meses);
    this.carregarEvolucao();
  }

  exportar(formato: 'CSV' | 'JSON'): void {
    if (!this.canExport()) {
      return;
    }
    this.exportando.set(true);
    this.erro.set('');
    const query = this.buildQueryParams();

    if (formato === 'CSV') {
      this.api.exportarDreCsv(query).subscribe({
        next: (csvText) => {
          this.exportando.set(false);
          this.mensagemSucesso.set('Exportação CSV gerada e auditada com sucesso.');
          this.triggerDownload(csvText, `dre-gerencial-${this.competencia() || 'periodo'}.csv`, 'text/csv;charset=utf-8;');
        },
        error: (err) => {
          this.exportando.set(false);
          this.erro.set(err?.error?.message ?? 'Falha ao exportar DRE em CSV.');
        }
      });
      return;
    }

    this.api.exportarDreJson(query).subscribe({
      next: (jsonDto) => {
        this.exportando.set(false);
        this.mensagemSucesso.set('Exportação JSON gerada e auditada com sucesso.');
        this.triggerDownload(
          JSON.stringify(jsonDto, null, 2),
          `dre-gerencial-${this.competencia() || 'periodo'}.json`,
          'application/json;charset=utf-8;'
        );
      },
      error: (err) => {
        this.exportando.set(false);
        this.erro.set(err?.error?.message ?? 'Falha ao exportar DRE em JSON.');
      }
    });
  }

  toggleLinha(numeroLinha: number): void {
    const atual = new Set(this.linhasExpandidas());
    if (atual.has(numeroLinha)) {
      atual.delete(numeroLinha);
    } else {
      atual.add(numeroLinha);
    }
    this.linhasExpandidas.set(atual);
  }

  isLinhaExpandida(numeroLinha: number): boolean {
    return this.linhasExpandidas().has(numeroLinha);
  }

  expandirTodasLinhas(): void {
    const todas = new Set<number>((this.dre()?.linhas ?? []).map((l) => l.numeroLinha));
    this.linhasExpandidas.set(todas);
  }

  recolherTodasLinhas(): void {
    this.linhasExpandidas.set(new Set());
  }

  navegarParaOrigem(item: DreLancamentoOrigem): void {
    const rota = item.rotaDetalheFrontend || '/finance/foundation';
    void this.router.navigateByUrl(rota);
  }

  formatCurrency(val: number | null | undefined): string {
    const num = Number(val ?? 0);
    return num.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  formatSignedCurrency(val: number | null | undefined): string {
    const num = Number(val ?? 0);
    const prefix = num > 0 ? '+ ' : '';
    return prefix + num.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  formatPercent(val: number | null | undefined): string {
    if (val === null || val === undefined) {
      return '—';
    }
    const num = Number(val);
    return `${num.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}%`;
  }

  calcAv(valor: number, receitaLiquida: number): number | null {
    if (!receitaLiquida) {
      return null;
    }
    return (valor / receitaLiquida) * 100;
  }

  formatSituacao(sit: string): string {
    const map: Record<string, string> = {
      LUCRO_SAUDAVEL: 'Lucro Saudável',
      LUCRO_APERTADO: 'Lucro Apertado',
      EQUILIBRIO: 'No Ponto de Equilíbrio',
      PREJUIZO: 'Prejuízo no Período',
      SEM_MOVIMENTO: 'Sem Movimentação'
    };
    return map[sit] ?? sit;
  }

  situacaoBadgeClass(sit: string): string {
    switch (sit) {
      case 'LUCRO_SAUDAVEL':
        return 'badge-success';
      case 'LUCRO_APERTADO':
      case 'EQUILIBRIO':
        return 'badge-warning';
      case 'PREJUIZO':
        return 'badge-danger';
      default:
        return 'badge-neutral';
    }
  }

  sinalClass(sinal: string): string {
    if (sinal === '+') return 'sig-plus';
    if (sinal === '-') return 'sig-minus';
    return 'sig-equal';
  }

  valorClass(linha: DreLinha): string {
    if (linha.tipoLinha === 'RESULTADO' || linha.tipoLinha === 'SUBTOTAL') {
      return linha.valor >= 0 ? 'text-success' : 'text-danger';
    }
    if (linha.sinalDemonstracao === '-' && linha.valor > 0) {
      return 'text-danger';
    }
    return '';
  }

  statusVariacaoClass(status: string): string {
    if (status === 'FAVORAVEL') return 'badge-success';
    if (status === 'DESFAVORAVEL') return 'badge-danger';
    return 'badge-neutral';
  }

  private carregarCentrosCusto(): void {
    this.api.listCentrosCusto(true).subscribe({
      next: (lista: CentroCustoResponse[]) => this.centrosCusto.set(lista ?? []),
      error: () => this.centrosCusto.set([])
    });
  }

  private carregarDrillDown(filtroExtra: { numeroLinha?: number; planoContaId?: number }): void {
    const query = {
      ...this.buildQueryParams(),
      numeroLinha: filtroExtra.numeroLinha,
      planoContaId: filtroExtra.planoContaId
    };
    this.api.getDreLancamentos(query).subscribe({
      next: (resp) => this.drillDown.set(resp),
      error: () => this.drillDown.set(null)
    });
  }

  private carregarEvolucao(): void {
    this.api
      .getDreEvolucao({
        competenciaFim: this.tipoPeriodo() === 'MENSAL' ? this.competencia() : undefined,
        meses: this.mesesEvolucao(),
        centroCustoId: this.centroCustoId(),
        consolidarFiliais: this.consolidarFiliais()
      })
      .subscribe({
        next: (resp) => this.evolucao.set(resp),
        error: () => this.evolucao.set(null)
      });
  }

  private carregarComparativoFiliais(): void {
    this.api.getDreComparativoFiliais(this.buildQueryParams()).subscribe({
      next: (resp) => this.comparativoFiliais.set(resp),
      error: () => this.comparativoFiliais.set(null)
    });
  }

  private buildQueryParams(): DreQueryParams {
    const tp = this.tipoPeriodo();
    return {
      tipoPeriodo: tp,
      competencia: tp === 'MENSAL' ? this.competencia() : undefined,
      ano: tp === 'TRIMESTRAL' || tp === 'SEMESTRAL' || tp === 'ANUAL' ? this.ano() : undefined,
      trimestre: tp === 'TRIMESTRAL' ? this.trimestre() : undefined,
      semestre: tp === 'SEMESTRAL' ? this.semestre() : undefined,
      dataInicio: tp === 'PERSONALIZADO' ? this.dataInicio() : undefined,
      dataFim: tp === 'PERSONALIZADO' ? this.dataFim() : undefined,
      compararPeriodoAnterior: this.compararPeriodoAnterior(),
      centroCustoId: this.centroCustoId(),
      consolidarFiliais: this.consolidarFiliais()
    };
  }

  private triggerDownload(content: string, filename: string, mimeType: string): void {
    try {
      const blob = new Blob([content], { type: mimeType });
      const url = globalThis.URL?.createObjectURL?.(blob);
      if (!url || typeof document === 'undefined') {
        return;
      }
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = filename;
      anchor.click();
      globalThis.URL.revokeObjectURL(url);
    } catch {
      // Ignorado em ambientes headless sem suporte a Blob URL
    }
  }

  private defaultYearMonth(): string {
    const now = new Date();
    const m = String(now.getMonth() + 1).padStart(2, '0');
    return `${now.getFullYear()}-${m}`;
  }

  private defaultMonthStart(): string {
    return `${this.defaultYearMonth()}-01`;
  }

  private defaultToday(): string {
    const now = new Date();
    const m = String(now.getMonth() + 1).padStart(2, '0');
    const d = String(now.getDate()).padStart(2, '0');
    return `${now.getFullYear()}-${m}-${d}`;
  }
}
