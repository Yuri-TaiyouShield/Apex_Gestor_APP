package Service;

import Model.*;
import Repository.CreditoClienteRepository;
import Repository.CreditoMovimentacaoRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditoClienteService {
    @Autowired private CreditoClienteRepository creditoRepository;
    @Autowired private CreditoMovimentacaoRepository movimentacaoRepository;
    @Autowired private PrivacyGuardService privacyGuardService;
    @Autowired private AuditTrailService auditTrailService;

    public List<CreditoCliente> listarDisponiveis(Long clienteId, Long empresaId, Long filialId) {
        return creditoRepository.findByClienteIdAndEmpresaIdAndFilialIdAndStatusInOrderByCriadoEmAsc(
                clienteId, empresaId, filialId, List.of(CreditoStatus.DISPONIVEL, CreditoStatus.PARCIAL));
    }

    @Transactional
    public CreditoCliente gerarPorDevolucao(Venda venda, VendaDevolucao devolucao, HttpServletRequest request) {
        return creditoRepository.findByDevolucaoIdAndEmpresaIdAndFilialId(
                devolucao.getIdVendaDevolucao(), venda.getEmpresaId(), venda.getFilialId()).orElseGet(() -> {
            if (venda.getCliente() == null || venda.getCliente().getIdCliente() == null) {
                throw new IllegalArgumentException("Credito exige cliente vinculado a venda original.");
            }
            CreditoCliente credito = new CreditoCliente();
            credito.setClienteId(venda.getCliente().getIdCliente());
            credito.setVendaOrigemId(venda.getIdVenda());
            credito.setDevolucaoId(devolucao.getIdVendaDevolucao());
            credito.setEmpresaId(venda.getEmpresaId());
            credito.setFilialId(venda.getFilialId());
            credito.setValorOriginal(devolucao.getValorAjuste());
            credito.setSaldo(devolucao.getValorAjuste());
            credito.setStatus(CreditoStatus.DISPONIVEL);
            credito.setCriadoEm(LocalDateTime.now());
            CreditoCliente salvo = creditoRepository.save(credito);
            registrarMovimento(salvo, null, CreditoMovimentoTipo.GERACAO, salvo.getSaldo(), request);
            auditTrailService.registrarPrivacidade("CREDITO_GERADO_DEVOLUCAO", "CreditoCliente",
                    salvo.getIdCredito(), "Financeiro", null,
                    "vendaOrigem=" + venda.getIdVenda() + ";devolucao=" + devolucao.getIdVendaDevolucao()
                            + ";valor=" + salvo.getSaldo(),
                    request, "Credito com origem em devolucao auditada.");
            return salvo;
        });
    }

    @Transactional
    public CreditoCliente usar(Long creditoId, Venda venda, BigDecimal valor, HttpServletRequest request) {
        if (creditoId == null) throw new IllegalArgumentException("Credito/vale exige identificador de origem.");
        CreditoCliente credito = creditoRepository.lockForUse(
                creditoId, venda.getCliente().getIdCliente(), venda.getEmpresaId(), venda.getFilialId())
                .orElseThrow(() -> new IllegalArgumentException("Credito nao encontrado para o cliente e tenant da venda."));
        if (!List.of(CreditoStatus.DISPONIVEL, CreditoStatus.PARCIAL).contains(credito.getStatus())) {
            throw new IllegalArgumentException("Credito nao esta disponivel para uso.");
        }
        if (valor == null || valor.signum() <= 0 || credito.getSaldo().compareTo(valor) < 0) {
            throw new IllegalArgumentException("Saldo de credito insuficiente.");
        }
        credito.setSaldo(credito.getSaldo().subtract(valor));
        credito.setStatus(credito.getSaldo().signum() == 0 ? CreditoStatus.UTILIZADO : CreditoStatus.PARCIAL);
        CreditoCliente salvo = creditoRepository.save(credito);
        registrarMovimento(salvo, venda.getIdVenda(), CreditoMovimentoTipo.USO, valor, request);
        auditTrailService.registrarPrivacidade("CREDITO_UTILIZADO", "CreditoCliente", salvo.getIdCredito(),
                "Financeiro", null, "venda=" + venda.getIdVenda() + ";valor=" + valor + ";saldo=" + salvo.getSaldo(),
                request, "Uso de credito com origem e cliente validados.");
        return salvo;
    }

    private void registrarMovimento(CreditoCliente credito, Long vendaId, CreditoMovimentoTipo tipo,
            BigDecimal valor, HttpServletRequest request) {
        CreditoMovimentacao movimento = new CreditoMovimentacao();
        movimento.setCreditoId(credito.getIdCredito()); movimento.setVendaId(vendaId);
        movimento.setEmpresaId(credito.getEmpresaId()); movimento.setFilialId(credito.getFilialId());
        movimento.setTipo(tipo); movimento.setValor(valor);
        movimento.setUsuarioLogin(privacyGuardService.currentUserLogin(request)); movimento.setCriadoEm(LocalDateTime.now());
        movimentacaoRepository.save(movimento);
    }
}
