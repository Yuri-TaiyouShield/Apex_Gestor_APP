package Model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Entity
@Table(name = "Meta_Vendedor")
@Data
public class MetaVendedor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_meta_vendedor")
    private Long idMetaVendedor;
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
    @Column(name = "filial_id", nullable = false)
    private Long filialId;
    @Column(name = "periodo_inicio", nullable = false)
    private LocalDate periodoInicio;
    @Column(name = "periodo_fim", nullable = false)
    private LocalDate periodoFim;
    @Column(name = "valor_meta", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorMeta;
}
