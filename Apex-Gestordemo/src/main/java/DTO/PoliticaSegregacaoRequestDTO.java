package DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PoliticaSegregacaoRequestDTO(
        @NotBlank @Size(max = 48) String dominioOperacao,
        Boolean exigirAprovadorDistinto,
        Boolean permitirExcecaoUnipessoal,
        Boolean exigirJustificativa,
        @DecimalMin("0.00") BigDecimal alcadaGerenteMaximo
) {
}
