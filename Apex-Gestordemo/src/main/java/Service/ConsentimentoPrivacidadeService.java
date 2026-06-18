package Service;

import DTO.ConsentimentoRequestDTO;
import DTO.ConsentimentoResponseDTO;
import Model.ConsentimentoPrivacidade;
import Repository.ConsentimentoPrivacidadeRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConsentimentoPrivacidadeService {

    @Autowired
    private ConsentimentoPrivacidadeRepository repository;

    @Autowired
    private PrivacyGuardService privacyGuardService;

    @Autowired
    private AuditTrailService auditTrailService;

    public ConsentimentoResponseDTO registrar(ConsentimentoRequestDTO request, HttpServletRequest httpRequest) {
        String titularId = required(request.titularId(), "Titular do consentimento e obrigatorio.");
        String tipoTitular = defaultText(request.tipoTitular(), "USUARIO");
        String finalidade = defaultText(request.finalidade(), "ACESSO_SISTEMA");
        String versao = required(request.versao(), "Versao do termo e obrigatoria.");
        String canal = defaultText(request.canal(), "WEB");

        ConsentimentoPrivacidade consentimento = new ConsentimentoPrivacidade();
        consentimento.setTitularId(titularId);
        consentimento.setTipoTitular(tipoTitular);
        consentimento.setDocumentoHash(privacyGuardService.sha256(request.documento()));
        consentimento.setFinalidade(finalidade);
        consentimento.setVersaoTermo(versao);
        consentimento.setOrigem(canal);
        consentimento.setStatus("ATIVO");
        consentimento.setEmpresaId(privacyGuardService.currentEmpresaId(httpRequest));
        consentimento.setFilialId(privacyGuardService.currentFilialId(httpRequest));

        ConsentimentoPrivacidade salvo = repository.save(consentimento);
        auditTrailService.registrarPrivacidade(
                "CONSENTIMENTO_REGISTRADO",
                "ConsentimentoPrivacidade",
                salvo.getIdConsentimento(),
                "Privacidade",
                null,
                "titular=" + salvo.getTitularId() + ";finalidade=" + salvo.getFinalidade()
                        + ";versao=" + salvo.getVersaoTermo(),
                httpRequest,
                "Aceite contextual registrado no fluxo operacional.");

        return new ConsentimentoResponseDTO(
                salvo.getIdConsentimento(),
                salvo.getTitularId(),
                salvo.getTipoTitular(),
                salvo.getFinalidade(),
                salvo.getVersaoTermo(),
                salvo.getStatus(),
                salvo.getAceitoEm());
    }

    public boolean possuiConsentimentoAtivo(String titularId, String tipoTitular, String finalidade) {
        if (titularId == null || titularId.isBlank()) {
            return false;
        }
        return !repository.findByTitularIdAndTipoTitularAndFinalidadeAndStatus(
                titularId,
                defaultText(tipoTitular, "CLIENTE"),
                defaultText(finalidade, "COMUNICACAO_COMERCIAL"),
                "ATIVO").isEmpty();
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
