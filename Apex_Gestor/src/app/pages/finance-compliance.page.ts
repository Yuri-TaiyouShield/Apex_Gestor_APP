import { DatePipe, KeyValuePipe, NgClass, NgTemplateOutlet } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { catchError, finalize, of } from 'rxjs';

import { ApexApiService } from '../core/apex-api.service';
import { AdmCalcRequest, FinancialCalculationResponse, FinancialDocument, TaxBracket } from '../core/models';
import { currency } from '../core/view-utils';

type FinanceTab = 'trabalhista' | 'tributario' | 'admcalc' | 'documentos';

@Component({
  standalone: true,
  imports: [IonicModule, ReactiveFormsModule, DatePipe, KeyValuePipe, NgClass, NgTemplateOutlet],
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
          <ion-item class="notice danger" lines="none">
            <ion-icon name="warning-outline" slot="start"></ion-icon>
            <ion-label>{{ error() }}</ion-label>
          </ion-item>
        }

        @if (tab() === 'trabalhista') {
          <section class="grid-two finance-grid">
            <form class="toolbar-card form-panel" [formGroup]="laborForm" (ngSubmit)="calculateLabor()">
              <h2>Folha, benefícios e rescisões</h2>
              <div class="form-grid">
                <ion-input label="Salário mensal" labelPlacement="stacked" type="number" formControlName="salarioMensal"></ion-input>
                <ion-input label="Jornada mensal" labelPlacement="stacked" type="number" formControlName="jornadaMensal"></ion-input>
                <ion-input label="% hora extra" labelPlacement="stacked" type="number" formControlName="percentualHoraExtra"></ion-input>
                <ion-input label="Horas noturnas" labelPlacement="stacked" type="number" formControlName="horasRelogioNoturnas"></ion-input>
                <ion-input label="Verbas variáveis" labelPlacement="stacked" type="number" formControlName="totalVerbasVariaveis"></ion-input>
                <ion-input label="Dias úteis" labelPlacement="stacked" type="number" formControlName="diasUteisMes"></ion-input>
                <ion-input label="Domingos/feriados" labelPlacement="stacked" type="number" formControlName="domingosFeriadosMes"></ion-input>
                <ion-input label="Salário mínimo" labelPlacement="stacked" type="number" formControlName="salarioMinimo"></ion-input>
                <ion-input label="% insalubridade" labelPlacement="stacked" type="number" formControlName="percentualInsalubridade"></ion-input>
                <ion-input label="Custo VT" labelPlacement="stacked" type="number" formControlName="custoRealPassagens"></ion-input>
                <ion-input label="Filhos elegíveis" labelPlacement="stacked" type="number" formControlName="quantidadeFilhosElegiveis"></ion-input>
                <ion-input label="Cota salário-família" labelPlacement="stacked" type="number" formControlName="cotaSalarioFamilia"></ion-input>
                <ion-input label="Meses 13º" labelPlacement="stacked" type="number" formControlName="mesesTrabalhados13"></ion-input>
                <ion-input label="Saldo FGTS" labelPlacement="stacked" type="number" formControlName="saldoFgts"></ion-input>
              </div>
              <ion-button type="submit" expand="block" [disabled]="loading()">Calcular trabalhista</ion-button>
            </form>
            <ng-container *ngTemplateOutlet="resultsTpl"></ng-container>
          </section>
        }

        @if (tab() === 'tributario') {
          <section class="grid-two finance-grid">
            <form class="toolbar-card form-panel" [formGroup]="taxForm" (ngSubmit)="calculateTaxes()">
              <h2>IRRF, Fator R e Lucro Presumido</h2>
              <div class="form-grid">
                <ion-input label="Salário bruto" labelPlacement="stacked" type="number" formControlName="salarioBruto"></ion-input>
                <ion-input label="INSS empregado" labelPlacement="stacked" type="number" formControlName="inssEmpregado"></ion-input>
                <ion-input label="Dependentes" labelPlacement="stacked" type="number" formControlName="dependentes"></ion-input>
                <ion-input label="Dedução dependente" labelPlacement="stacked" type="number" formControlName="deducaoLegalDependente"></ion-input>
                <ion-input label="Pensão" labelPlacement="stacked" type="number" formControlName="pensao"></ion-input>
                <ion-input label="Folha salários" labelPlacement="stacked" type="number" formControlName="folhaSalarios"></ion-input>
                <ion-input label="Pró-labore" labelPlacement="stacked" type="number" formControlName="proLabore"></ion-input>
                <ion-input label="Receita bruta" labelPlacement="stacked" type="number" formControlName="receitaBruta"></ion-input>
                <ion-input label="Receita lucro presumido" labelPlacement="stacked" type="number" formControlName="receitaLucroPresumido"></ion-input>
              </div>
              <ion-button type="submit" expand="block" [disabled]="loading()">Calcular tributário</ion-button>
            </form>
            <ng-container *ngTemplateOutlet="resultsTpl"></ng-container>
          </section>
        }

        @if (tab() === 'admcalc') {
          <section class="grid-two finance-grid">
            <form class="toolbar-card form-panel" [formGroup]="admCalcForm" (ngSubmit)="calculateAdmCalc()">
              <h2>Indicadores do AdmCalc</h2>
              <div class="form-grid">
                <ion-input label="Preço venda" labelPlacement="stacked" type="number" formControlName="precoVenda"></ion-input>
                <ion-input label="Quantidade" labelPlacement="stacked" type="number" formControlName="quantidade"></ion-input>
                <ion-input label="Receita total" labelPlacement="stacked" type="number" formControlName="receitaTotal"></ion-input>
                <ion-input label="CMV" labelPlacement="stacked" type="number" formControlName="cmv"></ion-input>
                <ion-input label="Despesas" labelPlacement="stacked" type="number" formControlName="despesas"></ion-input>
                <ion-input label="Impostos" labelPlacement="stacked" type="number" formControlName="impostos"></ion-input>
                <ion-input label="Custos fixos" labelPlacement="stacked" type="number" formControlName="custosFixos"></ion-input>
                <ion-input label="Custos variáveis" labelPlacement="stacked" type="number" formControlName="custosVariaveis"></ion-input>
                <ion-input label="Ativo circulante" labelPlacement="stacked" type="number" formControlName="ativoCirculante"></ion-input>
                <ion-input label="Passivo circulante" labelPlacement="stacked" type="number" formControlName="passivoCirculante"></ion-input>
                <ion-input label="Custo total" labelPlacement="stacked" type="number" formControlName="custoTotal"></ion-input>
                <ion-input label="Margem desejada" labelPlacement="stacked" type="number" formControlName="margemDesejada"></ion-input>
              </div>
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
                  <ion-input label="Tipo" labelPlacement="stacked" formControlName="tipoDocumento"></ion-input>
                  <ion-input label="Funcionário" labelPlacement="stacked" formControlName="funcionarioNome"></ion-input>
                  <ion-input label="E-mail" labelPlacement="stacked" type="email" formControlName="funcionarioEmail"></ion-input>
                  <ion-input label="Cargo assinante" labelPlacement="stacked" formControlName="cargoAssinanteObrigatorio"></ion-input>
                  <ion-textarea label="Conteúdo" labelPlacement="stacked" formControlName="conteudo" autoGrow="true"></ion-textarea>
                </div>
                <ion-button type="submit" expand="block" [disabled]="loading()">Reter para assinatura</ion-button>
              </form>

              <form class="sign-form" [formGroup]="signForm" (ngSubmit)="signDocument()">
                <h2>Assinar e enviar</h2>
                <div class="form-grid single">
                  <ion-input label="ID do documento" labelPlacement="stacked" type="number" formControlName="idDocumento"></ion-input>
                  <ion-input label="Nome do assinante" labelPlacement="stacked" formControlName="nomeAssinante"></ion-input>
                  <ion-input label="Cargo" labelPlacement="stacked" formControlName="cargoAssinante"></ion-input>
                  <ion-input label="Fingerprint certificado" labelPlacement="stacked" formControlName="certificadoFingerprint"></ion-input>
                </div>
                <ion-button type="submit" expand="block" color="success" [disabled]="loading()">Assinar documento</ion-button>
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
                      <strong>{{ formatResult(item.key, item.value) }}</strong>
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
      background: #ffffff;
      border: 1px solid var(--apex-border);
      border-radius: 8px;
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
      color: var(--apex-ink);
      font-size: 1rem;
      margin: 0 0 14px;
    }

    .form-grid {
      display: grid;
      gap: 12px;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      margin-bottom: 16px;
    }

    .form-grid.single {
      grid-template-columns: 1fr;
    }

    ion-input,
    ion-textarea {
      --background: #f8fafc;
      --border-color: var(--apex-border);
      --border-radius: 8px;
      --border-style: solid;
      --border-width: 1px;
      --padding-start: 12px;
      --padding-end: 12px;
      color: var(--apex-ink);
    }

    .notice {
      border: 1px solid rgba(185, 28, 28, 0.22);
      border-radius: 8px;
      margin-bottom: 14px;
    }

    .notice.danger {
      --background: rgba(185, 28, 28, 0.08);
      color: var(--apex-danger);
    }

    .result-meta,
    .result-item,
    .doc-row {
      align-items: center;
      display: flex;
      justify-content: space-between;
    }

    .result-meta {
      color: var(--apex-muted);
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
      background: #f8fafc;
      border: 1px solid var(--apex-border);
      border-radius: 8px;
      gap: 10px;
      padding: 12px;
    }

    .result-item span {
      color: var(--apex-muted);
      font-size: 0.82rem;
    }

    .result-item strong {
      color: var(--apex-ink);
      font-size: 0.98rem;
      text-align: right;
    }

    .sign-form {
      border-top: 1px solid var(--apex-border);
      margin-top: 18px;
      padding-top: 16px;
    }

    .doc-list {
      display: grid;
      gap: 10px;
    }

    .doc-row {
      background: #ffffff;
      border: 1px solid var(--apex-border);
      border-radius: 8px;
      color: var(--apex-ink);
      cursor: pointer;
      min-height: 56px;
      padding: 10px 12px;
      text-align: left;
      width: 100%;
    }

    .doc-row small {
      color: var(--apex-muted);
      display: block;
      margin-top: 3px;
    }

    .doc-row i {
      background: rgba(100, 116, 139, 0.14);
      border-radius: 999px;
      color: #334155;
      font-size: 0.72rem;
      font-style: normal;
      font-weight: 800;
      padding: 5px 8px;
    }

    .doc-row i.enviado,
    .doc-row i.assinado {
      background: rgba(22, 101, 52, 0.12);
      color: var(--apex-success);
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

  readonly laborForm = this.fb.nonNullable.group({
    salarioMensal: [3000, [Validators.required, Validators.min(0.01)]],
    jornadaMensal: [220, [Validators.required, Validators.min(1)]],
    percentualHoraExtra: [0.5],
    horasRelogioNoturnas: [10],
    totalVerbasVariaveis: [600],
    diasUteisMes: [22],
    domingosFeriadosMes: [5],
    salarioMinimo: [1500],
    percentualInsalubridade: [0.2],
    custoRealPassagens: [250],
    quantidadeFilhosElegiveis: [1],
    cotaSalarioFamilia: [62],
    mesesTrabalhados13: [6],
    saldoFgts: [10000]
  });

  readonly taxForm = this.fb.nonNullable.group({
    salarioBruto: [5000],
    inssEmpregado: [550],
    dependentes: [1],
    deducaoLegalDependente: [189.59],
    pensao: [0],
    folhaSalarios: [28000],
    proLabore: [2000],
    receitaBruta: [100000],
    receitaLucroPresumido: [100000]
  });

  readonly admCalcForm = this.fb.nonNullable.group({
    precoVenda: [49.9],
    quantidade: [100],
    receitaTotal: [10000],
    cmv: [6000],
    despesas: [1200],
    impostos: [800],
    custosFixos: [3000],
    custosVariaveis: [4000],
    ativoCirculante: [80000],
    passivoCirculante: [30000],
    custoTotal: [7000],
    margemDesejada: [0.3]
  });

  readonly documentForm = this.fb.nonNullable.group({
    tipoDocumento: ['Holerite'],
    funcionarioNome: ['Colaborador Apex'],
    funcionarioEmail: ['colaborador@empresa.com'],
    cargoAssinanteObrigatorio: ['CONTADOR'],
    conteudo: ['Documento financeiro gerado pelo Apex Gestor e retido para assinatura digital.']
  });

  readonly signForm = this.fb.nonNullable.group({
    idDocumento: [0, [Validators.required, Validators.min(1)]],
    nomeAssinante: ['Contador Responsável'],
    cargoAssinante: ['CONTADOR'],
    certificadoFingerprint: ['CERT-LOCAL-DEV']
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
    const payload = {
      ...this.laborForm.getRawValue(),
      salarioBase: this.laborForm.controls.salarioMensal.value,
      salarioContribuicao: this.laborForm.controls.salarioMensal.value,
      tabelaInss: this.defaultInssTable(),
      percentualMultaFgts: 0.4,
      anosCompletosTrabalhados: 2,
      maiorRemuneracao: this.laborForm.controls.salarioMensal.value,
      verbasIncontroversas: 2000,
      salariosAteTerminoContrato: 12000,
      mesesRestantesEstabilidade: 3,
      valorNominal: 1000,
      fatorIpcaEAcumulado: 1.1,
      taxaSelicAcumulada: 0.05
    };
    this.runCalculation(this.api.laborCalculation(payload));
  }

  calculateTaxes(): void {
    const payload = {
      ...this.taxForm.getRawValue(),
      tabelaIrrf: this.defaultIrrfTable(),
      limiteAdicionalIrpj: 20000
    };
    this.runCalculation(this.api.taxCalculation(payload));
  }

  calculateAdmCalc(): void {
    const raw = this.admCalcForm.getRawValue();
    const payload: AdmCalcRequest = {
      ...raw,
      margemContribuicao: 50,
      ativoNaoCirculante: 120000,
      passivoNaoCirculante: 40000,
      estoque: 10000,
      custoBem: 25000,
      valorResidual: 5000,
      vidaUtil: 5,
      quantidadeProduzida: 350,
      ganhoInvestimento: 15000,
      custoInvestimento: 10000,
      valorPresente: 10000,
      valorFuturo: 16105.1,
      taxaDesconto: 0.1,
      periodo: 5,
      fluxosDeCaixa: [-1000, 300, 400, 500, 600]
    };
    this.runCalculation(this.api.admCalcCalculation(payload));
  }

  createDocument(): void {
    if (this.documentForm.invalid) {
      this.error.set('Preencha os dados mínimos do documento.');
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
      this.error.set('Informe o ID do documento e os dados do assinante.');
      return;
    }
    const { idDocumento, ...signature } = this.signForm.getRawValue();
    this.loading.set(true);
    this.error.set(null);
    this.api.signFinancialDocument(idDocumento, { ...signature, aprovado: true }).pipe(
      catchError((error: unknown) => {
        this.error.set(this.messageFromError(error));
        return of(null);
      }),
      finalize(() => this.loading.set(false))
    ).subscribe((document) => {
      if (document) {
        this.documents.update((items) => items.map((item) => item.idDocumento === document.idDocumento ? document : item));
      }
    });
  }

  selectDocument(id: number): void {
    this.signForm.controls.idDocumento.setValue(id);
  }

  labelFor(key: string): string {
    return key.replace(/([A-Z])/g, ' $1').replace(/^./, (char) => char.toUpperCase());
  }

  formatResult(key: string, value: number): string {
    if (key.toLowerCase().includes('fator') || key.toLowerCase().includes('margem') || key.toLowerCase() === 'roi' || key.toLowerCase() === 'tir') {
      return `${value.toLocaleString('pt-BR', { maximumFractionDigits: 2 })}%`;
    }
    return this.money(value);
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
    if (typeof error === 'object' && error !== null && 'status' in error) {
      const status = Number((error as { status?: number }).status);
      if (status === 401 || status === 403) {
        return 'Acesso restrito: entre com usuário financeiro, administrador, contador ou advogado autorizado.';
      }
      if (status === 402) {
        return 'Licença não validada para este aplicativo. Ative a chave em Configurações.';
      }
    }
    return 'Não foi possível concluir a operação financeira agora.';
  }
}
