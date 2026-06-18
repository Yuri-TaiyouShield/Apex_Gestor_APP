import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { switchMap } from 'rxjs';

import { ApiConfigService } from './api-config.service';
import { ApiReadinessService } from './api-readiness.service';
import { PrivacyRequestPayload } from './models';

@Injectable({ providedIn: 'root' })
export class PrivacyService {
  private readonly http = inject(HttpClient);
  private readonly apiConfig = inject(ApiConfigService);
  private readonly readiness = inject(ApiReadinessService);

  request(payload: PrivacyRequestPayload) {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post(this.apiConfig.apiUrl('/api/privacy/admin/requests'), payload))
    );
  }

  exportCliente(id: number) {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.get(this.apiConfig.apiUrl(`/api/privacy/admin/clientes/${id}/export`)))
    );
  }

  anonymizeCliente(id: number) {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.post(this.apiConfig.apiUrl(`/api/privacy/admin/clientes/${id}/anonymize`), {
        titularId: String(id),
        tipoTitular: 'CLIENTE',
        tipo: 'ANONIMIZACAO',
        justificativa: 'Anonimizacao administrativa solicitada no contexto do cadastro.'
      }))
    );
  }

  maskDocument(value: string): string {
    const digits = value.replace(/\D/g, '');
    return digits.length > 4 ? `***${digits.slice(-4)}` : '***';
  }

  maskEmail(value: string): string {
    const [user, domain] = value.split('@');
    return domain ? `${user.slice(0, 1)}***@${domain}` : '***';
  }
}
