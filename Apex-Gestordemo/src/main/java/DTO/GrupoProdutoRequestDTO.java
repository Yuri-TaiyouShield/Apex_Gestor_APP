package DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GrupoProdutoRequestDTO(
        @NotBlank @Size(max = 80) String nome,
        @Size(max = 255) String descricao,
        Integer status) {
}
