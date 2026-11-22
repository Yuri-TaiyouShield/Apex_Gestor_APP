package DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PerfilRequestDTO(@NotBlank @Size(max = 45) String nome, Integer status) {
}
