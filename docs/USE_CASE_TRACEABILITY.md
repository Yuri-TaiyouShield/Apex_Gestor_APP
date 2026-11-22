# Rastreabilidade dos Casos de Uso

Data de referencia: 2026-06-14

Este documento conecta os diagramas anexados aos modulos, rotas e endpoints do Apex Gestor. Ele deve ser atualizado sempre que um caso de uso mudar.

## Anexos de Origem

- `C:\Users\ancan\Downloads\Caso uso Geral.png`
- `C:\Users\ancan\Downloads\Manter Usuario.png`
- `C:\Users\ancan\Downloads\Manter venda.png`
- `C:\Users\ancan\Downloads\ManterCliente.png`
- `C:\Users\ancan\Downloads\Manter Produto.png`
- `C:\Users\ancan\Downloads\Manter Fornecedor.png`
- `C:\Users\ancan\Downloads\Manter perfil.png`
- `C:\Users\ancan\Downloads\Manter Menu.png`
- `C:\Users\ancan\Downloads\Caso de Uso de Relatorio.png`
- `C:\Users\ancan\Downloads\Apex_BD.pdf`

## Mapa Geral

| Caso de uso | Ator minimo | Frontend | API |
| --- | --- | --- | --- |
| Manter usuario | Gestor | `/users` | `/api/usuarios` |
| Efetuar login/logoff | Atendente | `/login` | autenticao atual/futura |
| Manter venda | Atendente/Gestor | `/pos` | `/api/vendas` |
| Manter cliente | Atendente/Gestor | `/clients` | `/api/clientes` |
| Manter produto | Atendente/Gestor | `/products` | `/api/produtos` |
| Manter fornecedor | Gestor | `/suppliers` | `/api/fornecedores` |
| Manter perfil | Gestor/Admin | `/profiles` via Configuracoes | `/api/perfis` |
| Vincular/desvincular menu | Gestor/Admin | `/profiles` via Configuracoes | `/api/perfis/{perfilId}/menus/{menuId}` |
| Manter menu | Administrador | `/menus` via Configuracoes | `/api/menus` |
| Ver relatorio clientes | Gestor | `/reports` | `/api/relatorios/clientes` |
| Ver relatorio estoque | Gestor | `/reports` | `/api/relatorios/estoque` |
| Ver relatorio funcionario | Gestor | `/reports` | `/api/relatorios/funcionarios` |
| Ver relatorio vendas | Gestor | `/reports` | `/api/relatorios/vendas` |
| Ver relatorio fornecedores | Gestor | `/reports` | `/api/relatorios/fornecedores` |

## CRUD Padrao

Os casos `Cadastrar`, `Alterar`, `Listar` e `Desativar` seguem o padrao:

- `GET /api/{recurso}` lista.
- `GET /api/{recurso}/{id}` detalha.
- `POST /api/{recurso}` cadastra.
- `PUT /api/{recurso}/{id}` altera.
- `PATCH /api/{recurso}/{id}/desativar` desativa.

## Regras de Acesso

- Atendente pode listar produtos, clientes e realizar venda.
- Gestor herda operacoes do atendente e pode desativar vendas, clientes, produtos e usuarios conforme permissao.
- Administrador herda gestor e administra menus, perfis e parametros.
- Operacoes criticas devem gerar auditoria.

## Simplificacao de Interface

Para cumprir a diretriz de reduzir menus:

- Menu principal fica restrito a operacao diaria: Dashboard, PDV/Caixa, Produtos/Estoque, Clientes, Fornecedores, Compras/NF, Financeiro, Relatorios, Funcionarios/Ponto, Privacidade e Configuracoes.
- Perfis, menus, formas de pagamento, categorias, grupos e parametros financeiros ficam dentro de Configuracoes.
- Itens de venda pertencem a tela de venda/PDV.
- Enderecos pertencem ao cadastro de cliente/fornecedor.

## Pendencias Controladas

- Implementar tela especifica de vinculo visual de menus em perfis; a API ja existe.
- Expandir controle de ponto com eventos de jornada.
- Introduzir migrations formais antes de producao.
- Trocar fallback de status por `deleted_at`/`deleted_by` em todas as entidades criticas.
- Aprofundar relatorios com filtros, exportacao e reconciliacao.
