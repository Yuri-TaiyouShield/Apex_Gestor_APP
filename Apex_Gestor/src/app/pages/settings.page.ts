import { CommonModule } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { catchError, of } from 'rxjs';

import { ApiConfigService } from '../core/api-config.service';
import { AuthService } from '../core/auth.service';
import { LicenseService } from '../core/license.service';
import { LicenseValidationResponse } from '../core/models';
import { SessionService } from '../core/session.service';

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule, RouterLink],
  template: `
    <ion-content>
      <main class="page settings-page">
        <header class="page-header">
          <div>
            <span class="page-kicker">{{ adminMode() ? 'Administração' : 'Preferências' }}</span>
            <h1>{{ adminMode() ? 'Configurações administrativas' : 'Configurações pessoais' }}</h1>
            <p>{{ adminMode() ? 'Empresas, usuários, permissões, integrações, segurança e parâmetros globais.' : 'Conta, tema, sessão e preferências de interface do usuário atual.' }}</p>
          </div>
        </header>

        <section class="grid-two">
          <ion-card class="data-card">
            <ion-card-header>
              <ion-card-title>Conta e sessão</ion-card-title>
              <ion-card-subtitle>{{ auth.user()?.nome || 'Usuário autenticado' }} | {{ session.roleLabel() }}</ion-card-subtitle>
            </ion-card-header>
            <ion-card-content class="stack">
              <ion-item>
                <ion-input label="Login" label-placement="stacked" [value]="auth.user()?.login || ''" readonly></ion-input>
              </ion-item>
              <ion-item>
                <ion-input type="password" label="Nova senha" label-placement="stacked" [(ngModel)]="newPassword" placeholder="Mínimo 8 caracteres"></ion-input>
              </ion-item>
              <ion-note color="medium">Troca de senha fica preparada para endpoint dedicado; senhas nunca são exibidas em resposta.</ion-note>
              <ion-button expand="block" fill="outline" disabled>Trocar senha</ion-button>
              <ion-button expand="block" color="danger" (click)="logout()">
                <ion-icon slot="start" name="log-out-outline"></ion-icon>
                Encerrar sessão
              </ion-button>
            </ion-card-content>
          </ion-card>

          <ion-card class="data-card">
            <ion-card-header>
              <ion-card-title>Interface</ion-card-title>
              <ion-card-subtitle>Preferências locais deste navegador.</ion-card-subtitle>
            </ion-card-header>
            <ion-card-content class="stack">
              <ion-item>
                <ion-select label="Tema" label-placement="stacked" [(ngModel)]="theme">
                  <ion-select-option value="system">Sistema</ion-select-option>
                  <ion-select-option value="light">Claro</ion-select-option>
                  <ion-select-option value="dense">Operacional denso</ion-select-option>
                </ion-select>
              </ion-item>
              <ion-item>
                <ion-select label="Densidade" label-placement="stacked" [(ngModel)]="density">
                  <ion-select-option value="compact">Compacta</ion-select-option>
                  <ion-select-option value="comfortable">Confortável</ion-select-option>
                </ion-select>
              </ion-item>
              <ion-button expand="block" (click)="savePreferences()">Salvar preferências</ion-button>
              @if (preferenceMessage()) {
                <ion-note color="success">{{ preferenceMessage() }}</ion-note>
              }
            </ion-card-content>
          </ion-card>
        </section>

        @if (adminMode()) {
          <section class="grid-two admin-grid">
            <ion-card class="data-card">
              <ion-card-header>
                <ion-card-title>Integração com API</ion-card-title>
                <ion-card-subtitle>Parâmetro global, visível apenas para administração.</ion-card-subtitle>
              </ion-card-header>
              <ion-card-content class="stack">
                <ion-item>
                  <ion-input label="URL base da API" label-placement="stacked" [(ngModel)]="apiBaseUrl"></ion-input>
                </ion-item>
                <ion-button expand="block" (click)="saveApiUrl()">Salvar URL</ion-button>
                <ion-button expand="block" fill="outline" (click)="resetApiUrl()">Restaurar padrão</ion-button>
              </ion-card-content>
            </ion-card>

            <ion-card class="data-card">
              <ion-card-header>
                <ion-card-title>Licenciamento</ion-card-title>
                <ion-card-subtitle>Valida chave e dispositivo no servidor.</ion-card-subtitle>
              </ion-card-header>
              <ion-card-content class="stack">
                <ion-item>
                  <ion-input label="Chave da licença" label-placement="stacked" [(ngModel)]="licenseKey"></ion-input>
                </ion-item>
                <div class="license-device">
                  <span>App licenciado</span>
                  <strong>{{ appLabel() }}</strong>
                  <span>Dispositivo</span>
                  <strong>{{ deviceFingerprintShort() }}</strong>
                </div>
                <ion-button expand="block" (click)="validateLicense()">Validar licença</ion-button>
                @if (licenseStatus()) {
                  <ion-note [color]="licenseStatus()?.valid ? 'success' : 'danger'">
                    {{ licenseStatus()?.message }}
                  </ion-note>
                }
              </ion-card-content>
            </ion-card>

            <ion-card class="data-card admin-links">
              <ion-card-header>
                <ion-card-title>Cadastros administrativos</ion-card-title>
                <ion-card-subtitle>Rotinas globais fora do fluxo operacional diário.</ion-card-subtitle>
              </ion-card-header>
              <ion-card-content class="stack">
                <ion-button expand="block" fill="outline" routerLink="/users">Usuários e funcionários</ion-button>
                <ion-button expand="block" fill="outline" routerLink="/profiles">Perfis e permissões</ion-button>
                <ion-button expand="block" fill="outline" routerLink="/menus">Menus</ion-button>
                <ion-button expand="block" fill="outline" routerLink="/payment-methods">Formas de pagamento</ion-button>
                <ion-button expand="block" fill="outline" routerLink="/categories">Categorias de produtos</ion-button>
                <ion-button expand="block" fill="outline" routerLink="/product-groups">Grupos de produtos</ion-button>
                <ion-button expand="block" fill="outline" routerLink="/expense-types">Parâmetros financeiros</ion-button>
                <ion-note color="medium">LGPD permanece transversal; não há tela operacional separada de privacidade.</ion-note>
              </ion-card-content>
            </ion-card>
          </section>
        }
      </main>
    </ion-content>
  `,
  styles: [`
    .settings-page {
      max-width: 1320px;
    }

    .admin-grid {
      margin-top: 14px;
    }

    .license-device {
      background: var(--apex-card-muted);
      border: 1px solid var(--apex-border);
      border-radius: 8px;
      display: grid;
      gap: 4px;
      padding: 12px;
    }

    .license-device span {
      color: var(--apex-muted);
      font-size: 0.82rem;
    }

    .admin-links {
      grid-column: 1 / -1;
    }
  `]
})
export class SettingsPage {
  readonly apiConfig = inject(ApiConfigService);
  readonly license = inject(LicenseService);
  readonly session = inject(SessionService);
  readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly mode = signal<'personal' | 'admin'>('personal');
  readonly adminMode = computed(() => this.mode() === 'admin' && this.session.hasAnyRole(['ROLE_SYSADMIN', 'ROLE_DONO_GERENTE', 'ROLE_ADMIN', 'ROLE_GERENTE']));
  readonly preferenceMessage = signal('');
  readonly licenseStatus = signal<LicenseValidationResponse | null>(this.license.readCachedStatus());
  readonly deviceFingerprintShort = signal('Carregando...');
  readonly appLabel = signal('Detectando app...');

  apiBaseUrl = this.apiConfig.baseUrl();
  licenseKey = '';
  theme = localStorage.getItem('apex.pref.theme') || 'system';
  density = localStorage.getItem('apex.pref.density') || 'compact';
  newPassword = '';

  constructor() {
    this.route.data.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((data) => {
      this.mode.set(data['mode'] === 'admin' ? 'admin' : 'personal');
      this.preferenceMessage.set('');
    });
    void this.license.readLicenseKey().then((key) => {
      this.licenseKey = key;
    });
    void this.license.deviceInfo().then((device) => {
      this.deviceFingerprintShort.set(`${device.deviceLabel} - ${device.deviceFingerprint.slice(0, 12)}`);
      this.appLabel.set(device.appId);
    });
  }

  savePreferences(): void {
    localStorage.setItem('apex.pref.theme', this.theme);
    localStorage.setItem('apex.pref.density', this.density);
    this.preferenceMessage.set('Preferências salvas neste dispositivo.');
  }

  saveApiUrl(): void {
    this.apiConfig.setBaseUrl(this.apiBaseUrl);
  }

  resetApiUrl(): void {
    this.apiConfig.reset();
    this.apiBaseUrl = this.apiConfig.baseUrl();
  }

  validateLicense(): void {
    this.license.validate(this.licenseKey)
      .pipe(catchError(() => of({
        valid: false,
        status: 'OFFLINE',
        message: 'Não foi possível validar a licença com a API.'
      })))
      .subscribe((response) => this.licenseStatus.set(response));
  }

  logout(): void {
    this.auth.logout('manual');
    void this.router.navigate(['/login'], { queryParams: { reason: 'logout' } });
  }
}
