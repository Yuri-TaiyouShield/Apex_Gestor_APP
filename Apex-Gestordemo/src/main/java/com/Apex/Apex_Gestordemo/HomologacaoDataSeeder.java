package com.Apex.Apex_Gestordemo;

import Model.*;
import Repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"local", "dev"})
@ConditionalOnProperty(name = "apex.seed.homologacao", havingValue = "true")
public class HomologacaoDataSeeder implements ApplicationRunner {
    public static final String EMPRESA_CNPJ = "99999999000199";
    public static final String FILIAL_CODIGO = "HML-01";

    private final EmpresaRepository empresaRepository;
    private final FilialRepository filialRepository;
    private final PerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final FornecedorRepository fornecedorRepository;
    private final CategoriaRepository categoriaRepository;
    private final GrupoProdutoRepository grupoProdutoRepository;
    private final FormaPagamentoRepository formaPagamentoRepository;
    private final CondicaoPagamentoRepository condicaoPagamentoRepository;
    private final ProdutoRepository produtoRepository;
    private final MetaVendedorRepository metaVendedorRepository;
    private final ContaPagarRepository contaPagarRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Autowired(required = false)
    private EmpresaPlanoLicencaRepository empresaPlanoLicencaRepository;

    public HomologacaoDataSeeder(EmpresaRepository empresaRepository, FilialRepository filialRepository,
            PerfilRepository perfilRepository, UsuarioRepository usuarioRepository,
            ClienteRepository clienteRepository, FornecedorRepository fornecedorRepository,
            CategoriaRepository categoriaRepository, GrupoProdutoRepository grupoProdutoRepository,
            FormaPagamentoRepository formaPagamentoRepository,
            CondicaoPagamentoRepository condicaoPagamentoRepository, ProdutoRepository produtoRepository,
            MetaVendedorRepository metaVendedorRepository, ContaPagarRepository contaPagarRepository,
            PasswordEncoder passwordEncoder, Environment environment) {
        this.empresaRepository = empresaRepository;
        this.filialRepository = filialRepository;
        this.perfilRepository = perfilRepository;
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.categoriaRepository = categoriaRepository;
        this.grupoProdutoRepository = grupoProdutoRepository;
        this.formaPagamentoRepository = formaPagamentoRepository;
        this.condicaoPagamentoRepository = condicaoPagamentoRepository;
        this.produtoRepository = produtoRepository;
        this.metaVendedorRepository = metaVendedorRepository;
        this.contaPagarRepository = contaPagarRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException("HomologacaoDataSeeder nao pode ser executado com perfil prod ativo.");
        }
        Empresa empresa = empresaRepository.findByCnpj(EMPRESA_CNPJ).orElseGet(this::criarEmpresa);
        Filial filial = filialRepository.findByEmpresaIdEmpresaAndCodigo(empresa.getIdEmpresa(), FILIAL_CODIGO)
                .orElseGet(() -> criarFilial(empresa));

        Perfil sysadmin = perfil("SYSADMIN");
        Perfil dono = perfil("DONO_GERENTE");
        Perfil gerente = perfil("GERENTE");
        Perfil caixa = perfil("CAIXA");
        Perfil vendedor = perfil("VENDEDOR");
        usuario("v5_sysadmin", "Sysadmin Homologacao", sysadmin, empresa, filial);
        usuario("v5_dono", "Dono Homologacao", dono, empresa, filial);
        usuario("v5_gerente", "Gerente Homologacao", gerente, empresa, filial);
        usuario("v5_caixa", "Caixa Homologacao", caixa, empresa, filial);
        Usuario vendedorHml = usuario("v5_vendedor", "Vendedor Homologacao", vendedor, empresa, filial);

        Categoria categoria = categoria(empresa, filial);
        grupo(empresa, filial);
        Fornecedor fornecedor = fornecedor(empresa, filial);
        clientes(empresa, filial);
        formasPagamento(empresa, filial);
        condicoesPagamento(empresa, filial);
        produtos(empresa, filial, categoria, fornecedor);
        meta(vendedorHml, empresa, filial);
        contaPagar(empresa, filial, fornecedor, categoria);
        garantirPlanoLicencaHomologacao(empresa);
    }

    private void garantirPlanoLicencaHomologacao(Empresa empresa) {
        if (empresaPlanoLicencaRepository == null || empresa == null || empresa.getIdEmpresa() == null) {
            return;
        }
        if (empresaPlanoLicencaRepository.findByEmpresaId(empresa.getIdEmpresa()).isPresent()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        EmpresaPlanoLicenca plano = new EmpresaPlanoLicenca();
        plano.setEmpresaId(empresa.getIdEmpresa());
        plano.setTenantCode("hml-" + empresa.getIdEmpresa());
        plano.setTenantName(empresa.getNomeFantasia() != null ? empresa.getNomeFantasia() : empresa.getRazaoSocial());
        plano.setSubscriptionTier("PREMIUM");
        plano.setFeaturesHabilitadas("BASIC_PRICING,FINANCIAL_CORE,ADVANCED_FINANCE,HIERARCHICAL_REPORTS");
        plano.setStatus("ATIVO");
        plano.setValidoAte(now.plusYears(1));
        plano.setUltimaValidacaoEm(now);
        plano.setGracePeriodHoras(72);
        plano.setContingenciaAte(now.plusHours(72));
        plano.setAtualizadoPor("HOMOLOGACAO_SEEDER");
        empresaPlanoLicencaRepository.save(plano);
    }

    private Empresa criarEmpresa() {
        Empresa e = new Empresa();
        e.setRazaoSocial("Apex Homologacao Ltda - Dados Ficticios");
        e.setNomeFantasia("Apex Loja HML");
        e.setCnpj(EMPRESA_CNPJ);
        e.setStatus(1);
        return empresaRepository.save(e);
    }

    private Filial criarFilial(Empresa empresa) {
        Filial f = new Filial();
        f.setEmpresa(empresa);
        f.setNome("Filial Principal de Homologacao");
        f.setCodigo(FILIAL_CODIGO);
        f.setStatus(1);
        return filialRepository.save(f);
    }

    private Perfil perfil(String nome) {
        return perfilRepository.findByNome(nome).orElseGet(() -> {
            Perfil p = new Perfil(); p.setNome(nome); p.setStatus(1); return perfilRepository.save(p);
        });
    }

    private Usuario usuario(String login, String nome, Perfil perfil, Empresa empresa, Filial filial) {
        return usuarioRepository.findByLogin(login).orElseGet(() -> {
            Usuario u = new Usuario();
            u.setLogin(login); u.setNome(nome); u.setSenha(passwordEncoder.encode("Apex@2026"));
            u.setDataNascimento(LocalDate.of(1990, 1, 1)); u.setStatus(1);
            u.setEmpresaId(empresa.getIdEmpresa()); u.setFilialId(filial.getIdFilial()); u.setPerfil(perfil);
            return usuarioRepository.save(u);
        });
    }

    private Categoria categoria(Empresa empresa, Filial filial) {
        return categoriaRepository.findVisibleToTenant(empresa.getIdEmpresa(), filial.getIdFilial()).stream()
                .filter(c -> "Materiais HML".equals(c.getDescricao())).findFirst().orElseGet(() -> {
            Categoria c = new Categoria(); c.setDescricao("Materiais HML"); c.setStatus(1);
            c.setEmpresaId(empresa.getIdEmpresa()); c.setFilialId(filial.getIdFilial());
            return categoriaRepository.save(c);
        });
    }

    private void grupo(Empresa empresa, Filial filial) {
        boolean existe = grupoProdutoRepository.findVisibleToTenant(empresa.getIdEmpresa(), filial.getIdFilial())
                .stream().anyMatch(g -> "HOMOLOGACAO".equals(g.getNome()));
        if (!existe) {
            GrupoProduto g = new GrupoProduto(); g.setNome("HOMOLOGACAO");
            g.setDescricao("Grupo exclusivo para testes locais"); g.setStatus(1);
            g.setEmpresaId(empresa.getIdEmpresa()); g.setFilialId(filial.getIdFilial());
            grupoProdutoRepository.save(g);
        }
    }

    private Fornecedor fornecedor(Empresa empresa, Filial filial) {
        return fornecedorRepository.findByCnpj("88888888000188").orElseGet(() -> {
            Endereco end = endereco("Rua Ficticia do Fornecedor", "200", empresa, filial);
            Fornecedor f = new Fornecedor(); f.setRazaoSocial("Fornecedor HML Dados Ficticios Ltda");
            f.setNomeFantasia("Fornecedor HML"); f.setCnpj("88888888000188"); f.setInscricaoEstadual("ISENTO-HML");
            f.setEmail("fornecedor.hml@example.invalid"); f.setTelefone("11999990000"); f.setCelular("11999990001");
            f.setSituacaoFiscal("HOMOLOGACAO"); f.setCondicoesPagamento("Somente dados ficticios");
            f.setObservacoes("NAO USAR EM PRODUCAO"); f.setDataCadastro(LocalDateTime.now()); f.setStatus(1);
            f.setEmpresaId(empresa.getIdEmpresa()); f.setFilialId(filial.getIdFilial()); f.setEndereco(end);
            return fornecedorRepository.save(f);
        });
    }

    private void clientes(Empresa empresa, Filial filial) {
        cliente("99999999999", "Cliente Balcao HML", "11999991000", empresa, filial);
        cliente("88888888888", "Cliente Troca HML", "11999992000", empresa, filial);
    }

    private void cliente(String documento, String nome, String telefone, Empresa empresa, Filial filial) {
        if (clienteRepository.findByCpfCnpj(documento).isPresent()) return;
        Cliente c = new Cliente(); c.setNomeRazao(nome); c.setNomeFantasia(nome); c.setTelefone(telefone);
        c.setCelular(telefone); c.setEmail(nome.toLowerCase().replace(' ', '.') + "@example.invalid");
        c.setTipoDocumento(1); c.setCpfCnpj(documento); c.setRgIe("FICTICIO");
        c.setSituacaoFiscal("HOMOLOGACAO"); c.setObservacoes("DADO FICTICIO - NAO USAR EM PRODUCAO");
        c.setDataCadastro(LocalDateTime.now()); c.setStatus(1); c.setEmpresaId(empresa.getIdEmpresa());
        c.setFilialId(filial.getIdFilial());
        c.setEnderecos(Set.of(endereco("Rua Ficticia do Cliente", "100", empresa, filial)));
        clienteRepository.save(c);
    }

    private Endereco endereco(String logradouro, String numero, Empresa empresa, Filial filial) {
        Endereco e = new Endereco(); e.setEstado("Sao Paulo"); e.setCidade("Cidade HML"); e.setCep("00000-000");
        e.setBairro("Bairro Ficticio"); e.setLogradouro(logradouro); e.setUf("SP"); e.setNumero(numero);
        e.setComplemento("Ambiente de homologacao"); e.setEmpresaId(empresa.getIdEmpresa()); e.setFilialId(filial.getIdFilial());
        return e;
    }

    private void formasPagamento(Empresa empresa, Filial filial) {
        forma("Dinheiro", 1, empresa, filial);
        forma("PIX", 2, empresa, filial);
        forma("Cartao", 3, empresa, filial);
        forma("Credito/Vale", 4, empresa, filial);
    }

    private void forma(String nome, int tipo, Empresa empresa, Filial filial) {
        if (formaPagamentoRepository.findByNomeAndEmpresaIdAndFilialId(nome, empresa.getIdEmpresa(), filial.getIdFilial()).isPresent()) return;
        FormaPagamento f = new FormaPagamento(); f.setNome(nome); f.setDescricao(nome + " - homologacao");
        f.setTipoPagamento(tipo); f.setStatus(1); f.setEmpresaId(empresa.getIdEmpresa()); f.setFilialId(filial.getIdFilial());
        formaPagamentoRepository.save(f);
    }

    private void condicoesPagamento(Empresa empresa, Filial filial) {
        condicao("A vista", "A_VISTA", 1, 0, empresa, filial);
        condicao("Parcelado 2x", "PARCELADO", 2, 30, empresa, filial);
        condicao("Cartao", "CARTAO", 1, 30, empresa, filial);
        condicao("PIX", "PIX", 1, 0, empresa, filial);
        condicao("Dinheiro", "DINHEIRO", 1, 0, empresa, filial);
        condicao("Credito/Vale", "CREDITO_VALE", 1, 0, empresa, filial);
    }

    private void condicao(String nome, String tipo, int parcelas, int prazo, Empresa empresa, Filial filial) {
        if (condicaoPagamentoRepository.findByNomeAndEmpresaIdAndFilialId(nome, empresa.getIdEmpresa(), filial.getIdFilial()).isPresent()) return;
        CondicaoPagamento c = new CondicaoPagamento(); c.setNome(nome); c.setTipoPagamento(tipo);
        c.setQuantidadeParcelas(parcelas); c.setPrazoDias(prazo); c.setPercentual(new BigDecimal("100.0000"));
        c.setRestricoes("Somente homologacao"); c.setStatus(1); c.setEmpresaId(empresa.getIdEmpresa()); c.setFilialId(filial.getIdFilial());
        condicaoPagamentoRepository.save(c);
    }

    private void produtos(Empresa empresa, Filial filial, Categoria categoria, Fornecedor fornecedor) {
        produto("7890000001001", "HML-001", "Cimento HML 50kg", "32.00", "47.90", 80, empresa, filial, categoria, fornecedor);
        produto("7890000001002", "HML-002", "Argamassa HML 20kg", "12.00", "21.50", 120, empresa, filial, categoria, fornecedor);
        produto("7890000001003", "HML-003", "Tinta HML 18L", "95.00", "149.90", 30, empresa, filial, categoria, fornecedor);
    }

    private void produto(String barras, String interno, String descricao, String custo, String venda, int estoque,
            Empresa empresa, Filial filial, Categoria categoria, Fornecedor fornecedor) {
        if (produtoRepository.findByCodigoBarrasAndEmpresaIdAndFilialId(barras, empresa.getIdEmpresa(), filial.getIdFilial()).isPresent()) return;
        Produto p = new Produto(); p.setCodigoBarras(barras); p.setCodigoInterno(interno); p.setDescricao(descricao);
        p.setCusto(new BigDecimal(custo)); p.setValorVenda(new BigDecimal(venda)); p.setQuantidadeEstoque(estoque);
        p.setEstoqueMinimo(10); p.setStatus(1); p.setMarca("APEX HML"); p.setGrupo("HOMOLOGACAO");
        p.setUnidadeMedida("UN"); p.setEmbalagem("Unidade"); p.setSituacaoFiscal("HOMOLOGACAO");
        p.setNcm("00000000"); p.setCfop("5102"); p.setEmpresaId(empresa.getIdEmpresa()); p.setFilialId(filial.getIdFilial());
        p.setCategoria(categoria); p.setFornecedor(fornecedor); p.setDataAquisicao(LocalDateTime.now());
        produtoRepository.save(p);
    }

    private void meta(Usuario vendedor, Empresa empresa, Filial filial) {
        LocalDate hoje = LocalDate.now();
        if (metaVendedorRepository.findFirstByUsuarioIdAndEmpresaIdAndFilialIdAndPeriodoInicioLessThanEqualAndPeriodoFimGreaterThanEqualOrderByPeriodoInicioDesc(
                vendedor.getIdUsuario(), empresa.getIdEmpresa(), filial.getIdFilial(), hoje, hoje).isPresent()) return;
        YearMonth mes = YearMonth.from(hoje); MetaVendedor m = new MetaVendedor();
        m.setUsuarioId(vendedor.getIdUsuario()); m.setEmpresaId(empresa.getIdEmpresa()); m.setFilialId(filial.getIdFilial());
        m.setPeriodoInicio(mes.atDay(1)); m.setPeriodoFim(mes.atEndOfMonth()); m.setValorMeta(new BigDecimal("25000.00"));
        metaVendedorRepository.save(m);
    }

    private void contaPagar(Empresa empresa, Filial filial, Fornecedor fornecedor, Categoria categoria) {
        String descricao = "Obrigacao inicial HML - dados ficticios";
        if (contaPagarRepository.existsByDescricaoAndEmpresaIdAndFilialId(descricao, empresa.getIdEmpresa(), filial.getIdFilial())) return;
        ContaPagar c = new ContaPagar(); c.setFornecedorId(fornecedor.getIdFornecedor()); c.setCategoriaId(categoria.getIdCategoria());
        c.setEmpresaId(empresa.getIdEmpresa()); c.setFilialId(filial.getIdFilial()); c.setDescricao(descricao);
        c.setValor(new BigDecimal("350.00")); c.setVencimento(LocalDate.now().plusDays(10));
        c.setStatus(FinanceiroStatus.ABERTO); c.setCriadoEm(LocalDateTime.now()); contaPagarRepository.save(c);
    }
}
