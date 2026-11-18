package Controller;

import DTO.CondicaoPagamentoRequestDTO;
import DTO.CondicaoPagamentoResponseDTO;
import Service.AuthorizationService;
import Service.CondicaoPagamentoService;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/condicoes-pagamento")
public class CondicaoPagamentoController {

    @Autowired
    private CondicaoPagamentoService service;
    @Autowired
    private AuthorizationService authorizationService;

    @GetMapping
    public List<CondicaoPagamentoResponseDTO> listar(HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.PAYMENT_CONDITION_VIEW, "Financeiro",
                "listar condicoes de pagamento");
        return service.listar(request);
    }

    @PostMapping
    public ResponseEntity<CondicaoPagamentoResponseDTO> criar(@Valid @RequestBody CondicaoPagamentoRequestDTO body,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.PAYMENT_CONDITION_MANAGE, "Financeiro",
                "criar condicao de pagamento");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.salvar(body, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CondicaoPagamentoResponseDTO> atualizar(@PathVariable Long id,
            @Valid @RequestBody CondicaoPagamentoRequestDTO body,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.PAYMENT_CONDITION_MANAGE, "Financeiro",
                "atualizar condicao de pagamento");
        return ResponseEntity.ok(service.atualizar(id, body, request));
    }

    @PatchMapping("/{id}/inativar")
    public ResponseEntity<CondicaoPagamentoResponseDTO> inativar(@PathVariable Long id, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.PAYMENT_CONDITION_MANAGE, "Financeiro",
                "inativar condicao de pagamento");
        return ResponseEntity.ok(service.inativar(id, request));
    }
}
