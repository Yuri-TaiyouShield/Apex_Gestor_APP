import { Injectable, signal } from '@angular/core';

export type ApexToastTone = 'info' | 'success' | 'warning' | 'danger';

export interface ApexToast {
  id: number;
  title?: string;
  message: string;
  tone: ApexToastTone;
  durationMs: number;
}

export interface ApexToastOptions {
  title?: string;
  message: string;
  tone?: ApexToastTone;
  durationMs?: number;
}

const MAX_VISIBLE_TOASTS = 4;
const DEFAULT_DURATION_MS = 4500;

/**
 * Gerencia notificacoes transientes acessiveis (toasts) sem acoplamento com overlays nativos.
 */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private nextId = 1;
  private readonly timers = new Map<number, ReturnType<typeof setTimeout>>();

  readonly items = signal<ApexToast[]>([]);

  show(options: ApexToastOptions): number {
    const id = this.nextId++;
    const durationMs = options.durationMs ?? DEFAULT_DURATION_MS;
    const toast: ApexToast = {
      id,
      title: options.title,
      message: options.message,
      tone: options.tone ?? 'info',
      durationMs
    };

    this.items.update((list) => {
      const next = [...list, toast];
      if (next.length > MAX_VISIBLE_TOASTS) {
        const overflow = next.length - MAX_VISIBLE_TOASTS;
        const evicted = next.slice(0, overflow);
        for (const item of evicted) {
          const oldTimer = this.timers.get(item.id);
          if (oldTimer !== undefined) {
            clearTimeout(oldTimer);
            this.timers.delete(item.id);
          }
        }
        return next.slice(overflow);
      }
      return next;
    });

    if (durationMs > 0) {
      const handle = setTimeout(() => this.dismiss(id), durationMs);
      this.timers.set(id, handle);
    }
    return id;
  }

  info(message: string, title?: string): number {
    return this.show({ message, title, tone: 'info' });
  }

  success(message: string, title?: string): number {
    return this.show({ message, title, tone: 'success' });
  }

  warning(message: string, title?: string): number {
    return this.show({ message, title, tone: 'warning', durationMs: 6000 });
  }

  danger(message: string, title?: string): number {
    return this.show({ message, title, tone: 'danger', durationMs: 7000 });
  }

  dismiss(id: number): void {
    const timer = this.timers.get(id);
    if (timer !== undefined) {
      clearTimeout(timer);
      this.timers.delete(id);
    }
    this.items.update((list) => list.filter((item) => item.id !== id));
  }

  clear(): void {
    this.timers.forEach((timer) => clearTimeout(timer));
    this.timers.clear();
    this.items.set([]);
  }
}
