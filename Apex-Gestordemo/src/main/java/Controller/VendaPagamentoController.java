package Controller;

import Model.VendaPagamento;
import Model.VendaPagamentoId;
import Service.VendaPagamentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/pagamentosvenda")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:8100", "http://localhost:5173", "app://localhost", "ionic://localhost"})
public class VendaPagamentoController {

    @Autowired
    private VendaPagamentoService pagamentoVendaService;

    @GetMapping
    public List<VendaPagamento> listarTodos() {
        return pagamentoVendaService.listarTodos();
    }

    @GetMapping("/buscar")
    public ResponseEntity<VendaPagamento> buscarPorId(@RequestParam Long vendaId, @RequestParam Long formaPagamentoId) {
        VendaPagamentoId id = new VendaPagamentoId(vendaId, formaPagamentoId);

        return pagamentoVendaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
