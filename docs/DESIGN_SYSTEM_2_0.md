# Apex Gestor 2.0 — Premium Gradient Enterprise Design System (ADR & Referência Técnica)

> **Status:** Fase 2 (Fundação Arquitetural e Identidade Dual-Theme)
> **Stack:** Angular 19 Standalone + Ionic 8 (`@ionic/angular/css/palettes/dark.class.css`) + CSS Custom Properties em 3 camadas + SVG declarativo.
> **Restrições preservadas:** Zero Tailwind CSS, remoção completa do `bootstrap` (não utilizado), CSP restrita (`default-src 'self'`), multitenancy e White-Label B2C preservados.

---

## 1. Arquitetura do Design System (3 Camadas + Utilitários)

Sem dependência de Tailwind CSS, os tokens são carregados em cascata previsível a partir de [src/styles.css](file:///f:/Apex_Gestor_APP/Apex_Gestor/src/styles.css):

| Ordem | Arquivo | Camada | Responsabilidade |
| --- | --- | --- | --- |
| 1 | `@ionic/angular/css/*` + `dark.class.css` | Base Ionic 8 | Estrutura, padding/flex/typography utilitários nativos e suporte a `.ion-palette-dark`. |
| 2 | [00-fonts.css](file:///f:/Apex_Gestor_APP/Apex_Gestor/src/styles/00-fonts.css) | Tipografia | `@font-face` para `'Inter Variable'` (`100 900`, `font-display: swap`), subsets `latin` e `latin-ext`. |
| 3 | [01-primitives.css](file:///f:/Apex_Gestor_APP/Apex_Gestor/src/styles/01-primitives.css) | **Layer 1 — Primitive** | Escalas brutas neutras (`slate`, `navy`, `obsidian`), paleta cromática (`blue`, `violet`, `rose`, `cyan`, `emerald`, `amber`, `sky`), gradientes `dawn-dusk`, espaçamentos (base 4px), raios, z-index e curvas de transição. |
| 4a | [02-semantic-light.css](file:///f:/Apex_Gestor_APP/Apex_Gestor/src/styles/02-semantic-light.css) | **Layer 2A — Semantic Light** | Tema **Premium Gradient Light** (`:root`, `[data-theme="premium-light"]`), superfícies branco suave / cinza azulado / aurora sutil e aliases legados `--apex-*`. |
| 4b | [02-semantic-dark.css](file:///f:/Apex_Gestor_APP/Apex_Gestor/src/styles/02-semantic-dark.css) | **Layer 2B — Semantic Dark** | Tema **Dark Executive** (`[data-theme="dark-executive"]`, `:root.ion-palette-dark(.md|.ios)`), profundidades em `#070b14` -> `#0b1020` -> `#11182c` -> `#172038`. |
| 5 | [03-components.css](file:///f:/Apex_Gestor_APP/Apex_Gestor/src/styles/03-components.css) | **Layer 3 — Component** | Dimensões (`compact` / `comfortable`), integração com Shadow DOM Ionic (`ion-button::part(native)`, `--background`, `--color`), cards, tabelas e pills. |
| 6 | [04-utilities.css](file:///f:/Apex_Gestor_APP/Apex_Gestor/src/styles/04-utilities.css) | Utilitários Próprios | Classes prefixadas `.apex-sr-only`, `.apex-tabular`, `.apex-gradient-text`, `.apex-btn-gradient`, `.apex-pill--*`. |

---

## 2. Identidade Cromática Dual-Theme e Matriz de Contraste (WCAG AA/AAA)

A narrativa cromática **Amanhecer -> Anoitecer** combina Azul Elétrico (`#2563eb`), Violeta Real (`#7c3aed`) e Vermelho Carmesim (`#e11d48`), apoiados por Ciano (`#06b6d4` / `#22d3ee`) em séries analíticas.

| Token Semântico | Premium Gradient Light | Dark Executive | Papel & Validação de Contraste |
| --- | --- | --- | --- |
| `--apex-bg-canvas` | `#f2f5fc` | `#070b14` | Fundo base com `--apex-bg-aurora` sutil no topo. |
| `--apex-bg-surface` | `#ffffff` | `#11182c` | Cards principais com borda hairline `--apex-border-default`. |
| `--apex-bg-surface-elevated` | `#ffffff` | `#172038` | Modais, tooltips e toasts com elevação diferenciada. |
| `--apex-text-primary` | `#0f172a` | `#f1f5f9` | Contraste alto (> 13:1 sobre superfície principal em ambos os temas). |
| `--apex-text-secondary` | `#334155` | `#cbd5e1` | Leitura prolongada (> 8.5:1 sobre superfície principal). |
| `--apex-text-muted` | `#526077` | `#94a3b8` | Labels e legendas (>= 5.4:1 no claro e >= 6.4:1 no escuro). |
| `--apex-status-success` | `#047857` | `#34d399` | Resultados positivos sobre superfícies com fundo `--apex-status-success-bg`. |
| `--apex-status-warning` | `#92400e` | `#fbbf24` | Alertas moderados sobre superfícies e badges `--apex-status-warning-bg` (>= 4.5:1 AA). |
| `--apex-status-danger` | `#be123c` | `#fb7185` | Alertas críticos sobre superfícies com fundo `--apex-status-danger-bg`. |
| `--apex-fill-primary` / `-contrast` | `#2563eb` / `#ffffff` | `#2563eb` / `#ffffff` | Preenchimento sólido de botões primários (>= 4.5:1 AA independente dos acentos de texto). |
| `--apex-fill-warning` / `-contrast` | `#f59e0b` / `#0f172a` | `#fbbf24` / `#0f172a` | Preenchimento sólido de alertas com contraste alto (> 8:1). |
| `--apex-fill-danger` / `-contrast` | `#be123c` / `#ffffff` | `#be123c` / `#ffffff` | Ações destrutivas e confirmação crítica (>= 6.0:1). |
| `--apex-brand-gradient` | `135deg: #2563eb -> #7c3aed -> #e11d48` | `135deg: #2563eb -> #7c3aed -> #e11d48` | Aplicado estrategicamente em CTAs primários, ícone ativo, marca e séries de tendência. |

---

## 3. Tipografia Local (Self-Hosted Inter Variable + SIL OFL)

- **Arquivos em** `Apex_Gestor/public/fonts/`:
  - `inter-latin-wght-normal.woff2`
  - `inter-latin-ext-wght-normal.woff2` (cobertura completa de acentuação e diacríticos em português: `ç`, `ã`, `õ`, `á`, `é`, `ê`, `ó`, `ú`, etc.)
  - `OFL.txt` (SIL Open Font License 1.1 preservada no build final).
- **Configuração:** `@font-face` com `font-weight: 100 900`, `font-display: swap` e fallback do sistema (`ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif`), compatível com CSP `font-src 'self' data: app:` definida em [index.html](file:///f:/Apex_Gestor_APP/Apex_Gestor/src/index.html) sem chamadas a CDNs externos.
- **CSP multiplataforma (`index.html` vs `nginx.conf`):** A meta CSP em `index.html` permite `connect-src 'self' app: https: http://localhost:* http://127.0.0.1:* http://10.0.2.2:8080 ws://localhost:* ws://127.0.0.1:*` para suportar Electron e Capacitor, enquanto [nginx.conf](file:///f:/Apex_Gestor_APP/Apex_Gestor/nginx.conf) impõe cabeçalho HTTP `connect-src 'self'` em produção web.

---

## 4. Integração com Ionic 8 Shadow DOM e Compatibilidade White-Label B2C

- **Seletividade Ionic 8 Dark Mode:** As variáveis de modo Ionic (`--ion-item-background`, `--ion-background-color`, `--ion-text-color`, etc.) no tema escuro incluem `:root.ion-palette-dark.md` e `:root.ion-palette-dark.ios` para prevalecer sobre as regras de especificidade padrão do Ionic 8 (`palettes/dark.class.css`).
- **Shadow DOM Parts:** Estilizações estruturais de botões e campos Ionic atravessam o encapsulamento via `::part(native)`, `::part(scroll)` e Custom Properties públicas (`--background`, `--border-radius`, `--padding-*`), usando tokens dedicados `--apex-fill-*`.
- **Preservação White-Label B2C (`TenantFeatureService`):** `TenantFeatureService.isWhiteLabelB2C()` injeta `--apex-primary`, `--ion-color-primary` e `--ion-color-secondary` diretamente no elemento raiz e limpa essas propriedades inline quando o tenant não usa B2C White-Label. Os tokens derivados `--apex-primary-soft`, `--apex-primary-faint`, `--apex-primary-strong` e `--apex-text-brand` utilizam `color-mix` para preservar legibilidade e identidade visual nos dois temas.
- **Gerenciamento de Tema e Sobreposições (`src/app/core/ui/`):**
  - `ThemeService` ([theme.service.ts](file:///f:/Apex_Gestor_APP/Apex_Gestor/src/app/core/ui/theme.service.ts)): gerencia preferência `system | light | dark`, migra a chave legada `'dense'` para `'light'` + densidade `'compact'`, sincroniza `data-theme`, `data-density`, classe `.ion-palette-dark` e `<meta name="theme-color">`.
  - `DialogService` + `ApexDialogHostComponent`: substituto acessível por teclado (`Esc`, `Enter`, armadilha de foco via `Tab`/`Shift+Tab`, retorno de foco, `role="alertdialog" | "dialog"`). Os chamadores existentes (`cashier.page.ts`, `sales-history.page.ts`, `session-timeout.service.ts`) mantêm seus diálogos atuais até a migração progressiva das telas nas Fases 3–5.
  - `ToastService` + `ApexToastHostComponent`: região `aria-live="polite"` com fila limitada a 4 itens e cancelamento imediato de timers ao descartar excedentes.

---

## 5. Gráficos SVG Declarativos e Componentes Base (`src/app/shared/ui/`)

Implementados em SVG puro e Angular Signals, sem bibliotecas externas de terceiros:

- `ApexKpiCardComponent`: card executivo com gradiente iconográfico, leitura tabular e `ApexSparklineChartComponent` opcional (com skeleton em estado `loading`).
- `ApexTrendChartComponent`: modos `area`, `line` e `bar`, gradiente de série, linhas de grade responsivas, tooltip interativa e faixa focável por teclado (`.point-pill`), além de `apex-empty-state` quando `points` está vazio e tabela acessível `.apex-sr-only`.
- `ApexDonutChartComponent`: variantes `donut` (com total/percentual central dinâmico) e `pie`, legenda interativa por mouse/foco, estados `loading`/`empty` e tabela `.apex-sr-only`.
- `ApexPageHeaderComponent`, `ApexStatusBadgeComponent`, `ApexSkeletonComponent`, `ApexEmptyStateComponent`.

---

## 6. Auditoria de CORS — `NotificacaoController` e Inventário de Controllers

### 6.1 Causa Raiz e Correção Aplicada (Fase 2)

- Em [NotificacaoController.java](file:///f:/Apex_Gestor_APP/Apex-Gestordemo/src/main/java/Controller/NotificacaoController.java), a anotação local `@CrossOrigin(origins = "http://localhost:4200")` restringia e sobrescrevia a política central do [SecurityConfig.java](file:///f:/Apex_Gestor_APP/Apex-Gestordemo/src/main/java/com/Apex/Apex_Gestordemo/SecurityConfig.java), bloqueando requisições originadas do desktop Electron (`app://localhost`), Android Capacitor (`https://localhost`), iOS (`capacitor://localhost` / `ionic://localhost`) e `http://127.0.0.1:*`.
- **Correção aplicada:** remoção de `@CrossOrigin(origins = "http://localhost:4200")` em `NotificacaoController.java`, delegando integralmente à `corsConfigurationSource()` do `SecurityConfig` com origens explícitas e separação dev vs produção:
  - **Desenvolvimento / Local** (`application.properties`): `http://localhost,https://localhost,http://localhost:*,https://localhost:*,http://127.0.0.1,https://127.0.0.1,http://127.0.0.1:*,https://127.0.0.1:*,app://localhost,ionic://localhost,capacitor://localhost` (incluindo `https://localhost` sem porta exigido pelo `androidScheme: 'https'` do Capacitor).
  - **Produção** ([application-prod.properties](file:///f:/Apex_Gestor_APP/Apex-Gestordemo/src/main/resources/application-prod.properties)): remove padrões locais HTTP/loopback e restringe o default a `https://localhost,app://localhost,ionic://localhost,capacitor://localhost` mais domínios explicitamente configurados em `APEX_CORS_ALLOWED_ORIGINS`.
  - `allowCredentials(false)` mantido (autenticação stateless via header `Authorization: Bearer <jwt>`, sem wildcards com credenciais).
  - Nenhuma regra de RBAC (`ApexPermission.PRODUCT_VIEW`), isolamento multi-tenant (`empresaId` / `filialId`) ou postura fail-closed de produção foi enfraquecida.
  - Cobertura adicionada em [NotificacaoCorsTests.java](file:///f:/Apex_Gestor_APP/Apex-Gestordemo/src/test/java/com/Apex/Apex_Gestordemo/NotificacaoCorsTests.java).

### 6.2 Inventário de Controllers com Mesma Anotação Legada (Escopo de Fases Futuras)

Os sete controllers abaixo ainda possuem `@CrossOrigin(origins = "http://localhost:4200")` e foram preservados intactos na Fase 2 para futura remoção controlada quando seus respectivos módulos forem migrados/validados:

1. `AuditoriaController.java`
2. `CaixaController.java`
3. `CondicaoPagamentoController.java`
4. `EstoqueMovimentacaoController.java`
5. `FornecedorController.java`
6. `GrupoProdutoController.java`
7. `UsuarioController.java`
