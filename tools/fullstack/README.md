# Apex Gestor - pacote integrado de testes

Este pacote conecta os canais da versão ao mesmo backend e ao mesmo banco:

```text
Web / Desktop / Android
          | HTTP/HTTPS + REST/JSON
          v
Spring Boot API :8080
          | JDBC + Flyway/JPA
          v
MySQL 8.4 :2705
```

## Subir o ambiente local

Requisitos: Docker Desktop e Java não são necessários no host; a API usa a imagem Java 21 do Docker.

```powershell
.\start-stack.ps1
Invoke-RestMethod http://localhost:8080/actuator/health
```

Endereços:

- Web Staff: `http://localhost:4200`
- Web Client: `http://localhost:4201`
- API/health: `http://localhost:8080/actuator/health`
- MySQL para ferramentas locais: `localhost:2705`, banco `apex_db`, usuário `root`, senha `apex_dev_2026`

O frontend não acessa o MySQL diretamente. Toda leitura e gravação passa pela API HTTP; a API acessa o MySQL por JDBC.

## Android Emulator

O APK detecta o emulador Android e usa `http://10.0.2.2:8080`. Com a API em execução:

```powershell
adb install -r ..\mobile\*Mobile-Staff-debug.apk
adb install -r ..\mobile\*Mobile-Client-debug.apk
```

Se for um aparelho físico, configure na tela de configurações a URL `http://IP-DO-PC:8080` e permita a porta 8080 no firewall da rede local.

## Desktop

Abra `..\desktop\...\Apex Gestor.exe`. O desktop usa `http://localhost:8080` por padrão e também aceita `--api-url https://seu-dominio`.

## HTTPS e produção

O pacote local usa HTTP para permitir o teste no emulador. Em produção, coloque TLS em um proxy reverso (Nginx, Caddy, gateway cloud ou Load Balancer), use `https://` na URL da API, defina CORS apenas para os domínios oficiais, troque as senhas e mantenha o banco sem exposição pública.
