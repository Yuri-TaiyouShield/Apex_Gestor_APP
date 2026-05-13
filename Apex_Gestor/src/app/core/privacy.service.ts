import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { ApiConfigService } from './api-config.service';
import { PrivacyRequestPayload } from './models';

@Injectable({ providedIn: 'root' })
export class PrivacyService {
  private readonly http = inject(HttpClient);
  private readonly apiConfig = inject(ApiConfigService);

  request(payload: PrivacyRequestPayload) {
    return this.http.post(this.apiConfig.apiUrl('/api/privacy/requests'), payload);
  }

  exportCliente(id: number) {
    return this.http.get(this.apiConfig.apiUrl(`/api/privacy/export/clientes/${id}`));
  }

  anonymizeCliente(id: number) {
    return this.http.delete(this.apiConfig.apiUrl(`/api/privacy/clientes/${id}`));
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
