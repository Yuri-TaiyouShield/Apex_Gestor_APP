package DTO;

import jakarta.validation.constraints.NotBlank;

public record FornecedorRequestDTO(
        Integer status,
        @NotBlank String razaoSocial,
        @NotBlank String nomeFantasia,
        @NotBlank String cnpj,
        String inscricaoEstadual,
        String email,
        @NotBlank String telefone,
        String celular,
        String situacaoFiscal,
        String condicoesPagamento,
        String dadosBancarios,
        String observacoes,
        Long empresaId,
        Long filialId,
        EnderecoResumoDTO endereco) {
}
