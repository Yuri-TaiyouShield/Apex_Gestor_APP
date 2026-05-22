package Controller;

import Model.ProdutoVenda;
import Model.ProdutoVendaId;
import Service.ProdutoVendaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/itensvenda")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:8100", "http://localhost:5173", "app://localhost", "ionic://localhost"})
public class ProdutoVendaController {

    @Autowired private ProdutoVendaService ProdutoVendaService;

    @GetMapping
    public List<ProdutoVenda> listarTodos() {
        return ProdutoVendaService.listarTodos();
    }
    
    @GetMapping("/buscar")
    public ResponseEntity<ProdutoVenda> buscarPorId(@RequestParam Long produtoId, @RequestParam Long vendaId) {
        ProdutoVendaId id = new ProdutoVendaId(produtoId, vendaId);
        
        return ProdutoVendaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}