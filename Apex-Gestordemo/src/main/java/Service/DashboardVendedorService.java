package Service;

import DTO.DashboardVendedorDTO;
import Model.ProdutoVenda;
import Model.Venda;
import Model.VendaStatus;
import Repository.MetaVendedorRepository;
import Repository.VendaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardVendedorService {
    private static final Set<VendaStatus> CONCLUIDAS = Set.of(
            VendaStatus.FINALIZADA, VendaStatus.TROCA_PARCIAL, VendaStatus.TROCA_TOTAL);

    @Autowired private VendaRepository vendaRepository;
    @Autowired private MetaVendedorRepository metaVendedorRepository;

    @Transactional(readOnly = true)
    public DashboardVendedorDTO calcular(Long usuarioId, Long empresaId, Long filialId, boolean verCanceladas) {
        LocalDate hoje = LocalDate.now();
        YearMonth mes = YearMonth.from(hoje);
        LocalDateTime inicioMes = mes.atDay(1).atStartOfDay();
        LocalDateTime fimMes = mes.atEndOfMonth().atTime(LocalTime.MAX);
        List<Venda> vendas = vendaRepository
                .findByUsuarioIdUsuarioAndEmpresaIdAndFilialIdAndDataVendaBetweenOrderByDataVendaDesc(
                        usuarioId, empresaId, filialId, inicioMes, fimMes);
        vendas.forEach(v -> Hibernate.initialize(v.getItens()));
        List<Venda> concluidasMes = vendas.stream().filter(v -> CONCLUIDAS.contains(status(v))).toList();
        List<Venda> concluidasHoje = concluidasMes.stream()
                .filter(v -> v.getFinalizadoEm() != null && v.getFinalizadoEm().toLocalDate().equals(hoje)).toList();
        BigDecimal vendidoMes = soma(concluidasMes);
        BigDecimal vendidoHoje = soma(concluidasHoje);
        long itensMes = itens(concluidasMes);
        long itensHoje = itens(concluidasHoje);
        var metaOpt = metaVendedorRepository
                .findFirstByUsuarioIdAndEmpresaIdAndFilialIdAndPeriodoInicioLessThanEqualAndPeriodoFimGreaterThanEqualOrderByPeriodoInicioDesc(
                        usuarioId, empresaId, filialId, hoje, hoje);
        BigDecimal meta = metaOpt.map(Model.MetaVendedor::getValorMeta).orElse(BigDecimal.ZERO);
        BigDecimal faltante = meta.subtract(vendidoMes).max(BigDecimal.ZERO);
        BigDecimal percentual = meta.signum() == 0 ? BigDecimal.ZERO
                : vendidoMes.multiply(BigDecimal.valueOf(100)).divide(meta, 2, RoundingMode.HALF_UP);
        BigDecimal ticket = concluidasMes.isEmpty() ? BigDecimal.ZERO
                : vendidoMes.divide(BigDecimal.valueOf(concluidasMes.size()), 2, RoundingMode.HALF_UP);
        long aguardando = vendas.stream().filter(v -> status(v) == VendaStatus.AGUARDANDO_CAIXA).count();
        long recusadas = vendas.stream().filter(v -> status(v) == VendaStatus.EM_ATENDIMENTO
                && v.getMotivoRecusa() != null).count();
        long canceladas = verCanceladas
                ? vendas.stream().filter(v -> Set.of(VendaStatus.CANCELADA, VendaStatus.DEVOLVIDA).contains(status(v))).count()
                : 0;
        List<DashboardVendedorDTO.VendaResumoDTO> ultimas = vendaRepository
                .findTop10ByUsuarioIdUsuarioAndEmpresaIdAndFilialIdOrderByDataVendaDesc(usuarioId, empresaId, filialId)
                .stream().map(v -> new DashboardVendedorDTO.VendaResumoDTO(
                        v.getIdVenda(), v.getNumeroVenda(), status(v).name(), v.getValorTotal(), v.getDataVenda())).toList();
        return new DashboardVendedorDTO(meta, metaOpt.isPresent(), vendidoHoje, vendidoMes,
                concluidasHoje.size(), concluidasMes.size(), itensHoje, itensMes, faltante, percentual, ticket,
                aguardando, concluidasMes.size(), recusadas, canceladas, ultimas);
    }

    private BigDecimal soma(List<Venda> vendas) {
        return vendas.stream().map(Venda::getValorTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long itens(List<Venda> vendas) {
        return vendas.stream().flatMap(v -> v.getItens().stream()).mapToLong(ProdutoVenda::getQuantidade).sum();
    }

    private VendaStatus status(Venda venda) {
        return venda.getStatusOperacional() == null
                ? (venda.getStatus() == 0 ? VendaStatus.CANCELADA : VendaStatus.FINALIZADA)
                : venda.getStatusOperacional();
    }
}
