package DTO;

import java.math.BigDecimal;

public class AdmCalcRequestDTO {

    private BigDecimal salarioMensal = BigDecimal.ZERO;
    private BigDecimal jornadaMensal = new BigDecimal("220");
    private BigDecimal percentualHoraExtra = new BigDecimal("0.50");
    private BigDecimal horasExtras = BigDecimal.ZERO;
    private BigDecimal horasNoturnasRelogio = BigDecimal.ZERO;
    private boolean adicionalPericulosidade;
    private BigDecimal salarioMinimo = new BigDecimal("1412.00");
    private BigDecimal percentualInsalubridade = BigDecimal.ZERO;
    private BigDecimal custoPassagens = BigDecimal.ZERO;
    private Integer filhosElegiveis = 0;
    private BigDecimal cotaSalarioFamilia = BigDecimal.ZERO;
    private Integer mesesTrabalhados = 12;
    private BigDecimal receitaBruta = BigDecimal.ZERO;
    private BigDecimal folhaSalarios = BigDecimal.ZERO;
    private BigDecimal proLabore = BigDecimal.ZERO;

    public BigDecimal getSalarioMensal() {
        return salarioMensal;
    }

    public void setSalarioMensal(BigDecimal salarioMensal) {
        this.salarioMensal = salarioMensal;
    }

    public BigDecimal getJornadaMensal() {
        return jornadaMensal;
    }

    public void setJornadaMensal(BigDecimal jornadaMensal) {
        this.jornadaMensal = jornadaMensal;
    }

    public BigDecimal getPercentualHoraExtra() {
        return percentualHoraExtra;
    }

    public void setPercentualHoraExtra(BigDecimal percentualHoraExtra) {
        this.percentualHoraExtra = percentualHoraExtra;
    }

    public BigDecimal getHorasExtras() {
        return horasExtras;
    }

    public void setHorasExtras(BigDecimal horasExtras) {
        this.horasExtras = horasExtras;
    }

    public BigDecimal getHorasNoturnasRelogio() {
        return horasNoturnasRelogio;
    }

    public void setHorasNoturnasRelogio(BigDecimal horasNoturnasRelogio) {
        this.horasNoturnasRelogio = horasNoturnasRelogio;
    }

    public boolean isAdicionalPericulosidade() {
        return adicionalPericulosidade;
    }

    public void setAdicionalPericulosidade(boolean adicionalPericulosidade) {
        this.adicionalPericulosidade = adicionalPericulosidade;
    }

    public BigDecimal getSalarioMinimo() {
        return salarioMinimo;
    }

    public void setSalarioMinimo(BigDecimal salarioMinimo) {
        this.salarioMinimo = salarioMinimo;
    }

    public BigDecimal getPercentualInsalubridade() {
        return percentualInsalubridade;
    }

    public void setPercentualInsalubridade(BigDecimal percentualInsalubridade) {
        this.percentualInsalubridade = percentualInsalubridade;
    }

    public BigDecimal getCustoPassagens() {
        return custoPassagens;
    }

    public void setCustoPassagens(BigDecimal custoPassagens) {
        this.custoPassagens = custoPassagens;
    }

    public Integer getFilhosElegiveis() {
        return filhosElegiveis;
    }

    public void setFilhosElegiveis(Integer filhosElegiveis) {
        this.filhosElegiveis = filhosElegiveis;
    }

    public BigDecimal getCotaSalarioFamilia() {
        return cotaSalarioFamilia;
    }

    public void setCotaSalarioFamilia(BigDecimal cotaSalarioFamilia) {
        this.cotaSalarioFamilia = cotaSalarioFamilia;
    }

    public Integer getMesesTrabalhados() {
        return mesesTrabalhados;
    }

    public void setMesesTrabalhados(Integer mesesTrabalhados) {
        this.mesesTrabalhados = mesesTrabalhados;
    }

    public BigDecimal getReceitaBruta() {
        return receitaBruta;
    }

    public void setReceitaBruta(BigDecimal receitaBruta) {
        this.receitaBruta = receitaBruta;
    }

    public BigDecimal getFolhaSalarios() {
        return folhaSalarios;
    }

    public void setFolhaSalarios(BigDecimal folhaSalarios) {
        this.folhaSalarios = folhaSalarios;
    }

    public BigDecimal getProLabore() {
        return proLabore;
    }

    public void setProLabore(BigDecimal proLabore) {
        this.proLabore = proLabore;
    }
}
