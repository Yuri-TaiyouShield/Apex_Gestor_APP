package com.Apex.Apex_Gestordemo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import Model.*;
import Repository.MetaVendedorRepository;
import Repository.VendaRepository;
import Service.DashboardVendedorService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardVendedorServiceTests {
    @InjectMocks private DashboardVendedorService service;
    @Mock private VendaRepository vendaRepository;
    @Mock private MetaVendedorRepository metaRepository;

    @Test
    void calculaMetaFaltanteSomenteComVendasDoVendedorConsultado() {
        Venda venda = new Venda(); venda.setIdVenda(1L); venda.setNumeroVenda("V-1-1");
        venda.setValorTotal(new BigDecimal("300.00")); venda.setStatusOperacional(VendaStatus.FINALIZADA);
        venda.setDataVenda(LocalDateTime.now()); venda.setFinalizadoEm(LocalDateTime.now());
        ProdutoVenda item = new ProdutoVenda(); item.setQuantidade(3); venda.setItens(List.of(item));
        when(vendaRepository.findByUsuarioIdUsuarioAndEmpresaIdAndFilialIdAndDataVendaBetweenOrderByDataVendaDesc(
                eq(7L), eq(1L), eq(2L), any(), any())).thenReturn(List.of(venda));
        when(vendaRepository.findTop10ByUsuarioIdUsuarioAndEmpresaIdAndFilialIdOrderByDataVendaDesc(7L, 1L, 2L))
                .thenReturn(List.of(venda));
        MetaVendedor meta = new MetaVendedor(); meta.setValorMeta(new BigDecimal("1000.00"));
        when(metaRepository.findFirstByUsuarioIdAndEmpresaIdAndFilialIdAndPeriodoInicioLessThanEqualAndPeriodoFimGreaterThanEqualOrderByPeriodoInicioDesc(
                eq(7L), eq(1L), eq(2L), any(LocalDate.class), any(LocalDate.class))).thenReturn(Optional.of(meta));

        var dashboard = service.calcular(7L, 1L, 2L, false);

        assertThat(dashboard.vendidoMes()).isEqualByComparingTo("300.00");
        assertThat(dashboard.faltanteMeta()).isEqualByComparingTo("700.00");
        assertThat(dashboard.percentualMeta()).isEqualByComparingTo("30.00");
        assertThat(dashboard.itensMes()).isEqualTo(3);
        assertThat(dashboard.ultimasVendas()).hasSize(1);
    }
}
