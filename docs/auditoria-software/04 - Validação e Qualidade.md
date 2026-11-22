# 04 — Validação e qualidade

## Comandos executados

| Comando | Resultado | Evidência/limite |
|---|---|---|
| tools/audit/apex-enterprise-audit.ps1 -SkipBuild | passou com aviso | secret scan passou; encontrou 7 ocorrências de @Query |
| scripts/audit-spring-data-jpa.cjs | falhou | exit 1; listou 7 @Query em CategoriaRepository, CreditoClienteRepository, FormaPagamentoRepository e GrupoProdutoRepository |
| scripts/audit-no-secrets.cjs | passou | nenhum segredo de alta confiança detectado |
| mvnw.cmd -q test | não executado | JAVA_HOME aponta para JDK inexistente; nenhum java/JDK válido disponível |
| npm run lint | falhou | 6 no-unused-vars em apex-route-reuse.strategy.ts e cashier.page.ts |
| npm run test -- --watch=false --browsers=ChromeHeadless | passou | 11/11 testes frontend |
| npm audit --omit=dev --audit-level=high | falhou | 8 vulnerabilidades: 5 moderadas e 3 altas; correção sugerida exigiria upgrade breaking para Angular 22.1.7 |
| npm run build:web:client:secure | passou | build web concluído; manifesto de integridade com 149 arquivos |
| git diff --check | passou | sem erro de whitespace; alterações preexistentes em dois workflows preservadas |
| docker version | não disponível | daemon Docker Desktop não respondeu; Testcontainers/E2E MySQL não foram executados |

## Cobertura funcional observada

Existem 18 arquivos de teste backend, incluindo testes de serviço, segurança, Flyway, H2, Spring Boot e um fluxo MySQL/Testcontainers desabilitado quando não há Docker. O frontend possui testes unitários executáveis e uma suíte Playwright serial, mas a configuração não sobe automaticamente frontend/backend: o E2E depende de serviços externos.

Não foi produzido número de cobertura nesta auditoria: não há JaCoCo/PIT detectado no pom e o comando de cobertura não foi executado.

## Lacunas de teste prioritárias

- 401/403 reais por endpoint e papel, incluindo método financeiro e privacidade.
- tentativa cross-tenant com IDs válidos de outro tenant;
- login sem credencial demo e refresh inválido/revogado/reutilizado;
- concorrência de duas vendas no último item;
- duas aberturas de caixa e incrementos simultâneos;
- retry após timeout de finalização e entrada de NF duplicada;
- email/broker indisponível e reprocessamento de outbox;
- banco vazio, upgrade de banco existente e rollback de migration;
- volume de listagens, memória, P95/P99 e queries N+1;
- restore de backup e operação offline sem dados mock.

## Critério de confiança

O frontend tem evidência positiva de compilação segura e testes unitários. O backend não tem resultado de execução local nesta auditoria por falta de JDK; portanto, alegações documentais de que todos os testes passam não foram reproduzidas no ambiente disponível. A validação MySQL e E2E permanece não verificável até Docker, JDK e serviços serem disponibilizados.
