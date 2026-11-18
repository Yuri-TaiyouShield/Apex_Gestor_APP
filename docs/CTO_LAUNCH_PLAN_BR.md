# Plano CTO de Lancamento Brasil

Data: 2026-05-26

## Posicionamento

O Apex Gestor deve ser posicionado como ERP moderno para lojas de materiais de construcao que precisam vender rapido, comprar melhor, controlar estoque por margem real e operar com fiscal brasileiro sem improviso.

Decisao CTO: **nao lancar nacionalmente antes de fechar os bloqueadores de seguranca, fiscal, frontend versionado, backup, suporte e governanca de release**. A primeira etapa comercial recomendada e piloto pago controlado.

## Concorrentes e Linha de Corte

Os concorrentes citados mostram que o mercado espera profundidade operacional, nao apenas CRUD:

- Santri se posiciona como ERP especializado em MATCON, com compras, vendas, logistica, financeiro, fiscal e contabil integrados, alem de suporte de implantacao e treinamento.
- SoftCosmos divulga recursos amplos em financeiro, materiais, vendas, compras, logistica, gestao de precos, relatorios, integracao web, CNAB, fiscal, tintometria, kits e curva ABC.

Fontes publicas consultadas:

- Santri Sistemas: https://santri.com.br/sistemas/
- ERP para MATCON Santri: https://www.software.santri.com.br/
- SoftCosmos: https://www.softsystem.com/softsystem/produtos/softcosmos.jsp

Conclusao: para ser melhor, o Apex nao deve competir apenas em quantidade de telas. A vantagem deve vir de implantacao mais simples, interface moderna, automacao de decisao, mobile/desktop bem acabados, onboarding guiado, relatorios melhores e confiabilidade operacional.

## Produto Minimo Comercial

Para piloto pago, o Apex precisa entregar com qualidade:

- cadastro de produtos, fornecedores, clientes, usuarios e perfis;
- estoque com custo medio, estoque minimo, curva ABC e historico de movimentacao;
- PDV rapido com venda, desconto controlado, cancelamento e estorno de estoque;
- contas a pagar, contas a receber, caixa e relatorio financeiro reconciliavel;
- compras e entrada de nota com validacao de XML quando aplicavel;
- permissao por perfil, autenticacao obrigatoria, log de auditoria e senha forte;
- backup, restore e suporte remoto;
- treinamento operacional para dono, caixa, vendedor e estoque.

## Diferenciadores para Superar Sistemas Legados

1. **Experiencia de uso moderna**

   Menos telas pesadas, mais atalhos, busca global, PDV teclado-first, leitura de codigo de barras e fluxos mobile.

2. **Inteligencia operacional**

   Sugestao de compra por giro, ruptura, margem, sazonalidade, prazo de fornecedor e estoque por loja.

3. **Preco e margem melhores**

   Simulador de preco por custo, imposto, frete, desconto, comissao, margem alvo e preco praticado.

4. **Fiscal sem susto**

   NFC-e/NF-e por UF, rejeicoes explicadas, contingencia documentada, certificado digital seguro, XML armazenado e SPED/exportacao contabil.

5. **Multiempresa e multifilial**

   Separacao real de dados, lojas, caixas, centros de custo, usuarios, estoque e relatorios consolidados.

6. **Suporte e implantacao como produto**

   Checklist de migracao, importacao de planilhas, treinamento, base de conhecimento e SLA simples.

## Bloqueadores Atuais

| Area | Bloqueio | Decisao |
| --- | --- | --- |
| Frontend | `Apex_Gestor/` aparece nao versionado e parece conter artefatos gerados, nao source limpo | Recuperar ou recriar fonte Angular/Ionic antes de qualquer release |
| Seguranca | API pode operar permissiva em desenvolvimento | Producao deve exigir autenticacao e autorizacao por perfil |
| Banco | `ddl-auto=update` e ausencia de migrations versionadas | Introduzir Flyway ou Liquibase antes de producao |
| Fiscal | Entrada de NF existe, mas emissao/homologacao fiscal nao esta completa | Definir estrategia fiscal por UF e homologar com contador |
| LGPD | Ha dados pessoais, mas falta politica, base legal, logs e direitos do titular | Implementar programa LGPD antes de venda nacional |
| Observabilidade | Health existe, mas faltam logs de negocio, metricas e alertas | Padronizar monitoramento e runbook |
| CI/CD | Workflows originais eram GitHub Pages | CI de backend deve ser obrigatorio e expandido |

## Roadmap Executivo

### 0 a 30 dias - Base Comercial Segura

- Versionar fonte real do frontend ou declarar recriacao controlada.
- Criar migrations de banco e ambiente `prod`.
- Tornar autenticacao obrigatoria em producao.
- Criar usuarios/perfis reais: admin, gestor, caixa, vendedor, estoque, financeiro.
- Cobrir testes de venda, cancelamento, entrada de nota e relatorio financeiro.
- Definir contrato, politica de privacidade, termo de uso e SLA.

### 31 a 60 dias - Piloto Pago

- Rodar em 3 a 5 lojas com acompanhamento diario.
- Criar importacao de produtos/clientes/fornecedores por planilha.
- Implementar backup automatico e restore testado.
- Adicionar dashboard de margem, estoque baixo e vendas por vendedor.
- Medir tempo de venda no PDV, tempo de fechamento de caixa e erros operacionais.
- Documentar treinamento por papel.

### 61 a 90 dias - Release Brasil v1

- Homologar fiscal conforme UFs-alvo.
- Criar integracoes contabeis/exportacoes essenciais.
- Fechar observabilidade, alertas e suporte.
- Implementar licenciamento/assinatura e canais oficiais.
- Criar instalador/atualizador desktop se Electron continuar no escopo.
- Rodar auditoria de seguranca e teste de carga.

## Metricas de Sucesso

- PDV registra venda simples em menos de 20 segundos com operador treinado.
- 99,5% de disponibilidade em horario comercial no piloto.
- Restore de backup validado em menos de 30 minutos.
- Nenhuma operacao fiscal em producao sem XML, protocolo e trilha.
- Estoque reconciliado entre sistema e inventario fisico acima de 98%.
- Suporte responde incidente critico em ate 1 hora no piloto.

## Decisoes de Arquitetura

- Backend Java/Spring Boot permanece como nucleo transacional.
- MySQL 8.4 pode continuar em curto prazo, desde que tenha migrations, backup e tuning.
- Frontend deve ser fonte versionado, nao apenas build.
- Producao deve rodar com perfil `prod`, `ddl-auto=validate`, autenticacao obrigatoria e CORS restrito.
- Fiscal deve ser modulo isolado, testavel e homologavel por UF.
- Toda regra de estoque/financeiro deve ser transacional e auditavel.
