package DTO;

import Model.OrigemClassificacaoFinanceira;
import java.time.LocalDateTime;

public record RegraClassificacaoResponseDTO(
        Long idRegraClassificacao,
        String nome,
        OrigemClassificacaoFinanceira origemTipo,
        Long tipoDespesaId,
        Long categoriaId,
        Long formaPagamentoId,
        Long fornecedorId,
        String palavraChave,
        Long planoContaId,
        String planoContaCodigo,
        String planoContaNome,
        Long centroCustoId,
        String centroCustoCodigo,
        String centroCustoNome,
        Integer prioridade,
        boolean ativo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {
}
