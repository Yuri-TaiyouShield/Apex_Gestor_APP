# Estrutura de Branches do Apex Gestor

Este repositório mantém branches por marco de arquitetura, para que cada versão com impacto grande possa ser testada ou recuperada sem misturar contextos.

## Linha principal

- `main`: linha estável publicada no GitHub.
- `codex/release/v5.0-enterprise-b2c-rbac-catalog`: evolução atual com B2C público, RBAC B2B, catálogo enriquecido, migrations v5/v6 e documentação enterprise.

## Branches por versão impactante

| Branch | Base | Papel |
| --- | --- | --- |
| `codex/legacy/pre-v3-apex-gestor-2` | ultimo commit antes da modernizacao v3 | Estado legado anterior a CI/CD multiplataforma. |
| `codex/release/v3.0-multiplataforma` | `v3.0.5` | Angular/Ionic/Electron, ajustes de menu, audit e empacotamento por plataforma. |
| `codex/release/v3.1-orquestracao-resiliente` | `v3.1.0` | Docker Compose, healthchecks, quality gates e runbook de execucao. |
| `codex/release/v3.2-licenciamento-apps` | `v3.2.0` | Protecao de uso por licenca e combinacoes de apps liberados. |
| `codex/release/v3.3-admcalc-financeiro` | `v3.3.0` | Modulo AdmCalc financeiro, compliance trabalhista/tributario e auditoria. |
| `codex/release/v4.0-enterprise-base` | `v4.0.0` | Base enterprise, monorepo operacional, CI/CD e fundacao SecDevOps. |
| `codex/release/v4.0-enterprise-foundation` | `codex/v4-enterprise-foundation` | Linha alternativa com refinamentos de fundacao v4 ja existentes no historico. |
| `codex/release/v5.0-enterprise-b2c-rbac-catalog` | commit atual | Separacao B2C/B2B, RBAC estrito, enrichment de catalogo e seed/migrations v5. |

## Regras de uso

1. Desenvolvimento novo deve partir da branch de release mais recente.
2. Correcoes pequenas podem ser aplicadas na branch da versao afetada e depois promovidas para a branch mais nova.
3. Branches antigas `codex/*` foram preservadas como historico de trabalho e PRs; nao devem ser apagadas sem revisao.
4. Tags `v*` continuam sendo os pontos oficiais de release; branches de release sao linhas navegaveis para desenvolvimento e manutencao.

## Comandos uteis

```bash
git branch --list "codex/release/*"
git switch codex/release/v5.0-enterprise-b2c-rbac-catalog
git log --oneline --decorate --max-count=10
```
