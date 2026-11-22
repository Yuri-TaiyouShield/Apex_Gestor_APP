package Service;

import DTO.CaixaMovimentacaoResponseDTO;
import DTO.CaixaSessaoResponseDTO;
import DTO.CaixaFormaPagamentoResumoDTO;
import Model.CaixaMovimentacao;
import Model.CaixaSessao;
import Model.CaixaStatus;
import Repository.CaixaMovimentacaoRepository;
import Repository.CaixaSessaoRepository;
import Repository.VendaPagamentoRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaixaService {

    @Autowired
    private CaixaSessaoRepository caixaSessaoRepository;
    @Autowired
    private CaixaMovimentacaoRepository caixaMovimentacaoRepository;
    @Autowired
    private PrivacyGuardService privacyGuardService;
    @Autowired
    private AuditTrailService auditTrailService;
    @Autowired
    private VendaPagamentoRepository vendaPagamentoRepository;

    @Transactional(readOnly = true)
    public CaixaSessaoResponseDTO statusAtual(HttpServletRequest request) {
        return caixaAberto(request).map(this::toResponse).orElse(null);
    }

    public java.util.Optional<CaixaSessao> caixaAberto(HttpServletRequest request) {
        return caixaSessaoRepository.findFirstByOperadorLoginAndEmpresaIdAndFilialIdAndStatusOrderByAbertoEmDesc(
                privacyGuardService.currentUserLogin(request),
                empresaId(request),
                filialId(request),
                CaixaStatus.ABERTO);
    }

    public List<CaixaSessaoResponseDTO> historico(HttpServletRequest request) {
        boolean caixaPuro = privacyGuardService.currentRoles(request).contains("ROLE_CAIXA");
        String login = privacyGuardService.currentUserLogin(request);
        return caixaSessaoRepository.findTop20ByEmpresaIdAndFilialIdOrderByAbertoEmDesc(empresaId(request), filialId(request))
                .stream().filter(c -> !caixaPuro || login.equals(c.getOperadorLogin())).map(this::toResponse).toList();
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CaixaSessaoResponseDTO abrir(BigDecimal saldoInicial, HttpServletRequest request) {
        caixaAberto(request).ifPresent(open -> {
            throw new IllegalArgumentException("Ja existe caixa aberto para este operador nesta filial.");
        });
        CaixaSessao caixa = new CaixaSessao();
        caixa.setOperadorLogin(privacyGuardService.currentUserLogin(request));
        caixa.setEmpresaId(empresaId(request));
        caixa.setFilialId(filialId(request));
        caixa.setCaixaAbertoUnico(1);
        caixa.setStatus(CaixaStatus.ABERTO);
        caixa.setSaldoInicial(nonNegative(saldoInicial));
        caixa.setTotalVendas(BigDecimal.ZERO);
        caixa.setTotalSuprimentos(BigDecimal.ZERO);
        caixa.setTotalSangrias(BigDecimal.ZERO);
        caixa.setAbertoEm(LocalDateTime.now());
        CaixaSessao salvo = caixaSessaoRepository.save(caixa);
        registrarMovimento(salvo, "ABERTURA", salvo.getSaldoInicial(), "Fundo de troco inicial", "Caixa", salvo.getIdCaixaSessao());
        auditTrailService.registrarPrivacidade("CAIXA_ABERTO", "CaixaSessao", salvo.getIdCaixaSessao(), "Caixa",
                null, resumo(salvo), request, "Abertura de caixa operacional.");
        return toResponse(salvo);
    }

    @Transactional
    public CaixaSessaoResponseDTO suprimento(Long caixaId, BigDecimal valor, String observacao, HttpServletRequest request) {
        CaixaSessao caixa = abertoPorId(caixaId, request);
        BigDecimal amount = positive(valor);
        caixa.setTotalSuprimentos(caixa.getTotalSuprimentos().add(amount));
        CaixaSessao salvo = caixaSessaoRepository.save(caixa);
        registrarMovimento(salvo, "SUPRIMENTO", amount, observacao, "Caixa", salvo.getIdCaixaSessao());
        auditTrailService.registrarPrivacidade("CAIXA_SUPRIMENTO", "CaixaSessao", salvo.getIdCaixaSessao(), "Caixa",
                null, "valor=" + amount, request, "Suprimento de caixa.");
        return toResponse(salvo);
    }

    @Transactional
    public CaixaSessaoResponseDTO sangria(Long caixaId, BigDecimal valor, String observacao, HttpServletRequest request) {
        CaixaSessao caixa = abertoPorId(caixaId, request);
        BigDecimal amount = positive(valor);
        if (saldoAtual(caixa).compareTo(amount) < 0) {
            throw new IllegalArgumentException("Saldo insuficiente para sangria.");
        }
        caixa.setTotalSangrias(caixa.getTotalSangrias().add(amount));
        CaixaSessao salvo = caixaSessaoRepository.save(caixa);
        registrarMovimento(salvo, "SANGRIA", amount, observacao, "Caixa", salvo.getIdCaixaSessao());
        auditTrailService.registrarPrivacidade("CAIXA_SANGRIA", "CaixaSessao", salvo.getIdCaixaSessao(), "Caixa",
                null, "valor=" + amount, request, "Sangria de caixa.");
        return toResponse(salvo);
    }

    @Transactional
    public CaixaSessaoResponseDTO registrarVenda(BigDecimal valor, Long vendaId, HttpServletRequest request) {
        CaixaSessao caixa = caixaAberto(request)
                .orElseThrow(() -> new IllegalStateException("Abra o caixa antes de finalizar vendas neste perfil."));
        BigDecimal amount = positive(valor);
        caixa.setTotalVendas(caixa.getTotalVendas().add(amount));
        CaixaSessao salvo = caixaSessaoRepository.save(caixa);
        registrarMovimento(salvo, "VENDA", amount, "Recebimento de venda no PDV", "Venda", vendaId);
        auditTrailService.registrarPrivacidade("CAIXA_RECEBIMENTO_VENDA", "Venda", vendaId, "Caixa",
                null, "valor=" + amount + ";caixa=" + salvo.getIdCaixaSessao(), request, "Venda integrada ao caixa.");
        return toResponse(salvo);
    }

    @Transactional
    public CaixaSessaoResponseDTO fechar(Long caixaId, HttpServletRequest request) {
        return fechar(caixaId, null, request);
    }

    @Transactional
    public CaixaSessaoResponseDTO fechar(Long caixaId, BigDecimal saldoInformado, HttpServletRequest request) {
        CaixaSessao caixa = abertoPorId(caixaId, request);
        String anterior = resumo(caixa);
        caixa.setStatus(CaixaStatus.FECHADO);
        caixa.setCaixaAbertoUnico(null);
        BigDecimal esperado = saldoAtual(caixa);
        caixa.setSaldoFinal(esperado);
        caixa.setSaldoInformado(saldoInformado == null ? esperado : nonNegative(saldoInformado));
        caixa.setDiferencaFechamento(caixa.getSaldoInformado().subtract(esperado));
        caixa.setFechadoEm(LocalDateTime.now());
        CaixaSessao salvo = caixaSessaoRepository.save(caixa);
        registrarMovimento(salvo, "FECHAMENTO", salvo.getSaldoFinal(), "Fechamento diario", "Caixa", salvo.getIdCaixaSessao());
        auditTrailService.registrarPrivacidade("CAIXA_FECHADO", "CaixaSessao", salvo.getIdCaixaSessao(), "Caixa",
                anterior, resumo(salvo), request, salvo.getDiferencaFechamento().signum() == 0
                        ? "Fechamento de caixa operacional conciliado."
                        : "Fechamento de caixa com diferenca auditada.");
        return toResponse(salvo);
    }

    private CaixaSessao abertoPorId(Long caixaId, HttpServletRequest request) {
        CaixaSessao caixa = caixaSessaoRepository.findByIdCaixaSessaoAndEmpresaIdAndFilialId(
                        caixaId, empresaId(request), filialId(request))
                .orElseThrow(() -> new IllegalArgumentException("Caixa nao encontrado."));
        if (caixa.getStatus() != CaixaStatus.ABERTO) {
            throw new IllegalStateException("Caixa fechado bloqueia operacoes financeiras.");
        }
        if (!privacyGuardService.currentUserLogin(request).equals(caixa.getOperadorLogin())) {
            throw new org.springframework.security.access.AccessDeniedException("Caixa pertence a outro operador.");
        }
        return caixa;
    }

    private CaixaMovimentacao registrarMovimento(CaixaSessao caixa, String tipo, BigDecimal valor, String observacao,
            String referenciaTipo, Long referenciaId) {
        CaixaMovimentacao mov = new CaixaMovimentacao();
        mov.setCaixaSessao(caixa);
        mov.setTipo(tipo);
        mov.setValor(valor);
        mov.setObservacao(observacao);
        mov.setReferenciaTipo(referenciaTipo);
        mov.setReferenciaId(referenciaId);
        mov.setCriadoEm(LocalDateTime.now());
        return caixaMovimentacaoRepository.save(mov);
    }

    private CaixaSessaoResponseDTO toResponse(CaixaSessao caixa) {
        List<CaixaMovimentacaoResponseDTO> movimentacoes = caixa.getIdCaixaSessao() == null ? List.of()
                : caixaMovimentacaoRepository.findByCaixaSessaoIdCaixaSessaoOrderByCriadoEmDesc(caixa.getIdCaixaSessao())
                        .stream()
                        .map(this::toMovimentoResponse)
                        .toList();
        return new CaixaSessaoResponseDTO(
                caixa.getIdCaixaSessao(),
                caixa.getOperadorLogin(),
                caixa.getEmpresaId(),
                caixa.getFilialId(),
                caixa.getStatus().name(),
                caixa.getSaldoInicial(),
                caixa.getTotalVendas(),
                caixa.getTotalSuprimentos(),
                caixa.getTotalSangrias(),
                saldoAtual(caixa),
                caixa.getSaldoFinal(),
                caixa.getSaldoInformado(),
                caixa.getDiferencaFechamento(),
                caixa.getAbertoEm(),
                caixa.getFechadoEm(),
                movimentacoes,
                resumoPagamentos(caixa));
    }

    private List<CaixaFormaPagamentoResumoDTO> resumoPagamentos(CaixaSessao caixa) {
        if (caixa.getIdCaixaSessao() == null) return List.of();
        Map<Long, CaixaFormaPagamentoResumoDTO> totais = new LinkedHashMap<>();
        vendaPagamentoRepository.findByVendaCaixaSessaoIdAndVendaEmpresaIdAndVendaFilialId(
                caixa.getIdCaixaSessao(), caixa.getEmpresaId(), caixa.getFilialId()).forEach(p -> {
            Long id = p.getFormaPagamento().getIdFormaPagamento();
            CaixaFormaPagamentoResumoDTO atual = totais.get(id);
            totais.put(id, new CaixaFormaPagamentoResumoDTO(id, p.getFormaPagamento().getNome(),
                    (atual == null ? BigDecimal.ZERO : atual.total()).add(p.getValorPago()),
                    (atual == null ? 0L : atual.quantidadePagamentos()) + 1));
        });
        return List.copyOf(totais.values());
    }

    private CaixaMovimentacaoResponseDTO toMovimentoResponse(CaixaMovimentacao mov) {
        return new CaixaMovimentacaoResponseDTO(
                mov.getIdCaixaMovimentacao(),
                mov.getTipo(),
                mov.getValor(),
                mov.getObservacao(),
                mov.getReferenciaTipo(),
                mov.getReferenciaId(),
                mov.getCriadoEm());
    }

    private BigDecimal saldoAtual(CaixaSessao caixa) {
        return caixa.getSaldoInicial()
                .add(caixa.getTotalVendas())
                .add(caixa.getTotalSuprimentos())
                .subtract(caixa.getTotalSangrias());
    }

    private BigDecimal positive(BigDecimal value) {
        BigDecimal amount = nonNegative(value);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor deve ser maior que zero.");
        }
        return amount;
    }

    private BigDecimal nonNegative(BigDecimal value) {
        BigDecimal amount = value == null ? BigDecimal.ZERO : value;
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor nao pode ser negativo.");
        }
        return amount;
    }

    private Long empresaId(HttpServletRequest request) {
        Long value = privacyGuardService.currentEmpresaId(request);
        if (value == null) {
            throw new org.springframework.security.access.AccessDeniedException("Empresa ativa ausente.");
        }
        return value;
    }

    private Long filialId(HttpServletRequest request) {
        Long value = privacyGuardService.currentFilialId(request);
        if (value == null) {
            throw new org.springframework.security.access.AccessDeniedException("Filial ativa ausente.");
        }
        return value;
    }

    private String resumo(CaixaSessao caixa) {
        return "id=" + caixa.getIdCaixaSessao()
                + ";status=" + caixa.getStatus()
                + ";saldoAtual=" + saldoAtual(caixa)
                + ";operador=" + caixa.getOperadorLogin();
    }
}
