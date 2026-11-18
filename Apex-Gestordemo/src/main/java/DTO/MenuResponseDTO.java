package DTO;

import java.util.List;

public record MenuResponseDTO(Long idMenu, String nome, String link, String icone, Integer exibir,
        List<PerfilResumoDTO> perfis) {
}
