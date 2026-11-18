package Service;

import DTO.EnderecoResumoDTO;
import DTO.LgpdConfirmacaoAnonimizacaoRequestDTO;
import DTO.LgpdSimulacaoAnonimizacaoDTO;
import DTO.TitularDataRequestDTO;
import DTO.TitularDataResponseDTO;
import Model.Cliente;
import Model.CreditoStatus;
import Model.FinanceiroStatus;
import Model.SolicitacaoTitularDados;
import Repository.ClienteRepository;
import Repository.ContaReceberRepository;
import Repository.CreditoClienteRepository;
import Repository.SolicitacaoTitularDadosRepository;
import Repository.VendaRepository;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DireitosTitularService {

    private static final int MIN_JUSTIFICATION_LENGTH = 10;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private SolicitacaoTitularDadosRepository solicitacaoRepository;

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private ContaReceberRepository contaReceberRepository;

    @Autowired
    private CreditoClienteRepository creditoClienteRepository;

    @Autowired
    private PrivacyGuardService privacyGuardService;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private TenantScopeService tenantScopeService;

    @Autowired
    private SegregacaoFuncoesService segregacaoFuncoesService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Transactional
    public TitularDataResponseDTO registrarSolicitacao(TitularDataRequestDTO request, HttpServletRequest httpRequest) {
        authorizationService.require(httpRequest, ApexPermission.PERSONAL_DATA_VIEW,
                "Privacidade", "registrar solicitacao de titular");
        privacyGuardService.requirePrivacyOwner(httpRequest, "registrar solicitacao de titular");
        Long empresaId = tenantScopeService.empresaId(httpRequest);
        Long filialId = tenantScopeService.filialId(httpRequest);

        String justificativa = requiredJustification(request.justificativa(),
                "Justificativa formal obrigatoria (minimo 10 caracteres) para registrar solicitacao LGPD.");

        SolicitacaoTitularDados solicitacao = new SolicitacaoTitularDados();
        solicitacao.setTitularId(required(request.titularId(), "Titular e obrigatorio."));
        solicitacao.setTipoTitular(defaultText(request.tipoTitular(), "CLIENTE"));
        solicitacao.setTipo(required(request.tipo(), "Tipo de solicitacao e obrigatorio."));
        solicitacao.setStatus("PENDENTE_APROVACAO");
        solicitacao.setJustificativa(justificativa);
        solicitacao.setSolicitadoPor(privacyGuardService.currentUserLogin(httpRequest));
        solicitacao.setSolicitadoPorUsuarioId(privacyGuardService.currentUserId(httpRequest));
        solicitacao.setEmpresaId(empresaId);
        solicitacao.setFilialId(filialId);

        SolicitacaoTitularDados salva = solicitacaoRepository.save(solicitacao);
        auditTrailService.registrarPrivacidade(
                "SOLICITACAO_TITULAR_REGISTRADA",
                "SolicitacaoTitularDados",
                salva.getIdSolicitacao(),
                "Privacidade",
                null,
                "tipo=" + salva.getTipo() + ";titular=" + salva.getTitularId()
                        + ";solicitanteUid=" + salva.getSolicitadoPorUsuarioId(),
                httpRequest,
                justificativa);
        return response(salva);
    }

    @Transactional
    public Map<String, Object> exportarCliente(
            Long clienteId,
            String justificativaRaw,
            HttpServletRequest httpRequest) {
        authorizationService.require(httpRequest, ApexPermission.PERSONAL_DATA_EXPORT,
                "Privacidade", "exportar dados pessoais de titular");
        privacyGuardService.requirePrivacyOwner(httpRequest, "exportar dados pessoais");
        String justificativa = requiredJustification(justificativaRaw,
                "Justificativa formal obrigatoria (minimo 10 caracteres) para exportar dados pessoais.");

        Long empresaId = tenantScopeService.empresaId(httpRequest);
        Long filialId = tenantScopeService.filialId(httpRequest);
        Cliente cliente = clienteRepository.findByIdClienteAndEmpresaIdAndFilialId(clienteId, empresaId, filialId)
                .orElseThrow(() -> new NoSuchElementException("Cliente nao encontrado."));

        String actor = privacyGuardService.currentUserLogin(httpRequest);
        Long actorUid = privacyGuardService.currentUserId(httpRequest);

        Map<String, Object> export = new LinkedHashMap<>();
        export.put("titularTipo", "CLIENTE");
        export.put("titularId", cliente.getIdCliente());
        export.put("nomeRazao", cliente.getNomeRazao());
        export.put("nomeFantasia", cliente.getNomeFantasia());
        export.put("telefone", cliente.getTelefone());
        export.put("celular", cliente.getCelular());
        export.put("email", cliente.getEmail());
        export.put("tipoDocumento", cliente.getTipoDocumento());
        export.put("cpfCnpj", cliente.getCpfCnpj());
        export.put("rgIe", cliente.getRgIe());
        export.put("status", cliente.getStatus());
        export.put("dataCadastro", cliente.getDataCadastro());
        export.put("enderecos", cliente.getEnderecos() == null ? List.of()
                : cliente.getEnderecos().stream().map(EnderecoResumoDTO::from).collect(Collectors.toList()));
        export.put("anonimizadoEm", cliente.getAnonimizadoEm());
        export.put("motivoRetencao", cliente.getMotivoRetencao());
        export.put("baseLegal", cliente.getBaseLegal());
        export.put("justificativaExportacao", justificativa);
        export.put("exportadoEm", LocalDateTime.now());

        SolicitacaoTitularDados registro = new SolicitacaoTitularDados();
        registro.setTitularId(String.valueOf(cliente.getIdCliente()));
        registro.setTipoTitular("CLIENTE");
        registro.setTipo("EXPORTACAO");
        registro.setStatus("EXECUTADA");
        registro.setJustificativa(justificativa);
        registro.setSolicitadoPor(actor);
        registro.setSolicitadoPorUsuarioId(actorUid);
        registro.setExecutadoPor(actor);
        registro.setExecutadoPorUsuarioId(actorUid);
        registro.setExecutadoEm(LocalDateTime.now());
        registro.setRetencaoLegalValidada(true);
        registro.setEmpresaId(empresaId);
        registro.setFilialId(filialId);
        SolicitacaoTitularDados savedReq = solicitacaoRepository.save(registro);
        export.put("solicitacaoId", savedReq.getIdSolicitacao());

        auditTrailService.registrarPrivacidade(
                "DADOS_PESSOAIS_EXPORTADOS",
                "Cliente",
                cliente.getIdCliente(),
                "Clientes",
                null,
                "exportacao=CLIENTE;solicitacaoId=" + savedReq.getIdSolicitacao(),
                httpRequest,
                justificativa);
        return export;
    }

    @Transactional
    public LgpdSimulacaoAnonimizacaoDTO simularAnonimizacao(Long clienteId, HttpServletRequest httpRequest) {
        authorizationService.require(httpRequest, ApexPermission.PERSONAL_DATA_ANONYMIZE,
                "Privacidade", "simular anonimizacao de titular");
        privacyGuardService.requirePrivacyOwner(httpRequest, "simular anonimizacao de dados pessoais");

        Long empresaId = tenantScopeService.empresaId(httpRequest);
        Long filialId = tenantScopeService.filialId(httpRequest);
        Cliente cliente = clienteRepository.findByIdClienteAndEmpresaIdAndFilialId(clienteId, empresaId, filialId)
                .orElseThrow(() -> new NoSuchElementException("Cliente nao encontrado."));

        LgpdSimulacaoAnonimizacaoDTO simulacao = calcularSimulacao(cliente, empresaId, filialId);
        auditTrailService.registrarPrivacidade(
                "SIMULACAO_ANONIMIZACAO_CLIENTE",
                "Cliente",
                cliente.getIdCliente(),
                "Privacidade",
                null,
                "elegivel=" + simulacao.elegivelAnonimizacaoPessoal()
                        + ";vendas=" + simulacao.vendasVinculadas()
                        + ";contasAberto=" + simulacao.contasReceberEmAberto()
                        + ";creditos=" + simulacao.creditosAtivos(),
                httpRequest,
                "Dry-run obrigatorio antes de anonimizacao de titular LGPD.");
        return simulacao;
    }

    @Transactional
    public TitularDataResponseDTO anonimizarClienteConfirmado(
            Long clienteId,
            LgpdConfirmacaoAnonimizacaoRequestDTO request,
            HttpServletRequest httpRequest) {
        authorizationService.require(httpRequest, ApexPermission.PERSONAL_DATA_ANONYMIZE,
                "Privacidade", "anonimizar dados pessoais de titular");
        privacyGuardService.requirePrivacyOwner(httpRequest, "anonimizar dados pessoais");

        if (request == null || !Boolean.TRUE.equals(request.confirmadoExplicito())) {
            throw new IllegalArgumentException(
                    "Confirmacao explicita (confirmadoExplicito=true) e obrigatoria para anonimizacao irreversivel.");
        }
        String justificativa = requiredJustification(request.justificativa(),
                "Justificativa formal obrigatoria (minimo 10 caracteres) para anonimizacao LGPD.");

        Long empresaId = tenantScopeService.empresaId(httpRequest);
        Long filialId = tenantScopeService.filialId(httpRequest);
        Cliente cliente = clienteRepository.findWithLockByIdClienteAndEmpresaIdAndFilialId(clienteId, empresaId, filialId)
                .orElseThrow(() -> new NoSuchElementException("Cliente nao encontrado."));

        if (cliente.getAnonimizadoEm() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cliente ja foi anonimizado em " + cliente.getAnonimizadoEm() + ".");
        }

        LgpdSimulacaoAnonimizacaoDTO simulacao = calcularSimulacao(cliente, empresaId, filialId);
        if (simulacao.bloqueioOperacionalAtivo()) {
            auditTrailService.registrarPrivacidade(
                    "ANONIMIZACAO_BLOQUEADA_RETENCAO",
                    "Cliente",
                    cliente.getIdCliente(),
                    "Privacidade",
                    null,
                    "contasAberto=" + simulacao.contasReceberEmAberto()
                            + ";creditosAtivos=" + simulacao.creditosAtivos(),
                    httpRequest,
                    "Bloqueio por retencao operacional/financeira ativa.");
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Anonimizacao bloqueada: cliente possui " + simulacao.contasReceberEmAberto()
                            + " conta(s) a receber em aberto e " + simulacao.creditosAtivos()
                            + " credito(s) ativo(s).");
        }

        if (request.simulacaoHash() == null || request.simulacaoHash().isBlank()
                || !simulacao.simulacaoHash().equals(request.simulacaoHash().trim())) {
            throw new IllegalArgumentException(
                    "Hash da simulacao ausente ou divergente. Execute a simulacao antes de confirmar a anonimizacao.");
        }

        SolicitacaoTitularDados solicitacaoExistente = resolverSolicitacaoExistente(
                request.solicitacaoId(), clienteId, empresaId, filialId);
        Long actorUid = privacyGuardService.currentUserId(httpRequest);
        Long submetidoPorUid = solicitacaoExistente != null && solicitacaoExistente.getSolicitadoPorUsuarioId() != null
                ? solicitacaoExistente.getSolicitadoPorUsuarioId()
                : actorUid;

        segregacaoFuncoesService.validarAprovacaoInterna(
                SegregacaoFuncoesService.DOMINIO_LGPD_ANONIMIZACAO,
                submetidoPorUid,
                null,
                justificativa,
                httpRequest);

        String anterior = resumoCliente(cliente);
        String actor = privacyGuardService.currentUserLogin(httpRequest);
        LocalDateTime now = LocalDateTime.now();

        String baseLegal = simulacao.vendasVinculadas() > 0
                ? "Cumprimento de obrigacao legal/fiscal (art. 16, I, LGPD): historico comercial preservado sem PII."
                : "Execucao de eliminacao/anonimizacao a pedido (art. 18, VI, LGPD).";
        String motivoRetencao = simulacao.vendasVinculadas() > 0
                ? justificativa + " | Historico de " + simulacao.vendasVinculadas() + " venda(s) preservado."
                : justificativa;

        cliente.setNomeRazao("Titular anonimizado " + cliente.getIdCliente());
        cliente.setNomeFantasia(null);
        cliente.setTelefone("00000000000");
        cliente.setCelular(null);
        cliente.setEmail(null);
        cliente.setTipoDocumento(0);
        cliente.setCpfCnpj(String.format("ANON%010d", cliente.getIdCliente()));
        cliente.setRgIe(null);
        cliente.setObservacoes(null);
        cliente.setStatus(0);
        cliente.setDeletedAt(now);
        cliente.setDeletedBy(actor);
        cliente.setAnonimizadoEm(now);
        cliente.setAnonimizadoPor(actor);
        cliente.setMotivoRetencao(trim(motivoRetencao, 255));
        cliente.setBaseLegal(trim(baseLegal, 120));
        if (cliente.getEnderecos() != null) {
            cliente.getEnderecos().clear();
        }
        Cliente salvo = clienteRepository.save(cliente);

        SolicitacaoTitularDados solicitacao = solicitacaoExistente != null
                ? solicitacaoExistente
                : new SolicitacaoTitularDados();
        if (solicitacao.getIdSolicitacao() == null) {
            solicitacao.setTitularId(String.valueOf(clienteId));
            solicitacao.setTipoTitular("CLIENTE");
            solicitacao.setTipo("ANONIMIZACAO");
            solicitacao.setSolicitadoPor(actor);
            solicitacao.setSolicitadoPorUsuarioId(actorUid);
            solicitacao.setEmpresaId(empresaId);
            solicitacao.setFilialId(filialId);
        }
        solicitacao.setStatus("EXECUTADA");
        solicitacao.setJustificativa(justificativa);
        solicitacao.setAprovadoPorUsuarioId(actorUid);
        solicitacao.setAprovadoPor(actor);
        solicitacao.setAprovadoEm(now);
        solicitacao.setExecutadoPor(actor);
        solicitacao.setExecutadoPorUsuarioId(actorUid);
        solicitacao.setExecutadoEm(now);
        solicitacao.setRetencaoLegalValidada(true);
        solicitacao.setImpedimentoRetencao(simulacao.vendasVinculadas() > 0
                ? "Historico fiscal preservado (" + simulacao.vendasVinculadas() + " vendas)"
                : null);
        solicitacao.setSimulacaoHash(simulacao.simulacaoHash());
        solicitacao.setConfirmadoExplicito(true);
        SolicitacaoTitularDados salva = solicitacaoRepository.save(solicitacao);

        auditTrailService.registrarPrivacidade(
                "CLIENTE_ANONIMIZADO",
                "Cliente",
                salvo.getIdCliente(),
                "Clientes",
                anterior,
                resumoCliente(salvo),
                httpRequest,
                justificativa);
        return response(salva);
    }

    private SolicitacaoTitularDados resolverSolicitacaoExistente(
            Long solicitacaoId,
            Long clienteId,
            Long empresaId,
            Long filialId) {
        if (solicitacaoId == null) {
            return null;
        }
        SolicitacaoTitularDados existente = solicitacaoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new NoSuchElementException("Solicitacao LGPD nao encontrada."));
        if (!empresaId.equals(existente.getEmpresaId()) || !filialId.equals(existente.getFilialId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solicitacao LGPD pertence a outro tenant/filial.");
        }
        if (!String.valueOf(clienteId).equals(existente.getTitularId())) {
            throw new IllegalArgumentException("Solicitacao LGPD nao corresponde ao cliente informado.");
        }
        if (!"ANONIMIZACAO".equalsIgnoreCase(existente.getTipo())
                && !"EXCLUSAO".equalsIgnoreCase(existente.getTipo())) {
            throw new IllegalArgumentException("Solicitacao LGPD informada nao e do tipo ANONIMIZACAO.");
        }
        if (!"PENDENTE_APROVACAO".equalsIgnoreCase(existente.getStatus())
                && !"RECEBIDA".equalsIgnoreCase(existente.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solicitacao LGPD nao esta pendente de aprovacao (status atual: " + existente.getStatus() + ").");
        }
        return existente;
    }

    private LgpdSimulacaoAnonimizacaoDTO calcularSimulacao(Cliente cliente, Long empresaId, Long filialId) {
        long clienteId = cliente.getIdCliente();
        long vendas = vendaRepository.countByClienteIdClienteAndEmpresaIdAndFilialId(clienteId, empresaId, filialId);
        long contasAberto = contaReceberRepository.countByClienteIdAndEmpresaIdAndFilialIdAndStatusIn(
                clienteId, empresaId, filialId, List.of(FinanceiroStatus.ABERTO, FinanceiroStatus.PARCIAL));
        long creditosAtivos = creditoClienteRepository.countByClienteIdAndEmpresaIdAndFilialIdAndStatusIn(
                clienteId, empresaId, filialId, List.of(CreditoStatus.DISPONIVEL, CreditoStatus.PARCIAL));

        boolean bloqueioOperacional = contasAberto > 0 || creditosAtivos > 0;
        boolean bloqueioFisicoRetencao = vendas > 0 || bloqueioOperacional;
        boolean elegivel = !bloqueioOperacional && cliente.getAnonimizadoEm() == null;

        List<String> obrigacoes = new ArrayList<>();
        if (contasAberto > 0) {
            obrigacoes.add("Contas a receber em aberto/parcial (" + contasAberto + ") impedem anonimizacao imediata.");
        }
        if (creditosAtivos > 0) {
            obrigacoes.add("Creditos ativos do cliente (" + creditosAtivos + ") impedem anonimizacao imediata.");
        }
        if (vendas > 0) {
            obrigacoes.add("Historico fiscal de vendas (" + vendas + ") impede exclusao fisica; metadados mantidos.");
        }
        if (cliente.getAnonimizadoEm() != null) {
            obrigacoes.add("Titular ja anonimizado anteriormente em " + cliente.getAnonimizadoEm() + ".");
        }

        long elegiveis = segregacaoFuncoesService.contarAprovadoresElegiveisAtivos(
                empresaId, SegregacaoFuncoesService.DOMINIO_LGPD_ANONIMIZACAO);
        boolean exigeSegundo = elegiveis >= 2;

        String rawDigestInput = empresaId + "|" + filialId + "|" + clienteId + "|"
                + (cliente.getCpfCnpj() == null ? "" : cliente.getCpfCnpj()) + "|"
                + vendas + "|" + contasAberto + "|" + creditosAtivos;
        String hash = privacyGuardService.sha256(rawDigestInput);

        return new LgpdSimulacaoAnonimizacaoDTO(
                clienteId,
                elegivel,
                bloqueioOperacional,
                bloqueioFisicoRetencao,
                vendas,
                contasAberto,
                creditosAtivos,
                List.copyOf(obrigacoes),
                List.of("nomeRazao", "nomeFantasia", "telefone", "celular", "email", "cpfCnpj", "rgIe",
                        "observacoes", "enderecos"),
                List.of("idCliente", "empresaId", "filialId", "dataCadastro", "baseLegal", "motivoRetencao"),
                exigeSegundo,
                elegiveis,
                hash);
    }

    private TitularDataResponseDTO response(SolicitacaoTitularDados solicitacao) {
        return new TitularDataResponseDTO(
                solicitacao.getIdSolicitacao(),
                solicitacao.getTitularId(),
                solicitacao.getTipoTitular(),
                solicitacao.getTipo(),
                solicitacao.getStatus(),
                solicitacao.getJustificativa(),
                solicitacao.getSolicitadoPor(),
                solicitacao.getSolicitadoEm(),
                solicitacao.getExecutadoEm());
    }

    private String resumoCliente(Cliente cliente) {
        return "id=" + cliente.getIdCliente()
                + ";nome=" + cliente.getNomeRazao()
                + ";documento=" + privacyGuardService.maskDocument(cliente.getCpfCnpj())
                + ";status=" + cliente.getStatus()
                + ";anonimizadoEm=" + cliente.getAnonimizadoEm();
    }

    private String requiredJustification(String value, String message) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.length() < MIN_JUSTIFICATION_LENGTH) {
            throw new IllegalArgumentException(message);
        }
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
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

    private String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
