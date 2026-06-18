package Controller;

import DTO.CaixaOperacaoRequestDTO;
import DTO.CaixaSessaoResponseDTO;
import Service.AuthorizationService;
import Service.CaixaService;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/caixa")
@CrossOrigin(origins = "http://localhost:4200")
public class CaixaController {

    @Autowired
    private CaixaService caixaService;
    @Autowired
    private AuthorizationService authorizationService;

    @GetMapping("/atual")
    public ResponseEntity<CaixaSessaoResponseDTO> atual(HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.CASHIER_VIEW, "Caixa", "consultar caixa atual");
        CaixaSessaoResponseDTO atual = caixaService.statusAtual(request);
        return atual == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(atual);
    }

    @PostMapping("/abrir")
    public ResponseEntity<CaixaSessaoResponseDTO> abrir(@Valid @RequestBody CaixaOperacaoRequestDTO body,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.CASHIER_OPEN, "Caixa", "abrir caixa");
        return ResponseEntity.ok(caixaService.abrir(body.valor(), request));
    }

    @PostMapping("/{id}/suprimento")
    public ResponseEntity<CaixaSessaoResponseDTO> suprimento(@PathVariable Long id,
            @Valid @RequestBody CaixaOperacaoRequestDTO body,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.CASHIER_SUPPLY, "Caixa", "registrar suprimento");
        return ResponseEntity.ok(caixaService.suprimento(id, body.valor(), body.observacao(), request));
    }

    @PostMapping("/{id}/sangria")
    public ResponseEntity<CaixaSessaoResponseDTO> sangria(@PathVariable Long id,
            @Valid @RequestBody CaixaOperacaoRequestDTO body,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.CASHIER_WITHDRAW, "Caixa", "registrar sangria");
        return ResponseEntity.ok(caixaService.sangria(id, body.valor(), body.observacao(), request));
    }

    @PostMapping("/{id}/fechar")
    public ResponseEntity<CaixaSessaoResponseDTO> fechar(@PathVariable Long id, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.CASHIER_CLOSE, "Caixa", "fechar caixa");
        return ResponseEntity.ok(caixaService.fechar(id, request));
    }
}
