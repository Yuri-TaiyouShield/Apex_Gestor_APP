package DTO;

import Model.Endereco;

public record EnderecoResumoDTO(
        Long idEndereco,
        String estado,
        String cidade,
        String cep,
        String bairro,
        String logradouro,
        String uf,
        String numero,
        String complemento) {

    public static EnderecoResumoDTO from(Endereco endereco) {
        if (endereco == null) {
            return null;
        }
        return new EnderecoResumoDTO(
                endereco.getIdEndereco(),
                endereco.getEstado(),
                endereco.getCidade(),
                endereco.getCep(),
                endereco.getBairro(),
                endereco.getLogradouro(),
                endereco.getUf(),
                endereco.getNumero(),
                endereco.getComplemento());
    }

    public Endereco toEntity() {
        Endereco endereco = new Endereco();
        endereco.setIdEndereco(idEndereco);
        endereco.setEstado(estado);
        endereco.setCidade(cidade);
        endereco.setCep(cep);
        endereco.setBairro(bairro);
        endereco.setLogradouro(logradouro);
        endereco.setUf(uf);
        endereco.setNumero(numero);
        endereco.setComplemento(complemento);
        return endereco;
    }
}
