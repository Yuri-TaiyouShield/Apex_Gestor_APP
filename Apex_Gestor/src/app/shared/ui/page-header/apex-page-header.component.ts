import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

export interface ApexBreadcrumb {
  label: string;
  link?: string;
}

@Component({
  selector: 'apex-page-header',
  standalone: true,
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="apex-page-head">
      <div class="head-copy">
        @if (breadcrumbs().length) {
          <nav class="breadcrumbs" aria-label="Navegação estrutural">
            @for (crumb of breadcrumbs(); track crumb.label; let last = $last) {
              @if (crumb.link && !last) {
                <a [routerLink]="crumb.link">{{ crumb.label }}</a>
              } @else {
                <span [attr.aria-current]="last ? 'page' : null">{{ crumb.label }}</span>
              }
              @if (!last) {
                <span class="sep" aria-hidden="true">/</span>
              }
            }
          </nav>
        }
        @if (kicker()) {
          <span class="page-kicker">{{ kicker() }}</span>
        }
        <h1>{{ title() }}</h1>
        @if (subtitle()) {
          <p>{{ subtitle() }}</p>
        }
      </div>
      <div class="head-actions">
        <ng-content></ng-content>
      </div>
    </header>
  `,
  styles: [`
    .apex-page-head {
      align-items: flex-end;
      display: flex;
      flex-wrap: wrap;
      gap: 14px;
      justify-content: space-between;
      margin-bottom: 18px;
    }

    .head-copy {
      min-width: 0;
    }

    .breadcrumbs {
      align-items: center;
      color: var(--apex-text-muted);
      display: flex;
      flex-wrap: wrap;
      font-size: var(--apex-font-size-xs);
      gap: 6px;
      margin-bottom: 6px;
    }

    .breadcrumbs a {
      color: var(--apex-text-muted);
      text-decoration: none;
    }

    .breadcrumbs a:hover {
      color: var(--apex-brand-primary);
    }

    .breadcrumbs .sep {
      opacity: 0.6;
    }

    .head-copy h1 {
      color: var(--apex-text-primary);
      font-size: clamp(1.3rem, 1.8vw, 1.72rem);
      font-weight: 800;
      letter-spacing: -0.02em;
      line-height: 1.15;
      margin: 0;
    }

    .head-copy p {
      color: var(--apex-text-muted);
      font-size: var(--apex-font-size-sm);
      line-height: 1.45;
      margin: 5px 0 0;
      max-width: 760px;
    }

    .head-actions {
      align-items: center;
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .head-actions:empty {
      display: none;
    }
  `]
})
export class ApexPageHeaderComponent {
  readonly kicker = input('');
  readonly title = input.required<string>();
  readonly subtitle = input('');
  readonly breadcrumbs = input<ApexBreadcrumb[]>([]);
}
