package Service;

import DTO.PlanoContasBootstrapResponseDTO;
import DTO.PlanoContasRequestDTO;
import DTO.PlanoContasResponseDTO;
import Model.ClassificacaoCusto;
import Model.GrupoDreGerencial;
import Model.NaturezaContaGerencial;
import Model.PlanoContasGerencial;
import Model.TipoContaGerencial;
import Repository.PlanoContasGerencialRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanoContasService {

    private static final List<SeedConta> CONTAS_PADRAO = List.of(
            new SeedConta("1", null, "Receita Bruta", "Grupo sintetico de receitas brutas",
                    TipoContaGerencial.RECEITA, NaturezaContaGerencial.CREDITO, ClassificacaoCusto.NAO_APLICAVEL,
                    GrupoDreGerencial.RECEITA_BRUTA, 10, false),
            new SeedConta("1.01", "1", "Vendas PDV/Loja", "Receita operacional de vendas em loja/PDV",
                    TipoContaGerencial.RECEITA, NaturezaContaGerencial.CREDITO, ClassificacaoCusto.VARIAVEL,
                    GrupoDreGerencial.RECEITA_BRUTA, 20, true),
            new SeedConta("1.02", "1", "Outras Receitas Operacionais", "Receitas operacionais acessorias",
                    TipoContaGerencial.RECEITA, NaturezaContaGerencial.CREDITO, ClassificacaoCusto.NAO_APLICAVEL,
                    GrupoDreGerencial.RECEITA_BRUTA, 30, true),
            new SeedConta("2", null, "Deducoes sobre Vendas", "Grupo sintetico de deducoes da receita bruta",
                    TipoContaGerencial.DEDUCAO, NaturezaContaGerencial.DEBITO, ClassificacaoCusto.VARIAVEL,
                    GrupoDreGerencial.NAO_APLICAVEL_DRE, 40, false),
            new SeedConta("2.01", "2", "Devolucoes e Cancelamentos", "Abatimentos por devolucoes e cancelamentos",
                    TipoContaGerencial.DEDUCAO, NaturezaContaGerencial.DEBITO, ClassificacaoCusto.VARIAVEL,
                    GrupoDreGerencial.DEVOLUCOES_CANCELAMENTOS, 50, true),
            new SeedConta("2.02", "2", "Descontos Concedidos", "Descontos comerciais concedidos em vendas",
                    TipoContaGerencial.DEDUCAO, NaturezaContaGerencial.DEBITO, ClassificacaoCusto.VARIAVEL,
                    GrupoDreGerencial.DESCONTOS_CONCEDIDOS, 60, true),
            new SeedConta("2.03", "2", "Tributos sobre Vendas", "ICMS, PIS, COFINS, ISS e Simples sobre faturamento",
                    TipoContaGerencial.DEDUCAO, NaturezaContaGerencial.DEBITO, ClassificacaoCusto.VARIAVEL,
                    GrupoDreGerencial.TRIBUTOS_SOBRE_VENDAS, 70, true),
            new SeedConta("3", null, "CMV", "Grupo sintetico de custo das mercadorias vendidas",
                    TipoContaGerencial.CMV, NaturezaContaGerencial.DEBITO, ClassificacaoCusto.VARIAVEL,
                    GrupoDreGerencial.CMV, 80, false),
            new SeedConta("3.01", "3", "Custo das Mercadorias Vendidas", "Custo direto das mercadorias comercializadas",
                    TipoContaGerencial.CMV, NaturezaContaGerencial.DEBITO, ClassificacaoCusto.VARIAVEL,
                    GrupoDreGerencial.CMV, 90, true),
            new SeedConta("4", null, "Despesas Comerciais", "Grupo sintetico de despesas comerciais",
                    TipoContaGerencial.DESPESA, NaturezaContaGerencial.DEBITO, ClassificacaoCusto.VARIAVEL,
                    GrupoDreGerencial.DESPESA_COMERCIAL, 100, false),
            new SeedConta("4.01", "4", "Comissoes e Marketing", "Comissoes de vendas, promocoes e divulgacao",
                    TipoContaGerencial.DESPESA, NaturezaContaGerencial.DEBITO, ClassificacaoCusto.VARIAVEL,
                    GrupoDreGerencial.DESPESA_COMERCIAL, 110, true),
            new SeedConta("5", null, "Despesas Administrativas", "Grupo sintetico de despesas administrativas",
                    TipoContaGerencial.DESPESA, NaturezaContaGerencial.DEBITO, ClassificacaoCusto.FIXO,
                    GrupoDreGerencial.DESPESA_ADMINISTRATIVA, 120, false),
            new SeedConta("5.01", "5", "Despesas Gerais e Escritorio", "Contabilidade, material, servicos e escritorio",
                    TipoContaGerencial.DESPESA, NaturezaContaGerencial.DEBITO, ClassificacaoCusto.FIXO,
                    GrupoDreGerencial.DESPESA_ADMINISTRATIVA, 130, true),
            new SeedConta("6", null, "Despesas Operacionais", "Grupo sintetico de despesas operacionais",
                    TipoContaGerencial.DESPESA, NaturezaContaGerencial.DEBITO, ClassificacaoCusto.FIXO,
                    GrupoDreGerencial.DESPESA_OPERACIONAL, 140, false),
            new SeedConta("6.01", "6", "Manutencao e Operacao de Loja", "Manutencao, limpeza, suprimentos e operacao",
                    TipoContaGerencial.DESPESA, NaturezaContaGerencial.DEBITO, ClassificacaoCusto.FIXO,
                    GrupoDreGerencial.DESPESA_OPERACIONAL, 150, true),
            new SeedConta("7", null, "Despesas com Pessoal", "Grupo sintetico de despesas com pessoal",
                    TipoContaGerencial.DESPESA, NaturezaContaGerencial.DEBITO, ClassificacaoCusto.FIXO,
                    GrupoDreGerencial.DESPESA_PESSOAL, 160, false),
            new SeedConta("7.01", "7", "Folha e Encargos", "Salarios, pro-labore, beneficios e encargos sociais",
                    TipoContaGerencial.DESPESA, NaturezaContaGerencial.DEBITO, ClassificacaoCusto.FIXO,
                    GrupoDreGerencial.DESPESA_PESSOAL, 170, true),
            new SeedConta("8", null, "Despesas com Infraestrutura", "Grupo sintetico de infraestrutura e ocupacao",
                    TipoContaGerencial.DESPESA, NaturezaContaGerencial.DEBITO, ClassificacaoCusto.FIXO,
                    GrupoDreGerencial.DESPESA_INFRAESTRUTURA, 180, false),
            new SeedConta("8.01", "8", "Aluguel, Energia e Telecom", "Ocupacao, energia eletrica, agua e internet",
                    TipoContaGerencial.DESPESA, NaturezaContaGerencial.DEBITO, ClassificacaoCusto.FIXO,
                    GrupoDreGerencial.DESPESA_INFRAESTRUTURA, 190, true),
            new SeedConta("9", null, "Resultado Financeiro", "Grupo sintetico de receitas e despesas financeiras",
                    TipoContaGerencial.FINANCEIRO, NaturezaContaGerencial.DEBITO, ClassificacaoCusto.NAO_APLICAVEL,
                    GrupoDreGerencial.NAO_APLICAVEL_DRE, 200, false),
            new SeedConta("9.01", "9", "Despesas Financeiras e Tarifas", "Juros pagos, taxas bancarias e tarifas de cartao",
                    TipoContaGerencial.FINANCEIRO, NaturezaContaGerencial.DEBITO, ClassificacaoCusto.VARIAVEL,
                    GrupoDreGerencial.DESPESA_FINANCEIRA, 210, true),
            new SeedConta("9.02", "9", "Receitas Financeiras", "Rendimentos, juros recebidos e descontos obtidos",
                    TipoContaGerencial.FINANCEIRO, NaturezaContaGerencial.CREDITO, ClassificacaoCusto.NAO_APLICAVEL,
                    GrupoDreGerencial.RECEITA_FINANCEIRA, 220, true),
            new SeedConta("10", null, "Outras Receitas e Despesas", "Grupo sintetico de itens nao operacionais",
                    TipoContaGerencial.OUTRAS, NaturezaContaGerencial.DEBITO, ClassificacaoCusto.NAO_APLICAVEL,
                    GrupoDreGerencial.NAO_APLICAVEL_DRE, 230, false),
            new SeedConta("10.01", "10", "Outras Receitas", "Receitas eventuais nao operacionais",
                    TipoContaGerencial.OUTRAS, NaturezaContaGerencial.CREDITO, ClassificacaoCusto.NAO_APLICAVEL,
                    GrupoDreGerencial.OUTRAS_RECEITAS, 240, true),
            new SeedConta("10.02", "10", "Outras Despesas", "Despesas eventuais nao operacionais",
                    TipoContaGerencial.OUTRAS, NaturezaContaGerencial.DEBITO, ClassificacaoCusto.NAO_APLICAVEL,
                    GrupoDreGerencial.OUTRAS_DESPESAS, 250, true),
            new SeedConta("11.01", null, "Tributos sobre o Lucro", "IRPJ, CSLL e provisoes tributarias sobre o lucro",
                    TipoContaGerencial.TRIBUTO_LUCRO, NaturezaContaGerencial.DEBITO, ClassificacaoCusto.VARIAVEL,
                    GrupoDreGerencial.TRIBUTOS_SOBRE_LUCRO, 260, true)
    );

    private final PlanoContasGerencialRepository planoContasRepository;
    private final CentroCustoService centroCustoService;
    private final FinancialAuditTrailService financialAuditTrailService;
    private final AuditTrailService auditTrailService;
    private final PrivacyGuardService privacyGuardService;

    public PlanoContasService(
            PlanoContasGerencialRepository planoContasRepository,
            CentroCustoService centroCustoService,
            FinancialAuditTrailService financialAuditTrailService,
            AuditTrailService auditTrailService,
            PrivacyGuardService privacyGuardService) {
        this.planoContasRepository = planoContasRepository;
        this.centroCustoService = centroCustoService;
        this.financialAuditTrailService = financialAuditTrailService;
        this.auditTrailService = auditTrailService;
        this.privacyGuardService = privacyGuardService;
    }

    @Transactional(readOnly = true)
    public List<PlanoContasResponseDTO> listar(
            Boolean somenteAtivos,
            GrupoDreGerencial grupoDre,
            TipoContaGerencial tipoConta,
            Long empresaId,
            Long filialId) {
        requireTenant(empresaId, filialId);
        List<PlanoContasGerencial> contas = Boolean.TRUE.equals(somenteAtivos)
                ? planoContasRepository.findByEmpresaIdAndFilialIdAndAtivoTrueOrderByOrdemExibicaoAscCodigoAsc(
                        empresaId, filialId)
                : planoContasRepository.findByEmpresaIdAndFilialIdOrderByOrdemExibicaoAscCodigoAsc(
                        empresaId, filialId);
        return contas.stream()
                .filter(c -> grupoDre == null || c.getGrupoDre() == grupoDre)
                .filter(c -> tipoConta == null || c.getTipoConta() == tipoConta)
                .map(c -> toResponse(c, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlanoContasResponseDTO> arvore(Boolean somenteAtivos, Long empresaId, Long filialId) {
        requireTenant(empresaId, filialId);
        List<PlanoContasGerencial> contas = Boolean.TRUE.equals(somenteAtivos)
                ? planoContasRepository.findByEmpresaIdAndFilialIdAndAtivoTrueOrderByOrdemExibicaoAscCodigoAsc(
                        empresaId, filialId)
                : planoContasRepository.findByEmpresaIdAndFilialIdOrderByOrdemExibicaoAscCodigoAsc(
                        empresaId, filialId);

        Map<Long, PlanoContasGerencial> porId = new LinkedHashMap<>();
        for (PlanoContasGerencial conta : contas) {
            porId.put(conta.getIdPlanoConta(), conta);
        }

        Map<Long, List<PlanoContasGerencial>> filhasPorPai = new HashMap<>();
        List<PlanoContasGerencial> raizes = new ArrayList<>();
        for (PlanoContasGerencial conta : contas) {
            Long paiId = conta.getContaPaiId();
            if (paiId == null || !porId.containsKey(paiId) || Objects.equals(paiId, conta.getIdPlanoConta())) {
                raizes.add(conta);
            } else {
                filhasPorPai.computeIfAbsent(paiId, ignored -> new ArrayList<>()).add(conta);
            }
        }

        Set<Long> visitados = new HashSet<>();
        List<PlanoContasResponseDTO> resultado = new ArrayList<>();
        for (PlanoContasGerencial raiz : raizes) {
            if (visitados.add(raiz.getIdPlanoConta())) {
                resultado.add(montarNoArvore(raiz, filhasPorPai, visitados));
            }
        }
        return List.copyOf(resultado);
    }

    @Transactional(readOnly = true)
    public PlanoContasResponseDTO buscarPorId(Long id, Long empresaId, Long filialId) {
        requireTenant(empresaId, filialId);
        PlanoContasGerencial conta = planoContasRepository
                .findByIdPlanoContaAndEmpresaIdAndFilialId(id, empresaId, filialId)
                .orElseThrow(() -> new EntityNotFoundException("Conta gerencial nao encontrada."));
        return toResponse(conta, List.of());
    }

    @Transactional
    public PlanoContasBootstrapResponseDTO inicializarPadrao(
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);

        Map<String, Long> idPorCodigo = new HashMap<>();
        for (PlanoContasGerencial existente : planoContasRepository
                .findByEmpresaIdAndFilialIdOrderByOrdemExibicaoAscCodigoAsc(empresaId, filialId)) {
            idPorCodigo.put(existente.getCodigo(), existente.getIdPlanoConta());
        }

        int criadas = 0;
        int jaExistentes = 0;
        for (SeedConta seed : CONTAS_PADRAO) {
            Optional<PlanoContasGerencial> existente = planoContasRepository
                    .findByCodigoAndEmpresaIdAndFilialId(seed.codigo(), empresaId, filialId);
            if (existente.isPresent()) {
                jaExistentes++;
                idPorCodigo.put(seed.codigo(), existente.get().getIdPlanoConta());
                continue;
            }
            PlanoContasGerencial nova = new PlanoContasGerencial();
            nova.setEmpresaId(empresaId);
            nova.setFilialId(filialId);
            nova.setContaPaiId(seed.codigoPai() == null ? null : idPorCodigo.get(seed.codigoPai()));
            nova.setCodigo(seed.codigo());
            nova.setNome(seed.nome());
            nova.setDescricao(seed.descricao());
            nova.setTipoConta(seed.tipoConta());
            nova.setNatureza(seed.natureza());
            nova.setClassificacaoCusto(seed.classificacaoCusto());
            nova.setGrupoDre(seed.grupoDre());
            nova.setOrdemExibicao(seed.ordemExibicao());
            nova.setAceitaLancamento(seed.aceitaLancamento());
            nova.setSistemaPadrao(true);
            nova.setAtivo(true);
            PlanoContasGerencial salva = planoContasRepository.save(nova);
            idPorCodigo.put(salva.getCodigo(), salva.getIdPlanoConta());
            criadas++;
        }

        centroCustoService.garantirPadraoInterno(empresaId, filialId);

        List<PlanoContasResponseDTO> todas = planoContasRepository
                .findByEmpresaIdAndFilialIdOrderByOrdemExibicaoAscCodigoAsc(empresaId, filialId)
                .stream()
                .map(c -> toResponse(c, List.of()))
                .toList();

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("contasCriadas", criadas);
        metadata.put("contasJaExistentes", jaExistentes);
        metadata.put("totalContas", todas.size());
        financialAuditTrailService.record(
                "PLANO_CONTAS_BOOTSTRAP",
                "PlanoContasGerencial",
                null,
                actorLogin(request),
                null,
                "criadas=" + criadas + ";existentes=" + jaExistentes + ";total=" + todas.size(),
                metadata,
                empresaId,
                filialId);
        auditTrailService.registrarPrivacidade(
                "PLANO_CONTAS_BOOTSTRAP",
                "PlanoContasGerencial",
                null,
                "Financeiro",
                null,
                "criadas=" + criadas + ";existentes=" + jaExistentes + ";total=" + todas.size(),
                request,
                "Inicializacao idempotente do plano de contas gerencial padrao.");

        return new PlanoContasBootstrapResponseDTO(criadas, jaExistentes, todas.size(), todas);
    }

    @Transactional
    public PlanoContasResponseDTO criar(
            PlanoContasRequestDTO body,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        String codigo = normalizeRequired(body.codigo(), "Codigo da conta e obrigatorio.");
        String nome = normalizeRequired(body.nome(), "Nome da conta e obrigatorio.");
        if (planoContasRepository.existsByCodigoAndEmpresaIdAndFilialId(codigo, empresaId, filialId)) {
            throw new IllegalArgumentException("Ja existe conta gerencial com o codigo informado.");
        }
        validarContaPai(null, body.contaPaiId(), empresaId, filialId);

        PlanoContasGerencial entity = new PlanoContasGerencial();
        entity.setEmpresaId(empresaId);
        entity.setFilialId(filialId);
        entity.setContaPaiId(body.contaPaiId());
        entity.setCodigo(codigo);
        entity.setNome(nome);
        entity.setDescricao(trimToNull(body.descricao()));
        entity.setTipoConta(body.tipoConta());
        entity.setNatureza(body.natureza());
        entity.setClassificacaoCusto(body.classificacaoCusto());
        entity.setGrupoDre(body.grupoDre());
        entity.setOrdemExibicao(body.ordemExibicao() == null ? 0 : body.ordemExibicao());
        entity.setAceitaLancamento(body.aceitaLancamento() == null || body.aceitaLancamento());
        entity.setSistemaPadrao(false);
        entity.setAtivo(body.ativo() == null || body.ativo());

        PlanoContasGerencial salva = planoContasRepository.save(entity);
        registrarAuditoria(
                "PLANO_CONTAS_CRIADO",
                salva,
                null,
                resumo(salva),
                "Criacao de conta gerencial no plano de contas.",
                empresaId,
                filialId,
                request);
        return toResponse(salva, List.of());
    }

    @Transactional
    public PlanoContasResponseDTO atualizar(
            Long id,
            PlanoContasRequestDTO body,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        PlanoContasGerencial existente = planoContasRepository
                .findByIdPlanoContaAndEmpresaIdAndFilialId(id, empresaId, filialId)
                .orElseThrow(() -> new EntityNotFoundException("Conta gerencial nao encontrada."));

        String codigo = normalizeRequired(body.codigo(), "Codigo da conta e obrigatorio.");
        String nome = normalizeRequired(body.nome(), "Nome da conta e obrigatorio.");
        if (planoContasRepository.existsByCodigoAndEmpresaIdAndFilialIdAndIdPlanoContaNot(
                codigo, empresaId, filialId, id)) {
            throw new IllegalArgumentException("Ja existe outra conta gerencial com o codigo informado.");
        }
        validarContaPai(id, body.contaPaiId(), empresaId, filialId);
        if (Boolean.FALSE.equals(body.ativo()) && existente.isAtivo()) {
            validarSemFilhasAtivas(id, empresaId, filialId);
        }

        String anterior = resumo(existente);
        existente.setContaPaiId(body.contaPaiId());
        existente.setCodigo(codigo);
        existente.setNome(nome);
        existente.setDescricao(trimToNull(body.descricao()));
        existente.setTipoConta(body.tipoConta());
        existente.setNatureza(body.natureza());
        existente.setClassificacaoCusto(body.classificacaoCusto());
        existente.setGrupoDre(body.grupoDre());
        if (body.ordemExibicao() != null) {
            existente.setOrdemExibicao(body.ordemExibicao());
        }
        if (body.aceitaLancamento() != null) {
            existente.setAceitaLancamento(body.aceitaLancamento());
        }
        if (body.ativo() != null) {
            existente.setAtivo(body.ativo());
        }

        PlanoContasGerencial salva = planoContasRepository.save(existente);
        registrarAuditoria(
                "PLANO_CONTAS_ATUALIZADO",
                salva,
                anterior,
                resumo(salva),
                "Atualizacao de conta gerencial no plano de contas.",
                empresaId,
                filialId,
                request);
        return toResponse(salva, List.of());
    }

    @Transactional
    public PlanoContasResponseDTO alterarStatus(
            Long id,
            boolean ativo,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        requireTenant(empresaId, filialId);
        PlanoContasGerencial existente = planoContasRepository
                .findByIdPlanoContaAndEmpresaIdAndFilialId(id, empresaId, filialId)
                .orElseThrow(() -> new EntityNotFoundException("Conta gerencial nao encontrada."));
        if (!ativo && existente.isAtivo()) {
            validarSemFilhasAtivas(id, empresaId, filialId);
        }
        String anterior = resumo(existente);
        existente.setAtivo(ativo);
        PlanoContasGerencial salva = planoContasRepository.save(existente);
        registrarAuditoria(
                ativo ? "PLANO_CONTAS_ATIVADO" : "PLANO_CONTAS_INATIVADO",
                salva,
                anterior,
                resumo(salva),
                "Alteracao de status da conta gerencial.",
                empresaId,
                filialId,
                request);
        return toResponse(salva, List.of());
    }

    private PlanoContasResponseDTO montarNoArvore(
            PlanoContasGerencial atual,
            Map<Long, List<PlanoContasGerencial>> filhasPorPai,
            Set<Long> visitados) {
        List<PlanoContasGerencial> filhasEntidades = filhasPorPai.getOrDefault(atual.getIdPlanoConta(), List.of());
        List<PlanoContasResponseDTO> filhasDto = new ArrayList<>();
        for (PlanoContasGerencial filha : filhasEntidades) {
            if (visitados.add(filha.getIdPlanoConta())) {
                filhasDto.add(montarNoArvore(filha, filhasPorPai, visitados));
            }
        }
        return toResponse(atual, List.copyOf(filhasDto));
    }

    private void validarContaPai(Long contaId, Long contaPaiId, Long empresaId, Long filialId) {
        if (contaPaiId == null) {
            return;
        }
        if (contaId != null && Objects.equals(contaId, contaPaiId)) {
            throw new IllegalArgumentException("Uma conta nao pode ser pai de si mesma.");
        }
        PlanoContasGerencial pai = planoContasRepository
                .findByIdPlanoContaAndEmpresaIdAndFilialId(contaPaiId, empresaId, filialId)
                .orElseThrow(() -> new IllegalArgumentException("Conta pai nao encontrada no tenant."));
        if (contaId != null) {
            Set<Long> visitados = new HashSet<>();
            visitados.add(contaId);
            Long cursor = pai.getContaPaiId();
            while (cursor != null) {
                if (!visitados.add(cursor)) {
                    throw new IllegalArgumentException("Hierarquia invalida: ciclo detectado no plano de contas.");
                }
                cursor = planoContasRepository.findByIdPlanoContaAndEmpresaIdAndFilialId(cursor, empresaId, filialId)
                        .map(PlanoContasGerencial::getContaPaiId)
                        .orElse(null);
            }
        }
    }

    private void validarSemFilhasAtivas(Long contaId, Long empresaId, Long filialId) {
        boolean possuiFilhaAtiva = planoContasRepository
                .findByEmpresaIdAndFilialIdAndContaPaiIdOrderByOrdemExibicaoAscCodigoAsc(empresaId, filialId, contaId)
                .stream()
                .anyMatch(PlanoContasGerencial::isAtivo);
        if (possuiFilhaAtiva) {
            throw new IllegalArgumentException("Nao e possivel inativar conta que possui contas filhas ativas.");
        }
    }

    private void registrarAuditoria(
            String evento,
            PlanoContasGerencial salvo,
            String anterior,
            String novo,
            String justificativa,
            Long empresaId,
            Long filialId,
            HttpServletRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("idPlanoConta", salvo.getIdPlanoConta());
        metadata.put("contaPaiId", salvo.getContaPaiId());
        metadata.put("codigo", salvo.getCodigo());
        metadata.put("nome", salvo.getNome());
        metadata.put("grupoDre", salvo.getGrupoDre().name());
        metadata.put("tipoConta", salvo.getTipoConta().name());
        metadata.put("ativo", salvo.isAtivo());
        financialAuditTrailService.record(
                evento,
                "PlanoContasGerencial",
                salvo.getIdPlanoConta(),
                actorLogin(request),
                anterior,
                novo,
                metadata,
                empresaId,
                filialId);
        auditTrailService.registrarPrivacidade(
                evento,
                "PlanoContasGerencial",
                salvo.getIdPlanoConta(),
                "Financeiro",
                anterior,
                novo,
                request,
                justificativa);
    }

    private PlanoContasResponseDTO toResponse(PlanoContasGerencial c, List<PlanoContasResponseDTO> filhas) {
        return new PlanoContasResponseDTO(
                c.getIdPlanoConta(),
                c.getContaPaiId(),
                c.getCodigo(),
                c.getNome(),
                c.getDescricao(),
                c.getTipoConta(),
                c.getNatureza(),
                c.getClassificacaoCusto(),
                c.getGrupoDre(),
                c.getOrdemExibicao(),
                c.isAceitaLancamento(),
                c.isSistemaPadrao(),
                c.isAtivo(),
                c.getCriadoEm(),
                c.getAtualizadoEm(),
                filhas);
    }

    private void requireTenant(Long empresaId, Long filialId) {
        if (empresaId == null || filialId == null) {
            throw new IllegalArgumentException("Empresa e filial ativas sao obrigatorias.");
        }
    }

    private String normalizeRequired(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String actorLogin(HttpServletRequest request) {
        return privacyGuardService.currentUserLogin(request);
    }

    private String resumo(PlanoContasGerencial c) {
        return "codigo=" + c.getCodigo()
                + ";nome=" + c.getNome()
                + ";grupo=" + c.getGrupoDre()
                + ";ativo=" + c.isAtivo();
    }

    private record SeedConta(
            String codigo,
            String codigoPai,
            String nome,
            String descricao,
            TipoContaGerencial tipoConta,
            NaturezaContaGerencial natureza,
            ClassificacaoCusto classificacaoCusto,
            GrupoDreGerencial grupoDre,
            int ordemExibicao,
            boolean aceitaLancamento) {
    }
}
