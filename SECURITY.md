# Apex Gestor - guia tecnico de seguranca

## Backend Spring Boot

- JWT stateless em `/api/auth/login` e `/api/auth/refresh`.
- Access token curto: `JWT_ACCESS_TOKEN_MINUTES`, padrao `15`.
- Refresh token rotativo: salvo somente como hash SHA-256 com pepper.
- RBAC por `roles` no JWT: `ADMIN`, `GERENTE`, `VENDEDOR`.
- BCrypt cost `12` para senhas.
- CORS liberado para `localhost`, `capacitor://localhost`, `ionic://localhost` e `app://localhost`.
- Auditoria de `POST`, `PUT`, `PATCH` e `DELETE` em `/api/**`, com IP e user-agent pseudonimizados.
- LGPD: consentimento, protocolo de solicitacao, exportacao e anonimizacao de cliente.
- Licenciamento: `/api/licenses/validate` valida chave, dispositivo, app liberado e limite de ativacoes, salvando apenas hashes.
- Enforcement de propriedade intelectual: chamadas protegidas em `/api/**` exigem `X-Apex-License-Key`, `X-Apex-Device-Fingerprint` e `X-Apex-App-Id`; chaves podem liberar `desktop`, `mobile-staff`, `mobile-client`, `web-client` ou `all`.

Variaveis recomendadas em producao:

```bash
JWT_SECRET="troque-por-um-segredo-longo-com-mais-de-32-bytes"
JWT_ISSUER="apex-gestor"
JWT_ACCESS_TOKEN_MINUTES=15
JWT_REFRESH_TOKEN_DAYS=30
REQUIRE_HTTPS=true
PRIVACY_HASH_PEPPER="pepper-unico-do-ambiente"
APEX_LICENSE_KEYS="CHAVE-CLIENTE-001,CHAVE-CLIENTE-002"
APEX_LICENSE_CATALOG="CHAVE-GERAL|all|3|365;CHAVE-DESKTOP|desktop|1|365;CHAVE-DUO|desktop+mobile-staff|2|365;CHAVE-TRIO|desktop+mobile-staff+web-client|3|365"
APEX_LICENSE_MAX_DEVICES=3
APEX_LICENSE_VALIDITY_DAYS=30
```

TLS 1.3 deve ser terminado no proxy reverso ou load balancer. Se o Spring Boot expuser HTTPS diretamente, configure keystore e mantenha `REQUIRE_HTTPS=true`.

## Frontend Angular/Ionic

- `AuthService` armazena access token em `sessionStorage`.
- Refresh token usa storage seguro no Electron e fallback web/mobile quando a ponte nativa nao existe.
- `authInterceptor` envia `Authorization: Bearer` e os headers de licenca apenas para chamadas `/api`.
- A tela `Privacidade LGPD` permite solicitar exportacao/exclusao e mostra mascaramento em suporte.
- A tela `Configuracoes` permite definir URL da API e validar licenca por app/dispositivo.

Build seguro:

```bash
npm run build:web:secure
npm run build:electron:secure
```

O build seguro minifica, ofusca o bundle web e gera `integrity-manifest.json`.

## Electron

- `nodeIntegration=false`.
- `contextIsolation=true`.
- `sandbox=true`.
- CSP aplicada no processo principal.
- Navegacao externa bloqueada e aberta no browser do sistema.
- `safeStorage` protege dados locais sensiveis quando disponivel pelo SO.
- O pacote verifica hash dos arquivos listados no manifesto de integridade.

## Pendencias normais de producao

- Assinar instaladores Windows/macOS/Linux com certificado da empresa.
- Trocar as chaves demo `APEX-DEMO-*` por chaves emitidas pelo servidor comercial.
- Habilitar TLS real e renovar certificados automaticamente.
- Revisar retencao de auditoria e prazo legal dos registros LGPD.
- Trocar fallback de storage mobile por plugin nativo seguro antes da publicacao nas lojas.
