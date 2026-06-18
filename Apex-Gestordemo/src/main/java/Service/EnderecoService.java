package Service;

import Model.Endereco;
import Repository.EnderecoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class EnderecoService {

    @Autowired
    private EnderecoRepository enderecoRepository;

    public List<Endereco> listarTodos() {
        return enderecoRepository.findAll();
    }

    public Optional<Endereco> buscarPorId(Long id) {
        return enderecoRepository.findById(id);
    }

    public Endereco salvar(Endereco endereco) {
        return enderecoRepository.save(endereco);
    }

    public void deletar(Long id) {
        if (!enderecoRepository.existsById(id)) {
            throw new RuntimeException("Endereço não encontrado");
        }
        enderecoRepository.deleteById(id);
    }

    public List<Endereco> listarTodos(Long empresaId, Long filialId) {
        return enderecoRepository.findByEmpresaIdAndFilialIdOrderByCidadeAsc(empresaId, filialId);
    }

    public Optional<Endereco> buscarPorId(Long id, Long empresaId, Long filialId) {
        return enderecoRepository.findByIdEnderecoAndEmpresaIdAndFilialId(id, empresaId, filialId);
    }

    public Endereco salvar(Endereco endereco, Long empresaId, Long filialId) {
        endereco.setEmpresaId(empresaId);
        endereco.setFilialId(filialId);
        return enderecoRepository.save(endereco);
    }

    public void deletar(Long id, Long empresaId, Long filialId) {
        Endereco endereco = buscarPorId(id, empresaId, filialId)
                .orElseThrow(() -> new IllegalArgumentException("Endereco nao encontrado no tenant."));
        enderecoRepository.delete(endereco);
    }
}
