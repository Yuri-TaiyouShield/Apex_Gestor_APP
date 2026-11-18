package Service;

import Model.GrupoProduto;
import Repository.GrupoProdutoRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GrupoProdutoService {

    @Autowired
    private GrupoProdutoRepository grupoProdutoRepository;
    @Autowired
    private AuditTrailService auditTrailService;

    public List<GrupoProduto> listarTodos() {
        return grupoProdutoRepository.findAll();
    }

    public List<GrupoProduto> listarTodos(Long empresaId, Long filialId) {
        return grupoProdutoRepository.findVisibleToTenant(empresaId, filialId);
    }

    public Optional<GrupoProduto> buscarPorId(Long id) {
        return grupoProdutoRepository.findById(id);
    }

    public Optional<GrupoProduto> buscarPorId(Long id, Long empresaId, Long filialId) {
        return grupoProdutoRepository.findVisibleById(id, empresaId, filialId);
    }

    public GrupoProduto salvar(GrupoProduto grupoProduto) {
        GrupoProduto salvo = grupoProdutoRepository.save(grupoProduto);
        auditTrailService.registrarSistema("GRUPO_PRODUTO_SALVO", "GrupoProduto", salvo.getIdGrupoProduto(), null, resumo(salvo));
        return salvo;
    }

    public GrupoProduto salvar(GrupoProduto grupoProduto, Long empresaId, Long filialId) {
        if (grupoProduto.getNome() == null || grupoProduto.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome do grupo de produto e obrigatorio.");
        }
        if (grupoProduto.getStatus() == 0) {
            grupoProduto.setStatus(1);
        }
        grupoProduto.setEmpresaId(empresaId);
        grupoProduto.setFilialId(filialId);
        return salvar(grupoProduto);
    }

    public GrupoProduto desativar(Long id) {
        GrupoProduto grupoProduto = grupoProdutoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Grupo de produto nao encontrado"));
        String anterior = resumo(grupoProduto);
        grupoProduto.setStatus(0);
        GrupoProduto salvo = grupoProdutoRepository.save(grupoProduto);
        auditTrailService.registrarSistema("GRUPO_PRODUTO_DESATIVADO", "GrupoProduto", salvo.getIdGrupoProduto(), anterior, resumo(salvo));
        return salvo;
    }

    public GrupoProduto desativar(Long id, Long empresaId, Long filialId) {
        GrupoProduto grupoProduto = buscarPorId(id, empresaId, filialId)
                .orElseThrow(() -> new RuntimeException("Grupo de produto nao encontrado no escopo da empresa/filial."));
        String anterior = resumo(grupoProduto);
        grupoProduto.setStatus(0);
        grupoProduto.setEmpresaId(empresaId);
        grupoProduto.setFilialId(filialId);
        GrupoProduto salvo = grupoProdutoRepository.save(grupoProduto);
        auditTrailService.registrarSistema("GRUPO_PRODUTO_DESATIVADO", "GrupoProduto", salvo.getIdGrupoProduto(), anterior, resumo(salvo));
        return salvo;
    }

    private String resumo(GrupoProduto grupoProduto) {
        if (grupoProduto == null) {
            return null;
        }
        return "nome=" + grupoProduto.getNome() + ";status=" + grupoProduto.getStatus();
    }
}
