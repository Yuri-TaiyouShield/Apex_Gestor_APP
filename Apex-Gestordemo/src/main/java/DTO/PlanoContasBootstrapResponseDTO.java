package DTO;

import java.util.List;

public record PlanoContasBootstrapResponseDTO(
        int contasCriadas,
        int contasJaExistentes,
        int totalContas,
        List<PlanoContasResponseDTO> contas) {
}
