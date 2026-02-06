package Service;

import Model.*;
import Repository.PerfilRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class PerfilService { // Replique para as outras classes CRUD simples

    @Autowired
    private PerfilRepository repository;

    public List<Perfil> listarTodos() {
        return repository.findAll();
    }

    public Optional<Perfil> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public Perfil salvar(Perfil obj) {
        return repository.save(obj);
    }

    public Perfil desativar(Long id) {
        Perfil obj = repository.findById(id).orElseThrow(() -> new RuntimeException("Não encontrado"));
        obj.setStatus(0);
        return repository.save(obj);
    }

}
