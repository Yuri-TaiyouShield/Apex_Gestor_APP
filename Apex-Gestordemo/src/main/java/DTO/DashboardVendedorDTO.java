package DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DashboardVendedorDTO(
        BigDecimal meta,
        boolean metaConfigurada,
        BigDecimal vendidoHoje,
        BigDecimal vendidoMes,
        long vendasHoje,
        long vendasMes,
        long itensHoje,
        long itensMes,
        BigDecimal faltanteMeta,
        BigDecimal percentualMeta,
        BigDecimal ticketMedio,
        long aguardandoCaixa,
        long finalizadasCaixa,
        long recusadasCaixa,
        long canceladasOuDevolvidas,
        List<VendaResumoDTO> ultimasVendas) {
    public record VendaResumoDTO(Long idVenda, String numeroVenda, String status, BigDecimal total,
            LocalDateTime dataVenda) {
    }
}
