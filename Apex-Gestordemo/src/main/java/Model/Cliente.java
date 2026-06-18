package Model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "Cliente", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tipo_documento", "cpf_cnpj"})})
@Data
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Long idCliente;
    @Column(name = "nome_razao", nullable = false, length = 100)
    private String nomeRazao;
    @Column(name = "nome_fantasia", length = 100)
    private String nomeFantasia;
    @Column(nullable = false, length = 15)
    private String telefone;
    @Column(length = 15)
    private String celular;
    @Column(length = 120)
    private String email;
    @Column(name = "tipo_documento")
    private int tipoDocumento;
    @Column(name = "cpf_cnpj", nullable = false, length = 14)
    private String cpfCnpj;
    @Column(name = "rg_ie", length = 20)
    private String rgIe;
    @Column(name = "situacao_fiscal", length = 60)
    private String situacaoFiscal;
    @Column(length = 500)
    private String observacoes;
    @Column(name = "data_cadastro")
    private LocalDateTime dataCadastro;
    @Column(nullable = false)
    private int status;
    @Column(name = "empresa_id")
    private Long empresaId;
    @Column(name = "filial_id")
    private Long filialId;
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    @Column(name = "deleted_by", length = 80)
    private String deletedBy;
    @Column(name = "anonimizado_em")
    private LocalDateTime anonimizadoEm;
    @Column(name = "anonimizado_por", length = 80)
    private String anonimizadoPor;
    @Column(name = "motivo_retencao", length = 255)
    private String motivoRetencao;
    @Column(name = "base_legal", length = 120)
    private String baseLegal;
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "Cliente_Endereco", joinColumns = @JoinColumn(name = "cliente_id"), inverseJoinColumns = @JoinColumn(name = "endereco_id"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Endereco> enderecos;
}
