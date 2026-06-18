package Service;

import Model.Menu;
import Model.Perfil;
import Repository.MenuRepository;
import Repository.PerfilRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerfilService {

    @Autowired
    private PerfilRepository repository;
    @Autowired
    private MenuRepository menuRepository;
    @Autowired
    private AuditTrailService auditTrailService;

    @Transactional(readOnly = true)
    public List<Perfil> listarTodos() {
        List<Perfil> perfis = repository.findAll();
        perfis.forEach(perfil -> org.hibernate.Hibernate.initialize(perfil.getMenus()));
        return perfis;
    }

    @Transactional(readOnly = true)
    public Optional<Perfil> buscarPorId(Long id) {
        Optional<Perfil> perfil = repository.findById(id);
        perfil.ifPresent(item -> org.hibernate.Hibernate.initialize(item.getMenus()));
        return perfil;
    }

    public Perfil salvar(Perfil obj) {
        Perfil salvo = repository.save(obj);
        auditTrailService.registrarSistema("PERFIL_SALVO", "Perfil", salvo.getIdPerfil(), null, resumoPerfil(salvo));
        return salvo;
    }

    public Perfil desativar(Long id) {
        Perfil obj = repository.findById(id).orElseThrow(() -> new RuntimeException("Perfil nao encontrado"));
        String anterior = resumoPerfil(obj);
        obj.setStatus(0);
        Perfil salvo = repository.save(obj);
        auditTrailService.registrarSistema("PERFIL_DESATIVADO", "Perfil", salvo.getIdPerfil(), anterior, resumoPerfil(salvo));
        return salvo;
    }

    @Transactional
    public Perfil vincularMenu(Long perfilId, Long menuId) {
        Perfil perfil = repository.findById(perfilId).orElseThrow(() -> new RuntimeException("Perfil nao encontrado"));
        Menu menu = menuRepository.findById(menuId).orElseThrow(() -> new RuntimeException("Menu nao encontrado"));
        if (menu.getPerfis() == null) {
            menu.setPerfis(new HashSet<>());
        }
        menu.getPerfis().add(perfil);
        menuRepository.save(menu);
        auditTrailService.registrarSistema("PERFIL_MENU_VINCULADO", "Perfil", perfilId, null, "menuId=" + menuId);
        return repository.findById(perfilId).orElse(perfil);
    }

    @Transactional
    public Perfil desvincularMenu(Long perfilId, Long menuId) {
        Perfil perfil = repository.findById(perfilId).orElseThrow(() -> new RuntimeException("Perfil nao encontrado"));
        Menu menu = menuRepository.findById(menuId).orElseThrow(() -> new RuntimeException("Menu nao encontrado"));
        if (menu.getPerfis() != null) {
            menu.getPerfis().removeIf((item) -> perfilId.equals(item.getIdPerfil()));
            menuRepository.save(menu);
        }
        auditTrailService.registrarSistema("PERFIL_MENU_DESVINCULADO", "Perfil", perfilId, "menuId=" + menuId, null);
        return repository.findById(perfilId).orElse(perfil);
    }

    private String resumoPerfil(Perfil perfil) {
        return "nome=" + perfil.getNome() + ";status=" + perfil.getStatus();
    }
}
