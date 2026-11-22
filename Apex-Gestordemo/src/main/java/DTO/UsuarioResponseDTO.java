package DTO;

import java.time.LocalDate;

public record UsuarioResponseDTO(
        Long idUsuario,
        String nome,
        String login,
        LocalDate dataNascimento,
        Integer status,
        Long empresaId,
        Long filialId,
        PerfilResumoDTO perfil) {
}
