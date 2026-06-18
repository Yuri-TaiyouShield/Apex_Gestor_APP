package DTO;

import java.time.LocalDateTime;

public record FornecedorResponseDTO(
        Long idFornecedor,
        Integer status,
        String razaoSocial,
        String nomeFantasia,
        String cnpj,
        String inscricaoEstadual,
        String email,
        LocalDateTime dataCadastro,
        String telefone,
        String celular,
        String situacaoFiscal,
        String condicoesPagamento,
        String dadosBancarios,
        String observacoes,
        Long empresaId,
        Long filialId,
        EnderecoResumoDTO endereco,
        String lgpdStatus) {
}
