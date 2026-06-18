package DTO;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record UsuarioRequestDTO(
        @NotBlank String nome,
        @NotBlank String login,
        String senha,
        LocalDate dataNascimento,
        Integer status,
        Long empresaId,
        Long filialId,
        PerfilResumoDTO perfil) {
}
