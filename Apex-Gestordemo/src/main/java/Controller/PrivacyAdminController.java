package Controller;

import DTO.TitularDataRequestDTO;
import DTO.TitularDataResponseDTO;
import Service.DireitosTitularService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/privacy")
public class PrivacyAdminController {

    private final DireitosTitularService direitosTitularService;

    public PrivacyAdminController(DireitosTitularService direitosTitularService) {
        this.direitosTitularService = direitosTitularService;
    }

    @PostMapping({"/admin/requests", "/requests"})
    public ResponseEntity<TitularDataResponseDTO> registrarSolicitacao(@RequestBody TitularDataRequestDTO request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(direitosTitularService.registrarSolicitacao(request, httpRequest));
    }

    @GetMapping({"/admin/clientes/{id}/export", "/export/clientes/{id}"})
    public ResponseEntity<Map<String, Object>> exportarCliente(@PathVariable Long id,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(direitosTitularService.exportarCliente(id, httpRequest));
    }

    @PostMapping("/admin/clientes/{id}/anonymize")
    public ResponseEntity<TitularDataResponseDTO> anonimizarClientePost(@PathVariable Long id,
            @RequestBody TitularDataRequestDTO request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(direitosTitularService.anonimizarCliente(id, request, httpRequest));
    }

    @DeleteMapping("/clientes/{id}")
    public ResponseEntity<TitularDataResponseDTO> anonimizarClienteCompat(@PathVariable Long id,
            HttpServletRequest httpRequest) {
        TitularDataRequestDTO request = new TitularDataRequestDTO(String.valueOf(id), "CLIENTE", "ANONIMIZACAO",
                "Compatibilidade: exclusao logica convertida para anonimizacao.");
        return ResponseEntity.ok(direitosTitularService.anonimizarCliente(id, request, httpRequest));
    }
}
