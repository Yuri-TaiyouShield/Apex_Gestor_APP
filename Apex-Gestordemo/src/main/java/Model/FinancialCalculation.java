package Model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "financial_calculation", indexes = {
        @Index(name = "idx_fin_calc_tipo_data", columnList = "tipo_calculo, criado_em"),
        @Index(name = "idx_fin_calc_ator", columnList = "ator_login")
})
@Data
public class FinancialCalculation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_financial_calculation")
    private Long idFinancialCalculation;

    @Column(name = "tipo_calculo", nullable = false, length = 60)
    private String tipoCalculo;

    @Column(name = "ator_login", nullable = false, length = 120)
    private String atorLogin;

    @Column(name = "input_hash", nullable = false, length = 128)
    private String inputHash;

    @Lob
    @Column(name = "input_snapshot", nullable = false)
    private String inputSnapshot;

    @Lob
    @Column(name = "resultado_snapshot", nullable = false)
    private String resultadoSnapshot;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}
