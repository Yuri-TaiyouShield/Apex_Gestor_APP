package Controller;

import DTO.EnderecoResumoDTO;
import DTO.FornecedorRequestDTO;
import DTO.FornecedorResponseDTO;
import Model.Fornecedor;
import Service.AuditTrailService;
import Service.AuthorizationService;
import Service.FornecedorService;
import Service.PrivacyGuardService;
import Service.TenantScopeService;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/fornecedores")
public class FornecedorController {

    @Autowired private FornecedorService fornecedorService;
    @Autowired private PrivacyGuardService privacyGuardService;
    @Autowired private AuditTrailService auditTrailService;
    @Autowired private AuthorizationService authorizationService;
    @Autowired private TenantScopeService tenantScopeService;

    @GetMapping
    public List<FornecedorResponseDTO> listarTodos(HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.SUPPLIER_VIEW, "Fornecedores", "listar fornecedores");
        boolean fullAccess = privacyGuardService.canViewSensitiveData(request);
        auditTrailService.registrarPrivacidade(
                fullAccess ? "FORNECEDOR_LISTA_VISUALIZADA_COMPLETA" : "FORNECEDOR_LISTA_VISUALIZADA_MASCARADA",
                "Fornecedor",
                null,
                "Fornecedores",
                null,
                "consulta=listagem",
                request,
                "Consulta operacional de fornecedores.");
        return fornecedorService.listarTodos(empresaId(request), filialId(request)).stream()
                .map(fornecedor -> toResponse(fornecedor, fullAccess))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FornecedorResponseDTO> buscarPorId(@PathVariable Long id, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.SUPPLIER_VIEW, "Fornecedores", "consultar fornecedor");
        boolean fullAccess = privacyGuardService.canViewSensitiveData(request);
        return fornecedorService.buscarPorId(id, empresaId(request), filialId(request))
                .map(fornecedor -> {
                    auditTrailService.registrarPrivacidade(
                            fullAccess ? "FORNECEDOR_VISUALIZADO_COMPLETO" : "FORNECEDOR_VISUALIZADO_MASCARADO",
                            "Fornecedor",
                            fornecedor.getIdFornecedor(),
                            "Fornecedores",
                            null,
                            "consulta=detalhe",
                            request,
                            "Visualizacao contextual de fornecedor.");
                    return ResponseEntity.ok(toResponse(fornecedor, fullAccess));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<FornecedorResponseDTO> criar(@Valid @RequestBody FornecedorRequestDTO requestBody,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.SUPPLIER_CREATE, "Fornecedores", "criar fornecedor");
        Fornecedor salvo = fornecedorService.salvar(toEntity(requestBody, null), empresaId(request), filialId(request));
        auditTrailService.registrarPrivacidade(
                "FORNECEDOR_CRIADO",
                "Fornecedor",
                salvo.getIdFornecedor(),
                "Fornecedores",
                null,
                resumoFornecedor(salvo),
                request,
                "Cadastro operacional de fornecedor.");
        return new ResponseEntity<>(toResponse(salvo, privacyGuardService.canViewSensitiveData(request)),
                HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FornecedorResponseDTO> atualizar(@PathVariable Long id,
            @Valid @RequestBody FornecedorRequestDTO requestBody,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.SUPPLIER_UPDATE, "Fornecedores", "atualizar fornecedor");
        return fornecedorService.buscarPorId(id, empresaId(request), filialId(request))
                .map(existente -> {
                    String anterior = resumoFornecedor(existente);
                    Fornecedor salvo = fornecedorService.salvar(toEntity(requestBody, existente), empresaId(request), filialId(request));
                    auditTrailService.registrarPrivacidade(
                            "FORNECEDOR_ATUALIZADO",
                            "Fornecedor",
                            salvo.getIdFornecedor(),
                            "Fornecedores",
                            anterior,
                            resumoFornecedor(salvo),
                            request,
                            "Atualizacao operacional de fornecedor.");
                    return ResponseEntity.ok(toResponse(salvo, privacyGuardService.canViewSensitiveData(request)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<FornecedorResponseDTO> desativar(@PathVariable Long id, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.SUPPLIER_DISABLE, "Fornecedores", "desativar fornecedor");
        try {
            Fornecedor salvo = fornecedorService.desativar(id, empresaId(request), filialId(request));
            auditTrailService.registrarPrivacidade(
                    "FORNECEDOR_DESATIVADO",
                    "Fornecedor",
                    salvo.getIdFornecedor(),
                    "Fornecedores",
                    null,
                    resumoFornecedor(salvo),
                    request,
                    "Fornecedor desativado sem exclusao fisica.");
            return ResponseEntity.ok(toResponse(salvo, privacyGuardService.canViewSensitiveData(request)));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private Fornecedor toEntity(FornecedorRequestDTO request, Fornecedor existente) {
        Fornecedor fornecedor = existente == null ? new Fornecedor() : existente;
        if (request.status() != null) {
            fornecedor.setStatus(request.status());
        }
        fornecedor.setRazaoSocial(request.razaoSocial());
        fornecedor.setNomeFantasia(request.nomeFantasia());
        fornecedor.setCnpj(privacyGuardService.normalizeDocument(request.cnpj()));
        fornecedor.setInscricaoEstadual(request.inscricaoEstadual());
        fornecedor.setEmail(request.email());
        fornecedor.setTelefone(privacyGuardService.normalizeDocument(request.telefone()));
        fornecedor.setCelular(privacyGuardService.normalizeDocument(request.celular()));
        fornecedor.setSituacaoFiscal(request.situacaoFiscal());
        fornecedor.setCondicoesPagamento(request.condicoesPagamento());
        fornecedor.setDadosBancarios(request.dadosBancarios());
        fornecedor.setObservacoes(request.observacoes());
        if (request.endereco() != null) {
            fornecedor.setEndereco(request.endereco().toEntity());
        }
        return fornecedor;
    }

    private FornecedorResponseDTO toResponse(Fornecedor fornecedor, boolean fullAccess) {
        return new FornecedorResponseDTO(
                fornecedor.getIdFornecedor(),
                fornecedor.getStatus(),
                fornecedor.getRazaoSocial(),
                fornecedor.getNomeFantasia(),
                fullAccess ? fornecedor.getCnpj() : privacyGuardService.maskDocument(fornecedor.getCnpj()),
                fornecedor.getInscricaoEstadual(),
                fullAccess ? fornecedor.getEmail() : privacyGuardService.maskEmail(fornecedor.getEmail()),
                fornecedor.getDataCadastro(),
                fullAccess ? fornecedor.getTelefone() : privacyGuardService.maskPhone(fornecedor.getTelefone()),
                fullAccess ? fornecedor.getCelular() : privacyGuardService.maskPhone(fornecedor.getCelular()),
                fornecedor.getSituacaoFiscal(),
                fornecedor.getCondicoesPagamento(),
                fullAccess ? fornecedor.getDadosBancarios() : "***",
                fornecedor.getObservacoes(),
                fornecedor.getEmpresaId(),
                fornecedor.getFilialId(),
                privacyGuardService.maskAddress(fornecedor.getEndereco(), fullAccess),
                "PROTEGIDO");
    }

    private String resumoFornecedor(Fornecedor fornecedor) {
        return "id=" + fornecedor.getIdFornecedor()
                + ";razaoSocial=" + fornecedor.getRazaoSocial()
                + ";cnpj=" + privacyGuardService.maskDocument(fornecedor.getCnpj())
                + ";status=" + fornecedor.getStatus();
    }

    private Long empresaId(HttpServletRequest request) {
        return tenantScopeService.empresaId(request);
    }

    private Long filialId(HttpServletRequest request) {
        return tenantScopeService.filialId(request);
    }
}
