package DTO;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ParametroInteligenciaAdministrativaDTO(
        Long idParametroInteligencia,
        Long empresaId,
        Long filialId,
        String segmentoNegocio,
        @DecimalMin("0.00") BigDecimal retornoMinimoMensalPercentual,
        @DecimalMin("0.00") BigDecimal capitalInvestidoReferencia,
        @DecimalMin("0.00") BigDecimal taxaDescontoAnualPercentual,
        @DecimalMin("0.00") BigDecimal margemContribuicaoMinimaPercentual,
        @DecimalMin("0.00") BigDecimal margemLiquidaMinimaPercentual,
        @DecimalMin("0.00") BigDecimal markupMargemDesejadaPadrao,
        @DecimalMin("0.00") BigDecimal aliquotaTributosVendaPadrao,
        @DecimalMin("0.00") BigDecimal taxaCartaoMediaPadrao,
        @DecimalMin("0.00") BigDecimal comissaoVendaPadrao,
        @DecimalMin("0.00") BigDecimal freteVariavelVendaPadrao,
        @DecimalMin("0.00") BigDecimal cicloFinanceiroMaximoDias,
        @DecimalMin("0.00") BigDecimal pmeMaximoDias,
        @DecimalMin("0.00") BigDecimal liquidezCorrenteMinima,
        @DecimalMin("0.00") BigDecimal liquidezSecaMinima,
        @DecimalMin("0.00") BigDecimal liquidezImediataMinima,
        @DecimalMin("0.00") BigDecimal endividamentoGeralMaximoPercentual,
        String atualizadoPor,
        LocalDateTime atualizadoEm) {
}
