export interface AdmCalcRequest {
  salarioMensal: number;
  jornadaMensal: number;
  percentualHoraExtra: number;
  horasExtras: number;
  horasNoturnasRelogio: number;
  adicionalPericulosidade: boolean;
  salarioMinimo: number;
  percentualInsalubridade: number;
  custoPassagens: number;
  filhosElegiveis: number;
  cotaSalarioFamilia: number;
  mesesTrabalhados: number;
  receitaBruta: number;
  folhaSalarios: number;
  proLabore: number;
}

export interface AdmCalcResultado {
  salarioHora: number;
  salarioDiario: number;
  valorHorasExtras: number;
  horasNoturnasReduzidas: number;
  adicionalPericulosidade: number;
  adicionalInsalubridade: number;
  descontoValeTransporte: number;
  salarioFamilia: number;
  decimoTerceiroProporcional: number;
  feriasComTerco: number;
  fatorR: number;
  recomendacao: string;
}
