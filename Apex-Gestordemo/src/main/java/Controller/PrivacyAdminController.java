package Controller;

import DTO.LgpdConfirmacaoAnonimizacaoRequestDTO;
import DTO.LgpdSimulacaoAnonimizacaoDTO;
import DTO.TitularDataRequestDTO;
import DTO.TitularDataResponseDTO;
import Service.DireitosTitularService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/privacy")
public class PrivacyAdminController {

    private final DireitosTitularService direitosTitularService;

    public PrivacyAdminController(DireitosTitularService direitosTitularService) {
        this.direitosTitularService = direitosTitularService;
    }

    @PostMapping({"/admin/requests", "/requests"})
    public ResponseEntity<TitularDataResponseDTO> registrarSolicitacao(
            @RequestBody TitularDataRequestDTO request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(direitosTitularService.registrarSolicitacao(request, httpRequest));
    }

    @GetMapping({"/admin/clientes/{id}/export", "/export/clientes/{id}"})
    public ResponseEntity<Map<String, Object>> exportarCliente(
            @PathVariable Long id,
            @RequestParam(name = "justificativa", required = false) String justificativa,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(direitosTitularService.exportarCliente(id, justificativa, httpRequest));
    }

    @GetMapping("/admin/clientes/{id}/anonymize/simulate")
    public ResponseEntity<LgpdSimulacaoAnonimizacaoDTO> simularAnonimizacao(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(direitosTitularService.simularAnonimizacao(id, httpRequest));
    }

    @PostMapping("/admin/clientes/{id}/anonymize")
    public ResponseEntity<TitularDataResponseDTO> anonimizarClientePost(
            @PathVariable Long id,
            @Valid @RequestBody LgpdConfirmacaoAnonimizacaoRequestDTO request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(direitosTitularService.anonimizarClienteConfirmado(id, request, httpRequest));
    }

    @DeleteMapping("/clientes/{id}")
    public ResponseEntity<TitularDataResponseDTO> anonimizarClienteCompat(
            @PathVariable Long id,
            @RequestParam(name = "confirmadoExplicito", required = false) Boolean confirmadoExplicito,
            @RequestParam(name = "justificativa", required = false) String justificativa,
            @RequestParam(name = "simulacaoHash", required = false) String simulacaoHash,
            @RequestParam(name = "solicitacaoId", required = false) Long solicitacaoId,
            HttpServletRequest httpRequest) {
        LgpdConfirmacaoAnonimizacaoRequestDTO request = new LgpdConfirmacaoAnonimizacaoRequestDTO(
                confirmadoExplicito, justificativa, simulacaoHash, solicitacaoId);
        return ResponseEntity.ok(direitosTitularService.anonimizarClienteConfirmado(id, request, httpRequest));
    }
}
