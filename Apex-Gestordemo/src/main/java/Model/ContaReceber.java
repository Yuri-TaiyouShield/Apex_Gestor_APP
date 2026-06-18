package Model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "Conta_Receber")
@Data
public class ContaReceber {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conta_receber")
    private Long idContaReceber;
    @Column(name = "venda_id", nullable = false)
    private Long vendaId;
    @Column(name = "cliente_id")
    private Long clienteId;
    @Column(name = "forma_pagamento_id")
    private Long formaPagamentoId;
    @Column(name = "condicao_pagamento_id")
    private Long condicaoPagamentoId;
    @Column(name = "caixa_sessao_id")
    private Long caixaSessaoId;
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
    @Column(name = "filial_id", nullable = false)
    private Long filialId;
    @Column(name = "valor_bruto", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorBruto;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal desconto;
    @Column(name = "valor_liquido", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorLiquido;
    @Column(name = "valor_pago", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorPago;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal saldo;
    @Column(nullable = false)
    private LocalDate vencimento;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FinanceiroStatus status;
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
    @Column(name = "pago_em")
    private LocalDateTime pagoEm;
}
