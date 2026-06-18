package Service;

import Model.Menu;
import Repository.MenuRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MenuService {

    @Autowired
    private MenuRepository menuRepository;
    @Autowired
    private AuditTrailService auditTrailService;

    public List<Menu> listarTodos() {
        return menuRepository.findAll();
    }

    public Optional<Menu> buscarPorId(Long id) {
        return menuRepository.findById(id);
    }

    public Menu salvar(Menu menu) {
        Menu salvo = menuRepository.save(menu);
        auditTrailService.registrarSistema("MENU_SALVO", "Menu", salvo.getIdMenu(), null, resumoMenu(salvo));
        return salvo;
    }

    public Menu desativar(Long id) {
        Menu menu = menuRepository.findById(id).orElseThrow(() -> new RuntimeException("Menu nao encontrado"));
        String anterior = resumoMenu(menu);
        menu.setExibir(0);
        Menu salvo = menuRepository.save(menu);
        auditTrailService.registrarSistema("MENU_DESATIVADO", "Menu", salvo.getIdMenu(), anterior, resumoMenu(salvo));
        return salvo;
    }

    private String resumoMenu(Menu menu) {
        return "nome=" + menu.getNome() + ";link=" + menu.getLink() + ";exibir=" + menu.getExibir();
    }
}
