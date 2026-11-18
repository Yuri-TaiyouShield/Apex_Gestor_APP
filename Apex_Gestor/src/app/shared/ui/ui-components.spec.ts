import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { addIcons } from 'ionicons';
import { albumsOutline, barChartOutline, pieChartOutline, trendingUpOutline } from 'ionicons/icons';

import { DialogService } from '../../core/ui/dialog.service';
import {
  ApexBreadcrumb,
  ApexChartPoint,
  ApexChartSlice,
  ApexDialogHostComponent,
  ApexDonutChartComponent,
  ApexEmptyStateComponent,
  ApexKpiCardComponent,
  ApexPageHeaderComponent,
  ApexStatusBadgeComponent,
  ApexTrendChartComponent,
  ApexTrendChartMode
} from './index';

@Component({
  standalone: true,
  imports: [ApexStatusBadgeComponent, ApexEmptyStateComponent],
  template: `
    <apex-status-badge tone="success">Aprovado</apex-status-badge>
    <apex-empty-state
      title="Sem pedidos"
      description="Crie um pedido para visualizar os indicadores."
    >
      <button type="button" class="retry-btn">Atualizar</button>
    </apex-empty-state>
  `
})
class BasicUiHostComponent {}

@Component({
  standalone: true,
  imports: [ApexPageHeaderComponent],
  template: `
    <apex-page-header
      kicker="Operação"
      title="Dashboard Executivo"
      subtitle="Indicadores em tempo real"
      [breadcrumbs]="breadcrumbs"
    >
      <button type="button">Nova venda</button>
    </apex-page-header>
  `
})
class HeaderHostComponent {
  breadcrumbs: ApexBreadcrumb[] = [
    { label: 'Início', link: '/' },
    { label: 'Dashboard' }
  ];
}

describe('Design System UI & Declarative SVG Charts', () => {
  beforeAll(() => {
    addIcons({
      'albums-outline': albumsOutline,
      'bar-chart-outline': barChartOutline,
      'pie-chart-outline': pieChartOutline,
      'trending-up-outline': trendingUpOutline
    });
  });

  it('renderiza ApexStatusBadgeComponent e ApexEmptyStateComponent com projeção de conteúdo', () => {
    TestBed.configureTestingModule({
      imports: [BasicUiHostComponent]
    });
    const fixture = TestBed.createComponent(BasicUiHostComponent);
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    const badge = el.querySelector('apex-status-badge .badge');
    expect(badge?.classList.contains('tone-success')).toBeTrue();
    expect(badge?.textContent?.trim()).toBe('Aprovado');

    const empty = el.querySelector('apex-empty-state .empty-box');
    expect(empty?.textContent).toContain('Sem pedidos');
    expect(empty?.querySelector('.retry-btn')?.textContent).toBe('Atualizar');
  });

  it('renderiza ApexPageHeaderComponent com breadcrumbs estruturais e ações', () => {
    TestBed.configureTestingModule({
      imports: [HeaderHostComponent],
      providers: [provideRouter([])]
    });
    const fixture = TestBed.createComponent(HeaderHostComponent);
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('h1')?.textContent).toBe('Dashboard Executivo');
    expect(el.querySelector('.breadcrumbs a')?.textContent).toBe('Início');
    expect(el.querySelector('[aria-current="page"]')?.textContent).toBe('Dashboard');
    expect(el.querySelector('.head-actions button')?.textContent).toBe('Nova venda');
  });

  it('renderiza ApexKpiCardComponent com sparkline SVG e estado de loading com skeleton', () => {
    TestBed.configureTestingModule({
      imports: [ApexKpiCardComponent]
    });
    const fixture = TestBed.createComponent(ApexKpiCardComponent);
    fixture.componentRef.setInput('label', 'Vendas Totais');
    fixture.componentRef.setInput('value', 'R$ 12.400,00');
    fixture.componentRef.setInput('hint', '+8,2% no período');
    fixture.componentRef.setInput('hintTone', 'positive');
    fixture.componentRef.setInput('sparkline', [100, 140, 125, 180]);
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.kpi-value')?.textContent).toBe('R$ 12.400,00');
    expect(el.querySelector('apex-sparkline-chart svg path')).not.toBeNull();

    fixture.componentRef.setInput('loading', true);
    fixture.detectChanges();

    expect(el.querySelector('.kpi-value')).toBeNull();
    expect(el.querySelectorAll('apex-skeleton').length).toBe(2);
  });

  it('renderiza ApexTrendChartComponent em modos area/line/bar, tooltip interativa e tabela acessível', () => {
    const samplePoints: ApexChartPoint[] = [
      { label: 'Seg', value: 120, hint: '3 pedidos' },
      { label: 'Ter', value: 240, hint: '5 pedidos' },
      { label: 'Qua', value: 180, hint: '4 pedidos' }
    ];

    TestBed.configureTestingModule({
      imports: [ApexTrendChartComponent]
    });
    const fixture = TestBed.createComponent(ApexTrendChartComponent);
    fixture.componentRef.setInput('ariaTitle', 'Vendas semanais');
    fixture.componentRef.setInput('points', samplePoints);
    fixture.componentRef.setInput('mode', 'area' as ApexTrendChartMode);
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelectorAll('svg path').length).toBe(2);
    expect(el.querySelectorAll('table.apex-sr-only tbody tr').length).toBe(3);

    const pills = el.querySelectorAll<HTMLButtonElement>('.point-pill');
    expect(pills.length).toBe(3);
    pills[1].dispatchEvent(new FocusEvent('focus'));
    fixture.detectChanges();

    const tooltip = el.querySelector('.chart-tooltip');
    expect(tooltip?.textContent).toContain('Ter');
    expect(tooltip?.textContent).toContain('240');
    expect(tooltip?.textContent).toContain('5 pedidos');

    fixture.componentRef.setInput('mode', 'bar' as ApexTrendChartMode);
    fixture.detectChanges();
    expect(el.querySelectorAll('svg rect.chart-bar').length).toBe(3);

    fixture.componentRef.setInput('mode', 'line' as ApexTrendChartMode);
    fixture.detectChanges();
    expect(el.querySelectorAll('svg path').length).toBe(1);

    fixture.componentRef.setInput('points', []);
    fixture.detectChanges();
    expect(el.querySelector('apex-empty-state')).not.toBeNull();

    fixture.componentRef.setInput('loading', true);
    fixture.detectChanges();
    expect(el.querySelector('.chart-skeleton apex-skeleton')).not.toBeNull();
  });

  it('renderiza ApexDonutChartComponent em variantes donut e pie com legenda focável', () => {
    const slices: ApexChartSlice[] = [
      { label: 'PIX', value: 600 },
      { label: 'Crédito', value: 400 }
    ];

    TestBed.configureTestingModule({
      imports: [ApexDonutChartComponent]
    });
    const fixture = TestBed.createComponent(ApexDonutChartComponent);
    fixture.componentRef.setInput('ariaTitle', 'Formas de pagamento');
    fixture.componentRef.setInput('centerLabel', 'Receita');
    fixture.componentRef.setInput('slices', slices);
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.donut-center strong')?.textContent).toBe('1.000');
    expect(el.querySelector('.donut-center span')?.textContent).toBe('Receita');
    expect(el.querySelectorAll('table.apex-sr-only tbody tr').length).toBe(2);

    const legendButtons = el.querySelectorAll<HTMLButtonElement>('.legend-row');
    legendButtons[0].dispatchEvent(new FocusEvent('focus'));
    fixture.detectChanges();
    expect(el.querySelector('.donut-center strong')?.textContent).toBe('60%');
    expect(el.querySelector('.donut-center span')?.textContent).toBe('PIX');

    fixture.componentRef.setInput('variant', 'pie');
    fixture.detectChanges();
    expect(el.querySelectorAll('svg path.donut-ring').length).toBe(2);
    expect(el.querySelector('.donut-center')).toBeNull();

    fixture.componentRef.setInput('slices', []);
    fixture.detectChanges();
    expect(el.querySelector('apex-empty-state')).not.toBeNull();

    fixture.componentRef.setInput('loading', true);
    fixture.detectChanges();
    expect(el.querySelector('apex-skeleton')).not.toBeNull();
  });

  it('trata rótulos duplicados, NaN/Infinity, valores baixos e barras zeradas com acessibilidade', () => {
    TestBed.configureTestingModule({
      imports: [ApexTrendChartComponent, ApexDonutChartComponent]
    });
    const trendFixture = TestBed.createComponent(ApexTrendChartComponent);
    trendFixture.componentRef.setInput('tone', 'amber');
    trendFixture.componentRef.setInput('mode', 'bar');
    trendFixture.componentRef.setInput('points', [
      { label: 'Dia', value: 0 },
      { label: 'Dia', value: 2 },
      { label: 'Inválido', value: Number.NaN }
    ]);
    trendFixture.detectChanges();

    const trendEl = trendFixture.nativeElement as HTMLElement;
    expect(trendEl.querySelector('.point-strip')?.getAttribute('role')).toBe('group');
    const bars = trendEl.querySelectorAll('svg rect.chart-bar');
    expect(bars.length).toBe(2);
    expect(bars[0].getAttribute('height')).toBe('0');
    expect(bars[0].getAttribute('y')).toBe(String(trendFixture.componentInstance.baselineY));
    const yTickLabels = Array.from(trendEl.querySelectorAll('svg text')).map((node) => node.textContent?.trim());
    expect(yTickLabels.length).toBeGreaterThanOrEqual(5);

    const donutFixture = TestBed.createComponent(ApexDonutChartComponent);
    donutFixture.componentRef.setInput('slices', [
      { label: 'Item', value: 10 },
      { label: 'Item', value: 30 },
      { label: 'Ignorar', value: Number.POSITIVE_INFINITY }
    ]);
    donutFixture.detectChanges();

    const donutEl = donutFixture.nativeElement as HTMLElement;
    expect(donutEl.querySelector('.donut-legend')?.getAttribute('role')).toBe('group');
    expect(donutEl.querySelectorAll('.legend-row').length).toBe(2);
    expect(donutEl.querySelector('.donut-center strong')?.textContent).toBe('40');
  });

  it('omite aria-describedby quando mensagem está vazia e mantém foco preso por Tab no ApexDialogHostComponent', async () => {
    TestBed.configureTestingModule({
      imports: [ApexDialogHostComponent]
    });
    const fixture = TestBed.createComponent(ApexDialogHostComponent);
    const dialog = TestBed.inject(DialogService);
    fixture.detectChanges();

    const promptPromise = dialog.prompt({
      title: 'Código rápido',
      label: 'Código',
      message: '',
      required: false
    });
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    const card = el.querySelector<HTMLElement>('.dialog-card');
    expect(card?.getAttribute('aria-describedby')).toBeNull();

    const buttons = el.querySelectorAll<HTMLButtonElement>('.dialog-footer button');
    const confirmBtn = buttons[buttons.length - 1];
    confirmBtn.focus();
    const inputEl = el.querySelector<HTMLInputElement>('.dialog-field input');

    card?.dispatchEvent(new KeyboardEvent('keydown', { key: 'Tab', bubbles: true }));
    expect(document.activeElement).toBe(inputEl);

    card?.dispatchEvent(new KeyboardEvent('keydown', { key: 'Tab', shiftKey: true, bubbles: true }));
    expect(document.activeElement).toBe(confirmBtn);

    dialog.cancel();
    await expectAsync(promptPromise).toBeResolvedTo(null);
  });

  it('renderiza ApexDialogHostComponent com validação de prompt, submissão e fechamento via Escape', async () => {
    TestBed.configureTestingModule({
      imports: [ApexDialogHostComponent]
    });
    const fixture = TestBed.createComponent(ApexDialogHostComponent);
    const dialog = TestBed.inject(DialogService);
    fixture.detectChanges();

    const confirmPromise = dialog.confirm({
      title: 'Confirmar ação',
      message: 'Deseja prosseguir?',
      tone: 'warning'
    });
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.dialog-card')?.getAttribute('role')).toBe('alertdialog');

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();
    await expectAsync(confirmPromise).toBeResolvedTo(false);
    expect(el.querySelector('.dialog-card')).toBeNull();

    const promptPromise = dialog.prompt({
      title: 'Identificação',
      label: 'Código',
      required: true,
      requiredMessage: 'Código obrigatório'
    });
    fixture.detectChanges();

    const submitButton = el.querySelector<HTMLButtonElement>('.dialog-footer button:last-child');
    submitButton?.click();
    fixture.detectChanges();
    expect(el.querySelector('.dialog-error')?.textContent).toBe('Código obrigatório');

    dialog.updatePromptValue('  APX-99  ');
    fixture.detectChanges();
    el.querySelector<HTMLButtonElement>('.dialog-footer button:last-child')?.click();
    fixture.detectChanges();

    await expectAsync(promptPromise).toBeResolvedTo('APX-99');
  });
});
