package Service;

import Model.Despesa;
import Repository.DespesaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DespesaService {

    @Autowired
    private DespesaRepository despesaRepository;

    public List<Despesa> listarTodos() {
        return despesaRepository.findAll();
    }

    public List<Despesa> listarTodos(Long empresaId, Long filialId) {
        return despesaRepository.findByEmpresaIdAndFilialIdOrderByDataVencimentoDesc(empresaId, filialId);
    }

    public Optional<Despesa> buscarPorId(Long id) {
        return despesaRepository.findById(id);
    }

    public Optional<Despesa> buscarPorId(Long id, Long empresaId, Long filialId) {
        return despesaRepository.findByIdDespesaAndEmpresaIdAndFilialId(id, empresaId, filialId);
    }

    public Despesa salvar(Despesa despesa) {
        return despesaRepository.save(despesa);
    }

    public Despesa salvar(Despesa despesa, Long empresaId, Long filialId) {
        despesa.setEmpresaId(empresaId);
        despesa.setFilialId(filialId);
        return despesaRepository.save(despesa);
    }

    public Despesa cancelar(Long id) {
        Despesa despesa = despesaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Despesa nao encontrada"));
        return cancelar(despesa, despesa.getEmpresaId(), despesa.getFilialId());
    }

    public Despesa cancelar(Long id, Long empresaId, Long filialId) {
        Despesa despesa = buscarPorId(id, empresaId, filialId)
                .orElseThrow(() -> new RuntimeException("Despesa nao encontrada"));
        return cancelar(despesa, empresaId, filialId);
    }

    private Despesa cancelar(Despesa despesa, Long empresaId, Long filialId) {
        despesa.setStatus(0);
        despesa.setEmpresaId(empresaId);
        despesa.setFilialId(filialId);
        return despesaRepository.save(despesa);
    }
}
