import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IonContent, IonBadge, IonCard, IonCardHeader, IonCardTitle, IonCardSubtitle, IonCardContent, IonItem, IonInput, IonButton, IonNote } from '@ionic/angular/standalone';
import { catchError, of } from 'rxjs';

import { AuthService } from '../core/auth.service';
import { PrivacyService } from '../core/privacy.service';

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule, IonContent, IonBadge, IonCard, IonCardHeader, IonCardTitle, IonCardSubtitle, IonCardContent, IonItem, IonInput, IonButton, IonNote],
  template: `
    <ion-content>
      <main class="page">
        <header class="page-header">
          <div>
            <h1>Privacidade e LGPD</h1>
            <p>Consentimento, exportação, anonimização e trilha de auditoria.</p>
          </div>
          <ion-badge color="primary">Privacy by design</ion-badge>
        </header>

        <section class="grid-two">
          <ion-card class="data-card">
            <ion-card-header>
              <ion-card-title>Direitos do titular</ion-card-title>
              <ion-card-subtitle>Solicitações ficam protocoladas no backend.</ion-card-subtitle>
            </ion-card-header>
            <ion-card-content class="stack">
              <ion-item>
                <ion-input type="number" label="ID do cliente" label-placement="stacked" [(ngModel)]="clienteId"></ion-input>
              </ion-item>
              <ion-item>
                <ion-input label="Justificativa formal (mínimo 10 caracteres)" label-placement="stacked" [(ngModel)]="justificativa"></ion-input>
              </ion-item>
              <ion-button expand="block" (click)="requestExport()">Solicitar exportação</ion-button>
              <ion-button expand="block" color="warning" fill="outline" (click)="requestDeletion()">Solicitar anonimização/exclusão</ion-button>
              <ion-button expand="block" fill="outline" (click)="downloadExport()">Baixar exportação agora</ion-button>
              <ion-button expand="block" fill="outline" (click)="simulateAnonymization()">Simular anonimização</ion-button>
              @if (simulacaoHash()) {
                <ion-button expand="block" color="danger" (click)="confirmAnonymization()">Confirmar anonimização irreversível</ion-button>
              }
              @if (message()) {
                <ion-note color="primary">{{ message() }}</ion-note>
              }
            </ion-card-content>
          </ion-card>

          <ion-card class="data-card">
            <ion-card-header>
              <ion-card-title>Mascaramento para suporte</ion-card-title>
              <ion-card-subtitle>Exemplos de pseudonimização em tela.</ion-card-subtitle>
            </ion-card-header>
            <ion-card-content class="stack">
              <ion-item>
                <ion-input label="Documento" label-placement="stacked" [(ngModel)]="documento"></ion-input>
              </ion-item>
              <ion-item>
                <ion-input label="E-mail" label-placement="stacked" [(ngModel)]="email"></ion-input>
              </ion-item>
              <div class="masked-box">
                <span>Documento mascarado</span>
                <strong>{{ privacy.maskDocument(documento) }}</strong>
              </div>
              <div class="masked-box">
                <span>E-mail mascarado</span>
                <strong>{{ privacy.maskEmail(email) }}</strong>
              </div>
              <ion-button expand="block" (click)="logout()" [disabled]="!auth.isAuthenticated()">Encerrar sessão</ion-button>
            </ion-card-content>
          </ion-card>
        </section>

        @if (exportJson()) {
          <ion-card class="data-card">
            <ion-card-header>
              <ion-card-title>Exportação / Simulação do titular</ion-card-title>
            </ion-card-header>
            <ion-card-content>
              <pre>{{ exportJson() }}</pre>
            </ion-card-content>
          </ion-card>
        }
      </main>
    </ion-content>
  `,
  styles: [`
    .masked-box {
      background: var(--apex-bg-surface-muted);
      border: 1px solid var(--apex-border);
      border-radius: 8px;
      display: grid;
      gap: 6px;
      padding: 14px;
    }

    .masked-box span {
      color: var(--apex-text-muted);
      font-size: 0.82rem;
    }

    pre {
      background: #0f172a;
      border-radius: 8px;
      color: #e2e8f0;
      max-height: 360px;
      overflow: auto;
      padding: 14px;
    }
  `]
})
export class PrivacyPage {
  readonly privacy = inject(PrivacyService);
  readonly auth = inject(AuthService);
  readonly message = signal('');
  readonly exportJson = signal('');
  readonly simulacaoHash = signal('');
  clienteId = 1;
  justificativa = '';
  documento = '123.456.789-09';
  email = 'cliente@email.com';

  requestExport(): void {
    const just = this.validJustificativa();
    if (!just) return;
    this.privacy.request({ titularId: String(this.clienteId), tipoTitular: 'CLIENTE', tipo: 'EXPORTACAO', justificativa: just })
      .pipe(catchError((error) => { this.message.set(error?.error?.detail || 'Não foi possível registrar a solicitação na API.'); return of(null); }))
      .subscribe((response) => { if (response) this.message.set(`Solicitação de exportação registrada: ${JSON.stringify(response)}`); });
  }

  requestDeletion(): void {
    const just = this.validJustificativa();
    if (!just) return;
    this.privacy.request({ titularId: String(this.clienteId), tipoTitular: 'CLIENTE', tipo: 'ANONIMIZACAO', justificativa: just })
      .pipe(catchError((error) => { this.message.set(error?.error?.detail || 'Não foi possível registrar a solicitação na API.'); return of(null); }))
      .subscribe((response) => { if (response) this.message.set(`Solicitação de anonimização registrada: ${JSON.stringify(response)}`); });
  }

  downloadExport(): void {
    const just = this.validJustificativa();
    if (!just) return;
    this.privacy.exportCliente(Number(this.clienteId), just)
      .pipe(catchError((error) => of({ message: error?.error?.detail || 'Exportação indisponível ou restrita a proprietário/SYSADMIN.' })))
      .subscribe((response) => this.exportJson.set(JSON.stringify(response, null, 2)));
  }

  simulateAnonymization(): void {
    this.privacy.simularAnonimizacao(Number(this.clienteId))
      .pipe(catchError((error) => { this.message.set(error?.error?.detail || 'Falha ao simular anonimização.'); return of(null); }))
      .subscribe((sim) => {
        if (!sim) return;
        this.simulacaoHash.set(sim.simulacaoHash);
        this.exportJson.set(JSON.stringify(sim, null, 2));
        this.message.set(sim.elegivelAnonimizacaoPessoal
          ? 'Simulação elegível. Informe justificativa e confirme explicitamente se aplicável.'
          : `Bloqueio ativo: ${sim.obrigacoesRetencao.join(' | ')}`);
      });
  }

  confirmAnonymization(): void {
    const just = this.validJustificativa();
    const hash = this.simulacaoHash();
    if (!just || !hash) return;
    this.privacy.anonymizeCliente(Number(this.clienteId), {
      confirmadoExplicito: true,
      justificativa: just,
      simulacaoHash: hash
    })
      .pipe(catchError((error) => { this.message.set(error?.error?.detail || 'Não foi possível concluir a anonimização.'); return of(null); }))
      .subscribe((res) => { if (res) this.message.set(`Anonimização executada: ${JSON.stringify(res)}`); });
  }

  logout(): void {
    this.auth.logout();
    this.message.set('Sessão encerrada.');
  }

  private validJustificativa(): string | null {
    const just = this.justificativa.trim();
    if (just.length < 10) {
      this.message.set('Informe uma justificativa formal com pelo menos 10 caracteres.');
      return null;
    }
    return just;
  }
}
