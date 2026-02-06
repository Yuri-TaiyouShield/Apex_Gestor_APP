package Service;

import Model.Despesa;
import Repository.DespesaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class DespesaService {

    @Autowired
    private DespesaRepository despesaRepository;

    public List<Despesa> listarTodos() {
        return despesaRepository.findAll();
    }

    public Optional<Despesa> buscarPorId(Long id) {
        return despesaRepository.findById(id);
    }

    public Despesa salvar(Despesa despesa) {
        return despesaRepository.save(despesa);
    }

    public Despesa cancelar(Long id) {
        Despesa d = despesaRepository.findById(id).orElseThrow(() -> new RuntimeException("Despesa não encontrada"));
        d.setStatus(0);
        return despesaRepository.save(d);
    }
}
