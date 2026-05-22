# Paridade Funcional de Domínio

Este mapa garante que as classes do backend tenham presença operacional na UI.

| Grupo | Subcategoria | Classe principal | Endpoint |
| --- | --- | --- | --- |
| Comercial | Clientes CRM | Cliente.java | `/api/clientes` |
| Comercial | Endereços | Endereco.java | `/api/enderecos` |
| Comercial | Vendas | Venda.java | `/api/vendas` |
| Comercial | Itens de Venda | ProdutoVenda.java + ProdutoVendaId.java | `/api/itensvenda` |
| Estoque e Fiscal | Produtos | Produto.java | `/api/produtos` |
| Estoque e Fiscal | Categorias | Categoria.java | `/api/categorias` |
| Estoque e Fiscal | NF de Entrada | NotaFiscalEntrada.java | `/api/nfs/entrada` |
| Estoque e Fiscal | Itens da NF | ItemNotaFiscal.java | `/api/nfs/entrada` |
| Finanças | Gestão de Despesas | Despesa.java | `/api/despesas` |
| Finanças | Tipos de Despesa | TipoDespesa.java | `/api/despesas/tipos` |
| Finanças | Formas de Pagamento | FormaPagamento.java | `/api/formaspagamento` |
| Finanças | Pagamentos de Venda | VendaPagamento.java + VendaPagamentoId.java | `/api/pagamentosvenda` |
| Finanças | Balanço e Relatórios | RelatorioFinanceiroDTO.java | `/api/relatorios/financeiro` |
| Finanças | AdmCalc Financeiro | AdmCalcController.java | `/api/admcalc/calcular` |
| Administração | Usuários | Usuario.java | `/api/usuarios` |
| Administração | Perfis RBAC | Perfil.java | `/api/perfis` |
| Administração | Menus do Sistema | Menu.java | `/api/menus` |

## Critérios de UI

- Todo grupo principal tem subcategorias aninhadas.
- O módulo `Finanças` é um nó raiz agregador e concentra despesas, pagamentos, balanço e AdmCalc.
- Cada subcategoria mostra classe, endpoint, campos críticos e operações acionáveis.
- Quando a API está indisponível, a UI mantém fluxo local e informa o operador sem tela em branco.
