package com.Apex.Apex_Gestordemo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import DTO.RecebimentoVendaRequestDTO;
import Model.*;
import Repository.*;
import Service.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class VendaServiceOperationalTests {
    @InjectMocks private VendaService service;
    @Mock private VendaRepository vendaRepository;
    @Mock private ProdutoRepository produtoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private FormaPagamentoRepository formaPagamentoRepository;
    @Mock private CondicaoPagamentoRepository condicaoPagamentoRepository;
    @Mock private VendaPagamentoRepository vendaPagamentoRepository;
    @Mock private VendaEventoRepository vendaEventoRepository;
    @Mock private VendaDevolucaoRepository vendaDevolucaoRepository;
    @Mock private EstoqueMovimentacaoService estoqueMovimentacaoService;
    @Mock private CaixaService caixaService;
    @Mock private FinanceiroOperacionalService financeiroOperacionalService;
    @Mock private CreditoClienteService creditoClienteService;
    @Mock private AuditTrailService auditTrailService;
    @Mock private PrivacyGuardService privacyGuardService;

    private Produto produto;
    private Cliente cliente;
    private Usuario vendedor;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        produto = new Produto();
        produto.setIdProduto(3L);
        produto.setDescricao("Produto testavel");
        produto.setCodigoBarras("7890000000001");
        produto.setStatus(1);
        produto.setQuantidadeEstoque(10);
        produto.setCusto(new BigDecimal("4.00"));
        produto.setValorVenda(new BigDecimal("10.00"));
        produto.setEmpresaId(1L);
        produto.setFilialId(2L);
        cliente = new Cliente(); cliente.setIdCliente(4L); cliente.setNomeRazao("Cliente");
        vendedor = new Usuario(); vendedor.setIdUsuario(5L); vendedor.setNome("Vendedor");
        request = new MockHttpServletRequest();
        when(privacyGuardService.currentUserLogin(any())).thenReturn("vendedor");
        when(vendaRepository.save(any(Venda.class))).thenAnswer(invocation -> {
            Venda venda = invocation.getArgument(0);
            if (venda.getIdVenda() == null) venda.setIdVenda(11L);
            return venda;
        });
    }

    @Test
    void vendedorEnviaAoCaixaSemBaixarEstoque() {
        when(clienteRepository.findByIdClienteAndEmpresaIdAndFilialId(4L, 1L, 2L)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByIdUsuarioAndEmpresaIdAndFilialId(5L, 1L, 2L)).thenReturn(Optional.of(vendedor));
        when(produtoRepository.findByIdProdutoAndEmpresaIdAndFilialId(3L, 1L, 2L)).thenReturn(Optional.of(produto));
        Venda venda = vendaParaTeste();

        Venda criada = service.realizarVenda(venda, 1L, 2L, request);

        assertThat(criada.getStatusOperacional()).isEqualTo(VendaStatus.AGUARDANDO_CAIXA);
        assertThat(criada.getNumeroVenda()).isEqualTo("V-2-00000011");
        assertThat(criada.getValorTotal()).isEqualByComparingTo("20.00");
        assertThat(produto.getQuantidadeEstoque()).isEqualTo(10);
        verify(estoqueMovimentacaoService, never()).registrar(any(), any(), anyInt(), anyInt(), anyInt(), any(), any(), any());
        verify(vendaEventoRepository).save(argThat(e -> "ENVIADA_CAIXA".equals(e.getEvento())));
    }

    @Test
    void caixaFinalizaPagamentoBaixaEstoqueEGeraFinanceiro() {
        Venda venda = vendaParaTeste();
        venda.setIdVenda(11L); venda.setEmpresaId(1L); venda.setFilialId(2L);
        venda.setCliente(cliente); venda.setUsuario(vendedor); venda.setValorTotal(new BigDecimal("20.00"));
        venda.setStatusOperacional(VendaStatus.AGUARDANDO_CAIXA); venda.setPagamentos(new ArrayList<>());
        venda.getItens().get(0).setVenda(venda); venda.getItens().get(0).setProduto(produto);
        when(vendaRepository.findByIdVendaAndEmpresaIdAndFilialId(11L, 1L, 2L)).thenReturn(Optional.of(venda));
        when(produtoRepository.findByIdProdutoAndEmpresaIdAndFilialId(3L, 1L, 2L)).thenReturn(Optional.of(produto));
        CaixaSessao caixa = new CaixaSessao(); caixa.setIdCaixaSessao(8L); caixa.setStatus(CaixaStatus.ABERTO);
        when(caixaService.caixaAberto(request)).thenReturn(Optional.of(caixa));
        FormaPagamento forma = new FormaPagamento(); forma.setIdFormaPagamento(9L); forma.setNome("Dinheiro");
        when(formaPagamentoRepository.findVisibleById(9L, 1L, 2L)).thenReturn(Optional.of(forma));

        Venda finalizada = service.receberNoCaixa(11L,
                new RecebimentoVendaRequestDTO(9L, new BigDecimal("25.00"), 1), 1L, 2L, request);

        assertThat(finalizada.getStatusOperacional()).isEqualTo(VendaStatus.FINALIZADA);
        assertThat(finalizada.getTroco()).isEqualByComparingTo("5.00");
        assertThat(finalizada.isEstoqueMovimentado()).isTrue();
        assertThat(produto.getQuantidadeEstoque()).isEqualTo(8);
        verify(caixaService).registrarVenda(new BigDecimal("20.00"), 11L, request);
        verify(financeiroOperacionalService).registrarVendaFinalizada(venda, request);
        assertThat(finalizada.getPagamentos()).hasSize(1);
    }

    @Test
    void caixaRecusaComMotivoEDevolveParaAtendimento() {
        Venda venda = vendaParaTeste(); venda.setIdVenda(11L); venda.setEmpresaId(1L); venda.setFilialId(2L);
        venda.setStatusOperacional(VendaStatus.AGUARDANDO_CAIXA);
        when(vendaRepository.findByIdVendaAndEmpresaIdAndFilialId(11L, 1L, 2L)).thenReturn(Optional.of(venda));

        Venda recusada = service.recusarNoCaixa(11L, "Preço divergente", 1L, 2L, request);

        assertThat(recusada.getStatusOperacional()).isEqualTo(VendaStatus.EM_ATENDIMENTO);
        assertThat(recusada.getMotivoRecusa()).isEqualTo("Preço divergente");
        verify(vendaEventoRepository).save(argThat(e -> "RECUSADA_CAIXA".equals(e.getEvento())));
    }

    private Venda vendaParaTeste() {
        Venda venda = new Venda(); venda.setCliente(cliente); venda.setUsuario(vendedor); venda.setDesconto(BigDecimal.ZERO);
        Produto referencia = new Produto(); referencia.setIdProduto(3L);
        ProdutoVenda item = new ProdutoVenda(); item.setProduto(referencia); item.setQuantidade(2);
        venda.setItens(new ArrayList<>(List.of(item))); venda.setPagamentos(new ArrayList<>());
        return venda;
    }
}
