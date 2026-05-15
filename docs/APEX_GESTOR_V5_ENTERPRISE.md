# Apex Gestor v5.0 Enterprise

## Objetivo

A v5 separa formalmente a jornada B2C de compra da operacao interna B2B. A vitrine, filtros, favoritos e carrinho local continuam publicos. A autenticacao entra apenas no checkout, quando o carrinho local e mesclado ao banco e convertido em venda.

## RBAC v5

| Role | Escopo |
| --- | --- |
| `ROLE_SYSADMIN` | Infraestrutura, tenants, licencas, logs e configuracoes globais. |
| `ROLE_DONO_GERENTE` | DRE, caixa consolidado, estoque global, equipe e relatorios. |
| `ROLE_FINANCEIRO` | AdmCalc, pagamentos, notas fiscais e contabilidade. |
| `ROLE_VENDEDOR` | Catalogo B2B, CRM basico e comissionamento proprio. |
| `ROLE_CAIXA` | PDV, abertura/fechamento de turno e liquidacao de vendas. |
| `ROLE_DESPACHANTE` | Picking, packing e despacho de pedidos aprovados. |
| `ROLE_CLIENTE_B2C` | Checkout B2C autenticado e carrinho persistido do proprio cliente. |

Senha dos usuarios seed v5: `Apex@2026`.

## APIs principais

- `GET /api/produtos/**`: publico para vitrine B2C.
- `GET /api/categorias/**`: publico para filtros.
- `POST /api/b2c/cart/merge`: autenticado, persiste o carrinho do cliente B2C.
- `POST /api/vendas`: autenticado; para `ROLE_CLIENTE_B2C`, o backend resolve o cliente pelo login autenticado.
- `POST /api/catalogo/enriquecimento/produtos/{id}`: roles administrativas/financeiras para enriquecer imagem.

## CatalogEnrichmentService

O servico usa provedores configuraveis e autorizados por variavel de ambiente:

```env
APEX_CATALOG_ENRICHMENT_PROVIDER_URLS=https://catalog-api.exemplo.com/search?q={query}
APEX_CATALOG_STORAGE_DIR=/var/apex/catalog
APEX_CATALOG_PUBLIC_PATH=/media/catalog
```

Fluxo:

1. Produto e salvo com `marca`, `modelo` e `descricao`.
2. `ProdutoService` chama `CatalogEnrichmentService.enrichAsync`.
3. O servico consulta provedores HTTP(S) autorizados.
4. A primeira imagem valida e baixada, comprimida em JPEG e salva no storage interno.
5. `Produto.imagemUrl` passa a apontar para `/media/catalog/products/{id}/product-{id}.jpg`.

Protecoes:

- Bloqueio de URLs locais, loopback, link-local, multicast e redes privadas.
- Limite de 5 MB por imagem.
- Aceita apenas `content-type: image/*`.
- Sem hardcode de chaves; provedores entram por variaveis de ambiente.

## Artefatos visuais

- DER Workbench: `docs/workbench/apex_gestor_v5_der.sql`
- UML Astah: `docs/astah/APEX_GESTOR_V5_ASTAH_UML.md`
