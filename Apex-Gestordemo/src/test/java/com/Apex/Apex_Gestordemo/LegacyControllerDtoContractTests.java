package com.Apex.Apex_Gestordemo;

import static org.assertj.core.api.Assertions.assertThat;

import Controller.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;

class LegacyControllerDtoContractTests {
    @Test
    void controllersLegadosNaoRetornamEntidadesJpa() {
        List<Class<?>> controllers = List.of(
                MenuController.class,
                PerfilController.class,
                EnderecoController.class,
                ProdutoVendaController.class,
                VendaPagamentoController.class,
                NotaFiscalController.class,
                DespesaController.class,
                PlanoContasController.class,
                CentroCustoController.class,
                ClassificacaoFinanceiraController.class,
                ConsolidacaoCompetenciaController.class,
                DreGerencialController.class,
                FluxoCaixaController.class,
                IndicadoresAdministrativosController.class,
                LicenseController.class,
                GovernancaSegregacaoController.class);
        for (Class<?> controller : controllers) {
            for (Method method : controller.getDeclaredMethods()) {
                if (isEndpoint(method)) {
                    assertThat(method.getGenericReturnType().getTypeName())
                            .as(controller.getSimpleName() + "." + method.getName())
                            .doesNotContain("Model.");
                }
            }
        }
    }

    @Test
    void controllersFinanceirosNaoAceitamEntidadesJpaEmRequestBody() {
        List<Class<?>> financialControllers = List.of(
                DespesaController.class,
                PlanoContasController.class,
                CentroCustoController.class,
                ClassificacaoFinanceiraController.class,
                ConsolidacaoCompetenciaController.class,
                DreGerencialController.class,
                FluxoCaixaController.class,
                IndicadoresAdministrativosController.class,
                LicenseController.class,
                GovernancaSegregacaoController.class);
        for (Class<?> controller : financialControllers) {
            for (Method method : controller.getDeclaredMethods()) {
                if (isEndpoint(method)) {
                    for (Parameter parameter : method.getParameters()) {
                        if (parameter.isAnnotationPresent(RequestBody.class)) {
                            assertThat(parameter.getParameterizedType().getTypeName())
                                    .as(controller.getSimpleName() + "." + method.getName() + "(" + parameter.getName() + ")")
                                    .doesNotContain("Model.");
                        }
                    }
                }
            }
        }
    }

    private boolean isEndpoint(Method method) {
        return method.isAnnotationPresent(GetMapping.class) || method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(PutMapping.class) || method.isAnnotationPresent(PatchMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class);
    }
}
