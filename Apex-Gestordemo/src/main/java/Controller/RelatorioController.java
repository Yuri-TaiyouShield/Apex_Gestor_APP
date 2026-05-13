package Controller;

import DTO.RelatorioFinanceiroDTO;
import Service.FinanceiroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/relatorios")
@CrossOrigin(originPatterns = {"http://localhost:*", "capacitor://localhost", "ionic://localhost", "app://localhost"})
public class RelatorioController {

    @Autowired
    private FinanceiroService financeiroService;

    @GetMapping("/financeiro")
    public ResponseEntity<RelatorioFinanceiroDTO> getFinanceiro(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {

        return ResponseEntity.ok(financeiroService.gerarRelatorio(inicio, fim));
    }
}
