package Controller;

import Model.Notificacao;
import Repository.NotificacaoRepository;
import Service.AuthorizationService;
import Service.TenantScopeService;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoController {

    @Autowired
    private NotificacaoRepository notificacaoRepository;
    @Autowired
    private TenantScopeService tenantScopeService;
    @Autowired
    private AuthorizationService authorizationService;

    @GetMapping("/pendentes")
    public List<Notificacao> listarPendentes(HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.PRODUCT_VIEW,
                "Notificacoes", "listar notificacoes pendentes");
        return notificacaoRepository.findByEmpresaIdAndFilialIdAndStatusOrderByCriadoEmDesc(
                tenantScopeService.empresaId(request), tenantScopeService.filialId(request), "PENDENTE");
    }
}
