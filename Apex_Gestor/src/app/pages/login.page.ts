import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { catchError, of, switchMap } from 'rxjs';

import { AuthService } from '../core/auth.service';
import { Persona } from '../core/models';
import { SessionService } from '../core/session.service';

interface LoginPreset {
  label: string;
  login: string;
}

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule],
  template: `
    <ion-content>
      <main class="login-page">
        <section class="login-panel">
          <div class="brand-mark">A</div>
          <h1>Acesso seguro Apex</h1>
          <p>JWT com expiracao curta, refresh token rotativo e aceite LGPD auditavel.</p>

          <ion-item>
            <ion-input label="Login" label-placement="stacked" [(ngModel)]="login" placeholder="ex.: v5_dono"></ion-input>
          </ion-item>
          <ion-item>
            <ion-input type="password" label="Senha" label-placement="stacked" [(ngModel)]="senha" [placeholder]="testPassword"></ion-input>
          </ion-item>

          <div class="credential-help">
            <strong>Ambiente de teste</strong>
            <span>Use um login como <code>v5_dono</code> e a senha <code>{{ testPassword }}</code>. Login e senha sao campos diferentes.</span>
            <div class="preset-grid">
              @for (preset of loginPresets; track preset.login) {
                <ion-button size="small" fill="outline" (click)="usePreset(preset.login)">
                  {{ preset.label }}
                </ion-button>
              }
            </div>
          </div>

          <ion-item>
            <ion-input label="Codigo 2FA" label-placement="stacked" [(ngModel)]="totpCode" placeholder="Opcional nesta fase"></ion-input>
          </ion-item>
          <ion-item>
            <ion-checkbox [(ngModel)]="acceptedPrivacyTerms">
              Li e aceito a politica de privacidade v1.0
            </ion-checkbox>
          </ion-item>

          @if (message()) {
            <ion-note [color]="messageColor()">{{ message() }}</ion-note>
          }

          <ion-button expand="block" size="large" (click)="enter()" [disabled]="!login || !senha || !acceptedPrivacyTerms">
            Entrar com seguranca
          </ion-button>

          <ion-accordion-group>
            <ion-accordion value="demo">
              <ion-item slot="header">
                <ion-label>Modo demonstracao sem backend</ion-label>
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
      max-width: 500px;
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
      line-height: 1.45;
      margin: 12px 0;
      white-space: pre-line;
    }

    .credential-help {
      background: #f8fafc;
      border: 1px solid var(--apex-border);
      border-radius: 8px;
      color: #475569;
      display: grid;
      gap: 8px;
      font-size: 0.9rem;
      margin: 12px 0;
      padding: 12px;
    }

    .credential-help strong,
    .credential-help span {
      display: block;
    }

    code {
      background: #e2e8f0;
      border-radius: 6px;
      color: #0f172a;
      font-weight: 700;
      padding: 2px 6px;
    }

    .preset-grid {
      display: grid;
      gap: 8px;
      grid-template-columns: repeat(auto-fit, minmax(132px, 1fr));
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
  private readonly route = inject(ActivatedRoute);

  readonly message = signal('');
  readonly messageColor = signal<'primary' | 'danger'>('primary');
  readonly loginPresets: LoginPreset[] = [
    { label: 'Dono/Gerente', login: 'v5_dono' },
    { label: 'Sysadmin', login: 'v5_sysadmin' },
    { label: 'Caixa', login: 'v5_caixa' },
    { label: 'Cliente', login: 'v5_cliente' }
  ];
  readonly testPassword = 'Apex@2026';

  persona: Persona = this.session.persona();
  login = '';
  senha = '';
  totpCode = '';
  acceptedPrivacyTerms = false;

  enter(): void {
    this.message.set('');
    this.login = this.login.trim();

    if (this.login === 'Apex@2026') {
      this.messageColor.set('danger');
      this.message.set('Voce colocou a senha no campo Login.\nUse Login: v5_dono\nUse Senha: Apex@2026');
      return;
    }

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
      catchError((error: unknown) => {
        this.messageColor.set('danger');
        this.message.set(this.friendlyAuthError(error));
        return of(null);
      })
    ).subscribe((response) => {
      if (!response) {
        return;
      }
      this.messageColor.set('primary');
      this.message.set('Autenticado com sucesso.');
      this.router.navigateByUrl(this.redirectUrl());
    });
  }

  enterDemo(): void {
    this.session.setPersona(this.persona);
    this.router.navigateByUrl(this.redirectUrl());
  }

  usePreset(login: string): void {
    this.login = login;
    this.senha = 'Apex@2026';
    this.acceptedPrivacyTerms = true;
    this.messageColor.set('primary');
    this.message.set(`Credenciais de teste preenchidas para ${login}. Clique em Entrar com seguranca.`);
  }

  private redirectUrl(): string {
    return this.route.snapshot.queryParamMap.get('redirect') ?? (this.session.persona() === 'cliente' ? '/store' : '/');
  }

  private friendlyAuthError(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const backendMessage = this.backendMessage(error);
      if (error.status === 0) {
        return 'Nao consegui conectar na API.\nConfira se o backend esta rodando em http://localhost:8080/actuator/health e tente novamente.';
      }
      if (error.status === 401) {
        return 'Login ou senha invalidos.\nPara testar, use Login: v5_dono e Senha: Apex@2026. Para o app do cliente, use Login: v5_cliente.';
      }
      if (error.status === 402) {
        return 'Licenca nao validada para este app.\nAbra Configuracoes, valide uma chave de teste como APEX-DEMO-ALL e tente entrar novamente.';
      }
      if (error.status === 403) {
        return 'Acesso negado para este perfil.\nEntre com uma role autorizada para este modulo ou solicite permissao ao administrador.';
      }
      if (error.status >= 500) {
        return `A API encontrou um erro interno.\nVerifique os logs em run-logs/backend-api.out.log. ${backendMessage}`;
      }
      return `Nao foi possivel autenticar. HTTP ${error.status}.\n${backendMessage || 'Revise os dados e tente novamente.'}`;
    }

    if (error instanceof Error && error.message === 'API_UNAVAILABLE') {
      return 'Nao consegui conectar na API dentro do tempo esperado.\nConfira se o backend esta rodando em http://localhost:8080/actuator/health. Depois tente entrar novamente.';
    }

    return 'Nao foi possivel autenticar.\nVerifique sua conexao com a API, login, senha e permissao do usuario.';
  }

  private backendMessage(error: HttpErrorResponse): string {
    const payload = error.error as { message?: unknown; error?: unknown } | string | null;
    if (typeof payload === 'string') {
      return payload;
    }
    if (payload && typeof payload.message === 'string') {
      return payload.message;
    }
    if (payload && typeof payload.error === 'string') {
      return payload.error;
    }
    return error.message || '';
  }
}
