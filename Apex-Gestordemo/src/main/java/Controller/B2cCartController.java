package Controller;

import DTO.B2cCartMergeRequestDTO;
import DTO.B2cCartMergeResponseDTO;
import Service.B2cCartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/b2c/cart")
@CrossOrigin(originPatterns = {"http://localhost:*", "https://localhost:*", "https://*.app.github.dev", "capacitor://localhost", "ionic://localhost", "app://localhost"})
public class B2cCartController {

    private final B2cCartService cartService;

    public B2cCartController(B2cCartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/merge")
    public ResponseEntity<B2cCartMergeResponseDTO> merge(@Valid @RequestBody B2cCartMergeRequestDTO request) {
        return ResponseEntity.ok(cartService.merge(request));
    }
}
