package Service;

import Model.Fornecedor;
import Repository.FornecedorRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FornecedorService {

    @Autowired
    private FornecedorRepository fornecedorRepository;

    public List<Fornecedor> listarTodos() {
        return fornecedorRepository.findAll();
    }

    public List<Fornecedor> listarTodos(Long empresaId, Long filialId) {
        return fornecedorRepository.findByEmpresaIdAndFilialIdOrderByRazaoSocialAsc(empresaId, filialId);
    }

    public Optional<Fornecedor> buscarPorId(Long id) {
        return fornecedorRepository.findById(id);
    }

    public Optional<Fornecedor> buscarPorId(Long id, Long empresaId, Long filialId) {
        return fornecedorRepository.findByIdFornecedorAndEmpresaIdAndFilialId(id, empresaId, filialId);
    }

    public Fornecedor salvar(Fornecedor fornecedor) {
        return salvar(fornecedor, fornecedor.getEmpresaId(), fornecedor.getFilialId());
    }

    public Fornecedor salvar(Fornecedor fornecedor, Long empresaId, Long filialId) {
        Long empresaAtual = empresaId == null ? 1L : empresaId;
        Long filialAtual = filialId == null ? 1L : filialId;
        if (fornecedor.getRazaoSocial() == null || fornecedor.getRazaoSocial().isBlank()) {
            throw new IllegalArgumentException("Razao social do fornecedor e obrigatoria.");
        }
        if (fornecedor.getNomeFantasia() == null || fornecedor.getNomeFantasia().isBlank()) {
            throw new IllegalArgumentException("Nome fantasia do fornecedor e obrigatorio.");
        }
        if (fornecedor.getCnpj() == null || fornecedor.getCnpj().isBlank()) {
            throw new IllegalArgumentException("CNPJ do fornecedor e obrigatorio.");
        }
        if (fornecedor.getTelefone() == null || fornecedor.getTelefone().isBlank()) {
            throw new IllegalArgumentException("Telefone do fornecedor e obrigatorio.");
        }
        if (fornecedor.getEndereco() == null || fornecedor.getEndereco().getCep() == null
                || fornecedor.getEndereco().getCep().isBlank()) {
            throw new IllegalArgumentException("Endereco com CEP e obrigatorio para fornecedor.");
        }
        if (fornecedor.getDataCadastro() == null) {
            fornecedor.setDataCadastro(LocalDateTime.now());
        }
        if (fornecedor.getIdFornecedor() == null && fornecedor.getStatus() == 0) {
            fornecedor.setStatus(1);
        }
        if (fornecedor.getIdFornecedor() != null
                && buscarPorId(fornecedor.getIdFornecedor(), empresaAtual, filialAtual).isEmpty()) {
            throw new RuntimeException("Fornecedor nao encontrado no escopo da empresa/filial.");
        }
        fornecedor.setEmpresaId(empresaAtual);
        fornecedor.setFilialId(filialAtual);
        return fornecedorRepository.save(fornecedor);
    }

    public Fornecedor desativar(Long id) {
        Fornecedor fornecedor = fornecedorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fornecedor nao encontrado"));
        return desativar(fornecedor, fornecedor.getEmpresaId(), fornecedor.getFilialId());
    }

    public Fornecedor desativar(Long id, Long empresaId, Long filialId) {
        Fornecedor fornecedor = buscarPorId(id, empresaId, filialId)
                .orElseThrow(() -> new RuntimeException("Fornecedor nao encontrado"));
        return desativar(fornecedor, empresaId, filialId);
    }

    private Fornecedor desativar(Fornecedor fornecedor, Long empresaId, Long filialId) {
        fornecedor.setStatus(0);
        fornecedor.setEmpresaId(empresaId);
        fornecedor.setFilialId(filialId);
        return fornecedorRepository.save(fornecedor);
    }
}
