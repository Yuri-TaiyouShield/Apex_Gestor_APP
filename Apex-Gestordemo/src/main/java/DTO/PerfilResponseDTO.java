package DTO;

import java.util.List;

public record PerfilResponseDTO(Long idPerfil, String nome, Integer status, List<MenuResumoDTO> menus) {
    public record MenuResumoDTO(Long idMenu, String nome, String link, String icone, Integer exibir) {
    }
}
