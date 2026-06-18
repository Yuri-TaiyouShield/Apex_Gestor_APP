package Controller;

import DTO.*;
import Model.Endereco;
import Service.*;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/enderecos")
public class EnderecoController {
    @Autowired private EnderecoService service;
    @Autowired private AuthorizationService authorizationService;
    @Autowired private TenantScopeService tenantScopeService;
    @Autowired private PrivacyGuardService privacyGuardService;
    @GetMapping public List<EnderecoResponseDTO> listar(HttpServletRequest r) { authorizationService.require(r, ApexPermission.CUSTOMER_VIEW, "Enderecos", "listar enderecos"); return service.listarTodos(empresa(r), filial(r)).stream().map(e -> toResponse(e, r)).toList(); }
    @GetMapping("/{id}") public ResponseEntity<EnderecoResponseDTO> buscar(@PathVariable Long id, HttpServletRequest r) { authorizationService.require(r, ApexPermission.CUSTOMER_VIEW, "Enderecos", "consultar endereco"); return service.buscarPorId(id, empresa(r), filial(r)).map(e -> ResponseEntity.ok(toResponse(e, r))).orElse(ResponseEntity.notFound().build()); }
    @PostMapping public ResponseEntity<EnderecoResponseDTO> criar(@Valid @RequestBody EnderecoRequestDTO b, HttpServletRequest r) { authorizationService.require(r, ApexPermission.CUSTOMER_UPDATE, "Enderecos", "criar endereco"); return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(service.salvar(from(b, null), empresa(r), filial(r)), r)); }
    @PutMapping("/{id}") public EnderecoResponseDTO atualizar(@PathVariable Long id, @Valid @RequestBody EnderecoRequestDTO b, HttpServletRequest r) { authorizationService.require(r, ApexPermission.CUSTOMER_UPDATE, "Enderecos", "atualizar endereco"); service.buscarPorId(id, empresa(r), filial(r)).orElseThrow(); return toResponse(service.salvar(from(b, id), empresa(r), filial(r)), r); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deletar(@PathVariable Long id, HttpServletRequest r) { authorizationService.require(r, ApexPermission.CUSTOMER_DISABLE, "Enderecos", "remover endereco"); service.deletar(id, empresa(r), filial(r)); }
    private Endereco from(EnderecoRequestDTO b, Long id) { Endereco e = new Endereco(); e.setIdEndereco(id); e.setEstado(b.estado()); e.setCidade(b.cidade()); e.setCep(b.cep()); e.setBairro(b.bairro()); e.setLogradouro(b.logradouro()); e.setUf(b.uf()); e.setNumero(b.numero()); e.setComplemento(b.complemento()); return e; }
    private EnderecoResponseDTO toResponse(Endereco e, HttpServletRequest r) { boolean full = privacyGuardService.canViewSensitiveData(r); return new EnderecoResponseDTO(e.getIdEndereco(), full ? e.getEstado() : null, e.getCidade(), full ? e.getCep() : privacyGuardService.maskCep(e.getCep()), full ? e.getBairro() : null, full ? e.getLogradouro() : null, e.getUf(), full ? e.getNumero() : null, full ? e.getComplemento() : null, e.getEmpresaId(), e.getFilialId()); }
    private Long empresa(HttpServletRequest r) { return tenantScopeService.empresaId(r); } private Long filial(HttpServletRequest r) { return tenantScopeService.filialId(r); }
}
