import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';

import { ApexEmptyStateComponent } from '../empty-state/apex-empty-state.component';
import { ApexSkeletonComponent } from '../skeleton/apex-skeleton.component';
import { describePieSlice } from './chart-geometry';
import { APEX_SERIES_COLORS, ApexChartSlice } from './chart.models';

interface ComputedSlice extends ApexChartSlice {
  color: string;
  percent: number;
  dashArray: string;
  dashOffset: number;
  piePath: string;
  formattedValue: string;
}

@Component({
  selector: 'apex-donut-chart',
  standalone: true,
  imports: [ApexEmptyStateComponent, ApexSkeletonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (loading()) {
      <apex-skeleton height="190px" radius="var(--apex-radius-md)"></apex-skeleton>
    } @else if (!computedSlices().length) {
      <apex-empty-state
        icon="pie-chart-outline"
        [title]="emptyTitle()"
        [description]="emptyDescription()"
      ></apex-empty-state>
    } @else {
      <figure class="donut-layout">
        <div class="donut-visual">
          <svg
            viewBox="0 0 180 180"
            class="donut-svg"
            role="img"
            [attr.aria-label]="ariaTitle()"
          >
            <title>{{ ariaTitle() }}</title>

            @if (variant() === 'donut') {
              <circle
                cx="90"
                cy="90"
                [attr.r]="ringRadius"
                fill="none"
                stroke="var(--apex-bg-surface-subtle)"
                [attr.stroke-width]="ringWidth"
              />
              @for (slice of computedSlices(); track $index; let i = $index) {
                <circle
                  cx="90"
                  cy="90"
                  [attr.r]="ringRadius"
                  fill="none"
                  [attr.stroke]="slice.color"
                  [attr.stroke-width]="activeIndex() === i ? ringWidth + 4 : ringWidth"
                  [attr.stroke-dasharray]="slice.dashArray"
                  [attr.stroke-dashoffset]="slice.dashOffset"
                  stroke-linecap="butt"
                  transform="rotate(-90 90 90)"
                  class="donut-ring"
                  [class.dimmed]="activeIndex() !== null && activeIndex() !== i"
                  (mouseenter)="setActive(i)"
                  (mouseleave)="clearActive()"
                />
              }
            } @else {
              @for (slice of computedSlices(); track $index; let i = $index) {
                <path
                  [attr.d]="slice.piePath"
                  [attr.fill]="slice.color"
                  stroke="var(--apex-bg-surface)"
                  stroke-width="2"
                  class="donut-ring"
                  [class.dimmed]="activeIndex() !== null && activeIndex() !== i"
                  (mouseenter)="setActive(i)"
                  (mouseleave)="clearActive()"
                />
              }
            }
          </svg>

          @if (variant() === 'donut') {
            <div class="donut-center">
              @if (activeSlice(); as current) {
                <strong>{{ current.percent.toLocaleString('pt-BR', { maximumFractionDigits: 1 }) }}%</strong>
                <span>{{ current.label }}</span>
              } @else {
                <strong>{{ centerValue() || formattedTotal() }}</strong>
                <span>{{ centerLabel() }}</span>
              }
            </div>
          }
        </div>

        <div class="donut-legend" role="group" [attr.aria-label]="'Legenda de ' + ariaTitle()">
          @for (slice of computedSlices(); track $index; let i = $index) {
            <button
              type="button"
              class="legend-row"
              [class.active]="activeIndex() === i"
              (mouseenter)="setActive(i)"
              (mouseleave)="clearActive()"
              (focus)="setActive(i)"
              (blur)="clearActive()"
            >
              <span class="legend-dot" [style.background]="slice.color" aria-hidden="true"></span>
              <span class="legend-label">{{ slice.label }}</span>
              <strong class="legend-value">{{ slice.percent.toLocaleString('pt-BR', { maximumFractionDigits: 1 }) }}%</strong>
              <small class="legend-raw">{{ slice.formattedValue }}</small>
            </button>
          }
        </div>

        <table class="apex-sr-only">
          <caption>{{ ariaTitle() }}</caption>
          <thead>
            <tr>
              <th scope="col">Categoria</th>
              <th scope="col">Valor</th>
              <th scope="col">Participação</th>
            </tr>
          </thead>
          <tbody>
            @for (slice of computedSlices(); track $index) {
              <tr>
                <td>{{ slice.label }}</td>
                <td>{{ slice.formattedValue }}</td>
                <td>{{ slice.percent.toFixed(1) }}%</td>
              </tr>
            }
          </tbody>
        </table>
      </figure>
    }
  `,
  styles: [`
    .donut-layout {
      align-items: center;
      display: grid;
      gap: 16px;
      grid-template-columns: 164px minmax(0, 1fr);
      margin: 0;
    }

    .donut-visual {
      height: 164px;
      margin: 0 auto;
      position: relative;
      width: 164px;
    }

    .donut-svg {
      display: block;
      height: 100%;
      width: 100%;
    }

    .donut-ring {
      cursor: pointer;
      transition: opacity var(--apex-duration-fast) var(--apex-ease), stroke-width var(--apex-duration-fast) var(--apex-ease);
    }

    .donut-ring.dimmed {
      opacity: 0.42;
    }

    .donut-center {
      align-items: center;
      display: flex;
      flex-direction: column;
      inset: 0;
      justify-content: center;
      padding: 24px;
      pointer-events: none;
      position: absolute;
      text-align: center;
    }

    .donut-center strong {
      color: var(--apex-text-primary);
      font-size: 1.15rem;
      font-variant-numeric: tabular-nums;
      font-weight: 800;
      line-height: 1.1;
    }

    .donut-center span {
      color: var(--apex-text-muted);
      font-size: var(--apex-font-size-2xs);
      margin-top: 3px;
      max-width: 96px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .donut-legend {
      display: grid;
      gap: 6px;
    }

    .legend-row {
      align-items: center;
      background: transparent;
      border: 1px solid transparent;
      border-radius: var(--apex-radius-sm);
      color: var(--apex-text-secondary);
      cursor: pointer;
      display: grid;
      font: inherit;
      gap: 8px;
      grid-template-columns: 10px minmax(0, 1fr) auto auto;
      padding: 6px 8px;
      text-align: left;
      width: 100%;
    }

    .legend-row.active,
    .legend-row:hover {
      background: var(--apex-bg-surface-muted);
      border-color: var(--apex-border-default);
      color: var(--apex-text-primary);
    }

    .legend-dot {
      border-radius: 999px;
      height: 10px;
      width: 10px;
    }

    .legend-label {
      font-size: var(--apex-font-size-xs);
      font-weight: 600;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .legend-value {
      color: var(--apex-text-primary);
      font-size: var(--apex-font-size-xs);
      font-variant-numeric: tabular-nums;
      font-weight: 750;
    }

    .legend-raw {
      color: var(--apex-text-muted);
      font-size: var(--apex-font-size-2xs);
      font-variant-numeric: tabular-nums;
    }

    @media (max-width: 560px) {
      .donut-layout {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class ApexDonutChartComponent {
  readonly slices = input<readonly ApexChartSlice[]>([]);
  readonly variant = input<'donut' | 'pie'>('donut');
  readonly loading = input(false);
  readonly ariaTitle = input('Distribuição percentual');
  readonly centerLabel = input('Total');
  readonly centerValue = input('');
  readonly emptyTitle = input('Sem distribuição para exibir');
  readonly emptyDescription = input('Os percentuais serão calculados automaticamente assim que houver registros.');
  readonly valueFormatter = input<(value: number) => string>(
    (v) => v.toLocaleString('pt-BR', { maximumFractionDigits: 2 })
  );

  readonly ringRadius = 64;
  readonly ringWidth = 22;
  private readonly circumference = 2 * Math.PI * this.ringRadius;

  readonly activeIndex = signal<number | null>(null);

  readonly totalValue = computed(() =>
    this.slices().reduce((sum, item) => {
      const num = Number(item.value);
      return Number.isFinite(num) && num > 0 ? sum + num : sum;
    }, 0)
  );

  readonly formattedTotal = computed(() => this.valueFormatter()(this.totalValue()));

  readonly computedSlices = computed<ComputedSlice[]>(() => {
    const valid = this.slices().filter((item) => {
      const num = Number(item.value);
      return Number.isFinite(num) && num > 0;
    });
    const total = valid.reduce((sum, item) => sum + item.value, 0);
    if (!valid.length || total <= 0) {
      return [];
    }

    const fmt = this.valueFormatter();
    let offsetAcc = 0;
    let angleAcc = 0;

    return valid.map((item, idx) => {
      const ratio = item.value / total;
      const arcLength = ratio * this.circumference;
      const startAngle = angleAcc;
      const sweep = ratio * 360;
      angleAcc += sweep;

      const slice: ComputedSlice = {
        ...item,
        color: item.color ?? APEX_SERIES_COLORS[idx % APEX_SERIES_COLORS.length],
        percent: ratio * 100,
        dashArray: `${arcLength.toFixed(2)} ${(this.circumference - arcLength).toFixed(2)}`,
        dashOffset: -offsetAcc,
        piePath: describePieSlice(90, 90, 74, startAngle, angleAcc),
        formattedValue: fmt(item.value)
      };
      offsetAcc += arcLength;
      return slice;
    });
  });

  readonly activeSlice = computed(() => {
    const idx = this.activeIndex();
    const list = this.computedSlices();
    return idx !== null && idx >= 0 && idx < list.length ? list[idx] : null;
  });

  setActive(index: number): void {
    this.activeIndex.set(index);
  }

  clearActive(): void {
    this.activeIndex.set(null);
  }
}
