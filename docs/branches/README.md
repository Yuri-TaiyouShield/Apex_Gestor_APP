# Branches e Versoes

As linhas de evolucao do Apex Gestor estao organizadas por marcos funcionais. Para testes publicos, use o indice abaixo em vez de abrir branches manualmente.

## Testes Publicos

- Indice geral por branch: https://yuri-taiyoushield.github.io/Apex_Gestor_APP/branches/
- Aplicacao Web principal: https://yuri-taiyoushield.github.io/Apex_Gestor_APP/
- Releases com Desktop, Mobile e Web: https://github.com/Yuri-TaiyouShield/Apex_Gestor_APP/releases/latest

## Marcos Funcionais

| Marco | Finalidade |
| --- | --- |
| Linha principal | Versao estavel publicada para avaliacao. |
| Legado pre-v3 | Snapshot anterior a v3, mantido para comparacao historica. |
| v3.0 multiplataforma | Primeira organizacao Angular, Ionic, Electron e CI multiplataforma. |
| v3.1 orquestracao resiliente | Docker Compose, healthchecks, runbook e quality gates. |
| v3.2 licenciamento | Licenciamento por pacote de apps. |
| v3.3 financeiro | Modulo AdmCalc, compliance financeiro e auditoria. |
| v4.0 enterprise | Base enterprise com seguranca, orquestracao e DevOps. |
| v5.0 B2C/RBAC/catalogo | Evolucao com e-commerce B2C, RBAC B2B e catalogo enriquecido. |

## Politica

- A linha principal deve permanecer facil de navegar e com README atualizado.
- Linhas de release sao usadas para manutencao e demonstracao de marcos.
- Linhas legadas existem para consulta e execucao historica.
- Experimentos antigos so devem ser removidos apos confirmar que nao possuem PR, release ou trabalho util associado.
