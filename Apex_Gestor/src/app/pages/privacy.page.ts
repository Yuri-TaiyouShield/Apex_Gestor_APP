import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { catchError, of } from 'rxjs';

import { AuthService } from '../core/auth.service';
import { PrivacyService } from '../core/privacy.service';

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule],
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
              <ion-button expand="block" (click)="requestExport()">Solicitar exportação</ion-button>
              <ion-button expand="block" color="danger" fill="outline" (click)="requestDeletion()">Solicitar exclusão/anonimização</ion-button>
              <ion-button expand="block" fill="outline" (click)="downloadExport()">Baixar exportação agora</ion-button>
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
              <ion-card-title>Exportação do titular</ion-card-title>
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
      background: #f8fafc;
      border: 1px solid var(--apex-border);
      border-radius: 8px;
      display: grid;
      gap: 6px;
      padding: 14px;
    }

    .masked-box span {
      color: #64748b;
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
  clienteId = 1;
  documento = '123.456.789-09';
  email = 'cliente@email.com';

  requestExport(): void {
    this.privacy.request({ titularId: String(this.clienteId), tipo: 'EXPORTACAO' })
      .pipe(catchError(() => of({ offline: true })))
      .subscribe((response) => this.message.set(`Solicitação de exportação registrada: ${JSON.stringify(response)}`));
  }

  requestDeletion(): void {
    this.privacy.request({ titularId: String(this.clienteId), tipo: 'EXCLUSAO' })
      .pipe(catchError(() => of({ offline: true })))
      .subscribe((response) => this.message.set(`Solicitação de exclusão registrada: ${JSON.stringify(response)}`));
  }

  downloadExport(): void {
    this.privacy.exportCliente(Number(this.clienteId))
      .pipe(catchError(() => of({ message: 'Exportação indisponível sem API autenticada.' })))
      .subscribe((response) => this.exportJson.set(JSON.stringify(response, null, 2)));
  }

  logout(): void {
    this.auth.logout();
    this.message.set('Sessão encerrada.');
  }
}
