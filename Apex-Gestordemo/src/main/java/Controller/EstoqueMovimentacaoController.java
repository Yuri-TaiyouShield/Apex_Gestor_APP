package Controller;

import DTO.EstoqueMovimentacaoRequest;
import DTO.EstoqueMovimentacaoResponseDTO;
import Model.EstoqueMovimentacao;
import Service.AuthorizationService;
import Service.EstoqueMovimentacaoService;
import Service.TenantScopeService;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/estoque/movimentacoes")
@CrossOrigin(origins = "http://localhost:4200")
public class EstoqueMovimentacaoController {

    @Autowired
    private EstoqueMovimentacaoService estoqueMovimentacaoService;
    @Autowired
    private AuthorizationService authorizationService;
    @Autowired
    private TenantScopeService tenantScopeService;

    @GetMapping("/produto/{produtoId}")
    public List<EstoqueMovimentacaoResponseDTO> listarPorProduto(@PathVariable Long produtoId,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.STOCK_VIEW, "Estoque", "listar movimentacoes");
        return estoqueMovimentacaoService.listarPorProduto(
                produtoId,
                tenantScopeService.empresaId(request),
                tenantScopeService.filialId(request)).stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> registrar(@Valid @RequestBody EstoqueMovimentacaoRequest body,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.STOCK_ADJUST, "Estoque", "ajustar estoque");
        try {
            EstoqueMovimentacao movimentacao = estoqueMovimentacaoService.aplicar(
                    body.getProdutoId(),
                    body.getTipo(),
                    body.getQuantidade(),
                    body.getSaldoContado(),
                    body.getReferenciaTipo(),
                    body.getReferenciaId(),
                    body.getMotivo(),
                    tenantScopeService.empresaId(request),
                    tenantScopeService.filialId(request),
                    body.getUsuarioId());
            return new ResponseEntity<>(toResponse(movimentacao), HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private EstoqueMovimentacaoResponseDTO toResponse(EstoqueMovimentacao movimentacao) {
        return new EstoqueMovimentacaoResponseDTO(
                movimentacao.getIdMovimentacao(),
                movimentacao.getProduto() == null ? null : movimentacao.getProduto().getIdProduto(),
                movimentacao.getProduto() == null ? null : movimentacao.getProduto().getDescricao(),
                movimentacao.getTipo(),
                movimentacao.getQuantidade(),
                movimentacao.getSaldoAnterior(),
                movimentacao.getSaldoPosterior(),
                movimentacao.getReferenciaTipo(),
                movimentacao.getReferenciaId(),
                movimentacao.getMotivo(),
                movimentacao.getUsuarioId(),
                movimentacao.getEmpresaId(),
                movimentacao.getFilialId(),
                movimentacao.getCriadoEm());
    }
}
