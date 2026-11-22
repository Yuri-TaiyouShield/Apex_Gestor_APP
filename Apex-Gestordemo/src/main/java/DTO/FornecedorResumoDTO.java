package DTO;

public record FornecedorResumoDTO(
        Long idFornecedor,
        String razaoSocial,
        String nomeFantasia,
        String cnpj,
        String telefone) {
}
