package Service;

import DTO.RelatorioFinanceiroDTO;
import Model.*;
import Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FinanceiroService {

    @Autowired
    private DespesaRepository despesaRepository;
    @Autowired
    private VendaRepository vendaRepository;
    @Autowired
    private TipoDespesaRepository tipoDespesaRepository;

    public Despesa salvarDespesa(Despesa despesa) {
        return despesaRepository.save(despesa);
    }

    public List<Despesa> listarDespesas() {
        return despesaRepository.findAll();
    }

    public TipoDespesa salvarTipoDespesa(TipoDespesa td) {
        return tipoDespesaRepository.save(td);
    }

    public List<TipoDespesa> listarTiposDespesa() {
        return tipoDespesaRepository.findAll();
    }

    public RelatorioFinanceiroDTO gerarRelatorio(LocalDate inicio, LocalDate fim) {
        LocalDateTime inicioTime = inicio.atStartOfDay();
        LocalDateTime fimTime = fim.atTime(23, 59, 59);

        List<Venda> vendas = vendaRepository.findByPeriodo(inicioTime, fimTime);
        BigDecimal receita = BigDecimal.ZERO;
        BigDecimal cmv = BigDecimal.ZERO;

        for (Venda v : vendas) {
            receita = receita.add(v.getValorTotal());
            for (ProdutoVenda item : v.getItens()) {
                cmv = cmv.add(item.getCustoUnitario().multiply(new BigDecimal(item.getQuantidade())));
            }
        }

        List<Despesa> despesas = despesaRepository.findByDataVencimentoBetween(inicio, fim);
        BigDecimal totalDespesas = despesas.stream().map(Despesa::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal lucro = receita.subtract(cmv).subtract(totalDespesas);
        return new RelatorioFinanceiroDTO(receita, cmv, totalDespesas, lucro, lucro);
    }
}
