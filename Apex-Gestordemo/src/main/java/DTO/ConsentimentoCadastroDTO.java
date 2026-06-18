package DTO;

public record ConsentimentoCadastroDTO(
        String finalidade,
        String versao,
        String canal,
        Boolean aceito) {
}
