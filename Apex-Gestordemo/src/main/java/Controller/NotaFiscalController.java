package Controller;

import DTO.*;
import Model.*;
import Service.*;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nfs")
public class NotaFiscalController {
    @Autowired private NotaFiscalService service; @Autowired private AuthorizationService auth; @Autowired private TenantScopeService tenant;
    @GetMapping("/entrada") public List<NotaFiscalEntradaResponseDTO> listar(HttpServletRequest r) { auth.require(r, ApexPermission.STOCK_CHANGE, "NotaFiscal", "listar notas de entrada"); return service.listar(tenant.empresaId(r), tenant.filialId(r)).stream().map(this::toResponse).toList(); }
    @PostMapping("/entrada") public ResponseEntity<NotaFiscalEntradaResponseDTO> entrada(@Valid @RequestBody NotaFiscalEntradaRequestDTO b, HttpServletRequest r, @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) { auth.require(r, ApexPermission.STOCK_CHANGE, "NotaFiscal", "dar entrada de estoque"); return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(service.darEntrada(from(b), tenant.empresaId(r), tenant.filialId(r), r, idempotencyKey))); }
    private NotaFiscalEntrada from(NotaFiscalEntradaRequestDTO b) { NotaFiscalEntrada n = new NotaFiscalEntrada(); n.setNumero(b.numero()); n.setSerie(b.serie()); n.setDataEmissao(b.dataEmissao()); Fornecedor f = new Fornecedor(); f.setIdFornecedor(b.fornecedorId()); n.setFornecedor(f); List<ItemNotaFiscal> itens = new ArrayList<>(); for (var bi : b.itens()) { ItemNotaFiscal i = new ItemNotaFiscal(); Produto p = new Produto(); p.setIdProduto(bi.produtoId()); i.setProduto(p); i.setQuantidade(bi.quantidade()); i.setValorCustoUnitario(bi.valorCustoUnitario()); itens.add(i); } n.setItens(itens); return n; }
    private NotaFiscalEntradaResponseDTO toResponse(NotaFiscalEntrada n) { return new NotaFiscalEntradaResponseDTO(n.getIdNfEntrada(), n.getNumero(), n.getSerie(), n.getFornecedor().getIdFornecedor(), n.getFornecedor().getRazaoSocial(), n.getDataEmissao(), n.getDataEntrada(), n.getValorTotal(), n.getEmpresaId(), n.getFilialId(), n.getItens() == null ? List.of() : n.getItens().stream().map(i -> new NotaFiscalEntradaResponseDTO.ItemDTO(i.getIdItemNf(), i.getProduto().getIdProduto(), i.getProduto().getDescricao(), i.getQuantidade(), i.getValorCustoUnitario())).toList()); }
}
