package DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnderecoRequestDTO(String estado, String cidade,
        @NotBlank @Size(max = 9) String cep, String bairro, String logradouro,
        @Size(max = 2) String uf, String numero, String complemento) {
}
