import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, of } from 'rxjs';
import { AdmCalcRequest, AdmCalcResultado } from './admcalc.models';

@Injectable({ providedIn: 'root' })
export class AdmCalcService {
  private readonly endpoint = '/api/admcalc/calcular';

  constructor(private readonly http: HttpClient) {}

  calcular(payload: AdmCalcRequest) {
    return this.http
      .post<AdmCalcResultado>(this.endpoint, payload)
      .pipe(catchError(() => of(this.calcularLocal(payload))));
  }

  calcularLocal(payload: AdmCalcRequest): AdmCalcResultado {
    const salario = this.positivo(payload.salarioMensal);
    const jornada = this.positivo(payload.jornadaMensal) || 220;
    const salarioHora = salario / jornada;
    const salarioDiario = salario / 30;
    const valorHorasExtras = salarioHora * (1 + this.positivo(payload.percentualHoraExtra)) * this.positivo(payload.horasExtras);
    const horasNoturnasReduzidas = this.positivo(payload.horasNoturnasRelogio) * 1.142857;
    const adicionalPericulosidade = payload.adicionalPericulosidade ? salario * 0.3 : 0;
    const adicionalInsalubridade = this.positivo(payload.salarioMinimo || 1412) * this.positivo(payload.percentualInsalubridade);
    const descontoValeTransporte = Math.min(this.positivo(payload.custoPassagens), salario * 0.06);
    const salarioFamilia = this.positivo(payload.cotaSalarioFamilia) * Math.max(0, payload.filhosElegiveis || 0);
    const baseAnual = salario + adicionalPericulosidade + adicionalInsalubridade;
    const meses = Math.min(12, Math.max(0, payload.mesesTrabalhados || 0));
    const decimoTerceiroProporcional = (baseAnual / 12) * meses;
    const feriasComTerco = baseAnual * 1.3333;
    const receita = this.positivo(payload.receitaBruta);
    const fatorR = receita === 0 ? 0 : (this.positivo(payload.folhaSalarios) + this.positivo(payload.proLabore)) / receita;

    return {
      salarioHora: this.moeda(salarioHora),
      salarioDiario: this.moeda(salarioDiario),
      valorHorasExtras: this.moeda(valorHorasExtras),
      horasNoturnasReduzidas: this.moeda(horasNoturnasReduzidas),
      adicionalPericulosidade: this.moeda(adicionalPericulosidade),
      adicionalInsalubridade: this.moeda(adicionalInsalubridade),
      descontoValeTransporte: this.moeda(descontoValeTransporte),
      salarioFamilia: this.moeda(salarioFamilia),
      decimoTerceiroProporcional: this.moeda(decimoTerceiroProporcional),
      feriasComTerco: this.moeda(feriasComTerco),
      fatorR: this.moeda(fatorR),
      recomendacao: fatorR >= 0.28
        ? 'Fator R igual ou superior a 28%. Avaliar enquadramento tributário mais vantajoso.'
        : 'Fator R abaixo de 28%. Revisar folha, pró-labore e margem antes de precificar.'
    };
  }

  private positivo(value: number | null | undefined) {
    return Math.max(0, Number(value || 0));
  }

  private moeda(value: number) {
    return Number(value.toFixed(2));
  }
}
