package Service;

import DTO.AdmCalcRequestDTO;
import DTO.DreComparativoFiliaisResponseDTO;
import DTO.DreContaDetalheDTO;
import DTO.DreDiagnosticoGerencialDTO;
import DTO.DreDrillDownResponseDTO;
import DTO.DreEvolucaoPontoDTO;
import DTO.DreEvolucaoResponseDTO;
import DTO.DreFatorRelevanteDTO;
import DTO.DreFilialColunaDTO;
import DTO.DreGerencialResponseDTO;
import DTO.DreIndicadoresResumoDTO;
import DTO.DreLancamentoOrigemDTO;
import DTO.DreLinhaDTO;
import Model.CaixaMovimentacao;
import Model.CentroCusto;
import Model.ClassificacaoCusto;
import Model.ContaPagar;
import Model.ContaReceber;
import Model.Despesa;
import Model.Filial;
import Model.FinanceiroStatus;
import Model.GrupoDreGerencial;
import Model.NaturezaContaGerencial;
import Model.NotaFiscalEntrada;
import Model.PlanoContasGerencial;
import Model.ProdutoVenda;
import Model.TipoContaGerencial;
import Model.TipoDevolucao;
import Model.Venda;
import Model.VendaDevolucao;
import Model.VendaDevolucaoItem;
import Repository.CaixaMovimentacaoRepository;
import Repository.CentroCustoRepository;
import Repository.ContaPagarRepository;
import Repository.ContaReceberRepository;
import Repository.DespesaRepository;
import Repository.FilialRepository;
import Repository.NotaFiscalEntradaRepository;
import Repository.PlanoContasGerencialRepository;
import Repository.VendaDevolucaoRepository;
import Repository.VendaRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DreGerencialService {

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final int MAX_DIAS_PERIODO = 731;
    private static final int MAX_MESES_EVOLUCAO = 24;

    private record DefinicaoLinhaDre(
            int numero,
            String codigo,
            String titulo,
            String tipoLinha,
            String sinal,
            List<GrupoDreGerencial> grupos,
            String explicacao) {
    }

    private static final List<DefinicaoLinhaDre> ESTRUTURA_17_LINHAS = List.of(
            new DefinicaoLinhaDre(1, "RECEITA_BRUTA_VENDAS", "1. Receita Bruta de Vendas",
                    "RECEITA", "+", List.of(GrupoDreGerencial.RECEITA_BRUTA),
                    "Total faturado com vendas finalizadas e receitas operacionais pelo regime de competencia, antes de devolucoes, descontos e impostos."),
            new DefinicaoLinhaDre(2, "DEVOLUCOES_CANCELAMENTOS", "2. (-) Devolucoes e Cancelamentos",
                    "DEDUCAO", "-", List.of(GrupoDreGerencial.DEVOLUCOES_CANCELAMENTOS),
                    "Vendas devolvidas pelos clientes no periodo que reduzem o faturamento bruto."),
            new DefinicaoLinhaDre(3, "DESCONTOS_CONCEDIDOS", "3. (-) Descontos Concedidos",
                    "DEDUCAO", "-", List.of(GrupoDreGerencial.DESCONTOS_CONCEDIDOS),
                    "Descontos comerciais concedidos nas vendas finalizadas do periodo."),
            new DefinicaoLinhaDre(4, "TRIBUTOS_SOBRE_VENDAS", "4. (-) Deducoes e Tributos sobre Vendas",
                    "DEDUCAO", "-", List.of(GrupoDreGerencial.TRIBUTOS_SOBRE_VENDAS),
                    "Impostos incidentes diretamente sobre o faturamento (Simples Nacional, ICMS, PIS, COFINS, ISS)."),
            new DefinicaoLinhaDre(5, "RECEITA_LIQUIDA", "5. (=) Receita Liquida Operacional",
                    "RESULTADO_PARCIAL", "=", List.of(),
                    "Quanto efetivamente fica na empresa apos abater devolucoes, descontos e impostos sobre vendas. Base 100% da Analise Vertical."),
            new DefinicaoLinhaDre(6, "CMV", "6. (-) CMV - Custo das Mercadorias Vendidas",
                    "CUSTO", "-", List.of(GrupoDreGerencial.CMV),
                    "Custo de aquisicao dos produtos que foram efetivamente vendidos no periodo (abatendo itens devolvidos ao estoque)."),
            new DefinicaoLinhaDre(7, "LUCRO_BRUTO", "7. (=) Lucro Bruto",
                    "RESULTADO_PARCIAL", "=", List.of(),
                    "Ganho direto das vendas apos pagar o custo das mercadorias vendidas, disponivel para cobrir as despesas da loja."),
            new DefinicaoLinhaDre(8, "DESPESAS_COMERCIAIS", "8. (-) Despesas Comerciais",
                    "DESPESA", "-", List.of(GrupoDreGerencial.DESPESA_COMERCIAL),
                    "Gastos para vender e atrair clientes: comissoes, propaganda, embalagens e acoes promocionais."),
            new DefinicaoLinhaDre(9, "DESPESAS_ADMINISTRATIVAS", "9. (-) Despesas Administrativas",
                    "DESPESA", "-", List.of(GrupoDreGerencial.DESPESA_ADMINISTRATIVA),
                    "Gastos de gestao e suporte: contabilidade, sistemas, material de escritorio e servicos terceirizados."),
            new DefinicaoLinhaDre(10, "DESPESAS_OPERACIONAIS", "10. (-) Despesas Operacionais e Infraestrutura",
                    "DESPESA", "-", List.of(GrupoDreGerencial.DESPESA_OPERACIONAL, GrupoDreGerencial.DESPESA_INFRAESTRUTURA),
                    "Gastos de funcionamento e ocupacao da loja: aluguel, energia, agua, internet, manutencao e limpeza."),
            new DefinicaoLinhaDre(11, "DESPESAS_PESSOAL", "11. (-) Despesas com Pessoal",
                    "DESPESA", "-", List.of(GrupoDreGerencial.DESPESA_PESSOAL),
                    "Salarios, pro-labore, ferias, 13o, FGTS, INSS e beneficios da equipe."),
            new DefinicaoLinhaDre(12, "OUTRAS_RECEITAS_DESPESAS_OPERACIONAIS", "12. (+/-) Outras Receitas e Despesas Operacionais",
                    "OUTRAS", "+/-", List.of(
                            GrupoDreGerencial.OUTRAS_RECEITAS,
                            GrupoDreGerencial.OUTRAS_DESPESAS),
                    "Saldo liquido entre outras receitas operacionais eventuais e outras despesas operacionais nao classificadas nos grupos anteriores."),
            new DefinicaoLinhaDre(13, "RESULTADO_OPERACIONAL", "13. (=) Resultado Operacional (Antes do Resultado Financeiro)",
                    "RESULTADO_PARCIAL", "=", List.of(),
                    "Resultado gerado exclusivamente pela operacao principal da loja, antes do impacto de juros, tarifas e rendimentos financeiros (CPC 26)."),
            new DefinicaoLinhaDre(14, "RESULTADO_FINANCEIRO_LIQUIDO", "14. (+/-) Resultado Financeiro Liquido",
                    "FINANCEIRO", "+/-", List.of(
                            GrupoDreGerencial.RECEITA_FINANCEIRA,
                            GrupoDreGerencial.DESPESA_FINANCEIRA),
                    "Diferenca entre receitas financeiras (rendimentos, descontos obtidos) e despesas financeiras (juros pagos, tarifas bancarias e taxas de antecipacao/maquininha)."),
            new DefinicaoLinhaDre(15, "RESULTADO_ANTES_TRIBUTOS", "15. (=) Resultado Antes dos Tributos sobre o Lucro (LAIR)",
                    "RESULTADO_PARCIAL", "=", List.of(),
                    "Resultado economico operacional somado ao resultado financeiro liquido, antes da deducao de IRPJ e CSLL."),
            new DefinicaoLinhaDre(16, "TRIBUTOS_SOBRE_LUCRO", "16. (-) Tributos sobre o Lucro",
                    "DEDUCAO", "-", List.of(GrupoDreGerencial.TRIBUTOS_SOBRE_LUCRO),
                    "IRPJ e CSLL quando aplicaveis ao regime tributario e devidamente parametrizados/lancados."),
            new DefinicaoLinhaDre(17, "RESULTADO_LIQUIDO_GERENCIAL", "17. (=) Resultado Liquido Gerencial do Exercicio",
                    "RESULTADO_FINAL", "=", List.of(),
                    "Lucro ou prejuizo economico final do periodo pelo regime de competencia. Indica se a empresa gerou ou consumiu riqueza.")
    );

    private final PlanoContasGerencialRepository planoContasRepository;
    private final CentroCustoRepository centroCustoRepository;
    private final FilialRepository filialRepository;
    private final VendaRepository vendaRepository;
    private final VendaDevolucaoRepository vendaDevolucaoRepository;
    private final DespesaRepository despesaRepository;
    private final ContaPagarRepository contaPagarRepository;
    private final ContaReceberRepository contaReceberRepository;
    private final CaixaMovimentacaoRepository caixaMovimentacaoRepository;
    private final NotaFiscalEntradaRepository notaFiscalEntradaRepository;
    private final ConsolidacaoCompetenciaService consolidacaoService;
    private final AdmCalcEngine admCalcEngine;
    private final FinancialAuditTrailService financialAuditTrailService;
    private final AuditTrailService auditTrailService;
    private final PrivacyGuardService privacyGuardService;

    public DreGerencialService(
            PlanoContasGerencialRepository planoContasRepository,
            CentroCustoRepository centroCustoRepository,
            FilialRepository filialRepository,
            VendaRepository vendaRepository,
            VendaDevolucaoRepository vendaDevolucaoRepository,
            DespesaRepository despesaRepository,
            ContaPagarRepository contaPagarRepository,
            ContaReceberRepository contaReceberRepository,
            CaixaMovimentacaoRepository caixaMovimentacaoRepository,
            NotaFiscalEntradaRepository notaFiscalEntradaRepository,
            ConsolidacaoCompetenciaService consolidacaoService,
            AdmCalcEngine admCalcEngine,
            FinancialAuditTrailService financialAuditTrailService,
            AuditTrailService auditTrailService,
            PrivacyGuardService privacyGuardService) {
        this.planoContasRepository = planoContasRepository;
        this.centroCustoRepository = centroCustoRepository;
        this.filialRepository = filialRepository;
        this.vendaRepository = vendaRepository;
        this.vendaDevolucaoRepository = vendaDevolucaoRepository;
        this.despesaRepository = despesaRepository;
        this.contaPagarRepository = contaPagarRepository;
        this.contaReceberRepository = contaReceberRepository;
        this.caixaMovimentacaoRepository = caixaMovimentacaoRepository;
        this.notaFiscalEntradaRepository = notaFiscalEntradaRepository;
        this.consolidacaoService = consolidacaoService;
        this.admCalcEngine = admCalcEngine;
        this.financialAuditTrailService = financialAuditTrailService;
        this.auditTrailService = auditTrailService;
        this.privacyGuardService = privacyGuardService;
    }

    public record FiltroDreRequest(
            String tipoPeriodo,
            String competencia,
            Integer ano,
            Integer trimestre,
            Integer semestre,
            LocalDate dataInicio,
            LocalDate dataFim,
            Boolean compararPeriodoAnterior,
            String competenciaComparacao,
            LocalDate comparacaoInicio,
            LocalDate comparacaoFim,
            Long centroCustoId,
            Long filialId,
            Boolean consolidarFiliais) {
    }

    @Transactional(readOnly = true)
    public DreGerencialResponseDTO apurarDre(
            FiltroDreRequest filtro,
            Long empresaId,
            Long currentFilialId) {
        requireTenant(empresaId, currentFilialId);
        JanelaPeriodo janela = resolverPeriodo(filtro);
        EscopoFiliais escopo = resolverEscopoFiliais(filtro.filialId(), filtro.consolidarFiliais(), empresaId, currentFilialId);
        CentroCusto centroFiltro = validarCentroCustoFiltro(filtro.centroCustoId(), empresaId, escopo.filialReferenciaPlano());

        ApuracaoBruta principal = apurarIntervalo(
                empresaId,
                escopo.filiaisIds(),
                escopo.filialReferenciaPlano(),
                centroFiltro == null ? null : centroFiltro.getIdCentroCusto(),
                janela.inicio(),
                janela.fim());

        boolean comparar = filtro.compararPeriodoAnterior() == null || Boolean.TRUE.equals(filtro.compararPeriodoAnterior())
                || filtro.competenciaComparacao() != null
                || (filtro.comparacaoInicio() != null && filtro.comparacaoFim() != null);

        ApuracaoBruta comparacao = comparar
                ? apurarIntervalo(
                        empresaId,
                        escopo.filiaisIds(),
                        escopo.filialReferenciaPlano(),
                        centroFiltro == null ? null : centroFiltro.getIdCentroCusto(),
                        janela.comparacaoInicio(),
                        janela.comparacaoFim())
                : null;

        return montarDreResponse(
                janela,
                comparar,
                empresaId,
                escopo,
                centroFiltro,
                principal,
                comparacao);
    }

    @Transactional(readOnly = true)
    public DreDrillDownResponseDTO detalharLancamentos(
            FiltroDreRequest filtro,
            Integer numeroLinha,
            String codigoLinha,
            Long planoContaId,
            GrupoDreGerencial grupoDre,
            Long empresaId,
            Long currentFilialId) {
        requireTenant(empresaId, currentFilialId);
        JanelaPeriodo janela = resolverPeriodo(filtro);
        EscopoFiliais escopo = resolverEscopoFiliais(filtro.filialId(), filtro.consolidarFiliais(), empresaId, currentFilialId);
        CentroCusto centroFiltro = validarCentroCustoFiltro(filtro.centroCustoId(), empresaId, escopo.filialReferenciaPlano());

        ApuracaoBruta apuracao = apurarIntervalo(
                empresaId,
                escopo.filiaisIds(),
                escopo.filialReferenciaPlano(),
                centroFiltro == null ? null : centroFiltro.getIdCentroCusto(),
                janela.inicio(),
                janela.fim());

        DefinicaoLinhaDre defLinha = resolverDefinicaoLinha(numeroLinha, codigoLinha);
        Set<Long> contasFiltroIds = resolverContasComDescendentes(planoContaId, apuracao.contasPorId());

        List<DreLancamentoOrigemDTO> filtrados = apuracao.lancamentos().stream()
                .filter(l -> defLinha == null || l.numeroLinhaDre() == defLinha.numero())
                .filter(l -> contasFiltroIds.isEmpty()
                        || (l.planoContaId() != null && contasFiltroIds.contains(l.planoContaId()))
                        || (planoContaId != null && planoContaId == -1L && l.planoContaId() == null))
                .filter(l -> grupoDre == null || l.grupoDre() == grupoDre)
                .sorted(Comparator.comparing(DreLancamentoOrigemDTO::dataCompetencia,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(DreLancamentoOrigemDTO::registroId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        BigDecimal soma = filtrados.stream()
                .map(DreLancamentoOrigemDTO::valorCompetencia)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        PlanoContasGerencial contaRef = planoContaId != null && planoContaId > 0
                ? apuracao.contasPorId().get(planoContaId)
                : null;

        return new DreDrillDownResponseDTO(
                janela.rotulo(),
                janela.inicio(),
                janela.fim(),
                defLinha == null ? numeroLinha : defLinha.numero(),
                defLinha == null ? codigoLinha : defLinha.codigo(),
                defLinha == null ? null : defLinha.titulo(),
                planoContaId,
                contaRef == null ? (planoContaId != null && planoContaId == -1L ? "Sem Classificacao" : null) : contaRef.getNome(),
                grupoDre,
                centroFiltro == null ? null : centroFiltro.getIdCentroCusto(),
                escopo.consolidadoEmpresa() ? null : escopo.filialUnicaId(),
                escopo.consolidadoEmpresa(),
                filtrados.size(),
                scale2(soma),
                filtrados);
    }

    @Transactional(readOnly = true)
    public DreEvolucaoResponseDTO evoluirDre(
            String competenciaInicio,
            String competenciaFim,
            Integer meses,
            Long centroCustoId,
            Long filialId,
            Boolean consolidarFiliais,
            Long empresaId,
            Long currentFilialId) {
        requireTenant(empresaId, currentFilialId);
        EscopoFiliais escopo = resolverEscopoFiliais(filialId, consolidarFiliais, empresaId, currentFilialId);
        CentroCusto centroFiltro = validarCentroCustoFiltro(centroCustoId, empresaId, escopo.filialReferenciaPlano());

        YearMonth fimYm = (competenciaFim != null && !competenciaFim.isBlank())
                ? consolidacaoService.parseCompetencia(competenciaFim)
                : YearMonth.now();
        int qtdMeses = meses == null ? 6 : Math.min(Math.max(meses, 1), MAX_MESES_EVOLUCAO);
        YearMonth inicioYm = (competenciaInicio != null && !competenciaInicio.isBlank())
                ? consolidacaoService.parseCompetencia(competenciaInicio)
                : fimYm.minusMonths(qtdMeses - 1L);

        if (fimYm.isBefore(inicioYm)) {
            throw new IllegalArgumentException("Competencia final nao pode ser anterior a competencia inicial.");
        }
        long monthsBetween = ChronoUnit.MONTHS.between(inicioYm, fimYm) + 1;
        if (monthsBetween > MAX_MESES_EVOLUCAO) {
            throw new IllegalArgumentException("Evolucao da DRE suporta no maximo " + MAX_MESES_EVOLUCAO + " meses por consulta.");
        }

        List<DreEvolucaoPontoDTO> serie = new ArrayList<>();
        BigDecimal receitaLiquidaAcum = BigDecimal.ZERO;
        BigDecimal lucroBrutoAcum = BigDecimal.ZERO;
        BigDecimal resultadoLiquidoAcum = BigDecimal.ZERO;

        YearMonth cursor = inicioYm;
        while (!cursor.isAfter(fimYm)) {
            ApuracaoBruta mesApuracao = apurarIntervalo(
                    empresaId,
                    escopo.filiaisIds(),
                    escopo.filialReferenciaPlano(),
                    centroFiltro == null ? null : centroFiltro.getIdCentroCusto(),
                    cursor.atDay(1),
                    cursor.atEndOfMonth());
            Totais17Linhas t = calcularTotais17Linhas(mesApuracao);
            BigDecimal mb = calcPercent(t.lucroBruto(), t.receitaLiquida());
            BigDecimal mo = calcPercent(t.resultadoOperacional(), t.receitaLiquida());
            BigDecimal ml = calcPercent(t.resultadoLiquidoGerencial(), t.receitaLiquida());

            serie.add(new DreEvolucaoPontoDTO(
                    cursor.toString(),
                    formatarRotuloMes(cursor),
                    scale2(t.receitaBruta()),
                    scale2(t.receitaLiquida()),
                    scale2(t.cmv()),
                    scale2(t.lucroBruto()),
                    scale2(t.despesasOperacionaisTotais()),
                    scale2(t.resultadoOperacional()),
                    scale2(t.resultadoLiquidoGerencial()),
                    mb == null ? BigDecimal.ZERO : mb,
                    mo == null ? BigDecimal.ZERO : mo,
                    ml == null ? BigDecimal.ZERO : ml,
                    scale2(mesApuracao.entradasRealizadasCaixa().subtract(mesApuracao.saidasRealizadasCaixa()))));

            receitaLiquidaAcum = receitaLiquidaAcum.add(t.receitaLiquida());
            lucroBrutoAcum = lucroBrutoAcum.add(t.lucroBruto());
            resultadoLiquidoAcum = resultadoLiquidoAcum.add(t.resultadoLiquidoGerencial());
            cursor = cursor.plusMonths(1);
        }

        BigDecimal margemMedia = calcPercent(resultadoLiquidoAcum, receitaLiquidaAcum);
        String tendenciaReceita = avaliarTendencia(
                serie.isEmpty() ? BigDecimal.ZERO : serie.get(0).receitaLiquida(),
                serie.isEmpty() ? BigDecimal.ZERO : serie.get(serie.size() - 1).receitaLiquida());
        String tendenciaResultado = avaliarTendencia(
                serie.isEmpty() ? BigDecimal.ZERO : serie.get(0).resultadoLiquidoGerencial(),
                serie.isEmpty() ? BigDecimal.ZERO : serie.get(serie.size() - 1).resultadoLiquidoGerencial());

        return new DreEvolucaoResponseDTO(
                inicioYm.toString(),
                fimYm.toString(),
                serie.size(),
                empresaId,
                escopo.consolidadoEmpresa() ? null : escopo.filialUnicaId(),
                escopo.consolidadoEmpresa(),
                centroFiltro == null ? null : centroFiltro.getIdCentroCusto(),
                scale2(receitaLiquidaAcum),
                scale2(lucroBrutoAcum),
                scale2(resultadoLiquidoAcum),
                margemMedia == null ? BigDecimal.ZERO : margemMedia,
                tendenciaReceita,
                tendenciaResultado,
                serie);
    }

    @Transactional(readOnly = true)
    public DreComparativoFiliaisResponseDTO compararFiliais(
            FiltroDreRequest filtro,
            Long empresaId,
            Long currentFilialId) {
        requireTenant(empresaId, currentFilialId);
        JanelaPeriodo janela = resolverPeriodo(filtro);
        List<FilialInfo> filiaisEmpresa = listarFiliaisEmpresa(empresaId, currentFilialId);
        List<Long> todasIds = filiaisEmpresa.stream().map(FilialInfo::idFilial).toList();

        ApuracaoBruta consolidada = apurarIntervalo(
                empresaId,
                todasIds,
                currentFilialId,
                filtro.centroCustoId(),
                janela.inicio(),
                janela.fim());
        Totais17Linhas totaisEmpresa = calcularTotais17Linhas(consolidada);
        DreIndicadoresResumoDTO indicadoresEmpresa = montarIndicadores(consolidada, totaisEmpresa);

        List<DreFilialColunaDTO> colunas = new ArrayList<>();
        for (FilialInfo fil : filiaisEmpresa) {
            ApuracaoBruta apFilial = apurarIntervalo(
                    empresaId,
                    List.of(fil.idFilial()),
                    fil.idFilial(),
                    filtro.centroCustoId(),
                    janela.inicio(),
                    janela.fim());
            Totais17Linhas tf = calcularTotais17Linhas(apFilial);
            List<DreLinhaDTO> linhasFilial = construir17Linhas(apFilial, tf, null, null);
            BigDecimal partReceita = calcPercent(tf.receitaLiquida(), totaisEmpresa.receitaLiquida());
            BigDecimal partResultado = calcPercent(tf.resultadoLiquidoGerencial(), totaisEmpresa.resultadoLiquidoGerencial());

            colunas.add(new DreFilialColunaDTO(
                    fil.idFilial(),
                    fil.codigo(),
                    fil.nome(),
                    scale2(tf.receitaBruta()),
                    scale2(tf.receitaLiquida()),
                    scale2(tf.cmv()),
                    scale2(tf.lucroBruto()),
                    scale2(tf.despesasOperacionaisTotais()),
                    scale2(tf.resultadoOperacional()),
                    scale2(tf.resultadoLiquidoGerencial()),
                     defaultedPercent(calcPercent(tf.lucroBruto(), tf.receitaLiquida())),
                    defaultedPercent(calcPercent(tf.resultadoLiquidoGerencial(), tf.receitaLiquida())),
                    defaultedPercent(partReceita),
                    defaultedPercent(partResultado),
                    linhasFilial));
        }

        List<String> destaques = new ArrayList<>();
        if (colunas.size() > 1) {
            DreFilialColunaDTO maiorReceita = colunas.stream()
                    .max(Comparator.comparing(DreFilialColunaDTO::receitaLiquida))
                    .orElse(null);
            DreFilialColunaDTO maiorLucro = colunas.stream()
                    .max(Comparator.comparing(DreFilialColunaDTO::resultadoLiquidoGerencial))
                    .orElse(null);
            if (maiorReceita != null) {
                destaques.add("Maior Receita Liquida: " + maiorReceita.filialNome()
                        + " (R$ " + maiorReceita.receitaLiquida() + " / "
                        + maiorReceita.participacaoReceitaLiquidaPercentual() + "% da empresa).");
            }
            if (maiorLucro != null) {
                destaques.add("Maior Resultado Liquido: " + maiorLucro.filialNome()
                        + " (R$ " + maiorLucro.resultadoLiquidoGerencial()
                        + ", margem liquida de " + maiorLucro.margemLiquidaPercentual() + "%).");
            }
        } else {
            destaques.add("Empresa opera com 1 filial ativa cadastrada no periodo consultado.");
        }

        return new DreComparativoFiliaisResponseDTO(
                janela.tipoPeriodo(),
                janela.rotulo(),
                janela.inicio(),
                janela.fim(),
                empresaId,
                filtro.centroCustoId(),
                indicadoresEmpresa,
                colunas,
                destaques);
    }

    @Transactional
    public String exportarCsv(
            FiltroDreRequest filtro,
            Long empresaId,
            Long currentFilialId,
            HttpServletRequest request) {
        DreGerencialResponseDTO dre = apurarDre(filtro, empresaId, currentFilialId);
        registrarAuditoriaExportacao(dre, "CSV", empresaId, currentFilialId, request);

        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF'); // UTF-8 BOM para Excel PT-BR
        sb.append("DRE GERENCIAL - APEX GESTOR ERP\n");
        sb.append("Periodo;").append(csvSafe(dre.rotuloPeriodo()))
                .append(";Inicio;").append(dre.dataInicio())
                .append(";Fim;").append(dre.dataFim()).append("\n");
        sb.append("Escopo;").append(csvSafe(dre.filialNome()))
                .append(";Base Analise Vertical;").append(csvSafe(dre.baseAnaliseVertical())).append("\n\n");

        sb.append("Linha;Codigo;Descricao;Sinal;Valor Periodo (R$);Analise Vertical (%);Valor Comparacao (R$);Variacao (R$);Analise Horizontal (%);Status Variacao;Qtd Lancamentos\n");
        for (DreLinhaDTO linha : dre.linhas()) {
            sb.append(linha.numeroLinha()).append(';')
                    .append(csvSafe(linha.codigoLinha())).append(';')
                    .append(csvSafe(linha.titulo())).append(';')
                    .append(csvSafe(linha.sinalDemonstracao())).append(';')
                    .append(formatCsvDecimal(linha.valor())).append(';')
                    .append(formatCsvDecimal(linha.analiseVerticalPercentual())).append(';')
                    .append(formatCsvDecimal(linha.valorComparacao())).append(';')
                    .append(formatCsvDecimal(linha.variacaoAbsoluta())).append(';')
                    .append(formatCsvDecimal(linha.analiseHorizontalPercentual())).append(';')
                    .append(csvSafe(linha.statusVariacao())).append(';')
                    .append(linha.quantidadeLancamentos()).append('\n');

            appendContasCsv(sb, linha.numeroLinha(), linha.contas(), 1);
        }

        sb.append("\nResumo Gerencial;Valor\n");
        sb.append("Margem Bruta (%);").append(formatCsvDecimal(dre.indicadores().margemBrutaPercentual())).append('\n');
        sb.append("Margem Operacional (%);").append(formatCsvDecimal(dre.indicadores().margemOperacionalPercentual())).append('\n');
        sb.append("Margem Liquida (%);").append(formatCsvDecimal(dre.indicadores().margemLiquidaPercentual())).append('\n');
        sb.append("Margem de Contribuicao (R$);").append(formatCsvDecimal(dre.indicadores().margemContribuicaoValor())).append('\n');
        sb.append("Ponto de Equilibrio Contabil (R$);").append(formatCsvDecimal(dre.indicadores().pontoEquilibrioContabilReceita())).append('\n');
        sb.append("Saldo Realizado no Caixa (R$);").append(formatCsvDecimal(dre.indicadores().saldoRealizadoCaixa())).append('\n');
        sb.append("Aviso Contabil;Demonstracao gerencial por regime de competencia. Nao substitui escrituracao contabil societaria/fiscal oficial.\n");
        return sb.toString();
    }

    @Transactional
    public DreGerencialResponseDTO exportarJsonAuditado(
            FiltroDreRequest filtro,
            Long empresaId,
            Long currentFilialId,
            HttpServletRequest request) {
        DreGerencialResponseDTO dre = apurarDre(filtro, empresaId, currentFilialId);
        registrarAuditoriaExportacao(dre, "JSON", empresaId, currentFilialId, request);
        return dre;
    }

    private void appendContasCsv(StringBuilder sb, int numeroLinha, List<DreContaDetalheDTO> contas, int depth) {
        if (contas == null || contas.isEmpty()) {
            return;
        }
        String prefix = "  ".repeat(depth);
        for (DreContaDetalheDTO c : contas) {
            sb.append(numeroLinha).append('.')
                    .append(csvSafe(c.codigo())).append(';')
                    .append(csvSafe(c.codigo())).append(';')
                    .append(csvSafe(prefix + c.codigo() + " - " + c.nome())).append(';')
                    .append(c.sintetica() ? "SINTETICA" : "ANALITICA").append(';')
                    .append(formatCsvDecimal(c.valor())).append(';')
                    .append(formatCsvDecimal(c.analiseVerticalPercentual())).append(';')
                    .append(formatCsvDecimal(c.valorComparacao())).append(';')
                    .append(formatCsvDecimal(c.variacaoAbsoluta())).append(';')
                    .append(formatCsvDecimal(c.analiseHorizontalPercentual())).append(';')
                    .append(c.classificacaoCusto() == null ? "" : c.classificacaoCusto().name()).append(';')
                    .append(c.quantidadeLancamentos()).append('\n');
            appendContasCsv(sb, numeroLinha, c.filhas(), depth + 1);
        }
    }

    private void registrarAuditoriaExportacao(
            DreGerencialResponseDTO dre,
            String formato,
            Long empresaId,
            Long currentFilialId,
            HttpServletRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("formato", formato);
        metadata.put("periodo", dre.rotuloPeriodo());
        metadata.put("dataInicio", dre.dataInicio() == null ? null : dre.dataInicio().toString());
        metadata.put("dataFim", dre.dataFim() == null ? null : dre.dataFim().toString());
        metadata.put("consolidadoEmpresa", dre.consolidadoEmpresa());
        metadata.put("receitaLiquida", dre.indicadores().receitaLiquida());
        metadata.put("resultadoLiquidoGerencial", dre.indicadores().resultadoLiquidoGerencial());

        String resumo = "formato=" + formato
                + ";periodo=" + dre.rotuloPeriodo()
                + ";resultado=" + dre.indicadores().resultadoLiquidoGerencial();

        financialAuditTrailService.record(
                "DRE_EXPORTADA",
                "DreGerencial",
                null,
                privacyGuardService.currentUserLogin(request),
                null,
                resumo,
                metadata,
                empresaId,
                currentFilialId);
        auditTrailService.registrarPrivacidade(
                "DRE_EXPORTADA",
                "DreGerencial",
                null,
                "Financeiro",
                null,
                resumo,
                request,
                "Exportacao da DRE Gerencial em formato " + formato + ".");
    }

    // =========================================================================
    // Nucleo de Apuracao por Regime de Competencia
    // =========================================================================

    private ApuracaoBruta apurarIntervalo(
            Long empresaId,
            List<Long> filiaisIds,
            Long filialReferenciaPlano,
            Long centroCustoFiltroId,
            LocalDate inicio,
            LocalDate fim) {
        LocalDateTime inicioTime = inicio.atStartOfDay();
        LocalDateTime fimTime = fim.atTime(LocalTime.MAX);

        // Carrega plano de contas da filial de referencia (e demais filiais consultadas por ID/codigo)
        Map<Long, PlanoContasGerencial> contasPorId = new LinkedHashMap<>();
        Map<String, PlanoContasGerencial> contasPorCodigo = new LinkedHashMap<>();
        Set<Long> filiaisPlano = new LinkedHashSet<>(filiaisIds);
        filiaisPlano.add(filialReferenciaPlano);

        for (Long fId : filiaisPlano) {
            for (PlanoContasGerencial conta : planoContasRepository
                    .findByEmpresaIdAndFilialIdOrderByOrdemExibicaoAscCodigoAsc(empresaId, fId)) {
                contasPorId.put(conta.getIdPlanoConta(), conta);
                contasPorCodigo.putIfAbsent(conta.getCodigo(), conta);
            }
        }

        Map<Long, CentroCusto> centrosPorId = new LinkedHashMap<>();
        for (Long fId : filiaisPlano) {
            for (CentroCusto cc : centroCustoRepository.findByEmpresaIdAndFilialIdOrderByNomeAsc(empresaId, fId)) {
                centrosPorId.put(cc.getIdCentroCusto(), cc);
            }
        }

        PlanoContasGerencial contaReceitaPadrao = contasPorCodigo.get("1.01");
        PlanoContasGerencial contaDevolucao = contasPorCodigo.get("2.01");
        PlanoContasGerencial contaDesconto = contasPorCodigo.get("2.02");
        PlanoContasGerencial contaTributosVendas = contasPorCodigo.get("2.03");
        PlanoContasGerencial contaCmv = contasPorCodigo.get("3.01");

        Map<Long, AcumuladorConta> acumuladorPorConta = new LinkedHashMap<>();
        Map<GrupoDreGerencial, BigDecimal> valoresSemContaPorGrupo = new EnumMap<>(GrupoDreGerencial.class);
        Map<GrupoDreGerencial, Long> qtdSemContaPorGrupo = new EnumMap<>(GrupoDreGerencial.class);
        List<DreLancamentoOrigemDTO> lancamentos = new ArrayList<>();
        List<String> avisos = new ArrayList<>();

        long lancamentosSemClassificacao = 0L;
        BigDecimal valorSemClassificacao = BigDecimal.ZERO;
        long itensSemCusto = 0L;
        long lancamentosCompetenciaEstimada = 0L;
        long lancamentosCompetenciaPorVencimento = 0L;
        int nfsEntradaPeriodo = 0;

        BigDecimal entradasRealizadasCaixa = BigDecimal.ZERO;
        BigDecimal saidasRealizadasCaixa = BigDecimal.ZERO;

        for (Long filialId : filiaisIds) {
            List<Venda> vendasPeriodo = vendaRepository.findAll(
                    consolidacaoService.vendaPeriodoSpec(empresaId, filialId, inicio, fim, inicioTime, fimTime));
            Map<Long, Venda> vendasPorId = new HashMap<>();
            Set<Long> vendasRecebidasNoCaixa = new HashSet<>();

            for (Venda v : vendasPeriodo) {
                vendasPorId.put(v.getIdVenda(), v);
                if (!consolidacaoService.isVendaFinalizada(v)) {
                    continue;
                }
                LocalDate compDate = consolidacaoService.effectiveVendaCompetencia(v);
                if (!consolidacaoService.within(compDate, inicio, fim)) {
                    continue;
                }
                if (centroCustoFiltroId != null && !Objects.equals(v.getCentroCustoId(), centroCustoFiltroId)) {
                    continue;
                }

                boolean vendaCompetenciaEstimada = v.getDataCompetencia() == null;
                if (vendaCompetenciaEstimada) {
                    lancamentosCompetenciaEstimada++;
                }

                PlanoContasGerencial contaVenda = resolverContaValida(v.getPlanoContaId(), contasPorId, contasPorCodigo);
                CentroCusto ccVenda = v.getCentroCustoId() == null ? null : centrosPorId.get(v.getCentroCustoId());
                boolean pertenceReceitaBruta = contaVenda == null
                        || contaVenda.getGrupoDre() == GrupoDreGerencial.RECEITA_BRUTA;

                BigDecimal valorLiquidoDesconto = nvl(v.getValorTotal());
                BigDecimal descontoVenda = pertenceReceitaBruta
                        ? nvl(v.getDesconto()).max(BigDecimal.ZERO)
                        : BigDecimal.ZERO;
                BigDecimal receitaBrutaVenda = valorLiquidoDesconto.add(descontoVenda);

                String docRef = v.getNumeroVenda() != null && !v.getNumeroVenda().isBlank()
                        ? "Venda " + v.getNumeroVenda()
                        : "Venda #" + v.getIdVenda();
                String sufixoCompVenda = vendaCompetenciaEstimada ? " [Competencia estimada pela data da venda]" : "";
                String statusOpVenda = (v.getStatusOperacional() == null ? "FINALIZADA" : v.getStatusOperacional().name())
                        + (vendaCompetenciaEstimada ? "_COMPETENCIA_ESTIMADA" : "");
                LocalDate dataCaixaVenda = v.getFinalizadoEm() != null
                        ? v.getFinalizadoEm().toLocalDate()
                        : (v.getRecebidoCaixaEm() != null ? v.getRecebidoCaixaEm().toLocalDate() : null);

                if (contaVenda != null) {
                    acumularConta(acumuladorPorConta, contaVenda, receitaBrutaVenda, 1L);
                    DefinicaoLinhaDre linhaVenda = resolverLinhaPorGrupo(contaVenda.getGrupoDre(), 1);
                    lancamentos.add(criarLancamento(
                            "VENDA",
                            v.getIdVenda(),
                            docRef,
                            "Receita bruta de venda finalizada (" + docRef + ")" + sufixoCompVenda,
                            compDate,
                            v.getDataEmissao() != null ? v.getDataEmissao() : compDate,
                            dataCaixaVenda,
                            statusOpVenda,
                            receitaBrutaVenda,
                            "POSITIVO",
                            linhaVenda,
                            contaVenda,
                            ccVenda,
                            filialId,
                            "/sales-history"));
                } else {
                    lancamentosSemClassificacao++;
                    valorSemClassificacao = valorSemClassificacao.add(valorLiquidoDesconto);
                    somarSemConta(valoresSemContaPorGrupo, qtdSemContaPorGrupo,
                            GrupoDreGerencial.RECEITA_BRUTA, receitaBrutaVenda);
                    lancamentos.add(criarLancamento(
                            "VENDA",
                            v.getIdVenda(),
                            docRef,
                            "Venda finalizada pendente de classificacao (" + docRef + ")" + sufixoCompVenda,
                            compDate,
                            v.getDataEmissao() != null ? v.getDataEmissao() : compDate,
                            dataCaixaVenda,
                            statusOpVenda,
                            receitaBrutaVenda,
                            "POSITIVO",
                            ESTRUTURA_17_LINHAS.get(0),
                            contaReceitaPadrao,
                            ccVenda,
                            filialId,
                            "/finance/foundation"));
                }

                // Desconto comercial concedido na venda -> Linha 3 (DESCONTOS_CONCEDIDOS)
                if (descontoVenda.compareTo(BigDecimal.ZERO) > 0) {
                    if (contaDesconto != null) {
                        acumularConta(acumuladorPorConta, contaDesconto, descontoVenda, 1L);
                    } else {
                        somarSemConta(valoresSemContaPorGrupo, qtdSemContaPorGrupo,
                                GrupoDreGerencial.DESCONTOS_CONCEDIDOS, descontoVenda);
                    }
                    lancamentos.add(criarLancamento(
                            "DESCONTO_VENDA",
                            v.getIdVenda(),
                            docRef,
                            "Desconto comercial concedido na " + docRef,
                            compDate,
                            v.getDataEmissao() != null ? v.getDataEmissao() : compDate,
                            dataCaixaVenda,
                            "DEDUCAO_COMPETENCIA",
                            descontoVenda,
                            "NEGATIVO",
                            ESTRUTURA_17_LINHAS.get(2),
                            contaDesconto,
                            ccVenda,
                            filialId,
                            "/sales-history"));
                }

                // CMV dos itens vendidos -> Linha 6 (CMV)
                Hibernate.initialize(v.getItens());
                BigDecimal cmvVenda = BigDecimal.ZERO;
                if (v.getItens() != null) {
                    for (ProdutoVenda item : v.getItens()) {
                        BigDecimal custoUnit = item.getCustoUnitario();
                        if (custoUnit == null || custoUnit.compareTo(BigDecimal.ZERO) <= 0) {
                            itensSemCusto++;
                        } else {
                            cmvVenda = cmvVenda.add(custoUnit.multiply(BigDecimal.valueOf(item.getQuantidade())));
                        }
                    }
                }
                if (cmvVenda.compareTo(BigDecimal.ZERO) > 0) {
                    if (contaCmv != null) {
                        acumularConta(acumuladorPorConta, contaCmv, cmvVenda, 1L);
                    } else {
                        somarSemConta(valoresSemContaPorGrupo, qtdSemContaPorGrupo, GrupoDreGerencial.CMV, cmvVenda);
                    }
                    lancamentos.add(criarLancamento(
                            "CMV_VENDA",
                            v.getIdVenda(),
                            docRef,
                            "Custo das mercadorias vendidas na " + docRef,
                            compDate,
                            v.getDataEmissao() != null ? v.getDataEmissao() : compDate,
                            dataCaixaVenda,
                            "APURADO_ESTOQUE",
                            cmvVenda,
                            "NEGATIVO",
                            ESTRUTURA_17_LINHAS.get(5),
                            contaCmv,
                            ccVenda,
                            filialId,
                            "/sales-history"));
                }
            }

            // 2. Devolucoes no periodo -> Linha 2 (DEVOLUCOES_CANCELAMENTOS) e abatimento de CMV na Linha 6
            List<VendaDevolucao> devolucoes = vendaDevolucaoRepository
                    .findByEmpresaIdAndFilialIdAndCriadoEmBetweenOrderByCriadoEmDesc(
                            empresaId, filialId, inicioTime, fimTime);
            for (VendaDevolucao dev : devolucoes) {
                if (dev.getTipo() == TipoDevolucao.TROCA) {
                    continue;
                }
                Venda vendaOrigem = dev.getVendaOriginalId() == null
                        ? null
                        : vendasPorId.computeIfAbsent(dev.getVendaOriginalId(), id -> vendaRepository
                                .findByIdVendaAndEmpresaIdAndFilialId(id, empresaId, filialId).orElse(null));
                if (centroCustoFiltroId != null
                        && (vendaOrigem == null || !Objects.equals(vendaOrigem.getCentroCustoId(), centroCustoFiltroId))) {
                    continue;
                }
                CentroCusto ccDev = (vendaOrigem != null && vendaOrigem.getCentroCustoId() != null)
                        ? centrosPorId.get(vendaOrigem.getCentroCustoId())
                        : null;
                LocalDate dataDev = dev.getCriadoEm() != null ? dev.getCriadoEm().toLocalDate() : inicio;
                BigDecimal ajuste = nvl(dev.getValorAjuste()).max(BigDecimal.ZERO);
                String refDev = "Devolucao #" + dev.getIdVendaDevolucao()
                        + (dev.getVendaOriginalId() != null ? " (Venda #" + dev.getVendaOriginalId() + ")" : "");

                if (ajuste.compareTo(BigDecimal.ZERO) > 0) {
                    if (contaDevolucao != null) {
                        acumularConta(acumuladorPorConta, contaDevolucao, ajuste, 1L);
                    } else {
                        somarSemConta(valoresSemContaPorGrupo, qtdSemContaPorGrupo,
                                GrupoDreGerencial.DEVOLUCOES_CANCELAMENTOS, ajuste);
                    }
                    lancamentos.add(criarLancamento(
                            "DEVOLUCAO",
                            dev.getIdVendaDevolucao(),
                            refDev,
                            "Devolucao de venda: " + (dev.getMotivo() == null ? refDev : dev.getMotivo()),
                            dataDev,
                            dataDev,
                            dataDev,
                            dev.getTipo().name(),
                            ajuste,
                            "NEGATIVO",
                            ESTRUTURA_17_LINHAS.get(1),
                            contaDevolucao,
                            ccDev,
                            filialId,
                            "/sales-history"));
                }

                Hibernate.initialize(dev.getItens());
                BigDecimal cmvDevolvido = BigDecimal.ZERO;
                if (dev.getItens() != null) {
                    for (VendaDevolucaoItem item : dev.getItens()) {
                        BigDecimal custoUnit = consolidacaoService.resolverCustoDevolucao(item, vendaOrigem);
                        if (custoUnit != null && custoUnit.compareTo(BigDecimal.ZERO) > 0 && item.getQuantidade() != null) {
                            cmvDevolvido = cmvDevolvido.add(custoUnit.multiply(BigDecimal.valueOf(item.getQuantidade())));
                        }
                    }
                }
                if (cmvDevolvido.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal estornoCmv = cmvDevolvido.negate();
                    if (contaCmv != null) {
                        acumularConta(acumuladorPorConta, contaCmv, estornoCmv, 1L);
                    } else {
                        somarSemConta(valoresSemContaPorGrupo, qtdSemContaPorGrupo, GrupoDreGerencial.CMV, estornoCmv);
                    }
                    lancamentos.add(criarLancamento(
                            "CMV_DEVOLUCAO",
                            dev.getIdVendaDevolucao(),
                            refDev,
                            "Reversao de CMV por devolucao de mercadoria (" + refDev + ")",
                            dataDev,
                            dataDev,
                            dataDev,
                            "REVERSAO_CMV",
                            estornoCmv,
                            "POSITIVO",
                            ESTRUTURA_17_LINHAS.get(5),
                            contaCmv,
                            ccDev,
                            filialId,
                            "/sales-history"));
                }
            }

            // 3. Despesas (Regime de Competencia + controle de caixa realizado)
            for (Despesa d : despesaRepository.findAll(
                    consolidacaoService.despesaPeriodoSpec(empresaId, filialId, inicio, fim))) {
                if (d.getStatus() == 0) {
                    continue;
                }
                if (centroCustoFiltroId != null && !Objects.equals(d.getCentroCustoId(), centroCustoFiltroId)) {
                    continue;
                }
                LocalDate compDate = consolidacaoService.effectiveDespesaCompetencia(d);
                boolean inCompetencia = consolidacaoService.within(compDate, inicio, fim);
                boolean inCaixa = consolidacaoService.within(d.getDataPagamento(), inicio, fim);
                if (!inCompetencia && !inCaixa) {
                    continue;
                }
                BigDecimal valor = nvl(d.getValor());
                PlanoContasGerencial conta = resolverContaValida(d.getPlanoContaId(), contasPorId, contasPorCodigo);
                if (inCaixa) {
                    if (conta != null && conta.getNatureza() == NaturezaContaGerencial.CREDITO) {
                        entradasRealizadasCaixa = entradasRealizadasCaixa.add(valor);
                    } else {
                        saidasRealizadasCaixa = saidasRealizadasCaixa.add(valor);
                    }
                }
                if (!inCompetencia) {
                    continue;
                }

                boolean compEstimada = d.getDataCompetencia() == null;
                boolean compPorVencimento = compEstimada && d.getDataEmissao() == null;
                if (compEstimada) {
                    lancamentosCompetenciaEstimada++;
                    if (compPorVencimento) {
                        lancamentosCompetenciaPorVencimento++;
                    }
                }
                String sufixoComp = compPorVencimento
                        ? " [Competencia estimada por vencimento]"
                        : (compEstimada ? " [Competencia estimada por emissao]" : "");

                CentroCusto cc = d.getCentroCustoId() == null ? null : centrosPorId.get(d.getCentroCustoId());
                String docRef = "Despesa #" + d.getIdDespesa();
                String statusOp = (d.getDataPagamento() != null ? "PAGA" : "EM_ABERTO")
                        + (compPorVencimento ? "_COMPETENCIA_ESTIMADA_VENCIMENTO" : (compEstimada ? "_COMPETENCIA_ESTIMADA_EMISSAO" : ""));

                if (conta != null) {
                    if (conta.getTipoConta() == TipoContaGerencial.INVESTIMENTO) {
                        continue;
                    }
                    acumularConta(acumuladorPorConta, conta, valor, 1L);
                    DefinicaoLinhaDre linha = resolverLinhaPorGrupo(conta.getGrupoDre(), 10);
                    boolean isLinhaSinalizadaDebito = "+/-".equals(linha.sinal())
                            && conta.getNatureza() == NaturezaContaGerencial.DEBITO;
                    BigDecimal valorLancamento = isLinhaSinalizadaDebito ? valor.negate() : valor;
                    lancamentos.add(criarLancamento(
                            "DESPESA",
                            d.getIdDespesa(),
                            docRef,
                            d.getDescricao() + sufixoComp,
                            compDate,
                            d.getDataEmissao() != null ? d.getDataEmissao() : compDate,
                            d.getDataPagamento(),
                            statusOp,
                            valorLancamento,
                            conta.getNatureza() == NaturezaContaGerencial.CREDITO ? "POSITIVO" : "NEGATIVO",
                            linha,
                            conta,
                            cc,
                            filialId,
                            "/expenses"));
                } else {
                    lancamentosSemClassificacao++;
                    valorSemClassificacao = valorSemClassificacao.add(valor);
                    somarSemConta(valoresSemContaPorGrupo, qtdSemContaPorGrupo,
                            GrupoDreGerencial.DESPESA_OPERACIONAL, valor);
                    lancamentos.add(criarLancamento(
                            "DESPESA",
                            d.getIdDespesa(),
                            docRef,
                            d.getDescricao() + " (Pendente de classificacao)" + sufixoComp,
                            compDate,
                            d.getDataEmissao() != null ? d.getDataEmissao() : compDate,
                            d.getDataPagamento(),
                            statusOp,
                            valor,
                            "NEGATIVO",
                            ESTRUTURA_17_LINHAS.get(9),
                            null,
                            cc,
                            filialId,
                            "/finance/foundation"));
                }
            }

            // 4. Contas a Pagar (Regime de Competencia + controle de caixa realizado)
            for (ContaPagar cp : contaPagarRepository.findAll(
                    consolidacaoService.contaPagarPeriodoSpec(empresaId, filialId, inicio, fim, inicioTime, fimTime))) {
                if (cp.getStatus() == FinanceiroStatus.CANCELADO || cp.getStatus() == FinanceiroStatus.DEVOLVIDO) {
                    continue;
                }
                if (centroCustoFiltroId != null && !Objects.equals(cp.getCentroCustoId(), centroCustoFiltroId)) {
                    continue;
                }
                LocalDate compDate = consolidacaoService.effectiveContaPagarCompetencia(cp);
                boolean inCompetencia = consolidacaoService.within(compDate, inicio, fim);
                boolean inCaixa = cp.getStatus() == FinanceiroStatus.PAGO
                        && cp.getPagoEm() != null
                        && consolidacaoService.within(cp.getPagoEm().toLocalDate(), inicio, fim);
                if (!inCompetencia && !inCaixa) {
                    continue;
                }
                BigDecimal valor = nvl(cp.getValor());
                PlanoContasGerencial conta = resolverContaValida(cp.getPlanoContaId(), contasPorId, contasPorCodigo);
                if (inCaixa) {
                    if (conta != null && conta.getNatureza() == NaturezaContaGerencial.CREDITO) {
                        entradasRealizadasCaixa = entradasRealizadasCaixa.add(valor);
                    } else {
                        saidasRealizadasCaixa = saidasRealizadasCaixa.add(valor);
                    }
                }
                if (!inCompetencia) {
                    continue;
                }

                boolean compEstimada = cp.getDataCompetencia() == null;
                boolean compPorVencimento = compEstimada && cp.getDataEmissao() == null && cp.getCriadoEm() == null;
                if (compEstimada) {
                    lancamentosCompetenciaEstimada++;
                    if (compPorVencimento) {
                        lancamentosCompetenciaPorVencimento++;
                    }
                }
                String sufixoComp = compPorVencimento
                        ? " [Competencia estimada por vencimento]"
                        : (compEstimada ? " [Competencia estimada por emissao]" : "");
                String statusOpCp = cp.getStatus().name()
                        + (compPorVencimento ? "_COMPETENCIA_ESTIMADA_VENCIMENTO" : (compEstimada ? "_COMPETENCIA_ESTIMADA_EMISSAO" : ""));

                CentroCusto cc = cp.getCentroCustoId() == null ? null : centrosPorId.get(cp.getCentroCustoId());
                String docRef = "Conta a Pagar #" + cp.getIdContaPagar();
                LocalDate dataPago = cp.getPagoEm() == null ? null : cp.getPagoEm().toLocalDate();

                if (conta != null) {
                    if (conta.getTipoConta() == TipoContaGerencial.INVESTIMENTO) {
                        continue;
                    }
                    acumularConta(acumuladorPorConta, conta, valor, 1L);
                    DefinicaoLinhaDre linha = resolverLinhaPorGrupo(conta.getGrupoDre(), 10);
                    boolean isLinhaSinalizadaDebito = "+/-".equals(linha.sinal())
                            && conta.getNatureza() == NaturezaContaGerencial.DEBITO;
                    BigDecimal valorLancamento = isLinhaSinalizadaDebito ? valor.negate() : valor;
                    lancamentos.add(criarLancamento(
                            "CONTA_PAGAR",
                            cp.getIdContaPagar(),
                            docRef,
                            cp.getDescricao() + sufixoComp,
                            compDate,
                            cp.getDataEmissao() != null ? cp.getDataEmissao() : compDate,
                            dataPago,
                            statusOpCp,
                            valorLancamento,
                            conta.getNatureza() == NaturezaContaGerencial.CREDITO ? "POSITIVO" : "NEGATIVO",
                            linha,
                            conta,
                            cc,
                            filialId,
                            "/finance-operations"));
                } else {
                    lancamentosSemClassificacao++;
                    valorSemClassificacao = valorSemClassificacao.add(valor);
                    somarSemConta(valoresSemContaPorGrupo, qtdSemContaPorGrupo,
                            GrupoDreGerencial.DESPESA_OPERACIONAL, valor);
                    lancamentos.add(criarLancamento(
                            "CONTA_PAGAR",
                            cp.getIdContaPagar(),
                            docRef,
                            cp.getDescricao() + " (Pendente de classificacao)" + sufixoComp,
                            compDate,
                            cp.getDataEmissao() != null ? cp.getDataEmissao() : compDate,
                            dataPago,
                            statusOpCp,
                            valor,
                            "NEGATIVO",
                            ESTRUTURA_17_LINHAS.get(9),
                            null,
                            cc,
                            filialId,
                            "/finance/foundation"));
                }
            }

            // 4b. Contas a Receber avulsas sem vinculo com Venda (vendaId == null; parcelas de venda NUNCA entram aqui)
            for (ContaReceber crAvulsa : contaReceberRepository.findAll(
                    consolidacaoService.contaReceberAvulsaPeriodoSpec(empresaId, filialId, inicio, fim, inicioTime, fimTime))) {
                if (crAvulsa.getVendaId() != null
                        || crAvulsa.getStatus() == FinanceiroStatus.CANCELADO
                        || crAvulsa.getStatus() == FinanceiroStatus.DEVOLVIDO) {
                    continue;
                }
                if (centroCustoFiltroId != null && !Objects.equals(crAvulsa.getCentroCustoId(), centroCustoFiltroId)) {
                    continue;
                }
                LocalDate compDate = consolidacaoService.effectiveContaReceberCompetencia(crAvulsa);
                if (!consolidacaoService.within(compDate, inicio, fim)) {
                    continue;
                }
                BigDecimal valor = nvl(crAvulsa.getValorBruto())
                        .subtract(nvl(crAvulsa.getDesconto()))
                        .max(nvl(crAvulsa.getValorLiquido()));
                if (valor.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                PlanoContasGerencial conta = resolverContaValida(crAvulsa.getPlanoContaId(), contasPorId, contasPorCodigo);
                CentroCusto cc = crAvulsa.getCentroCustoId() == null ? null : centrosPorId.get(crAvulsa.getCentroCustoId());
                String docRef = "Conta a Receber #" + crAvulsa.getIdContaReceber();
                String descCr = "Recebivel avulso (" + docRef + ")";
                LocalDate dataPago = crAvulsa.getPagoEm() == null ? null : crAvulsa.getPagoEm().toLocalDate();

                if (conta != null) {
                    acumularConta(acumuladorPorConta, conta, valor, 1L);
                    DefinicaoLinhaDre linha = resolverLinhaPorGrupo(conta.getGrupoDre(), 1);
                    lancamentos.add(criarLancamento(
                            "CONTA_RECEBER",
                            crAvulsa.getIdContaReceber(),
                            docRef,
                            descCr,
                            compDate,
                            crAvulsa.getDataEmissao() != null ? crAvulsa.getDataEmissao() : compDate,
                            dataPago,
                            crAvulsa.getStatus().name(),
                            valor,
                            "POSITIVO",
                            linha,
                            conta,
                            cc,
                            filialId,
                            "/finance-operations"));
                } else {
                    lancamentosSemClassificacao++;
                    valorSemClassificacao = valorSemClassificacao.add(valor);
                    somarSemConta(valoresSemContaPorGrupo, qtdSemContaPorGrupo,
                            GrupoDreGerencial.RECEITA_BRUTA, valor);
                    lancamentos.add(criarLancamento(
                            "CONTA_RECEBER",
                            crAvulsa.getIdContaReceber(),
                            docRef,
                            descCr + " (Pendente de classificacao)",
                            compDate,
                            crAvulsa.getDataEmissao() != null ? crAvulsa.getDataEmissao() : compDate,
                            dataPago,
                            crAvulsa.getStatus().name(),
                            valor,
                            "POSITIVO",
                            ESTRUTURA_17_LINHAS.get(0),
                            contaReceitaPadrao,
                            cc,
                            filialId,
                            "/finance/foundation"));
                }
            }

            // 5. Caixa Realizado de Entradas (ContaReceber pagas + fallback PDV; NUNCA duplica receita na DRE!)
            List<ContaReceber> contasReceberCaixa = contaReceberRepository.findAll(
                    consolidacaoService.contaReceberCaixaSpec(empresaId, filialId, inicioTime, fimTime));
            for (ContaReceber cr : contasReceberCaixa) {
                if (cr.getStatus() == FinanceiroStatus.CANCELADO) {
                    continue;
                }
                if (centroCustoFiltroId != null && !Objects.equals(cr.getCentroCustoId(), centroCustoFiltroId)) {
                    continue;
                }
                if (cr.getPagoEm() != null
                        && consolidacaoService.within(cr.getPagoEm().toLocalDate(), inicio, fim)
                        && nvl(cr.getValorPago()).compareTo(BigDecimal.ZERO) > 0) {
                    entradasRealizadasCaixa = entradasRealizadasCaixa.add(nvl(cr.getValorPago()));
                    if (cr.getVendaId() != null) {
                        vendasRecebidasNoCaixa.add(cr.getVendaId());
                    }
                }
            }

            List<CaixaMovimentacao> movsCaixa = caixaMovimentacaoRepository
                    .findByCaixaSessaoEmpresaIdAndCaixaSessaoFilialIdAndCriadoEmBetweenOrderByCriadoEmDesc(
                            empresaId, filialId, inicioTime, fimTime);
            Set<Long> candidatasFallback = new HashSet<>();
            for (CaixaMovimentacao mov : movsCaixa) {
                if ("VENDA".equalsIgnoreCase(mov.getTipo()) && mov.getReferenciaId() != null
                        && !vendasRecebidasNoCaixa.contains(mov.getReferenciaId())) {
                    candidatasFallback.add(mov.getReferenciaId());
                }
            }
            for (Venda v : vendasPeriodo) {
                if (consolidacaoService.isVendaFinalizada(v) && !vendasRecebidasNoCaixa.contains(v.getIdVenda())) {
                    candidatasFallback.add(v.getIdVenda());
                }
            }
            Set<Long> vendasComCr = new HashSet<>();
            for (Long vId : candidatasFallback) {
                contaReceberRepository.findFirstByVendaIdAndEmpresaIdAndFilialIdOrderByIdContaReceberAsc(vId, empresaId, filialId)
                        .ifPresent(cr -> vendasComCr.add(vId));
            }
            for (CaixaMovimentacao mov : movsCaixa) {
                if (!"VENDA".equalsIgnoreCase(mov.getTipo()) || mov.getCriadoEm() == null) {
                    continue;
                }
                if (!consolidacaoService.within(mov.getCriadoEm().toLocalDate(), inicio, fim)) {
                    continue;
                }
                Long vId = mov.getReferenciaId();
                if (vId != null && (vendasRecebidasNoCaixa.contains(vId) || vendasComCr.contains(vId))) {
                    continue;
                }
                entradasRealizadasCaixa = entradasRealizadasCaixa.add(nvl(mov.getValor()));
                if (vId != null) {
                    vendasRecebidasNoCaixa.add(vId);
                }
            }
            for (Venda v : vendasPeriodo) {
                if (!consolidacaoService.isVendaFinalizada(v)
                        || vendasRecebidasNoCaixa.contains(v.getIdVenda())
                        || vendasComCr.contains(v.getIdVenda())) {
                    continue;
                }
                if (centroCustoFiltroId != null && !Objects.equals(v.getCentroCustoId(), centroCustoFiltroId)) {
                    continue;
                }
                LocalDateTime pagoDt = v.getFinalizadoEm() != null ? v.getFinalizadoEm() : v.getRecebidoCaixaEm();
                if (pagoDt != null && consolidacaoService.within(pagoDt.toLocalDate(), inicio, fim)) {
                    entradasRealizadasCaixa = entradasRealizadasCaixa.add(nvl(v.getValorTotal()));
                    vendasRecebidasNoCaixa.add(v.getIdVenda());
                }
            }

            List<NotaFiscalEntrada> nfs = notaFiscalEntradaRepository
                    .findByEmpresaIdAndFilialIdAndDataCompetenciaBetweenOrderByDataCompetenciaDesc(
                            empresaId, filialId, inicio, fim);
            nfsEntradaPeriodo += nfs.size();
        }

        avisos.add("Demonstracao gerencial pelo regime de competencia para apoio a decisao; nao substitui escrituracao contabil ou fiscal oficial.");
        if (lancamentosSemClassificacao > 0) {
            avisos.add("Existem " + lancamentosSemClassificacao + " lancamento(s) (R$ "
                    + scale2(valorSemClassificacao)
                    + ") pendentes de classificacao no plano de contas; seus valores foram preservados na DRE para nao distorcer o resultado.");
        }
        if (lancamentosCompetenciaEstimada > 0) {
            avisos.add("Existem " + lancamentosCompetenciaEstimada
                    + " lancamento(s) sem data de competencia explicita ("
                    + lancamentosCompetenciaPorVencimento
                    + " estimado(s) pela data de vencimento); recomenda-se informar a data de competencia real do fato gerador.");
        }
        if (itensSemCusto > 0) {
            avisos.add("Existem " + itensSemCusto
                    + " item(ns) vendidos no periodo sem custo unitario cadastrado; revise o custo dos produtos para precisao total do CMV.");
        }
        if (nfsEntradaPeriodo > 0) {
            avisos.add("Foram registradas " + nfsEntradaPeriodo
                    + " nota(s) fiscal(is) de entrada no periodo; compras de estoque nao entram como despesa operacional para nao duplicar com o CMV.");
        }
        BigDecimal tribVendasVal = somaGrupo(acumuladorPorConta, valoresSemContaPorGrupo, GrupoDreGerencial.TRIBUTOS_SOBRE_VENDAS);
        BigDecimal recBrutaVal = somaGrupo(acumuladorPorConta, valoresSemContaPorGrupo, GrupoDreGerencial.RECEITA_BRUTA);
        if (recBrutaVal.compareTo(BigDecimal.ZERO) > 0 && tribVendasVal.compareTo(BigDecimal.ZERO) == 0 && contaTributosVendas != null) {
            avisos.add("Nao ha lancamentos classificados em Tributos sobre Vendas (2.03) no periodo; caso haja impostos sobre faturamento, classifique a guia/provisao na conta 2.03.");
        }

        return new ApuracaoBruta(
                contasPorId,
                contasPorCodigo,
                acumuladorPorConta,
                valoresSemContaPorGrupo,
                qtdSemContaPorGrupo,
                lancamentos,
                lancamentosSemClassificacao,
                scale2(valorSemClassificacao),
                scale2(entradasRealizadasCaixa),
                scale2(saidasRealizadasCaixa),
                avisos);
    }

    private PlanoContasGerencial resolverContaValida(
            Long planoContaId,
            Map<Long, PlanoContasGerencial> contasPorId,
            Map<String, PlanoContasGerencial> contasPorCodigo) {
        if (planoContaId == null || planoContaId <= 0) {
            return null;
        }
        PlanoContasGerencial direta = contasPorId.get(planoContaId);
        if (direta != null) {
            return direta;
        }
        return null;
    }

    private void acumularConta(
            Map<Long, AcumuladorConta> mapa,
            PlanoContasGerencial conta,
            BigDecimal valor,
            long qtd) {
        if (conta == null || conta.getIdPlanoConta() == null) {
            return;
        }
        AcumuladorConta acc = mapa.computeIfAbsent(
                conta.getIdPlanoConta(),
                ignored -> new AcumuladorConta(conta));
        acc.valor = acc.valor.add(nvl(valor));
        acc.quantidade += qtd;
        if (acc.quantidade == 0 && acc.valor.signum() != 0) {
            acc.quantidade = 1L;
        }
    }

    private void somarSemConta(
            Map<GrupoDreGerencial, BigDecimal> valores,
            Map<GrupoDreGerencial, Long> quantidades,
            GrupoDreGerencial grupo,
            BigDecimal valor) {
        valores.merge(grupo, nvl(valor), BigDecimal::add);
        quantidades.merge(grupo, 1L, Long::sum);
    }

    private BigDecimal somaGrupo(
            Map<Long, AcumuladorConta> acumuladorPorConta,
            Map<GrupoDreGerencial, BigDecimal> valoresSemConta,
            GrupoDreGerencial grupo) {
        BigDecimal total = nvl(valoresSemConta.get(grupo));
        for (AcumuladorConta acc : acumuladorPorConta.values()) {
            if (acc.conta.getGrupoDre() == grupo) {
                total = total.add(acc.valor);
            }
        }
        return total;
    }

    private long qtdGrupo(
            Map<Long, AcumuladorConta> acumuladorPorConta,
            Map<GrupoDreGerencial, Long> qtdSemConta,
            GrupoDreGerencial grupo) {
        long total = qtdSemConta.getOrDefault(grupo, 0L);
        for (AcumuladorConta acc : acumuladorPorConta.values()) {
            if (acc.conta.getGrupoDre() == grupo) {
                total += acc.quantidade;
            }
        }
        return total;
    }

    private Totais17Linhas calcularTotais17Linhas(ApuracaoBruta ap) {
        BigDecimal l1ReceitaBruta = somaGrupo(ap.acumuladorPorConta(), ap.valoresSemContaPorGrupo(), GrupoDreGerencial.RECEITA_BRUTA);
        BigDecimal l2Devolucoes = somaGrupo(ap.acumuladorPorConta(), ap.valoresSemContaPorGrupo(), GrupoDreGerencial.DEVOLUCOES_CANCELAMENTOS);
        BigDecimal l3Descontos = somaGrupo(ap.acumuladorPorConta(), ap.valoresSemContaPorGrupo(), GrupoDreGerencial.DESCONTOS_CONCEDIDOS);
        BigDecimal l4TributosVendas = somaGrupo(ap.acumuladorPorConta(), ap.valoresSemContaPorGrupo(), GrupoDreGerencial.TRIBUTOS_SOBRE_VENDAS);
        BigDecimal l5ReceitaLiquida = l1ReceitaBruta.subtract(l2Devolucoes).subtract(l3Descontos).subtract(l4TributosVendas);

        BigDecimal l6Cmv = somaGrupo(ap.acumuladorPorConta(), ap.valoresSemContaPorGrupo(), GrupoDreGerencial.CMV);
        BigDecimal l7LucroBruto = l5ReceitaLiquida.subtract(l6Cmv);

        BigDecimal l8DespComerciais = somaGrupo(ap.acumuladorPorConta(), ap.valoresSemContaPorGrupo(), GrupoDreGerencial.DESPESA_COMERCIAL);
        BigDecimal l9DespAdministrativas = somaGrupo(ap.acumuladorPorConta(), ap.valoresSemContaPorGrupo(), GrupoDreGerencial.DESPESA_ADMINISTRATIVA);
        BigDecimal l10DespOperacionais = somaGrupo(ap.acumuladorPorConta(), ap.valoresSemContaPorGrupo(), GrupoDreGerencial.DESPESA_OPERACIONAL)
                .add(somaGrupo(ap.acumuladorPorConta(), ap.valoresSemContaPorGrupo(), GrupoDreGerencial.DESPESA_INFRAESTRUTURA));
        BigDecimal l11DespPessoal = somaGrupo(ap.acumuladorPorConta(), ap.valoresSemContaPorGrupo(), GrupoDreGerencial.DESPESA_PESSOAL);

        BigDecimal outrasReceitas = somaGrupo(ap.acumuladorPorConta(), ap.valoresSemContaPorGrupo(), GrupoDreGerencial.OUTRAS_RECEITAS);
        BigDecimal outrasDespesas = somaGrupo(ap.acumuladorPorConta(), ap.valoresSemContaPorGrupo(), GrupoDreGerencial.OUTRAS_DESPESAS);
        BigDecimal l12OutrasReceitasDespesasOp = outrasReceitas.subtract(outrasDespesas);

        BigDecimal despesasOperacionaisGrupos = l8DespComerciais
                .add(l9DespAdministrativas)
                .add(l10DespOperacionais)
                .add(l11DespPessoal);

        BigDecimal l13ResultadoOperacional = l7LucroBruto
                .subtract(despesasOperacionaisGrupos)
                .add(l12OutrasReceitasDespesasOp);

        BigDecimal receitasFinanceiras = somaGrupo(ap.acumuladorPorConta(), ap.valoresSemContaPorGrupo(), GrupoDreGerencial.RECEITA_FINANCEIRA);
        BigDecimal despesasFinanceiras = somaGrupo(ap.acumuladorPorConta(), ap.valoresSemContaPorGrupo(), GrupoDreGerencial.DESPESA_FINANCEIRA);
        BigDecimal l14ResultadoFinanceiroLiquido = receitasFinanceiras.subtract(despesasFinanceiras);

        BigDecimal l15ResultadoAntesTributos = l13ResultadoOperacional.add(l14ResultadoFinanceiroLiquido);
        BigDecimal l16TributosSobreLucro = somaGrupo(ap.acumuladorPorConta(), ap.valoresSemContaPorGrupo(), GrupoDreGerencial.TRIBUTOS_SOBRE_LUCRO);
        BigDecimal l17ResultadoLiquido = l15ResultadoAntesTributos.subtract(l16TributosSobreLucro);

        Map<Integer, BigDecimal> porLinha = new LinkedHashMap<>();
        porLinha.put(1, l1ReceitaBruta);
        porLinha.put(2, l2Devolucoes);
        porLinha.put(3, l3Descontos);
        porLinha.put(4, l4TributosVendas);
        porLinha.put(5, l5ReceitaLiquida);
        porLinha.put(6, l6Cmv);
        porLinha.put(7, l7LucroBruto);
        porLinha.put(8, l8DespComerciais);
        porLinha.put(9, l9DespAdministrativas);
        porLinha.put(10, l10DespOperacionais);
        porLinha.put(11, l11DespPessoal);
        porLinha.put(12, l12OutrasReceitasDespesasOp);
        porLinha.put(13, l13ResultadoOperacional);
        porLinha.put(14, l14ResultadoFinanceiroLiquido);
        porLinha.put(15, l15ResultadoAntesTributos);
        porLinha.put(16, l16TributosSobreLucro);
        porLinha.put(17, l17ResultadoLiquido);

        return new Totais17Linhas(
                l1ReceitaBruta,
                l2Devolucoes.add(l3Descontos).add(l4TributosVendas),
                l5ReceitaLiquida,
                l6Cmv,
                l7LucroBruto,
                despesasOperacionaisGrupos.add(outrasDespesas),
                l13ResultadoOperacional,
                l14ResultadoFinanceiroLiquido,
                l15ResultadoAntesTributos,
                l16TributosSobreLucro,
                l17ResultadoLiquido,
                porLinha);
    }

    private DreGerencialResponseDTO montarDreResponse(
            JanelaPeriodo janela,
            boolean comparar,
            Long empresaId,
            EscopoFiliais escopo,
            CentroCusto centroFiltro,
            ApuracaoBruta principal,
            ApuracaoBruta comparacao) {
        Totais17Linhas totaisPrincipal = calcularTotais17Linhas(principal);
        Totais17Linhas totaisComparacao = comparacao == null ? null : calcularTotais17Linhas(comparacao);

        List<DreLinhaDTO> linhas = construir17Linhas(principal, totaisPrincipal, comparacao, totaisComparacao);
        DreIndicadoresResumoDTO indicadores = montarIndicadores(principal, totaisPrincipal);
        DreDiagnosticoGerencialDTO diagnostico = construirDiagnostico(
                janela, principal, totaisPrincipal, totaisComparacao, indicadores, linhas);

        return new DreGerencialResponseDTO(
                janela.tipoPeriodo(),
                janela.rotulo(),
                janela.inicio(),
                janela.fim(),
                comparar,
                comparar ? janela.rotuloComparacao() : null,
                comparar ? janela.comparacaoInicio() : null,
                comparar ? janela.comparacaoFim() : null,
                empresaId,
                escopo.consolidadoEmpresa() ? null : escopo.filialUnicaId(),
                escopo.rotuloFilial(),
                escopo.consolidadoEmpresa(),
                centroFiltro == null ? null : centroFiltro.getIdCentroCusto(),
                centroFiltro == null ? null : centroFiltro.getNome(),
                "RECEITA_LIQUIDA (Linha 5 = 100,00%)",
                principal.lancamentosSemClassificacao(),
                principal.valorSemClassificacao(),
                indicadores,
                diagnostico,
                linhas);
    }

    private List<DreLinhaDTO> construir17Linhas(
            ApuracaoBruta principal,
            Totais17Linhas totaisPrincipal,
            ApuracaoBruta comparacao,
            Totais17Linhas totaisComparacao) {
        BigDecimal baseVerticalPrincipal = totaisPrincipal.receitaLiquida();
        BigDecimal baseVerticalComparacao = totaisComparacao == null ? null : totaisComparacao.receitaLiquida();

        List<DreLinhaDTO> resultado = new ArrayList<>(17);
        for (DefinicaoLinhaDre def : ESTRUTURA_17_LINHAS) {
            BigDecimal valor = scale2(totaisPrincipal.porLinha().getOrDefault(def.numero(), BigDecimal.ZERO));
            BigDecimal av = calcPercent(valor, baseVerticalPrincipal);

            BigDecimal valorComp = totaisComparacao == null
                    ? null
                    : scale2(totaisComparacao.porLinha().getOrDefault(def.numero(), BigDecimal.ZERO));
            BigDecimal variacaoAbs = valorComp == null ? null : scale2(valor.subtract(valorComp));
            BigDecimal ah = valorComp == null ? null : calcHorizontalPercent(valor, valorComp);
            String statusVar = classificarStatusVariacao(def.tipoLinha(), valor, valorComp);

            List<DreContaDetalheDTO> contasLinha = def.grupos().isEmpty()
                    ? List.of()
                    : construirContasDaLinha(
                            def,
                            principal,
                            baseVerticalPrincipal,
                            comparacao,
                            baseVerticalComparacao);

            long qtdLancamentos = def.grupos().stream()
                    .mapToLong(g -> qtdGrupo(principal.acumuladorPorConta(), principal.qtdSemContaPorGrupo(), g))
                    .sum();

            resultado.add(new DreLinhaDTO(
                    def.numero(),
                    def.codigo(),
                    def.titulo(),
                    def.tipoLinha(),
                    def.sinal(),
                    def.grupos(),
                    valor,
                    av,
                    valorComp,
                    variacaoAbs,
                    ah,
                    statusVar,
                    qtdLancamentos,
                    def.explicacao(),
                    contasLinha));
        }
        return resultado;
    }

    private List<DreContaDetalheDTO> construirContasDaLinha(
            DefinicaoLinhaDre def,
            ApuracaoBruta principal,
            BigDecimal baseVerticalPrincipal,
            ApuracaoBruta comparacao,
            BigDecimal baseVerticalComparacao) {
        Set<GrupoDreGerencial> gruposAlvo = EnumSet.copyOf(def.grupos());
        boolean linhaSinalizada = "+/-".equals(def.sinal());
        Map<String, RollupContaCodigo> porCodigoPrincipal = agruparPorCodigoConta(principal, gruposAlvo, linhaSinalizada);
        Map<String, RollupContaCodigo> porCodigoComp = comparacao == null
                ? Map.of()
                : agruparPorCodigoConta(comparacao, gruposAlvo, linhaSinalizada);

        List<DreContaDetalheDTO> folhas = new ArrayList<>();
        for (RollupContaCodigo r : porCodigoPrincipal.values()) {
            PlanoContasGerencial c = r.conta;
            BigDecimal val = scale2(r.valor);
            RollupContaCodigo rc = porCodigoComp.get(c.getCodigo());
            BigDecimal valComp = comparacao == null ? null : scale2(rc == null ? BigDecimal.ZERO : rc.valor);
            long qtd = r.quantidade;

            if (val.compareTo(BigDecimal.ZERO) == 0
                    && (valComp == null || valComp.compareTo(BigDecimal.ZERO) == 0)
                    && qtd == 0
                    && !c.isSistemaPadrao()) {
                continue;
            }

            BigDecimal av = calcPercent(val, baseVerticalPrincipal);
            BigDecimal varAbs = valComp == null ? null : scale2(val.subtract(valComp));
            BigDecimal ah = valComp == null ? null : calcHorizontalPercent(val, valComp);

            folhas.add(new DreContaDetalheDTO(
                    c.getIdPlanoConta(),
                    c.getContaPaiId(),
                    c.getCodigo(),
                    c.getNome(),
                    c.getTipoConta(),
                    c.getNatureza(),
                    c.getClassificacaoCusto(),
                    c.getGrupoDre(),
                    !c.isAceitaLancamento(),
                    val,
                    av,
                    valComp,
                    varAbs,
                    ah,
                    qtd,
                    List.of()));
        }

        // Inclui linha explicita para valores pendentes de classificacao naquela linha da DRE
        for (GrupoDreGerencial g : def.grupos()) {
            BigDecimal semContaPrincipal = nvl(principal.valoresSemContaPorGrupo().get(g));
            BigDecimal semContaComp = comparacao == null
                    ? null
                    : nvl(comparacao.valoresSemContaPorGrupo().get(g));
            long qtdSemConta = principal.qtdSemContaPorGrupo().getOrDefault(g, 0L);

            if (semContaPrincipal.compareTo(BigDecimal.ZERO) != 0
                    || (semContaComp != null && semContaComp.compareTo(BigDecimal.ZERO) != 0)) {
                BigDecimal val = scale2(semContaPrincipal);
                BigDecimal valComp = semContaComp == null ? null : scale2(semContaComp);
                folhas.add(new DreContaDetalheDTO(
                        -1L,
                        null,
                        "PENDENTE",
                        "Lancamentos Pendentes de Classificacao (" + g.name() + ")",
                        def.numero() == 1 ? TipoContaGerencial.RECEITA : TipoContaGerencial.DESPESA,
                        def.numero() == 1 ? NaturezaContaGerencial.CREDITO : NaturezaContaGerencial.DEBITO,
                        ClassificacaoCusto.NAO_APLICAVEL,
                        g,
                        false,
                        val,
                        calcPercent(val, baseVerticalPrincipal),
                        valComp,
                        valComp == null ? null : scale2(val.subtract(valComp)),
                        valComp == null ? null : calcHorizontalPercent(val, valComp),
                        qtdSemConta,
                        List.of()));
            }
        }

        return montarHierarquiaContasLinha(folhas, principal.contasPorId(), baseVerticalPrincipal, comparacao != null);
    }

    private List<DreContaDetalheDTO> montarHierarquiaContasLinha(
            List<DreContaDetalheDTO> analiticas,
            Map<Long, PlanoContasGerencial> contasPorId,
            BigDecimal baseVerticalPrincipal,
            boolean temComparacao) {
        Map<Long, List<DreContaDetalheDTO>> filhasPorPai = new LinkedHashMap<>();
        List<DreContaDetalheDTO> raizes = new ArrayList<>();

        for (DreContaDetalheDTO item : analiticas) {
            if (item.contaPaiId() != null && contasPorId.containsKey(item.contaPaiId())) {
                filhasPorPai.computeIfAbsent(item.contaPaiId(), ignored -> new ArrayList<>()).add(item);
            } else {
                raizes.add(item);
            }
        }

        List<DreContaDetalheDTO> resultado = new ArrayList<>();
        for (Map.Entry<Long, List<DreContaDetalheDTO>> entry : filhasPorPai.entrySet()) {
            PlanoContasGerencial pai = contasPorId.get(entry.getKey());
            List<DreContaDetalheDTO> filhas = entry.getValue();
            BigDecimal somaVal = filhas.stream().map(DreContaDetalheDTO::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal somaComp = temComparacao
                    ? filhas.stream()
                            .map(f -> f.valorComparacao() == null ? BigDecimal.ZERO : f.valorComparacao())
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                    : null;
            long somaQtd = filhas.stream().mapToLong(DreContaDetalheDTO::quantidadeLancamentos).sum();

            resultado.add(new DreContaDetalheDTO(
                    pai.getIdPlanoConta(),
                    pai.getContaPaiId(),
                    pai.getCodigo(),
                    pai.getNome(),
                    pai.getTipoConta(),
                    pai.getNatureza(),
                    pai.getClassificacaoCusto(),
                    pai.getGrupoDre(),
                    true,
                    scale2(somaVal),
                    calcPercent(somaVal, baseVerticalPrincipal),
                    somaComp == null ? null : scale2(somaComp),
                    somaComp == null ? null : scale2(somaVal.subtract(somaComp)),
                    somaComp == null ? null : calcHorizontalPercent(somaVal, somaComp),
                    somaQtd,
                    filhas));
        }

        for (DreContaDetalheDTO r : raizes) {
            if (!filhasPorPai.containsKey(r.idPlanoConta())) {
                resultado.add(r);
            }
        }

        resultado.sort(Comparator.comparing(DreContaDetalheDTO::codigo, Comparator.nullsLast(String::compareTo)));
        return resultado;
    }

    private Map<String, RollupContaCodigo> agruparPorCodigoConta(
            ApuracaoBruta ap,
            Set<GrupoDreGerencial> gruposAlvo,
            boolean linhaSinalizada) {
        Map<String, RollupContaCodigo> mapa = new LinkedHashMap<>();
        for (PlanoContasGerencial conta : ap.contasPorCodigo().values()) {
            if (conta.isAceitaLancamento() && gruposAlvo.contains(conta.getGrupoDre())) {
                mapa.put(conta.getCodigo(), new RollupContaCodigo(conta));
            }
        }
        for (AcumuladorConta acc : ap.acumuladorPorConta().values()) {
            PlanoContasGerencial c = acc.conta;
            if (!gruposAlvo.contains(c.getGrupoDre())) {
                continue;
            }
            RollupContaCodigo slot = mapa.computeIfAbsent(c.getCodigo(), ignored -> new RollupContaCodigo(c));
            boolean isDebitoEmLinhaSinalizada = linhaSinalizada
                    && (c.getGrupoDre() == GrupoDreGerencial.OUTRAS_DESPESAS
                            || c.getGrupoDre() == GrupoDreGerencial.DESPESA_FINANCEIRA
                            || c.getNatureza() == NaturezaContaGerencial.DEBITO);
            BigDecimal valorAjustado = isDebitoEmLinhaSinalizada
                    ? acc.valor.negate()
                    : acc.valor;
            slot.valor = slot.valor.add(valorAjustado);
            slot.quantidade += acc.quantidade;
        }
        return mapa;
    }

    private DreIndicadoresResumoDTO montarIndicadores(ApuracaoBruta ap, Totais17Linhas t) {
        BigDecimal despesasFixas = BigDecimal.ZERO;
        BigDecimal despesasVariaveis = BigDecimal.ZERO;

        for (AcumuladorConta acc : ap.acumuladorPorConta().values()) {
            PlanoContasGerencial c = acc.conta;
            if (c.getTipoConta() == TipoContaGerencial.DESPESA
                    || (c.getTipoConta() == TipoContaGerencial.FINANCEIRO && c.getNatureza() == NaturezaContaGerencial.DEBITO)
                    || (c.getTipoConta() == TipoContaGerencial.OUTRAS && c.getNatureza() == NaturezaContaGerencial.DEBITO)) {
                if (c.getClassificacaoCusto() == ClassificacaoCusto.VARIAVEL) {
                    despesasVariaveis = despesasVariaveis.add(acc.valor);
                } else if (c.getClassificacaoCusto() == ClassificacaoCusto.FIXO) {
                    despesasFixas = despesasFixas.add(acc.valor);
                }
            }
        }
        // Despesas sem classificacao entram conservadoramente como fixas ate classificacao
        BigDecimal despSemConta = nvl(ap.valoresSemContaPorGrupo().get(GrupoDreGerencial.DESPESA_OPERACIONAL));
        despesasFixas = despesasFixas.add(despSemConta);

        BigDecimal custosVariaveisTotais = t.cmv().add(despesasVariaveis).max(BigDecimal.ZERO);
        BigDecimal margemContribuicaoValor = t.receitaLiquida().subtract(custosVariaveisTotais);
        BigDecimal margemContribuicaoPct = defaultedPercent(calcPercent(margemContribuicaoValor, t.receitaLiquida()));

        BigDecimal pontoEquilibrioReceita = BigDecimal.ZERO;
        if (t.receitaLiquida().compareTo(BigDecimal.ZERO) > 0
                && margemContribuicaoValor.compareTo(BigDecimal.ZERO) > 0
                && despesasFixas.compareTo(BigDecimal.ZERO) >= 0) {
            try {
                Map<String, BigDecimal> calc = admCalcEngine.calculate(new AdmCalcRequestDTO(
                        null, null,
                        t.receitaLiquida().max(BigDecimal.ZERO),
                        t.cmv().max(BigDecimal.ZERO),
                        despesasFixas.max(BigDecimal.ZERO),
                        t.tributosSobreLucro().max(BigDecimal.ZERO),
                        despesasFixas.max(BigDecimal.ZERO),
                        custosVariaveisTotais,
                        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                        "PERCENTUAL"));
                pontoEquilibrioReceita = nvl(calc.get("pontoEquilibrioReceita"));
            } catch (IllegalArgumentException ignored) {
                pontoEquilibrioReceita = BigDecimal.ZERO;
            }
        }

        BigDecimal saldoCaixa = ap.entradasRealizadasCaixa().subtract(ap.saidasRealizadasCaixa());
        BigDecimal diffCompCaixa = t.resultadoLiquidoGerencial().subtract(saldoCaixa);

        return new DreIndicadoresResumoDTO(
                scale2(t.receitaBruta()),
                scale2(t.deducoesTotais()),
                scale2(t.receitaLiquida()),
                scale2(t.cmv()),
                scale2(t.lucroBruto()),
                defaultedPercent(calcPercent(t.lucroBruto(), t.receitaLiquida())),
                scale2(t.despesasOperacionaisTotais()),
                scale2(despesasFixas),
                scale2(despesasVariaveis),
                scale2(custosVariaveisTotais),
                scale2(margemContribuicaoValor),
                margemContribuicaoPct,
                scale2(pontoEquilibrioReceita),
                scale2(t.resultadoOperacional()),
                defaultedPercent(calcPercent(t.resultadoOperacional(), t.receitaLiquida())),
                scale2(t.resultadoFinanceiroLiquido()),
                scale2(t.resultadoAntesTributos()),
                scale2(t.tributosSobreLucro()),
                scale2(t.resultadoLiquidoGerencial()),
                defaultedPercent(calcPercent(t.resultadoLiquidoGerencial(), t.receitaLiquida())),
                ap.entradasRealizadasCaixa(),
                ap.saidasRealizadasCaixa(),
                scale2(saldoCaixa),
                scale2(diffCompCaixa));
    }

    private DreDiagnosticoGerencialDTO construirDiagnostico(
            JanelaPeriodo janela,
            ApuracaoBruta principal,
            Totais17Linhas t,
            Totais17Linhas tComp,
            DreIndicadoresResumoDTO ind,
            List<DreLinhaDTO> linhas) {
        String situacao;
        String manchete;
        if (t.receitaBruta().compareTo(BigDecimal.ZERO) == 0 && t.despesasOperacionaisTotais().compareTo(BigDecimal.ZERO) == 0) {
            situacao = "EQUILIBRIO";
            manchete = "Sem movimentacoes de receita ou despesa registradas na competencia " + janela.rotulo() + ".";
        } else if (t.resultadoLiquidoGerencial().compareTo(BigDecimal.ZERO) > 0) {
            situacao = "LUCRO";
            manchete = "A operacao gerou lucro liquido gerencial de R$ " + scale2(t.resultadoLiquidoGerencial())
                    + " (" + ind.margemLiquidaPercentual() + "% da receita liquida) em " + janela.rotulo() + ".";
        } else if (t.resultadoLiquidoGerencial().compareTo(BigDecimal.ZERO) < 0) {
            situacao = "PREJUIZO";
            manchete = "A operacao acumulou prejuizo gerencial de R$ " + scale2(t.resultadoLiquidoGerencial().abs())
                    + " (" + ind.margemLiquidaPercentual() + "% sobre a receita liquida) em " + janela.rotulo() + ".";
        } else {
            situacao = "EQUILIBRIO";
            manchete = "A operacao fechou exatamente no ponto de equilibrio (R$ 0,00) em " + janela.rotulo() + ".";
        }

        String expResultado = "A Receita Liquida foi de R$ " + ind.receitaLiquida()
                + ", o CMV consumiu R$ " + ind.cmv()
                + ", gerando R$ " + ind.lucroBruto() + " de Lucro Bruto (" + ind.margemBrutaPercentual()
                + "%). Apos as despesas operacionais (R$ " + ind.despesasOperacionaisTotais()
                + "), o Resultado Operacional foi de R$ " + ind.resultadoOperacional()
                + ", somado ao Resultado Financeiro Liquido de R$ " + ind.resultadoFinanceiroLiquido()
                + " (LAIR de R$ " + ind.resultadoAntesTributos()
                + "), resultando em R$ " + ind.resultadoLiquidoGerencial() + " de Resultado Liquido Gerencial.";

        String expCompCaixa;
        if (ind.diferencaCompetenciaVsCaixa().compareTo(BigDecimal.ZERO) == 0) {
            expCompCaixa = "O resultado economico da DRE (R$ " + ind.resultadoLiquidoGerencial()
                    + ") coincide com o saldo realizado no caixa no periodo (R$ " + ind.saldoRealizadoCaixa() + ").";
        } else if (ind.resultadoLiquidoGerencial().compareTo(ind.saldoRealizadoCaixa()) > 0) {
            expCompCaixa = "Atencao: o lucro economico na DRE (R$ " + ind.resultadoLiquidoGerencial()
                    + ") e maior que o dinheiro liquido que entrou no caixa (R$ " + ind.saldoRealizadoCaixa()
                    + "). Isso ocorre quando ha vendas a prazo/parceladas ainda nao recebidas ou pagamentos de obrigacoes de meses anteriores.";
        } else {
            expCompCaixa = "O saldo realizado no caixa (R$ " + ind.saldoRealizadoCaixa()
                    + ") superou o resultado por competencia da DRE (R$ " + ind.resultadoLiquidoGerencial()
                    + "), pois houve recebimento de vendas de periodos anteriores ou despesas lançadas na competencia que ainda nao venceram/foram pagas.";
        }

        String expPe = ind.pontoEquilibrioContabilReceita().compareTo(BigDecimal.ZERO) > 0
                ? "Com margem de contribuicao de " + ind.margemContribuicaoPercentual()
                        + "% e despesas fixas de R$ " + ind.despesasFixas()
                        + ", a empresa precisa faturar R$ " + ind.pontoEquilibrioContabilReceita()
                        + " de receita liquida no periodo para empatar custos e despesas."
                : "Cadastre e classifique custos variaveis e despesas fixas para apurar o Ponto de Equilibrio Contabil.";

        List<DreFatorRelevanteDTO> fatores = new ArrayList<>();
        for (DreLinhaDTO l : linhas) {
            if (l.numeroLinha() == 6 || (l.numeroLinha() >= 8 && l.numeroLinha() <= 11)) {
                if (l.valor().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal pctRec = defaultedPercent(l.analiseVerticalPercentual());
                    String imp = pctRec.compareTo(new BigDecimal("25.00")) >= 0 ? "NEGATIVO" : "ATENCAO";
                    fatores.add(new DreFatorRelevanteDTO(
                            l.titulo(),
                            l.tipoLinha(),
                            l.valor(),
                            pctRec,
                            l.analiseHorizontalPercentual(),
                            imp,
                            l.titulo() + " representou " + pctRec + "% da receita liquida do periodo."));
                }
            } else if (l.numeroLinha() == 14 && l.valor().compareTo(BigDecimal.ZERO) < 0) {
                BigDecimal valAbs = l.valor().abs();
                BigDecimal pctRec = defaultedPercent(calcPercent(valAbs, ind.receitaLiquida()));
                fatores.add(new DreFatorRelevanteDTO(
                        l.titulo(),
                        l.tipoLinha(),
                        valAbs,
                        pctRec,
                        l.analiseHorizontalPercentual(),
                        "ATENCAO",
                        "O Resultado Financeiro Liquido negativo consumiu " + pctRec + "% da receita liquida."));
            }
        }
        fatores.sort(Comparator.comparing(DreFatorRelevanteDTO::valor).reversed());
        if (fatores.size() > 4) {
            fatores = new ArrayList<>(fatores.subList(0, 4));
        }

        List<String> recomendacoes = new ArrayList<>();
        if (principal.lancamentosSemClassificacao() > 0) {
            recomendacoes.add("Classifique os " + principal.lancamentosSemClassificacao()
                    + " lancamento(s) pendentes no Plano de Contas para detalhar corretamente cada grupo de despesa/receita.");
        }
        if (t.resultadoLiquidoGerencial().compareTo(BigDecimal.ZERO) < 0) {
            recomendacoes.add("O resultado do periodo foi negativo; revise os principais grupos de despesas acima e compare o preco de venda/margem dos produtos vendidos.");
        }
        if (tComp != null && tComp.receitaLiquida().compareTo(BigDecimal.ZERO) > 0
                && t.despesasOperacionaisTotais().compareTo(tComp.despesasOperacionaisTotais()) > 0) {
            BigDecimal crescRec = defaultedPercent(calcHorizontalPercent(t.receitaLiquida(), tComp.receitaLiquida()));
            BigDecimal crescDesp = defaultedPercent(calcHorizontalPercent(t.despesasOperacionaisTotais(), tComp.despesasOperacionaisTotais()));
            if (crescDesp.compareTo(crescRec) > 0) {
                recomendacoes.add("As despesas cresceram " + crescDesp
                        + "% em relacao ao periodo anterior, ritmo superior a variacao da receita liquida (" + crescRec + "%).");
            }
        }
        if (recomendacoes.isEmpty()) {
            recomendacoes.add("Mantenha o registro diario de vendas, custos de entrada e classificacao das despesas para preservar a confiabilidade da DRE.");
        }

        return new DreDiagnosticoGerencialDTO(
                situacao,
                manchete,
                expResultado,
                expCompCaixa,
                expPe,
                fatores,
                recomendacoes,
                List.copyOf(principal.avisos()));
    }

    // =========================================================================
    // Resolucao de Periodo, Escopo Multi-Filial e Helpers
    // =========================================================================

    private JanelaPeriodo resolverPeriodo(FiltroDreRequest f) {
        String tipo = f.tipoPeriodo() == null || f.tipoPeriodo().isBlank()
                ? "MENSAL"
                : f.tipoPeriodo().trim().toUpperCase(Locale.ROOT);

        LocalDate inicio;
        LocalDate fim;
        String rotulo;
        LocalDate compInicio;
        LocalDate compFim;
        String rotuloComp;

        switch (tipo) {
            case "MENSAL" -> {
                YearMonth ym = consolidacaoService.parseCompetencia(f.competencia());
                inicio = ym.atDay(1);
                fim = ym.atEndOfMonth();
                rotulo = formatarRotuloMes(ym);
                YearMonth ymComp = (f.competenciaComparacao() != null && !f.competenciaComparacao().isBlank())
                        ? consolidacaoService.parseCompetencia(f.competenciaComparacao())
                        : ym.minusMonths(1);
                compInicio = ymComp.atDay(1);
                compFim = ymComp.atEndOfMonth();
                rotuloComp = formatarRotuloMes(ymComp);
            }
            case "TRIMESTRAL" -> {
                int ano = f.ano() != null ? f.ano() : LocalDate.now().getYear();
                int tri = f.trimestre() != null ? f.trimestre() : ((LocalDate.now().getMonthValue() - 1) / 3) + 1;
                if (tri < 1 || tri > 4) {
                    throw new IllegalArgumentException("Trimestre deve estar entre 1 e 4.");
                }
                YearMonth m1 = YearMonth.of(ano, (tri - 1) * 3 + 1);
                YearMonth m3 = m1.plusMonths(2);
                inicio = m1.atDay(1);
                fim = m3.atEndOfMonth();
                rotulo = tri + "o Trimestre/" + ano;

                YearMonth compM1 = m1.minusMonths(3);
                YearMonth compM3 = compM1.plusMonths(2);
                compInicio = compM1.atDay(1);
                compFim = compM3.atEndOfMonth();
                int triAnt = ((compM1.getMonthValue() - 1) / 3) + 1;
                rotuloComp = triAnt + "o Trimestre/" + compM1.getYear();
            }
            case "SEMESTRAL" -> {
                int ano = f.ano() != null ? f.ano() : LocalDate.now().getYear();
                int sem = f.semestre() != null ? f.semestre() : (LocalDate.now().getMonthValue() <= 6 ? 1 : 2);
                if (sem < 1 || sem > 2) {
                    throw new IllegalArgumentException("Semestre deve ser 1 ou 2.");
                }
                YearMonth m1 = YearMonth.of(ano, sem == 1 ? 1 : 7);
                YearMonth m6 = m1.plusMonths(5);
                inicio = m1.atDay(1);
                fim = m6.atEndOfMonth();
                rotulo = sem + "o Semestre/" + ano;

                YearMonth compM1 = m1.minusMonths(6);
                YearMonth compM6 = compM1.plusMonths(5);
                compInicio = compM1.atDay(1);
                compFim = compM6.atEndOfMonth();
                int semAnt = compM1.getMonthValue() <= 6 ? 1 : 2;
                rotuloComp = semAnt + "o Semestre/" + compM1.getYear();
            }
            case "ANUAL" -> {
                int ano = f.ano() != null ? f.ano() : LocalDate.now().getYear();
                if (ano < 2000 || ano > 2100) {
                    throw new IllegalArgumentException("Ano invalido para apuracao da DRE.");
                }
                inicio = LocalDate.of(ano, 1, 1);
                fim = LocalDate.of(ano, 12, 31);
                rotulo = "Ano " + ano;
                compInicio = LocalDate.of(ano - 1, 1, 1);
                compFim = LocalDate.of(ano - 1, 12, 31);
                rotuloComp = "Ano " + (ano - 1);
            }
            case "PERSONALIZADO" -> {
                if (f.dataInicio() == null || f.dataFim() == null) {
                    throw new IllegalArgumentException("Para periodo PERSONALIZADO informe dataInicio e dataFim.");
                }
                if (f.dataFim().isBefore(f.dataInicio())) {
                    throw new IllegalArgumentException("dataFim nao pode ser anterior a dataInicio.");
                }
                long dias = ChronoUnit.DAYS.between(f.dataInicio(), f.dataFim()) + 1;
                if (dias > MAX_DIAS_PERIODO) {
                    throw new IllegalArgumentException("Periodo personalizado excede o limite de " + MAX_DIAS_PERIODO + " dias.");
                }
                inicio = f.dataInicio();
                fim = f.dataFim();
                rotulo = inicio + " a " + fim;
                compInicio = inicio.minusDays(dias);
                compFim = inicio.minusDays(1);
                rotuloComp = compInicio + " a " + compFim;
            }
            default -> throw new IllegalArgumentException(
                    "tipoPeriodo invalido. Valores aceitos: MENSAL, TRIMESTRAL, SEMESTRAL, ANUAL, PERSONALIZADO.");
        }

        if (f.comparacaoInicio() != null && f.comparacaoFim() != null) {
            if (f.comparacaoFim().isBefore(f.comparacaoInicio())) {
                throw new IllegalArgumentException("comparacaoFim nao pode ser anterior a comparacaoInicio.");
            }
            compInicio = f.comparacaoInicio();
            compFim = f.comparacaoFim();
            rotuloComp = compInicio + " a " + compFim;
        }

        return new JanelaPeriodo(tipo, rotulo, inicio, fim, rotuloComp, compInicio, compFim);
    }

    private EscopoFiliais resolverEscopoFiliais(
            Long filialSolicitadaId,
            Boolean consolidarFiliais,
            Long empresaId,
            Long currentFilialId) {
        List<FilialInfo> filiaisEmpresa = listarFiliaisEmpresa(empresaId, currentFilialId);
        if (Boolean.TRUE.equals(consolidarFiliais)) {
            List<Long> ids = filiaisEmpresa.stream().map(FilialInfo::idFilial).toList();
            return new EscopoFiliais(ids, currentFilialId, null, "Consolidado Empresa (" + ids.size() + " filial/is)", true);
        }
        Long alvoId = filialSolicitadaId == null ? currentFilialId : filialSolicitadaId;
        FilialInfo info = filiaisEmpresa.stream()
                .filter(f -> Objects.equals(f.idFilial(), alvoId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Filial nao encontrada no escopo da empresa ativa."));
        return new EscopoFiliais(List.of(alvoId), alvoId, alvoId, info.nome(), false);
    }

    private List<FilialInfo> listarFiliaisEmpresa(Long empresaId, Long currentFilialId) {
        List<Filial> cadastradas = filialRepository.findByEmpresaIdEmpresaAndDeletedAtIsNullOrderByIdFilialAsc(empresaId);
        Map<Long, FilialInfo> mapa = new LinkedHashMap<>();
        for (Filial f : cadastradas) {
            if (f.getStatus() != 0) {
                mapa.put(f.getIdFilial(), new FilialInfo(
                        f.getIdFilial(),
                        f.getCodigo() == null ? "FIL-" + f.getIdFilial() : f.getCodigo(),
                        f.getNome()));
            }
        }
        if (currentFilialId != null && !mapa.containsKey(currentFilialId)) {
            mapa.put(currentFilialId, new FilialInfo(
                    currentFilialId,
                    "FIL-" + currentFilialId,
                    "Filial #" + currentFilialId));
        }
        return List.copyOf(mapa.values());
    }

    private CentroCusto validarCentroCustoFiltro(Long centroCustoId, Long empresaId, Long filialId) {
        if (centroCustoId == null) {
            return null;
        }
        return centroCustoRepository.findByIdCentroCustoAndEmpresaIdAndFilialId(centroCustoId, empresaId, filialId)
                .orElseThrow(() -> new EntityNotFoundException("Centro de custo nao encontrado no escopo do tenant."));
    }

    private DefinicaoLinhaDre resolverDefinicaoLinha(Integer numeroLinha, String codigoLinha) {
        if (numeroLinha != null) {
            return ESTRUTURA_17_LINHAS.stream()
                    .filter(d -> d.numero() == numeroLinha)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("numeroLinha da DRE deve estar entre 1 e 17."));
        }
        if (codigoLinha != null && !codigoLinha.isBlank()) {
            String norm = codigoLinha.trim().toUpperCase(Locale.ROOT);
            return ESTRUTURA_17_LINHAS.stream()
                    .filter(d -> d.codigo().equals(norm))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("codigoLinha da DRE desconhecido: " + codigoLinha));
        }
        return null;
    }

    private DefinicaoLinhaDre resolverLinhaPorGrupo(GrupoDreGerencial grupo, int fallbackNumero) {
        if (grupo != null) {
            for (DefinicaoLinhaDre d : ESTRUTURA_17_LINHAS) {
                if (d.grupos().contains(grupo)) {
                    return d;
                }
            }
        }
        return ESTRUTURA_17_LINHAS.get(Math.max(0, Math.min(ESTRUTURA_17_LINHAS.size() - 1, fallbackNumero - 1)));
    }

    private Set<Long> resolverContasComDescendentes(Long planoContaId, Map<Long, PlanoContasGerencial> contasPorId) {
        if (planoContaId == null || planoContaId <= 0) {
            return Set.of();
        }
        if (!contasPorId.containsKey(planoContaId)) {
            throw new EntityNotFoundException("Conta gerencial nao encontrada no plano de contas do tenant.");
        }
        Set<Long> resultado = new LinkedHashSet<>();
        resultado.add(planoContaId);
        boolean adicionou = true;
        int guard = 0;
        while (adicionou && guard++ < 20) {
            adicionou = false;
            for (PlanoContasGerencial c : contasPorId.values()) {
                if (c.getContaPaiId() != null && resultado.contains(c.getContaPaiId())
                        && resultado.add(c.getIdPlanoConta())) {
                    adicionou = true;
                }
            }
        }
        // Em consolidacao multi-filial, inclui tambem contas homonimas pelo mesmo codigo nas demais filiais da empresa
        Set<String> codigosAlvo = new LinkedHashSet<>();
        for (Long id : resultado) {
            PlanoContasGerencial c = contasPorId.get(id);
            if (c != null && c.getCodigo() != null) {
                codigosAlvo.add(c.getCodigo());
            }
        }
        for (PlanoContasGerencial c : contasPorId.values()) {
            if (c.getCodigo() != null && codigosAlvo.contains(c.getCodigo())) {
                resultado.add(c.getIdPlanoConta());
            }
        }
        return resultado;
    }

    private DreLancamentoOrigemDTO criarLancamento(
            String origemTipo,
            Long registroId,
            String documentoReferencia,
            String descricao,
            LocalDate dataCompetencia,
            LocalDate dataEmissao,
            LocalDate dataLiquidacaoCaixa,
            String statusOperacional,
            BigDecimal valorCompetencia,
            String impactoDre,
            DefinicaoLinhaDre linha,
            PlanoContasGerencial conta,
            CentroCusto cc,
            Long filialId,
            String rotaFrontend) {
        return new DreLancamentoOrigemDTO(
                origemTipo,
                registroId,
                documentoReferencia,
                descricao,
                dataCompetencia,
                dataEmissao,
                dataLiquidacaoCaixa,
                statusOperacional,
                scale2(valorCompetencia),
                impactoDre,
                linha.numero(),
                linha.codigo(),
                linha.titulo(),
                conta == null ? null : conta.getIdPlanoConta(),
                conta == null ? null : conta.getCodigo(),
                conta == null ? "Pendente de Classificacao" : conta.getNome(),
                conta == null ? null : conta.getGrupoDre(),
                cc == null ? null : cc.getIdCentroCusto(),
                cc == null ? null : cc.getCodigo(),
                cc == null ? null : cc.getNome(),
                filialId,
                rotaFrontend);
    }

    private String classificarStatusVariacao(String tipoLinha, BigDecimal atual, BigDecimal anterior) {
        if (anterior == null || anterior.compareTo(BigDecimal.ZERO) == 0) {
            return "SEM_BASE";
        }
        int cmp = atual.compareTo(anterior);
        if (cmp == 0) {
            return "ESTAVEL";
        }
        boolean aumentoBom = "RECEITA".equals(tipoLinha)
                || "RESULTADO_PARCIAL".equals(tipoLinha)
                || "RESULTADO_FINAL".equals(tipoLinha)
                || "OUTRAS".equals(tipoLinha)
                || "FINANCEIRO".equals(tipoLinha);
        if (aumentoBom) {
            return cmp > 0 ? "FAVORAVEL" : "DESFAVORAVEL";
        }
        return cmp < 0 ? "FAVORAVEL" : "DESFAVORAVEL";
    }

    private String avaliarTendencia(BigDecimal primeiro, BigDecimal ultimo) {
        int cmp = nvl(ultimo).compareTo(nvl(primeiro));
        if (cmp > 0) return "CRESCENTE";
        if (cmp < 0) return "DECRESCENTE";
        return "ESTAVEL";
    }

    private BigDecimal calcPercent(BigDecimal parte, BigDecimal base) {
        if (parte == null || base == null || base.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return parte.multiply(CEM).divide(base.abs(), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcHorizontalPercent(BigDecimal atual, BigDecimal anterior) {
        if (atual == null || anterior == null || anterior.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return atual.subtract(anterior)
                .multiply(CEM)
                .divide(anterior.abs(), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultedPercent(BigDecimal val) {
        return val == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : val;
    }

    private BigDecimal scale2(BigDecimal val) {
        return nvl(val).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nvl(BigDecimal val) {
        return val == null ? BigDecimal.ZERO : val;
    }

    private String formatarRotuloMes(YearMonth ym) {
        String[] nomes = {"Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"};
        return nomes[ym.getMonthValue() - 1] + "/" + ym.getYear();
    }

    private String csvSafe(String val) {
        if (val == null) return "";
        return val.replace(';', ',').replace('\r', ' ').replace('\n', ' ').trim();
    }

    private String formatCsvDecimal(BigDecimal val) {
        return val == null ? "" : scale2(val).toPlainString().replace('.', ',');
    }

    private void requireTenant(Long empresaId, Long filialId) {
        if (empresaId == null || filialId == null) {
            throw new IllegalArgumentException("Empresa e filial ativas sao obrigatorias.");
        }
    }

    private record JanelaPeriodo(
            String tipoPeriodo,
            String rotulo,
            LocalDate inicio,
            LocalDate fim,
            String rotuloComparacao,
            LocalDate comparacaoInicio,
            LocalDate comparacaoFim) {
    }

    private record FilialInfo(Long idFilial, String codigo, String nome) {
    }

    private record EscopoFiliais(
            List<Long> filiaisIds,
            Long filialReferenciaPlano,
            Long filialUnicaId,
            String rotuloFilial,
            boolean consolidadoEmpresa) {
    }

    private static final class AcumuladorConta {
        private final PlanoContasGerencial conta;
        private BigDecimal valor = BigDecimal.ZERO;
        private long quantidade = 0L;

        private AcumuladorConta(PlanoContasGerencial conta) {
            this.conta = conta;
        }
    }

    private static final class RollupContaCodigo {
        private final PlanoContasGerencial conta;
        private BigDecimal valor = BigDecimal.ZERO;
        private long quantidade = 0L;

        private RollupContaCodigo(PlanoContasGerencial conta) {
            this.conta = conta;
        }
    }

    private record ApuracaoBruta(
            Map<Long, PlanoContasGerencial> contasPorId,
            Map<String, PlanoContasGerencial> contasPorCodigo,
            Map<Long, AcumuladorConta> acumuladorPorConta,
            Map<GrupoDreGerencial, BigDecimal> valoresSemContaPorGrupo,
            Map<GrupoDreGerencial, Long> qtdSemContaPorGrupo,
            List<DreLancamentoOrigemDTO> lancamentos,
            long lancamentosSemClassificacao,
            BigDecimal valorSemClassificacao,
            BigDecimal entradasRealizadasCaixa,
            BigDecimal saidasRealizadasCaixa,
            List<String> avisos) {
    }

    private record Totais17Linhas(
            BigDecimal receitaBruta,
            BigDecimal deducoesTotais,
            BigDecimal receitaLiquida,
            BigDecimal cmv,
            BigDecimal lucroBruto,
            BigDecimal despesasOperacionaisTotais,
            BigDecimal resultadoOperacional,
            BigDecimal resultadoFinanceiroLiquido,
            BigDecimal resultadoAntesTributos,
            BigDecimal tributosSobreLucro,
            BigDecimal resultadoLiquidoGerencial,
            Map<Integer, BigDecimal> porLinha) {
    }
}
