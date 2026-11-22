package Controller;

import DTO.ProdutoVendaResponseDTO;
import Model.*;
import Service.*;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/itensvenda")
public class ProdutoVendaController {
    @Autowired private ProdutoVendaService service; @Autowired private AuthorizationService auth; @Autowired private TenantScopeService tenant;
    @GetMapping public List<ProdutoVendaResponseDTO> listar(HttpServletRequest r) { auth.require(r, ApexPermission.SALE_HISTORY_VIEW, "Vendas", "listar itens de venda"); return service.listarTodos(tenant.empresaId(r), tenant.filialId(r)).stream().map(this::toResponse).toList(); }
    @GetMapping("/buscar") public ResponseEntity<ProdutoVendaResponseDTO> buscar(@RequestParam Long produtoId, @RequestParam Long vendaId, HttpServletRequest r) { auth.require(r, ApexPermission.SALE_HISTORY_VIEW, "Vendas", "consultar item de venda"); return service.buscarPorId(new ProdutoVendaId(produtoId, vendaId), tenant.empresaId(r), tenant.filialId(r)).map(i -> ResponseEntity.ok(toResponse(i))).orElse(ResponseEntity.notFound().build()); }
    private ProdutoVendaResponseDTO toResponse(ProdutoVenda i) { return new ProdutoVendaResponseDTO(i.getProduto().getIdProduto(), i.getVenda().getIdVenda(), i.getProduto().getDescricao(), i.getQuantidade(), i.getPrecoUnitario(), i.getPrecoTotal()); }
}
