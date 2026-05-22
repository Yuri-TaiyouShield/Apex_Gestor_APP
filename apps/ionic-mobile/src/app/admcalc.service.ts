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
    const adicionalPericulosidade = payload.adicionalPericulosidade ? salario * 0.3 : 0;
    const adicionalInsalubridade = this.positivo(payload.salarioMinimo || 1412) * this.positivo(payload.percentualInsalubridade);
    const receita = this.positivo(payload.receitaBruta);
    const fatorR = receita === 0 ? 0 : (this.positivo(payload.folhaSalarios) + this.positivo(payload.proLabore)) / receita;

    return {
      salarioHora: this.round(salarioHora),
      salarioDiario: this.round(salario / 30),
      valorHorasExtras: this.round(salarioHora * (1 + this.positivo(payload.percentualHoraExtra)) * this.positivo(payload.horasExtras)),
      horasNoturnasReduzidas: this.round(this.positivo(payload.horasNoturnasRelogio) * 1.142857),
      adicionalPericulosidade: this.round(adicionalPericulosidade),
      adicionalInsalubridade: this.round(adicionalInsalubridade),
      descontoValeTransporte: this.round(Math.min(this.positivo(payload.custoPassagens), salario * 0.06)),
      salarioFamilia: this.round(this.positivo(payload.cotaSalarioFamilia) * Math.max(0, payload.filhosElegiveis || 0)),
      decimoTerceiroProporcional: this.round(((salario + adicionalPericulosidade + adicionalInsalubridade) / 12) * Math.min(12, Math.max(0, payload.mesesTrabalhados || 0))),
      feriasComTerco: this.round((salario + adicionalPericulosidade + adicionalInsalubridade) * 1.3333),
      fatorR: this.round(fatorR),
      recomendacao: fatorR >= 0.28 ? 'Fator R saudável para avaliação tributária.' : 'Fator R baixo. Revisar folha, pró-labore e precificação.'
    };
  }

  private positivo(value: number | null | undefined) {
    return Math.max(0, Number(value || 0));
  }

  private round(value: number) {
    return Number(value.toFixed(2));
  }
}
