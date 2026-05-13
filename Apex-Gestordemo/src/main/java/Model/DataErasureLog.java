package Model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "Data_Erasure_Log")
@Data
public class DataErasureLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_data_erasure_log")
    private Long idDataErasureLog;

    @Column(name = "titular_hash", nullable = false, length = 128)
    private String titularHash;

    @Column(nullable = false, length = 120)
    private String motivo;

    @Column(name = "operador_login", length = 80)
    private String operadorLogin;

    @Column(name = "executado_em", nullable = false)
    private LocalDateTime executadoEm;
}
