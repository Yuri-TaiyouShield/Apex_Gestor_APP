import { DatePipe, KeyValuePipe, NgClass, NgTemplateOutlet } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
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
  IonNote,
  IonSegment,
  IonSegmentButton,
  IonSelect,
  IonSelectOption,
  IonTextarea
} from '@ionic/angular/standalone';
import { catchError, finalize, of } from 'rxjs';

import { ApexApiService } from '../core/apex-api.service';
import {
  AdmCalcRequest,
  FinancialCalculationResponse,
  FinancialDocument,
  LaborCalculationRequest,
  TaxBracket,
  TaxCalculationRequest
} from '../core/models';
import { currency } from '../core/view-utils';

type FinanceTab = 'trabalhista' | 'tributario' | 'admcalc' | 'documentos';
type PercentScale = 'PERCENTUAL' | 'DECIMAL' | 'AUTO';

const RESULT_LABELS: Record<string, string> = {
  // AdmCalcEngine (27 indicadores)
  faturamento: 'Faturamento',
  lucroBruto: 'Lucro Bruto',
  lucroLiquido: 'Lucro Líquido',
  margemLucroBruta: 'Margem de Lucro Bruta',
  margemLucroLiquida: 'Margem de Lucro Líquida',
  margemContribuicao: 'Margem de Contribuição (Valor)',
  margemContribuicaoPercentual: 'Margem de Contribuição (%)',
  pontoEquilibrio: 'Ponto de Equilíbrio (Unidades)',
  pontoEquilibrioReceita: 'Ponto de Equilíbrio (Receita)',
  pontoEquilibrioUnidades: 'Ponto de Equilíbrio (Unidades)',
  ativoTotal: 'Ativo Total',
  passivoTotal: 'Passivo Total',
  patrimonioLiquido: 'Patrimônio Líquido',
  capitalGiro: 'Capital de Giro',
  liquidezCorrente: 'Liquidez Corrente',
  liquidezSeca: 'Liquidez Seca',
  depreciacaoAnual: 'Depreciação Anual',
  depreciacaoMensal: 'Depreciação Mensal',
  custoTotal: 'Custo Total',
  custoUnitario: 'Custo Unitário',
  precoMarkup: 'Preço Markup',
  markupMultiplicador: 'Markup Multiplicador',
  roi: 'ROI',
  valorPresente: 'Valor Presente',
  valorFuturo: 'Valor Futuro',
  vpl: 'VPL',
  tir: 'TIR',

  // LaborCalculationEngine
  salarioHora: 'Salário Hora',
  salarioDiario: 'Salário Diário',
  horaExtraComum: 'Hora Extra Comum',
  fatorReducaoNoturna: 'Horas Noturnas Reduzidas',
  horaExtraNoturna: 'Hora Extra Noturna',
  dsrVariaveis: 'DSR sobre Variáveis',
  baseFeriasDecimoComDsr: 'Base Férias/13º com DSR',
  horaSobreaviso: 'Hora Sobreaviso',
  horaProntidao: 'Hora Prontidão',
  adicionalInsalubridade: 'Adicional de Insalubridade',
  adicionalPericulosidade: 'Adicional de Periculosidade',
  salarioHoraIntegrado: 'Salário Hora Integrado',
  descontoValeTransporte: 'Desconto Vale-Transporte',
  salarioFamilia: 'Salário-Família',
  decimoTerceiroProporcional: '13º Salário Proporcional',
  feriasComTerco: 'Férias com 1/3',
  descontoFaltasDsr: 'Desconto de Faltas e DSR',
  inssEmpregado: 'INSS Empregado',
  avisoPrevioIndenizado: 'Aviso Prévio Indenizado',
  multaFgts: 'Multa FGTS',
  multa477: 'Multa Art. 477 CLT',
  multa467: 'Multa Art. 467 CLT',
  indenizacao479: 'Indenização Art. 479 CLT',
  indenizacaoEstabilidade: 'Indenização de Estabilidade',
  atualizacaoPreJudicial: 'Atualização Pré-Judicial',
  atualizacaoJudicial: 'Atualização Judicial',

  // TaxCalculationEngine
  baseIrrf: 'Base de Cálculo IRRF',
  irrf: 'IRRF Retido',
  fatorR: 'Fator R',
  lucroPresumidoBaseIrpj: 'Lucro Presumido — Base IRPJ',
  lucroPresumidoIrpj: 'Lucro Presumido — IRPJ',
  lucroPresumidoCsll: 'Lucro Presumido — CSLL',
  lucroPresumidoPisCofins: 'Lucro Presumido — PIS/COFINS',
  lucroPresumidoAdicionalIrpj: 'Lucro Presumido — Adicional IRPJ'
};

@Component({
  standalone: true,
  imports: [
    IonContent,
    IonBadge,
    IonSegment,
    IonSegmentButton,
    IonLabel,
    IonItem,
    IonIcon,
    IonInput,
    IonSelect,
    IonSelectOption,
    IonCheckbox,
    IonButton,
    IonTextarea,
    IonCard,
    IonCardHeader,
    IonCardTitle,
    IonCardSubtitle,
    IonCardContent,
    IonNote,
    ReactiveFormsModule,
    DatePipe,
    KeyValuePipe,
    NgClass,
    NgTemplateOutlet
  ],
  template: `
    <ion-content>
      <main class="page finance-page">
        <header class="page-header">
          <div>
            <h1>Financeiro inteligente</h1>
            <p>AdmCalc integrado, cálculos trabalhistas, tributários, auditoria e documentos digitais.</p>
          </div>
          <ion-badge color="primary">Acesso restrito</ion-badge>
        </header>

        <ion-segment [value]="tab()" (ionChange)="setTab($event.detail.value)">
          <ion-segment-button value="trabalhista">
            <ion-label>Trabalhista</ion-label>
          </ion-segment-button>
          <ion-segment-button value="tributario">
            <ion-label>Tributário</ion-label>
          </ion-segment-button>
          <ion-segment-button value="admcalc">
            <ion-label>AdmCalc</ion-label>
          </ion-segment-button>
          <ion-segment-button value="documentos">
            <ion-label>Documentos</ion-label>
          </ion-segment-button>
        </ion-segment>

        @if (error()) {
          <ion-item class="notice danger" lines="none" role="alert">
            <ion-icon name="warning-outline" slot="start" aria-hidden="true"></ion-icon>
            <ion-label>{{ error() }}</ion-label>
          </ion-item>
        }

        @if (tab() === 'trabalhista') {
          <section class="grid-two finance-grid">
            <form class="toolbar-card form-panel" [formGroup]="laborForm" (ngSubmit)="calculateLabor()">
              <h2>Folha, benefícios e rescisões</h2>

              <fieldset class="calc-section">
                <legend>Parâmetros principais</legend>
                <div class="form-grid">
                  <ion-input label="Salário mensal (obrigatório)" labelPlacement="stacked" type="number" formControlName="salarioMensal"></ion-input>
                  <ion-input label="Jornada mensal (obrigatório)" labelPlacement="stacked" type="number" formControlName="jornadaMensal"></ion-input>
                  <ion-input label="Salário base (opcional)" labelPlacement="stacked" type="number" placeholder="Padrão: salário mensal" formControlName="salarioBase"></ion-input>
                  <ion-input label="% hora extra (opcional)" labelPlacement="stacked" type="number" placeholder="Ex.: 0.5 ou 50" formControlName="percentualHoraExtra"></ion-input>
                  <ion-input label="Horas noturnas (opcional)" labelPlacement="stacked" type="number" placeholder="Ex.: 10" formControlName="horasRelogioNoturnas"></ion-input>
                  <ion-input label="Verbas variáveis (opcional)" labelPlacement="stacked" type="number" placeholder="Ex.: 600" formControlName="totalVerbasVariaveis"></ion-input>
                  <ion-input label="Dias úteis no mês (opcional)" labelPlacement="stacked" type="number" placeholder="Ex.: 22" formControlName="diasUteisMes"></ion-input>
                  <ion-input label="Domingos/feriados (opcional)" labelPlacement="stacked" type="number" placeholder="Ex.: 5" formControlName="domingosFeriadosMes"></ion-input>
                </div>
              </fieldset>

              <fieldset class="calc-section">
                <legend>Adicionais, benefícios e encargos (opcionais)</legend>
                <div class="form-grid">
                  <ion-input label="Salário mínimo ref. (opcional)" labelPlacement="stacked" type="number" placeholder="Ex.: 1500" formControlName="salarioMinimo"></ion-input>
                  <ion-input label="% insalubridade (opcional)" labelPlacement="stacked" type="number" placeholder="Ex.: 0.2 ou 20" formControlName="percentualInsalubridade"></ion-input>
                  <ion-input label="Custo VT (opcional)" labelPlacement="stacked" type="number" placeholder="Ex.: 250" formControlName="custoRealPassagens"></ion-input>
                  <ion-input label="Filhos elegíveis (opcional)" labelPlacement="stacked" type="number" placeholder="Ex.: 1" formControlName="quantidadeFilhosElegiveis"></ion-input>
                  <ion-input label="Cota salário-família (opcional)" labelPlacement="stacked" type="number" placeholder="Ex.: 62" formControlName="cotaSalarioFamilia"></ion-input>
                  <ion-input label="Média adicionais (opcional)" labelPlacement="stacked" type="number" formControlName="mediaAdicionais"></ion-input>
                  <ion-input label="Média DSR (opcional)" labelPlacement="stacked" type="number" formControlName="mediaDsr"></ion-input>
                  <ion-input label="Meses 13º (opcional)" labelPlacement="stacked" type="number" placeholder="Ex.: 6" formControlName="mesesTrabalhados13"></ion-input>
                  <ion-input label="Dias de falta (opcional)" labelPlacement="stacked" type="number" formControlName="diasFalta"></ion-input>
                  <ion-input label="DSR por falta (opcional)" labelPlacement="stacked" type="number" formControlName="dsrSemanaFalta"></ion-input>
                  <ion-input label="Salário contribuição INSS (opcional)" labelPlacement="stacked" type="number" formControlName="salarioContribuicao"></ion-input>
                </div>
                <ion-checkbox formControlName="usarTabelaInssReferencia">
                  Aplicar tabela progressiva INSS de referência (até R$ 2.000: 8% | acima: 9%)
                </ion-checkbox>
              </fieldset>

              <fieldset class="calc-section">
                <legend>Rescisão e atualização monetária (parâmetros opcionais)</legend>
                <div class="form-grid">
                  <ion-input label="Anos completos trabalhados (opcional)" labelPlacement="stacked" type="number" formControlName="anosCompletosTrabalhados"></ion-input>
                  <ion-input label="Saldo FGTS (opcional)" labelPlacement="stacked" type="number" formControlName="saldoFgts"></ion-input>
                  <ion-input label="% multa FGTS (opcional)" labelPlacement="stacked" type="number" placeholder="Padrão: 0.40" formControlName="percentualMultaFgts"></ion-input>
                  <ion-input label="Maior remuneração (opcional)" labelPlacement="stacked" type="number" formControlName="maiorRemuneracao"></ion-input>
                  <ion-input label="Verbas incontroversas art. 467 (opcional)" labelPlacement="stacked" type="number" formControlName="verbasIncontroversas"></ion-input>
                  <ion-input label="Salários até término art. 479 (opcional)" labelPlacement="stacked" type="number" formControlName="salariosAteTerminoContrato"></ion-input>
                  <ion-input label="Meses restantes estabilidade (opcional)" labelPlacement="stacked" type="number" formControlName="mesesRestantesEstabilidade"></ion-input>
                  <ion-input label="Valor nominal atualização (opcional)" labelPlacement="stacked" type="number" formControlName="valorNominal"></ion-input>
                  <ion-input label="Fator IPCA-E acumulado (opcional)" labelPlacement="stacked" type="number" placeholder="Ex.: 1.1" formControlName="fatorIpcaEAcumulado"></ion-input>
                  <ion-input label="Taxa SELIC acumulada (opcional)" labelPlacement="stacked" type="number" placeholder="Ex.: 0.05" formControlName="taxaSelicAcumulada"></ion-input>
                </div>
              </fieldset>

              <ion-button type="submit" expand="block" [disabled]="loading()">Calcular trabalhista</ion-button>
            </form>
            <ng-container *ngTemplateOutlet="resultsTpl"></ng-container>
          </section>
        }

        @if (tab() === 'tributario') {
          <section class="grid-two finance-grid">
            <form class="toolbar-card form-panel" [formGroup]="taxForm" (ngSubmit)="calculateTaxes()">
              <h2>IRRF, Fator R e Lucro Presumido</h2>

              <fieldset class="calc-section">
                <legend>IRRF (parâmetros opcionais)</legend>
                <div class="form-grid">
                  <ion-input label="Salário bruto (opcional)" labelPlacement="stacked" type="number" placeholder="Ex.: 5000" formControlName="salarioBruto"></ion-input>
                  <ion-input label="INSS empregado (opcional)" labelPlacement="stacked" type="number" placeholder="Ex.: 550" formControlName="inssEmpregado"></ion-input>
                  <ion-input label="Dependentes (opcional)" labelPlacement="stacked" type="number" placeholder="Ex.: 1" formControlName="dependentes"></ion-input>
                  <ion-input label="Dedução por dependente (opcional)" labelPlacement="stacked" type="number" placeholder="Ex.: 189.59" formControlName="deducaoLegalDependente"></ion-input>
                  <ion-input label="Pensão alimentícia (opcional)" labelPlacement="stacked" type="number" placeholder="Ex.: 0" formControlName="pensao"></ion-input>
                </div>
                <ion-checkbox formControlName="usarTabelaIrrfReferencia">
                  Aplicar tabela IRRF de referência (até R$ 2.500 isento | até R$ 5.000: 15% | acima: 27,5%)
                </ion-checkbox>
              </fieldset>

              <fieldset class="calc-section">
                <legend>Fator R e Lucro Presumido (parâmetros opcionais)</legend>
                <div class="form-grid">
                  <ion-input label="Folha salários (opcional)" labelPlacement="stacked" type="number" placeholder="Ex.: 28000" formControlName="folhaSalarios"></ion-input>
                  <ion-input label="Pró-labore (opcional)" labelPlacement="stacked" type="number" placeholder="Ex.: 2000" formControlName="proLabore"></ion-input>
                  <ion-input label="Receita bruta (opcional)" labelPlacement="stacked" type="number" placeholder="Ex.: 100000" formControlName="receitaBruta"></ion-input>
                  <ion-input label="Receita lucro presumido (opcional)" labelPlacement="stacked" type="number" placeholder="Ex.: 100000" formControlName="receitaLucroPresumido"></ion-input>
                  <ion-input label="Limite adicional IRPJ (opcional)" labelPlacement="stacked" type="number" placeholder="Padrão backend: 20000" formControlName="limiteAdicionalIrpj"></ion-input>
                </div>
              </fieldset>

              <ion-button type="submit" expand="block" [disabled]="loading()">Calcular tributário</ion-button>
            </form>
            <ng-container *ngTemplateOutlet="resultsTpl"></ng-container>
          </section>
        }

        @if (tab() === 'admcalc') {
          <section class="grid-two finance-grid">
            <form class="toolbar-card form-panel" [formGroup]="admCalcForm" (ngSubmit)="calculateAdmCalc()">
              <h2>Indicadores do AdmCalc</h2>

              <fieldset class="calc-section">
                <legend>Escala de taxas e margens</legend>
                <div class="form-grid single">
                  <ion-select label="Escala percentual" labelPlacement="stacked" formControlName="escalaPercentual">
                    <ion-select-option value="PERCENTUAL">PERCENTUAL (ex.: 20 = 20%)</ion-select-option>
                    <ion-select-option value="DECIMAL">DECIMAL (ex.: 0.20 = 20%)</ion-select-option>
                    <ion-select-option value="AUTO">AUTO (legado)</ion-select-option>
                  </ion-select>
                </div>
                <ion-note class="scale-hint">
                  Use PERCENTUAL (ex.: 20 = 20%) ou DECIMAL (ex.: 0.20 = 20%) para margem desejada e taxa de desconto.
                </ion-note>
              </fieldset>

              <fieldset class="calc-section">
                <legend>Precificação e lucratividade</legend>
                <div class="form-grid">
                  <ion-input label="Preço de venda (R$)" labelPlacement="stacked" type="number" formControlName="precoVenda"></ion-input>
                  <ion-input label="Quantidade" labelPlacement="stacked" type="number" formControlName="quantidade"></ion-input>
                  <ion-input label="Receita total (R$)" labelPlacement="stacked" type="number" formControlName="receitaTotal"></ion-input>
                  <ion-input label="CMV (R$)" labelPlacement="stacked" type="number" formControlName="cmv"></ion-input>
                  <ion-input label="Despesas (R$)" labelPlacement="stacked" type="number" formControlName="despesas"></ion-input>
                  <ion-input label="Impostos (R$)" labelPlacement="stacked" type="number" formControlName="impostos"></ion-input>
                  <ion-input label="Margem desejada" labelPlacement="stacked" type="number" formControlName="margemDesejada"></ion-input>
                </div>
              </fieldset>

              <fieldset class="calc-section">
                <legend>Custos e ponto de equilíbrio</legend>
                <div class="form-grid">
                  <ion-input label="Custos fixos (R$)" labelPlacement="stacked" type="number" formControlName="custosFixos"></ion-input>
                  <ion-input label="Custos variáveis (R$)" labelPlacement="stacked" type="number" formControlName="custosVariaveis"></ion-input>
                  <ion-input label="Margem contrib. unitária (R$/un)" labelPlacement="stacked" type="number" formControlName="margemContribuicao"></ion-input>
                  <ion-input label="Custo total (R$)" labelPlacement="stacked" type="number" formControlName="custoTotal"></ion-input>
                  <ion-input label="Quantidade produzida (un)" labelPlacement="stacked" type="number" formControlName="quantidadeProduzida"></ion-input>
                </div>
              </fieldset>

              <fieldset class="calc-section">
                <legend>Balanço e liquidez</legend>
                <div class="form-grid">
                  <ion-input label="Ativo circulante (R$)" labelPlacement="stacked" type="number" formControlName="ativoCirculante"></ion-input>
                  <ion-input label="Ativo não circulante (R$)" labelPlacement="stacked" type="number" formControlName="ativoNaoCirculante"></ion-input>
                  <ion-input label="Passivo circulante (R$)" labelPlacement="stacked" type="number" formControlName="passivoCirculante"></ion-input>
                  <ion-input label="Passivo não circulante (R$)" labelPlacement="stacked" type="number" formControlName="passivoNaoCirculante"></ion-input>
                  <ion-input label="Estoque (R$)" labelPlacement="stacked" type="number" formControlName="estoque"></ion-input>
                </div>
              </fieldset>

              <fieldset class="calc-section">
                <legend>Depreciação</legend>
                <div class="form-grid">
                  <ion-input label="Custo do bem (R$)" labelPlacement="stacked" type="number" formControlName="custoBem"></ion-input>
                  <ion-input label="Valor residual (R$)" labelPlacement="stacked" type="number" formControlName="valorResidual"></ion-input>
                  <ion-input label="Vida útil (anos)" labelPlacement="stacked" type="number" formControlName="vidaUtil"></ion-input>
                </div>
              </fieldset>

              <fieldset class="calc-section">
                <legend>Investimentos, valor no tempo e fluxos de caixa</legend>
                <div class="form-grid">
                  <ion-input label="Ganho do investimento (R$)" labelPlacement="stacked" type="number" formControlName="ganhoInvestimento"></ion-input>
                  <ion-input label="Custo do investimento (R$)" labelPlacement="stacked" type="number" formControlName="custoInvestimento"></ion-input>
                  <ion-input label="Valor presente (R$)" labelPlacement="stacked" type="number" formControlName="valorPresente"></ion-input>
                  <ion-input label="Valor futuro (R$)" labelPlacement="stacked" type="number" formControlName="valorFuturo"></ion-input>
                  <ion-input label="Taxa de desconto" labelPlacement="stacked" type="number" formControlName="taxaDesconto"></ion-input>
                  <ion-input label="Período" labelPlacement="stacked" type="number" formControlName="periodo"></ion-input>
                </div>
                <div class="form-grid single">
                  <ion-input
                    label="Fluxos de caixa (separados por vírgula)"
                    labelPlacement="stacked"
                    type="text"
                    placeholder="-1000, 300, 400, 500, 600"
                    formControlName="fluxosDeCaixaTexto"
                  ></ion-input>
                </div>
              </fieldset>

              <ion-button type="submit" expand="block" [disabled]="loading()">Calcular AdmCalc</ion-button>
            </form>
            <ng-container *ngTemplateOutlet="resultsTpl"></ng-container>
          </section>
        }

        @if (tab() === 'documentos') {
          <section class="grid-two finance-grid">
            <div class="toolbar-card form-panel">
              <form [formGroup]="documentForm" (ngSubmit)="createDocument()">
                <h2>Gerar documento retido</h2>
                <div class="form-grid single">
                  <ion-input label="Tipo" labelPlacement="stacked" placeholder="Ex.: Holerite" formControlName="tipoDocumento"></ion-input>
                  <ion-input label="Funcionário" labelPlacement="stacked" placeholder="Nome completo" formControlName="funcionarioNome"></ion-input>
                  <ion-input label="E-mail" labelPlacement="stacked" type="email" placeholder="nome@empresa.com" formControlName="funcionarioEmail"></ion-input>
                  <ion-input label="Cargo assinante" labelPlacement="stacked" placeholder="Ex.: CONTADOR" formControlName="cargoAssinanteObrigatorio"></ion-input>
                  <ion-textarea label="Conteúdo" labelPlacement="stacked" placeholder="Texto do documento" formControlName="conteudo" autoGrow="true"></ion-textarea>
                </div>
                <ion-button type="submit" expand="block" [disabled]="loading()">Reter para assinatura</ion-button>
              </form>

              <form class="sign-form" [formGroup]="signForm" (ngSubmit)="signDocument()">
                <h2>Assinar e enviar</h2>
                <div class="form-grid single">
                  <ion-input label="ID do documento" labelPlacement="stacked" type="number" formControlName="idDocumento"></ion-input>
                  <ion-input label="Nome do assinante" labelPlacement="stacked" placeholder="Confirme seu nome completo" formControlName="nomeAssinante"></ion-input>
                  <ion-input label="Cargo" labelPlacement="stacked" placeholder="Ex.: CONTADOR, ADVOGADO" formControlName="cargoAssinante"></ion-input>
                  <ion-input label="Fingerprint do certificado" labelPlacement="stacked" placeholder="Informe o hash/fingerprint real do certificado" formControlName="certificadoFingerprint"></ion-input>
                  <ion-select label="Decisão da assinatura" labelPlacement="stacked" formControlName="aprovado">
                    <ion-select-option [value]="true">Aprovar e assinar documento</ion-select-option>
                    <ion-select-option [value]="false">Reprovar / recusar documento</ion-select-option>
                  </ion-select>
                  <ion-input label="Observação (opcional)" labelPlacement="stacked" placeholder="Justificativa ou nota de conferência" formControlName="observacao"></ion-input>
                </div>
                <ion-button
                  type="submit"
                  expand="block"
                  [color]="signForm.controls.aprovado.value ? 'success' : 'danger'"
                  [disabled]="loading()"
                >
                  {{ signForm.controls.aprovado.value ? 'Assinar e aprovar documento' : 'Registrar reprovação' }}
                </ion-button>
              </form>
            </div>

            <ion-card class="data-card">
              <ion-card-header>
                <ion-card-title>Documentos recentes</ion-card-title>
                <ion-card-subtitle>Retenção, aprovação e envio automático</ion-card-subtitle>
              </ion-card-header>
              <ion-card-content>
                @if (documents().length === 0) {
                  <div class="empty-state">Nenhum documento financeiro gerado nesta sessão.</div>
                } @else {
                  <div class="doc-list">
                    @for (document of documents(); track document.idDocumento) {
                      <button type="button" class="doc-row" (click)="selectDocument(document.idDocumento)">
                        <span>
                          <strong>#{{ document.idDocumento }} {{ document.tipoDocumento }}</strong>
                          <small>{{ document.funcionarioNome }} | {{ document.cargoAssinanteObrigatorio }}</small>
                        </span>
                        <i [ngClass]="document.status.toLowerCase()">{{ document.status }}</i>
                      </button>
                    }
                  </div>
                }
              </ion-card-content>
            </ion-card>
          </section>
        }

        <ng-template #resultsTpl>
          <ion-card class="data-card">
            <ion-card-header>
              <ion-card-title>Resultado auditado</ion-card-title>
              <ion-card-subtitle>Cada execução fica registrada no backend</ion-card-subtitle>
            </ion-card-header>
            <ion-card-content>
              @if (!result()) {
                <div class="empty-state">Execute um cálculo para visualizar os indicadores.</div>
              } @else {
                <div class="result-meta">
                  <span>#{{ result()?.calculationId }} | {{ result()?.tipo }}</span>
                  <span>{{ result()?.calculadoEm | date:'short' }}</span>
                </div>
                <div class="result-grid">
                  @for (item of result()?.resultados | keyvalue; track item.key) {
                    <div class="result-item">
                      <span>{{ labelFor(item.key) }}</span>
                      <strong class="apex-tabular">{{ formatResult(item.key, item.value) }}</strong>
                    </div>
                  }
                </div>
                @for (alert of result()?.alertas ?? []; track alert) {
                  <ion-note>{{ alert }}</ion-note>
                }
              }
            </ion-card-content>
          </ion-card>
        </ng-template>
      </main>
    </ion-content>
  `,
  styles: [`
    .finance-page ion-segment {
      background: var(--apex-bg-surface);
      border: 1px solid var(--apex-border-default);
      border-radius: var(--apex-radius-md);
      margin-bottom: 16px;
      padding: 4px;
    }

    .finance-grid {
      align-items: start;
    }

    .form-panel {
      padding: 16px;
    }

    .form-panel h2 {
      color: var(--apex-text-primary);
      font-size: 1rem;
      margin: 0 0 14px;
    }

    .calc-section {
      border: 1px solid var(--apex-border-subtle);
      border-radius: var(--apex-radius-md);
      margin: 0 0 14px;
      padding: 12px;
    }

    .calc-section legend {
      color: var(--apex-text-secondary);
      font-size: var(--apex-font-size-xs);
      font-weight: 750;
      padding: 0 6px;
    }

    .scale-hint {
      display: block;
      margin-top: -6px;
    }

    .form-grid {
      display: grid;
      gap: 12px;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      margin-bottom: 12px;
    }

    .form-grid.single {
      grid-template-columns: 1fr;
    }

    ion-input,
    ion-select,
    ion-textarea {
      --background: var(--apex-bg-input);
      --border-color: var(--apex-border-default);
      --border-radius: 8px;
      --border-style: solid;
      --border-width: 1px;
      --padding-start: 12px;
      --padding-end: 12px;
      color: var(--apex-text-primary);
    }

    .notice {
      border: 1px solid var(--apex-status-danger-border);
      border-radius: 8px;
      margin-bottom: 14px;
    }

    .notice.danger {
      --background: var(--apex-status-danger-bg);
      color: var(--apex-status-danger);
    }

    .result-meta,
    .result-item,
    .doc-row {
      align-items: center;
      display: flex;
      justify-content: space-between;
    }

    .result-meta {
      color: var(--apex-text-muted);
      font-size: 0.82rem;
      margin-bottom: 14px;
    }

    .result-grid {
      display: grid;
      gap: 10px;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      margin-bottom: 14px;
    }

    .result-item {
      background: var(--apex-bg-surface-muted);
      border: 1px solid var(--apex-border-default);
      border-radius: 8px;
      gap: 10px;
      padding: 12px;
    }

    .result-item span {
      color: var(--apex-text-muted);
      font-size: 0.82rem;
    }

    .result-item strong {
      color: var(--apex-text-primary);
      font-size: 0.98rem;
      text-align: right;
    }

    .sign-form {
      border-top: 1px solid var(--apex-border-default);
      margin-top: 18px;
      padding-top: 16px;
    }

    .doc-list {
      display: grid;
      gap: 10px;
    }

    .doc-row {
      background: var(--apex-bg-surface);
      border: 1px solid var(--apex-border-default);
      border-radius: 8px;
      color: var(--apex-text-primary);
      cursor: pointer;
      min-height: 56px;
      padding: 10px 12px;
      text-align: left;
      width: 100%;
    }

    .doc-row small {
      color: var(--apex-text-muted);
      display: block;
      margin-top: 3px;
    }

    .doc-row i {
      background: var(--apex-bg-surface-subtle);
      border-radius: 999px;
      color: var(--apex-text-secondary);
      font-size: 0.72rem;
      font-style: normal;
      font-weight: 800;
      padding: 5px 8px;
    }

    .doc-row i.enviado,
    .doc-row i.assinado {
      background: var(--apex-status-success-bg);
      color: var(--apex-status-success);
    }

    @media (max-width: 720px) {
      .form-grid,
      .result-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class FinanceCompliancePage {
  private readonly api = inject(ApexApiService);
  private readonly fb = inject(FormBuilder);

  readonly tab = signal<FinanceTab>('trabalhista');
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly result = signal<FinancialCalculationResponse | null>(null);
  readonly documents = signal<FinancialDocument[]>([]);

  readonly laborForm = this.fb.group({
    salarioMensal: this.fb.nonNullable.control(3000, [Validators.required, Validators.min(0.01)]),
    jornadaMensal: this.fb.nonNullable.control(220, [Validators.required, Validators.min(1)]),
    salarioBase: [null as number | null],
    percentualHoraExtra: [null as number | null],
    horasRelogioNoturnas: [null as number | null],
    totalVerbasVariaveis: [null as number | null],
    diasUteisMes: [null as number | null],
    domingosFeriadosMes: [null as number | null],
    salarioMinimo: [null as number | null],
    percentualInsalubridade: [null as number | null],
    custoRealPassagens: [null as number | null],
    quantidadeFilhosElegiveis: [null as number | null],
    cotaSalarioFamilia: [null as number | null],
    mediaAdicionais: [null as number | null],
    mediaDsr: [null as number | null],
    mesesTrabalhados13: [null as number | null],
    diasFalta: [null as number | null],
    dsrSemanaFalta: [null as number | null],
    salarioContribuicao: [null as number | null],
    usarTabelaInssReferencia: this.fb.nonNullable.control(false),
    anosCompletosTrabalhados: [null as number | null],
    saldoFgts: [null as number | null],
    percentualMultaFgts: [null as number | null],
    maiorRemuneracao: [null as number | null],
    verbasIncontroversas: [null as number | null],
    salariosAteTerminoContrato: [null as number | null],
    mesesRestantesEstabilidade: [null as number | null],
    valorNominal: [null as number | null],
    fatorIpcaEAcumulado: [null as number | null],
    taxaSelicAcumulada: [null as number | null]
  });

  readonly taxForm = this.fb.group({
    salarioBruto: [null as number | null],
    inssEmpregado: [null as number | null],
    dependentes: [null as number | null],
    deducaoLegalDependente: [null as number | null],
    pensao: [null as number | null],
    usarTabelaIrrfReferencia: this.fb.nonNullable.control(false),
    folhaSalarios: [null as number | null],
    proLabore: [null as number | null],
    receitaBruta: [null as number | null],
    receitaLucroPresumido: [null as number | null],
    limiteAdicionalIrpj: [null as number | null]
  });

  readonly admCalcForm = this.fb.group({
    escalaPercentual: this.fb.nonNullable.control<PercentScale>('PERCENTUAL'),
    precoVenda: [49.9 as number | null],
    quantidade: [100 as number | null],
    receitaTotal: [10000 as number | null],
    cmv: [6000 as number | null],
    despesas: [1200 as number | null],
    impostos: [800 as number | null],
    custosFixos: [3000 as number | null],
    custosVariaveis: [4000 as number | null],
    margemContribuicao: [50 as number | null],
    ativoCirculante: [80000 as number | null],
    ativoNaoCirculante: [120000 as number | null],
    passivoCirculante: [30000 as number | null],
    passivoNaoCirculante: [40000 as number | null],
    estoque: [10000 as number | null],
    custoBem: [25000 as number | null],
    valorResidual: [5000 as number | null],
    vidaUtil: [5 as number | null],
    custoTotal: [7000 as number | null],
    quantidadeProduzida: [350 as number | null],
    margemDesejada: [30 as number | null],
    ganhoInvestimento: [15000 as number | null],
    custoInvestimento: [10000 as number | null],
    valorPresente: [10000 as number | null],
    valorFuturo: [16105.1 as number | null],
    taxaDesconto: [10 as number | null],
    periodo: [5 as number | null],
    fluxosDeCaixaTexto: ['-1000, 300, 400, 500, 600']
  });

  readonly documentForm = this.fb.nonNullable.group({
    tipoDocumento: ['', Validators.required],
    funcionarioNome: ['', Validators.required],
    funcionarioEmail: ['', [Validators.required, Validators.email]],
    cargoAssinanteObrigatorio: ['CONTADOR', Validators.required],
    conteudo: ['', Validators.required]
  });

  readonly signForm = this.fb.nonNullable.group({
    idDocumento: [0, [Validators.required, Validators.min(1)]],
    nomeAssinante: ['', Validators.required],
    cargoAssinante: ['', Validators.required],
    certificadoFingerprint: ['', [Validators.required, Validators.minLength(4)]],
    aprovado: [true],
    observacao: ['']
  });

  readonly money = currency;

  constructor() {
    this.loadDocuments();
  }

  setTab(value: unknown): void {
    if (value === 'trabalhista' || value === 'tributario' || value === 'admcalc' || value === 'documentos') {
      this.tab.set(value);
      this.error.set(null);
      if (value === 'documentos') {
        this.loadDocuments();
      }
    }
  }

  calculateLabor(): void {
    if (this.laborForm.invalid) {
      this.error.set('Preencha salário e jornada com valores válidos.');
      return;
    }
    const raw = this.laborForm.getRawValue();
    const payload: LaborCalculationRequest = {
      salarioMensal: Number(raw.salarioMensal),
      jornadaMensal: Number(raw.jornadaMensal)
    };
    assignIfDefined(payload, 'salarioBase', toOptionalNumber(raw.salarioBase));
    assignIfDefined(payload, 'percentualHoraExtra', toOptionalNumber(raw.percentualHoraExtra));
    assignIfDefined(payload, 'horasRelogioNoturnas', toOptionalNumber(raw.horasRelogioNoturnas));
    assignIfDefined(payload, 'totalVerbasVariaveis', toOptionalNumber(raw.totalVerbasVariaveis));
    assignIfDefined(payload, 'diasUteisMes', toOptionalNumber(raw.diasUteisMes));
    assignIfDefined(payload, 'domingosFeriadosMes', toOptionalNumber(raw.domingosFeriadosMes));
    assignIfDefined(payload, 'salarioMinimo', toOptionalNumber(raw.salarioMinimo));
    assignIfDefined(payload, 'percentualInsalubridade', toOptionalNumber(raw.percentualInsalubridade));
    assignIfDefined(payload, 'custoRealPassagens', toOptionalNumber(raw.custoRealPassagens));
    assignIfDefined(payload, 'quantidadeFilhosElegiveis', toOptionalNumber(raw.quantidadeFilhosElegiveis));
    assignIfDefined(payload, 'cotaSalarioFamilia', toOptionalNumber(raw.cotaSalarioFamilia));
    assignIfDefined(payload, 'mediaAdicionais', toOptionalNumber(raw.mediaAdicionais));
    assignIfDefined(payload, 'mediaDsr', toOptionalNumber(raw.mediaDsr));
    assignIfDefined(payload, 'mesesTrabalhados13', toOptionalNumber(raw.mesesTrabalhados13));
    assignIfDefined(payload, 'diasFalta', toOptionalNumber(raw.diasFalta));
    assignIfDefined(payload, 'dsrSemanaFalta', toOptionalNumber(raw.dsrSemanaFalta));
    assignIfDefined(payload, 'salarioContribuicao', toOptionalNumber(raw.salarioContribuicao));
    assignIfDefined(payload, 'anosCompletosTrabalhados', toOptionalNumber(raw.anosCompletosTrabalhados));
    assignIfDefined(payload, 'saldoFgts', toOptionalNumber(raw.saldoFgts));
    assignIfDefined(payload, 'percentualMultaFgts', toOptionalNumber(raw.percentualMultaFgts));
    assignIfDefined(payload, 'maiorRemuneracao', toOptionalNumber(raw.maiorRemuneracao));
    assignIfDefined(payload, 'verbasIncontroversas', toOptionalNumber(raw.verbasIncontroversas));
    assignIfDefined(payload, 'salariosAteTerminoContrato', toOptionalNumber(raw.salariosAteTerminoContrato));
    assignIfDefined(payload, 'mesesRestantesEstabilidade', toOptionalNumber(raw.mesesRestantesEstabilidade));
    assignIfDefined(payload, 'valorNominal', toOptionalNumber(raw.valorNominal));
    assignIfDefined(payload, 'fatorIpcaEAcumulado', toOptionalNumber(raw.fatorIpcaEAcumulado));
    assignIfDefined(payload, 'taxaSelicAcumulada', toOptionalNumber(raw.taxaSelicAcumulada));
    if (raw.usarTabelaInssReferencia) {
      payload.tabelaInss = this.defaultInssTable();
    }
    this.runCalculation(this.api.laborCalculation(payload));
  }

  calculateTaxes(): void {
    const raw = this.taxForm.getRawValue();
    const payload: TaxCalculationRequest = {};
    assignIfDefined(payload, 'salarioBruto', toOptionalNumber(raw.salarioBruto));
    assignIfDefined(payload, 'inssEmpregado', toOptionalNumber(raw.inssEmpregado));
    assignIfDefined(payload, 'dependentes', toOptionalNumber(raw.dependentes));
    assignIfDefined(payload, 'deducaoLegalDependente', toOptionalNumber(raw.deducaoLegalDependente));
    assignIfDefined(payload, 'pensao', toOptionalNumber(raw.pensao));
    assignIfDefined(payload, 'folhaSalarios', toOptionalNumber(raw.folhaSalarios));
    assignIfDefined(payload, 'proLabore', toOptionalNumber(raw.proLabore));
    assignIfDefined(payload, 'receitaBruta', toOptionalNumber(raw.receitaBruta));
    assignIfDefined(payload, 'receitaLucroPresumido', toOptionalNumber(raw.receitaLucroPresumido));
    assignIfDefined(payload, 'limiteAdicionalIrpj', toOptionalNumber(raw.limiteAdicionalIrpj));
    if (raw.usarTabelaIrrfReferencia) {
      payload.tabelaIrrf = this.defaultIrrfTable();
    }
    this.runCalculation(this.api.taxCalculation(payload));
  }

  calculateAdmCalc(): void {
    const raw = this.admCalcForm.getRawValue();
    const parsedCashFlows = this.parseCashFlows(raw.fluxosDeCaixaTexto);
    if (parsedCashFlows.invalid) {
      this.error.set('Fluxos de caixa devem conter apenas números separados por vírgula ou ponto e vírgula.');
      return;
    }

    const payload: AdmCalcRequest = {
      escalaPercentual: raw.escalaPercentual,
      precoVenda: toOptionalNumber(raw.precoVenda),
      quantidade: toOptionalNumber(raw.quantidade),
      receitaTotal: toOptionalNumber(raw.receitaTotal),
      cmv: toOptionalNumber(raw.cmv),
      despesas: toOptionalNumber(raw.despesas),
      impostos: toOptionalNumber(raw.impostos),
      custosFixos: toOptionalNumber(raw.custosFixos),
      custosVariaveis: toOptionalNumber(raw.custosVariaveis),
      margemContribuicao: toOptionalNumber(raw.margemContribuicao),
      ativoCirculante: toOptionalNumber(raw.ativoCirculante),
      ativoNaoCirculante: toOptionalNumber(raw.ativoNaoCirculante),
      passivoCirculante: toOptionalNumber(raw.passivoCirculante),
      passivoNaoCirculante: toOptionalNumber(raw.passivoNaoCirculante),
      estoque: toOptionalNumber(raw.estoque),
      custoBem: toOptionalNumber(raw.custoBem),
      valorResidual: toOptionalNumber(raw.valorResidual),
      vidaUtil: toOptionalNumber(raw.vidaUtil),
      custoTotal: toOptionalNumber(raw.custoTotal),
      quantidadeProduzida: toOptionalNumber(raw.quantidadeProduzida),
      margemDesejada: toOptionalNumber(raw.margemDesejada),
      ganhoInvestimento: toOptionalNumber(raw.ganhoInvestimento),
      custoInvestimento: toOptionalNumber(raw.custoInvestimento),
      valorPresente: toOptionalNumber(raw.valorPresente),
      valorFuturo: toOptionalNumber(raw.valorFuturo),
      taxaDesconto: toOptionalNumber(raw.taxaDesconto),
      periodo: toOptionalNumber(raw.periodo),
      fluxosDeCaixa: parsedCashFlows.values
    };
    this.runCalculation(this.api.admCalcCalculation(payload));
  }

  createDocument(): void {
    if (this.documentForm.invalid) {
      this.error.set('Preencha os dados válidos do documento antes de reter para assinatura.');
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.api.createFinancialDocument(this.documentForm.getRawValue()).pipe(
      catchError((error: unknown) => {
        this.error.set(this.messageFromError(error));
        return of(null);
      }),
      finalize(() => this.loading.set(false))
    ).subscribe((document) => {
      if (document) {
        this.documents.update((items) => [document, ...items]);
        this.selectDocument(document.idDocumento);
      }
    });
  }

  signDocument(): void {
    if (this.signForm.invalid) {
      this.error.set('Informe o ID do documento, nome, cargo e fingerprint real do certificado do assinante.');
      return;
    }
    const { idDocumento, observacao, ...signature } = this.signForm.getRawValue();
    this.loading.set(true);
    this.error.set(null);
    this.api
      .signFinancialDocument(idDocumento, {
        ...signature,
        nomeAssinante: signature.nomeAssinante.trim(),
        cargoAssinante: signature.cargoAssinante.trim(),
        certificadoFingerprint: signature.certificadoFingerprint.trim(),
        observacao: observacao.trim() || undefined
      })
      .pipe(
        catchError((error: unknown) => {
          this.error.set(this.messageFromError(error));
          return of(null);
        }),
        finalize(() => this.loading.set(false))
      )
      .subscribe((document) => {
        if (document) {
          this.documents.update((items) => items.map((item) => item.idDocumento === document.idDocumento ? document : item));
        }
      });
  }

  selectDocument(id: number): void {
    this.signForm.controls.idDocumento.setValue(id);
  }

  labelFor(key: string): string {
    if (RESULT_LABELS[key]) {
      return RESULT_LABELS[key];
    }
    return key.replace(/([A-Z])/g, ' $1').replace(/^./, (char) => char.toUpperCase());
  }

  formatResult(key: string, value: number): string {
    const normalized = key.toLowerCase();
    if (normalized === 'margemcontribuicao') {
      return this.money(value);
    }
    if (
      normalized === 'pontoequilibrio' ||
      normalized === 'pontoequilibriounidades'
    ) {
      return `${formatDecimal(value)} un`;
    }
    if (
      normalized === 'liquidezcorrente' ||
      normalized === 'liquidezseca' ||
      normalized === 'markupmultiplicador'
    ) {
      return `${formatDecimal(value)} x`;
    }
    if (normalized === 'fatorreducaonoturna') {
      return `${formatDecimal(value)} h`;
    }
    if (
      normalized.startsWith('margem') ||
      normalized.startsWith('taxa') ||
      normalized === 'roi' ||
      normalized === 'tir' ||
      normalized === 'fatorr' ||
      normalized.includes('percentual') ||
      normalized.includes('%')
    ) {
      return `${formatDecimal(value)}%`;
    }
    return this.money(value);
  }

  private parseCashFlows(rawText: string | null | undefined): { values?: number[]; invalid: boolean } {
    const trimmed = (rawText ?? '').trim();
    if (!trimmed) {
      return { values: undefined, invalid: false };
    }
    const tokens = trimmed.split(/[;,]/).map((token) => token.trim()).filter((token) => token.length > 0);
    if (tokens.length === 0) {
      return { values: undefined, invalid: false };
    }
    const numbers: number[] = [];
    for (const token of tokens) {
      const parsed = Number(token);
      if (!Number.isFinite(parsed)) {
        return { invalid: true };
      }
      numbers.push(parsed);
    }
    return { values: numbers, invalid: false };
  }

  private runCalculation(source: ReturnType<ApexApiService['laborCalculation']>): void {
    this.loading.set(true);
    this.error.set(null);
    source.pipe(
      catchError((error: unknown) => {
        this.error.set(this.messageFromError(error));
        return of(null);
      }),
      finalize(() => this.loading.set(false))
    ).subscribe((response) => {
      if (response) {
        this.result.set(response);
      }
    });
  }

  private loadDocuments(): void {
    this.api.financialDocuments().pipe(
      catchError(() => of([]))
    ).subscribe((documents) => this.documents.set(documents));
  }

  private defaultInssTable(): TaxBracket[] {
    return [
      { limite: 2000, aliquota: 0.08 },
      { limite: null, aliquota: 0.09 }
    ];
  }

  private defaultIrrfTable(): TaxBracket[] {
    return [
      { limite: 2500, aliquota: 0, parcelaDeduzir: 0 },
      { limite: 5000, aliquota: 0.15, parcelaDeduzir: 370 },
      { limite: null, aliquota: 0.275, parcelaDeduzir: 900 }
    ];
  }

  private messageFromError(error: unknown): string {
    if (typeof error === 'object' && error !== null) {
      const status = 'status' in error ? Number((error as { status?: number }).status) : undefined;
      if (status === 401 || status === 403) {
        return 'Acesso restrito: entre com usuário financeiro, administrador, contador ou advogado autorizado.';
      }
      if (status === 402) {
        return 'Licença não validada para este aplicativo. Ative a chave em Configurações.';
      }
      const payload = (error as { error?: { detail?: string; message?: string } }).error;
      if (payload?.detail || payload?.message) {
        return payload.detail || payload.message || 'Não foi possível concluir a operação financeira agora.';
      }
    }
    return 'Não foi possível concluir a operação financeira agora.';
  }
}

function toOptionalNumber(value: unknown): number | undefined {
  if (value === null || value === undefined || String(value).trim() === '') {
    return undefined;
  }
  const numeric = Number(value);
  return Number.isFinite(numeric) ? numeric : undefined;
}

function assignIfDefined<T extends object, K extends keyof T>(target: T, key: K, value: T[K] | undefined): void {
  if (value !== undefined) {
    target[key] = value;
  }
}

function formatDecimal(value: number): string {
  return value.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
