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
    <main class="login-page">
      <section class="login-shell">
        <aside class="login-context">
          <div>
            <div class="brand-mark" aria-hidden="true">A</div>
            <span class="page-kicker">Apex Gestor</span>
            <h1>Operação pronta para loja, caixa e gestão.</h1>
            <p>ERP, PDV, estoque, financeiro e permissões em uma interface única para o varejo brasileiro.</p>
          </div>

          <div class="context-grid">
            <span>
              <strong>LGPD</strong>
              <small>Consentimento auditável</small>
            </span>
            <span>
              <strong>RBAC</strong>
              <small>Perfis por função</small>
            </span>
            <span>
              <strong>API</strong>
              <small>Dados sincronizados</small>
            </span>
          </div>
        </aside>

        <section class="login-panel">
          <div class="panel-title">
            <span>Acesso seguro</span>
            <h2>Entrar no Apex</h2>
          </div>

          <label class="field">
            <span>Login</span>
            <input [(ngModel)]="login" placeholder="ex.: v5_dono" autocomplete="username">
          </label>
          <label class="field">
            <span>Senha</span>
            <input type="password" [(ngModel)]="senha" [placeholder]="testPassword" autocomplete="current-password">
          </label>

          <div class="credential-help">
            <strong>Ambiente de teste</strong>
            <span>Use um login como <code>v5_dono</code> e a senha <code>{{ testPassword }}</code>. Login e senha são campos diferentes.</span>
            <div class="preset-grid">
              @for (preset of loginPresets; track preset.login) {
                <button type="button" class="soft-button" (click)="usePreset(preset.login)">
                  {{ preset.label }}
                </button>
              }
            </div>
          </div>

          @if (twoFactorEnabled) {
            <label class="field">
              <span>Código 2FA</span>
              <input [(ngModel)]="totpCode" placeholder="Código do autenticador" inputmode="numeric">
            </label>
          }
          <label class="check-row">
            <input type="checkbox" [(ngModel)]="acceptedPrivacyTerms">
            <span>Li e aceito a política de privacidade v1.0</span>
          </label>

          @if (message()) {
            <p class="auth-message" [class.danger]="messageColor() === 'danger'">{{ message() }}</p>
          }

          <button type="button" class="primary-action" (click)="enter()" [disabled]="!login || !senha || !acceptedPrivacyTerms">
            Entrar com segurança
          </button>

          <details class="demo-details">
            <summary>Modo demonstração sem backend</summary>
            <div class="demo-panel">
              <div class="segmented-control" role="group" aria-label="Perfil de demonstração">
                <button type="button" [class.selected]="persona === 'cliente'" (click)="persona = 'cliente'">Cliente</button>
                <button type="button" [class.selected]="persona === 'vendedor'" (click)="persona = 'vendedor'">Vendedor</button>
                <button type="button" [class.selected]="persona === 'gerente'" (click)="persona = 'gerente'">Gerente</button>
                <button type="button" [class.selected]="persona === 'admin'" (click)="persona = 'admin'">Admin</button>
              </div>
              <button type="button" class="secondary-action" (click)="enterDemo()">Entrar em modo demo</button>
            </div>
          </details>
        </section>
      </section>
    </main>
  `,
  styles: [`
    .login-page {
      align-items: center;
      display: flex;
      min-height: 100%;
      justify-content: center;
      padding: 24px;
    }

    .login-shell {
      align-items: stretch;
      display: grid;
      gap: 18px;
      grid-template-columns: minmax(300px, 0.86fr) minmax(360px, 500px);
      max-width: 1080px;
      width: 100%;
    }

    .login-context,
    .login-panel {
      background: #ffffff;
      border: 1px solid var(--apex-border);
      border-radius: 8px;
      box-shadow: var(--apex-shadow);
    }

    .login-context {
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      min-height: 610px;
      padding: clamp(24px, 5vw, 46px);
    }

    .login-context h1 {
      color: var(--apex-ink);
      font-size: clamp(2rem, 4vw, 3.25rem);
      font-weight: 850;
      letter-spacing: 0;
      line-height: 1;
      margin: 10px 0 14px;
      max-width: 620px;
    }

    .login-context p {
      color: var(--apex-muted);
      font-size: 1rem;
      line-height: 1.55;
      margin: 0;
      max-width: 560px;
    }

    .context-grid {
      display: grid;
      gap: 10px;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      margin-top: 34px;
    }

    .context-grid span {
      background: var(--apex-card-muted);
      border: 1px solid var(--apex-border);
      border-radius: 8px;
      padding: 12px;
    }

    .context-grid strong,
    .context-grid small {
      display: block;
    }

    .context-grid strong {
      color: var(--apex-primary-strong);
      font-size: 0.95rem;
    }

    .context-grid small {
      color: var(--apex-muted);
      font-size: 0.76rem;
      line-height: 1.3;
      margin-top: 4px;
    }

    .login-panel {
      align-self: center;
      display: grid;
      gap: 12px;
      padding: 24px;
      width: 100%;
    }

    .panel-title span {
      color: var(--apex-primary);
      display: block;
      font-size: 0.78rem;
      font-weight: 850;
      margin-bottom: 4px;
      text-transform: uppercase;
    }

    .panel-title h2 {
      color: var(--apex-ink);
      font-size: 1.38rem;
      font-weight: 850;
      margin: 0 0 4px;
    }

    .brand-mark {
      align-items: center;
      background: var(--apex-primary);
      border-radius: 8px;
      color: #fff;
      display: flex;
      font-size: 1.4rem;
      font-weight: 800;
      height: 52px;
      justify-content: center;
      margin-bottom: 22px;
      width: 52px;
    }

    .field {
      display: grid;
      gap: 6px;
    }

    .field span,
    .check-row span {
      color: var(--apex-muted);
      font-size: 0.82rem;
      font-weight: 700;
    }

    .field input {
      background: var(--apex-card-muted);
      border: 1px solid var(--apex-border);
      border-radius: 8px;
      color: var(--apex-ink);
      font: inherit;
      min-height: 46px;
      outline: none;
      padding: 10px 12px;
      width: 100%;
    }

    .field input:focus,
    .check-row:focus-within,
    .soft-button:focus-visible,
    .primary-action:focus-visible,
    .secondary-action:focus-visible,
    .segmented-control button:focus-visible {
      border-color: var(--apex-focus);
      box-shadow: 0 0 0 3px rgba(199, 116, 18, 0.16);
    }

    .check-row {
      align-items: center;
      background: var(--apex-card-muted);
      border: 1px solid var(--apex-border);
      border-radius: 8px;
      cursor: pointer;
      display: flex;
      gap: 10px;
      min-height: 44px;
      padding: 10px 12px;
    }

    .check-row input {
      accent-color: var(--apex-primary);
      height: 18px;
      width: 18px;
    }

    .auth-message {
      color: var(--apex-primary-strong);
      display: block;
      line-height: 1.45;
      margin: 2px 0;
      white-space: pre-line;
    }

    .auth-message.danger {
      color: var(--apex-danger);
    }

    .credential-help {
      background: var(--apex-primary-faint);
      border: 1px solid rgba(15, 118, 110, 0.18);
      border-radius: 8px;
      color: var(--apex-muted);
      display: grid;
      gap: 8px;
      font-size: 0.9rem;
      padding: 12px;
    }

    .credential-help strong,
    .credential-help span {
      display: block;
    }

    code {
      background: #e3eee9;
      border-radius: 6px;
      color: var(--apex-ink);
      font-weight: 700;
      padding: 2px 6px;
    }

    .preset-grid {
      display: grid;
      gap: 8px;
      grid-template-columns: repeat(auto-fit, minmax(132px, 1fr));
    }

    .soft-button,
    .primary-action,
    .secondary-action,
    .segmented-control button {
      align-items: center;
      border-radius: 8px;
      cursor: pointer;
      display: inline-flex;
      font: inherit;
      font-weight: 800;
      justify-content: center;
      min-height: 40px;
      text-align: center;
    }

    .soft-button,
    .secondary-action {
      background: #ffffff;
      border: 1px solid rgba(15, 118, 110, 0.28);
      color: var(--apex-primary-strong);
      padding: 8px 10px;
    }

    .primary-action {
      background: var(--apex-primary);
      border: 1px solid var(--apex-primary);
      color: #ffffff;
      padding: 12px 14px;
      width: 100%;
    }

    .primary-action:disabled {
      cursor: not-allowed;
      opacity: 0.48;
    }

    .demo-details {
      border-top: 1px solid var(--apex-border);
      padding-top: 10px;
    }

    .demo-details summary {
      color: var(--apex-ink);
      cursor: pointer;
      font-weight: 800;
    }

    .demo-panel {
      display: grid;
      gap: 12px;
      padding: 12px 0 0;
    }

    .segmented-control {
      background: var(--apex-card-muted);
      border: 1px solid var(--apex-border);
      border-radius: 8px;
      display: grid;
      gap: 4px;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      padding: 4px;
    }

    .segmented-control button {
      background: transparent;
      border: 1px solid transparent;
      color: var(--apex-muted);
      min-height: 36px;
      padding: 6px 8px;
    }

    .segmented-control button.selected {
      background: #ffffff;
      border-color: var(--apex-border);
      box-shadow: var(--apex-shadow-soft);
      color: var(--apex-primary-strong);
    }

    @media (max-width: 900px) {
      .login-shell {
        grid-template-columns: 1fr;
      }

      .login-context {
        min-height: auto;
      }
    }

    @media (max-width: 560px) {
      .login-page {
        padding: 12px;
      }

      .context-grid {
        grid-template-columns: 1fr;
      }
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
    { label: 'Gerente', login: 'v5_gerente' },
    { label: 'Caixa', login: 'v5_caixa' },
    { label: 'Vendedor', login: 'v5_vendedor' },
    { label: 'Cliente', login: 'v5_cliente' }
  ];
  readonly testPassword = 'Apex@2026';
  readonly twoFactorEnabled = this.isTwoFactorEnabled();

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
      this.message.set('Você colocou a senha no campo Login.\nUse Login: v5_dono\nUse Senha: Apex@2026');
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
        totpCode: this.twoFactorEnabled ? this.totpCode : undefined,
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
    this.session.setRoles(this.rolesForPersona(this.persona));
    this.router.navigateByUrl(this.redirectUrl());
  }

  usePreset(login: string): void {
    this.login = login;
    this.senha = 'Apex@2026';
    this.acceptedPrivacyTerms = true;
    this.messageColor.set('primary');
    this.message.set(`Credenciais de teste preenchidas para ${login}. Clique em Entrar com segurança.`);
  }

  private redirectUrl(): string {
    return this.route.snapshot.queryParamMap.get('redirect') ?? (this.session.persona() === 'cliente' ? '/store' : '/');
  }

  private rolesForPersona(persona: Persona): string[] {
    const roles: Record<Persona, string[]> = {
      cliente: ['ROLE_CLIENTE_B2C'],
      vendedor: ['ROLE_VENDEDOR'],
      gerente: ['ROLE_GERENTE'],
      admin: ['ROLE_ADMIN']
    };
    return roles[persona];
  }

  private friendlyAuthError(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const backendMessage = this.backendMessage(error);
      if (error.status === 0) {
        return 'Não consegui conectar na API.\nConfira se o backend está rodando em http://localhost:8080/actuator/health e tente novamente.';
      }
      if (error.status === 401) {
        return 'Login ou senha inválidos.\nPara testar, use Login: v5_dono e Senha: Apex@2026. Para o app do cliente, use Login: v5_cliente.';
      }
      if (error.status === 402) {
        return 'Licença não validada para este app.\nAbra Configurações, valide uma chave de teste como APEX-DEMO-ALL e tente entrar novamente.';
      }
      if (error.status === 403) {
        return 'Acesso negado para este perfil.\nEntre com uma role autorizada para este módulo ou solicite permissão ao administrador.';
      }
      if (error.status >= 500) {
        return `A API encontrou um erro interno.\nVerifique os logs em run-logs/backend-api.out.log. ${backendMessage}`;
      }
      return `Não foi possível autenticar. HTTP ${error.status}.\n${backendMessage || 'Revise os dados e tente novamente.'}`;
    }

    if (error instanceof Error && error.message === 'API_UNAVAILABLE') {
      return 'Não consegui conectar na API dentro do tempo esperado.\nConfira se o backend está rodando em http://localhost:8080/actuator/health. Depois tente novamente.';
    }

    return 'Não foi possível autenticar.\nVerifique sua conexão com a API, login, senha e permissão do usuário.';
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

  private isTwoFactorEnabled(): boolean {
    try {
      return globalThis.localStorage?.getItem('apex.feature.2fa') === 'enabled';
    } catch {
      return false;
    }
  }
}
