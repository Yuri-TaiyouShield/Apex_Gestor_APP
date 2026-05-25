# UX/UI Gestor Apex Bootstrap

Documento de design e arquitetura de interface para o Gestor Apex, um sistema de gestão para lojas de material de construção. A entrega atual está em `apps/gestor-apex-bootstrap` e foi feita em HTML, CSS e Bootstrap para futura integração com Java Servlets, JSP, PostgreSQL e Apache Tomcat.

## Screen Descriptions

### Dashboard Gerencial

Tela de operação normal para dono, gerente e equipe administrativa. Exibe KPIs de vendas, estoque crítico, margem média e entregas pendentes. A tabela de estoque permite busca rápida, leitura por status e ações contextuais como comprar, abrir ficha ou histórico.

### PDV Focado

Tela limpa para caixa. Remove ruído gerencial e prioriza busca por código, botões grandes de produto, carrinho, forma de pagamento e finalização. O foco é reduzir cliques e manter as ações mais frequentes acima da dobra.

### Central de Pendências

Tela de sobrecarga e erro para situações em que o operador precisa de orientação clara. Mostra falha de conexão, estoque baixo e permissão negada com explicação prática e botão de próxima ação.

## Layout Hierarchy

1. Shell principal com sidebar fixa em desktop e empilhada em telas menores.
2. Topbar com contexto da tela, alternância de tema e ações globais.
3. Conteúdo por modo:
   - Dashboard: KPIs no topo, tabela de estoque e atalhos operacionais.
   - PDV: catálogo rápido à esquerda, carrinho e pagamento à direita.
   - Alertas: cards de erro com recuperação guiada.
4. Feedback global com toast e modal Bootstrap.

## UX Behavior Explanations

### Navegação

Os botões da sidebar alternam a tela ativa sem recarregar a página. O botão ativo recebe `aria-pressed="true"`, cor de destaque e título atualizado na topbar.

### Tema Claro e Escuro

O botão "Modo escuro" alterna `data-bs-theme` no elemento HTML. A preferência fica salva no `localStorage`, então o usuário mantém o tema na próxima abertura.

### Dashboard

Os botões de período "Hoje", "Semana" e "Mês" funcionam como controle segmentado. Ao clicar, o botão ativo muda visualmente e um toast informa que o dashboard foi atualizado.

### Estoque

O campo de busca filtra as linhas da tabela em tempo real. Botões "Comprar", "Histórico" e "Ficha" dão feedback imediato, simulando o comportamento futuro com backend.

### PDV

Botões de produto adicionam itens ao carrinho e atualizam o total em reais. O botão "Limpar" zera a venda. O botão "Finalizar venda" valida se existe item no carrinho; se não existir, mostra orientação clara. Quando a venda é válida, abre modal de sucesso, limpa o carrinho e simula reserva/baixa de estoque.

### Alertas e Erros

Botões de recuperação seguem ação direta:

- "Tentar novamente" e "Repetir sincronização": entram em estado de carregamento, ficam desabilitados temporariamente e depois mostram sucesso.
- "Gerar pedido de compra": informa que um pedido foi criado em rascunho.
- "Solicitar liberação": informa que o gerente receberá solicitação de aprovação.

## Visual Style Direction

### Paleta

| Uso | Hex | Motivo |
| --- | --- | --- |
| Primária petróleo | `#064E5A` | Marca profissional, boa leitura em botões e barras. |
| Primária profunda | `#073B43` | Hover/active de ações principais. |
| Texto claro | `#0F172A` | Alto contraste em fundo claro. |
| Fundo neve | `#F8FAFC` | Reduz fadiga visual em longas jornadas. |
| Borda suave | `#E2E8F0` | Separação visual sem poluição. |
| Sucesso | `#116B4D` | Operação concluída e API online. |
| Atenção | `#B45309` | Estoque baixo sem saturação excessiva. |
| Crítico | `#B42318` | Falhas, bloqueios e risco operacional. |
| Foco acessível | `#22D3EE` | Ring visível em claro e escuro. |

### Tipografia

Fonte sem serifa de sistema, com fallback para `Inter`, `Segoe UI` e `system-ui`. Títulos usam peso 800, botões usam peso 800 e textos operacionais ficam entre 14px e 16px para leitura em ambientes com reflexo.

### Espaçamento

O sistema usa cards com raio de 8px, espaçamento mínimo de 12px entre controles e botões com altura mínima de 42px. No PDV, botões de produto têm área ampla para uso rápido com mouse ou toque.

## Component System Suggestions

### Botões

| Componente | Uso | Normal | Hover | Active | Focus | Disabled |
| --- | --- | --- | --- | --- | --- | --- |
| `btn-apex-primary` | Ação principal | Fundo `#064E5A`, texto branco | Escurece para `#073B43` | Desce 1px | Ring ciano | Opacidade 56%, sem clique |
| `btn-apex-outline` | Ação secundária | Borda neutra, texto petróleo | Fundo petróleo translúcido | Desce 1px | Ring ciano | Opacidade 56% |
| `btn-apex-ghost` | Ação leve | Sem borda | Fundo cinza suave | Desce 1px | Ring ciano | Opacidade 56% |
| `btn-apex-danger-soft` | Erro/risco | Vermelho em fundo suave | Borda vermelha | Desce 1px | Ring ciano | Opacidade 56% |
| `apex-product-button` | Produto do PDV | Card clicável | Eleva e destaca borda | Desce 1px | Ring ciano | N/A |

### Inputs

Campos de busca usam ícone, fundo suave, altura mínima de 44px e borda neutra. Em foco, recebem ring acessível sem alterar o tamanho do layout.

### Cards

Cards são usados para KPIs, tabela, carrinho e alertas. Cada card tem borda leve, raio de 8px e sombra moderada. Não há cards aninhados para manter a leitura limpa.

### Estados de Erro

Mensagens devem explicar:

1. O que aconteceu.
2. Se o operador consegue resolver sozinho.
3. Qual botão deve apertar.
4. Quando acionar suporte.

Exemplo: "A API não respondeu. Continue em modo local e clique em Tentar novamente. Se o erro persistir, informe o protocolo ao suporte."
