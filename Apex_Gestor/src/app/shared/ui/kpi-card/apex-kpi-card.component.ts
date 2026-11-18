import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { IonIcon } from '@ionic/angular/standalone';

import { ApexChartTone } from '../charts/chart.models';
import { ApexSparklineChartComponent } from '../charts/apex-sparkline-chart.component';
import { ApexSkeletonComponent } from '../skeleton/apex-skeleton.component';

@Component({
  selector: 'apex-kpi-card',
  standalone: true,
  imports: [IonIcon, ApexSparklineChartComponent, ApexSkeletonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <article [class]="'kpi-card tone-' + tone()">
      <header class="kpi-head">
        <span class="kpi-label">{{ label() }}</span>
        <span class="kpi-icon" aria-hidden="true">
          <ion-icon [name]="icon()"></ion-icon>
        </span>
      </header>

      @if (loading()) {
        <apex-skeleton height="28px" width="72%" radius="var(--apex-radius-sm)"></apex-skeleton>
        <apex-skeleton height="14px" width="52%" radius="var(--apex-radius-sm)"></apex-skeleton>
      } @else {
        <strong class="kpi-value">{{ value() }}</strong>
        @if (hint()) {
          <small [class]="'kpi-hint hint-' + hintTone()">{{ hint() }}</small>
        }
      }

      @if (!loading() && sparkline().length > 1) {
        <apex-sparkline-chart class="kpi-spark" [values]="sparkline()" [tone]="tone()"></apex-sparkline-chart>
      }
    </article>
  `,
  styles: [`
    .kpi-card {
      background: var(--apex-bg-surface);
      border: 1px solid var(--apex-border-default);
      border-radius: var(--apex-radius-lg);
      box-shadow: var(--apex-shadow-sm);
      display: grid;
      gap: 8px;
      min-height: 118px;
      overflow: hidden;
      padding: var(--apex-card-pad);
      position: relative;
      transition: border-color var(--apex-duration-fast) var(--apex-ease), box-shadow var(--apex-duration-fast) var(--apex-ease);
    }

    .kpi-card:hover {
      border-color: var(--apex-border-strong);
      box-shadow: var(--apex-shadow-md);
    }

    .kpi-head {
      align-items: center;
      display: flex;
      gap: 10px;
      justify-content: space-between;
    }

    .kpi-label {
      color: var(--apex-text-muted);
      font-size: var(--apex-font-size-xs);
      font-weight: 650;
    }

    .kpi-icon {
      align-items: center;
      border-radius: var(--apex-radius-md);
      color: #ffffff;
      display: inline-flex;
      flex: 0 0 auto;
      font-size: 1.1rem;
      height: 36px;
      justify-content: center;
      width: 36px;
    }

    .tone-blue .kpi-icon { background: var(--apex-raw-grad-blue-violet); }
    .tone-violet .kpi-icon { background: var(--apex-raw-grad-violet-rose); }
    .tone-rose .kpi-icon { background: linear-gradient(135deg, #e11d48 0%, #f43f5e 100%); }
    .tone-cyan .kpi-icon { background: var(--apex-raw-grad-cyan-blue); }
    .tone-emerald .kpi-icon { background: linear-gradient(135deg, #059669 0%, #10b981 100%); }
    .tone-amber .kpi-icon { background: linear-gradient(135deg, #d97706 0%, #f59e0b 100%); }
    .tone-dawn .kpi-icon { background: var(--apex-brand-gradient); }

    .kpi-value {
      color: var(--apex-text-primary);
      font-size: clamp(1.32rem, 2vw, 1.75rem);
      font-variant-numeric: tabular-nums;
      font-weight: 800;
      letter-spacing: -0.02em;
      line-height: 1.05;
    }

    .kpi-hint {
      color: var(--apex-text-muted);
      font-size: var(--apex-font-size-xs);
      font-weight: 600;
    }

    .kpi-hint.hint-positive { color: var(--apex-status-success); }
    .kpi-hint.hint-attention { color: var(--apex-status-warning); }
    .kpi-hint.hint-danger { color: var(--apex-status-danger); }

    .kpi-spark {
      margin-top: 2px;
    }
  `]
})
export class ApexKpiCardComponent {
  readonly label = input.required<string>();
  readonly value = input.required<string>();
  readonly icon = input('trending-up-outline');
  readonly tone = input<ApexChartTone>('blue');
  readonly hint = input('');
  readonly hintTone = input<'neutral' | 'positive' | 'attention' | 'danger'>('neutral');
  readonly sparkline = input<readonly number[]>([]);
  readonly loading = input(false);
}
