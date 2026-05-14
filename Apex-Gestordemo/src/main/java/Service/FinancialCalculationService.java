package Service;

import DTO.AdmCalcRequestDTO;
import DTO.FinancialCalculationResponseDTO;
import DTO.LaborCalculationRequestDTO;
import DTO.TaxCalculationRequestDTO;
import Model.FinancialCalculation;
import Repository.FinancialCalculationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class FinancialCalculationService {

    private final LaborCalculationEngine laborCalculationEngine;
    private final TaxCalculationEngine taxCalculationEngine;
    private final AdmCalcEngine admCalcEngine;
    private final FinancialCalculationRepository repository;
    private final FinancialAuditTrailService auditTrailService;
    private final DataProtectionService dataProtectionService;
    private final ObjectMapper objectMapper;

    public FinancialCalculationService(
            LaborCalculationEngine laborCalculationEngine,
            TaxCalculationEngine taxCalculationEngine,
            AdmCalcEngine admCalcEngine,
            FinancialCalculationRepository repository,
            FinancialAuditTrailService auditTrailService,
            DataProtectionService dataProtectionService,
            ObjectMapper objectMapper
    ) {
        this.laborCalculationEngine = laborCalculationEngine;
        this.taxCalculationEngine = taxCalculationEngine;
        this.admCalcEngine = admCalcEngine;
        this.repository = repository;
        this.auditTrailService = auditTrailService;
        this.dataProtectionService = dataProtectionService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public FinancialCalculationResponseDTO calculateLabor(LaborCalculationRequestDTO request, String actorLogin) {
        return persist("TRABALHISTA", request, laborCalculationEngine.calculate(request), actorLogin, List.of("Use tabelas oficiais vigentes parametrizadas pelo contador responsavel."));
    }

    @Transactional
    public FinancialCalculationResponseDTO calculateTaxes(TaxCalculationRequestDTO request, String actorLogin) {
        return persist("TRIBUTARIO", request, taxCalculationEngine.calculate(request), actorLogin, List.of("IRRF, INSS e Simples Nacional dependem de tabelas oficiais atualizadas."));
    }

    @Transactional
    public FinancialCalculationResponseDTO calculateAdmCalc(AdmCalcRequestDTO request, String actorLogin) {
        return persist("ADMCALC", request, admCalcEngine.calculate(request), actorLogin, List.of("Motor migrado do projeto AdmCalc original para BigDecimal e API."));
    }

    @Transactional(readOnly = true)
    public List<FinancialCalculationResponseDTO> listRecent(int limit) {
        int pageSize = Math.min(Math.max(limit, 1), 100);
        return repository.findAll(PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "criadoEm")))
                .stream()
                .map(this::toDto)
                .toList();
    }

    private FinancialCalculationResponseDTO persist(String type, Object request, Map<String, BigDecimal> results, String actorLogin, List<String> warnings) {
        String input = serialize(request);
        String output = serialize(results);
        FinancialCalculation calculation = new FinancialCalculation();
        calculation.setTipoCalculo(type);
        calculation.setAtorLogin(actorLogin == null || actorLogin.isBlank() ? "anonymous" : actorLogin);
        calculation.setInputHash(dataProtectionService.hash(input));
        calculation.setInputSnapshot(input);
        calculation.setResultadoSnapshot(output);
        calculation.setCriadoEm(LocalDateTime.now());
        FinancialCalculation saved = repository.save(calculation);
        auditTrailService.record("CALCULO_EXECUTADO", "financial_calculation", saved.getIdFinancialCalculation(), saved.getAtorLogin(), null, type, results);
        return new FinancialCalculationResponseDTO(saved.getIdFinancialCalculation(), type, saved.getCriadoEm(), results, warnings);
    }

    private FinancialCalculationResponseDTO toDto(FinancialCalculation calculation) {
        return new FinancialCalculationResponseDTO(
                calculation.getIdFinancialCalculation(),
                calculation.getTipoCalculo(),
                calculation.getCriadoEm(),
                Map.of(),
                List.of("Resultado historico armazenado no backend; consulte a auditoria para trilha completa.")
        );
    }

    private String serialize(Object value) {
        try {
            return dataProtectionService.maskSensitiveText(objectMapper.writeValueAsString(value));
        } catch (JacksonException e) {
            return dataProtectionService.maskSensitiveText(String.valueOf(value));
        }
    }
}
