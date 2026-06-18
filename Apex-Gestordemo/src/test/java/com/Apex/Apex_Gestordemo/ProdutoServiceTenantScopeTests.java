package com.Apex.Apex_Gestordemo;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import Model.Categoria;
import Model.Fornecedor;
import Model.Produto;
import Repository.FornecedorRepository;
import Repository.ProdutoRepository;
import Service.AuditTrailService;
import Service.EstoqueMovimentacaoService;
import Service.ProdutoService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ProdutoServiceTenantScopeTests {

    private ProdutoRepository produtoRepository;
    private FornecedorRepository fornecedorRepository;
    private ProdutoService produtoService;

    @BeforeEach
    void setUp() {
        produtoRepository = mock(ProdutoRepository.class);
        fornecedorRepository = mock(FornecedorRepository.class);
        produtoService = new ProdutoService();
        ReflectionTestUtils.setField(produtoService, "produtoRepository", produtoRepository);
        ReflectionTestUtils.setField(produtoService, "fornecedorRepository", fornecedorRepository);
        ReflectionTestUtils.setField(produtoService, "auditTrailService", mock(AuditTrailService.class));
        ReflectionTestUtils.setField(produtoService, "estoqueMovimentacaoService", mock(EstoqueMovimentacaoService.class));
    }

    @Test
    void blocksSupplierOutsideCurrentTenantWhenSavingProduct() {
        Produto produto = produtoValido(10L);
        when(produtoRepository.findByCodigoBarrasAndEmpresaIdAndFilialId("7891000000999", 1L, 1L))
                .thenReturn(Optional.empty());
        when(fornecedorRepository.findByIdFornecedorAndEmpresaIdAndFilialId(10L, 1L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> produtoService.salvar(produto, 1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Fornecedor fora do escopo");
        verify(produtoRepository, never()).save(produto);
    }

    @Test
    void blocksSellableProductWithoutBarcode() {
        Produto produto = produtoValido(10L);
        produto.setCodigoBarras(" ");

        assertThatThrownBy(() -> produtoService.salvar(produto, 1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Codigo de barras");
        verify(produtoRepository, never()).save(produto);
    }

    @Test
    void blocksDuplicateBarcodeInsideTenant() {
        Produto produto = produtoValido(10L);
        Produto existente = new Produto();
        existente.setIdProduto(99L);
        existente.setCodigoBarras("7891000000999");
        when(produtoRepository.findByCodigoBarrasAndEmpresaIdAndFilialId("7891000000999", 1L, 1L))
                .thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> produtoService.salvar(produto, 1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ja cadastrado");
        verify(produtoRepository, never()).save(produto);
    }

    private Produto produtoValido(Long fornecedorId) {
        Produto produto = new Produto();
        produto.setDescricao("Produto tenant safe");
        produto.setCodigoBarras("7891000000999");
        produto.setCusto(BigDecimal.TEN);
        produto.setValorVenda(new BigDecimal("25.00"));
        produto.setQuantidadeEstoque(5);
        produto.setEstoqueMinimo(1);
        Categoria categoria = new Categoria();
        categoria.setIdCategoria(1L);
        produto.setCategoria(categoria);
        Fornecedor fornecedor = new Fornecedor();
        fornecedor.setIdFornecedor(fornecedorId);
        produto.setFornecedor(fornecedor);
        return produto;
    }
}
