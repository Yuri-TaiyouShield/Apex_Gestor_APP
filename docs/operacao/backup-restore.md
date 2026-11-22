# Backup e restore operacional

Este procedimento usa `mysqldump`/`mysql` e exige que o operador defina as variáveis `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` e `DB_PASSWORD` no ambiente do processo. Nenhuma credencial deve ser gravada no repositório ou passada na linha de comando.

## Objetivos

- RPO alvo: 24 horas, com execução diária e verificação de sucesso.
- RTO alvo: 4 horas para restaurar o último backup íntegro e validar as migrations.
- Retenção padrão local: 14 dias, ajustável no script.
- Armazenamento: copiar o arquivo para uma conta/volume separado, com criptografia em repouso e acesso restrito ao operador de produção. O diretório local de trabalho não é a única cópia.

## Execução

No host que possui o cliente MySQL:

O job deve injetar `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` e `DB_PASSWORD` como variáveis de processo a partir do secret manager. Não use senha literal em script, arquivo `.env`, pipeline ou linha de comando.

```powershell
& .\scripts\backup-mysql.ps1 -OutputDirectory "D:\Backups\apex" -RetentionDays 14
```

O job agendado deve executar o mesmo script diariamente, copiar o arquivo para armazenamento separado e registrar o tamanho, checksum e resultado da execução. Falha de backup deve gerar alerta operacional.

## Restore controlado

1. Selecionar um backup e conferir checksum, data e tamanho.
2. Restaurar em uma base isolada com a mesma versão compatível do MySQL.
3. Executar o restore com confirmação explícita:

```powershell
& .\scripts\restore-mysql.ps1 -SqlFile "D:\Backups\apex\apex-20260917-020000.sql" -AllowDataLoss
```

4. Validar migrations, constraints e contagens mínimas antes de qualquer tráfego:

```powershell
& .\mvnw.cmd -DskipTests flyway:info
& .\mvnw.cmd test -Dtest=FlywayRuntimeTests
```

5. Executar smoke tests de login, consulta tenant-scoped, criação de venda, leitura de estoque e outbox. Conferir que o banco restaurado não expõe dados de outro tenant.
6. Registrar horário de início/fim, backup usado, checksum, versão da aplicação, resultado da validação e decisão de retorno ao tráfego.

## Proteções

- O script de restore exige `-AllowDataLoss` para reduzir execução acidental no banco errado.
- O backup não deve ser enviado para issue, log, artefato público ou diretório versionado.
- A restauração de produção deve ocorrer primeiro em ambiente isolado; a troca de tráfego exige aprovação operacional e plano de rollback.
- Criptografia, cópia geograficamente separada e monitoramento dependem do provedor de infraestrutura e precisam ser confirmados no runbook do ambiente.
