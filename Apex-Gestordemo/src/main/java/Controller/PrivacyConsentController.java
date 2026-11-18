package Controller;

import DTO.ConsentimentoRequestDTO;
import DTO.ConsentimentoResponseDTO;
import Service.ConsentimentoPrivacidadeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/privacy/consents")
public class PrivacyConsentController {

    private final ConsentimentoPrivacidadeService consentimentoService;

    public PrivacyConsentController(ConsentimentoPrivacidadeService consentimentoService) {
        this.consentimentoService = consentimentoService;
    }

    @PostMapping
    public ResponseEntity<ConsentimentoResponseDTO> register(@RequestBody ConsentimentoRequestDTO request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(consentimentoService.registrar(request, httpRequest));
    }
}
