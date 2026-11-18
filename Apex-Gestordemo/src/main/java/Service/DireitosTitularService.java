package Service;

import DTO.EnderecoResumoDTO;
import DTO.TitularDataRequestDTO;
import DTO.TitularDataResponseDTO;
import Model.Cliente;
import Model.SolicitacaoTitularDados;
import Repository.ClienteRepository;
import Repository.SolicitacaoTitularDadosRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DireitosTitularService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private SolicitacaoTitularDadosRepository solicitacaoRepository;

    @Autowired
    private PrivacyGuardService privacyGuardService;

    @Autowired
    private TenantScopeService tenantScopeService;

    @Autowired
    private AuditTrailService auditTrailService;

    public TitularDataResponseDTO registrarSolicitacao(TitularDataRequestDTO request, HttpServletRequest httpRequest) {
        privacyGuardService.requirePrivacyAdmin(httpRequest, "registrar solicitacao de titular");
        SolicitacaoTitularDados solicitacao = new SolicitacaoTitularDados();
        solicitacao.setTitularId(required(request.titularId(), "Titular e obrigatorio."));
        solicitacao.setTipoTitular(defaultText(request.tipoTitular(), "CLIENTE"));
        solicitacao.setTipo(required(request.tipo(), "Tipo de solicitacao e obrigatorio."));
        solicitacao.setJustificativa(defaultText(request.justificativa(), "Solicitacao administrativa registrada."));
        solicitacao.setSolicitadoPor(privacyGuardService.currentUserLogin(httpRequest));
        solicitacao.setEmpresaId(privacyGuardService.currentEmpresaId(httpRequest));
        solicitacao.setFilialId(privacyGuardService.currentFilialId(httpRequest));

        SolicitacaoTitularDados salva = solicitacaoRepository.save(solicitacao);
        auditTrailService.registrarPrivacidade(
                "SOLICITACAO_TITULAR_REGISTRADA",
                "SolicitacaoTitularDados",
                salva.getIdSolicitacao(),
                "Privacidade",
                null,
                "tipo=" + salva.getTipo() + ";titular=" + salva.getTitularId(),
                httpRequest,
                salva.getJustificativa());
        return response(salva);
    }

    public Map<String, Object> exportarCliente(Long clienteId, HttpServletRequest httpRequest) {
        privacyGuardService.requirePrivacyAdmin(httpRequest, "exportar dados pessoais");
        Cliente cliente = clienteRepository.findByIdClienteAndEmpresaIdAndFilialId(
                        clienteId, tenantScopeService.empresaId(httpRequest), tenantScopeService.filialId(httpRequest))
                .orElseThrow(() -> new NoSuchElementException("Cliente nao encontrado."));

        Map<String, Object> export = new LinkedHashMap<>();
        export.put("titularTipo", "CLIENTE");
        export.put("titularId", cliente.getIdCliente());
        export.put("nomeRazao", cliente.getNomeRazao());
        export.put("telefone", cliente.getTelefone());
        export.put("tipoDocumento", cliente.getTipoDocumento());
        export.put("cpfCnpj", cliente.getCpfCnpj());
        export.put("status", cliente.getStatus());
        export.put("dataCadastro", cliente.getDataCadastro());
        export.put("enderecos", cliente.getEnderecos() == null ? java.util.List.of()
                : cliente.getEnderecos().stream().map(EnderecoResumoDTO::from).collect(Collectors.toList()));
        export.put("anonimizadoEm", cliente.getAnonimizadoEm());
        export.put("motivoRetencao", cliente.getMotivoRetencao());
        export.put("baseLegal", cliente.getBaseLegal());

        auditTrailService.registrarPrivacidade(
                "DADOS_PESSOAIS_EXPORTADOS",
                "Cliente",
                cliente.getIdCliente(),
                "Clientes",
                null,
                "exportacao=CLIENTE;campos=basicos,enderecos,lgpd",
                httpRequest,
                "Exportacao estruturada por solicitacao administrativa controlada.");
        return export;
    }

    public TitularDataResponseDTO anonimizarCliente(Long clienteId, TitularDataRequestDTO request,
            HttpServletRequest httpRequest) {
        privacyGuardService.requirePrivacyAdmin(httpRequest, "anonimizar dados pessoais");
        Cliente cliente = clienteRepository.findByIdClienteAndEmpresaIdAndFilialId(
                        clienteId, tenantScopeService.empresaId(httpRequest), tenantScopeService.filialId(httpRequest))
                .orElseThrow(() -> new NoSuchElementException("Cliente nao encontrado."));
        String anterior = resumoCliente(cliente);
        String actor = privacyGuardService.currentUserLogin(httpRequest);
        String justificativa = defaultText(request.justificativa(),
                "Anonimizacao executada por solicitacao administrativa validada.");

        cliente.setNomeRazao("Titular anonimizado " + cliente.getIdCliente());
        cliente.setTelefone("00000000000");
        cliente.setTipoDocumento(0);
        cliente.setCpfCnpj(String.format("ANON%010d", cliente.getIdCliente()));
        cliente.setStatus(0);
        cliente.setDeletedAt(LocalDateTime.now());
        cliente.setDeletedBy(actor);
        cliente.setAnonimizadoEm(LocalDateTime.now());
        cliente.setAnonimizadoPor(actor);
        cliente.setMotivoRetencao(justificativa);
        cliente.setBaseLegal("Retencao operacional/fiscal; dados pessoais anonimizados.");
        if (cliente.getEnderecos() != null) {
            cliente.getEnderecos().clear();
        }
        Cliente salvo = clienteRepository.save(cliente);

        SolicitacaoTitularDados solicitacao = new SolicitacaoTitularDados();
        solicitacao.setTitularId(String.valueOf(clienteId));
        solicitacao.setTipoTitular("CLIENTE");
        solicitacao.setTipo(defaultText(request.tipo(), "ANONIMIZACAO"));
        solicitacao.setStatus("EXECUTADA");
        solicitacao.setJustificativa(justificativa);
        solicitacao.setSolicitadoPor(actor);
        solicitacao.setExecutadoPor(actor);
        solicitacao.setExecutadoEm(LocalDateTime.now());
        solicitacao.setEmpresaId(privacyGuardService.currentEmpresaId(httpRequest));
        solicitacao.setFilialId(privacyGuardService.currentFilialId(httpRequest));
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
