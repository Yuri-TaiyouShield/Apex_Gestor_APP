package Model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "financial_digital_document", indexes = {
        @Index(name = "idx_fin_doc_status_data", columnList = "status, gerado_em"),
        @Index(name = "idx_fin_doc_funcionario", columnList = "funcionario_email"),
        @Index(name = "idx_fin_doc_referencia", columnList = "referencia")
})
@Data
public class FinancialDigitalDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_financial_digital_document")
    private Long idFinancialDigitalDocument;

    @Column(name = "tipo_documento", nullable = false, length = 80)
    private String tipoDocumento;

    @Column(name = "funcionario_nome", nullable = false, length = 160)
    private String funcionarioNome;

    @Column(name = "funcionario_email", nullable = false, length = 160)
    private String funcionarioEmail;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(name = "cargo_assinante_obrigatorio", nullable = false, length = 80)
    private String cargoAssinanteObrigatorio;

    @Column(name = "gerado_por", nullable = false, length = 120)
    private String geradoPor;

    @Column(name = "gerado_em", nullable = false)
    private LocalDateTime geradoEm;

    @Column(name = "assinado_por", length = 160)
    private String assinadoPor;

    @Column(name = "cargo_assinante", length = 80)
    private String cargoAssinante;

    @Column(name = "assinado_em")
    private LocalDateTime assinadoEm;

    @Column(name = "enviado_em")
    private LocalDateTime enviadoEm;

    @Column(name = "assinatura_digital_hash", length = 128)
    private String assinaturaDigitalHash;

    @Column(name = "conteudo_hash", nullable = false, length = 128)
    private String conteudoHash;

    @Lob
    @Column(nullable = false)
    private String conteudo;

    @Column(name = "assunto_email", length = 180)
    private String assuntoEmail;

    @Lob
    @Column(name = "mensagem_email")
    private String mensagemEmail;

    @Column(length = 120)
    private String referencia;

    @Column(name = "ultimo_erro", length = 255)
    private String ultimoErro;
}
