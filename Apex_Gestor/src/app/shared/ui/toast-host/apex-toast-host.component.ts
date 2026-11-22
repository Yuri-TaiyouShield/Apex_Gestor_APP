import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { IonIcon } from '@ionic/angular/standalone';

import { ApexToastTone, ToastService } from '../../../core/ui/toast.service';

@Component({
  selector: 'app-toast-host',
  standalone: true,
  imports: [CommonModule, IonIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="toast-region" aria-live="polite" aria-label="Notificações da interface">
      @for (toast of toastService.items(); track toast.id) {
        <article class="toast-item" [class]="'toast-item tone-' + toast.tone" role="status">
          <span class="toast-icon" aria-hidden="true">
            <ion-icon [name]="iconFor(toast.tone)"></ion-icon>
          </span>
          <div class="toast-body">
            @if (toast.title) {
              <strong>{{ toast.title }}</strong>
            }
            <p>{{ toast.message }}</p>
          </div>
          <button
            type="button"
            class="toast-close"
            (click)="toastService.dismiss(toast.id)"
            aria-label="Fechar notificação"
          >
            &times;
          </button>
        </article>
      }
    </section>
  `,
  styles: [`
    .toast-region {
      bottom: 20px;
      display: grid;
      gap: 10px;
      max-width: min(400px, calc(100vw - 24px));
      pointer-events: none;
      position: fixed;
      right: 20px;
      z-index: var(--apex-z-toast);
    }

    .toast-item {
      align-items: flex-start;
      background: var(--apex-bg-surface-elevated);
      border: 1px solid var(--apex-border-strong);
      border-left-width: 4px;
      border-radius: var(--apex-radius-md);
      box-shadow: var(--apex-shadow-lg);
      color: var(--apex-text-primary);
      display: grid;
      gap: 10px;
      grid-template-columns: auto 1fr auto;
      padding: 12px 14px;
      pointer-events: auto;
    }

    .toast-item.tone-info { border-left-color: var(--apex-status-info); }
    .toast-item.tone-success { border-left-color: var(--apex-status-success); }
    .toast-item.tone-warning { border-left-color: var(--apex-status-warning); }
    .toast-item.tone-danger { border-left-color: var(--apex-status-danger); }

    .toast-icon {
      align-items: center;
      border-radius: var(--apex-radius-sm);
      display: inline-flex;
      font-size: 1.1rem;
      height: 28px;
      justify-content: center;
      width: 28px;
    }

    .tone-info .toast-icon { background: var(--apex-status-info-bg); color: var(--apex-status-info); }
    .tone-success .toast-icon { background: var(--apex-status-success-bg); color: var(--apex-status-success); }
    .tone-warning .toast-icon { background: var(--apex-status-warning-bg); color: var(--apex-status-warning); }
    .tone-danger .toast-icon { background: var(--apex-status-danger-bg); color: var(--apex-status-danger); }

    .toast-body strong {
      display: block;
      font-size: var(--apex-font-size-sm);
      font-weight: 700;
      margin-bottom: 2px;
    }

    .toast-body p {
      color: var(--apex-text-secondary);
      font-size: var(--apex-font-size-xs);
      line-height: 1.45;
      margin: 0;
    }

    .toast-close {
      background: transparent;
      border: 0;
      border-radius: var(--apex-radius-xs);
      color: var(--apex-text-muted);
      cursor: pointer;
      font-size: 1.15rem;
      height: 26px;
      line-height: 1;
      width: 26px;
    }

    .toast-close:hover {
      background: var(--apex-bg-surface-subtle);
      color: var(--apex-text-primary);
    }

    @media (max-width: 640px) {
      .toast-region {
        bottom: 12px;
        left: 12px;
        max-width: none;
        right: 12px;
      }
    }
  `]
})
export class ApexToastHostComponent {
  readonly toastService = inject(ToastService);

  iconFor(tone: ApexToastTone): string {
    if (tone === 'success') {
      return 'checkmark-circle-outline';
    }
    if (tone === 'warning' || tone === 'danger') {
      return 'warning-outline';
    }
    return 'shield-checkmark-outline';
  }
}
