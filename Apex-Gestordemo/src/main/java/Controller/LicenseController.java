package Controller;

import DTO.LicenseValidationRequestDTO;
import DTO.LicenseValidationResponseDTO;
import Service.LicenseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/licenses")
public class LicenseController {

    private final LicenseService licenseService;

    public LicenseController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @PostMapping("/validate")
    public LicenseValidationResponseDTO validate(@Valid @RequestBody LicenseValidationRequestDTO request) {
        return licenseService.validate(request);
    }
}
