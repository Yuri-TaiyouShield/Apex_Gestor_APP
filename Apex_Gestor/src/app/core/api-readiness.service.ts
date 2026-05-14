import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, filter, map, of, switchMap, take, timer } from 'rxjs';

import { ApiConfigService } from './api-config.service';

interface HealthResponse {
  status?: string;
}

@Injectable({ providedIn: 'root' })
export class ApiReadinessService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(ApiConfigService);

  waitForApi(): Observable<void> {
    return timer(0, 1500).pipe(
      switchMap(() => this.http.get<HealthResponse>(this.config.healthUrl()).pipe(
        map((response) => response.status === 'UP'),
        catchError(() => of(false))
      )),
      filter(Boolean),
      take(1),
      map(() => undefined)
    );
  }
}
