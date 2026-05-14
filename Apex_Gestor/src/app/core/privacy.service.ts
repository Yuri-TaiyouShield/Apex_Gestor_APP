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
      switchMap(() => this.http.post(this.apiConfig.apiUrl('/api/privacy/requests'), payload))
    );
  }

  exportCliente(id: number) {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.get(this.apiConfig.apiUrl(`/api/privacy/export/clientes/${id}`)))
    );
  }

  anonymizeCliente(id: number) {
    return this.readiness.waitForApi().pipe(
      switchMap(() => this.http.delete(this.apiConfig.apiUrl(`/api/privacy/clientes/${id}`)))
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
