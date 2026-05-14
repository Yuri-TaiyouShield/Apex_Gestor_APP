# Guia GitHub - Commit, Push e Releases Apex Gestor 3.0

## 1. Conferir alteracoes antes de salvar

```bash
git status
git diff --stat
```

Revise se nao existem arquivos gerados sendo enviados por engano, como `node_modules`, `dist`, `target`, `release`, `.electron-package` ou APKs locais.

## 2. Adicionar somente arquivos do projeto

Opcao simples:

```bash
git add .
```

Opcao mais segura quando houver muitos arquivos gerados:

```bash
git add .github/workflows/main.yml
git add README.md SECURITY.md docs
git add Apex_Gestor Apex-Gestordemo .gitignore
```

Depois confira:

```bash
git status
```

## 3. Criar commit

```bash
git commit -m "feat: atualiza Apex Gestor 3.0"
```

## 4. Enviar branch principal

```bash
git push origin main
```

Esse push executa a Action e disponibiliza os builds como artifacts na aba Actions.

## 5. Criar tag para publicar Release

```bash
git tag v3.0.4
git push origin v3.0.4
```

Use uma tag nova a cada publicacao (`v3.0.5`, `v3.0.6` etc.). A tag aciona a publicacao automatica em GitHub Releases. Ao terminar, acesse:

```text
https://github.com/Yuri-TaiyouShield/Apex_Gestor_APP/releases
```

## 6. Permissoes necessarias no GitHub

No repositorio, abra `Settings > Actions > General` e confirme:

- Actions habilitado.
- Workflow permissions: `Read and write permissions`.
- Allow GitHub Actions to create and approve pull requests pode ficar desativado.

## 7. Arquivos publicados na Release

Cada plataforma e publicada em um `.zip` proprio, contendo uma pasta com nome claro:

- `apex-gestor-app-site-web-vX.Y.Z.zip` contem `App Site Web`.
- `apex-gestor-app-desktop-vX.Y.Z.zip` contem `App Desktop`.
- `apex-gestor-app-mobile-empresa-vX.Y.Z.zip` contem `App Mobile Empresa`.
- `apex-gestor-app-mobile-cliente-vX.Y.Z.zip` contem `App Mobile Cliente`.

Baixe e extraia o `.zip` da plataforma desejada. Dentro dele havera um arquivo `LEIA-ME.txt` com a orientacao rapida de uso.

## 8. Seguranca

- Nao commitar senhas, `.env`, certificados, keystores ou tokens.
- Usar GitHub Secrets para credenciais de assinatura Android, certificado desktop e chaves fiscais.
- A pipeline gera APKs debug de teste. Para loja/producao, adicione assinatura Android release com secrets.
