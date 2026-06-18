package com.Apex.Apex_Gestordemo;

import static org.assertj.core.api.Assertions.assertThat;

import Controller.*;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;

class LegacyControllerDtoContractTests {
    @Test
    void controllersLegadosNaoRetornamEntidadesJpa() {
        List<Class<?>> controllers = List.of(MenuController.class, PerfilController.class, EnderecoController.class,
                ProdutoVendaController.class, VendaPagamentoController.class, NotaFiscalController.class);
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

    private boolean isEndpoint(Method method) {
        return method.isAnnotationPresent(GetMapping.class) || method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(PutMapping.class) || method.isAnnotationPresent(PatchMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class);
    }
}
