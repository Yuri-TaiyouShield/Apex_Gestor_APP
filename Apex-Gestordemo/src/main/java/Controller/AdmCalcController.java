package Controller;

import DTO.AdmCalcRequestDTO;
import DTO.AdmCalcResultadoDTO;
import Service.AdmCalcFinanceiroService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admcalc")
@CrossOrigin(origins = {
        "http://localhost:4200",
        "http://localhost:8100",
        "http://localhost:5173",
        "app://localhost"
})
public class AdmCalcController {

    private final AdmCalcFinanceiroService admCalcFinanceiroService;

    public AdmCalcController(AdmCalcFinanceiroService admCalcFinanceiroService) {
        this.admCalcFinanceiroService = admCalcFinanceiroService;
    }

    @PostMapping("/calcular")
    public ResponseEntity<AdmCalcResultadoDTO> calcular(@RequestBody AdmCalcRequestDTO request) {
        return ResponseEntity.ok(admCalcFinanceiroService.calcular(request));
    }
}
