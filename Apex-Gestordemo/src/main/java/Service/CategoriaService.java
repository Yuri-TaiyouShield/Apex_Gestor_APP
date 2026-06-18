package Service;

import Model.Categoria;
import Repository.CategoriaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    public List<Categoria> listarTodos() {
        return categoriaRepository.findAll();
    }

    public List<Categoria> listarTodos(Long empresaId, Long filialId) {
        return categoriaRepository.findVisibleToTenant(empresaId, filialId);
    }

    public Optional<Categoria> buscarPorId(Long id) {
        return categoriaRepository.findById(id);
    }

    public Optional<Categoria> buscarPorId(Long id, Long empresaId, Long filialId) {
        return categoriaRepository.findVisibleById(id, empresaId, filialId);
    }

    public Categoria salvar(Categoria categoria) {
        return categoriaRepository.save(categoria);
    }

    public Categoria salvar(Categoria categoria, Long empresaId, Long filialId) {
        if (categoria.getDescricao() == null || categoria.getDescricao().isBlank()) {
            throw new IllegalArgumentException("Descricao da categoria e obrigatoria.");
        }
        if (categoria.getStatus() == 0) {
            categoria.setStatus(1);
        }
        categoria.setEmpresaId(empresaId);
        categoria.setFilialId(filialId);
        return categoriaRepository.save(categoria);
    }

    public Categoria desativar(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoria nao encontrada"));
        categoria.setStatus(0);
        return categoriaRepository.save(categoria);
    }

    public Categoria desativar(Long id, Long empresaId, Long filialId) {
        Categoria categoria = buscarPorId(id, empresaId, filialId)
                .orElseThrow(() -> new RuntimeException("Categoria nao encontrada no escopo da empresa/filial."));
        categoria.setStatus(0);
        categoria.setEmpresaId(empresaId);
        categoria.setFilialId(filialId);
        return categoriaRepository.save(categoria);
    }
}
