# Guia de Banco de Dados Local

O backend Spring Boot usa MySQL em `localhost:2705` e o schema `apex_db`.

## Subir o MySQL com Docker

Na raiz do projeto:

```powershell
docker compose up -d apex-mysql
docker compose ps
```

O container carrega automaticamente o arquivo `Apex_BD.sql` na primeira inicializacao do volume.

Credenciais locais padrao:

- Host: `localhost`
- Porta externa: `2705`
- Banco: `apex_db`
- Usuario: `root`
- Senha: `apex_dev_2026`

## Rodar o backend

```powershell
cd Apex-Gestordemo
.\mvnw.cmd spring-boot:run
```

Para usar outro MySQL, sobrescreva as variaveis:

```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_NAME="apex_db"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="sua_senha"
.\mvnw.cmd spring-boot:run
```

## Diagnostico rapido

Se aparecer `Connection refused`, o MySQL nao esta aceitando conexoes na porta configurada. Confira:

```powershell
docker compose ps
Test-NetConnection localhost -Port 2705
```

Se aparecer erro de banco inexistente, confirme que o schema esta como `apex_db`. O dump do projeto foi padronizado para esse nome.
