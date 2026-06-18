package Model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "Conta_Pagar")
@Data
public class ContaPagar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conta_pagar")
    private Long idContaPagar;
    @Column(name = "fornecedor_id")
    private Long fornecedorId;
    @Column(name = "categoria_id")
    private Long categoriaId;
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
    @Column(name = "filial_id", nullable = false)
    private Long filialId;
    @Column(nullable = false, length = 160)
    private String descricao;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;
    @Column(nullable = false)
    private LocalDate vencimento;
    @Column(name = "forma_pagamento_prevista_id")
    private Long formaPagamentoPrevistaId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FinanceiroStatus status;
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
    @Column(name = "pago_em")
    private LocalDateTime pagoEm;
}
