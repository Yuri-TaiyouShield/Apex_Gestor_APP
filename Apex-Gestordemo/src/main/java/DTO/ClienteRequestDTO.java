package DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record ClienteRequestDTO(
        @NotBlank String nomeRazao,
        String nomeFantasia,
        @NotBlank String telefone,
        String celular,
        String email,
        @NotNull Integer tipoDocumento,
        @NotBlank String cpfCnpj,
        String rgIe,
        String situacaoFiscal,
        String observacoes,
        Integer status,
        Set<EnderecoResumoDTO> enderecos,
        ConsentimentoCadastroDTO consentimento) {
}
