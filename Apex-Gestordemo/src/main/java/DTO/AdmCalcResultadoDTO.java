package DTO;

import java.math.BigDecimal;

public class AdmCalcResultadoDTO {

    private BigDecimal salarioHora;
    private BigDecimal salarioDiario;
    private BigDecimal valorHorasExtras;
    private BigDecimal horasNoturnasReduzidas;
    private BigDecimal adicionalPericulosidade;
    private BigDecimal adicionalInsalubridade;
    private BigDecimal descontoValeTransporte;
    private BigDecimal salarioFamilia;
    private BigDecimal decimoTerceiroProporcional;
    private BigDecimal feriasComTerco;
    private BigDecimal fatorR;
    private String recomendacao;

    public BigDecimal getSalarioHora() {
        return salarioHora;
    }

    public void setSalarioHora(BigDecimal salarioHora) {
        this.salarioHora = salarioHora;
    }

    public BigDecimal getSalarioDiario() {
        return salarioDiario;
    }

    public void setSalarioDiario(BigDecimal salarioDiario) {
        this.salarioDiario = salarioDiario;
    }

    public BigDecimal getValorHorasExtras() {
        return valorHorasExtras;
    }

    public void setValorHorasExtras(BigDecimal valorHorasExtras) {
        this.valorHorasExtras = valorHorasExtras;
    }

    public BigDecimal getHorasNoturnasReduzidas() {
        return horasNoturnasReduzidas;
    }

    public void setHorasNoturnasReduzidas(BigDecimal horasNoturnasReduzidas) {
        this.horasNoturnasReduzidas = horasNoturnasReduzidas;
    }

    public BigDecimal getAdicionalPericulosidade() {
        return adicionalPericulosidade;
    }

    public void setAdicionalPericulosidade(BigDecimal adicionalPericulosidade) {
        this.adicionalPericulosidade = adicionalPericulosidade;
    }

    public BigDecimal getAdicionalInsalubridade() {
        return adicionalInsalubridade;
    }

    public void setAdicionalInsalubridade(BigDecimal adicionalInsalubridade) {
        this.adicionalInsalubridade = adicionalInsalubridade;
    }

    public BigDecimal getDescontoValeTransporte() {
        return descontoValeTransporte;
    }

    public void setDescontoValeTransporte(BigDecimal descontoValeTransporte) {
        this.descontoValeTransporte = descontoValeTransporte;
    }

    public BigDecimal getSalarioFamilia() {
        return salarioFamilia;
    }

    public void setSalarioFamilia(BigDecimal salarioFamilia) {
        this.salarioFamilia = salarioFamilia;
    }

    public BigDecimal getDecimoTerceiroProporcional() {
        return decimoTerceiroProporcional;
    }

    public void setDecimoTerceiroProporcional(BigDecimal decimoTerceiroProporcional) {
        this.decimoTerceiroProporcional = decimoTerceiroProporcional;
    }

    public BigDecimal getFeriasComTerco() {
        return feriasComTerco;
    }

    public void setFeriasComTerco(BigDecimal feriasComTerco) {
        this.feriasComTerco = feriasComTerco;
    }

    public BigDecimal getFatorR() {
        return fatorR;
    }

    public void setFatorR(BigDecimal fatorR) {
        this.fatorR = fatorR;
    }

    public String getRecomendacao() {
        return recomendacao;
    }

    public void setRecomendacao(String recomendacao) {
        this.recomendacao = recomendacao;
    }
}
