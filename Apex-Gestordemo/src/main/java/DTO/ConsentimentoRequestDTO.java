package DTO;

public record ConsentimentoRequestDTO(
        String titularId,
        String tipoTitular,
        String documento,
        String finalidade,
        String versao,
        String canal) {
}
