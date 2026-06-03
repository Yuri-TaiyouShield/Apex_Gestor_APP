import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { catchError, of } from 'rxjs';

import { ApiConfigService } from '../core/api-config.service';
import { LicenseService } from '../core/license.service';
import { LicenseValidationResponse, Persona } from '../core/models';
import { SessionService } from '../core/session.service';

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule],
  template: `
    <ion-content>
      <main class="page">
        <header class="page-header">
          <div>
            <h1>Configurações</h1>
            <p>Base de API, perfil de acesso, licença e preferências cross-platform.</p>
          </div>
        </header>

        <section class="grid-two">
          <ion-card class="data-card">
            <ion-card-header>
              <ion-card-title>Integração com API</ion-card-title>
              <ion-card-subtitle>Web usa proxy /api; Electron e Capacitor usam URL absoluta.</ion-card-subtitle>
            </ion-card-header>
            <ion-card-content class="stack">
              <label class="field">
                <span>URL base da API</span>
                <input type="url" [(ngModel)]="apiBaseUrl">
              </label>
              <button class="primary-action" type="button" (click)="saveApiUrl()">Salvar URL</button>
              <button class="secondary-action" type="button" (click)="resetApiUrl()">Restaurar padrão</button>
            </ion-card-content>
          </ion-card>

          <ion-card class="data-card">
            <ion-card-header>
              <ion-card-title>Perfil ativo</ion-card-title>
              <ion-card-subtitle>Simula RBAC até o login real ser conectado.</ion-card-subtitle>
            </ion-card-header>
            <ion-card-content class="stack">
              <label class="field">
                <span>Perfil</span>
                <select [(ngModel)]="persona">
                  <option value="cliente">Cliente</option>
                  <option value="vendedor">Vendedor</option>
                  <option value="gerente">Gerente</option>
                  <option value="admin">Admin</option>
                </select>
              </label>
              <button class="primary-action" type="button" (click)="savePersona()">Aplicar perfil</button>
            </ion-card-content>
          </ion-card>

          <ion-card class="data-card">
            <ion-card-header>
              <ion-card-title>Licenciamento</ion-card-title>
              <ion-card-subtitle>Valida a chave no servidor e vincula a ativação ao app e dispositivo.</ion-card-subtitle>
            </ion-card-header>
            <ion-card-content class="stack">
              <label class="field">
                <span>Chave da licença</span>
                <input type="text" autocomplete="off" [(ngModel)]="licenseKey">
              </label>
              <div class="license-device">
                <span>App licenciado</span>
                <strong>{{ appLabel() }}</strong>
                <span>Dispositivo</span>
                <strong>{{ deviceFingerprintShort() }}</strong>
              </div>
              <button class="primary-action" type="button" (click)="validateLicense()">Validar licença</button>
              @if (licenseStatus()) {
                <ion-note [color]="licenseStatus()?.valid ? 'success' : 'danger'">
                  {{ licenseStatus()?.message }}
                </ion-note>
                @if (licenseStatus()?.allowedApps?.length) {
                  <ion-note color="medium">
                    Plano: {{ licenseStatus()?.licensePlan }} | Apps liberados: {{ licenseStatus()?.allowedApps?.join(', ') }}
                  </ion-note>
                }
              }
            </ion-card-content>
          </ion-card>
        </section>
      </main>
    </ion-content>
  `,
  styles: [`
    .license-device {
      background: #f8fafc;
      border: 1px solid var(--apex-border);
      border-radius: 8px;
      display: grid;
      gap: 4px;
      padding: 12px;
    }

    .license-device span {
      color: #64748b;
      font-size: 0.82rem;
    }

    .field {
      display: grid;
      gap: 6px;
    }

    .field span {
      color: #334155;
      font-size: 0.9rem;
      font-weight: 700;
    }

    .field input,
    .field select {
      background: #f8fafc;
      border: 1px solid var(--apex-border);
      border-radius: 8px;
      color: #0f172a;
      font: inherit;
      min-height: 44px;
      padding: 9px 12px;
      width: 100%;
    }

    .field input:focus,
    .field select:focus {
      border-color: #0f766e;
      box-shadow: 0 0 0 3px rgba(15, 118, 110, 0.16);
      outline: none;
    }

    .primary-action,
    .secondary-action {
      border-radius: 8px;
      cursor: pointer;
      font: inherit;
      font-weight: 800;
      min-height: 44px;
      width: 100%;
    }

    .primary-action {
      background: #0f4c5c;
      border: 1px solid #0f4c5c;
      color: #fff;
    }

    .primary-action:hover {
      background: #0b3f4c;
    }

    .secondary-action {
      background: transparent;
      border: 1px solid #0f4c5c;
      color: #0f4c5c;
    }
  `]
})
export class SettingsPage {
  readonly apiConfig = inject(ApiConfigService);
  readonly license = inject(LicenseService);
  readonly session = inject(SessionService);
  apiBaseUrl = this.apiConfig.baseUrl();
  persona: Persona = this.session.persona();
  licenseKey = '';
  readonly licenseStatus = signal<LicenseValidationResponse | null>(this.license.readCachedStatus());
  readonly deviceFingerprintShort = signal('Carregando...');
  readonly appLabel = signal('Detectando app...');

  constructor() {
    void this.license.readLicenseKey().then((key) => {
      this.licenseKey = key;
    });
    void this.license.deviceInfo().then((device) => {
      this.deviceFingerprintShort.set(`${device.deviceLabel} - ${device.deviceFingerprint.slice(0, 12)}`);
      this.appLabel.set(device.appId);
    });
  }

  saveApiUrl(): void {
    this.apiConfig.setBaseUrl(this.apiBaseUrl);
  }

  resetApiUrl(): void {
    this.apiConfig.reset();
    this.apiBaseUrl = this.apiConfig.baseUrl();
  }

  savePersona(): void {
    this.session.setPersona(this.persona);
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
}
