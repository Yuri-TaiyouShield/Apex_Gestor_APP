package Model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "Caixa_Movimentacao")
@Data
public class CaixaMovimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_caixa_movimentacao")
    private Long idCaixaMovimentacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caixa_sessao_id", nullable = false)
    private CaixaSessao caixaSessao;

    @Column(nullable = false, length = 24)
    private String tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(length = 255)
    private String observacao;

    @Column(name = "referencia_tipo", length = 40)
    private String referenciaTipo;

    @Column(name = "referencia_id")
    private Long referenciaId;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}
