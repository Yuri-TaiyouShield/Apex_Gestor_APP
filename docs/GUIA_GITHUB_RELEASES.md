# Guia GitHub - Commit, Push e Releases Apex Gestor 3.0

## 1. Conferir alteracoes antes de salvar

```bash
git status
git diff --stat
```

Revise se nao existem arquivos gerados sendo enviados por engano, como `node_modules`, `dist`, `target`, `release`, `.electron-package` ou APKs locais.

## 2. Adicionar somente arquivos do projeto

Se `git status` mostrar arquivos em conflito como `Apex_BD.pdf` ou `Apex_BD.sql`, resolva antes do commit:

```bash
# manter os arquivos no repositorio
git add Apex_BD.pdf Apex_BD.sql

# ou, se decidir remover esses dumps antigos do repositorio
git rm Apex_BD.pdf Apex_BD.sql
```

Opcao simples:

```bash
git add .
```

Opcao mais segura, recomendada quando o repositorio esta com muitos arquivos gerados:

```bash
git add .github/workflows/main.yml
git add docs/APEX_GESTOR_3_0_DOCUMENTACAO.md docs/GUIA_GITHUB_RELEASES.md docs/ROADMAP_PRODUCAO.md SECURITY.md README.md
git add Apex_Gestor Apex-Gestordemo .gitignore
```

Depois confira:

```bash
git status
```

## 3. Criar commit

```bash
git commit -m "feat: prepara Apex Gestor 3.0 multi-nicho com CI/CD"
```

## 4. Enviar branch principal

```bash
git push origin main
```

Esse push executa a Action e disponibiliza os builds como artifacts na aba Actions.

## 5. Criar tag para publicar Release

```bash
git tag v3.0.0
git push origin v3.0.0
```

A tag `v3.0.0` aciona a publicacao automatica em GitHub Releases. Ao terminar, acesse:

```text
https://github.com/Yuri-TaiyouShield/Apex_Gestor_APP/releases
```

## 6. Permissoes necessarias no GitHub

No repositorio, abra `Settings > Actions > General` e confirme:

- Actions habilitado.
- Workflow permissions: `Read and write permissions`.
- Allow GitHub Actions to create and approve pull requests pode ficar desativado.

## 7. Seguranca

- Nao commitar senhas, `.env`, certificados, keystores ou tokens.
- Usar GitHub Secrets para credenciais de assinatura Android, certificado desktop e chaves fiscais.
- A primeira pipeline gera APKs debug de teste. Para loja/produção, adicionar assinatura Android release com secrets.
