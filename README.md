# Apex Gestor 🏗️

![GitHub last commit](https://img.shields.io/github/last-commit/JorginhoVigas/apex-gestor?style=for-the-badge&logo=github)
![GitHub language count](https://img.shields.io/github/languages/count/JorginhoVigas/apex-gestor?style=for-the-badge)
![License](https://img.shields.io/github/license/JorginhoVigas/apex-gestor?style=for-the-badge)

> Sistema completo para gerenciamento de lojas de material de construção. O Apex Gestor foi desenvolvido para otimizar processos, controlar o estoque e facilitar a gestão financeira do seu negócio.

---

## 📋 Índice

- [Visão Geral do Projeto](#-visão-geral-do-projeto)
- [✨ Funcionalidades Principais](#-funcionalidades-principais)
- [🚀 Tecnologias Utilizadas](#-tecnologias-utilizadas)
- [🤝 Como Contribuir](#-como-contribuir)
- [👥 Equipe](#-equipe)
- [📄 Licença](#-licença)

---

## 📖 Visão Geral do Projeto

O **Apex Gestor** é uma solução de software robusta, pensada para atender às necessidades específicas de uma loja de material de construção. O sistema centraliza informações e automatiza tarefas, desde o controle de entrada e saída de produtos até a geração de relatórios gerenciais, proporcionando uma visão 360° da empresa.

---

## ✨ Funcionalidades Principais

- **Gestão de Estoque:** Controle de produtos, lotes, fornecedores e notificações de estoque baixo.
- **Módulo de Vendas (PDV):** Interface rápida para registro de vendas, orçamentos e emissão de notas.
- **Controle Financeiro:** Gerenciamento de contas a pagar e a receber, fluxo de caixa e relatórios financeiros.
- **Cadastro de Clientes e Fornecedores:** Base de dados centralizada para contatos e histórico de transações.
- **Relatórios Gerenciais:** Dashboards intuitivos com indicadores de performance, vendas por período, produtos mais vendidos, etc.
- **Controle de Acesso:** Níveis de permissão por usuário (vendedor, gerente, administrador).

---

## 🚀 Tecnologias Utilizadas

- **Frontend:** `Ionic Angular`, `HTML`, `CSS`
- **Backend:** `Java (Servlets & JSP)`
- **Banco de Dados:** `PostgreSQL`
- **Servidor de Aplicação:** `Apache Tomcat`
- **Controle de Versão:** `Git & GitHub`
- **Gerenciador de Dependências:** `Maven`

---

## 📱 Branch UI Ionic Mobile

Esta branch (`codex/ui/ionic-mobile-admcalc`) foi provisionada a partir de `codex/legacy/pre-v3-apex-gestor-2` para isolar a camada Mobile em Ionic.

- App: `apps/ionic-mobile`
- Menu lateral: interativo, retrátil e responsivo para toque.
- Tema: alternância entre Light Mode e Dark Mode.
- AdmCalc: fluxo mobile funcional com cálculo local e integração REST em `/api/admcalc/calcular`.
- Backend: endpoint `AdmCalcController` adicionado ao Spring Boot legado.

Executar:

```bash
cd apps/ionic-mobile
npm install
npm run start
```

---

## 🤝 Como Contribuir

Ficou interessado em contribuir? Confira nosso guia de contribuição `CONTRIBUTING.md` para saber mais detalhes sobre o fluxo de trabalho, como abrir *issues* e submeter *pull requests*. Toda ajuda é bem-vinda!

---

## 👥 Equipe

| Foto | Nome | Função | GitHub | LinkedIn |
| :--: | :----------------------------------------------------------: | :----------------: | :----------------------------------------------------------: | :----------------------------------------------------------: |
| <img src="URL_DA_FOTO_AQUI" width="70" height="70"> | **Yuri Alcantara** | Desenvolvedor Full Stack | [GitHub](https://github.com/Yuri-TaiyouShield) | [LinkedIn](https://www.linkedin.com/in/yuri-alcantara-802a76243/) |


---

## 📄 Licença

Este projeto está sob a licença [MIT](link-para-o-arquivo-de-licenca). Veja o arquivo `LICENSE` para mais detalhes.

