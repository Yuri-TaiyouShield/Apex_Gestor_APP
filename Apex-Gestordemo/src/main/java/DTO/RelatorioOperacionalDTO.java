package DTO;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RelatorioOperacionalDTO {

    private String tipo;
    private long totalRegistros;
    private long ativos;
    private long inativos;
    private BigDecimal valorTotal;
    private List<String> alertas;
}
