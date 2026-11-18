import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { IonIcon } from '@ionic/angular/standalone';

@Component({
  selector: 'apex-empty-state',
  standalone: true,
  imports: [IonIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="empty-box" role="status">
      <span class="empty-icon" aria-hidden="true">
        <ion-icon [name]="icon()"></ion-icon>
      </span>
      <strong>{{ title() }}</strong>
      @if (description()) {
        <p>{{ description() }}</p>
      }
      <div class="empty-actions">
        <ng-content></ng-content>
      </div>
    </div>
  `,
  styles: [`
    .empty-box {
      align-items: center;
      background: var(--apex-bg-surface-muted);
      border: 1px dashed var(--apex-border-default);
      border-radius: var(--apex-radius-lg);
      display: flex;
      flex-direction: column;
      gap: 6px;
      justify-content: center;
      min-height: 156px;
      padding: 24px 18px;
      text-align: center;
    }

    .empty-icon {
      align-items: center;
      background: var(--apex-primary-soft);
      border-radius: var(--apex-radius-md);
      color: var(--apex-brand-primary);
      display: inline-flex;
      font-size: 1.35rem;
      height: 44px;
      justify-content: center;
      margin-bottom: 4px;
      width: 44px;
    }

    .empty-box strong {
      color: var(--apex-text-primary);
      font-size: var(--apex-font-size-md);
      font-weight: 700;
    }

    .empty-box p {
      color: var(--apex-text-muted);
      font-size: var(--apex-font-size-sm);
      line-height: 1.45;
      margin: 0;
      max-width: 420px;
    }

    .empty-actions:empty {
      display: none;
    }

    .empty-actions {
      margin-top: 8px;
    }
  `]
})
export class ApexEmptyStateComponent {
  readonly icon = input('albums-outline');
  readonly title = input('Nenhum dado disponível');
  readonly description = input('');
}
