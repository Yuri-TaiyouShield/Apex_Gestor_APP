package Service;

import DTO.CentroCustoRequestDTO;
import DTO.CentroCustoResponseDTO;
import Model.CentroCusto;
import Model.TipoCentroCusto;
import Repository.CentroCustoRepository;
import Repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CentroCustoService {

    private static final List<SeedCentroCusto> CENTROS_PADRAO = List.of(
            new SeedCentroCusto("CC-GERAL", "Geral / Corporativo", "Centro de custo geral do tenant", TipoCentroCusto.GERAL),
            new SeedCentroCusto("CC-LOJA", "Operacao de Loja / PDV", "Custos e receitas da operacao de loja", TipoCentroCusto.LOJA),
            new SeedCentroCusto("CC-ADM", "Administrativo", "Despesas administrativas e escritorio", TipoCentroCusto.ADMINISTRATIVO),
            new SeedCentroCusto("CC-COM", "Comercial e Vendas", "Despesas comerciais, comissoes e marketing", TipoCentroCusto.COMERCIAL)
    );

    private final CentroCustoRepository centroCustoRepository;
    private final UsuarioRepository usuarioRepository;
    private final FinancialAuditTrailService financialAuditTrailService;
    private final AuditTrailService auditTrailService;
    private final PrivacyGuardService privacyGuardService;

    public CentroCustoService(
            CentroCustoRepository centroCustoRepository,
            UsuarioRepository usuarioRepository,
            FinancialAuditTrailService financialAuditTrailService,
            AuditTrailService auditTrailService,
            PrivacyGuardService privacyGuardService) {
        this.centroCustoRepository = centroCustoRepository;
        this.usuarioRepository = usuarioRepository;
        this.financialAuditTrailService = financialAuditTrailService;
        this.auditTrailService = auditTrailService;
        this.privacyGuardService = privacyGuardService;
    }

    @Transactional(readOnly = true)
    public List<CentroCustoResponseDTO> listar(
            Boolean somenteAtivos,
            TipoCentroCusto tipoOperacional,
            Long empresaId,
            Long filialId) {
        requireTenant(empresaId, filialId);
        List<CentroCusto> base = Boolean.TRUE.equals(somenteAtivos)
                ? centroCustoRepository.findByEmpresaIdAndFilialIdAndAtivoTrueOrderByNomeAsc(empresaId, filialId)
                : centroCustoRepository.findByEmpresaIdAndFilialIdOrderByNomeAsc(empresaId, filialId);
        return base.stream()
                .filter(c -> tipoOperacional == null || c.getTipoOperacional() == tipoOperacional)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CentroCustoResponseDTO buscarPorId(Long id, Long empresaId, Long filialId) {
        requireTenant(empresaId, filialId);
        CentroCusto entity = centroCustoRepository.findByIdCentroCustoAndEmpresaIdAndFilialId(id, empresaId, filialId)
                .orElseThrow(() -> new EntityNotFoundException("Centro de custo nao encontrado."));
        return toResponse(entity);
    }

    @Transactional
    public List<CentroCustoResponseDTO> inicializarPadrao(Long empresaId, Long filialId, HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        List<CentroCusto> criados = garantirPadraoInterno(empresaId, filialId);
        List<CentroCusto> todos = centroCustoRepository.findByEmpresaIdAndFilialIdOrderByNomeAsc(empresaId, filialId);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("criados", criados.size());
        metadata.put("total", todos.size());
        financialAuditTrailService.record(
                "CENTRO_CUSTO_BOOTSTRAP",
                "CentroCusto",
                null,
                actorLogin(request),
                null,
                "criados=" + criados.size() + ";total=" + todos.size(),
                metadata,
                empresaId,
                filialId);
        auditTrailService.registrarPrivacidade(
                "CENTRO_CUSTO_BOOTSTRAP",
                "CentroCusto",
                null,
                "Financeiro",
                null,
                "criados=" + criados.size() + ";total=" + todos.size(),
                request,
                "Inicializacao idempotente de centros de custo padrao.");
        return todos.stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<CentroCusto> garantirPadraoInterno(Long empresaId, Long filialId) {
        requireTenant(empresaId, filialId);
        List<CentroCusto> criados = new ArrayList<>();
        for (SeedCentroCusto seed : CENTROS_PADRAO) {
            Optional<CentroCusto> existente = centroCustoRepository
                    .findByCodigoAndEmpresaIdAndFilialId(seed.codigo(), empresaId, filialId);
            if (existente.isEmpty()) {
                CentroCusto novo = new CentroCusto();
                novo.setEmpresaId(empresaId);
                novo.setFilialId(filialId);
                novo.setCodigo(seed.codigo());
                novo.setNome(seed.nome());
                novo.setDescricao(seed.descricao());
                novo.setTipoOperacional(seed.tipoOperacional());
                novo.setAtivo(true);
                criados.add(centroCustoRepository.save(novo));
            }
        }
        return criados;
    }

    @Transactional
    public CentroCustoResponseDTO criar(
            CentroCustoRequestDTO body,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        String codigo = normalizeRequired(body.codigo(), "Codigo do centro de custo e obrigatorio.");
        String nome = normalizeRequired(body.nome(), "Nome do centro de custo e obrigatorio.");
        if (centroCustoRepository.existsByCodigoAndEmpresaIdAndFilialId(codigo, empresaId, filialId)) {
            throw new IllegalArgumentException("Ja existe centro de custo com o codigo informado.");
        }
        validarResponsavel(body.responsavelUsuarioId(), empresaId, filialId);

        CentroCusto entity = new CentroCusto();
        entity.setEmpresaId(empresaId);
        entity.setFilialId(filialId);
        entity.setCodigo(codigo);
        entity.setNome(nome);
        entity.setDescricao(trimToNull(body.descricao()));
        entity.setTipoOperacional(body.tipoOperacional());
        entity.setResponsavelUsuarioId(body.responsavelUsuarioId());
        entity.setAtivo(body.ativo() == null || body.ativo());

        CentroCusto salvo = centroCustoRepository.save(entity);
        registrarAuditoria(
                "CENTRO_CUSTO_CRIADO",
                salvo,
                null,
                resumo(salvo),
                "Criacao de centro de custo gerencial.",
                empresaId,
                filialId,
                request);
        return toResponse(salvo);
    }

    @Transactional
    public CentroCustoResponseDTO atualizar(
            Long id,
            CentroCustoRequestDTO body,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        CentroCusto existente = centroCustoRepository.findByIdCentroCustoAndEmpresaIdAndFilialId(id, empresaId, filialId)
                .orElseThrow(() -> new EntityNotFoundException("Centro de custo nao encontrado."));
        String codigo = normalizeRequired(body.codigo(), "Codigo do centro de custo e obrigatorio.");
        String nome = normalizeRequired(body.nome(), "Nome do centro de custo e obrigatorio.");
        if (centroCustoRepository.existsByCodigoAndEmpresaIdAndFilialIdAndIdCentroCustoNot(
                codigo, empresaId, filialId, id)) {
            throw new IllegalArgumentException("Ja existe outro centro de custo com o codigo informado.");
        }
        validarResponsavel(body.responsavelUsuarioId(), empresaId, filialId);

        String anterior = resumo(existente);
        existente.setCodigo(codigo);
        existente.setNome(nome);
        existente.setDescricao(trimToNull(body.descricao()));
        existente.setTipoOperacional(body.tipoOperacional());
        existente.setResponsavelUsuarioId(body.responsavelUsuarioId());
        if (body.ativo() != null) {
            existente.setAtivo(body.ativo());
        }

        CentroCusto salvo = centroCustoRepository.save(existente);
        registrarAuditoria(
                "CENTRO_CUSTO_ATUALIZADO",
                salvo,
                anterior,
                resumo(salvo),
                "Atualizacao de centro de custo gerencial.",
                empresaId,
                filialId,
                request);
        return toResponse(salvo);
    }

    @Transactional
    public CentroCustoResponseDTO alterarStatus(
            Long id,
            boolean ativo,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        CentroCusto existente = centroCustoRepository.findByIdCentroCustoAndEmpresaIdAndFilialId(id, empresaId, filialId)
                .orElseThrow(() -> new EntityNotFoundException("Centro de custo nao encontrado."));
        String anterior = resumo(existente);
        existente.setAtivo(ativo);
        CentroCusto salvo = centroCustoRepository.save(existente);
        registrarAuditoria(
                ativo ? "CENTRO_CUSTO_ATIVADO" : "CENTRO_CUSTO_INATIVADO",
                salvo,
                anterior,
                resumo(salvo),
                "Alteracao de status do centro de custo.",
                empresaId,
                filialId,
                request);
        return toResponse(salvo);
    }

    private void validarResponsavel(Long responsavelUsuarioId, Long empresaId, Long filialId) {
        if (responsavelUsuarioId != null) {
            usuarioRepository.findByIdUsuarioAndEmpresaIdAndFilialId(responsavelUsuarioId, empresaId, filialId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario responsavel nao encontrado no tenant."));
        }
    }

    private void registrarAuditoria(
            String evento,
            CentroCusto salvo,
            String anterior,
            String novo,
            String justificativa,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("idCentroCusto", salvo.getIdCentroCusto());
        metadata.put("codigo", salvo.getCodigo());
        metadata.put("nome", salvo.getNome());
        metadata.put("tipoOperacional", salvo.getTipoOperacional().name());
        metadata.put("ativo", salvo.isAtivo());
        financialAuditTrailService.record(
                evento,
                "CentroCusto",
                salvo.getIdCentroCusto(),
                actorLogin(request),
                anterior,
                novo,
                metadata,
                empresaId,
                filialId);
        auditTrailService.registrarPrivacidade(
                evento,
                "CentroCusto",
                salvo.getIdCentroCusto(),
                "Financeiro",
                anterior,
                novo,
                request,
                justificativa);
    }

    private CentroCustoResponseDTO toResponse(CentroCusto entity) {
        return new CentroCustoResponseDTO(
                entity.getIdCentroCusto(),
                entity.getCodigo(),
                entity.getNome(),
                entity.getDescricao(),
                entity.getTipoOperacional(),
                entity.getResponsavelUsuarioId(),
                entity.isAtivo(),
                entity.getCriadoEm(),
                entity.getAtualizadoEm());
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

    private String actorLogin(HttpServletRequest request) {
        return privacyGuardService.currentUserLogin(request);
    }

    private String resumo(CentroCusto c) {
        return "codigo=" + c.getCodigo()
                + ";nome=" + c.getNome()
                + ";tipo=" + c.getTipoOperacional()
                + ";ativo=" + c.isAtivo();
    }

    private record SeedCentroCusto(String codigo, String nome, String descricao, TipoCentroCusto tipoOperacional) {
    }
}
