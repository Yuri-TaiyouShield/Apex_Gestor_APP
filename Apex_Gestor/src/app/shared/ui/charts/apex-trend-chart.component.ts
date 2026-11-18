import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';

import { ApexEmptyStateComponent } from '../empty-state/apex-empty-state.component';
import { ApexSkeletonComponent } from '../skeleton/apex-skeleton.component';
import { buildSmoothAreaPath, buildSmoothLinePath, computeNiceMax, SvgCoord } from './chart-geometry';
import { ApexChartPoint, ApexChartTone } from './chart.models';

export type ApexTrendChartMode = 'area' | 'line' | 'bar';

interface PlottedPoint extends SvgCoord {
  label: string;
  value: number;
  hint?: string;
  barY: number;
  barHeight: number;
  barWidth: number;
  barX: number;
  formattedValue: string;
}

let trendChartIdSeq = 0;

@Component({
  selector: 'apex-trend-chart',
  standalone: true,
  imports: [ApexEmptyStateComponent, ApexSkeletonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (loading()) {
      <div class="chart-skeleton" aria-busy="true" aria-label="Carregando gráfico">
        <apex-skeleton height="220px" radius="var(--apex-radius-md)"></apex-skeleton>
      </div>
    } @else if (!plotted().length) {
      <apex-empty-state
        icon="bar-chart-outline"
        [title]="emptyTitle()"
        [description]="emptyDescription()"
      ></apex-empty-state>
    } @else {
      <figure class="chart-shell">
        <div class="chart-canvas-wrap">
          @if (activePoint(); as hovered) {
            <div
              class="chart-tooltip"
              [style.left.%]="tooltipLeftPercent(hovered.x)"
              role="status"
            >
              <span>{{ hovered.label }}</span>
              <strong>{{ hovered.formattedValue }}</strong>
              @if (hovered.hint) {
                <small>{{ hovered.hint }}</small>
              }
            </div>
          }

          <svg
            class="chart-svg"
            [attr.viewBox]="'0 0 ' + viewWidth + ' ' + viewHeight"
            role="img"
            [attr.aria-label]="ariaTitle()"
          >
            <title>{{ ariaTitle() }}</title>
            <defs>
              <linearGradient [id]="strokeGradId" x1="0%" y1="0%" x2="100%" y2="0%">
                <stop offset="0%" [attr.stop-color]="gradientStart()" />
                <stop offset="52%" [attr.stop-color]="gradientMid()" />
                <stop offset="100%" [attr.stop-color]="gradientEnd()" />
              </linearGradient>

              <linearGradient [id]="areaGradId" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" [attr.stop-color]="gradientStart()" stop-opacity="0.28" />
                <stop offset="55%" [attr.stop-color]="gradientMid()" stop-opacity="0.20" />
                <stop offset="100%" [attr.stop-color]="gradientEnd()" stop-opacity="0.06" />
              </linearGradient>

              <linearGradient [id]="barGradId" x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" [attr.stop-color]="gradientMid()" stop-opacity="0.95" />
                <stop offset="100%" [attr.stop-color]="gradientStart()" stop-opacity="0.72" />
              </linearGradient>
            </defs>

            <!-- Linhas de grade horizontais + rotulos do eixo Y -->
            @for (tick of yTicks(); track tick.y) {
              <line
                [attr.x1]="padLeft"
                [attr.y1]="tick.y"
                [attr.x2]="viewWidth - padRight"
                [attr.y2]="tick.y"
                class="grid-line"
              />
              <text
                [attr.x]="padLeft - 8"
                [attr.y]="tick.y + 4"
                text-anchor="end"
                class="axis-label"
              >
                {{ tick.label }}
              </text>
            }

            @if (mode() === 'bar') {
              @for (pt of plotted(); track $index; let i = $index) {
                <rect
                  [attr.x]="pt.barX"
                  [attr.y]="pt.barY"
                  [attr.width]="pt.barWidth"
                  [attr.height]="pt.barHeight"
                  rx="6"
                  [attr.fill]="'url(#' + barGradId + ')'"
                  class="chart-bar"
                  [class.active]="activeIndex() === i"
                  (mouseenter)="setActive(i)"
                  (mouseleave)="clearActive()"
                />
              }
            } @else {
              @if (mode() === 'area') {
                <path [attr.d]="areaPath()" [attr.fill]="'url(#' + areaGradId + ')'" />
              }

              <path
                [attr.d]="linePath()"
                fill="none"
                [attr.stroke]="'url(#' + strokeGradId + ')'"
                stroke-width="3"
                stroke-linecap="round"
                stroke-linejoin="round"
              />

              @if (activePoint(); as hovered) {
                <line
                  [attr.x1]="hovered.x"
                  [attr.y1]="padTop"
                  [attr.x2]="hovered.x"
                  [attr.y2]="baselineY"
                  class="focus-guide"
                />
              }

              @for (pt of plotted(); track $index; let i = $index) {
                <circle
                  [attr.cx]="pt.x"
                  [attr.cy]="pt.y"
                  [attr.r]="activeIndex() === i ? 6 : 4"
                  class="chart-dot"
                  [class.active]="activeIndex() === i"
                />
                <rect
                  [attr.x]="pt.x - hitBandWidth() / 2"
                  [attr.y]="padTop"
                  [attr.width]="hitBandWidth()"
                  [attr.height]="plotHeight"
                  fill="transparent"
                  (mouseenter)="setActive(i)"
                  (mouseleave)="clearActive()"
                />
              }
            }

            <!-- Rotulos do eixo X -->
            @for (pt of plotted(); track $index; let i = $index) {
              @if (shouldShowXLabel(i)) {
                <text
                  [attr.x]="pt.x"
                  [attr.y]="viewHeight - 8"
                  text-anchor="middle"
                  class="axis-label"
                >
                  {{ pt.label }}
                </text>
              }
            }
          </svg>
        </div>

        <!-- Legenda interativa acessivel por teclado -->
        <div class="point-strip" role="group" [attr.aria-label]="'Pontos da série ' + ariaTitle()">
          @for (pt of plotted(); track $index; let i = $index) {
            <button
              type="button"
              class="point-pill"
              [class.active]="activeIndex() === i"
              (mouseenter)="setActive(i)"
              (mouseleave)="clearActive()"
              (focus)="setActive(i)"
              (blur)="clearActive()"
            >
              <span>{{ pt.label }}</span>
              <strong>{{ pt.formattedValue }}</strong>
            </button>
          }
        </div>

        <table class="apex-sr-only">
          <caption>{{ ariaTitle() }}</caption>
          <thead>
            <tr>
              <th scope="col">Período</th>
              <th scope="col">Valor</th>
            </tr>
          </thead>
          <tbody>
            @for (pt of plotted(); track $index) {
              <tr>
                <td>{{ pt.label }}</td>
                <td>{{ pt.formattedValue }}</td>
              </tr>
            }
          </tbody>
        </table>
      </figure>
    }
  `,
  styles: [`
    .chart-shell {
      display: grid;
      gap: 10px;
      margin: 0;
    }

    .chart-canvas-wrap {
      position: relative;
      width: 100%;
    }

    .chart-svg {
      display: block;
      height: auto;
      max-height: 280px;
      width: 100%;
    }

    .grid-line {
      stroke: var(--apex-border-subtle);
      stroke-dasharray: 4 4;
      stroke-width: 1;
    }

    .focus-guide {
      stroke: var(--apex-brand-secondary);
      stroke-dasharray: 4 4;
      stroke-opacity: 0.6;
      stroke-width: 1.5;
    }

    .axis-label {
      fill: var(--apex-text-muted);
      font-family: var(--apex-font-sans);
      font-size: 11px;
      font-weight: 600;
    }

    .chart-dot {
      fill: var(--apex-bg-surface);
      stroke: var(--apex-brand-secondary);
      stroke-width: 2.5;
      transition: r var(--apex-duration-fast) var(--apex-ease);
    }

    .chart-dot.active {
      fill: var(--apex-brand-accent);
      stroke: #ffffff;
      stroke-width: 2.5;
    }

    .chart-bar {
      transition: opacity var(--apex-duration-fast) var(--apex-ease);
    }

    .chart-bar.active {
      opacity: 1;
      stroke: var(--apex-text-primary);
      stroke-width: 1;
    }

    .chart-tooltip {
      background: var(--apex-bg-surface-elevated);
      border: 1px solid var(--apex-border-strong);
      border-radius: var(--apex-radius-md);
      box-shadow: var(--apex-shadow-md);
      display: grid;
      gap: 2px;
      min-width: 120px;
      padding: 8px 10px;
      pointer-events: none;
      position: absolute;
      top: 4px;
      transform: translateX(-50%);
      z-index: 2;
    }

    .chart-tooltip span,
    .chart-tooltip small {
      color: var(--apex-text-muted);
      font-size: var(--apex-font-size-2xs);
    }

    .chart-tooltip strong {
      color: var(--apex-text-primary);
      font-size: var(--apex-font-size-sm);
      font-variant-numeric: tabular-nums;
      font-weight: 750;
    }

    .point-strip {
      display: flex;
      gap: 6px;
      overflow-x: auto;
      padding-bottom: 2px;
    }

    .point-pill {
      align-items: baseline;
      background: var(--apex-bg-surface-muted);
      border: 1px solid var(--apex-border-subtle);
      border-radius: var(--apex-radius-sm);
      color: var(--apex-text-secondary);
      cursor: pointer;
      display: inline-flex;
      flex: 0 0 auto;
      font: inherit;
      font-size: var(--apex-font-size-2xs);
      gap: 6px;
      padding: 4px 8px;
      transition: border-color var(--apex-duration-fast) var(--apex-ease);
    }

    .point-pill.active,
    .point-pill:hover {
      border-color: var(--apex-brand-primary);
      color: var(--apex-text-primary);
    }

    .point-pill strong {
      font-variant-numeric: tabular-nums;
      font-weight: 700;
    }
  `]
})
export class ApexTrendChartComponent {
  readonly points = input<readonly ApexChartPoint[]>([]);
  readonly mode = input<ApexTrendChartMode>('area');
  readonly tone = input<ApexChartTone>('dawn');
  readonly loading = input(false);
  readonly ariaTitle = input('Série temporal de indicadores');
  readonly emptyTitle = input('Sem registros no período');
  readonly emptyDescription = input('Não há movimentações reais disponíveis para montar o gráfico.');
  readonly valueFormatter = input<(value: number) => string>(
    (v) => v.toLocaleString('pt-BR', { maximumFractionDigits: 2 })
  );

  private readonly uid = ++trendChartIdSeq;
  readonly strokeGradId = `apex-trend-stroke-${this.uid}`;
  readonly areaGradId = `apex-trend-area-${this.uid}`;
  readonly barGradId = `apex-trend-bar-${this.uid}`;

  readonly viewWidth = 640;
  readonly viewHeight = 230;
  readonly padLeft = 58;
  readonly padRight = 18;
  readonly padTop = 18;
  readonly padBottom = 30;
  readonly plotHeight = this.viewHeight - this.padTop - this.padBottom;
  readonly baselineY = this.padTop + this.plotHeight;

  readonly activeIndex = signal<number | null>(null);

  readonly gradientStart = computed(() => {
    const tone = this.tone();
    if (tone === 'cyan') return '#06b6d4';
    if (tone === 'violet') return '#7c3aed';
    if (tone === 'rose') return '#e11d48';
    if (tone === 'emerald') return '#059669';
    if (tone === 'amber') return '#f59e0b';
    return '#2563eb';
  });

  readonly gradientMid = computed(() => {
    const tone = this.tone();
    if (tone === 'cyan') return '#2563eb';
    if (tone === 'emerald') return '#10b981';
    if (tone === 'amber') return '#d97706';
    return '#7c3aed';
  });

  readonly gradientEnd = computed(() => {
    const tone = this.tone();
    if (tone === 'cyan') return '#7c3aed';
    if (tone === 'blue') return '#7c3aed';
    if (tone === 'emerald') return '#06b6d4';
    return '#e11d48';
  });

  private readonly sanitizedPoints = computed(() =>
    this.points().filter((point) => Number.isFinite(point.value))
  );

  private readonly niceMax = computed(() => {
    const maxVal = Math.max(0, ...this.sanitizedPoints().map((p) => p.value));
    return computeNiceMax(maxVal);
  });

  readonly yTicks = computed(() => {
    const max = this.niceMax();
    const fmt = this.valueFormatter();
    return [0, 0.25, 0.5, 0.75, 1].map((ratio) => {
      const raw = max * ratio;
      const tickVal = max >= 4 ? Math.round(raw) : Math.round(raw * 100) / 100;
      return {
        y: this.baselineY - ratio * this.plotHeight,
        label: fmt(tickVal)
      };
    });
  });

  readonly plotted = computed<PlottedPoint[]>(() => {
    const raw = this.sanitizedPoints();
    if (!raw.length) {
      return [];
    }
    const max = this.niceMax() || 1;
    const plotW = this.viewWidth - this.padLeft - this.padRight;
    const fmt = this.valueFormatter();
    const barSlot = plotW / raw.length;
    const barW = Math.min(38, Math.max(12, barSlot * 0.54));

    return raw.map((item, idx) => {
      const clamped = Math.max(0, item.value);
      const ratio = Math.min(1, clamped / max);
      const barH = clamped > 0 ? Math.max(2, ratio * this.plotHeight) : 0;
      const x =
        this.mode() === 'bar'
          ? this.padLeft + barSlot * idx + barSlot / 2
          : raw.length === 1
            ? this.padLeft + plotW / 2
            : this.padLeft + (idx / (raw.length - 1)) * plotW;
      const y = this.baselineY - ratio * this.plotHeight;
      const barY = this.baselineY - barH;

      return {
        x,
        y,
        label: item.label,
        value: item.value,
        hint: item.hint,
        barY,
        barHeight: barH,
        barWidth: barW,
        barX: x - barW / 2,
        formattedValue: fmt(item.value)
      };
    });
  });

  tooltipLeftPercent(x: number): number {
    const rawPct = (x / this.viewWidth) * 100;
    return Math.min(88, Math.max(12, rawPct));
  }

  readonly linePath = computed(() => buildSmoothLinePath(this.plotted()));
  readonly areaPath = computed(() => buildSmoothAreaPath(this.plotted(), this.baselineY));
  readonly hitBandWidth = computed(() => {
    const count = Math.max(1, this.plotted().length);
    return Math.max(18, (this.viewWidth - this.padLeft - this.padRight) / count);
  });
  readonly activePoint = computed(() => {
    const idx = this.activeIndex();
    const list = this.plotted();
    return idx !== null && idx >= 0 && idx < list.length ? list[idx] : null;
  });

  shouldShowXLabel(index: number): boolean {
    const len = this.plotted().length;
    if (len <= 8) {
      return true;
    }
    const step = Math.ceil(len / 7);
    return index % step === 0 || index === len - 1;
  }

  setActive(index: number): void {
    this.activeIndex.set(index);
  }

  clearActive(): void {
    this.activeIndex.set(null);
  }
}
