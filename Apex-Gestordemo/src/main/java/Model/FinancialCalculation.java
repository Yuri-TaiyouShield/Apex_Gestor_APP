package Model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

@Entity
@Audited
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

    @Column(name = "input_snapshot", nullable = false, columnDefinition = "TEXT")
    private String inputSnapshot;

    @Column(name = "resultado_snapshot", nullable = false, columnDefinition = "TEXT")
    private String resultadoSnapshot;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(name = "filial_id")
    private Long filialId;
}
