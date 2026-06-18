package Controller;

import DTO.ClienteRequestDTO;
import DTO.ClienteResponseDTO;
import DTO.ConsentimentoRequestDTO;
import DTO.EnderecoResumoDTO;
import Model.Cliente;
import Model.Endereco;
import Service.AuditTrailService;
import Service.AuthorizationService;
import Service.ClienteService;
import Service.ConsentimentoPrivacidadeService;
import Service.PrivacyGuardService;
import Service.TenantScopeService;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.LinkedHashSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "http://localhost:4200")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;
    @Autowired
    private PrivacyGuardService privacyGuardService;
    @Autowired
    private AuditTrailService auditTrailService;
    @Autowired
    private ConsentimentoPrivacidadeService consentimentoService;
    @Autowired
    private AuthorizationService authorizationService;
    @Autowired
    private TenantScopeService tenantScopeService;

    @GetMapping
    public List<ClienteResponseDTO> listarTodos(HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.CUSTOMER_VIEW, "Clientes", "listar clientes");
        boolean fullAccess = privacyGuardService.canViewSensitiveData(request);
        auditTrailService.registrarPrivacidade(
                fullAccess ? "CLIENTE_LISTA_VISUALIZADA_COMPLETA" : "CLIENTE_LISTA_VISUALIZADA_MASCARADA",
                "Cliente",
                null,
                "Clientes",
                null,
                "total=consulta",
                request,
                "Consulta operacional de clientes.");
        return clienteService.listarTodos(empresaId(request), filialId(request)).stream()
                .map(cliente -> toResponse(cliente, fullAccess))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> buscarPorId(@PathVariable Long id, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.CUSTOMER_VIEW, "Clientes", "consultar cliente");
        boolean fullAccess = privacyGuardService.canViewSensitiveData(request);
        return clienteService.buscarPorId(id, empresaId(request), filialId(request))
                .map(cliente -> {
                    auditTrailService.registrarPrivacidade(
                            fullAccess ? "CLIENTE_VISUALIZADO_COMPLETO" : "CLIENTE_VISUALIZADO_MASCARADO",
                            "Cliente",
                            cliente.getIdCliente(),
                            "Clientes",
                            null,
                            "consulta=detalhe",
                            request,
                            "Visualizacao contextual de cadastro.");
                    return ResponseEntity.ok(toResponse(cliente, fullAccess));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ClienteResponseDTO> criar(@Valid @RequestBody ClienteRequestDTO requestBody,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.CUSTOMER_CREATE, "Clientes", "criar cliente");
        Cliente salvo = clienteService.salvar(toEntity(requestBody, null), empresaId(request), filialId(request));
        registrarConsentimentoCadastro(salvo, requestBody, request);
        auditTrailService.registrarPrivacidade(
                "CLIENTE_CRIADO",
                "Cliente",
                salvo.getIdCliente(),
                "Clientes",
                null,
                resumoCliente(salvo),
                request,
                "Cadastro operacional de cliente.");
        return new ResponseEntity<>(toResponse(salvo, privacyGuardService.canViewSensitiveData(request)),
                HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> atualizar(@PathVariable Long id,
            @Valid @RequestBody ClienteRequestDTO requestBody,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.CUSTOMER_UPDATE, "Clientes", "atualizar cliente");
        return clienteService.buscarPorId(id, empresaId(request), filialId(request))
                .map(existente -> {
                    String anterior = resumoCliente(existente);
                    Cliente salvo = clienteService.salvar(toEntity(requestBody, existente), empresaId(request), filialId(request));
                    registrarConsentimentoCadastro(salvo, requestBody, request);
                    auditTrailService.registrarPrivacidade(
                            "CLIENTE_ATUALIZADO",
                            "Cliente",
                            salvo.getIdCliente(),
                            "Clientes",
                            anterior,
                            resumoCliente(salvo),
                            request,
                            "Atualizacao operacional de cliente.");
                    return ResponseEntity.ok(toResponse(salvo, privacyGuardService.canViewSensitiveData(request)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<ClienteResponseDTO> desativar(@PathVariable Long id, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.CUSTOMER_DISABLE, "Clientes", "desativar cliente");
        try {
            Cliente salvo = clienteService.desativar(id, empresaId(request), filialId(request));
            auditTrailService.registrarPrivacidade(
                    "CLIENTE_DESATIVADO",
                    "Cliente",
                    salvo.getIdCliente(),
                    "Clientes",
                    null,
                    resumoCliente(salvo),
                    request,
                    "Soft delete aplicado; retencao mantida para historico operacional.");
            return ResponseEntity.ok(toResponse(salvo, privacyGuardService.canViewSensitiveData(request)));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private Cliente toEntity(ClienteRequestDTO request, Cliente existente) {
        Cliente cliente = existente == null ? new Cliente() : existente;
        cliente.setNomeRazao(request.nomeRazao());
        cliente.setNomeFantasia(request.nomeFantasia());
        cliente.setTelefone(privacyGuardService.normalizeDocument(request.telefone()));
        cliente.setCelular(privacyGuardService.normalizeDocument(request.celular()));
        cliente.setEmail(request.email());
        cliente.setTipoDocumento(request.tipoDocumento());
        cliente.setCpfCnpj(privacyGuardService.normalizeDocument(request.cpfCnpj()));
        cliente.setRgIe(request.rgIe());
        cliente.setSituacaoFiscal(request.situacaoFiscal());
        cliente.setObservacoes(request.observacoes());
        if (request.status() != null) {
            cliente.setStatus(request.status());
        }
        if (request.enderecos() != null) {
            cliente.setEnderecos(request.enderecos().stream()
                    .map(EnderecoResumoDTO::toEntity)
                    .collect(Collectors.toCollection(LinkedHashSet::new)));
        }
        return cliente;
    }

    private ClienteResponseDTO toResponse(Cliente cliente, boolean fullAccess) {
        Set<EnderecoResumoDTO> enderecos = cliente.getEnderecos() == null ? Set.of()
                : cliente.getEnderecos().stream()
                        .map(endereco -> privacyGuardService.maskAddress(endereco, fullAccess))
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        return new ClienteResponseDTO(
                cliente.getIdCliente(),
                cliente.getNomeRazao(),
                cliente.getNomeFantasia(),
                fullAccess ? cliente.getTelefone() : privacyGuardService.maskPhone(cliente.getTelefone()),
                fullAccess ? cliente.getCelular() : privacyGuardService.maskPhone(cliente.getCelular()),
                fullAccess ? cliente.getEmail() : privacyGuardService.maskEmail(cliente.getEmail()),
                cliente.getTipoDocumento(),
                fullAccess ? cliente.getCpfCnpj() : privacyGuardService.maskDocument(cliente.getCpfCnpj()),
                cliente.getRgIe(),
                cliente.getSituacaoFiscal(),
                cliente.getObservacoes(),
                cliente.getDataCadastro(),
                cliente.getStatus(),
                cliente.getEmpresaId(),
                cliente.getFilialId(),
                enderecos,
                cliente.getAnonimizadoEm() != null,
                cliente.getAnonimizadoEm() != null ? "ANONIMIZADO" : "PROTEGIDO");
    }

    private void registrarConsentimentoCadastro(Cliente cliente, ClienteRequestDTO requestBody,
            HttpServletRequest request) {
        if (requestBody.consentimento() == null || !Boolean.TRUE.equals(requestBody.consentimento().aceito())) {
            return;
        }
        consentimentoService.registrar(new ConsentimentoRequestDTO(
                String.valueOf(cliente.getIdCliente()),
                "CLIENTE",
                cliente.getCpfCnpj(),
                requestBody.consentimento().finalidade(),
                requestBody.consentimento().versao(),
                requestBody.consentimento().canal()), request);
    }

    private String resumoCliente(Cliente cliente) {
        return "id=" + cliente.getIdCliente()
                + ";nome=" + cliente.getNomeRazao()
                + ";documento=" + privacyGuardService.maskDocument(cliente.getCpfCnpj())
                + ";status=" + cliente.getStatus();
    }

    private Long empresaId(HttpServletRequest request) {
        return tenantScopeService.empresaId(request);
    }

    private Long filialId(HttpServletRequest request) {
        return tenantScopeService.filialId(request);
    }
}
