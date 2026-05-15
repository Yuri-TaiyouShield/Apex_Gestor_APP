# Apex Gestor v5.0 - Especificacao UML para Astah

Use este roteiro para replicar os diagramas diretamente no Astah Professional. A nomenclatura foi alinhada ao backend Spring Boot e ao dominio v5.

## 1. Diagrama de Casos de Uso Geral

Sistema: `Apex Gestor v5 Enterprise`

Atores:

- `Visitante B2C`
- `Cliente B2C`
- `ROLE_SYSADMIN`
- `ROLE_DONO_GERENTE`
- `ROLE_FINANCEIRO`
- `ROLE_VENDEDOR`
- `ROLE_CAIXA`
- `ROLE_DESPACHANTE`
- `CatalogEnrichmentJob`
- `Gateway/API de Busca Autorizada`

Casos de uso:

- `Navegar vitrine publica`
- `Buscar e filtrar produtos`
- `Adicionar item ao carrinho local`
- `Autenticar para checkout`
- `Mesclar carrinho local com carrinho persistido`
- `Finalizar pedido`
- `Baixar estoque`
- `Gerenciar catalogo`
- `Enriquecer imagem do produto`
- `Operar PDV`
- `Abrir e fechar caixa`
- `Gerenciar clientes CRM`
- `Executar AdmCalc`
- `Emitir nota fiscal`
- `Despachar pedido aprovado`
- `Auditar logs e tenants`
- `Gerenciar usuarios e perfis`

Relacionamentos:

- `Visitante B2C` -> `Navegar vitrine publica`, `Buscar e filtrar produtos`, `Adicionar item ao carrinho local`
- `Cliente B2C` -> `Autenticar para checkout`, `Mesclar carrinho local com carrinho persistido`, `Finalizar pedido`
- `Finalizar pedido` includes `Baixar estoque`
- `ROLE_CAIXA` -> `Operar PDV`, `Abrir e fechar caixa`, `Finalizar pedido`
- `ROLE_VENDEDOR` -> `Gerenciar clientes CRM`, `Buscar e filtrar produtos`
- `ROLE_FINANCEIRO` -> `Executar AdmCalc`, `Emitir nota fiscal`
- `ROLE_DESPACHANTE` -> `Despachar pedido aprovado`
- `ROLE_DONO_GERENTE` -> todos os casos operacionais internos
- `ROLE_SYSADMIN` -> `Auditar logs e tenants`, `Gerenciar usuarios e perfis`
- `CatalogEnrichmentJob` -> `Enriquecer imagem do produto`
- `Enriquecer imagem do produto` uses `Gateway/API de Busca Autorizada`

## 2. Diagramas de Casos de Uso por Role

### ROLE_SYSADMIN

- `Auditar logs globais`
- `Configurar tenants`
- `Validar licencas`
- `Gerenciar usuarios e roles`
- `Inspecionar healthchecks`

### ROLE_DONO_GERENTE

- `Consultar DRE`
- `Consultar fluxo de caixa consolidado`
- `Gerenciar estoque global`
- `Gerenciar acessos da equipe`
- `Aprovar rotinas financeiras`

### ROLE_FINANCEIRO

- `Executar calculos AdmCalc`
- `Controlar pagamentos`
- `Emitir NF-e/NFC-e`
- `Consultar relatorios contabeis`

### ROLE_VENDEDOR

- `Consultar catalogo B2B`
- `Cadastrar cliente CRM`
- `Consultar comissao propria`
- `Criar pedido assistido`

### ROLE_CAIXA

- `Abrir turno`
- `Fechar turno`
- `Liquidar venda`
- `Emitir comprovante`

### ROLE_DESPACHANTE

- `Visualizar pedidos aprovados`
- `Separar pedido`
- `Embalar pedido`
- `Atualizar status de entrega`

## 3. Diagrama de Classes DDD

Pacotes:

- `catalog.domain`
- `sales.domain`
- `identity.domain`
- `finance.domain`
- `shared.infrastructure`

Classes principais:

- `Produto`
  - atributos: `idProduto`, `descricao`, `marca`, `modelo`, `imagemUrl`, `custo`, `valorVenda`, `quantidadeEstoque`, `status`
  - metodos: `isDisponivel()`, `baixarEstoque(qtd)`, `vincularImagem(url, mimeType)`
- `CatalogEnrichmentService`
  - metodos: `enrichAsync(produtoId)`, `enrich(produtoId)`, `downloadCompressAndStore(produto, imageUrl)`
- `B2cCart`
  - atributos: `idB2cCart`, `cliente`, `usuario`, `status`, `itens`
  - metodos: `replaceItems(itens)`, `subtotal()`
- `B2cCartItem`
  - atributos: `produto`, `quantidade`, `precoUnitarioSnapshot`
- `Venda`
  - atributos: `idVenda`, `usuario`, `cliente`, `itens`, `pagamentos`, `valorTotal`, `status`
  - metodos: `calcularTotal()`, `cancelar()`
- `VendaService`
  - metodos: `realizarVenda(venda)`, `cancelarVenda(id)`, `applyAuthenticatedB2cIdentity(venda)`
- `Usuario`
  - atributos: `idUsuario`, `login`, `senha`, `perfil`, `status`
- `Perfil`
  - atributos: `idPerfil`, `nome`, `status`
- `Cliente`
  - atributos: `idCliente`, `usuario`, `nomeRazao`, `cpfCnpj`, `status`
- `FinancialCalculation`
  - atributos: `tipoCalculo`, `inputHash`, `resultadoSnapshot`, `atorLogin`
- `FinancialDigitalDocument`
  - atributos: `status`, `conteudoHash`, `assinaturaDigitalHash`

Relacionamentos:

- `Cliente` 0..1 -- 1 `Usuario`
- `Usuario` * -- 1 `Perfil`
- `B2cCart` 1 -- * `B2cCartItem`
- `B2cCartItem` * -- 1 `Produto`
- `Venda` 1 -- * `ProdutoVenda`
- `ProdutoVenda` * -- 1 `Produto`
- `Venda` * -- 1 `Cliente`
- `Venda` * -- 1 `Usuario`
- `CatalogEnrichmentService` depends on `ProdutoRepository`
- `VendaService` depends on `VendaRepository`, `ProdutoRepository`, `ClienteRepository`, `UsuarioRepository`

## 4. Sequencia - CatalogEnrichmentService

Lifelines:

- `ProdutoController`
- `ProdutoService`
- `ProdutoRepository`
- `CatalogEnrichmentService`
- `Provider API Autorizada`
- `Internal Storage`

Mensagens:

1. `ProdutoController -> ProdutoService: salvar(produto)`
2. `ProdutoService -> ProdutoRepository: save(produto)`
3. `ProdutoService -> CatalogEnrichmentService: enrichAsync(produtoId)`
4. `CatalogEnrichmentService -> ProdutoRepository: findById(produtoId)`
5. `CatalogEnrichmentService -> Provider API Autorizada: GET search?q=marca+modelo+descricao`
6. `Provider API Autorizada -> CatalogEnrichmentService: JSON com imageUrl`
7. `CatalogEnrichmentService -> Provider API Autorizada: GET imageUrl`
8. `CatalogEnrichmentService -> Internal Storage: salvar JPEG comprimido`
9. `CatalogEnrichmentService -> ProdutoRepository: save(produto.imagemUrl, status=READY)`

Fluxos alternativos:

- Sem provider configurado: status `NO_PROVIDER`
- Imagem nao encontrada: status `NOT_FOUND`
- URL insegura/local/private IP: descartar candidato e tentar proximo provider

## 5. Sequencia - Checkout Interceptor B2C

Lifelines:

- `Visitante B2C`
- `Angular/Ionic CartService`
- `AuthGuard`
- `LoginPage`
- `AuthController`
- `B2cCartController`
- `VendaController`
- `VendaService`
- `ProdutoRepository`
- `MySQL`

Mensagens:

1. `Visitante B2C -> CartService: addProduct(produto)`
2. `Visitante B2C -> AuthGuard: navegar /checkout`
3. `AuthGuard -> LoginPage: redirect /login?redirect=/checkout`
4. `LoginPage -> AuthController: POST /api/auth/login`
5. `AuthController -> LoginPage: JWT com ROLE_CLIENTE_B2C`
6. `LoginPage -> AuthGuard: voltar /checkout`
7. `CheckoutPage -> B2cCartController: POST /api/b2c/cart/merge`
8. `B2cCartController -> MySQL: persistir carrinho do cliente autenticado`
9. `CheckoutPage -> VendaController: POST /api/vendas`
10. `VendaController -> VendaService: realizarVenda(venda)`
11. `VendaService -> ProdutoRepository: lock produtos por id`
12. `VendaService -> MySQL: baixa estoque + grava venda`
13. `VendaController -> CheckoutPage: 201 Created`

Regra PoLP:

- Antes do passo 3, vitrine e carrinho local sao publicos.
- A partir do passo 7, JWT e role sao obrigatorios.
- Se o usuario e `ROLE_CLIENTE_B2C`, o backend ignora `clienteId` enviado e resolve o cliente vinculado ao proprio login.
