# Apps do Apex Gestor

Esta pasta documenta a topologia alvo do monorepo por plataforma. O codigo executavel ainda fica nos caminhos historicos usados pela CI, para evitar quebra de build durante a transicao.

| Pasta alvo | Codigo ativo hoje | Uso |
| --- | --- | --- |
| `apps/backend-api` | `../Apex-Gestordemo` | API Spring Boot. |
| `apps/web-cliente` | `../Apex_Gestor` | E-commerce Angular/Ionic. |
| `apps/desktop-pdv` | `../Apex_Gestor/electron` | PDV Electron. |
| `apps/mobile-cliente` | `../Apex_Gestor` configuracao `mobile-client` | App Android do cliente. |
| `apps/mobile-gestao` | `../Apex_Gestor` configuracao `mobile-staff` | App Android da empresa. |
| `apps/gestor-apex-bootstrap` | Prototipo estatico independente | Base HTML/CSS/Bootstrap para futura integracao com Java Servlets, JSP, PostgreSQL e Tomcat. |

Quando a migracao fisica acontecer, mova um app por vez e atualize no mesmo commit:

1. `package.json` raiz.
2. `.github/workflows/main.yml`.
3. `docker-compose.yml`.
4. Configuracoes Angular, Capacitor e Electron.
5. Runbooks em `docs/`.
