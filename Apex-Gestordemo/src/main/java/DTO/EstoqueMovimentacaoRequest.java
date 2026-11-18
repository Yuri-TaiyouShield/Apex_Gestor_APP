package DTO;

import Model.TipoMovimentacaoEstoque;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EstoqueMovimentacaoRequest {

    @NotNull
    private Long produtoId;

    @NotNull
    private TipoMovimentacaoEstoque tipo;

    @Min(0)
    private int quantidade;

    private Integer saldoContado;
    private String referenciaTipo;
    private Long referenciaId;
    private String motivo;
    private Long empresaId;
    private Long filialId;
    private Long usuarioId;
}
