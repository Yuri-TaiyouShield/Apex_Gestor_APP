package Service;

import Model.Cliente;
import Repository.ClienteRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    public List<Cliente> listarTodos(Long empresaId, Long filialId) {
        return clienteRepository.findByEmpresaIdAndFilialIdAndDeletedAtIsNullOrderByNomeRazaoAsc(empresaId, filialId);
    }

    public Optional<Cliente> buscarPorId(Long id) {
        return clienteRepository.findById(id);
    }

    public Optional<Cliente> buscarPorId(Long id, Long empresaId, Long filialId) {
        return clienteRepository.findByIdClienteAndEmpresaIdAndFilialId(id, empresaId, filialId);
    }

    public Cliente salvar(Cliente cliente) {
        return salvar(cliente, cliente.getEmpresaId(), cliente.getFilialId());
    }

    public Cliente salvar(Cliente cliente, Long empresaId, Long filialId) {
        Long empresaAtual = empresaId == null ? 1L : empresaId;
        Long filialAtual = filialId == null ? 1L : filialId;
        if (cliente.getNomeRazao() == null || cliente.getNomeRazao().isBlank()) {
            throw new IllegalArgumentException("Nome/Razao social do cliente e obrigatorio.");
        }
        if (cliente.getTelefone() == null || cliente.getTelefone().isBlank()) {
            throw new IllegalArgumentException("Telefone do cliente e obrigatorio.");
        }
        if (cliente.getCpfCnpj() == null || cliente.getCpfCnpj().isBlank()) {
            throw new IllegalArgumentException("CPF/CNPJ do cliente e obrigatorio.");
        }
        if (cliente.getEnderecos() == null || cliente.getEnderecos().isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos um endereco para o cliente.");
        }
        cliente.getEnderecos().forEach(endereco -> {
            if (endereco.getCep() == null || endereco.getCep().isBlank()) {
                throw new IllegalArgumentException("CEP do endereco do cliente e obrigatorio.");
            }
        });
        if (cliente.getDataCadastro() == null) {
            cliente.setDataCadastro(LocalDateTime.now());
        }
        if (cliente.getIdCliente() == null && cliente.getStatus() == 0) {
            cliente.setStatus(1);
        }
        if (cliente.getEmpresaId() == null) {
            cliente.setEmpresaId(empresaAtual);
        }
        if (cliente.getFilialId() == null) {
            cliente.setFilialId(filialAtual);
        }
        if (!cliente.getEmpresaId().equals(empresaAtual) || !cliente.getFilialId().equals(filialAtual)) {
            throw new IllegalArgumentException("Cliente fora do escopo da empresa/filial ativa.");
        }
        return clienteRepository.save(cliente);
    }

    public Cliente desativar(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente nao encontrado"));
        cliente.setStatus(0);
        cliente.setDeletedAt(LocalDateTime.now());
        cliente.setDeletedBy("sistema");
        cliente.setMotivoRetencao("Registro mantido para historico operacional, financeiro e auditoria.");
        return clienteRepository.save(cliente);
    }

    public Cliente desativar(Long id, Long empresaId, Long filialId) {
        Cliente cliente = buscarPorId(id, empresaId, filialId)
                .orElseThrow(() -> new RuntimeException("Cliente nao encontrado"));
        cliente.setStatus(0);
        cliente.setDeletedAt(LocalDateTime.now());
        cliente.setDeletedBy("sistema");
        cliente.setMotivoRetencao("Registro mantido para historico operacional, financeiro e auditoria.");
        return clienteRepository.save(cliente);
    }
}
