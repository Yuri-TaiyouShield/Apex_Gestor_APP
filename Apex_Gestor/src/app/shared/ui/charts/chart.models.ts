export type ApexChartTone = 'blue' | 'violet' | 'rose' | 'cyan' | 'emerald' | 'amber' | 'dawn';

export interface ApexChartPoint {
  label: string;
  value: number;
  hint?: string;
}

export interface ApexChartSlice {
  label: string;
  value: number;
  color?: string;
}

export const APEX_SERIES_COLORS: readonly string[] = [
  'var(--apex-brand-primary)',
  'var(--apex-brand-secondary)',
  'var(--apex-brand-accent)',
  'var(--apex-brand-cyan)',
  'var(--apex-status-success)',
  'var(--apex-status-warning)'
];
