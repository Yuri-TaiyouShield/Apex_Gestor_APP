package Controller;

import DTO.VendaPagamentoResponseDTO;
import Model.*;
import Service.*;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pagamentosvenda")
public class VendaPagamentoController {
    @Autowired private VendaPagamentoService service; @Autowired private AuthorizationService auth; @Autowired private TenantScopeService tenant;
    @GetMapping public List<VendaPagamentoResponseDTO> listar(HttpServletRequest r) { auth.require(r, ApexPermission.SALE_HISTORY_VIEW, "Vendas", "listar pagamentos de venda"); return service.listarTodos(tenant.empresaId(r), tenant.filialId(r)).stream().map(this::toResponse).toList(); }
    @GetMapping("/buscar") public ResponseEntity<VendaPagamentoResponseDTO> buscar(@RequestParam Long vendaId, @RequestParam Long formaPagamentoId, HttpServletRequest r) { auth.require(r, ApexPermission.SALE_HISTORY_VIEW, "Vendas", "consultar pagamento de venda"); return service.buscarPorId(new VendaPagamentoId(vendaId, formaPagamentoId), tenant.empresaId(r), tenant.filialId(r)).map(p -> ResponseEntity.ok(toResponse(p))).orElse(ResponseEntity.notFound().build()); }
    private VendaPagamentoResponseDTO toResponse(VendaPagamento p) { return new VendaPagamentoResponseDTO(p.getVenda().getIdVenda(), p.getFormaPagamento().getIdFormaPagamento(), p.getFormaPagamento().getNome(), p.getValorPago(), p.getNumeroParcelas(), p.getCreditoId()); }
}
