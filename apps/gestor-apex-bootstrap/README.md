# Gestor Apex Bootstrap

Protótipo front-end estático para o Gestor Apex, voltado a lojas de material de construção. Esta versão usa HTML, CSS e Bootstrap para servir como base visual e funcional antes da integração com Java Servlets, JSP, PostgreSQL e Apache Tomcat.

## Como abrir

Abra o arquivo abaixo no navegador:

```text
apps/gestor-apex-bootstrap/index.html
```

Como o protótipo é estático, não precisa de servidor local. Para testar em um servidor simples:

```bash
cd apps/gestor-apex-bootstrap
python -m http.server 8090
```

Depois acesse `http://localhost:8090`.

## Telas incluídas

- Dashboard gerencial em modo normal.
- PDV focado para operação de caixa.
- Central de alertas para estoque baixo, falha de conexão e permissão negada.

## Interações implementadas

- Alternância entre modo claro e escuro com persistência em `localStorage`.
- Navegação entre Dashboard, PDV e Alertas.
- Busca em tabela de estoque.
- Carrinho de PDV com adição, remoção, limpeza e finalização de venda.
- Seleção visual de forma de pagamento.
- Botões com estados `hover`, `active`, `focus-visible` e `disabled`.
- Feedback por toast e modal Bootstrap.

## Integração futura com Java

Ao migrar para JSP, preserve:

- `assets/css/apex-design-system.css` como base do design system.
- `assets/js/apex-ui.js` como camada de comportamento inicial ou referência para módulos JS por página.
- Componentes Bootstrap como botões, cards, tabelas, modais e toasts.
- IDs semânticos de tela para ligação com Servlets, permissões e mensagens de erro vindas do backend.
