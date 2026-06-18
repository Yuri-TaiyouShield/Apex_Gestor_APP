package Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "Venda_Evento")
@Data
public class VendaEvento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venda_evento")
    private Long idVendaEvento;
    @Column(name = "venda_id", nullable = false)
    private Long vendaId;
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;
    @Column(name = "filial_id", nullable = false)
    private Long filialId;
    @Column(nullable = false, length = 40)
    private String evento;
    @Column(name = "status_anterior", length = 24)
    private String statusAnterior;
    @Column(name = "status_novo", length = 24)
    private String statusNovo;
    @Column(length = 255)
    private String motivo;
    @Column(name = "usuario_login", length = 80)
    private String usuarioLogin;
    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}
