# Checklist de Go-Live Brasil

Use este checklist antes de qualquer piloto pago, implantacao em loja real ou lancamento oficial.

## Produto

- [ ] PDV com venda, desconto, cancelamento, devolucao e multiplas formas de pagamento.
- [ ] Cadastro de produto com codigo de barras, unidade, categoria, fornecedor, custo, preco, margem e estoque minimo.
- [ ] Estoque com historico de movimentacao, inventario, ajuste autorizado e curva ABC.
- [ ] Compras com pedido, recebimento, custo medio, divergencia e entrada por XML quando aplicavel.
- [ ] Financeiro com contas a pagar, contas a receber, caixa, conciliacao e relatorios.
- [ ] Perfis de usuario por funcao: admin, gestor, caixa, vendedor, estoque, financeiro.
- [ ] Dashboard executivo com venda, margem, lucro, estoque baixo, despesas e inadimplencia.
- [ ] Importacao inicial de produtos, clientes, fornecedores e saldos.
- [ ] Fluxo de onboarding de loja documentado.

## Fiscal e Contabil

- [ ] Definir UFs-alvo da primeira versao.
- [ ] Definir estrategia: emissao propria, API fiscal parceira ou integracao com emissor homologado.
- [ ] NFC-e modelo 65 quando exigido para venda a consumidor.
- [ ] NF-e modelo 55 quando houver operacao de mercadoria apropriada.
- [ ] SAT/CF-e somente quando juridicamente aplicavel e atualizado por UF.
- [ ] Certificado digital A1/A3 com armazenamento e renovacao seguros.
- [ ] XML autorizado, cancelado e inutilizado armazenado com protocolo.
- [ ] Contingencia fiscal documentada e testada.
- [ ] SPED/EFD/exportacao contabil definida com contador.
- [ ] Regras fiscais por NCM, CFOP, CST/CSOSN, origem, aliquota e regime tributario.

Fontes oficiais de referencia:

- Portal NF-e: https://www.nfe.fazenda.gov.br/PORTal/listaConteudo.aspx?tipoConteudo=04BIflQt1aY%3D
- Manual NF-e/NFC-e: https://moc.sped.fazenda.pr.gov.br/
- SPED EFD ICMS IPI: https://sped.rfb.gov.br/item/show/274
- LGPD Planalto: https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm

## Seguranca e LGPD

- [ ] Autenticacao obrigatoria em producao.
- [ ] Senhas armazenadas apenas com hash forte.
- [ ] Controle de acesso por perfil em todos os endpoints criticos.
- [ ] Log de auditoria para venda, cancelamento, desconto, estoque, usuario, fiscal e financeiro.
- [ ] Politica de privacidade publicada.
- [ ] Base legal documentada para cliente, fornecedor, funcionario e usuario.
- [ ] Retencao e exclusao de dados definidas.
- [ ] Exportacao de dados do titular quando aplicavel.
- [ ] Criptografia de backups e segredos.
- [ ] Plano de resposta a incidente.

## Engenharia

- [ ] Fonte frontend versionado e builds reproduziveis.
- [ ] `node_modules`, `dist`, `release`, logs e builds gerados fora do git.
- [ ] CI executando testes e auditoria a cada push/PR.
- [ ] Migrations de banco com Flyway ou Liquibase.
- [ ] Perfil `prod` com `ddl-auto=validate`.
- [ ] Observabilidade: health, logs estruturados, metricas e alertas.
- [ ] Testes de dominio: venda, cancelamento, estoque, custo medio, financeiro e permissao.
- [ ] Teste de carga minimo no PDV e relatorios.
- [ ] SAST/dependency scan antes de release.
- [ ] Versionamento semantico e changelog.

## Operacao

- [ ] Backup automatico diario.
- [ ] Restore testado e cronometrado.
- [ ] Ambiente de homologacao separado de producao.
- [ ] Monitoramento de API, banco e disco.
- [ ] Plano de rollback por versao.
- [ ] SLA de suporte definido.
- [ ] Base de conhecimento para implantacao e atendimento.
- [ ] Canal oficial de suporte.
- [ ] Contrato, termo de uso e politica comercial.
- [ ] Treinamento por perfil de usuario.

## Criterio de Aprovacao

O lancamento oficial so deve ser aprovado quando:

- todos os itens criticos de fiscal, seguranca, backup e fonte versionado estiverem concluidos;
- piloto pago estiver estavel por pelo menos 30 dias;
- houver suporte documentado e responsavel;
- houver contador ou especialista fiscal validando as operacoes;
- houver evidencias de teste e rollback.
