package DTO;

public record EnderecoResponseDTO(Long idEndereco, String estado, String cidade, String cep,
        String bairro, String logradouro, String uf, String numero, String complemento,
        Long empresaId, Long filialId) {
}
