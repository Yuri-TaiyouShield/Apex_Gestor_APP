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
            <p>Base de API, perfil de acesso e preferências cross-platform.</p>
          </div>
        </header>

        <section class="grid-two">
          <ion-card class="data-card">
            <ion-card-header>
              <ion-card-title>Integração com API</ion-card-title>
              <ion-card-subtitle>Web usa proxy /api; Electron e Capacitor usam URL absoluta.</ion-card-subtitle>
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
              <ion-card-title>Perfil ativo</ion-card-title>
              <ion-card-subtitle>Simula RBAC até o login real ser conectado.</ion-card-subtitle>
            </ion-card-header>
            <ion-card-content class="stack">
              <ion-segment [(ngModel)]="persona">
                <ion-segment-button value="cliente">Cliente</ion-segment-button>
                <ion-segment-button value="vendedor">Vendedor</ion-segment-button>
                <ion-segment-button value="gerente">Gerente</ion-segment-button>
                <ion-segment-button value="admin">Admin</ion-segment-button>
              </ion-segment>
              <ion-button expand="block" (click)="savePersona()">Aplicar perfil</ion-button>
            </ion-card-content>
          </ion-card>

          <ion-card class="data-card">
            <ion-card-header>
              <ion-card-title>Licenciamento</ion-card-title>
              <ion-card-subtitle>Valida a chave no servidor e vincula a ativação ao dispositivo.</ion-card-subtitle>
            </ion-card-header>
            <ion-card-content class="stack">
              <ion-item>
                <ion-input label="Chave da licença" label-placement="stacked" [(ngModel)]="licenseKey"></ion-input>
              </ion-item>
              <div class="license-device">
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

  constructor() {
    void this.license.readLicenseKey().then((key) => {
      this.licenseKey = key;
    });
    void this.license.deviceInfo().then((device) => {
      this.deviceFingerprintShort.set(`${device.deviceLabel} - ${device.deviceFingerprint.slice(0, 12)}`);
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
