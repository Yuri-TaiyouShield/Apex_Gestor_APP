# DevEx Bootstrap Windows

Este guia provisiona a toolchain local do Apex Gestor em Windows sem depender de configuracao manual de PATH.

## O que o script configura

- `DOCKER_HOME` e PATH para Docker Desktop/Engine.
- `JAVA_HOME` para JDK 17+ e `%JAVA_HOME%\bin`.
- `NODE_HOME` para Node.js e npm.
- `NPM_CONFIG_PREFIX` em `C:\ProgramData\npm-global` e PATH para CLIs globais.
- `MAVEN_HOME`, quando Maven global existe, ou validacao do `mvnw.cmd` do backend.
- CLIs globais via npm: Angular CLI, Ionic CLI, Electron, Electron Builder e Electron Forge.

O script e idempotente: pode rodar varias vezes sem duplicar entradas no PATH.

## Auditoria sem alterar a maquina

Abra PowerShell normal ou administrativo na raiz do projeto e execute:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\bootstrap-env.ps1 -CheckOnly
```

## Aplicar configuracao no sistema

Abra o menu iniciar, procure por `PowerShell`, clique com o botao direito e escolha `Executar como administrador`.

Dentro da raiz do projeto:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
.\scripts\bootstrap-env.ps1
```

Depois feche e abra um novo terminal para carregar o PATH de maquina atualizado.

## Pular instalacao global de CLIs

Use este modo quando quiser somente ajustar variaveis e PATH:

```powershell
.\scripts\bootstrap-env.ps1 -SkipNpmGlobalInstall
```

## Health check esperado

Ao final, o script imprime uma tabela com:

- `docker --version`
- `docker compose version`
- `java -version`
- `node --version`
- `npm --version`
- `ng version`
- `ionic --version`
- `electron --version`
- `electron-builder --version`
- `electron-forge --version`
- `mvn -version`

Se algum item aparecer como `MISSING`, instale o runtime indicado, abra um novo terminal administrativo e rode o script novamente.
