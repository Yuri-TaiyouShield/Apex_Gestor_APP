package Controller;

import DTO.CatalogEnrichmentResultDTO;
import Service.CatalogEnrichmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalogo/enriquecimento")
@CrossOrigin(originPatterns = {"http://localhost:*", "https://localhost:*", "https://*.app.github.dev", "capacitor://localhost", "ionic://localhost", "app://localhost"})
public class CatalogEnrichmentController {

    private final CatalogEnrichmentService catalogEnrichmentService;

    public CatalogEnrichmentController(CatalogEnrichmentService catalogEnrichmentService) {
        this.catalogEnrichmentService = catalogEnrichmentService;
    }

    @PostMapping("/produtos/{id}")
    public ResponseEntity<CatalogEnrichmentResultDTO> enrichProduct(@PathVariable Long id) {
        return ResponseEntity.accepted().body(catalogEnrichmentService.enrich(id));
    }
}
