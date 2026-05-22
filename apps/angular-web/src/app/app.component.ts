import { CommonModule } from '@angular/common';
import { Component, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdmCalcRequest, AdmCalcResultado } from './admcalc.models';
import { AdmCalcService } from './admcalc.service';

type View = 'dashboard' | 'estoque' | 'admcalc';

@Component({
  selector: 'apex-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  readonly sidebarCollapsed = signal(false);
  readonly theme = signal<'light' | 'dark'>('light');
  readonly activeView = signal<View>('dashboard');
  readonly toast = signal('Interface Angular pronta para operação.');
  readonly loading = signal(false);
  readonly resultado = signal<AdmCalcResultado | null>(null);

  readonly title = computed(() => {
    const labels: Record<View, string> = {
      dashboard: 'Painel Web',
      estoque: 'Estoque inteligente',
      admcalc: 'AdmCalc financeiro'
    };
    return labels[this.activeView()];
  });

  form: AdmCalcRequest = {
    salarioMensal: 4200,
    jornadaMensal: 220,
    percentualHoraExtra: 0.5,
    horasExtras: 12,
    horasNoturnasRelogio: 8,
    adicionalPericulosidade: true,
    salarioMinimo: 1412,
    percentualInsalubridade: 0.2,
    custoPassagens: 310,
    filhosElegiveis: 1,
    cotaSalarioFamilia: 62.04,
    mesesTrabalhados: 12,
    receitaBruta: 82000,
    folhaSalarios: 24600,
    proLabore: 6500
  };

  constructor(private readonly admCalcService: AdmCalcService) {
    document.documentElement.dataset['theme'] = this.theme();
  }

  setView(view: View) {
    this.activeView.set(view);
    this.toast.set(`Tela ${this.title()} carregada.`);
  }

  toggleSidebar() {
    this.sidebarCollapsed.update((value) => !value);
  }

  toggleTheme() {
    this.theme.update((value) => value === 'light' ? 'dark' : 'light');
    document.documentElement.dataset['theme'] = this.theme();
    this.toast.set(`Tema ${this.theme() === 'dark' ? 'escuro' : 'claro'} aplicado.`);
  }

  calcularAdmCalc() {
    this.loading.set(true);
    this.admCalcService.calcular(this.form).subscribe((resultado) => {
      this.resultado.set(resultado);
      this.loading.set(false);
      this.toast.set('AdmCalc calculado com sucesso. Resultados prontos para conferência.');
    });
  }

  sync() {
    this.toast.set('Sincronização solicitada. A API será usada quando estiver disponível.');
  }

  exportarResumo() {
    this.toast.set('Resumo financeiro preparado para exportação em PDF na próxima etapa.');
  }
}
