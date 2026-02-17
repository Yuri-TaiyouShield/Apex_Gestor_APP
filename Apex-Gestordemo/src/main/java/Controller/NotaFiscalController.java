package Controller;

import Model.NotaFiscalEntrada;
import Service.NotaFiscalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nfs")
@CrossOrigin(origins = "http://localhost:4200")
public class NotaFiscalController {

    @Autowired
    private NotaFiscalService nfService;

    @PostMapping("/entrada")
    public ResponseEntity<?> entradaEstoque(@RequestBody NotaFiscalEntrada nf) {
        try {
            return new ResponseEntity<>(nfService.darEntrada(nf), HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
