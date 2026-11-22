package DTO;

import java.time.LocalDateTime;
import java.util.Set;

public record ClienteResponseDTO(
        Long idCliente,
        String nomeRazao,
        String nomeFantasia,
        String telefone,
        String celular,
        String email,
        Integer tipoDocumento,
        String cpfCnpj,
        String rgIe,
        String situacaoFiscal,
        String observacoes,
        LocalDateTime dataCadastro,
        Integer status,
        Long empresaId,
        Long filialId,
        Set<EnderecoResumoDTO> enderecos,
        Boolean anonimizado,
        String lgpdStatus) {
}
