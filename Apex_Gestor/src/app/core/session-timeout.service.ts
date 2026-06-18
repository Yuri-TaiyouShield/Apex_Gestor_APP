import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from './auth.service';

const TIMEOUT_KEY = 'apex.session.timeoutMinutes';
const DEFAULT_TIMEOUT_MINUTES = 30;
const WARNING_BEFORE_MS = 60_000;

@Injectable({ providedIn: 'root' })
export class SessionTimeoutService {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private timeoutHandle: number | undefined;
  private warningHandle: number | undefined;
  private started = false;

  start(): void {
    if (this.started) {
      return;
    }
    this.started = true;
    ['click', 'keydown', 'mousemove', 'touchstart', 'scroll'].forEach((eventName) => {
      window.addEventListener(eventName, () => this.reset(), { passive: true });
    });
    this.reset();
  }

  reset(): void {
    window.clearTimeout(this.timeoutHandle);
    window.clearTimeout(this.warningHandle);
    if (!this.auth.isAuthenticated()) {
      return;
    }
    const timeoutMs = this.timeoutMinutes() * 60_000;
    const warningAt = Math.max(0, timeoutMs - WARNING_BEFORE_MS);
    this.warningHandle = window.setTimeout(() => this.warn(), warningAt);
    this.timeoutHandle = window.setTimeout(() => this.expire(), timeoutMs);
  }

  private warn(): void {
    if (!this.auth.isAuthenticated()) {
      return;
    }
    const keepSession = window.confirm('Sua sessão será encerrada por inatividade. Deseja continuar conectado?');
    if (keepSession) {
      this.reset();
    }
  }

  private expire(): void {
    if (!this.auth.isAuthenticated()) {
      return;
    }
    this.auth.logout('expired');
    void this.router.navigate(['/login'], { queryParams: { reason: 'session-expired' } });
  }

  private timeoutMinutes(): number {
    const configured = Number(localStorage.getItem(TIMEOUT_KEY) || DEFAULT_TIMEOUT_MINUTES);
    return Number.isFinite(configured) && configured >= 5 ? configured : DEFAULT_TIMEOUT_MINUTES;
  }
}
