# Apex Gestor - roadmap tecnico de producao

Este roadmap usa o `src.zip` como referencia visual: layout limpo, sidebar clara, cards pequenos, KPIs diretos, tabelas escaneaveis e foco em produtividade. A stack final continua Angular/Ionic no frontend, Electron no desktop, Capacitor no mobile e Spring Boot/MySQL no backend.

## 1. Gates de producao

O criterio correto nao deve prometer "zero falhas" de forma abstrata. O gate de producao deve bloquear release com:

- 0 vulnerabilidades criticas ou altas em dependencias de runtime.
- 0 findings criticos/altos em SAST.
- 0 findings criticos/altos em DAST autenticado.
- 100% dos fluxos criticos cobertos por teste automatizado: entrada de NF, baixa de estoque e finalizacao de venda.
- Build web, Electron e Android gerados sem source map de producao.

Comandos recomendados no CI:

```bash
cd Apex_Gestor
npm ci
npm audit --omit=dev --audit-level=high
npm run build:web:secure

cd ../Apex-Gestordemo
./mvnw -q test
./mvnw -q -DskipTests package
./mvnw -q org.owasp:dependency-check-maven:check
```

DAST recomendado: OWASP ZAP Baseline/Full Scan contra `https://staging.apexgestor.com`, com login de teste e rotas `/api/**` autenticadas.

## 2. Paleta Apex acessivel

Tokens de producao para WCAG 2.1 AA/AAA em ambientes com reflexo:

| Uso | Hex | Observacao |
| --- | --- | --- |
| Primaria / azul petroleo profundo | `#0B3A42` | Excelente contraste com branco; usar em botoes primarios, toolbar e marca. |
| Primaria forte | `#062B31` | Estados pressed/hover e fundos compactos do PDV. |
| Secundaria azul executivo | `#1E3A8A` | Inspirada no ZIP; usar em graficos, links e acoes secundarias. |
| Fundo neve | `#F8FAFC` | Base web/mobile/desktop, reduz fadiga visual. |
| Card | `#FFFFFF` | Superficie de tabelas, formularios e KPIs. |
| Borda suave | `#CBD5E1` | Divisores e outlines sem ruído visual. |
| Texto principal | `#020617` | Leitura extrema sob reflexo. |
| Texto secundario | `#475569` | Mantem contraste AA em branco. |
| Sucesso | `#166534` | Status confirmado/entregue. |
| Alerta | `#B45309` | Pendencias financeiras e estoque baixo. |
| Erro | `#B91C1C` | Falhas, cancelamentos e validacoes criticas. |
| Foco acessivel | `#F59E0B` | Outline visivel em teclado e leitores externos. |

Regras de UI:

- Tamanho minimo de toque: 44 x 44 px.
- Raio padrao: 8 px ou menor.
- Texto de dados operacionais: peso 500/600, sem fontes estreitas demais.
- Tabelas e cards devem usar alta densidade, mas com espacamento minimo de 12 px por celula.
- Mobile cliente prioriza busca, filtros, carrinho persistente e checkout curto.
- Mobile staff prioriza KPIs, leitura rapida de estoque e acoes em um toque.

## 3. Seguranca backend Spring Boot

Configuracao minima de producao:

```properties
apex.security.jwt.secret=${JWT_SECRET}
apex.security.jwt.issuer=${JWT_ISSUER:apex-gestor}
apex.security.access-token-minutes=${JWT_ACCESS_TOKEN_MINUTES:15}
apex.security.refresh-token-days=${JWT_REFRESH_TOKEN_DAYS:30}
apex.security.require-https=true
server.forward-headers-strategy=framework
```

Controles obrigatorios:

- JWT curto, refresh token rotativo e armazenado somente como hash com pepper.
- BCrypt cost 12 ou Argon2id quando a base puder migrar.
- RBAC por `ADMIN`, `GERENTE`, `VENDEDOR` e `CLIENTE`.
- Validacao anti-IDOR na camada de servico: usuario so acessa recurso do proprio tenant/loja/cliente.
- JPA/Hibernate com parametros tipados; proibido concatenar SQL com entrada do usuario.
- Auditoria de `POST`, `PUT`, `PATCH`, `DELETE`, login, consentimento, exportacao e exclusao LGPD.
- TLS 1.3 no proxy reverso; HSTS ativo; cookies `Secure`, `HttpOnly`, `SameSite=Strict` quando usados.

## 4. Electron hardening

Configuracao obrigatoria:

```js
webPreferences: {
  preload: path.join(__dirname, 'preload.cjs'),
  contextIsolation: true,
  nodeIntegration: false,
  sandbox: true,
  webSecurity: true,
  allowRunningInsecureContent: false
}
```

Tambem manter:

- CSP estrita sem `unsafe-eval`.
- `setPermissionRequestHandler` negando permissoes por padrao.
- Bloqueio de navegacao para fora de `app://localhost` em producao.
- `safeStorage` para segredos locais e refresh token.
- `asar: true`, manifesto SHA-256, assinatura de instalador e verificacao de integridade.
- Validacao de licenca por servidor com fingerprint do dispositivo, pacote de apps (`desktop`, `mobile-staff`, `mobile-client`, `web-client`) e limite de ativacoes.

## 5. Ofuscacao e propriedade intelectual

Pipeline:

```bash
cd Apex_Gestor
npm run build:web:secure
npm run build:electron:secure
```

Praticas:

- `sourceMap=false` em producao.
- Angular CLI/Terser para minificacao padrao.
- `javascript-obfuscator` somente apos o build, com intensidade moderada para nao quebrar lazy chunks.
- Nunca colocar `JWT_SECRET`, senha MySQL, chaves NF-e ou gateways de pagamento no frontend.
- Mobile Android: habilitar R8/Proguard no build release.
- Desktop: assinar binarios e validar atualizacoes por canal HTTPS assinado.

## 6. Fluxos criticos

Entrada XML NF:

- Validar XML e chave da NF-e.
- Garantir idempotencia por `chaveNFe`.
- Registrar itens importados em `movimentacao_estoque`.
- Atualizar custo medio e estoque em transacao unica.
- Auditar usuario, data, IP pseudonimizado e origem.

Venda omnichannel:

- Carrinho reserva estoque por janela curta.
- Finalizacao confirma pagamento, baixa estoque e cria venda em transacao unica.
- Se estoque insuficiente, venda falha antes da cobranca definitiva.
- Cancelamento estorna pagamento e gera movimentacao reversa.

Entrega:

- Status simples: `PENDENTE -> EM_ROTA -> ENTREGUE`.
- Estados extras permitidos: `EM_SEPARACAO`, `FALHA_ENTREGA`, `CANCELADA`.
- Desktop/web podem usar kanban drag and drop; mobile staff deve usar botoes de status grandes.

## 7. Plano de testes

JUnit/integracao:

- `NotaFiscalServiceTest`: importa XML, rejeita chave duplicada e atualiza estoque.
- `VendaServiceTest`: finaliza venda, baixa estoque, rejeita estoque insuficiente e cancela com estorno.
- `FinanceiroServiceTest`: DRE basico, contas a pagar/receber e caixa diario.
- `SecurityTest`: rotas sem JWT retornam 401; perfil errado retorna 403; IDOR retorna 404/403.
- `PrivacyServiceTest`: consentimento, exportacao e anonimizacao/exclusao.

Cypress:

- Login staff.
- Entrada de NF por XML.
- Conferencia do estoque atualizado.
- Venda no PDV com pagamento.
- Validacao de baixa de estoque.
- Compra cliente web/mobile: busca, filtro, carrinho e checkout.
- Entrega: mover pedido de pendente para em rota e entregue.

## 8. Roadmap de execucao

1. Congelar dependencias e limpar artefatos versionados (`target`, `dist`, `release`, `node_modules`).
2. Fechar build Angular/Ionic/Electron/Capacitor com auditoria sem vulnerabilidade alta de runtime.
3. Completar testes JUnit dos servicos de NF, estoque, venda, financeiro, seguranca e LGPD.
4. Adicionar Cypress E2E com fixtures reais e perfis staff/cliente.
5. Finalizar fluxo de entregas com status/kanban responsivo.
6. Implementar reserva de estoque para e-commerce e carrinho omnichannel.
7. Configurar CI com SAST, dependency scan, DAST em staging e upload de artefatos.
8. Assinar Electron, habilitar R8 Android, preparar certificado TLS e variaveis secretas no servidor.
9. Rodar homologacao em desktop Windows, web Chrome/Edge e Android real.
10. Liberar producao somente quando todos os gates da secao 1 estiverem verdes.
