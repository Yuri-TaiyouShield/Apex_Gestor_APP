package Service;

import DTO.FinancialAuditEventDTO;
import Model.FinancialAuditEvent;
import Repository.FinancialAuditEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FinancialAuditTrailService {

    private final FinancialAuditEventRepository repository;
    private final DataProtectionService dataProtectionService;
    private final ObjectMapper objectMapper;

    public FinancialAuditTrailService(FinancialAuditEventRepository repository, DataProtectionService dataProtectionService, ObjectMapper objectMapper) {
        this.repository = repository;
        this.dataProtectionService = dataProtectionService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public FinancialAuditEvent record(String eventType, String targetType, Long targetId, String actorLogin, String oldValue, String newValue, Object metadata) {
        String snapshot = serialize(metadata);
        FinancialAuditEvent event = new FinancialAuditEvent();
        event.setTipoEvento(eventType);
        event.setAlvoTipo(targetType);
        event.setAlvoId(targetId);
        event.setAtorLogin(actorLogin == null || actorLogin.isBlank() ? "anonymous" : actorLogin);
        event.setValorAnterior(limit(oldValue));
        event.setValorNovo(limit(newValue));
        event.setMetadados(snapshot);
        event.setMetadadosHash(dataProtectionService.hash(snapshot));
        event.setCriadoEm(LocalDateTime.now());
        return repository.save(event);
    }

    @Transactional(readOnly = true)
    public List<FinancialAuditEventDTO> listRecent(int limit) {
        int pageSize = Math.min(Math.max(limit, 1), 200);
        return repository.findAll(PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "criadoEm")))
                .stream()
                .map(this::toDto)
                .toList();
    }

    private FinancialAuditEventDTO toDto(FinancialAuditEvent event) {
        return new FinancialAuditEventDTO(
                event.getIdFinancialAuditEvent(),
                event.getTipoEvento(),
                event.getAlvoTipo(),
                event.getAlvoId(),
                event.getAtorLogin(),
                event.getValorAnterior(),
                event.getValorNovo(),
                event.getMetadados(),
                event.getCriadoEm()
        );
    }

    private String serialize(Object value) {
        try {
            return dataProtectionService.maskSensitiveText(objectMapper.writeValueAsString(value));
        } catch (JacksonException e) {
            return dataProtectionService.maskSensitiveText(String.valueOf(value));
        }
    }

    private String limit(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 255 ? value : value.substring(0, 255);
    }
}
