package Service;

import DTO.DespesaRequestDTO;
import DTO.DespesaResponseDTO;
import DTO.TipoDespesaRequestDTO;
import DTO.TipoDespesaResponseDTO;
import Model.CentroCusto;
import Model.Despesa;
import Model.PlanoContasGerencial;
import Model.TipoDespesa;
import Repository.CentroCustoRepository;
import Repository.DespesaRepository;
import Repository.PlanoContasGerencialRepository;
import Repository.TipoDespesaRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DespesaService {

    private static final String TIPO_DESPESA_FALLBACK = "Despesas Gerais";

    private final DespesaRepository despesaRepository;
    private final TipoDespesaRepository tipoDespesaRepository;
    private final PlanoContasGerencialRepository planoContasRepository;
    private final CentroCustoRepository centroCustoRepository;
    private final ClassificacaoFinanceiraService classificacaoFinanceiraService;
    private final FinancialAuditTrailService financialAuditTrailService;
    private final AuditTrailService auditTrailService;
    private final PrivacyGuardService privacyGuardService;

    public DespesaService(
            DespesaRepository despesaRepository,
            TipoDespesaRepository tipoDespesaRepository,
            PlanoContasGerencialRepository planoContasRepository,
            CentroCustoRepository centroCustoRepository,
            ClassificacaoFinanceiraService classificacaoFinanceiraService,
            FinancialAuditTrailService financialAuditTrailService,
            AuditTrailService auditTrailService,
            PrivacyGuardService privacyGuardService) {
        this.despesaRepository = despesaRepository;
        this.tipoDespesaRepository = tipoDespesaRepository;
        this.planoContasRepository = planoContasRepository;
        this.centroCustoRepository = centroCustoRepository;
        this.classificacaoFinanceiraService = classificacaoFinanceiraService;
        this.financialAuditTrailService = financialAuditTrailService;
        this.auditTrailService = auditTrailService;
        this.privacyGuardService = privacyGuardService;
    }

    @Transactional(readOnly = true)
    public List<DespesaResponseDTO> listarTodos(Long empresaId, Long filialId) {
        requireTenant(empresaId, filialId);
        List<Despesa> despesas = despesaRepository.findByEmpresaIdAndFilialIdOrderByDataVencimentoDesc(empresaId, filialId);
        Map<Long, PlanoContasGerencial> contas = mapContas(empresaId, filialId);
        Map<Long, CentroCusto> centros = mapCentros(empresaId, filialId);
        return despesas.stream()
                .map(d -> toResponse(d, contas, centros))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<DespesaResponseDTO> buscarPorId(Long id, Long empresaId, Long filialId) {
        requireTenant(empresaId, filialId);
        return despesaRepository.findByIdDespesaAndEmpresaIdAndFilialId(id, empresaId, filialId)
                .map(d -> toResponse(d, mapContas(empresaId, filialId), mapCentros(empresaId, filialId)));
    }

    @Transactional
    public DespesaResponseDTO criar(
            DespesaRequestDTO body,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        TipoDespesa tipo = resolverTipoDespesa(body, empresaId, filialId);
        validarClassificacao(body.planoContaId(), body.centroCustoId(), empresaId, filialId);

        LocalDate dataEmissao = body.dataEmissao() != null ? body.dataEmissao() : LocalDate.now();
        LocalDate dataCompetencia = body.dataCompetencia() != null ? body.dataCompetencia() : dataEmissao;
        LocalDate dataVencimento = body.dataVencimento() != null ? body.dataVencimento() : dataCompetencia;

        Despesa despesa = new Despesa();
        despesa.setEmpresaId(empresaId);
        despesa.setFilialId(filialId);
        despesa.setDescricao(normalizeRequired(body.descricao(), "Descricao da despesa e obrigatoria."));
        despesa.setValor(body.valor());
        despesa.setDataEmissao(dataEmissao);
        despesa.setDataCompetencia(dataCompetencia);
        despesa.setDataVencimento(dataVencimento);
        despesa.setDataPagamento(null);
        despesa.setTipoDespesa(tipo);
        despesa.setStatus(1);
        despesa.setPlanoContaId(body.planoContaId());
        despesa.setCentroCustoId(body.centroCustoId());

        if (despesa.getPlanoContaId() == null) {
            classificacaoFinanceiraService.classificarAutomaticamenteDespesa(despesa);
        }

        Despesa salva = despesaRepository.save(despesa);
        registrarAuditoriaDespesa(
                "DESPESA_CRIADA",
                salva,
                null,
                resumoDespesa(salva),
                "Criacao de despesa no tenant.",
                empresaId,
                filialId,
                request);
        return toResponse(salva, mapContas(empresaId, filialId), mapCentros(empresaId, filialId));
    }

    @Transactional
    public DespesaResponseDTO atualizar(
            Long id,
            DespesaRequestDTO body,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        Despesa existente = despesaRepository.findByIdDespesaAndEmpresaIdAndFilialId(id, empresaId, filialId)
                .orElseThrow(() -> new EntityNotFoundException("Despesa nao encontrada."));
        if (existente.getStatus() == 0 || existente.getDataPagamento() != null) {
            throw new IllegalArgumentException("Despesa paga ou cancelada nao pode ser alterada diretamente.");
        }

        TipoDespesa tipo = resolverTipoDespesa(body, empresaId, filialId);
        validarClassificacao(body.planoContaId(), body.centroCustoId(), empresaId, filialId);

        String anterior = resumoDespesa(existente);
        LocalDate dataEmissao = body.dataEmissao() != null
                ? body.dataEmissao()
                : (existente.getDataEmissao() != null ? existente.getDataEmissao() : LocalDate.now());
        LocalDate dataCompetencia = body.dataCompetencia() != null
                ? body.dataCompetencia()
                : (existente.getDataCompetencia() != null ? existente.getDataCompetencia() : dataEmissao);
        LocalDate dataVencimento = body.dataVencimento() != null
                ? body.dataVencimento()
                : (existente.getDataVencimento() != null ? existente.getDataVencimento() : dataCompetencia);

        existente.setDescricao(normalizeRequired(body.descricao(), "Descricao da despesa e obrigatoria."));
        existente.setValor(body.valor());
        existente.setDataEmissao(dataEmissao);
        existente.setDataCompetencia(dataCompetencia);
        existente.setDataVencimento(dataVencimento);
        existente.setTipoDespesa(tipo);
        if (body.planoContaId() != null) {
            existente.setPlanoContaId(body.planoContaId());
        }
        if (body.centroCustoId() != null) {
            existente.setCentroCustoId(body.centroCustoId());
        }

        if (existente.getPlanoContaId() == null) {
            classificacaoFinanceiraService.classificarAutomaticamenteDespesa(existente);
        }

        Despesa salva = despesaRepository.save(existente);
        registrarAuditoriaDespesa(
                "DESPESA_ATUALIZADA",
                salva,
                anterior,
                resumoDespesa(salva),
                "Atualizacao de despesa no tenant.",
                empresaId,
                filialId,
                request);
        return toResponse(salva, mapContas(empresaId, filialId), mapCentros(empresaId, filialId));
    }

    @Transactional
    public DespesaResponseDTO pagar(
            Long id,
            LocalDate dataPagamento,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        Despesa despesa = despesaRepository.findByIdDespesaAndEmpresaIdAndFilialId(id, empresaId, filialId)
                .orElseThrow(() -> new EntityNotFoundException("Despesa nao encontrada."));
        if (despesa.getStatus() == 0) {
            throw new IllegalArgumentException("Despesa cancelada nao pode ser quitada.");
        }
        if (despesa.getDataPagamento() != null) {
            throw new IllegalArgumentException("Despesa ja foi paga e nao pode ser quitada novamente.");
        }
        String anterior = resumoDespesa(despesa);
        despesa.setDataPagamento(dataPagamento != null ? dataPagamento : LocalDate.now());
        Despesa salva = despesaRepository.save(despesa);
        registrarAuditoriaDespesa(
                "DESPESA_PAGA",
                salva,
                anterior,
                resumoDespesa(salva),
                "Liquidacao/pagamento de despesa.",
                empresaId,
                filialId,
                request);
        return toResponse(salva, mapContas(empresaId, filialId), mapCentros(empresaId, filialId));
    }

    @Transactional
    public DespesaResponseDTO cancelar(
            Long id,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        Despesa despesa = despesaRepository.findByIdDespesaAndEmpresaIdAndFilialId(id, empresaId, filialId)
                .orElseThrow(() -> new EntityNotFoundException("Despesa nao encontrada."));
        if (despesa.getStatus() == 0) {
            throw new IllegalArgumentException("Despesa ja esta cancelada.");
        }
        String anterior = resumoDespesa(despesa);
        despesa.setStatus(0);
        Despesa salva = despesaRepository.save(despesa);
        registrarAuditoriaDespesa(
                "DESPESA_CANCELADA",
                salva,
                anterior,
                resumoDespesa(salva),
                "Cancelamento de despesa no tenant.",
                empresaId,
                filialId,
                request);
        return toResponse(salva, mapContas(empresaId, filialId), mapCentros(empresaId, filialId));
    }

    @Transactional(readOnly = true)
    public List<TipoDespesaResponseDTO> listarTipos(Boolean somenteAtivos, Long empresaId, Long filialId) {
        requireTenant(empresaId, filialId);
        List<TipoDespesa> tipos = Boolean.FALSE.equals(somenteAtivos)
                ? tipoDespesaRepository.findVisibleToTenant(empresaId, filialId)
                : tipoDespesaRepository.findVisibleActiveToTenant(empresaId, filialId);
        Map<Long, PlanoContasGerencial> contas = mapContas(empresaId, filialId);
        Map<Long, CentroCusto> centros = mapCentros(empresaId, filialId);
        return tipos.stream()
                .map(t -> toTipoResponse(t, contas, centros))
                .toList();
    }

    @Transactional
    public TipoDespesaResponseDTO criarTipo(
            TipoDespesaRequestDTO body,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        String nome = normalizeRequired(body.nome(), "Nome do tipo de despesa e obrigatorio.");
        if (tipoDespesaRepository.existsByNomeIgnoreCaseAndEmpresaIdAndFilialId(nome, empresaId, filialId)) {
            throw new IllegalArgumentException("Ja existe tipo de despesa com este nome no tenant.");
        }
        validarClassificacao(body.planoContaId(), body.centroCustoId(), empresaId, filialId);

        TipoDespesa tipo = new TipoDespesa();
        tipo.setNome(nome);
        tipo.setStatus(body.status() == null ? 1 : body.status());
        tipo.setEmpresaId(empresaId);
        tipo.setFilialId(filialId);
        tipo.setNaturezaDespesa(trimToNull(body.naturezaDespesa()));
        tipo.setPlanoContaId(body.planoContaId());
        tipo.setCentroCustoId(body.centroCustoId());

        TipoDespesa salvo = tipoDespesaRepository.save(tipo);
        registrarAuditoriaTipo(
                "TIPO_DESPESA_CRIADO",
                salvo,
                null,
                resumoTipo(salvo),
                "Criacao de tipo de despesa no tenant.",
                empresaId,
                filialId,
                request);
        return toTipoResponse(salvo, mapContas(empresaId, filialId), mapCentros(empresaId, filialId));
    }

    @Transactional
    public TipoDespesaResponseDTO atualizarTipo(
            Long id,
            TipoDespesaRequestDTO body,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        TipoDespesa existente = tipoDespesaRepository.findVisibleById(id, empresaId, filialId)
                .orElseThrow(() -> new EntityNotFoundException("Tipo de despesa nao encontrado."));
        if (existente.getEmpresaId() == null) {
            throw new AccessDeniedException("Tipos globais do catalogo nao podem ser alterados pelo tenant.");
        }
        String nome = normalizeRequired(body.nome(), "Nome do tipo de despesa e obrigatorio.");
        if (tipoDespesaRepository.existsByNomeIgnoreCaseAndEmpresaIdAndFilialIdAndIdTipoDespesaNot(
                nome, empresaId, filialId, id)) {
            throw new IllegalArgumentException("Ja existe outro tipo de despesa com este nome no tenant.");
        }
        validarClassificacao(body.planoContaId(), body.centroCustoId(), empresaId, filialId);

        String anterior = resumoTipo(existente);
        existente.setNome(nome);
        if (body.status() != null) {
            existente.setStatus(body.status());
        }
        existente.setNaturezaDespesa(trimToNull(body.naturezaDespesa()));
        existente.setPlanoContaId(body.planoContaId());
        existente.setCentroCustoId(body.centroCustoId());

        TipoDespesa salvo = tipoDespesaRepository.save(existente);
        registrarAuditoriaTipo(
                "TIPO_DESPESA_ATUALIZADO",
                salvo,
                anterior,
                resumoTipo(salvo),
                "Atualizacao de tipo de despesa no tenant.",
                empresaId,
                filialId,
                request);
        return toTipoResponse(salvo, mapContas(empresaId, filialId), mapCentros(empresaId, filialId));
    }

    private TipoDespesa resolverTipoDespesa(DespesaRequestDTO body, Long empresaId, Long filialId) {
        Long tipoId = body.tipoDespesaId();
        if (tipoId == null && body.tipoDespesa() != null) {
            tipoId = body.tipoDespesa().idTipoDespesa();
        }
        if (tipoId != null) {
            return tipoDespesaRepository.findVisibleById(tipoId, empresaId, filialId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Tipo de despesa nao encontrado no escopo da empresa/filial."));
        }
        return tipoDespesaRepository
                .findByNomeIgnoreCaseAndEmpresaIdAndFilialId(TIPO_DESPESA_FALLBACK, empresaId, filialId)
                .or(() -> tipoDespesaRepository.findByNomeIgnoreCaseAndEmpresaIdIsNull(TIPO_DESPESA_FALLBACK))
                .orElseGet(() -> {
                    TipoDespesa padrao = new TipoDespesa();
                    padrao.setNome(TIPO_DESPESA_FALLBACK);
                    padrao.setStatus(1);
                    padrao.setEmpresaId(empresaId);
                    padrao.setFilialId(filialId);
                    return tipoDespesaRepository.save(padrao);
                });
    }

    private void validarClassificacao(Long planoContaId, Long centroCustoId, Long empresaId, Long filialId) {
        if (planoContaId != null) {
            classificacaoFinanceiraService.validarContaAnaliticaAtiva(
                    planoContaId, empresaId, filialId, Model.OrigemClassificacaoFinanceira.DESPESA);
        }
        if (centroCustoId != null) {
            classificacaoFinanceiraService.validarCentroAtivo(centroCustoId, empresaId, filialId);
        }
    }

    private Map<Long, PlanoContasGerencial> mapContas(Long empresaId, Long filialId) {
        Map<Long, PlanoContasGerencial> map = new HashMap<>();
        for (PlanoContasGerencial c : planoContasRepository
                .findByEmpresaIdAndFilialIdOrderByOrdemExibicaoAscCodigoAsc(empresaId, filialId)) {
            map.put(c.getIdPlanoConta(), c);
        }
        return map;
    }

    private Map<Long, CentroCusto> mapCentros(Long empresaId, Long filialId) {
        Map<Long, CentroCusto> map = new HashMap<>();
        for (CentroCusto cc : centroCustoRepository.findByEmpresaIdAndFilialIdOrderByNomeAsc(empresaId, filialId)) {
            map.put(cc.getIdCentroCusto(), cc);
        }
        return map;
    }

    private DespesaResponseDTO toResponse(
            Despesa d,
            Map<Long, PlanoContasGerencial> contas,
            Map<Long, CentroCusto> centros) {
        TipoDespesaResponseDTO tipoDto = d.getTipoDespesa() == null
                ? null
                : toTipoResponse(d.getTipoDespesa(), contas, centros);
        PlanoContasGerencial conta = d.getPlanoContaId() == null ? null : contas.get(d.getPlanoContaId());
        CentroCusto cc = d.getCentroCustoId() == null ? null : centros.get(d.getCentroCustoId());
        return new DespesaResponseDTO(
                d.getIdDespesa(),
                d.getDescricao(),
                d.getValor(),
                d.getDataVencimento(),
                d.getDataPagamento(),
                d.getDataCompetencia(),
                d.getDataEmissao(),
                d.getStatus(),
                d.getEmpresaId(),
                d.getFilialId(),
                d.getTipoDespesa() == null ? null : d.getTipoDespesa().getIdTipoDespesa(),
                d.getTipoDespesa() == null ? null : d.getTipoDespesa().getNome(),
                tipoDto,
                d.getPlanoContaId(),
                conta == null ? null : conta.getCodigo(),
                conta == null ? null : conta.getNome(),
                d.getCentroCustoId(),
                cc == null ? null : cc.getCodigo(),
                cc == null ? null : cc.getNome());
    }

    private TipoDespesaResponseDTO toTipoResponse(
            TipoDespesa t,
            Map<Long, PlanoContasGerencial> contas,
            Map<Long, CentroCusto> centros) {
        PlanoContasGerencial conta = t.getPlanoContaId() == null ? null : contas.get(t.getPlanoContaId());
        CentroCusto cc = t.getCentroCustoId() == null ? null : centros.get(t.getCentroCustoId());
        return new TipoDespesaResponseDTO(
                t.getIdTipoDespesa(),
                t.getNome(),
                t.getStatus(),
                t.getEmpresaId(),
                t.getFilialId(),
                t.getNaturezaDespesa(),
                t.getPlanoContaId(),
                conta == null ? null : conta.getCodigo(),
                conta == null ? null : conta.getNome(),
                t.getCentroCustoId(),
                cc == null ? null : cc.getCodigo(),
                cc == null ? null : cc.getNome());
    }

    private void registrarAuditoriaDespesa(
            String evento,
            Despesa d,
            String anterior,
            String novo,
            String justificativa,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("idDespesa", d.getIdDespesa());
        metadata.put("valor", d.getValor());
        metadata.put("status", d.getStatus());
        metadata.put("dataCompetencia", String.valueOf(d.getDataCompetencia()));
        metadata.put("dataPagamento", String.valueOf(d.getDataPagamento()));
        metadata.put("planoContaId", d.getPlanoContaId());
        metadata.put("centroCustoId", d.getCentroCustoId());
        financialAuditTrailService.record(
                evento,
                "Despesa",
                d.getIdDespesa(),
                privacyGuardService.currentUserLogin(request),
                anterior,
                novo,
                metadata,
                empresaId,
                filialId);
        auditTrailService.registrarPrivacidade(
                evento,
                "Despesa",
                d.getIdDespesa(),
                "Financeiro",
                anterior,
                novo,
                request,
                justificativa);
    }

    private void registrarAuditoriaTipo(
            String evento,
            TipoDespesa t,
            String anterior,
            String novo,
            String justificativa,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("idTipoDespesa", t.getIdTipoDespesa());
        metadata.put("nome", t.getNome());
        metadata.put("status", t.getStatus());
        metadata.put("planoContaId", t.getPlanoContaId());
        metadata.put("centroCustoId", t.getCentroCustoId());
        financialAuditTrailService.record(
                evento,
                "TipoDespesa",
                t.getIdTipoDespesa(),
                privacyGuardService.currentUserLogin(request),
                anterior,
                novo,
                metadata,
                empresaId,
                filialId);
        auditTrailService.registrarPrivacidade(
                evento,
                "TipoDespesa",
                t.getIdTipoDespesa(),
                "Financeiro",
                anterior,
                novo,
                request,
                justificativa);
    }

    private void requireTenant(Long empresaId, Long filialId) {
        if (empresaId == null || filialId == null) {
            throw new IllegalArgumentException("Empresa e filial ativas sao obrigatorias.");
        }
    }

    private String normalizeRequired(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String resumoDespesa(Despesa d) {
        return "id=" + d.getIdDespesa()
                + ";valor=" + d.getValor()
                + ";status=" + d.getStatus()
                + ";pag=" + d.getDataPagamento()
                + ";planoConta=" + d.getPlanoContaId();
    }

    private String resumoTipo(TipoDespesa t) {
        return "id=" + t.getIdTipoDespesa()
                + ";nome=" + t.getNome()
                + ";status=" + t.getStatus()
                + ";planoConta=" + t.getPlanoContaId();
    }
}
