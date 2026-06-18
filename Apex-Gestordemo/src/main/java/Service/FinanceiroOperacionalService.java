package Service;

import DTO.ContaPagarRequestDTO;
import Model.*;
import Repository.*;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceiroOperacionalService {
    @Autowired private ContaReceberRepository contaReceberRepository;
    @Autowired private ContaPagarRepository contaPagarRepository;
    @Autowired private CondicaoPagamentoRepository condicaoPagamentoRepository;
    @Autowired private FornecedorRepository fornecedorRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private AuditTrailService auditTrailService;

    @Transactional
    public ContaReceber registrarVendaFinalizada(Venda venda, HttpServletRequest request) {
        return contaReceberRepository.findByVendaIdAndEmpresaIdAndFilialId(
                venda.getIdVenda(), venda.getEmpresaId(), venda.getFilialId()).orElseGet(() -> {
            ContaReceber conta = new ContaReceber();
            conta.setVendaId(venda.getIdVenda());
            conta.setClienteId(venda.getCliente() == null ? null : venda.getCliente().getIdCliente());
            conta.setFormaPagamentoId(venda.getFormaPagamentoRealId());
            conta.setCondicaoPagamentoId(venda.getCondicaoPagamentoId());
            conta.setCaixaSessaoId(venda.getCaixaSessaoId());
            conta.setEmpresaId(venda.getEmpresaId());
            conta.setFilialId(venda.getFilialId());
            BigDecimal desconto = venda.getDesconto() == null ? BigDecimal.ZERO : venda.getDesconto();
            conta.setDesconto(desconto);
            conta.setValorLiquido(venda.getValorTotal());
            conta.setValorPago(venda.getValorTotal());
            conta.setSaldo(BigDecimal.ZERO);
            conta.setValorBruto(venda.getValorTotal().add(desconto));
            int prazo = venda.getCondicaoPagamentoId() == null ? 0
                    : condicaoPagamentoRepository.findById(venda.getCondicaoPagamentoId())
                            .map(CondicaoPagamento::getPrazoDias).orElse(0);
            conta.setVencimento(LocalDate.now().plusDays(Math.max(0, prazo)));
            conta.setStatus(FinanceiroStatus.PAGO);
            conta.setCriadoEm(LocalDateTime.now());
            conta.setPagoEm(LocalDateTime.now());
            ContaReceber salva = contaReceberRepository.save(conta);
            auditTrailService.registrarPrivacidade("CONTA_RECEBER_CRIADA", "ContaReceber",
                    salva.getIdContaReceber(), "Financeiro", null,
                    "venda=" + venda.getIdVenda() + ";valor=" + salva.getValorLiquido(), request,
                    "Rastro financeiro automatico da venda finalizada.");
            return salva;
        });
    }

    public List<ContaReceber> listarReceber(Long empresaId, Long filialId) {
        return contaReceberRepository.findByEmpresaIdAndFilialIdOrderByVencimentoDesc(empresaId, filialId);
    }

    public List<ContaPagar> listarPagar(Long empresaId, Long filialId) {
        return contaPagarRepository.findByEmpresaIdAndFilialIdOrderByVencimentoDesc(empresaId, filialId);
    }

    @Transactional
    public ContaPagar criarContaPagar(ContaPagarRequestDTO body, Long empresaId, Long filialId,
            HttpServletRequest request) {
        if (body.fornecedorId() != null) {
            fornecedorRepository.findByIdFornecedorAndEmpresaIdAndFilialId(body.fornecedorId(), empresaId, filialId)
                    .orElseThrow(() -> new IllegalArgumentException("Fornecedor nao encontrado no tenant."));
        }
        if (body.categoriaId() != null) {
            categoriaRepository.findVisibleById(body.categoriaId(), empresaId, filialId)
                    .orElseThrow(() -> new IllegalArgumentException("Categoria nao encontrada no tenant."));
        }
        ContaPagar conta = new ContaPagar();
        conta.setFornecedorId(body.fornecedorId());
        conta.setCategoriaId(body.categoriaId());
        conta.setDescricao(body.descricao().trim());
        conta.setValor(body.valor());
        conta.setVencimento(body.vencimento());
        conta.setFormaPagamentoPrevistaId(body.formaPagamentoPrevistaId());
        conta.setEmpresaId(empresaId);
        conta.setFilialId(filialId);
        conta.setStatus(FinanceiroStatus.ABERTO);
        conta.setCriadoEm(LocalDateTime.now());
        ContaPagar salva = contaPagarRepository.save(conta);
        auditTrailService.registrarPrivacidade("CONTA_PAGAR_CRIADA", "ContaPagar", salva.getIdContaPagar(),
                "Financeiro", null, "valor=" + salva.getValor(), request, "Obrigacao financeira cadastrada.");
        return salva;
    }

    @Transactional
    public void cancelarPorVenda(Venda venda) {
        contaReceberRepository.findByVendaIdAndEmpresaIdAndFilialId(
                venda.getIdVenda(), venda.getEmpresaId(), venda.getFilialId()).ifPresent(conta -> {
            conta.setStatus(FinanceiroStatus.CANCELADO);
            conta.setValorPago(BigDecimal.ZERO);
            conta.setSaldo(BigDecimal.ZERO);
            contaReceberRepository.save(conta);
        });
    }

    @Transactional
    public void ajustarPorDevolucao(Venda venda, BigDecimal valor, boolean total) {
        contaReceberRepository.findByVendaIdAndEmpresaIdAndFilialId(
                venda.getIdVenda(), venda.getEmpresaId(), venda.getFilialId()).ifPresent(conta -> {
            conta.setStatus(total ? FinanceiroStatus.DEVOLVIDO : FinanceiroStatus.PARCIAL);
            conta.setValorLiquido(total ? BigDecimal.ZERO : conta.getValorLiquido().subtract(valor).max(BigDecimal.ZERO));
            conta.setValorPago(total ? BigDecimal.ZERO : conta.getValorPago().min(conta.getValorLiquido()));
            conta.setSaldo(BigDecimal.ZERO);
            contaReceberRepository.save(conta);
        });
    }
}
