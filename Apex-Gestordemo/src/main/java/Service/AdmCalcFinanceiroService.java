package Service;

import DTO.AdmCalcRequestDTO;
import DTO.AdmCalcResultadoDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class AdmCalcFinanceiroService {

    private static final BigDecimal TRINTA = new BigDecimal("30");
    private static final BigDecimal UM = BigDecimal.ONE;
    private static final BigDecimal FATOR_NOTURNO = new BigDecimal("1.142857");
    private static final BigDecimal PERCENTUAL_PERICULOSIDADE = new BigDecimal("0.30");
    private static final BigDecimal LIMITE_VALE_TRANSPORTE = new BigDecimal("0.06");
    private static final BigDecimal TERCO_FERIAS = new BigDecimal("1.3333");

    public AdmCalcResultadoDTO calcular(AdmCalcRequestDTO request) {
        BigDecimal salario = positivo(request.getSalarioMensal());
        BigDecimal jornada = positivoOuPadrao(request.getJornadaMensal(), new BigDecimal("220"));
        BigDecimal percentualHoraExtra = positivoOuPadrao(request.getPercentualHoraExtra(), new BigDecimal("0.50"));
        BigDecimal horasExtras = positivo(request.getHorasExtras());

        BigDecimal salarioHora = dividir(salario, jornada);
        BigDecimal salarioDiario = dividir(salario, TRINTA);
        BigDecimal valorHorasExtras = salarioHora
                .multiply(UM.add(percentualHoraExtra))
                .multiply(horasExtras);
        BigDecimal horasNoturnasReduzidas = positivo(request.getHorasNoturnasRelogio()).multiply(FATOR_NOTURNO);
        BigDecimal adicionalPericulosidade = request.isAdicionalPericulosidade()
                ? salario.multiply(PERCENTUAL_PERICULOSIDADE)
                : BigDecimal.ZERO;
        BigDecimal adicionalInsalubridade = positivoOuPadrao(request.getSalarioMinimo(), new BigDecimal("1412.00"))
                .multiply(positivo(request.getPercentualInsalubridade()));
        BigDecimal descontoValeTransporte = positivo(request.getCustoPassagens())
                .min(salario.multiply(LIMITE_VALE_TRANSPORTE));
        BigDecimal salarioFamilia = positivo(request.getCotaSalarioFamilia())
                .multiply(BigDecimal.valueOf(Math.max(0, nuloZero(request.getFilhosElegiveis()))));
        BigDecimal baseAnual = salario.add(adicionalPericulosidade).add(adicionalInsalubridade);
        BigDecimal meses = BigDecimal.valueOf(Math.max(0, Math.min(12, nuloZero(request.getMesesTrabalhados()))));
        BigDecimal decimoTerceiro = dividir(baseAnual, new BigDecimal("12")).multiply(meses);
        BigDecimal feriasComTerco = baseAnual.multiply(TERCO_FERIAS);
        BigDecimal fatorR = calcularFatorR(request);

        AdmCalcResultadoDTO resultado = new AdmCalcResultadoDTO();
        resultado.setSalarioHora(escala(salarioHora));
        resultado.setSalarioDiario(escala(salarioDiario));
        resultado.setValorHorasExtras(escala(valorHorasExtras));
        resultado.setHorasNoturnasReduzidas(escala(horasNoturnasReduzidas));
        resultado.setAdicionalPericulosidade(escala(adicionalPericulosidade));
        resultado.setAdicionalInsalubridade(escala(adicionalInsalubridade));
        resultado.setDescontoValeTransporte(escala(descontoValeTransporte));
        resultado.setSalarioFamilia(escala(salarioFamilia));
        resultado.setDecimoTerceiroProporcional(escala(decimoTerceiro));
        resultado.setFeriasComTerco(escala(feriasComTerco));
        resultado.setFatorR(escala(fatorR));
        resultado.setRecomendacao(fatorR.compareTo(new BigDecimal("0.28")) >= 0
                ? "Fator R igual ou superior a 28%. Avaliar enquadramento tributario mais vantajoso."
                : "Fator R abaixo de 28%. Revisar folha, pro-labore e margem antes de precificar.");
        return resultado;
    }

    private BigDecimal calcularFatorR(AdmCalcRequestDTO request) {
        BigDecimal receita = positivo(request.getReceitaBruta());
        if (receita.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return dividir(positivo(request.getFolhaSalarios()).add(positivo(request.getProLabore())), receita);
    }

    private BigDecimal dividir(BigDecimal valor, BigDecimal divisor) {
        if (divisor == null || divisor.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return valor.divide(divisor, 8, RoundingMode.HALF_UP);
    }

    private BigDecimal positivo(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return valor;
    }

    private BigDecimal positivoOuPadrao(BigDecimal valor, BigDecimal padrao) {
        BigDecimal seguro = positivo(valor);
        return seguro.compareTo(BigDecimal.ZERO) == 0 ? padrao : seguro;
    }

    private Integer nuloZero(Integer valor) {
        return valor == null ? 0 : valor;
    }

    private BigDecimal escala(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
