# Matriz RBAC, Auditoria e Operacoes Criticas

Data de referencia: 2026-06-12

Esta matriz define o contrato minimo de seguranca para a evolucao do Apex Gestor. Ela deve guiar backend, frontend, testes automatizados, treinamento e documentacao operacional.

## Hierarquia de Papeis

| Papel | Nivel | Responsabilidade |
| --- | --- | --- |
| Administrador Master | 100 | Dono tecnico/comercial da conta, multiempresa, licenca, seguranca e auditoria |
| Administrador | 90 | Parametrizacao geral, usuarios, permissoes e operacao administrativa |
| Gerente | 80 | Operacao da loja, aprovacao de desconto, caixa, estoque e relatorios |
| Supervisor | 70 | Supervisao diaria, ajustes autorizados e suporte a operadores |
| Financeiro | 60 | Contas, caixa, pagamentos, recebimentos e relatorios financeiros |
| RH | 55 | Funcionarios, ponto, jornada e documentos de RH |
| Estoquista | 50 | Entrada, saida, ajuste, transferencia e inventario |
| Vendedor | 40 | Venda assistida, clientes e consulta de produtos |
| Operador de Caixa | 30 | PDV, recebimento, fechamento e operacoes de caixa |

## Permissoes Criticas

| Codigo | Acao | Papeis minimos |
| --- | --- | --- |
| `sale.create` | Criar venda | Operador de Caixa, Vendedor, Supervisor, Gerente, Administrador |
| `sale.cancel` | Cancelar venda | Supervisor, Gerente, Administrador |
| `sale.delete` | Excluir venda logica | Administrador Master, Administrador |
| `discount.approve` | Aprovar desconto acima da regra | Supervisor, Gerente, Administrador |
| `invoice.cancel` | Cancelar nota | Gerente, Administrador |
| `stock.change` | Alterar estoque | Estoquista, Supervisor, Gerente, Administrador |
| `stock.transfer` | Transferencia interna | Estoquista, Supervisor, Gerente, Administrador |
| `stock.inventory.approve` | Aprovar inventario | Supervisor, Gerente, Administrador |
| `finance.change` | Alteracao financeira | Financeiro, Gerente, Administrador |
| `cash.close` | Fechamento de caixa | Operador de Caixa, Supervisor, Gerente |
| `user.manage` | Criar/alterar usuario | Administrador Master, Administrador, Gerente |
| `role.manage` | Alterar perfis e permissoes | Administrador Master, Administrador |
| `timeclock.adjust` | Corrigir ponto | RH, Supervisor, Gerente, Administrador |
| `delivery.dispatch` | Despachar entrega | Supervisor, Gerente, Administrador |

## Auditoria Obrigatoria

Toda operacao critica deve registrar:

- Usuario.
- Data e hora.
- Acao.
- Entidade.
- ID da entidade.
- Valor anterior.
- Valor novo.
- IP.
- Dispositivo.
- Empresa.
- Filial.
- Trace ID quando existir.

Eventos minimos:

- Login e falha de login.
- Criacao, alteracao e desativacao de usuarios.
- Criacao, cancelamento e exclusao logica de vendas.
- Aprovacao de desconto.
- Entrada, saida, ajuste, transferencia e inventario de estoque.
- Alteracoes financeiras.
- Cancelamento fiscal.
- Correcao de ponto.
- Backup, restauracao e falha de restauracao.

## Soft Delete

Registros criticos nao devem ser removidos fisicamente:

- Clientes.
- Produtos.
- Fornecedores.
- Funcionarios/usuarios.
- Vendas.
- Financeiro.
- Estoque.
- Fiscal.
- Ponto.

Campos padrao:

- `deleted_at`
- `deleted_by`

Enquanto a base legada ainda usa `status`, o contrato de evolucao e manter compatibilidade e acrescentar `deleted_at`/`deleted_by` nas novas estruturas.

## Multiempresa

Toda nova tabela operacional deve prever:

- `empresa_id`
- `filial_id`

Regras:

- Usuario pode ter permissoes por unidade.
- Relatorios gerenciais devem separar filial e consolidar empresa.
- Estoque deve ser por filial.
- Caixa deve ser por filial e operador.
- Auditoria sempre deve apontar empresa e filial.

## Testes Obrigatorios

Cada acao critica precisa ter pelo menos:

- Teste de permissao autorizada.
- Teste de permissao negada.
- Teste de auditoria gerada.
- Teste de impacto em estoque/financeiro quando aplicavel.
- Teste de soft delete quando houver desativacao/cancelamento.

## Regra de Produto

Se uma funcionalidade nao puder ser auditada, revertida ou protegida por permissao, ela nao deve entrar em release de producao.
