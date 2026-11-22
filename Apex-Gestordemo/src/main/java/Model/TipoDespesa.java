package Model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Table(name = "tipo_despesa", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tipo_despesa_tenant_nome", columnNames = {"empresa_id", "filial_id", "nome"})
}, indexes = {
        @Index(name = "idx_tipo_despesa_tenant", columnList = "empresa_id, filial_id, status"),
        @Index(name = "idx_tipo_despesa_classificacao", columnList = "empresa_id, filial_id, plano_conta_id, centro_custo_id")
})
@Data
public class TipoDespesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_despesa")
    private Long idTipoDespesa;
    @Column(nullable = false, length = 50)
    private String nome;
    @Column(nullable = false)
    private int status;
    @Column(name = "empresa_id")
    private Long empresaId;
    @Column(name = "filial_id")
    private Long filialId;
    @Column(name = "natureza_despesa", length = 24)
    private String naturezaDespesa;
    @Column(name = "plano_conta_id")
    private Long planoContaId;
    @Column(name = "centro_custo_id")
    private Long centroCustoId;
}
