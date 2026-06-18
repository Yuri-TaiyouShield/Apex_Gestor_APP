import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { IonicModule } from '@ionic/angular';
import { catchError, forkJoin, of } from 'rxjs';

import { ApexApiService } from '../core/apex-api.service';
import { RelatorioOperacional } from '../core/models';
import { currency } from '../core/view-utils';

type ReportKind = 'clientes' | 'estoque' | 'funcionarios' | 'vendas' | 'fornecedores';

@Component({
  standalone: true,
  imports: [CommonModule, IonicModule],
  template: `
    <ion-content>
      <main class="page">
        <header class="page-header">
          <div>
            <span class="page-kicker">Relatorios gerenciais</span>
            <h1>Relatorios</h1>
            <p>Clientes, estoque, funcionarios, vendas e fornecedores em uma leitura unica para o gestor.</p>
          </div>
          <ion-button (click)="loadReports()">
            <ion-icon slot="start" name="sync-outline"></ion-icon>
            Atualizar
          </ion-button>
        </header>

        @if (offline()) {
          <div class="state-line warning">
            <ion-icon name="cloud-offline-outline" aria-hidden="true"></ion-icon>
            API indisponível. Relatórios exibindo amostra operacional para manter a tela utilizável.
          </div>
        }

        <section class="report-grid">
          @for (report of reports(); track report.tipo) {
            <ion-card class="data-card report-card">
              <ion-card-header>
                <ion-card-title>{{ report.tipo }}</ion-card-title>
                <ion-card-subtitle>{{ subtitleFor(report.tipo) }}</ion-card-subtitle>
              </ion-card-header>
              <ion-card-content>
                <div class="report-metrics">
                  <span>
                    <small>Total</small>
                    <strong>{{ report.totalRegistros }}</strong>
                  </span>
                  <span>
                    <small>Ativos</small>
                    <strong>{{ report.ativos }}</strong>
                  </span>
                  <span>
                    <small>Inativos</small>
                    <strong>{{ report.inativos }}</strong>
                  </span>
                </div>

                @if (report.valorTotal > 0) {
                  <div class="report-value">
                    <small>Valor consolidado</small>
                    <strong>{{ money(report.valorTotal) }}</strong>
                  </div>
                }

                @if (report.alertas.length) {
                  <div class="alert-list">
                    @for (alert of report.alertas; track alert) {
                      <ion-note color="warning">{{ alert }}</ion-note>
                    }
                  </div>
                }
              </ion-card-content>
            </ion-card>
          } @empty {
            <ion-card class="data-card">
              <ion-card-content>
                <div class="empty-state">Nenhum relatorio carregado.</div>
              </ion-card-content>
            </ion-card>
          }
        </section>
      </main>
    </ion-content>
  `,
  styles: [`
    .report-grid {
      display: grid;
      gap: 16px;
      grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
    }

    .state-line {
      align-items: center;
      background: var(--apex-accent-soft);
      border: 1px solid rgba(180, 105, 14, 0.24);
      border-radius: 8px;
      color: var(--apex-warning);
      display: flex;
      font-size: 0.86rem;
      gap: 8px;
      margin-bottom: 12px;
      padding: 10px 12px;
    }

    .report-card ion-card-content {
      display: grid;
      gap: 14px;
    }

    .report-metrics {
      display: grid;
      gap: 8px;
      grid-template-columns: repeat(3, minmax(0, 1fr));
    }

    .report-metrics span,
    .report-value {
      background: var(--apex-card-muted);
      border: 1px solid var(--apex-border);
      border-radius: 8px;
      display: grid;
      gap: 4px;
      padding: 10px;
    }

    .report-metrics small,
    .report-value small {
      color: var(--apex-muted);
      font-size: 0.74rem;
      font-weight: 750;
    }

    .report-metrics strong,
    .report-value strong {
      color: var(--apex-ink);
      font-size: 1.2rem;
      font-weight: 850;
    }

    .alert-list {
      display: grid;
      gap: 6px;
    }
  `]
})
export class ReportsPage {
  private readonly api = inject(ApexApiService);
  readonly reports = signal<RelatorioOperacional[]>([]);
  readonly offline = signal(false);
  readonly money = currency;

  constructor() {
    this.loadReports();
  }

  loadReports(): void {
    const kinds: ReportKind[] = ['clientes', 'estoque', 'funcionarios', 'vendas', 'fornecedores'];
    this.reports.set(kinds.map((kind) => this.mockReport(kind)));
    this.offline.set(false);
    forkJoin(kinds.map((kind) => this.api.operationalReport(kind).pipe(catchError(() => {
      this.offline.set(true);
      return of(this.mockReport(kind));
    })))).subscribe((reports) => {
      this.reports.set(reports);
    });
  }

  subtitleFor(tipo: string): string {
    const subtitles: Record<string, string> = {
      Clientes: 'Base CRM e relacionamento',
      Estoque: 'Saldo, valor e minimo operacional',
      Funcionarios: 'Usuarios, equipe e ponto',
      Vendas: 'Operacoes realizadas no PDV',
      Fornecedores: 'Base de compras e reposicao'
    };
    return subtitles[tipo] ?? 'Resumo operacional';
  }

  private mockReport(kind: ReportKind): RelatorioOperacional {
    const labels: Record<ReportKind, string> = {
      clientes: 'Clientes',
      estoque: 'Estoque',
      funcionarios: 'Funcionarios',
      vendas: 'Vendas',
      fornecedores: 'Fornecedores'
    };
    return {
      tipo: labels[kind],
      totalRegistros: this.mockTotals[kind].totalRegistros,
      ativos: this.mockTotals[kind].ativos,
      inativos: this.mockTotals[kind].inativos,
      valorTotal: this.mockTotals[kind].valorTotal,
      alertas: this.mockTotals[kind].alertas
    };
  }

  private readonly mockTotals: Record<ReportKind, Omit<RelatorioOperacional, 'tipo'>> = {
    clientes: {
      totalRegistros: 42,
      ativos: 39,
      inativos: 3,
      valorTotal: 0,
      alertas: ['8 clientes sem compra nos últimos 60 dias']
    },
    estoque: {
      totalRegistros: 128,
      ativos: 124,
      inativos: 4,
      valorTotal: 184300,
      alertas: ['6 produtos abaixo do estoque mínimo', '2 itens sem venda há mais de 90 dias']
    },
    funcionarios: {
      totalRegistros: 12,
      ativos: 11,
      inativos: 1,
      valorTotal: 0,
      alertas: ['3 ajustes de ponto aguardando aprovação']
    },
    vendas: {
      totalRegistros: 316,
      ativos: 309,
      inativos: 7,
      valorTotal: 89640,
      alertas: ['7 vendas canceladas exigem conferência de auditoria']
    },
    fornecedores: {
      totalRegistros: 18,
      ativos: 17,
      inativos: 1,
      valorTotal: 0,
      alertas: ['4 fornecedores sem pedido nos últimos 90 dias']
    }
  };
}
