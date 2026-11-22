package com.Apex.Apex_Gestordemo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import DTO.VendaDevolucaoRequestDTO;
import Model.Cliente;
import Model.Usuario;
import Model.Venda;
import Model.VendaDevolucao;
import Repository.ProdutoRepository;
import Repository.VendaDevolucaoRepository;
import Repository.VendaRepository;
import Service.VendaService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class VendaServiceIdempotencyTests {

    private VendaService service;
    private VendaRepository vendaRepository;
    private VendaDevolucaoRepository devolucaoRepository;
    private ProdutoRepository produtoRepository;

    @BeforeEach
    void setUp() {
        service = new VendaService();
        vendaRepository = mock(VendaRepository.class);
        devolucaoRepository = mock(VendaDevolucaoRepository.class);
        produtoRepository = mock(ProdutoRepository.class);
        ReflectionTestUtils.setField(service, "vendaRepository", vendaRepository);
        ReflectionTestUtils.setField(service, "vendaDevolucaoRepository", devolucaoRepository);
        ReflectionTestUtils.setField(service, "produtoRepository", produtoRepository);
    }

    @Test
    void replayComMesmoPayloadRetornaDevolucaoPersistidaSemNovoEfeito() {
        Venda venda = vendaFinalizada(10L);
        VendaDevolucaoRequestDTO body = request();
        VendaDevolucao anterior = devolucaoPersistida(10L, body);
        when(vendaRepository.findByIdVendaAndEmpresaIdAndFilialId(10L, 1L, 2L))
                .thenReturn(Optional.of(venda));
        when(devolucaoRepository.findByIdempotencyKeyAndEmpresaIdAndFilialId("return-1", 1L, 2L))
                .thenReturn(Optional.of(anterior));

        VendaDevolucao resultado = service.registrarDevolucao(10L, body, 1L, 2L, null, "return-1");

        assertThat(resultado).isSameAs(anterior);
        verifyNoInteractions(produtoRepository);
    }

    @Test
    void replayComPayloadDiferenteRetornaConflito() {
        Venda venda = vendaFinalizada(10L);
        VendaDevolucaoRequestDTO body = request();
        VendaDevolucao anterior = devolucaoPersistida(10L,
                new VendaDevolucaoRequestDTO("TOTAL", "outro motivo", body.itens()));
        when(vendaRepository.findByIdVendaAndEmpresaIdAndFilialId(10L, 1L, 2L))
                .thenReturn(Optional.of(venda));
        when(devolucaoRepository.findByIdempotencyKeyAndEmpresaIdAndFilialId("return-1", 1L, 2L))
                .thenReturn(Optional.of(anterior));

        assertThatThrownBy(() -> service.registrarDevolucao(10L, body, 1L, 2L, null, "return-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Idempotency-Key");
        verifyNoInteractions(produtoRepository);
    }

    private Venda vendaFinalizada(Long id) {
        Venda venda = new Venda();
        venda.setIdVenda(id);
        venda.setStatus(1);
        venda.setCliente(new Cliente());
        venda.setUsuario(new Usuario());
        venda.setItens(new ArrayList<>());
        venda.setPagamentos(new ArrayList<>());
        return venda;
    }

    private VendaDevolucaoRequestDTO request() {
        return new VendaDevolucaoRequestDTO("PARCIAL", "cliente desistiu",
                List.of(new VendaDevolucaoRequestDTO.ItemDTO(7L, null, 1)));
    }

    private VendaDevolucao devolucaoPersistida(Long vendaId, VendaDevolucaoRequestDTO body) {
        VendaDevolucao devolucao = new VendaDevolucao();
        devolucao.setVendaOriginalId(vendaId);
        devolucao.setEmpresaId(1L);
        devolucao.setFilialId(2L);
        devolucao.setIdempotencyKey("return-1");
        String hash = ReflectionTestUtils.invokeMethod(service, "devolucaoPayloadHash", vendaId, body);
        devolucao.setIdempotencyPayloadHash(hash);
        devolucao.setItens(new ArrayList<>());
        return devolucao;
    }
}
