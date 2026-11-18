import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ApexApiService } from '../core/apex-api.service';
import { AuthService } from '../core/auth.service';
import {
  BalancoPatrimonialGerencialRequest,
  IndicadoresQueryParams,
  InvestimentosRetornoPainel,
  PainelInteligenciaAdministrativaResponse,
  ParametroInteligenciaAdministrativa,
  Produto,
  SimulacaoInvestimentoRequest,
  SimulacaoPrecificacaoRequest,
  SimulacaoPrecificacaoResponse
} from '../core/models';

type IndicatorsTab =
  | 'visao-geral'
  | 'rentabilidade'
  | 'ponto-equilibrio'
  | 'precificacao'
  | 'capital-giro'
  | 'liquidez-balanco'
  | 'investimentos'
  | 'diagnosticos';

@Component({
  selector: 'app-finance-indicators-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="ind-shell">
      <header class="ind-hero">
        <div class="ind-hero__main">
          <div class="ind-hero__badges">
            <span class="badge badge--accent">Lote 4 • Centro de Inteligência Administrativa</span>
            <span class="badge badge--outline">
              {{ painel()?.consolidadoEmpresa ? 'Consolidado Multiloja' : painel()?.filialNome || 'Escopo Filial Ativa' }}
            </span>
          </div>
          <h1>Cálculos Administrativos, Margens, Capital de Giro & Saúde Financeira</h1>
          <p class="ind-hero__subtitle">
            Motor de indicadores alimentado em tempo real pela DRE Gerencial (Lote 2), Central de Fluxo de Caixa (Lote 3),
            Estoque, Vendas PDV e Compras, com controle explícito de confiabilidade dos dados.
          </p>
        </div>

        <div class="ind-hero__actions">
          <a routerLink="/finance/dre" class="btn btn--ghost">DRE Gerencial</a>
          <a routerLink="/finance/cash-flow" class="btn btn--ghost">Fluxo de Caixa</a>
          <a routerLink="/finance/foundation" class="btn btn--ghost">Fundação Financeira</a>
          <button type="button" class="btn btn--primary" (click)="carregarPainel()" [disabled]="carregando()">
            {{ carregando() ? 'Calculando...' : 'Recalcular Painel' }}
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
      <section class="card ind-filters" aria-label="Filtros do painel administrativo">
        <div class="ind-filters__grid">
          <label class="field">
            <span>Competência (Mês/Ano)</span>
            <input type="month" [(ngModel)]="filtroCompetencia" (change)="onCompetenciaChange()" />
          </label>

          <label class="field">
            <span>Data Inicial (opcional)</span>
            <input type="date" [(ngModel)]="filtroDataInicio" (change)="carregarPainel()" />
          </label>

          <label class="field">
            <span>Data Final (opcional)</span>
            <input type="date" [(ngModel)]="filtroDataFim" (change)="carregarPainel()" />
          </label>

          <label class="field field--checkbox">
            <input type="checkbox" [(ngModel)]="filtroConsolidarFiliais" (change)="carregarPainel()" />
            <span>Consolidar todas as filiais da empresa</span>
          </label>
        </div>
      </section>

      @if (painel(); as p) {
        <!-- KPIs Executivos -->
        <section class="ind-kpis">
          <article class="kpi-card">
            <span class="kpi-card__label">Receita Líquida Operacional</span>
            <strong class="kpi-card__value">{{ formatCurrency(p.rentabilidade.receitaLiquidaTotal) }}</strong>
            <span class="kpi-card__meta">Bruta: {{ formatCurrency(p.rentabilidade.receitaBrutaTotal) }}</span>
          </article>

          <article class="kpi-card kpi-card--highlight">
            <span class="kpi-card__label">Margem de Contribuição Total</span>
            <strong class="kpi-card__value">{{ formatCurrency(p.rentabilidade.margemContribuicaoTotalValor) }}</strong>
            <span class="kpi-card__meta">
              {{ formatPercent(p.rentabilidade.margemContribuicaoTotalPercentual) }} • Meta:
              {{ formatPercent(p.parametrosVigentes.margemContribuicaoMinimaPercentual) }}
            </span>
          </article>

          <article class="kpi-card">
            <span class="kpi-card__label">Ponto de Equilíbrio Contábil (PEC)</span>
            <strong class="kpi-card__value">{{ formatCurrencyOrNA(p.pontoEquilibrio.pontoEquilibrioContabilReceita) }}</strong>
            <span class="kpi-card__meta">
              Segurança: {{ formatPercentOrNA(p.pontoEquilibrio.margemSegurancaContabilPercentual) }}
            </span>
          </article>

          <article class="kpi-card">
            <span class="kpi-card__label">Necessidade de Capital de Giro (NCG)</span>
            <strong class="kpi-card__value">{{ formatCurrency(p.capitalGiro.necessidadeCapitalGiroNcg) }}</strong>
            <span class="kpi-card__meta">
              CGL: {{ formatCurrency(p.capitalGiro.capitalGiroLiquidoCgl) }} | ST: {{ formatCurrency(p.capitalGiro.saldoTesourariaSt) }}
            </span>
          </article>

          <article class="kpi-card">
            <span class="kpi-card__label">Ciclo Financeiro</span>
            <strong class="kpi-card__value">{{ formatDaysOrNA(p.prazosCiclos.cicloFinanceiroDias) }}</strong>
            <span class="kpi-card__meta">
              PME {{ formatDaysOrNA(p.prazosCiclos.pmePrazoMedioEstocagemDias) }} +
              PMR {{ formatDaysOrNA(p.prazosCiclos.pmrPrazoMedioRecebimentoDias) }} -
              PMP {{ formatDaysOrNA(p.prazosCiclos.pmpPrazoMedioPagamentoDias) }}
            </span>
          </article>

          <article class="kpi-card">
            <span class="kpi-card__label">Liquidez & Saúde Patrimonial</span>
            <strong class="kpi-card__value">
              {{ p.liquidezEndividamento.liquidezCorrente !== null ? formatIndex(p.liquidezEndividamento.liquidezCorrente) : 'Pendente Balanço' }}
            </strong>
            <span class="kpi-card__meta">
              Imediata: {{ formatIndexOrNA(p.liquidezEndividamento.liquidezImediata) }} •
              {{ p.liquidezEndividamento.statusConfiabilidadePatrimonial }}
            </span>
          </article>
        </section>

        <!-- Abas de Navegação -->
        <nav class="ind-tabs" aria-label="Áreas do Centro de Inteligência Administrativa">
          <button type="button" [class.active]="abaAtiva() === 'visao-geral'" (click)="abaAtiva.set('visao-geral')">
            Visão Geral & Catálogo ({{ p.catalogoIndicadores.length }})
          </button>
          <button type="button" [class.active]="abaAtiva() === 'rentabilidade'" (click)="abaAtiva.set('rentabilidade')">
            Margem & Rentabilidade ({{ p.rentabilidade.porProduto.length }})
          </button>
          <button type="button" [class.active]="abaAtiva() === 'ponto-equilibrio'" (click)="abaAtiva.set('ponto-equilibrio')">
            Pontos de Equilíbrio
          </button>
          <button type="button" [class.active]="abaAtiva() === 'precificacao'" (click)="abaAtiva.set('precificacao')">
            Markup & Precificação
          </button>
          <button type="button" [class.active]="abaAtiva() === 'capital-giro'" (click)="abaAtiva.set('capital-giro')">
            Capital de Giro & Ciclos
          </button>
          <button type="button" [class.active]="abaAtiva() === 'liquidez-balanco'" (click)="abaAtiva.set('liquidez-balanco')">
            Liquidez, Endividamento & Balanço
          </button>
          <button type="button" [class.active]="abaAtiva() === 'investimentos'" (click)="abaAtiva.set('investimentos')">
            ROI, ROE & Investimentos
          </button>
          <button type="button" [class.active]="abaAtiva() === 'diagnosticos'" (click)="abaAtiva.set('diagnosticos')">
            Diagnósticos & Parâmetros ({{ p.diagnosticosExplicaveis.length }})
          </button>
        </nav>

        <!-- ABA 1: VISÃO GERAL & CATÁLOGO -->
        @if (abaAtiva() === 'visao-geral') {
          <section class="tab-panel">
            <div class="card">
              <div class="card__header-row">
                <div>
                  <h2>Catálogo Auditável de Indicadores ({{ p.competencia }})</h2>
                  <p class="text-muted">
                    Todos os indicadores exibem fórmula explícita, variáveis reais usadas, fonte e situação de confiabilidade — sem zeros falsos quando dados faltam.
                  </p>
                </div>
                <select [(ngModel)]="filtroCategoriaCatalogo">
                  <option value="TODAS">Todas as categorias</option>
                  <option value="RENTABILIDADE">P0 • Rentabilidade</option>
                  <option value="PONTO_EQUILIBRIO">P0 • Ponto de Equilíbrio</option>
                  <option value="PRECIFICACAO">P0 • Precificação</option>
                  <option value="CAPITAL_GIRO">P1 • Capital de Giro</option>
                  <option value="PRAZOS_CICLOS">P1 • Prazos e Ciclos</option>
                  <option value="LIQUIDEZ">P2 • Liquidez</option>
                  <option value="ENDIVIDAMENTO">P2 • Endividamento</option>
                  <option value="INVESTIMENTO">P2 • Investimentos & Retorno</option>
                </select>
              </div>

              <div class="catalog-grid">
                @for (ind of indicadoresCatalogoFiltrados(); track ind.codigo) {
                  <article class="indicator-card">
                    <div class="indicator-card__top">
                      <span class="badge badge--outline">{{ ind.prioridade }} • {{ ind.categoriaIndicador }}</span>
                      <span class="badge" [ngClass]="confiabilidadeBadgeClass(ind.statusConfiabilidade)">
                        {{ ind.statusConfiabilidade }}
                      </span>
                    </div>
                    <h3>{{ ind.nome }}</h3>
                    <div class="indicator-card__result">
                      @if (ind.resultado !== null && ind.resultado !== undefined) {
                        <strong>{{ formatByUnit(ind.resultado, ind.unidadeMedida) }}</strong>
                      } @else {
                        <span class="text-danger">Indisponível ({{ ind.statusConfiabilidade }})</span>
                      }
                    </div>
                    <p class="indicator-card__desc">{{ ind.descricao }}</p>
                    <div class="indicator-card__formula">
                      <small>Fórmula:</small>
                      <code>{{ ind.formula }}</code>
                    </div>
                    <p class="indicator-card__interp">{{ ind.interpretacaoGerencial }}</p>
                    <small class="text-muted d-block">Fonte: {{ ind.fonteDados }} | Limitações: {{ ind.limitacoes }}</small>
                    @if (ind.dependenciasFaltantes.length > 0) {
                      <div class="dep-list">
                        @for (dep of ind.dependenciasFaltantes; track dep) {
                          <small class="text-danger d-block">• Falta: {{ dep }}</small>
                        }
                      </div>
                    }
                  </article>
                }
              </div>
            </div>
          </section>
        }

        <!-- ABA 2: MARGEM DE CONTRIBUIÇÃO & RENTABILIDADE (P0) -->
        @if (abaAtiva() === 'rentabilidade') {
          <section class="tab-panel">
            <div class="card">
              <h2>Composição da Margem de Contribuição Global</h2>
              <p class="text-muted">
                Receita Líquida já deduz devoluções, descontos e tributos sobre vendas da DRE (Linha 5), evitando dupla dedução.
                Taxas de cartão e comissões variáveis somam-se ao CMV para apurar a contribuição efetiva.
              </p>
              <div class="summary-strip">
                <div><span>Receita Bruta</span><strong>{{ formatCurrency(p.rentabilidade.receitaBrutaTotal) }}</strong></div>
                <div><span>(-) Deduções / Tributos Venda</span><strong>{{ formatCurrency(p.rentabilidade.deducoesTributosVendasTotal) }}</strong></div>
                <div><span>(=) Receita Líquida</span><strong>{{ formatCurrency(p.rentabilidade.receitaLiquidaTotal) }}</strong></div>
                <div><span>(-) CMV</span><strong>{{ formatCurrency(p.rentabilidade.cmvTotal) }}</strong></div>
                <div><span>(-) Despesas Variáveis</span><strong>{{ formatCurrency(p.rentabilidade.custosDespesasVariaveisTotais - p.rentabilidade.cmvTotal) }}</strong></div>
                <div class="summary-strip__highlight">
                  <span>(=) Margem Contribuição</span>
                  <strong>{{ formatCurrency(p.rentabilidade.margemContribuicaoTotalValor) }} ({{ formatPercent(p.rentabilidade.margemContribuicaoTotalPercentual) }})</strong>
                </div>
              </div>
            </div>

            @if (p.rentabilidade.alertasAltoVolumeBaixaMargem.length > 0) {
              <div class="card card--warning">
                <h3>Produtos de Atenção: Alto Faturamento com Baixa Contribuição ou Margem Negativa</h3>
                <ul class="clean-list">
                  @for (item of p.rentabilidade.alertasAltoVolumeBaixaMargem; track item.referenciaId) {
                    <li>
                      <strong>{{ item.nome }}</strong> — Fat.: {{ formatCurrency(item.receitaLiquida) }}
                      ({{ formatPercent(item.participacaoFaturamentoPercentual) }}) |
                      MC: {{ formatCurrency(item.margemContribuicaoValor) }} ({{ formatPercent(item.margemContribuicaoPercentual) }}) —
                      <span class="text-danger">{{ item.recomendacaoItem }}</span>
                    </li>
                  }
                </ul>
              </div>
            }

            <div class="card">
              <h3>Margem de Contribuição por Produto</h3>
              @if (p.rentabilidade.porProduto.length === 0) {
                <p class="text-muted">Nenhuma venda finalizada encontrada neste período.</p>
              } @else {
                <div class="table-wrap">
                  <table class="ind-table">
                    <thead>
                      <tr>
                        <th>Produto</th>
                        <th>Categoria</th>
                        <th class="text-right">Qtd Líq.</th>
                        <th class="text-right">Receita Líq.</th>
                        <th class="text-right">CMV</th>
                        <th class="text-right">Taxas + Variáveis</th>
                        <th class="text-right">MC (R$)</th>
                        <th class="text-right">MC (%)</th>
                        <th class="text-right">Part. Fat.</th>
                        <th>Classificação</th>
                      </tr>
                    </thead>
                    <tbody>
                      @for (prod of p.rentabilidade.porProduto; track prod.referenciaId) {
                        <tr>
                          <td><strong>{{ prod.nome }}</strong></td>
                          <td>{{ prod.categoriaNome }}</td>
                          <td class="text-right">{{ prod.quantidadeVendidaLiquida }}</td>
                          <td class="text-right">{{ formatCurrency(prod.receitaLiquida) }}</td>
                          <td class="text-right">{{ formatCurrency(prod.cmvTotal) }}</td>
                          <td class="text-right">{{ formatCurrency(prod.taxasCartaoAlocadas + prod.comissoesFretesVariaveis) }}</td>
                          <td class="text-right" [class.text-danger]="prod.margemContribuicaoValor < 0">
                            <strong>{{ formatCurrency(prod.margemContribuicaoValor) }}</strong>
                          </td>
                          <td class="text-right">{{ formatPercent(prod.margemContribuicaoPercentual) }}</td>
                          <td class="text-right">{{ formatPercent(prod.participacaoFaturamentoPercentual) }}</td>
                          <td>
                            <span class="badge" [ngClass]="rentabilidadeBadgeClass(prod.classificacaoRentabilidade)">
                              {{ prod.classificacaoRentabilidade }}
                            </span>
                          </td>
                        </tr>
                      }
                    </tbody>
                  </table>
                </div>
              }
            </div>

            <div class="grid-2">
              <div class="card">
                <h3>Por Categoria</h3>
                <div class="table-wrap">
                  <table class="ind-table">
                    <thead>
                      <tr>
                        <th>Categoria</th>
                        <th class="text-right">Receita Líq.</th>
                        <th class="text-right">MC (R$)</th>
                        <th class="text-right">MC (%)</th>
                      </tr>
                    </thead>
                    <tbody>
                      @for (cat of p.rentabilidade.porCategoria; track cat.referenciaId) {
                        <tr>
                          <td><strong>{{ cat.nome }}</strong></td>
                          <td class="text-right">{{ formatCurrency(cat.receitaLiquida) }}</td>
                          <td class="text-right">{{ formatCurrency(cat.margemContribuicaoValor) }}</td>
                          <td class="text-right">{{ formatPercent(cat.margemContribuicaoPercentual) }}</td>
                        </tr>
                      }
                    </tbody>
                  </table>
                </div>
              </div>

              <div class="card">
                <h3>Por Filial</h3>
                <div class="table-wrap">
                  <table class="ind-table">
                    <thead>
                      <tr>
                        <th>Filial</th>
                        <th class="text-right">Receita Líq.</th>
                        <th class="text-right">MC (R$)</th>
                        <th class="text-right">MC (%)</th>
                      </tr>
                    </thead>
                    <tbody>
                      @for (fil of p.rentabilidade.porFilial; track fil.referenciaId) {
                        <tr>
                          <td><strong>{{ fil.nome }}</strong></td>
                          <td class="text-right">{{ formatCurrency(fil.receitaLiquida) }}</td>
                          <td class="text-right">{{ formatCurrency(fil.margemContribuicaoValor) }}</td>
                          <td class="text-right">{{ formatPercent(fil.margemContribuicaoPercentual) }}</td>
                        </tr>
                      }
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </section>
        }

        <!-- ABA 3: PONTOS DE EQUILÍBRIO (P0) -->
        @if (abaAtiva() === 'ponto-equilibrio') {
          <section class="tab-panel">
            <div class="card">
              <div class="card__header-row">
                <div>
                  <h2>Tríade do Ponto de Equilíbrio (Contábil, Financeiro e Econômico)</h2>
                  <p class="text-muted">{{ p.pontoEquilibrio.metodologiaExplicita }}</p>
                </div>
                <span class="badge" [ngClass]="confiabilidadeBadgeClass(p.pontoEquilibrio.statusConfiabilidade)">
                  {{ p.pontoEquilibrio.statusConfiabilidade }}
                </span>
              </div>

              <div class="pe-cards-3">
                <article class="pe-box">
                  <span class="badge badge--outline">1. PEC — Contábil</span>
                  <h3>{{ formatCurrencyOrNA(p.pontoEquilibrio.pontoEquilibrioContabilReceita) }}</h3>
                  <small>Mix estimado: {{ formatUnitsOrNA(p.pontoEquilibrio.pontoEquilibrioContabilUnidadesMix) }}</small>
                  <p>Cobre os custos e despesas fixas contábeis ({{ formatCurrency(p.pontoEquilibrio.custosDespesasFixasContabeis) }}).</p>
                </article>

                <article class="pe-box">
                  <span class="badge badge--outline">2. PEF — Financeiro (Desembolso)</span>
                  <h3>{{ formatCurrencyOrNA(p.pontoEquilibrio.pontoEquilibrioFinanceiroReceita) }}</h3>
                  <small>Mix estimado: {{ formatUnitsOrNA(p.pontoEquilibrio.pontoEquilibrioFinanceiroUnidadesMix) }}</small>
                  <p>
                    Base fixo de caixa: {{ formatCurrency(p.pontoEquilibrio.desembolsosFixosFinanceiros) }}
                    (exclui depreciação de {{ formatCurrency(p.pontoEquilibrio.despesasNaoDesembolsaveisDepreciacao) }} e
                    soma amortização principal de {{ formatCurrency(p.pontoEquilibrio.amortizacaoPrincipalDividasPeriodo) }}).
                  </p>
                </article>

                <article class="pe-box">
                  <span class="badge badge--outline">3. PEE — Econômico (Custo de Oportunidade)</span>
                  <h3>{{ formatCurrencyOrNA(p.pontoEquilibrio.pontoEquilibrioEconomicoReceita) }}</h3>
                  <small>Mix estimado: {{ formatUnitsOrNA(p.pontoEquilibrio.pontoEquilibrioEconomicoUnidadesMix) }}</small>
                  <p>
                    Soma remuneração mínima de {{ formatCurrency(p.pontoEquilibrio.remuneracaoMinimaCapitalValor) }}
                    ({{ formatPercent(p.pontoEquilibrio.taxaRetornoMinimoPeriodoPercentual) }} sobre capital de {{ formatCurrency(p.pontoEquilibrio.capitalInvestidoBase) }}).
                  </p>
                </article>
              </div>
            </div>

            <div class="grid-2">
              <div class="card">
                <h3>Ponto de Equilíbrio por Filial</h3>
                <table class="ind-table">
                  <thead>
                    <tr>
                      <th>Filial</th>
                      <th class="text-right">Receita Atual</th>
                      <th class="text-right">PEC (R$)</th>
                      <th class="text-right">PEF (R$)</th>
                      <th class="text-right">PEE (R$)</th>
                      <th>Situação</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (item of p.pontoEquilibrio.porFilial; track item.escopoId) {
                      <tr>
                        <td><strong>{{ item.escopoNome }}</strong></td>
                        <td class="text-right">{{ formatCurrency(item.receitaLiquidaAtual) }}</td>
                        <td class="text-right">{{ formatCurrencyOrNA(item.pontoEquilibrioContabilReceita) }}</td>
                        <td class="text-right">{{ formatCurrencyOrNA(item.pontoEquilibrioFinanceiroReceita) }}</td>
                        <td class="text-right">{{ formatCurrencyOrNA(item.pontoEquilibrioEconomicoReceita) }}</td>
                        <td>{{ item.situacaoEquilibrio }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>

              <div class="card">
                <h3>Ponto de Equilíbrio por Categoria (Mix Rateado)</h3>
                <table class="ind-table">
                  <thead>
                    <tr>
                      <th>Categoria</th>
                      <th class="text-right">Receita Atual</th>
                      <th class="text-right">PEC (R$)</th>
                      <th class="text-right">Margem Seg.</th>
                      <th>Situação</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (item of p.pontoEquilibrio.porCategoria; track item.escopoId) {
                      <tr>
                        <td><strong>{{ item.escopoNome }}</strong></td>
                        <td class="text-right">{{ formatCurrency(item.receitaLiquidaAtual) }}</td>
                        <td class="text-right">{{ formatCurrencyOrNA(item.pontoEquilibrioContabilReceita) }}</td>
                        <td class="text-right">{{ formatPercentOrNA(item.margemSegurancaPercentual) }}</td>
                        <td>{{ item.situacaoEquilibrio }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        }

        <!-- ABA 4: MARKUP & PRECIFICAÇÃO INTELIGENTE (P0) -->
        @if (abaAtiva() === 'precificacao') {
          <section class="tab-panel">
            <div class="grid-2">
              <div class="card">
                <h2>Simulador de Formação de Preços (Read-Only)</h2>
                <p class="text-muted">
                  Simule custos acessórios, alíquotas de venda, margem desejada e promoções sem alterar o catálogo. A aplicação no cadastro de produto só ocorre com confirmação explícita.
                </p>

                <label class="field" style="margin-bottom: 0.85rem;">
                  <span>Produto do Catálogo (para carregar custo/preço real e habilitar aplicação autorizada)</span>
                  <select [(ngModel)]="precifForm.produtoId" (ngModelChange)="onSelecionarProdutoPrecificacao($event)">
                    <option [ngValue]="undefined">Simulação Avulsa (Sem vínculo ao catálogo)</option>
                    @for (prod of produtosCatalogo(); track prod.idProduto) {
                      <option [ngValue]="prod.idProduto">
                        {{ prod.descricao }} (Custo: {{ formatCurrency(prod.custo) }} • Venda atual: {{ formatCurrency(prod.valorVenda) }})
                      </option>
                    }
                  </select>
                </label>

                <div class="form-row">
                  <label class="field">
                    <span>Custo Aquisição Base (R$)</span>
                    <input type="number" step="0.01" [(ngModel)]="precifForm.custoAquisicaoBase" />
                  </label>
                  <label class="field">
                    <span>Frete Compra (R$)</span>
                    <input type="number" step="0.01" [(ngModel)]="precifForm.freteCompraValor" />
                  </label>
                  <label class="field">
                    <span>Seguro Compra (R$)</span>
                    <input type="number" step="0.01" [(ngModel)]="precifForm.seguroCompraValor" />
                  </label>
                </div>

                <div class="form-row">
                  <label class="field">
                    <span>Impostos Não Recuperáveis (R$)</span>
                    <input type="number" step="0.01" [(ngModel)]="precifForm.impostosNaoRecuperaveisValor" />
                  </label>
                  <label class="field">
                    <span>Despesas Acessórias (R$)</span>
                    <input type="number" step="0.01" [(ngModel)]="precifForm.despesasAcessoriasValor" />
                  </label>
                  <label class="field">
                    <span>Preço Praticado Atual (R$)</span>
                    <input type="number" step="0.01" [(ngModel)]="precifForm.precoPraticadoAtual" />
                  </label>
                </div>

                <div class="form-row">
                  <label class="field">
                    <span>Tributos s/ Venda (%)</span>
                    <input type="number" step="0.1" [(ngModel)]="precifForm.aliquotaTributosVendaPercentual" />
                  </label>
                  <label class="field">
                    <span>Taxa Cartão Média (%)</span>
                    <input type="number" step="0.1" [(ngModel)]="precifForm.taxaCartaoPercentual" />
                  </label>
                  <label class="field">
                    <span>Comissão (%)</span>
                    <input type="number" step="0.1" [(ngModel)]="precifForm.comissaoVendaPercentual" />
                  </label>
                </div>

                <div class="form-row">
                  <label class="field">
                    <span>Frete Variável Venda (%)</span>
                    <input type="number" step="0.1" [(ngModel)]="precifForm.freteVendaVariavelPercentual" />
                  </label>
                  <label class="field">
                    <span>Outras Despesas Variáveis (%)</span>
                    <input type="number" step="0.1" [(ngModel)]="precifForm.outrasDespesasVariaveisPercentual" />
                  </label>
                  <label class="field">
                    <span>Margem Desejada s/ Venda (%)</span>
                    <input type="number" step="0.5" [(ngModel)]="precifForm.margemLucroDesejadaPercentual" />
                  </label>
                </div>

                <div class="form-row">
                  <label class="field">
                    <span>Desconto Promocional Simulado (%)</span>
                    <input type="number" step="0.5" [(ngModel)]="precifForm.descontoPromocionalSimuladoPercentual" />
                  </label>
                  <label class="field">
                    <span>Variação de Custo Simulada (%)</span>
                    <input type="number" step="1" [(ngModel)]="precifForm.variacaoCustoSimuladaPercentual" />
                  </label>
                </div>

                <button type="button" class="btn btn--primary" (click)="simularPrecificacao()" [disabled]="carregando()">
                  Simular Formação de Preço
                </button>
              </div>

              @if (precificacaoResultado(); as pr) {
                <div class="card">
                  <div class="card__header-row">
                    <h3>Resultado da Simulação</h3>
                    <span class="badge badge--simulado">Simulação Read-Only</span>
                  </div>

                  <div class="summary-strip">
                    <div><span>Custo Efetivo Total</span><strong>{{ formatCurrency(pr.custoAquisicaoEfetivoTotal) }}</strong></div>
                    <div><span>Despesas Variáveis Venda</span><strong>{{ formatPercent(pr.percentualDespesasVariaveisTotal) }}</strong></div>
                    <div><span>Markup Divisor</span><strong>{{ pr.markupDivisor }}</strong></div>
                    <div><span>Markup Multiplicador</span><strong>{{ pr.markupMultiplicador }}x</strong></div>
                    <div><span>Preço Mínimo (Zero Lucro)</span><strong>{{ formatCurrency(pr.precoMinimoEquilibrio) }}</strong></div>
                    <div class="summary-strip__highlight">
                      <span>Preço Recomendado ({{ formatPercent(pr.margemLucroDesejadaPercentual) }} margem)</span>
                      <strong>{{ formatCurrency(pr.precoRecomendado) }}</strong>
                    </div>
                  </div>

                  <div class="grid-2" style="margin-top: 0.85rem;">
                    <div class="pe-box">
                      <small>Preço Atual vs Margem Real</small>
                      <strong>{{ formatCurrency(pr.precoPraticadoAtual) }} → {{ formatPercent(pr.margemRealPrecoAtualPercentual) }}</strong>
                      <span>({{ formatCurrency(pr.margemRealPrecoAtualValor) }}/un • Situação: {{ pr.situacaoPrecoAtual }})</span>
                    </div>
                    <div class="pe-box">
                      <small>Simulação Promocional</small>
                      <strong>{{ formatCurrency(pr.precoPromocionalSimulado) }} → {{ formatPercent(pr.margemPromocionalPercentual) }}</strong>
                      @if (pr.aumentoVolumeNecessarioParaMesmoLucroPercentual !== null) {
                        <span>Exige +{{ formatPercent(pr.aumentoVolumeNecessarioParaMesmoLucroPercentual) }} em volume</span>
                      }
                    </div>
                  </div>

                  <p class="text-muted" style="margin-top: 0.75rem;">{{ pr.diferencaMarkupVsMargemExplicacao }}</p>
                  <ul class="clean-list">
                    @for (rec of pr.recomendacoesPrecificacao; track rec) {
                      <li>• {{ rec }}</li>
                    }
                  </ul>

                  @if (pr.produtoId && podeEditar()) {
                    <div class="ind-inline-form">
                      <h4>Aplicar Preço Autorizado no Produto #{{ pr.produtoId }}</h4>
                      <div class="form-row">
                        <label class="field">
                          <span>Novo Preço de Venda (R$)</span>
                          <input type="number" step="0.01" [(ngModel)]="aplicarPrecoValor" />
                        </label>
                        <label class="field">
                          <span>Justificativa Obrigatória</span>
                          <input type="text" [(ngModel)]="aplicarPrecoJustificativa" placeholder="Ex.: Revisao markup P0" />
                        </label>
                      </div>
                      <button type="button" class="btn btn--outline" (click)="aplicarPrecoAutorizado(pr.produtoId!)">
                        Confirmar e Atualizar Preço do Produto
                      </button>
                    </div>
                  }
                </div>
              }
            </div>
          </section>
        }

        <!-- ABA 5: CAPITAL DE GIRO, NCG, TESOURARIA E PRAZOS/CICLOS (P1) -->
        @if (abaAtiva() === 'capital-giro') {
          <section class="tab-panel">
            <div class="grid-2">
              <div class="card">
                <h2>Capital de Giro: CGL, NCG e Saldo de Tesouraria</h2>
                <p>{{ p.capitalGiro.interpretacaoCapitalGiro }}</p>

                <div class="summary-strip">
                  <div>
                    <span>Ativo Circulante Operacional (ACO)</span>
                    <strong>{{ formatCurrency(p.capitalGiro.ativoCirculanteOperacional) }}</strong>
                    <small>Estoque {{ formatPercent(p.capitalGiro.percentualCapitalPresoEstoque) }} | Clientes {{ formatPercent(p.capitalGiro.percentualCapitalPresoClientes) }}</small>
                  </div>
                  <div>
                    <span>Passivo Circulante Operacional (PCO)</span>
                    <strong>{{ formatCurrency(p.capitalGiro.passivoCirculanteOperacional) }}</strong>
                    <small>Financiado por terceiros: {{ formatPercent(p.capitalGiro.percentualFinanciadoFornecedores) }}</small>
                  </div>
                  <div class="summary-strip__highlight">
                    <span>NCG = ACO − PCO</span>
                    <strong>{{ formatCurrency(p.capitalGiro.necessidadeCapitalGiroNcg) }}</strong>
                  </div>
                  <div>
                    <span>CGL = AC − PC</span>
                    <strong>{{ formatCurrency(p.capitalGiro.capitalGiroLiquidoCgl) }}</strong>
                  </div>
                  <div>
                    <span>Saldo Tesouraria (ST = CGL − NCG)</span>
                    <strong [class.text-danger]="p.capitalGiro.saldoTesourariaSt < 0">{{ formatCurrency(p.capitalGiro.saldoTesourariaSt) }}</strong>
                  </div>
                </div>

                <ul class="clean-list" style="margin-top: 0.75rem;">
                  @for (f of p.capitalGiro.fatoresPressaoCapitalGiro; track f) {
                    <li>• {{ f }}</li>
                  }
                </ul>
              </div>

              <div class="card">
                <h2>Prazos Médios (PME, PMR, PMP) & Ciclos</h2>
                <p>{{ p.prazosCiclos.interpretacaoPrazosCiclos }}</p>
                <div class="summary-strip">
                  <div>
                    <span>PME (Estocagem)</span>
                    <strong>{{ formatDaysOrNA(p.prazosCiclos.pmePrazoMedioEstocagemDias) }}</strong>
                    <small>{{ p.prazosCiclos.statusConfiabilidadePme }}</small>
                  </div>
                  <div>
                    <span>PMR (Recebimento Prazo)</span>
                    <strong>{{ formatDaysOrNA(p.prazosCiclos.pmrPrazoMedioRecebimentoDias) }}</strong>
                    <small>{{ p.prazosCiclos.statusConfiabilidadePmr }}</small>
                  </div>
                  <div>
                    <span>PMP (Pagamento Fornecedores)</span>
                    <strong>{{ formatDaysOrNA(p.prazosCiclos.pmpPrazoMedioPagamentoDias) }}</strong>
                    <small>{{ p.prazosCiclos.statusConfiabilidadePmp }}</small>
                  </div>
                  <div>
                    <span>Ciclo Operacional</span>
                    <strong>{{ formatDaysOrNA(p.prazosCiclos.cicloOperacionalDias) }}</strong>
                  </div>
                  <div class="summary-strip__highlight">
                    <span>Ciclo Financeiro</span>
                    <strong [class.text-danger]="p.prazosCiclos.pagaFornecedorAntesDeReceber">
                      {{ formatDaysOrNA(p.prazosCiclos.cicloFinanceiroDias) }}
                    </strong>
                    <small>Tendência: {{ p.prazosCiclos.tendenciaCicloFinanceiro }}</small>
                  </div>
                </div>

                <h4 style="margin-top: 1rem;">Evolução Mensal dos Prazos (Últimos 3 Meses)</h4>
                <table class="ind-table">
                  <thead>
                    <tr>
                      <th>Competência</th>
                      <th class="text-right">PME</th>
                      <th class="text-right">PMR</th>
                      <th class="text-right">PMP</th>
                      <th class="text-right">Ciclo Op.</th>
                      <th class="text-right">Ciclo Fin.</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (ev of p.prazosCiclos.evolucaoMensal; track ev.competencia) {
                      <tr>
                        <td>{{ ev.competencia }}</td>
                        <td class="text-right">{{ formatDaysOrNA(ev.pmeDias) }}</td>
                        <td class="text-right">{{ formatDaysOrNA(ev.pmrDias) }}</td>
                        <td class="text-right">{{ formatDaysOrNA(ev.pmpDias) }}</td>
                        <td class="text-right">{{ formatDaysOrNA(ev.cicloOperacionalDias) }}</td>
                        <td class="text-right"><strong>{{ formatDaysOrNA(ev.cicloFinanceiroDias) }}</strong></td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        }

        <!-- ABA 6: BALANÇO PATRIMONIAL GERENCIAL, LIQUIDEZ & ENDIVIDAMENTO (P2) -->
        @if (abaAtiva() === 'liquidez-balanco') {
          <section class="tab-panel">
            <div class="grid-2">
              <div class="card">
                <div class="card__header-row">
                  <h2>Liquidez e Endividamento</h2>
                  <span class="badge" [ngClass]="confiabilidadeBadgeClass(p.liquidezEndividamento.statusConfiabilidadePatrimonial)">
                    {{ p.liquidezEndividamento.statusConfiabilidadePatrimonial }}
                  </span>
                </div>
                <p>{{ p.liquidezEndividamento.interpretacaoLiquidezEndividamento }}</p>

                <div class="summary-strip">
                  <div><span>Liquidez Imediata</span><strong>{{ formatIndexOrNA(p.liquidezEndividamento.liquidezImediata) }}</strong></div>
                  <div><span>Liquidez Seca</span><strong>{{ formatIndexOrNA(p.liquidezEndividamento.liquidezSeca) }}</strong></div>
                  <div><span>Liquidez Corrente</span><strong>{{ formatIndexOrNA(p.liquidezEndividamento.liquidezCorrente) }}</strong></div>
                  <div><span>Liquidez Geral</span><strong>{{ formatIndexOrNA(p.liquidezEndividamento.liquidezGeral) }}</strong></div>
                  <div><span>Endividamento Geral</span><strong>{{ formatPercentOrNA(p.liquidezEndividamento.endividamentoGeralPercentual) }}</strong></div>
                  <div><span>Capital Terceiros (PCT)</span><strong>{{ formatPercentOrNA(p.liquidezEndividamento.participacaoCapitalTerceirosPercentual) }}</strong></div>
                </div>

                @if (p.liquidezEndividamento.dependenciasPatrimoniaisFaltantes.length > 0) {
                  <div class="card--warning" style="margin-top: 1rem; padding: 0.75rem; border-radius: 0.5rem;">
                    <strong>Dependências Patrimoniais Faltantes:</strong>
                    @for (dep of p.liquidezEndividamento.dependenciasPatrimoniaisFaltantes; track dep) {
                      <small class="d-block">• {{ dep }}</small>
                    }
                  </div>
                }
              </div>

              <div class="card">
                <h2>Balanço Patrimonial Gerencial ({{ balancoForm.competenciaAnoMes }})</h2>
                <p class="text-muted">
                  Os saldos de Disponibilidades, Contas a Receber, Estoques e Fornecedores são sincronizados do ERP quando a opção abaixo estiver marcada, ou podem ser editados manualmente ao desmarcá-la. Confirme o balanço completo para habilitar os indicadores P2.
                </p>

                <div class="form-row">
                  <label class="field">
                    <span>Disponibilidades Caixa/Bancos (R$)</span>
                    <input type="number" step="0.01" [(ngModel)]="balancoForm.disponibilidadesCaixaBancos" [disabled]="!!balancoForm.usarSaldosOperacionaisErp" />
                  </label>
                  <label class="field">
                    <span>Contas a Receber Op. (R$)</span>
                    <input type="number" step="0.01" [(ngModel)]="balancoForm.contasReceberOperacionais" [disabled]="!!balancoForm.usarSaldosOperacionaisErp" />
                  </label>
                  <label class="field">
                    <span>Estoques Operacionais (R$)</span>
                    <input type="number" step="0.01" [(ngModel)]="balancoForm.estoquesOperacionais" [disabled]="!!balancoForm.usarSaldosOperacionaisErp" />
                  </label>
                </div>

                <div class="form-row">
                  <label class="field">
                    <span>Adiantamentos Operacionais (R$)</span>
                    <input type="number" step="0.01" [(ngModel)]="balancoForm.adiantamentosOperacionais" />
                  </label>
                  <label class="field">
                    <span>Despesas Antecipadas (R$)</span>
                    <input type="number" step="0.01" [(ngModel)]="balancoForm.despesasAntecipadas" />
                  </label>
                  <label class="field">
                    <span>Outros Ativos Circulantes (R$)</span>
                    <input type="number" step="0.01" [(ngModel)]="balancoForm.outrosAtivosCirculantes" />
                  </label>
                </div>

                <div class="form-row">
                  <label class="field">
                    <span>Realizável a Longo Prazo (R$)</span>
                    <input type="number" step="0.01" [(ngModel)]="balancoForm.realizavelLongoPrazo" />
                  </label>
                  <label class="field">
                    <span>Investimentos Longo Prazo (R$)</span>
                    <input type="number" step="0.01" [(ngModel)]="balancoForm.investimentosLongoPrazo" />
                  </label>
                  <label class="field">
                    <span>Imobilizado Bruto (R$)</span>
                    <input type="number" step="0.01" [(ngModel)]="balancoForm.imobilizadoBruto" />
                  </label>
                </div>

                <div class="form-row">
                  <label class="field">
                    <span>Depreciação Acumulada (R$)</span>
                    <input type="number" step="0.01" [(ngModel)]="balancoForm.depreciacaoAcumulada" />
                  </label>
                  <label class="field">
                    <span>Intangível (R$)</span>
                    <input type="number" step="0.01" [(ngModel)]="balancoForm.intangivel" />
                  </label>
                  <label class="field">
                    <span>Fornecedores a Pagar CP (R$)</span>
                    <input type="number" step="0.01" [(ngModel)]="balancoForm.fornecedoresOperacionaisCp" [disabled]="!!balancoForm.usarSaldosOperacionaisErp" />
                  </label>
                </div>

                <div class="form-row">
                  <label class="field">
                    <span>Obrigações Trib./Trab. CP (R$)</span>
                    <input type="number" step="0.01" [(ngModel)]="balancoForm.obrigacoesTributariasTrabalhistasCp" />
                  </label>
                  <label class="field">
                    <span>Outras Obrigações Op. CP (R$)</span>
                    <input type="number" step="0.01" [(ngModel)]="balancoForm.outrasObrigacoesOperacionaisCp" />
                  </label>
                  <label class="field">
                    <span>Empréstimos CP (R$)</span>
                    <input type="number" step="0.01" [(ngModel)]="balancoForm.emprestimosFinanciamentosCp" />
                  </label>
                </div>

                <div class="form-row">
                  <label class="field">
                    <span>Empréstimos LP (R$)</span>
                    <input type="number" step="0.01" [(ngModel)]="balancoForm.emprestimosFinanciamentosLp" />
                  </label>
                  <label class="field">
                    <span>Outras Obrigações LP (R$)</span>
                    <input type="number" step="0.01" [(ngModel)]="balancoForm.outrasObrigacoesLp" />
                  </label>
                  <label class="field">
                    <span>Capital Social (R$)</span>
                    <input type="number" step="0.01" [(ngModel)]="balancoForm.capitalSocial" />
                  </label>
                </div>

                <div class="form-row">
                  <label class="field">
                    <span>Reservas Capital/Lucros (R$)</span>
                    <input type="number" step="0.01" [(ngModel)]="balancoForm.reservasCapitalLucros" />
                  </label>
                  <label class="field">
                    <span>Lucros/Prejuízos Acumulados (R$)</span>
                    <input type="number" step="0.01" [(ngModel)]="balancoForm.lucrosPrejuizosAcumulados" />
                  </label>
                  <label class="field">
                    <span>Depreciação do Período (PEF)</span>
                    <input type="number" step="0.01" [(ngModel)]="balancoForm.depreciacaoAmortizacaoPeriodo" />
                  </label>
                </div>

                <div class="form-row">
                  <label class="field">
                    <span>Amortização Principal Dívidas (PEF)</span>
                    <input type="number" step="0.01" [(ngModel)]="balancoForm.amortizacaoPrincipalDividaPeriodo" />
                  </label>
                  <label class="field">
                    <span>Observações Contábeis</span>
                    <input type="text" [(ngModel)]="balancoForm.observacoes" placeholder="Ex.: Conferência patrimonial" />
                  </label>
                </div>

                <div class="form-row">
                  <label class="field field--checkbox">
                    <input type="checkbox" [(ngModel)]="balancoForm.usarSaldosOperacionaisErp" />
                    <span>Sincronizar Caixa/CR/Estoque/CP do ERP</span>
                  </label>
                  <label class="field field--checkbox">
                    <input type="checkbox" [(ngModel)]="balancoForm.confirmadoGestor" />
                    <span>Confirmar Balanço para Indicadores P2</span>
                  </label>
                </div>

                @if (podeEditar()) {
                  <button type="button" class="btn btn--primary" (click)="salvarBalancoPatrimonial()">
                    Salvar Balanço Patrimonial Gerencial
                  </button>
                }
              </div>
            </div>
          </section>
        }

        <!-- ABA 7: INVESTIMENTOS, ROI, ROA, ROE, VPL & TIR (P2) -->
        @if (abaAtiva() === 'investimentos') {
          <section class="tab-panel">
            <div class="grid-2">
              <div class="card">
                <h2>Retorno Contábil vs Fluxo de Caixa Descontado</h2>
                <p class="text-muted">{{ p.investimentosRetorno.separacaoContabilVsCaixaExplicacao }}</p>
                <div class="summary-strip">
                  <div><span>ROA (Lucro / Ativo)</span><strong>{{ formatPercentOrNA(p.investimentosRetorno.roaRetornoSobreAtivoPercentual) }}</strong></div>
                  <div><span>ROE (Lucro / PL)</span><strong>{{ formatPercentOrNA(p.investimentosRetorno.roeRetornoSobrePatrimonioPercentual) }}</strong></div>
                  <div><span>ROIC</span><strong>{{ formatPercentOrNA(p.investimentosRetorno.roicRetornoSobreCapitalInvestidoPercentual) }}</strong></div>
                  <div><span>Caixa Disponível p/ Investir</span><strong>{{ formatCurrency(p.investimentosRetorno.disponibilidadeCaixaAposReserva) }}</strong></div>
                </div>

                @for (alerta of p.investimentosRetorno.alertasInvestimento; track alerta) {
                  <p class="text-muted" style="margin-top: 0.65rem;">• {{ alerta }}</p>
                }
              </div>

              <div class="card">
                <h2>Simulador de Viabilidade de Investimento (VPL, TIR & Payback)</h2>
                <div class="form-row">
                  <label class="field">
                    <span>Projeto</span>
                    <input type="text" [(ngModel)]="investimentoForm.nomeProjeto" />
                  </label>
                  <label class="field">
                    <span>Investimento Inicial (R$)</span>
                    <input type="number" step="100" [(ngModel)]="investimentoForm.investimentoInicial" />
                  </label>
                  <label class="field">
                    <span>Taxa de Desconto por Período (%)</span>
                    <input type="number" step="0.25" [(ngModel)]="investimentoForm.taxaDescontoPeriodicaPercentual" />
                  </label>
                </div>
                <label class="field" style="margin-bottom: 0.75rem;">
                  <span>Fluxos de Caixa Futuros (separados por vírgula)</span>
                  <input type="text" [(ngModel)]="fluxosProjetoTexto" placeholder="4000, 4000, 4000, 3000" />
                </label>
                <button type="button" class="btn btn--primary" (click)="simularInvestimento()">
                  Calcular VPL, TIR e Payback
                </button>

                @if (investimentoResultado(); as ir) {
                  <div class="summary-strip" style="margin-top: 1rem;">
                    <div><span>ROI</span><strong>{{ formatPercentOrNA(ir.roiSimuladoPercentual) }}</strong></div>
                    <div><span>VPL</span><strong [class.text-success]="(ir.vplValorPresenteLiquido ?? 0) > 0">{{ formatCurrencyOrNA(ir.vplValorPresenteLiquido) }}</strong></div>
                    <div><span>TIR</span><strong>{{ formatPercentOrNA(ir.tirTaxaInternaRetornoPercentual) }}</strong></div>
                    <div><span>Payback Simples</span><strong>{{ ir.paybackSimplesPeriodos !== null ? ir.paybackSimplesPeriodos + ' períodos' : 'Não recupera' }}</strong></div>
                    <div><span>Payback Descontado</span><strong>{{ ir.paybackDescontadoPeriodos !== null ? ir.paybackDescontadoPeriodos + ' períodos' : 'Não recupera' }}</strong></div>
                    <div>
                      <span>Viabilidade Econômica</span>
                      <strong [class.text-success]="ir.investimentoViavelEconomicamente" [class.text-danger]="!ir.investimentoViavelEconomicamente">
                        {{ ir.investimentoViavelEconomicamente ? 'VIÁVEL' : 'INVIÁVEL' }}
                      </strong>
                    </div>
                    <div>
                      <span>Capacidade de Caixa</span>
                      <strong [class.text-success]="ir.capacidadeFinanceiraCaixaParaInvestir" [class.text-danger]="!ir.capacidadeFinanceiraCaixaParaInvestir">
                        {{ ir.capacidadeFinanceiraCaixaParaInvestir ? 'COM FOLGA' : 'SEM FOLGA' }}
                      </strong>
                      <small>Reserva livre: {{ formatCurrency(ir.disponibilidadeCaixaAposReserva) }}</small>
                    </div>
                  </div>
                  @if (ir.alertasInvestimento.length > 0) {
                    <ul class="clean-list" style="margin-top: 0.75rem;">
                      @for (a of ir.alertasInvestimento; track a) {
                        <li class="text-muted">• {{ a }}</li>
                      }
                    </ul>
                  }
                }
              </div>
            </div>
          </section>
        }

        <!-- ABA 8: DIAGNÓSTICOS EXPLICÁVEIS & PARÂMETROS -->
        @if (abaAtiva() === 'diagnosticos') {
          <section class="tab-panel">
            <div class="card">
              <h2>Diagnósticos Determinísticos e Recomendações Gerenciais</h2>
              <div class="catalog-grid">
                @for (diag of p.diagnosticosExplicaveis; track diag.codigoDiagnostico) {
                  <article class="indicator-card">
                    <div class="indicator-card__top">
                      <span class="badge" [ngClass]="severidadeBadgeClass(diag.severidade)">{{ diag.severidade }}</span>
                      <span class="badge badge--outline">{{ diag.grauConfiabilidade }}</span>
                    </div>
                    <h3>{{ diag.indicadorRelacionado }}</h3>
                    <strong>{{ diag.resultadoFormatado }}</strong>
                    <p>{{ diag.interpretacao }}</p>
                    <small class="d-block text-danger">Consequência: {{ diag.possivelConsequencia }}</small>
                    <small class="d-block text-success">Recomendação: {{ diag.recomendacaoGerencial }}</small>
                    <small class="d-block text-muted">Fonte: {{ diag.fonteDados }} ({{ diag.periodo }})</small>
                  </article>
                }
              </div>
            </div>

            <div class="card">
              <h2>Benchmarks e Parâmetros Configuráveis da Empresa</h2>
              <div class="form-row">
                <label class="field">
                  <span>Segmento de Negócio</span>
                  <input type="text" [(ngModel)]="parametrosForm.segmentoNegocio" />
                </label>
                <label class="field">
                  <span>Capital Investido Referência (R$)</span>
                  <input type="number" step="100" [(ngModel)]="parametrosForm.capitalInvestidoReferencia" />
                </label>
                <label class="field">
                  <span>Retorno Mínimo Mensal (%)</span>
                  <input type="number" step="0.1" [(ngModel)]="parametrosForm.retornoMinimoMensalPercentual" />
                </label>
              </div>
              <div class="form-row">
                <label class="field">
                  <span>Taxa Desconto Anual (%)</span>
                  <input type="number" step="0.5" [(ngModel)]="parametrosForm.taxaDescontoAnualPercentual" />
                </label>
                <label class="field">
                  <span>MC Mínima Esperada (%)</span>
                  <input type="number" step="0.5" [(ngModel)]="parametrosForm.margemContribuicaoMinimaPercentual" />
                </label>
                <label class="field">
                  <span>Margem Líquida Mínima (%)</span>
                  <input type="number" step="0.5" [(ngModel)]="parametrosForm.margemLiquidaMinimaPercentual" />
                </label>
              </div>
              <div class="form-row">
                <label class="field">
                  <span>Margem Desejada Markup Padrão (%)</span>
                  <input type="number" step="0.5" [(ngModel)]="parametrosForm.markupMargemDesejadaPadrao" />
                </label>
                <label class="field">
                  <span>Tributos Venda Padrão (%)</span>
                  <input type="number" step="0.5" [(ngModel)]="parametrosForm.aliquotaTributosVendaPadrao" />
                </label>
                <label class="field">
                  <span>Taxa Cartão Média Padrão (%)</span>
                  <input type="number" step="0.1" [(ngModel)]="parametrosForm.taxaCartaoMediaPadrao" />
                </label>
              </div>
              <div class="form-row">
                <label class="field">
                  <span>Comissão Venda Padrão (%)</span>
                  <input type="number" step="0.1" [(ngModel)]="parametrosForm.comissaoVendaPadrao" />
                </label>
                <label class="field">
                  <span>Frete Variável Padrão (%)</span>
                  <input type="number" step="0.1" [(ngModel)]="parametrosForm.freteVariavelVendaPadrao" />
                </label>
                <label class="field">
                  <span>Ciclo Financeiro Máximo (dias)</span>
                  <input type="number" step="1" [(ngModel)]="parametrosForm.cicloFinanceiroMaximoDias" />
                </label>
              </div>
              <div class="form-row">
                <label class="field">
                  <span>PME Máximo (dias)</span>
                  <input type="number" step="1" [(ngModel)]="parametrosForm.pmeMaximoDias" />
                </label>
                <label class="field">
                  <span>Liquidez Corrente Mínima</span>
                  <input type="number" step="0.05" [(ngModel)]="parametrosForm.liquidezCorrenteMinima" />
                </label>
                <label class="field">
                  <span>Liquidez Seca Mínima</span>
                  <input type="number" step="0.05" [(ngModel)]="parametrosForm.liquidezSecaMinima" />
                </label>
              </div>
              <div class="form-row">
                <label class="field">
                  <span>Liquidez Imediata Mínima</span>
                  <input type="number" step="0.05" [(ngModel)]="parametrosForm.liquidezImediataMinima" />
                </label>
                <label class="field">
                  <span>Endividamento Geral Máximo (%)</span>
                  <input type="number" step="1" [(ngModel)]="parametrosForm.endividamentoGeralMaximoPercentual" />
                </label>
              </div>
              @if (podeEditar()) {
                <button type="button" class="btn btn--primary" (click)="salvarParametros()">
                  Salvar Parâmetros da Empresa
                </button>
              }
            </div>
          </section>
        }
      }
    </div>
  `,
  styles: [
    `
      .ind-shell {
        padding: 1.5rem;
        display: flex;
        flex-direction: column;
        gap: 1.25rem;
        max-width: 1480px;
        margin: 0 auto;
      }
      .ind-hero {
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
      .ind-hero__badges {
        display: flex;
        gap: 0.5rem;
        flex-wrap: wrap;
        margin-bottom: 0.5rem;
      }
      .ind-hero h1 {
        margin: 0 0 0.4rem;
        font-size: 1.45rem;
      }
      .ind-hero__subtitle {
        margin: 0;
        color: #cbd5e1;
        max-width: 780px;
        font-size: 0.92rem;
      }
      .ind-hero__actions {
        display: flex;
        gap: 0.5rem;
        flex-wrap: wrap;
      }
      .card {
        background: #ffffff;
        border: 1px solid #e2e8f0;
        border-radius: 0.85rem;
        padding: 1.15rem;
        color: #0f172a;
      }
      .card--warning {
        border-color: #f59e0b;
        background: #fffbeb;
      }
      .card__header-row {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 1rem;
        flex-wrap: wrap;
        margin-bottom: 1rem;
      }
      .ind-filters__grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
        gap: 0.85rem;
        align-items: end;
      }
      .field {
        display: flex;
        flex-direction: column;
        gap: 0.3rem;
        font-size: 0.82rem;
        font-weight: 600;
        color: #334155;
      }
      .field input,
      .field select {
        padding: 0.5rem 0.65rem;
        border: 1px solid #cbd5e1;
        border-radius: 0.5rem;
        font-size: 0.88rem;
      }
      .field--checkbox {
        flex-direction: row;
        align-items: center;
        gap: 0.5rem;
      }
      .ind-kpis {
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
        font-size: 0.76rem;
        color: #64748b;
        text-transform: uppercase;
      }
      .kpi-card__value {
        font-size: 1.25rem;
        color: #0f172a;
      }
      .kpi-card__meta {
        font-size: 0.78rem;
        color: #64748b;
      }
      .ind-tabs {
        display: flex;
        gap: 0.5rem;
        flex-wrap: wrap;
        border-bottom: 1px solid #cbd5e1;
        padding-bottom: 0.5rem;
      }
      .ind-tabs button {
        padding: 0.5rem 0.9rem;
        border-radius: 0.5rem;
        border: 1px solid transparent;
        background: transparent;
        cursor: pointer;
        font-weight: 600;
        font-size: 0.84rem;
        color: #475569;
      }
      .ind-tabs button.active {
        background: #0f172a;
        color: #ffffff;
      }
      .tab-panel {
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }
      .grid-2 {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(420px, 1fr));
        gap: 1rem;
      }
      .catalog-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(310px, 1fr));
        gap: 0.85rem;
      }
      .indicator-card {
        border: 1px solid #e2e8f0;
        border-radius: 0.75rem;
        padding: 0.9rem;
        background: #f8fafc;
        display: flex;
        flex-direction: column;
        gap: 0.45rem;
      }
      .indicator-card__top {
        display: flex;
        justify-content: space-between;
        gap: 0.5rem;
      }
      .indicator-card__result strong {
        font-size: 1.35rem;
        color: #0f172a;
      }
      .indicator-card__formula {
        background: #ffffff;
        border: 1px solid #e2e8f0;
        border-radius: 0.45rem;
        padding: 0.4rem 0.55rem;
        font-size: 0.78rem;
      }
      .pe-cards-3 {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
        gap: 0.85rem;
        margin-top: 0.75rem;
      }
      .pe-box {
        border: 1px solid #e2e8f0;
        background: #f8fafc;
        border-radius: 0.65rem;
        padding: 0.85rem;
        display: flex;
        flex-direction: column;
        gap: 0.35rem;
      }
      .summary-strip {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
        gap: 0.65rem;
        margin-top: 0.65rem;
      }
      .summary-strip div {
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 0.55rem;
        padding: 0.6rem 0.75rem;
        display: flex;
        flex-direction: column;
        gap: 0.2rem;
      }
      .summary-strip__highlight {
        background: #f0f9ff !important;
        border-color: #0ea5e9 !important;
      }
      .ind-table {
        width: 100%;
        border-collapse: collapse;
        font-size: 0.84rem;
      }
      .ind-table th,
      .ind-table td {
        padding: 0.55rem 0.6rem;
        border-bottom: 1px solid #e2e8f0;
        text-align: left;
      }
      .ind-table th.text-right,
      .ind-table td.text-right {
        text-align: right;
      }
      .table-wrap {
        overflow-x: auto;
      }
      .form-row {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
        gap: 0.65rem;
        margin-bottom: 0.65rem;
      }
      .ind-inline-form {
        margin-top: 1rem;
        padding-top: 0.85rem;
        border-top: 1px solid #e2e8f0;
      }
      .btn {
        padding: 0.5rem 0.9rem;
        border-radius: 0.5rem;
        border: 1px solid transparent;
        cursor: pointer;
        font-weight: 600;
        font-size: 0.84rem;
        text-decoration: none;
        display: inline-flex;
        align-items: center;
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
      .badge {
        display: inline-block;
        padding: 0.16rem 0.5rem;
        border-radius: 999px;
        font-size: 0.72rem;
        font-weight: 600;
      }
      .badge--accent { background: #0284c7; color: #ffffff; }
      .badge--simulado { background: #7c3aed; color: #ffffff; }
      .badge--outline { border: 1px solid #cbd5e1; color: #475569; }
      .badge--success { background: #dcfce7; color: #166534; }
      .badge--danger { background: #fee2e2; color: #991b1b; }
      .badge--warning { background: #fef3c7; color: #92400e; }
      .clean-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 0.35rem; }
      .text-success { color: #15803d; }
      .text-danger { color: #b91c1c; }
      .text-muted { color: #64748b; }
      .d-block { display: block; }
      .alert {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 0.75rem 1rem;
        border-radius: 0.65rem;
      }
      .alert--success { background: #dcfce7; color: #166534; }
      .alert--error { background: #fee2e2; color: #991b1b; }
      .alert__close { background: none; border: none; font-size: 1.1rem; cursor: pointer; }
    `
  ]
})
export class FinanceIndicatorsPage implements OnInit {
  private readonly api = inject(ApexApiService);
  private readonly auth = inject(AuthService);

  readonly carregando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly mensagemSucesso = signal<string | null>(null);

  readonly abaAtiva = signal<IndicatorsTab>('visao-geral');
  readonly painel = signal<PainelInteligenciaAdministrativaResponse | null>(null);
  readonly precificacaoResultado = signal<SimulacaoPrecificacaoResponse | null>(null);
  readonly investimentoResultado = signal<InvestimentosRetornoPainel | null>(null);
  readonly produtosCatalogo = signal<Produto[]>([]);

  filtroCompetencia = new Date().toISOString().slice(0, 7);
  filtroDataInicio = '';
  filtroDataFim = '';
  filtroConsolidarFiliais = false;
  filtroCategoriaCatalogo = 'TODAS';

  precifForm: SimulacaoPrecificacaoRequest = {
    produtoId: undefined,
    custoAquisicaoBase: 50,
    freteCompraValor: 0,
    seguroCompraValor: 0,
    impostosNaoRecuperaveisValor: 0,
    despesasAcessoriasValor: 0,
    aliquotaTributosVendaPercentual: 8,
    taxaCartaoPercentual: 2.5,
    comissaoVendaPercentual: 2,
    freteVendaVariavelPercentual: 0,
    outrasDespesasVariaveisPercentual: 0,
    margemLucroDesejadaPercentual: 20,
    precoPraticadoAtual: 75,
    descontoPromocionalSimuladoPercentual: 10,
    variacaoCustoSimuladaPercentual: 5
  };

  aplicarPrecoValor = 0;
  aplicarPrecoJustificativa = '';

  balancoForm: BalancoPatrimonialGerencialRequest = {
    competenciaAnoMes: this.filtroCompetencia,
    disponibilidadesCaixaBancos: 0,
    contasReceberOperacionais: 0,
    estoquesOperacionais: 0,
    adiantamentosOperacionais: 0,
    despesasAntecipadas: 0,
    outrosAtivosCirculantes: 0,
    realizavelLongoPrazo: 0,
    investimentosLongoPrazo: 0,
    imobilizadoBruto: 0,
    depreciacaoAcumulada: 0,
    intangivel: 0,
    fornecedoresOperacionaisCp: 0,
    obrigacoesTributariasTrabalhistasCp: 0,
    outrasObrigacoesOperacionaisCp: 0,
    emprestimosFinanciamentosCp: 0,
    emprestimosFinanciamentosLp: 0,
    outrasObrigacoesLp: 0,
    capitalSocial: 0,
    reservasCapitalLucros: 0,
    lucrosPrejuizosAcumulados: 0,
    depreciacaoAmortizacaoPeriodo: 0,
    amortizacaoPrincipalDividaPeriodo: 0,
    usarSaldosOperacionaisErp: true,
    confirmadoGestor: false,
    observacoes: ''
  };

  investimentoForm: SimulacaoInvestimentoRequest = {
    nomeProjeto: 'Expansão / Novo Equipamento',
    investimentoInicial: 10000,
    taxaDescontoPeriodicaPercentual: 1.5
  };
  fluxosProjetoTexto = '3000, 3500, 4000, 4500';

  parametrosForm: ParametroInteligenciaAdministrativa = {
    segmentoNegocio: 'VAREJO_GERAL',
    retornoMinimoMensalPercentual: 1.5,
    capitalInvestidoReferencia: 0,
    taxaDescontoAnualPercentual: 12,
    margemContribuicaoMinimaPercentual: 25,
    margemLiquidaMinimaPercentual: 8,
    markupMargemDesejadaPadrao: 20,
    aliquotaTributosVendaPadrao: 8,
    taxaCartaoMediaPadrao: 2.5,
    comissaoVendaPadrao: 2,
    freteVariavelVendaPadrao: 0,
    cicloFinanceiroMaximoDias: 45,
    pmeMaximoDias: 60,
    liquidezCorrenteMinima: 1.2,
    liquidezSecaMinima: 0.9,
    liquidezImediataMinima: 0.3,
    endividamentoGeralMaximoPercentual: 60
  };

  readonly podeEditar = computed(() => {
    const roles = this.auth.user()?.roles ?? [];
    return roles.some((r) =>
      [
        'ROLE_SYSADMIN',
        'ROLE_ADMIN',
        'ROLE_ADMIN_MASTER',
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

  readonly indicadoresCatalogoFiltrados = computed(() => {
    const lista = this.painel()?.catalogoIndicadores ?? [];
    if (this.filtroCategoriaCatalogo === 'TODAS') {
      return lista;
    }
    return lista.filter((i) => i.categoriaIndicador === this.filtroCategoriaCatalogo);
  });

  ngOnInit(): void {
    this.carregarProdutosCatalogo();
    this.carregarPainel();
  }

  carregarProdutosCatalogo(): void {
    this.api.products().subscribe({
      next: (prods) => this.produtosCatalogo.set((prods ?? []).filter((p) => p.status !== 0)),
      error: () => {
        // Silencioso para não bloquear o painel caso catálogo esteja vazio ou restrito
      }
    });
  }

  onCompetenciaChange(): void {
    this.balancoForm.competenciaAnoMes = this.filtroCompetencia;
    this.carregarPainel();
  }

  carregarPainel(): void {
    this.carregando.set(true);
    this.erro.set(null);
    const params: IndicadoresQueryParams = {
      competencia: this.filtroCompetencia,
      dataInicio: this.filtroDataInicio || undefined,
      dataFim: this.filtroDataFim || undefined,
      consolidarFiliais: this.filtroConsolidarFiliais
    };
    this.api.getIndicadoresPainel(params).subscribe({
      next: (res) => {
        this.painel.set(res);
        this.precificacaoResultado.set(res.precificacaoPadrao);
        this.aplicarPrecoValor = res.precificacaoPadrao.precoRecomendado;
        this.precifForm = {
          ...this.precifForm,
          produtoId: res.precificacaoPadrao.produtoId ?? undefined,
          custoAquisicaoBase: res.precificacaoPadrao.custoAquisicaoBase,
          aliquotaTributosVendaPercentual: res.parametrosVigentes.aliquotaTributosVendaPadrao,
          taxaCartaoPercentual: res.parametrosVigentes.taxaCartaoMediaPadrao,
          comissaoVendaPercentual: res.parametrosVigentes.comissaoVendaPadrao,
          freteVendaVariavelPercentual: res.parametrosVigentes.freteVariavelVendaPadrao,
          margemLucroDesejadaPercentual: res.precificacaoPadrao.margemLucroDesejadaPercentual,
          precoPraticadoAtual: res.precificacaoPadrao.precoPraticadoAtual ?? undefined
        };
        this.investimentoResultado.set(res.investimentosRetorno);
        this.parametrosForm = { ...res.parametrosVigentes };
        this.balancoForm = {
          competenciaAnoMes: res.balancoPatrimonial.competenciaAnoMes,
          disponibilidadesCaixaBancos: res.balancoPatrimonial.disponibilidadesCaixaBancos,
          contasReceberOperacionais: res.balancoPatrimonial.contasReceberOperacionais,
          estoquesOperacionais: res.balancoPatrimonial.estoquesOperacionais,
          adiantamentosOperacionais: res.balancoPatrimonial.adiantamentosOperacionais,
          despesasAntecipadas: res.balancoPatrimonial.despesasAntecipadas,
          outrosAtivosCirculantes: res.balancoPatrimonial.outrosAtivosCirculantes,
          realizavelLongoPrazo: res.balancoPatrimonial.realizavelLongoPrazo,
          investimentosLongoPrazo: res.balancoPatrimonial.investimentosLongoPrazo,
          imobilizadoBruto: res.balancoPatrimonial.imobilizadoBruto,
          depreciacaoAcumulada: res.balancoPatrimonial.depreciacaoAcumulada,
          intangivel: res.balancoPatrimonial.intangivel,
          fornecedoresOperacionaisCp: res.balancoPatrimonial.fornecedoresOperacionaisCp,
          obrigacoesTributariasTrabalhistasCp: res.balancoPatrimonial.obrigacoesTributariasTrabalhistasCp,
          outrasObrigacoesOperacionaisCp: res.balancoPatrimonial.outrasObrigacoesOperacionaisCp,
          emprestimosFinanciamentosCp: res.balancoPatrimonial.emprestimosFinanciamentosCp,
          emprestimosFinanciamentosLp: res.balancoPatrimonial.emprestimosFinanciamentosLp,
          outrasObrigacoesLp: res.balancoPatrimonial.outrasObrigacoesLp,
          capitalSocial: res.balancoPatrimonial.capitalSocial,
          reservasCapitalLucros: res.balancoPatrimonial.reservasCapitalLucros,
          lucrosPrejuizosAcumulados: res.balancoPatrimonial.lucrosPrejuizosAcumulados,
          depreciacaoAmortizacaoPeriodo: res.balancoPatrimonial.depreciacaoAmortizacaoPeriodo,
          amortizacaoPrincipalDividaPeriodo: res.balancoPatrimonial.amortizacaoPrincipalDividaPeriodo,
          usarSaldosOperacionaisErp: res.balancoPatrimonial.sincronizadoOperacional,
          confirmadoGestor: res.balancoPatrimonial.confirmadoGestor,
          observacoes: res.balancoPatrimonial.observacoes ?? ''
        };
        this.carregando.set(false);
      },
      error: () => {
        this.erro.set('Não foi possível carregar o painel de Inteligência Administrativa.');
        this.carregando.set(false);
      }
    });
  }

  onSelecionarProdutoPrecificacao(produtoIdRaw: number | string | null | undefined): void {
    const produtoId =
      produtoIdRaw === null || produtoIdRaw === undefined || produtoIdRaw === '' ? undefined : Number(produtoIdRaw);
    if (!produtoId || Number.isNaN(produtoId)) {
      this.precifForm = { ...this.precifForm, produtoId: undefined };
      return;
    }
    const prod = this.produtosCatalogo().find((p) => p.idProduto === produtoId);
    if (!prod) {
      this.precifForm = { ...this.precifForm, produtoId };
      return;
    }
    const impostosProd = (prod.icms ?? 0) + (prod.ipi ?? 0) + (prod.pis ?? 0) + (prod.cofins ?? 0);
    const aliqPadrao = this.painel()?.parametrosVigentes.aliquotaTributosVendaPadrao ?? this.precifForm.aliquotaTributosVendaPercentual;
    this.precifForm = {
      ...this.precifForm,
      produtoId: prod.idProduto,
      custoAquisicaoBase: prod.custo ?? this.precifForm.custoAquisicaoBase,
      precoPraticadoAtual: prod.valorVenda ?? undefined,
      aliquotaTributosVendaPercentual: impostosProd > 0 ? impostosProd : aliqPadrao
    };
    this.simularPrecificacao();
  }

  simularPrecificacao(): void {
    this.erro.set(null);
    this.api.simularPrecificacaoInteligente(this.precifForm).subscribe({
      next: (res) => {
        this.precificacaoResultado.set(res);
        this.aplicarPrecoValor = res.precoRecomendado;
        this.mensagemSucesso.set('Simulação de precificação atualizada (sem alteração nos preços do catálogo).');
      },
      error: () => this.erro.set('Verifique os percentuais informados na simulação de precificação.')
    });
  }

  aplicarPrecoAutorizado(produtoId: number): void {
    if (!this.aplicarPrecoJustificativa.trim() || this.aplicarPrecoValor <= 0) {
      this.erro.set('Informe o novo preço e a justificativa antes de atualizar o produto.');
      return;
    }
    this.api
      .aplicarPrecoProdutoAutorizado({
        produtoId,
        novoValorVenda: this.aplicarPrecoValor,
        novaMargemPercentual: this.precificacaoResultado()?.margemLucroDesejadaPercentual ?? null,
        justificativa: this.aplicarPrecoJustificativa.trim()
      })
      .subscribe({
        next: () => {
          this.mensagemSucesso.set('Preço comercial atualizado no cadastro do produto com trilha de auditoria.');
          this.aplicarPrecoJustificativa = '';
          this.carregarProdutosCatalogo();
          this.carregarPainel();
        },
        error: () => this.erro.set('Falha ao atualizar o preço do produto.')
      });
  }

  salvarBalancoPatrimonial(): void {
    this.erro.set(null);
    const syncErp = Boolean(this.balancoForm.usarSaldosOperacionaisErp);
    const payload: BalancoPatrimonialGerencialRequest = {
      ...this.balancoForm,
      disponibilidadesCaixaBancos: syncErp ? null : this.balancoForm.disponibilidadesCaixaBancos,
      contasReceberOperacionais: syncErp ? null : this.balancoForm.contasReceberOperacionais,
      estoquesOperacionais: syncErp ? null : this.balancoForm.estoquesOperacionais,
      fornecedoresOperacionaisCp: syncErp ? null : this.balancoForm.fornecedoresOperacionaisCp
    };
    this.api.salvarBalancoPatrimonialGerencial(payload).subscribe({
      next: () => {
        this.mensagemSucesso.set('Balanço Patrimonial Gerencial salvo com sucesso.');
        this.carregarPainel();
      },
      error: () => this.erro.set('Falha ao salvar o Balanço Patrimonial Gerencial.')
    });
  }

  simularInvestimento(): void {
    const fluxos = this.fluxosProjetoTexto
      .split(',')
      .map((s) => Number(s.trim()))
      .filter((n) => !Number.isNaN(n));
    const payload: SimulacaoInvestimentoRequest = {
      ...this.investimentoForm,
      fluxosCaixaProjetados: fluxos
    };
    this.api.simularInvestimentoIndicadores(payload, this.filtroCompetencia).subscribe({
      next: (res) => {
        this.investimentoResultado.set(res);
        this.mensagemSucesso.set('Simulação de VPL, TIR e Payback calculada.');
      },
      error: () => this.erro.set('Não foi possível simular a viabilidade do investimento.')
    });
  }

  salvarParametros(): void {
    this.api.salvarIndicadoresParametros(this.parametrosForm).subscribe({
      next: () => {
        this.mensagemSucesso.set('Benchmarks e parâmetros salvos.');
        this.carregarPainel();
      },
      error: () => this.erro.set('Falha ao salvar os parâmetros da empresa.')
    });
  }

  confiabilidadeBadgeClass(status: string): string {
    if (status === 'CALCULADO') return 'badge--success';
    if (status === 'SIMULADO_OU_PROJETADO') return 'badge--simulado';
    if (status === 'DADOS_INSUFICIENTES' || status === 'DADOS_DESATUALIZADOS') return 'badge--warning';
    return 'badge--danger';
  }

  rentabilidadeBadgeClass(classif: string): string {
    if (classif === 'ESTRELA_ALTA_CONTRIBUICAO') return 'badge--success';
    if (classif === 'MARGEM_CRITICA_OU_NEGATIVA') return 'badge--danger';
    if (classif === 'BAIXO_VOLUME_E_BAIXA_MARGEM') return 'badge--outline';
    if (classif === 'MARGEM_SAUDAVEL_BAIXO_VOLUME') return 'badge--simulado';
    return 'badge--warning';
  }

  severidadeBadgeClass(sev: string): string {
    if (sev === 'CRITICO') return 'badge--danger';
    if (sev === 'ALERTA') return 'badge--warning';
    return 'badge--success';
  }

  formatByUnit(value: number, unit: string): string {
    if (unit === 'R$') return this.formatCurrency(value);
    if (unit === '%') return this.formatPercent(value);
    if (unit === 'DIAS') return `${value.toFixed(2)} dias`;
    return `${value.toFixed(2)}`;
  }

  formatCurrency(value: number | null | undefined): string {
    return Number(value ?? 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  formatCurrencyOrNA(value: number | null | undefined): string {
    return value === null || value === undefined ? 'N/D' : this.formatCurrency(value);
  }

  formatPercent(value: number | null | undefined): string {
    return `${Number(value ?? 0).toFixed(2)}%`;
  }

  formatPercentOrNA(value: number | null | undefined): string {
    return value === null || value === undefined ? 'N/D' : this.formatPercent(value);
  }

  formatDaysOrNA(value: number | null | undefined): string {
    return value === null || value === undefined ? 'N/D' : `${Number(value).toFixed(1)}d`;
  }

  formatIndex(value: number): string {
    return `${Number(value).toFixed(2)}x`;
  }

  formatIndexOrNA(value: number | null | undefined): string {
    return value === null || value === undefined ? 'N/D' : this.formatIndex(value);
  }

  formatUnitsOrNA(value: number | null | undefined): string {
    return value === null || value === undefined ? 'N/D' : `${Number(value).toFixed(0)} un`;
  }
}
