import { CommonModule } from '@angular/common';
import { Component, CUSTOM_ELEMENTS_SCHEMA, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { AdmCalcRequest, AdmCalcResultado } from './admcalc.models';
import { AdmCalcService } from './admcalc.service';

type View = 'home' | 'admcalc' | 'tarefas';

@Component({
  selector: 'apex-root',
  standalone: true,
  imports: [CommonModule, FormsModule, IonicModule],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class AppComponent {
  readonly collapsed = signal(false);
  readonly theme = signal<'light' | 'dark'>('light');
  readonly activeView = signal<View>('home');
  readonly loading = signal(false);
  readonly message = signal('Mobile pronto para operação.');
  readonly resultado = signal<AdmCalcResultado | null>(null);

  form: AdmCalcRequest = {
    salarioMensal: 3800,
    jornadaMensal: 220,
    percentualHoraExtra: 0.5,
    horasExtras: 9,
    horasNoturnasRelogio: 4,
    adicionalPericulosidade: false,
    salarioMinimo: 1412,
    percentualInsalubridade: 0.1,
    custoPassagens: 220,
    filhosElegiveis: 1,
    cotaSalarioFamilia: 62.04,
    mesesTrabalhados: 10,
    receitaBruta: 52000,
    folhaSalarios: 14600,
    proLabore: 4800
  };

  constructor(private readonly admCalcService: AdmCalcService) {
    document.documentElement.dataset['theme'] = this.theme();
  }

  setView(view: View) {
    this.activeView.set(view);
    this.message.set(`Tela ${view} carregada.`);
  }

  toggleCollapsed() {
    this.collapsed.update((value) => !value);
  }

  toggleTheme() {
    this.theme.update((value) => value === 'light' ? 'dark' : 'light');
    document.documentElement.dataset['theme'] = this.theme();
    this.message.set(`Tema ${this.theme() === 'dark' ? 'escuro' : 'claro'} aplicado.`);
  }

  calcular() {
    this.loading.set(true);
    this.admCalcService.calcular(this.form).subscribe((resultado) => {
      this.resultado.set(resultado);
      this.loading.set(false);
      this.message.set('AdmCalc calculado e pronto para conferência.');
    });
  }
}
