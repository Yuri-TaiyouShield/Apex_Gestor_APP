# Como Contribuir

Este projeto precisa ser tratado como produto comercial, nao como apenas um trabalho academico ou demonstracao. Mudancas devem preservar operacao de loja, consistencia de estoque, financeiro, fiscal e privacidade.

## Fluxo de Trabalho

1. Crie uma branch curta e descritiva.
2. Mantenha mudancas pequenas e revisaveis.
3. Atualize documentacao quando alterar comportamento, instalacao, API, banco ou processo operacional.
4. Rode os testes e auditorias antes de abrir PR.
5. Descreva impacto, risco, como testar e plano de rollback.

## Comandos Obrigatorios

```powershell
cd Apex-Gestordemo
cmd /c mvnw.cmd -q test
```

```powershell
powershell -ExecutionPolicy Bypass -File tools\audit\apex-enterprise-audit.ps1 -SkipBuild
```

## Areas de Maior Risco

- Estoque: qualquer venda, cancelamento, entrada fiscal ou ajuste manual precisa manter saldo e trilha de auditoria.
- Fiscal: NF-e, NFC-e, SAT, SPED e regras por UF exigem homologacao e validacao externa.
- Financeiro: contas, pagamentos, comissoes, caixa e relatorios precisam ser reconciliaveis.
- Seguranca: usuarios, senhas, permissoes, logs e dados pessoais devem seguir LGPD.
- Banco: producao nao pode depender de `ddl-auto=update`; usar migrations e backup testado.

## Padroes Tecnicos

- Preferir servicos transacionais para regras de negocio.
- Evitar SQL manual sem necessidade; quando necessario, documentar motivo e teste.
- Nunca versionar senhas, tokens, certificados ou dados reais de clientes.
- Nao commitar `node_modules`, `dist`, `release`, logs ou builds mobile/desktop gerados.
- Usar respostas de erro padronizadas e sem stack trace em producao.

## PR Pronto para Revisao

Inclua no PR:

- resumo da mudanca;
- telas ou exemplos de API quando houver mudanca visivel;
- testes executados;
- riscos conhecidos;
- migracoes ou passos manuais;
- impacto em suporte, treinamento ou documentacao.
