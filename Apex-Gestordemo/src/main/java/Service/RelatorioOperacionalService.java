package Service;

import DTO.RelatorioOperacionalDTO;
import Model.Cliente;
import Model.Fornecedor;
import Model.Produto;
import Model.Usuario;
import Model.Venda;
import Repository.ClienteRepository;
import Repository.FornecedorRepository;
import Repository.ProdutoRepository;
import Repository.UsuarioRepository;
import Repository.VendaRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RelatorioOperacionalService {

    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ProdutoRepository produtoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private VendaRepository vendaRepository;
    @Autowired
    private FornecedorRepository fornecedorRepository;

    public RelatorioOperacionalDTO clientes(Long empresaId, Long filialId) {
        List<Cliente> clientes = clienteRepository.findByEmpresaIdAndFilialIdAndDeletedAtIsNullOrderByNomeRazaoAsc(empresaId, filialId);
        return simples("Clientes", clientes.size(), contarAtivosClientes(clientes), List.of());
    }

    public RelatorioOperacionalDTO estoque(Long empresaId, Long filialId) {
        List<Produto> produtos = produtoRepository.findByEmpresaIdAndFilialIdOrderByDescricaoAsc(empresaId, filialId);
        long ativos = produtos.stream().filter((produto) -> produto.getStatus() == 1).count();
        long estoqueBaixo = produtos.stream()
                .filter((produto) -> produto.getQuantidadeEstoque() <= produto.getEstoqueMinimo())
                .count();
        BigDecimal valorEstoque = produtos.stream()
                .map((produto) -> produto.getCusto().multiply(BigDecimal.valueOf(produto.getQuantidadeEstoque())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<String> alertas = new ArrayList<>();
        if (estoqueBaixo > 0) {
            alertas.add(estoqueBaixo + " produtos abaixo do estoque minimo");
        }
        return new RelatorioOperacionalDTO("Estoque", produtos.size(), ativos, produtos.size() - ativos, valorEstoque, alertas);
    }

    public RelatorioOperacionalDTO funcionarios(Long empresaId, Long filialId) {
        List<Usuario> usuarios = usuarioRepository.findByEmpresaIdAndFilialIdOrderByNomeAsc(empresaId, filialId);
        long ativos = usuarios.stream().filter((usuario) -> usuario.getStatus() == 1).count();
        return new RelatorioOperacionalDTO("Funcionarios", usuarios.size(), ativos, usuarios.size() - ativos, BigDecimal.ZERO, List.of());
    }

    public RelatorioOperacionalDTO vendas(Long empresaId, Long filialId) {
        List<Venda> vendas = vendaRepository.findByEmpresaIdAndFilialIdOrderByDataVendaDesc(empresaId, filialId);
        long ativas = vendas.stream().filter((venda) -> venda.getStatus() == 1).count();
        BigDecimal total = vendas.stream()
                .filter((venda) -> venda.getStatus() == 1)
                .map(Venda::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new RelatorioOperacionalDTO("Vendas", vendas.size(), ativas, vendas.size() - ativas, total, List.of());
    }

    public RelatorioOperacionalDTO fornecedores(Long empresaId, Long filialId) {
        List<Fornecedor> fornecedores = fornecedorRepository.findByEmpresaIdAndFilialIdOrderByRazaoSocialAsc(empresaId, filialId);
        long ativos = fornecedores.stream().filter((fornecedor) -> fornecedor.getStatus() == 1).count();
        return new RelatorioOperacionalDTO("Fornecedores", fornecedores.size(), ativos, fornecedores.size() - ativos, BigDecimal.ZERO, List.of());
    }

    private RelatorioOperacionalDTO simples(String tipo, long total, long ativos, List<String> alertas) {
        return new RelatorioOperacionalDTO(tipo, total, ativos, total - ativos, BigDecimal.ZERO, alertas);
    }

    private long contarAtivosClientes(List<Cliente> clientes) {
        return clientes.stream().filter((cliente) -> cliente.getStatus() == 1).count();
    }
}
