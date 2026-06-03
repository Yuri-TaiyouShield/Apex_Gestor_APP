import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { catchError, of, switchMap } from 'rxjs';

import { AuthService } from '../core/auth.service';
import { Persona } from '../core/models';
import { SessionService } from '../core/session.service';

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule],
  template: `
    <ion-content>
      <main class="login-page">
        <section class="login-panel">
          <div class="brand-mark">A</div>
          <h1>Acesso seguro Apex</h1>
          <p>JWT com expiração curta, refresh token rotativo e aceite LGPD auditável.</p>

          <label class="field">
            <span>Login</span>
            <input type="text" autocomplete="username" [(ngModel)]="login">
          </label>
          <label class="field">
            <span>Senha</span>
            <input type="password" autocomplete="current-password" [(ngModel)]="senha">
          </label>
          <label class="field">
            <span>Código 2FA</span>
            <input type="text" inputmode="numeric" placeholder="Opcional nesta fase" [(ngModel)]="totpCode">
          </label>
          <label class="check-field">
            <input type="checkbox" [(ngModel)]="acceptedPrivacyTerms">
            <span>Li e aceito a política de privacidade v1.0</span>
          </label>

          @if (message()) {
            <ion-note [color]="messageColor()">{{ message() }}</ion-note>
          }

          <button class="primary-action" type="button" (click)="enter()" [disabled]="!login || !senha || !acceptedPrivacyTerms">
            Entrar com segurança
          </button>

          <ion-accordion-group>
            <ion-accordion value="demo">
              <ion-item slot="header">
                <ion-label>Modo demonstração sem backend</ion-label>
              </ion-item>
              <div class="demo-panel" slot="content">
                <ion-segment [(ngModel)]="persona">
                  <ion-segment-button value="cliente">Cliente</ion-segment-button>
                  <ion-segment-button value="vendedor">Vendedor</ion-segment-button>
                  <ion-segment-button value="gerente">Gerente</ion-segment-button>
                  <ion-segment-button value="admin">Admin</ion-segment-button>
                </ion-segment>
                <button class="secondary-action" type="button" (click)="enterDemo()">Entrar em modo demo</button>
              </div>
            </ion-accordion>
          </ion-accordion-group>
        </section>
      </main>
    </ion-content>
  `,
  styles: [`
    .login-page {
      align-items: center;
      display: flex;
      min-height: 100%;
      justify-content: center;
      padding: 18px;
    }

    .login-panel {
      background: #ffffff;
      border: 1px solid var(--apex-border);
      border-radius: 8px;
      box-shadow: 0 22px 50px rgba(15, 23, 42, 0.12);
      max-width: 480px;
      padding: 24px;
      width: 100%;
    }

    .brand-mark {
      align-items: center;
      background: linear-gradient(135deg, #0f766e, #2563eb);
      border-radius: 8px;
      color: #fff;
      display: flex;
      font-size: 1.4rem;
      font-weight: 800;
      height: 52px;
      justify-content: center;
      width: 52px;
    }

    h1 {
      margin: 16px 0 6px;
    }

    p {
      color: #64748b;
      margin: 0 0 18px;
    }

    ion-note {
      display: block;
      margin: 12px 0;
    }

    .field,
    .check-field {
      display: grid;
      gap: 6px;
      margin-bottom: 12px;
    }

    .field span,
    .check-field span {
      color: #334155;
      font-size: 0.9rem;
      font-weight: 700;
    }

    .field input {
      background: #f8fafc;
      border: 1px solid var(--apex-border);
      border-radius: 8px;
      color: #0f172a;
      font: inherit;
      min-height: 46px;
      padding: 10px 12px;
      width: 100%;
    }

    .field input:focus {
      border-color: #0f766e;
      box-shadow: 0 0 0 3px rgba(15, 118, 110, 0.16);
      outline: none;
    }

    .check-field {
      align-items: center;
      grid-template-columns: 22px 1fr;
      margin-top: 4px;
    }

    .check-field input {
      accent-color: #0f766e;
      height: 18px;
      width: 18px;
    }

    .primary-action,
    .secondary-action {
      border-radius: 8px;
      cursor: pointer;
      font: inherit;
      font-weight: 800;
      min-height: 48px;
      text-transform: uppercase;
      width: 100%;
    }

    .primary-action {
      background: #0f4c5c;
      border: 1px solid #0f4c5c;
      color: #fff;
      letter-spacing: 0;
      margin-top: 12px;
    }

    .primary-action:hover:not(:disabled) {
      background: #0b3f4c;
    }

    .primary-action:disabled {
      cursor: not-allowed;
      opacity: 0.55;
    }

    .secondary-action {
      background: transparent;
      border: 1px solid #0f4c5c;
      color: #0f4c5c;
    }

    .demo-panel {
      display: grid;
      gap: 12px;
      padding: 12px;
    }
  `]
})
export class LoginPage {
  private readonly session = inject(SessionService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly message = signal('');
  readonly messageColor = signal<'primary' | 'danger'>('primary');
  persona: Persona = this.session.persona();
  login = '';
  senha = '';
  totpCode = '';
  acceptedPrivacyTerms = false;

  enter(): void {
    this.message.set('');
    const consent$ = this.auth.registerConsent({
      titularId: this.login,
      tipoTitular: 'USUARIO',
      documento: this.login,
      versao: '1.0',
      canal: 'WEB'
    }).pipe(catchError(() => of(null)));

    consent$.pipe(
      switchMap(() => this.auth.login({
        login: this.login,
        senha: this.senha,
        totpCode: this.totpCode,
        acceptedPrivacyTerms: this.acceptedPrivacyTerms,
        consentVersion: '1.0'
      })),
      catchError(() => {
        this.messageColor.set('danger');
        this.message.set('Não foi possível autenticar. Verifique credenciais e API.');
        return of(null);
      })
    ).subscribe((response) => {
      if (!response) {
        return;
      }
      this.messageColor.set('primary');
      this.message.set('Autenticado com sucesso.');
      this.router.navigateByUrl(this.session.persona() === 'cliente' ? '/store' : '/');
    });
  }

  enterDemo(): void {
    this.session.setPersona(this.persona);
    this.router.navigateByUrl(this.persona === 'cliente' ? '/store' : '/');
  }
}
