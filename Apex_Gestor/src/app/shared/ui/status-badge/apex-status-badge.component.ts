import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type ApexBadgeTone = 'neutral' | 'primary' | 'secondary' | 'success' | 'warning' | 'danger' | 'info';

@Component({
  selector: 'apex-status-badge',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span [class]="'badge tone-' + tone()">
      <span class="badge-dot" aria-hidden="true"></span>
      <ng-content></ng-content>
    </span>
  `,
  styles: [`
    .badge {
      align-items: center;
      border: 1px solid var(--apex-border-default);
      border-radius: var(--apex-radius-pill);
      display: inline-flex;
      font-size: var(--apex-font-size-xs);
      font-weight: 700;
      gap: 6px;
      line-height: 1;
      padding: 5px 10px;
      white-space: nowrap;
    }

    .badge-dot {
      background: currentColor;
      border-radius: 999px;
      height: 6px;
      width: 6px;
    }

    .tone-neutral {
      background: var(--apex-bg-surface-subtle);
      color: var(--apex-text-secondary);
    }

    .tone-primary {
      background: var(--apex-primary-soft);
      border-color: color-mix(in srgb, var(--apex-primary) 26%, transparent);
      color: var(--apex-text-brand);
    }

    .tone-secondary {
      background: var(--apex-indigo-soft);
      border-color: color-mix(in srgb, var(--apex-brand-secondary) 28%, transparent);
      color: var(--apex-brand-secondary);
    }

    .tone-success {
      background: var(--apex-status-success-bg);
      border-color: var(--apex-status-success-border);
      color: var(--apex-status-success);
    }

    .tone-warning {
      background: var(--apex-status-warning-bg);
      border-color: var(--apex-status-warning-border);
      color: var(--apex-status-warning);
    }

    .tone-danger {
      background: var(--apex-status-danger-bg);
      border-color: var(--apex-status-danger-border);
      color: var(--apex-status-danger);
    }

    .tone-info {
      background: var(--apex-status-info-bg);
      border-color: var(--apex-status-info-border);
      color: var(--apex-status-info);
    }
  `]
})
export class ApexStatusBadgeComponent {
  readonly tone = input<ApexBadgeTone>('neutral');
}
