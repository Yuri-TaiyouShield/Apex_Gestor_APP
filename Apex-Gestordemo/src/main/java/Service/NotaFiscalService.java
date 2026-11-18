package Service;

import Model.*;
import Repository.*;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotaFiscalService {
    @Autowired private NotaFiscalEntradaRepository nfRepository;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private FornecedorRepository fornecedorRepository;
    @Autowired private EstoqueMovimentacaoService estoqueMovimentacaoService;
    @Autowired private AuditTrailService auditTrailService;

    @Transactional
    public NotaFiscalEntrada darEntrada(NotaFiscalEntrada nf, Long empresaId, Long filialId, HttpServletRequest request) {
        return darEntrada(nf, empresaId, filialId, request, null);
    }

    @Transactional
    public NotaFiscalEntrada darEntrada(NotaFiscalEntrada nf, Long empresaId, Long filialId,
            HttpServletRequest request, String idempotencyKey) {
        nf.setEmpresaId(empresaId);
        nf.setFilialId(filialId);
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        String payloadHash = payloadHash(nf);
        if (normalizedKey != null) {
            var previous = nfRepository.findByIdempotencyKeyAndEmpresaIdAndFilialId(
                    normalizedKey, empresaId, filialId);
            if (previous.isPresent()) {
                if (!payloadHash.equals(previous.get().getIdempotencyPayloadHash())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Idempotency-Key ja utilizada com outro payload.");
                }
                return previous.get();
            }
            nf.setIdempotencyKey(normalizedKey);
            nf.setIdempotencyPayloadHash(payloadHash);
        }
        nf.setFornecedor(fornecedorRepository.findByIdFornecedorAndEmpresaIdAndFilialId(
                nf.getFornecedor().getIdFornecedor(), empresaId, filialId)
                .orElseThrow(() -> new IllegalArgumentException("Fornecedor nao encontrado no tenant.")));
        LocalDateTime agora = LocalDateTime.now();
        if (nf.getDataEmissao() == null) {
            nf.setDataEmissao(agora);
        }
        nf.setDataEntrada(agora);
        if (nf.getDataCompetencia() == null) {
            nf.setDataCompetencia(nf.getDataEmissao().toLocalDate());
        }
        BigDecimal totalNF = BigDecimal.ZERO;
        for (ItemNotaFiscal item : nf.getItens()) {
            Produto produto = produtoRepository.findByIdProdutoAndEmpresaIdAndFilialId(
                    item.getProduto().getIdProduto(), empresaId, filialId)
                    .orElseThrow(() -> new IllegalArgumentException("Produto nao encontrado no tenant."));
            int qtdAtual = produto.getQuantidadeEstoque();
            int qtdNova = item.getQuantidade();
            BigDecimal custoAnterior = produto.getCusto() == null ? BigDecimal.ZERO : produto.getCusto();
            BigDecimal totalEstoque = custoAnterior.multiply(BigDecimal.valueOf(qtdAtual));
            BigDecimal totalEntrada = item.getValorCustoUnitario().multiply(BigDecimal.valueOf(qtdNova));
            BigDecimal custoPosterior = qtdAtual + qtdNova > 0
                    ? totalEstoque.add(totalEntrada).divide(BigDecimal.valueOf(qtdAtual + qtdNova), 2, RoundingMode.HALF_UP)
                    : item.getValorCustoUnitario();
            produto.setCusto(custoPosterior);
            produto.setQuantidadeEstoque(qtdAtual + qtdNova);
            produtoRepository.save(produto);
            estoqueMovimentacaoService.registrar(produto, TipoMovimentacaoEstoque.ENTRADA, qtdNova,
                    qtdAtual, qtdAtual + qtdNova, "NotaFiscalEntrada", null, "Entrada por nota fiscal",
                    item.getValorCustoUnitario(), custoAnterior, custoPosterior,
                    totalEntrada.setScale(2, RoundingMode.HALF_UP));
            item.setNotaFiscal(nf);
            item.setProduto(produto);
            totalNF = totalNF.add(totalEntrada);
        }
        nf.setValorTotal(totalNF);
        NotaFiscalEntrada salva = nfRepository.save(nf);
        auditTrailService.registrarPrivacidade("NOTA_FISCAL_ENTRADA", "NotaFiscalEntrada", salva.getIdNfEntrada(),
                "Estoque", null, "numero=" + salva.getNumero() + ";valor=" + salva.getValorTotal(), request,
                "Entrada de estoque por nota fiscal no tenant ativo.");
        return salva;
    }

    public NotaFiscalEntrada darEntrada(NotaFiscalEntrada nf) {
        return darEntrada(nf, nf.getEmpresaId(), nf.getFilialId(), null);
    }

    private String normalizeIdempotencyKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key excede 128 caracteres.");
        }
        return normalized;
    }

    private String payloadHash(NotaFiscalEntrada nf) {
        StringBuilder payload = new StringBuilder()
                .append(nf.getNumero()).append('|').append(nf.getSerie()).append('|')
                .append(nf.getFornecedor() == null ? null : nf.getFornecedor().getIdFornecedor()).append('|')
                .append(nf.getDataEmissao());
        if (nf.getItens() != null) {
            nf.getItens().forEach(item -> payload.append('|')
                    .append(item.getProduto() == null ? null : item.getProduto().getIdProduto())
                    .append(':').append(item.getQuantidade()).append(':').append(item.getValorCustoUnitario()));
        }
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(payload.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponivel", ex);
        }
    }

    @Transactional(readOnly = true)
    public List<NotaFiscalEntrada> listar(Long empresaId, Long filialId) {
        List<NotaFiscalEntrada> notas = nfRepository.findByEmpresaIdAndFilialIdOrderByDataEntradaDesc(empresaId, filialId);
        notas.forEach(n -> {
            org.hibernate.Hibernate.initialize(n.getFornecedor());
            org.hibernate.Hibernate.initialize(n.getItens());
            n.getItens().forEach(i -> org.hibernate.Hibernate.initialize(i.getProduto()));
        });
        return notas;
    }
}
