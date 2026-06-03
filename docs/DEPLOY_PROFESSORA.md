# Deploy publico para avaliacao

Este documento centraliza os links e o fluxo profissional para demonstrar o Apex Gestor fora da maquina local.

## Links publicos

- Web publicada no GitHub Pages: https://yuri-taiyoushield.github.io/Apex_Gestor_APP/
- Testes por branch no GitHub Pages: https://yuri-taiyoushield.github.io/Apex_Gestor_APP/branches/
- Releases com Desktop, Mobile Empresa, Mobile Cliente e Web zip: https://github.com/Yuri-TaiyouShield/Apex_Gestor_APP/releases/latest
- Repositorio: https://github.com/Yuri-TaiyouShield/Apex_Gestor_APP

## Status da API remota

O frontend web ja esta publicado. A API Spring Boot e o MySQL precisam ser provisionados em uma conta Render ou Railway porque esses provedores exigem autenticacao do titular da conta para criar servicos, bancos e dominios.

## Caminho Railway recomendado para API + MySQL

1. Abra o projeto no Railway e conecte o repositorio `Yuri-TaiyouShield/Apex_Gestor_APP`.
2. Crie um banco MySQL pelo template oficial do Railway.
3. Crie um servico para a API usando o repositorio e mantenha o arquivo `railway.toml`.
4. Configure as variaveis do servico da API:

```env
DB_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=${{MySQL.MYSQLUSER}}
DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}
JPA_DDL_AUTO=update
APEX_SECURITY_REQUIRE_AUTH=false
APEX_CORS_ALLOWED_ORIGINS=https://yuri-taiyoushield.github.io
```

5. Ative um dominio publico no servico da API.
6. Teste `https://SEU-DOMINIO-RAILWAY/actuator/health`.

## Caminho Render para API

1. No Render, crie um Blueprint apontando para este repositorio.
2. O arquivo `render.yaml` cria o servico Docker `apex-gestor-api`.
3. Informe manualmente `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`, apontando para um MySQL externo.
4. Teste `https://SEU-SERVICO.onrender.com/actuator/health`.

## Como demonstrar para a professora

Envie estes links primeiro:

```text
Apex Gestor Web:
https://yuri-taiyoushield.github.io/Apex_Gestor_APP/

Testes por branch:
https://yuri-taiyoushield.github.io/Apex_Gestor_APP/branches/

Downloads das versoes Desktop, Mobile e Web:
https://github.com/Yuri-TaiyouShield/Apex_Gestor_APP/releases/latest
```

Se a API remota ainda nao estiver provisionada, use o modo demonstracao da tela de login para navegar no sistema com dados mockados.

Quando a API estiver no Railway ou Render, abra o Apex Gestor, va em Configuracoes e informe a URL base da API remota.
