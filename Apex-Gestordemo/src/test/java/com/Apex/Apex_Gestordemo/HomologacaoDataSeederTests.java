package com.Apex.Apex_Gestordemo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import Repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Profile;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class HomologacaoDataSeederTests {

    @Mock private EmpresaRepository empresaRepository;
    @Mock private FilialRepository filialRepository;
    @Mock private PerfilRepository perfilRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private FornecedorRepository fornecedorRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private GrupoProdutoRepository grupoProdutoRepository;
    @Mock private FormaPagamentoRepository formaPagamentoRepository;
    @Mock private CondicaoPagamentoRepository condicaoPagamentoRepository;
    @Mock private ProdutoRepository produtoRepository;
    @Mock private MetaVendedorRepository metaVendedorRepository;
    @Mock private ContaPagarRepository contaPagarRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @Test
    void perfilApenasLocalEDevEFalhaRapidoEmProducao() {
        Profile profileAnnotation = HomologacaoDataSeeder.class.getAnnotation(Profile.class);
        assertThat(profileAnnotation).isNotNull();
        assertThat(profileAnnotation.value()).containsExactlyInAnyOrder("local", "dev");

        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");
        HomologacaoDataSeeder seeder = new HomologacaoDataSeeder(
                empresaRepository,
                filialRepository,
                perfilRepository,
                usuarioRepository,
                clienteRepository,
                fornecedorRepository,
                categoriaRepository,
                grupoProdutoRepository,
                formaPagamentoRepository,
                condicaoPagamentoRepository,
                produtoRepository,
                metaVendedorRepository,
                contaPagarRepository,
                passwordEncoder,
                prodEnv);

        assertThatThrownBy(() -> seeder.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("prod");
    }
}
