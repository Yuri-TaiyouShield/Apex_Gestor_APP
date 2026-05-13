package Model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "usuario")
@Data
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;
    @Column(nullable = false, length = 45)
    private String nome;
    @Column(nullable = false, unique = true, length = 45)
    private String login;
    @Column(nullable = false, length = 255)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String senha;
    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;
    @Column(nullable = false)
    private int status;
    @ManyToOne
    @JoinColumn(name = "perfil_id", nullable = false)
    private Perfil perfil;
}
