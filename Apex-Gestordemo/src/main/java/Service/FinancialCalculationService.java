package Service;

import DTO.AdmCalcRequestDTO;
import DTO.FinancialCalculationResponseDTO;
import DTO.LaborCalculationRequestDTO;
import DTO.TaxCalculationRequestDTO;
import Model.FinancialCalculation;
import Repository.FinancialCalculationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FinancialCalculationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FinancialCalculationService.class);
    private static final TypeReference<Map<String, BigDecimal>> RESULT_MAP_TYPE = new TypeReference<>() {
    };

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
    public FinancialCalculationResponseDTO calculateLabor(LaborCalculationRequestDTO request, String actorLogin,
            Long empresaId, Long filialId) {
        return persist("TRABALHISTA", request, laborCalculationEngine.calculate(request), actorLogin, empresaId, filialId,
                List.of("Use tabelas oficiais vigentes parametrizadas pelo contador responsavel."));
    }

    @Transactional
    public FinancialCalculationResponseDTO calculateTaxes(TaxCalculationRequestDTO request, String actorLogin,
            Long empresaId, Long filialId) {
        return persist("TRIBUTARIO", request, taxCalculationEngine.calculate(request), actorLogin, empresaId, filialId,
                List.of("IRRF, INSS e Simples Nacional dependem de tabelas oficiais atualizadas."));
    }

    @Transactional
    public FinancialCalculationResponseDTO calculateAdmCalc(AdmCalcRequestDTO request, String actorLogin,
            Long empresaId, Long filialId) {
        Map<String, BigDecimal> calculated = admCalcEngine.calculate(request);
        List<String> warnings = new ArrayList<>();
        warnings.add("Motor migrado do projeto AdmCalc original para BigDecimal e API.");
        warnings.addAll(admCalcEngine.warnings(request));
        return persist("ADMCALC", request, calculated, actorLogin, empresaId, filialId, List.copyOf(warnings));
    }

    @Transactional(readOnly = true)
    public List<FinancialCalculationResponseDTO> listRecent(int limit, Long empresaId, Long filialId) {
        int pageSize = Math.min(Math.max(limit, 1), 100);
        return repository.findByEmpresaIdAndFilialId(empresaId, filialId,
                        PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "criadoEm")))
                .stream()
                .map(this::toDto)
                .toList();
    }

    private FinancialCalculationResponseDTO persist(String type, Object request, Map<String, BigDecimal> results,
            String actorLogin, Long empresaId, Long filialId, List<String> warnings) {
        String input = serialize(request);
        String output = serializeResults(results);
        FinancialCalculation calculation = new FinancialCalculation();
        calculation.setTipoCalculo(type);
        calculation.setAtorLogin(actorLogin == null || actorLogin.isBlank() ? "anonymous" : actorLogin);
        calculation.setEmpresaId(empresaId);
        calculation.setFilialId(filialId);
        calculation.setInputHash(dataProtectionService.hash(input));
        calculation.setInputSnapshot(input);
        calculation.setResultadoSnapshot(output);
        calculation.setCriadoEm(LocalDateTime.now());
        FinancialCalculation saved = repository.save(calculation);
        auditTrailService.record("CALCULO_EXECUTADO", "financial_calculation", saved.getIdFinancialCalculation(),
                saved.getAtorLogin(), null, type, sanitizedAuditSummary(type, results, saved.getInputHash()),
                empresaId, filialId);
        return new FinancialCalculationResponseDTO(saved.getIdFinancialCalculation(), type, saved.getCriadoEm(), results, warnings);
    }

    private Map<String, Object> sanitizedAuditSummary(String type, Map<String, BigDecimal> results, String inputHash) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("tipoCalculo", type);
        summary.put("quantidadeIndicadores", results == null ? 0 : results.size());
        summary.put("indicadores", results == null ? List.of() : List.copyOf(results.keySet()));
        summary.put("inputHash", inputHash);
        summary.put("sigiloDadosPessoais", "TRABALHISTA".equals(type) || "TRIBUTARIO".equals(type));
        summary.put("resumo", "Indicadores gravados sem exposicao de salarios ou entradas nominais na trilha de auditoria.");
        return summary;
    }

    private FinancialCalculationResponseDTO toDto(FinancialCalculation calculation) {
        String snapshot = calculation.getResultadoSnapshot();
        if (snapshot == null || snapshot.isBlank()) {
            LOGGER.warn("Snapshot de calculo ausente para financial_calculation id={}", calculation.getIdFinancialCalculation());
            return new FinancialCalculationResponseDTO(
                    calculation.getIdFinancialCalculation(),
                    calculation.getTipoCalculo(),
                    calculation.getCriadoEm(),
                    Map.of(),
                    List.of("Snapshot de resultado ausente no historico.")
            );
        }
        try {
            Map<String, BigDecimal> parsed = objectMapper.readValue(snapshot, RESULT_MAP_TYPE);
            return new FinancialCalculationResponseDTO(
                    calculation.getIdFinancialCalculation(),
                    calculation.getTipoCalculo(),
                    calculation.getCriadoEm(),
                    parsed == null ? Map.of() : parsed,
                    List.of()
            );
        } catch (RuntimeException ex) {
            LOGGER.warn("Falha ao desserializar resultadoSnapshot de financial_calculation id={}: {}",
                    calculation.getIdFinancialCalculation(), ex.getMessage());
            return new FinancialCalculationResponseDTO(
                    calculation.getIdFinancialCalculation(),
                    calculation.getTipoCalculo(),
                    calculation.getCriadoEm(),
                    Map.of(),
                    List.of("Nao foi possivel ler o snapshot historico do calculo.")
            );
        }
    }

    private String serializeResults(Map<String, BigDecimal> results) {
        try {
            return objectMapper.writeValueAsString(results == null ? Map.of() : results);
        } catch (JacksonException e) {
            LOGGER.warn("Falha ao serializar resultados do calculo: {}", e.getMessage());
            return "{}";
        }
    }

    private String serialize(Object value) {
        try {
            return dataProtectionService.maskSensitiveText(objectMapper.writeValueAsString(value));
        } catch (JacksonException e) {
            return dataProtectionService.maskSensitiveText(String.valueOf(value));
        }
    }
}
