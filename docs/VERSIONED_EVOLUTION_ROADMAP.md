# Roadmap Versionado ERP/PDV

Data de referencia: 2026-06-12

Este documento consolida a diretriz de evolucao do Apex Gestor em branches versionadas. A regra central e simples: cada versao precisa entregar um marco funcional claro, testavel e auditavel, sem atalhos que dificultem multiempresa, seguranca, mobile, API ou integracoes futuras.

## Principios

- Toda funcionalidade nasce API first.
- Toda operacao critica deve passar por permissao e auditoria.
- Nenhum registro critico deve ser removido fisicamente.
- Multiempresa e multifilial devem estar previstos desde a primeira versao.
- Menus auxiliares devem virar secoes internas dos modulos principais sempre que fizer sentido.
- Mudancas em estoque, caixa, venda, fiscal, financeiro e RH exigem testes automatizados e plano de rollback.

## Branches de Produto

| Branch | Marco | Escopo principal | Criterio de saida |
| --- | --- | --- | --- |
| `v1-desktop-core` | Operacao essencial desktop | PDV, caixa, financeiro basico, estoque, inventario, cadastros base, RBAC, auditoria, soft delete, multiempresa preparado | Venda/cancelamento/estoque/financeiro com testes, permissoes criticas, trilha de auditoria e backup documentado |
| `v2-admCalc` | Gestao e calculos empresariais | Tudo da v1 mais AdmCalc, formacao de preco, markup, margem, simulacoes, dashboard gerencial e alertas operacionais | Calculos auditados, indicadores reconciliaveis e alertas acionaveis por permissao |
| `v3-logistica-delivery` | Entrega e logistica | Tudo da v2 mais delivery, entregadores, rotas, regioes, frete, status em tempo real e comprovante digital | Fluxo de pedido completo, historico de entrega, evidencia digital e rastreabilidade |

## Roadmap Futuro

| Branch | Objetivo |
| --- | --- |
| `v4-mobile` | Apps Android/iOS, operacao offline e sincronizacao automatica |
| `v5-business-intelligence` | Data warehouse, dashboards avancados, previsao de vendas e IA para reposicao |
| `v6-ecosystem` | Marketplace, bancos, fiscal completo, e-commerce integrado, CRM avancado e automacao de marketing |

## v1-desktop-core

Modulos obrigatorios:

- PDV e caixa.
- Financeiro basico.
- Estoque e controle de inventario.
- Cadastros base: clientes, produtos, fornecedores, funcionarios, formas de pagamento, categorias e grupos.
- RBAC por papel.
- Auditoria completa.
- Soft delete.
- Multiempresa preparado.
- Notificacoes operacionais.
- Backup e recuperacao.

Controles de estoque:

- Entrada de mercadorias.
- Saida de mercadorias.
- Ajustes.
- Transferencias internas.
- Inventario rotativo.
- Contagem de estoque.
- Historico de movimentacoes.
- Rastreabilidade de usuario, data, IP e dispositivo.

## v2-admCalc

Pre-requisito: `v1-desktop-core` aprovada.

Recursos:

- Formacao de preco de venda.
- Margem de lucro e markup.
- Simulacao tributaria.
- Simulacao de descontos.
- Simulacao de comissoes.
- Simulacao de metas.
- Simulacao de parcelamentos.

Indicadores:

- Faturamento.
- Ticket medio.
- Produtos mais vendidos.
- Curva ABC.
- Lucratividade.
- Giro de estoque.
- Fluxo de caixa projetado.

Alertas:

- Estoque minimo.
- Estoque parado.
- Produtos sem venda.
- Clientes inativos.
- Inadimplencia.

## v3-logistica-delivery

Pre-requisito: `v2-admCalc` aprovada.

Fluxo operacional:

```text
Recebido -> Em Separacao -> Producao -> Pronto -> Saiu para Entrega -> Entregue
```

Recursos:

- Controle de entregadores.
- Rastreamento de pedidos.
- Rotas.
- Status em tempo real.
- Calculo automatico de frete.
- Regioes de entrega.
- Taxa por bairro.
- Integracao com mapas.
- Comprovante digital.
- Assinatura do cliente.
- Foto da entrega.

## Simplificacao da Interface

Clientes CRM:

- Enderecos deixam de ser modulo isolado.
- O cadastro do cliente deve concentrar dados gerais, contatos, enderecos, historico de compras, financeiro e observacoes.

Vendas:

- Itens da venda deixam de ser modulo isolado.
- A tela da venda deve concentrar dados da venda, produtos, pagamentos, historico e log de alteracoes.

Cadastros auxiliares:

- Formas de pagamento, categorias, grupos e tipos devem ser acessados por configuracoes ou abas internas.
- O menu principal deve priorizar operacao diaria: Dashboard, PDV/Caixa, Produtos/Estoque, Clientes, Fornecedores, Financeiro, Funcionarios/Ponto e Configuracoes.

## Controle de Ponto

Eventos:

- Entrada.
- Saida para almoco.
- Retorno do almoco.
- Saida final.

Processamentos esperados:

- Horas trabalhadas.
- Horas extras.
- Banco de horas.
- Atrasos.
- Faltas.
- Adicional noturno.
- Jornada semanal.
- Jornada mensal.

Painel do gestor:

- Aprovacao de horas extras.
- Correcao de registros.
- Justificativas.
- Relatorios.

Integracoes futuras:

- Biometria.
- Reconhecimento facial.
- Geolocalizacao.
- QR Code.
- Aplicativo movel.

## Politica de Branch

1. A branch de uma versao nasce da ultima versao estavel anterior.
2. Correcoes criticas devem voltar para a menor versao afetada.
3. Migrations devem ser versionadas e reversiveis quando possivel.
4. Cada PR deve declarar impacto em banco, API, permissao, auditoria e experiencia do usuario.
5. Nenhuma branch sobe para release sem testes, auditoria local, changelog e plano de rollback.

## Referencias do Projeto

- Modelo de banco anexado: `C:\Users\ancan\Downloads\Apex_BD.pdf`.
- Diagrama de caso de uso anexado: `C:\Users\ancan\Downloads\Caso uso Geral.png`.
- Diagramas detalhados de manter menu, perfil, venda, cliente, fornecedor, usuario, produto e relatorios anexados em `C:\Users\ancan\Downloads`.
- Rastreabilidade consolidada em `docs/USE_CASE_TRACEABILITY.md`.
- Entidades JPA atuais em `Apex-Gestordemo/src/main/java/Model`.
- Front Angular/Ionic atual em `Apex_Gestor/src/app`.
