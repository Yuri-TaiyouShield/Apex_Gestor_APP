# Branches e Versoes

As branches abaixo separam marcos grandes do Apex Gestor. Use este arquivo para identificar rapidamente qual linha abrir no GitHub.

| Branch | Finalidade |
| --- | --- |
| `main` | Linha principal estavel publicada no GitHub. |
| `codex/legacy/pre-v3-apex-gestor-2` | Snapshot legado anterior a v3, com backend Spring Boot antigo. |
| `codex/release/v3.0-multiplataforma` | Primeira organizacao Angular/Ionic/Electron e CI multiplataforma. |
| `codex/release/v3.1-orquestracao-resiliente` | Docker Compose, healthchecks, runbook e quality gates. |
| `codex/release/v3.2-licenciamento-apps` | Licenciamento por pacote de apps. |
| `codex/release/v3.3-admcalc-financeiro` | Modulo financeiro AdmCalc e compliance. |
| `codex/release/v4.0-enterprise-base` | Base enterprise v4 publicada como main. |
| `codex/release/v4.0-enterprise-foundation` | Linha alternativa com refinamentos da fundacao enterprise. |
| `codex/release/v5.0-enterprise-b2c-rbac-catalog` | Evolucao v5 com B2C publico, RBAC B2B e catalogo enriquecido. |

## Branches arquivadas

| Branch | Motivo |
| --- | --- |
| `codex/archive/orchestration-healthchecks` | Preserva a linha antiga de healthchecks/orquestracao sem competir com `codex/release/v3.1-orquestracao-resiliente`. |

## Comandos uteis

```bash
git fetch --all --tags
git branch -r
git switch main
git switch codex/release/v5.0-enterprise-b2c-rbac-catalog
```

## Politica

- `main` deve permanecer facil de navegar e com README atualizado.
- Branches `codex/release/*` sao linhas de manutencao por versao.
- Branches `codex/legacy/*` existem para consulta e execucao historica.
- Branches `codex/archive/*` guardam experimentos antigos que ainda podem ter valor historico.
- Experimentos antigos so devem ser removidos depois de confirmar que nao possuem PR, release ou trabalho util associado.
