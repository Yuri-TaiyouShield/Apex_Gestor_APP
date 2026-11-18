package DTO;

import java.util.List;

public record PendenciaClassificacaoResponseDTO(
        long totalPendentes,
        long despesasPendentes,
        long contasPagarPendentes,
        long contasReceberPendentes,
        long vendasPendentes,
        List<ItemPendenteClassificacaoDTO> itens) {
}
