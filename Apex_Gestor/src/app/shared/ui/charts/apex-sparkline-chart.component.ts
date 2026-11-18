import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { buildSmoothAreaPath, buildSmoothLinePath, SvgCoord } from './chart-geometry';
import { ApexChartTone } from './chart.models';

let sparklineIdSeq = 0;

@Component({
  selector: 'apex-sparkline-chart',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (coords().length > 1) {
      <svg
        class="sparkline-svg"
        viewBox="0 0 180 46"
        preserveAspectRatio="none"
        aria-hidden="true"
      >
        <defs>
          <linearGradient [id]="gradientId" x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" [attr.stop-color]="strokeColor()" stop-opacity="0.28" />
            <stop offset="100%" [attr.stop-color]="strokeColor()" stop-opacity="0.0" />
          </linearGradient>
        </defs>
        <path [attr.d]="areaPath()" [attr.fill]="'url(#' + gradientId + ')'" />
        <path
          [attr.d]="linePath()"
          fill="none"
          [attr.stroke]="strokeColor()"
          stroke-width="2.2"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
    }
  `,
  styles: [`
    :host {
      display: block;
      height: 44px;
      width: 100%;
    }

    .sparkline-svg {
      display: block;
      height: 100%;
      overflow: visible;
      width: 100%;
    }
  `]
})
export class ApexSparklineChartComponent {
  readonly values = input<readonly number[]>([]);
  readonly tone = input<ApexChartTone>('blue');
  readonly gradientId = `apex-spark-${++sparklineIdSeq}`;

  readonly strokeColor = computed(() => {
    const map: Record<ApexChartTone, string> = {
      blue: 'var(--apex-brand-primary)',
      violet: 'var(--apex-brand-secondary)',
      rose: 'var(--apex-brand-accent)',
      cyan: 'var(--apex-brand-cyan)',
      emerald: 'var(--apex-status-success)',
      amber: 'var(--apex-status-warning)',
      dawn: 'var(--apex-brand-secondary)'
    };
    return map[this.tone()];
  });

  readonly coords = computed<SvgCoord[]>(() => {
    const series = this.values().filter((v) => Number.isFinite(v));
    if (series.length < 2) {
      return [];
    }
    const min = Math.min(...series);
    const max = Math.max(...series);
    const span = max - min || 1;
    const width = 176;
    const top = 6;
    const usableH = 34;

    return series.map((val, idx) => ({
      x: 2 + (idx / (series.length - 1)) * width,
      y: top + usableH - ((val - min) / span) * usableH
    }));
  });

  readonly linePath = computed(() => buildSmoothLinePath(this.coords()));
  readonly areaPath = computed(() => buildSmoothAreaPath(this.coords(), 45));
}
