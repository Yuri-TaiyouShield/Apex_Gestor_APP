package com.Apex.Apex_Gestordemo;

import Model.Categoria;
import Model.Cliente;
import Model.Perfil;
import Model.Produto;
import Model.ProdutoVenda;
import Model.Usuario;
import Model.Venda;
import Repository.CategoriaRepository;
import Repository.ClienteRepository;
import Repository.PerfilRepository;
import Repository.ProdutoRepository;
import Repository.UsuarioRepository;
import Repository.VendaRepository;
import Service.VendaService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PdvVendaEstoqueEnversIntegrationTests {

    @Autowired
    private VendaService vendaService;
    @Autowired
    private ProdutoRepository produtoRepository;
    @Autowired
    private VendaRepository vendaRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PerfilRepository perfilRepository;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void pdvSaleDebitsStockAndCreatesForensicEnversRevisions() {
        Perfil perfil = new Perfil();
        perfil.setNome("vendedor-e2e");
        perfil.setStatus(1);
        perfil = perfilRepository.save(perfil);

        Usuario usuario = new Usuario();
        usuario.setNome("Operador PDV");
        usuario.setLogin("operador-pdv-e2e");
        usuario.setSenha("$2a$12$e2e");
        usuario.setDataNascimento(LocalDate.of(1990, 1, 1));
        usuario.setStatus(1);
        usuario.setPerfil(perfil);
        usuario = usuarioRepository.save(usuario);

        Cliente cliente = new Cliente();
        cliente.setNomeRazao("Cliente E2E");
        cliente.setTelefone("(11)99999-0000");
        cliente.setTipoDocumento(1);
        cliente.setCpfCnpj("00000000000191");
        cliente.setDataCadastro(LocalDateTime.now());
        cliente.setStatus(1);
        cliente = clienteRepository.save(cliente);

        Categoria categoria = new Categoria();
        categoria.setDescricao("E2E");
        categoria.setStatus(1);
        categoria = categoriaRepository.save(categoria);

        Produto produto = new Produto();
        produto.setDescricao("Produto auditado E2E");
        produto.setCusto(BigDecimal.valueOf(10));
        produto.setValorVenda(BigDecimal.valueOf(25));
        produto.setQuantidadeEstoque(10);
        produto.setEstoqueMinimo(1);
        produto.setStatus(1);
        produto.setCategoria(categoria);
        produto = produtoRepository.save(produto);

        ProdutoVenda item = new ProdutoVenda();
        Produto produtoRef = new Produto();
        produtoRef.setIdProduto(produto.getIdProduto());
        item.setProduto(produtoRef);
        item.setQuantidade(3);

        Venda venda = new Venda();
        venda.setUsuario(usuario);
        venda.setCliente(cliente);
        venda.setDesconto(BigDecimal.ZERO);
        venda.setItens(List.of(item));

        Venda vendaSalva = vendaService.realizarVenda(venda);

        Produto produtoAtualizado = produtoRepository.findById(produto.getIdProduto()).orElseThrow();
        assertThat(produtoAtualizado.getQuantidadeEstoque()).isEqualTo(7);
        assertThat(vendaRepository.findById(vendaSalva.getIdVenda())).isPresent();

        EntityManager auditEntityManager = entityManagerFactory.createEntityManager();
        try {
            assertThat(AuditReaderFactory.get(auditEntityManager).getRevisions(Produto.class, produto.getIdProduto()))
                    .isNotEmpty();
            assertThat(AuditReaderFactory.get(auditEntityManager).getRevisions(Venda.class, vendaSalva.getIdVenda()))
                    .isNotEmpty();
        } finally {
            auditEntityManager.close();
        }
    }
}
