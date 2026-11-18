import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'apex-skeleton',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span
      class="skeleton"
      [style.width]="width()"
      [style.height]="height()"
      [style.borderRadius]="radius()"
      aria-hidden="true"
    ></span>
  `,
  styles: [`
    :host {
      display: inline-block;
      line-height: 0;
      width: 100%;
    }

    .skeleton {
      animation: apex-pulse 1.5s var(--apex-ease) infinite;
      background: linear-gradient(
        90deg,
        var(--apex-bg-surface-subtle) 0%,
        var(--apex-bg-surface-elevated) 50%,
        var(--apex-bg-surface-subtle) 100%
      );
      background-size: 200% 100%;
      display: block;
    }

    @keyframes apex-pulse {
      0% { background-position: 200% 0; }
      100% { background-position: -200% 0; }
    }

    @media (prefers-reduced-motion: reduce) {
      .skeleton {
        animation: none;
        background: var(--apex-bg-surface-subtle);
      }
    }
  `]
})
export class ApexSkeletonComponent {
  readonly width = input('100%');
  readonly height = input('16px');
  readonly radius = input('var(--apex-radius-sm)');
}
