import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, of, switchMap } from 'rxjs';

import { AuthService } from '../core/auth.service';
import { DEMO_MODE, DEMO_PASSWORD } from '../core/app-variant';
import { LicenseService } from '../core/license.service';
import { SessionService } from '../core/session.service';

interface LoginPreset {
  label: string;
  login: string;
}

type DemoPersona = 'caixa' | 'vendedor' | 'gerente' | 'admin';

const CLIENT_ROLES = new Set(['ROLE_CLIENTE_B2C', 'ROLE_CLIENTE']);

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.page.html',
  styleUrl: './login.page.css'
})
export class LoginPage {
  private readonly session = inject(SessionService);
  private readonly auth = inject(AuthService);
  private readonly licenseService = inject(LicenseService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly message = signal('');
  readonly messageColor = signal<'primary' | 'danger'>('primary');
  readonly demoMode = DEMO_MODE;
  readonly loginPresets: LoginPreset[] = [
    { label: 'Dono/Gerente', login: 'v5_dono' },
    { label: 'Sysadmin', login: 'v5_sysadmin' },
    { label: 'Gerente', login: 'v5_gerente' },
    { label: 'Caixa', login: 'v5_caixa' },
    { label: 'Vendedor', login: 'v5_vendedor' }
  ];
  readonly testPassword = DEMO_PASSWORD;
  readonly twoFactorEnabled = this.isTwoFactorEnabled();

  demoPersona: DemoPersona = this.initialDemoPersona();
  login = '';
  senha = '';
  totpCode = '';
  acceptedPrivacyTerms = false;

  constructor() {
    this.applyRouteMessage();
  }

  ionViewWillEnter(): void {
    this.applyRouteMessage();
  }

  private applyRouteMessage(): void {
    const reason = this.route.snapshot.queryParamMap.get('reason');
    if (reason === 'inactivity') {
      this.messageColor.set('danger');
      this.message.set('Sessão encerrada por inatividade. Faça login novamente.');
    } else if (reason === 'session-expired') {
      this.messageColor.set('danger');
      this.message.set('Sessão expirada. Faça login novamente.');
    } else if (reason === 'forbidden') {
      this.messageColor.set('danger');
      this.message.set('Acesso negado para este perfil.');
    }
  }

  enter(): void {
    this.message.set('');
    this.login = this.login.trim();

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
      if (!this.hasStaffRole(response.roles)) {
        this.auth.logout('forbidden');
        this.messageColor.set('danger');
        this.message.set('Acesso de login para perfil cliente foi desativado.');
        return;
      }
      this.licenseService.loadEntitlement().pipe(catchError(() => of(null))).subscribe(() => {
        this.messageColor.set('primary');
        this.message.set('Autenticado com sucesso.');
        void this.router.navigateByUrl(this.redirectUrl());
      });
    });
  }

  enterDemo(): void {
    if (!this.demoMode) {
      return;
    }
    if (this.auth.enterDemoSession(this.rolesForDemoPersona(this.demoPersona))) {
      void this.router.navigateByUrl(this.redirectUrl());
    }
  }

  usePreset(login: string): void {
    if (!this.demoMode) {
      return;
    }
    this.login = login;
    this.senha = this.testPassword;
    this.acceptedPrivacyTerms = true;
    this.messageColor.set('primary');
    this.message.set(`Credenciais de teste preenchidas para ${login}. Clique em Entrar com segurança.`);
  }

  private initialDemoPersona(): DemoPersona {
    const current: string = this.session.persona();
    if (current === 'caixa' || current === 'vendedor' || current === 'gerente' || current === 'admin') {
      return current;
    }
    return 'gerente';
  }

  private redirectUrl(): string {
    return this.route.snapshot.queryParamMap.get('redirect') ?? '/';
  }

  private rolesForDemoPersona(persona: DemoPersona): string[] {
    const roles: Record<DemoPersona, string[]> = {
      caixa: ['ROLE_CAIXA'],
      vendedor: ['ROLE_VENDEDOR'],
      gerente: ['ROLE_GERENTE'],
      admin: ['ROLE_ADMIN']
    };
    return roles[persona];
  }

  private hasStaffRole(roles: readonly string[] | undefined): boolean {
    if (!roles || roles.length === 0) {
      return false;
    }
    return roles.some((rawRole) => {
      const upper = rawRole.trim().toUpperCase();
      const normalized = upper.startsWith('ROLE_') ? upper : `ROLE_${upper}`;
      return normalized.length > 5 && !CLIENT_ROLES.has(normalized);
    });
  }

  private friendlyAuthError(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const backendMessage = this.backendMessage(error);
      if (error.status === 0) {
        return 'Não consegui conectar na API.\nConfira se o backend está rodando em http://localhost:8080/actuator/health e tente novamente.';
      }
      if (error.status === 401) {
        return this.demoMode
          ? 'Login ou senha inválidos. Confira o usuário do ambiente local e tente novamente.'
          : 'Login ou senha inválidos. Confira suas credenciais e tente novamente.';
      }
      if (error.status === 402) {
        return 'Licença não validada para este app.\nAbra Configurações, valide uma chave de teste como APEX-DEMO-2026 e tente entrar novamente.';
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
