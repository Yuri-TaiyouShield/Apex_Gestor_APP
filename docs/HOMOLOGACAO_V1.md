# Homologacao operacional v1

## Massa controlada

A massa e criada somente com o perfil Spring `local` e a propriedade `apex.seed.homologacao=true`. Ela e idempotente, nao faz parte das migrations estruturais e usa apenas documentos marcadamente ficticios, e-mails `.invalid` e enderecos de homologacao.

Para iniciar ou recriar a massa H2 em memoria:

```powershell
cd Apex-Gestordemo
cmd /c mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

Reiniciar o processo recria o H2 e reaplica Flyway V2-V5. Para desativar o seed local, defina `APEX_SEED_HOMOLOGACAO=false`. Nunca habilite o perfil `local` em producao.

Credenciais exclusivas de homologacao (senha comum `Apex@2026`):

- `v5_sysadmin`, `v5_dono`, `v5_gerente`, `v5_caixa` e `v5_vendedor`;
- empresa Apex Homologacao, filial `HML-01`;
- clientes, fornecedor, categoria, grupo, meta e conta a pagar ficticios;
- produtos `HML-001` a `HML-003`, com codigos de barras `7890000001001` a `7890000001003`;
- Dinheiro, PIX, Cartao e Credito/Vale; condicoes a vista, parcelada e por forma.

## Testes

```powershell
cd Apex-Gestordemo
cmd /c mvnw.cmd -q test

cd ..\Apex_Gestor
npm test -- --watch=false --browsers=ChromeHeadless
npm run build:web
npm run e2e
```

O teste `MySqlOperationalFlowTests` sobe MySQL 8.4, aplica a variante MySQL das migrations V2-V5 e valida JWT, RBAC, tenant, venda, caixa, pagamento misto, estoque, financeiro, devolucao com credito, auditoria, mascaramento e ausencia de segredos nos DTOs. Docker Desktop deve estar ativo para sua execucao real.

O Playwright executa Vendedor -> Caixa com Dinheiro + PIX, confirma estoque e financeiro, verifica os bloqueios de perfil, exige motivo na devolucao e preserva o badge `Privacidade ativa` sem criar menu LGPD.

## Limites da v1

Credito/vale exige origem em devolucao, cliente e tenant coincidentes e registra geracao/uso. O financeiro cobre contas a receber, contas a pagar e conciliacao inicial; fiscal nacional, backup/restore e operacao comercial continuam bloqueadores. A v2-admCalc permanece fora de escopo.
