package DTO;

import Model.OrigemClassificacaoFinanceira;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ItemPendenteClassificacaoDTO(
        OrigemClassificacaoFinanceira origemTipo,
        Long registroId,
        String descricao,
        BigDecimal valor,
        LocalDate dataCompetencia,
        Long sugestaoPlanoContaId,
        String sugestaoPlanoContaNome,
        Long sugestaoCentroCustoId) {
}
