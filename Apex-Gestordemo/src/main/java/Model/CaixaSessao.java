package Model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "Caixa_Sessao")
@Data
public class CaixaSessao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_caixa_sessao")
    private Long idCaixaSessao;

    @Column(name = "operador_login", nullable = false, length = 80)
    private String operadorLogin;

    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(name = "filial_id")
    private Long filialId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CaixaStatus status;

    @Column(name = "saldo_inicial", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoInicial;

    @Column(name = "total_vendas", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalVendas;

    @Column(name = "total_suprimentos", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSuprimentos;

    @Column(name = "total_sangrias", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSangrias;

    @Column(name = "saldo_final", precision = 12, scale = 2)
    private BigDecimal saldoFinal;

    @Column(name = "aberto_em", nullable = false)
    private LocalDateTime abertoEm;

    @Column(name = "fechado_em")
    private LocalDateTime fechadoEm;
}
