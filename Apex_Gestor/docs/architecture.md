# Apex Gestor Hybrid Architecture

## Arquitetura de Pastas

```text
Apex_Gestor/
  src/app/
    core/
      apex-api.service.ts       # Cliente HTTP para o backend Spring Boot
      api-config.service.ts     # URL da API por plataforma
      cart.service.ts           # Carrinho B2C e base do checkout
      entity-config.ts          # Metadados dos CRUDs internos
      models.ts                 # Tipos compartilhados do domínio
      session.service.ts        # Persona/RBAC local até autenticação real
    pages/
      storefront.page.ts        # E-commerce web/mobile cliente
      cart.page.ts              # Carrinho B2C
      checkout.page.ts          # Pedido B2C integrado com vendas
      dashboard.page.ts         # ERP interno por perfil
      entity-list.page.ts       # CRUDs Produtos, Clientes, Fornecedores etc.
      point-of-sale.page.ts     # PDV/Caixa para desktop e staff mobile
      invoice-entry.page.ts     # Entrada de XML de NF-e
      settings.page.ts          # API, plataforma e perfil ativo
      login.page.ts             # Entrada por persona/RBAC
  electron/
    main.cjs                    # Janela desktop, protocolo app://
    preload.cjs                 # Ponte segura para config desktop
  capacitor.config.ts           # Mobile Android/iOS com Ionic/Capacitor
  proxy.conf.json               # Proxy web /api -> Spring Boot
```

Para evolução maior, a divisão recomendada é separar o domínio em bibliotecas: `libs/catalog`, `libs/orders`, `libs/inventory`, `libs/finance`, `libs/rbac`, `libs/ui`. O app atual já deixa os limites preparados nos serviços e modelos.

## Entidades Principais

- `Usuario`, `Perfil`, `Permissao`: base de RBAC para cliente, vendedor, gerente e administrador.
- `Cliente`: CRM, dados B2C, endereços, histórico e consentimentos.
- `Fornecedor`: compras, vínculo com NF-e e lead time de reposição.
- `Produto`: catálogo base, custo, preço, status, SEO, imagens e marca.
- `ProdutoVariacao`: tamanho, cor, voltagem, unidade, SKU e código de barras.
- `Estoque`: saldo por produto/variação/local, estoque mínimo e reservado.
- `MovimentoEstoque`: entrada, saída, ajuste, reserva, cancelamento e origem.
- `Pedido`: carrinho confirmado no e-commerce, canal de venda e status.
- `Venda`: venda fiscal/financeira consolidada do PDV ou e-commerce.
- `ItemVenda`: produto, quantidade, preço, desconto e snapshot de custo.
- `Pagamento`: forma, valor, parcelas, autorização e status.
- `NotaFiscalEntrada`: XML importado, fornecedor, itens e custos.
- `NotaFiscalSaida`: NF-e/NFC-e emitida para venda ou pedido.
- `Despesa`, `ContaReceber`, `ContaPagar`, `CaixaDiario`: financeiro e DRE básico.
- `Entrega`: transportadora, endereço, status e rastreio simples.

## Fluxo Estoque x E-commerce

1. Cliente navega pelo e-commerce e o catálogo consulta apenas produtos ativos com saldo disponível.
2. Ao adicionar ao carrinho, o app mostra estoque disponível, mas ainda não baixa estoque.
3. No checkout, o backend cria `Pedido` e gera uma reserva em `Estoque.reservado`.
4. Pagamento aprovado converte o pedido em `Venda`, cria `ItemVenda`, registra `Pagamento` e baixa `Estoque.disponivel`.
5. NF-e/NFC-e é emitida a partir da venda e vinculada ao pedido.
6. Se houver entrega, o módulo logístico cria `Entrega` e acompanha despacho/rastreio.
7. Cancelamento ou pagamento recusado libera a reserva e cria movimento de estorno quando necessário.

Regra crítica: a baixa real do estoque deve acontecer em transação no backend, nunca só no frontend. O frontend apenas solicita a venda/pedido e reflete o estado retornado pela API.
