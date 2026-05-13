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

          <ion-item>
            <ion-input label="Login" label-placement="stacked" [(ngModel)]="login"></ion-input>
          </ion-item>
          <ion-item>
            <ion-input type="password" label="Senha" label-placement="stacked" [(ngModel)]="senha"></ion-input>
          </ion-item>
          <ion-item>
            <ion-input label="Código 2FA" label-placement="stacked" [(ngModel)]="totpCode" placeholder="Opcional nesta fase"></ion-input>
          </ion-item>
          <ion-item>
            <ion-checkbox [(ngModel)]="acceptedPrivacyTerms">
              Li e aceito a política de privacidade v1.0
            </ion-checkbox>
          </ion-item>

          @if (message()) {
            <ion-note [color]="messageColor()">{{ message() }}</ion-note>
          }

          <ion-button expand="block" size="large" (click)="enter()" [disabled]="!login || !senha || !acceptedPrivacyTerms">
            Entrar com segurança
          </ion-button>

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
                <ion-button expand="block" fill="outline" (click)="enterDemo()">Entrar em modo demo</ion-button>
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
