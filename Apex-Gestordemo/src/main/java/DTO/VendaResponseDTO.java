package DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VendaResponseDTO(
        Long idVenda,
        Long clienteId,
        String clienteNome,
        Long usuarioId,
        String usuarioNome,
        Integer status,
        BigDecimal valorTotal,
        BigDecimal desconto,
        String observacao,
        Long empresaId,
        Long filialId,
        Long caixaSessaoId,
        Long condicaoPagamentoId,
        LocalDateTime dataVenda,
        List<ItemDTO> itens,
        List<PagamentoDTO> pagamentos) {

    public record ItemDTO(Long produtoId, String descricao, Integer quantidade, BigDecimal precoUnitario,
            BigDecimal precoTotal) {
    }

    public record PagamentoDTO(Long formaPagamentoId, String nome, BigDecimal valorPago, Integer numeroParcelas) {
    }
}
