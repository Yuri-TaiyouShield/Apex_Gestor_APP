# Guia de Escalabilidade do Banco

O schema `apex_db` foi revisado para suportar o ecossistema Apex Gestor 3.0: ERP, PDV, e-commerce, mobile staff, mobile cliente, LGPD, licenciamento e auditoria.

## Melhorias aplicadas

- Padronizacao dos nomes de tabelas em `lower_snake_case`, evitando falhas de case-sensitive em MySQL Linux/Docker.
- Tabelas faltantes adicionadas para `despesas`, `tipo_despesa`, `nota_fiscal_entrada`, `item_nota_fiscal`, `refresh_token`, `audit_log`, `consent_audit`, `privacy_request`, `data_erasure_log` e `license_activation`.
- Estoque preparado para escala com `estoque_movimento`, permitindo trilha historica de entradas, saidas, ajustes e origem da movimentacao.
- Entregas preparadas com `entrega`, cobrindo status `PENDENTE`, `EM_ROTA` e `ENTREGUE`.
- Chaves e indices adicionados para filtros frequentes: vendas por periodo/status, despesas por vencimento/status, produtos por status/descricao/codigo de barras, clientes por documento/status/nome e tokens/licencas por hash, app e status.
- Senhas seed do dump agora usam BCrypt em vez de texto puro.
- Fluxo de venda usa lock pessimista nos produtos durante baixa de estoque para reduzir risco de venda concorrente acima do saldo disponivel.

## Pontos de atencao para producao

- Usar backups automatizados e teste periodico de restore.
- Separar usuario de aplicacao do usuario `root`, concedendo apenas permissoes necessarias.
- Habilitar TLS na conexao MySQL quando o banco estiver fora da mesma rede privada.
- Planejar particionamento ou arquivamento para `audit_log`, `estoque_movimento` e `refresh_token` quando o volume crescer.
- Migrar futuras alteracoes com Flyway ou Liquibase antes de operar varias lojas/tenants.
