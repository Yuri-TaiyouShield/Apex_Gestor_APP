package Controller;

import DTO.CategoriaResumoDTO;
import DTO.FornecedorResumoDTO;
import DTO.ProdutoRequestDTO;
import DTO.ProdutoResponseDTO;
import Model.Categoria;
import Model.Fornecedor;
import Model.Produto;
import Service.AuditTrailService;
import Service.AuthorizationService;
import Service.ProdutoService;
import Service.PrivacyGuardService;
import Service.TenantScopeService;
import com.Apex.Apex_Gestordemo.security.ApexPermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    @Autowired
    private ProdutoService produtoService;
    @Autowired
    private PrivacyGuardService privacyGuardService;
    @Autowired
    private AuthorizationService authorizationService;
    @Autowired
    private AuditTrailService auditTrailService;
    @Autowired
    private TenantScopeService tenantScopeService;

    @GetMapping
    public List<ProdutoResponseDTO> listarTodos(HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.PRODUCT_VIEW, "Produtos", "listar produtos");
        boolean fullAccess = privacyGuardService.canViewSensitiveData(request);
        return produtoService.listarTodos(empresaId(request), filialId(request)).stream()
                .map(produto -> toResponse(produto, fullAccess))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> buscarPorId(@PathVariable Long id, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.PRODUCT_VIEW, "Produtos", "consultar produto");
        boolean fullAccess = privacyGuardService.canViewSensitiveData(request);
        return produtoService.buscarPorId(id, empresaId(request), filialId(request))
                .map(produto -> ResponseEntity.ok(toResponse(produto, fullAccess)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ProdutoResponseDTO> criar(@Valid @RequestBody ProdutoRequestDTO requestBody,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.PRODUCT_CREATE, "Produtos", "criar produto");
        requirePricePermission(request, null, requestBody.valorVenda());
        Produto produto = toEntity(requestBody, new Produto());
        Produto salvo = produtoService.salvar(produto, empresaId(request), filialId(request));
        auditTrailService.registrarPrivacidade(
                "PRODUTO_CRIADO",
                "Produto",
                salvo.getIdProduto(),
                "Produtos",
                null,
                resumoProduto(salvo),
                request,
                "Cadastro operacional de produto.");
        return new ResponseEntity<>(toResponse(salvo, privacyGuardService.canViewSensitiveData(request)),
                HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> atualizar(@PathVariable Long id,
            @Valid @RequestBody ProdutoRequestDTO requestBody,
            HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.PRODUCT_UPDATE, "Produtos", "atualizar produto");
        return produtoService.buscarPorId(id, empresaId(request), filialId(request))
                .map(existente -> {
                    requirePricePermission(request, existente.getValorVenda(), requestBody.valorVenda());
                    String anterior = resumoProduto(existente);
                    Produto salvo = produtoService.salvar(toEntity(requestBody, existente), empresaId(request), filialId(request));
                    auditTrailService.registrarPrivacidade(
                            "PRODUTO_ATUALIZADO",
                            "Produto",
                            salvo.getIdProduto(),
                            "Produtos",
                            anterior,
                            resumoProduto(salvo),
                            request,
                            "Atualizacao operacional de produto.");
                    return ResponseEntity.ok(toResponse(salvo, privacyGuardService.canViewSensitiveData(request)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<ProdutoResponseDTO> desativar(@PathVariable Long id, HttpServletRequest request) {
        authorizationService.require(request, ApexPermission.PRODUCT_DISABLE, "Produtos", "desativar produto");
        try {
            Produto salvo = produtoService.desativar(id, empresaId(request), filialId(request));
            auditTrailService.registrarPrivacidade(
                    "PRODUTO_DESATIVADO",
                    "Produto",
                    salvo.getIdProduto(),
                    "Produtos",
                    null,
                    resumoProduto(salvo),
                    request,
                    "Inativacao operacional de produto.");
            return ResponseEntity.ok(toResponse(salvo, privacyGuardService.canViewSensitiveData(request)));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private Produto toEntity(ProdutoRequestDTO request, Produto produto) {
        produto.setDescricao(request.descricao());
        produto.setCusto(request.custo());
        produto.setCodigoBarras(request.codigoBarras());
        produto.setCodigoInterno(request.codigoInterno());
        produto.setCodigoOriginal(request.codigoOriginal());
        produto.setMarca(request.marca());
        produto.setGrupo(request.grupo());
        produto.setSubgrupo(request.subgrupo());
        produto.setUnidadeMedida(request.unidadeMedida());
        produto.setEmbalagem(request.embalagem());
        produto.setDataAquisicao(request.dataAquisicao());
        produto.setQuantidadeEstoque(request.quantidadeEstoque() == null ? 0 : request.quantidadeEstoque());
        produto.setEstoqueMinimo(request.estoqueMinimo() == null ? 0 : request.estoqueMinimo());
        produto.setValorVenda(request.valorVenda());
        produto.setMargem(request.margem());
        produto.setLocalizacao(request.localizacao());
        produto.setSituacaoFiscal(request.situacaoFiscal());
        produto.setNcm(request.ncm());
        produto.setCfop(request.cfop());
        produto.setIcms(request.icms());
        produto.setIpi(request.ipi());
        produto.setPis(request.pis());
        produto.setCofins(request.cofins());
        produto.setObservacoes(request.observacoes());
        produto.setStatus(request.status() == null ? 1 : request.status());
        if (request.categoria() == null || request.categoria().categoriaId() == null) {
            throw new IllegalArgumentException("Categoria do produto e obrigatoria.");
        }
        if (request.fornecedor() == null || request.fornecedor().fornecedorId() == null) {
            throw new IllegalArgumentException("Fornecedor do produto e obrigatorio.");
        }
        Categoria categoria = new Categoria();
        categoria.setIdCategoria(request.categoria().categoriaId());
        produto.setCategoria(categoria);
        Fornecedor fornecedor = new Fornecedor();
        fornecedor.setIdFornecedor(request.fornecedor().fornecedorId());
        produto.setFornecedor(fornecedor);
        return produto;
    }

    private void requirePricePermission(HttpServletRequest request, Object previousPrice, Object nextPrice) {
        if (!Objects.equals(previousPrice, nextPrice)) {
            authorizationService.require(request, ApexPermission.PRODUCT_PRICE_UPDATE, "Produtos", "alterar preco");
        }
    }

    private ProdutoResponseDTO toResponse(Produto produto, boolean fullAccess) {
        return new ProdutoResponseDTO(
                produto.getIdProduto(),
                produto.getDescricao(),
                produto.getCusto(),
                produto.getCodigoBarras(),
                produto.getCodigoInterno(),
                produto.getCodigoOriginal(),
                produto.getMarca(),
                produto.getGrupo(),
                produto.getSubgrupo(),
                produto.getUnidadeMedida(),
                produto.getEmbalagem(),
                produto.getDataAquisicao(),
                produto.getQuantidadeEstoque(),
                produto.getEstoqueMinimo(),
                produto.getValorVenda(),
                produto.getMargem(),
                produto.getLocalizacao(),
                produto.getSituacaoFiscal(),
                produto.getNcm(),
                produto.getCfop(),
                produto.getIcms(),
                produto.getIpi(),
                produto.getPis(),
                produto.getCofins(),
                produto.getObservacoes(),
                produto.getStatus(),
                produto.getEmpresaId(),
                produto.getFilialId(),
                fornecedorResumo(produto.getFornecedor(), fullAccess),
                categoriaResumo(produto.getCategoria()));
    }

    private FornecedorResumoDTO fornecedorResumo(Fornecedor fornecedor, boolean fullAccess) {
        if (fornecedor == null) {
            return null;
        }
        return new FornecedorResumoDTO(
                fornecedor.getIdFornecedor(),
                fornecedor.getRazaoSocial(),
                fornecedor.getNomeFantasia(),
                fullAccess ? fornecedor.getCnpj() : privacyGuardService.maskDocument(fornecedor.getCnpj()),
                fullAccess ? fornecedor.getTelefone() : privacyGuardService.maskPhone(fornecedor.getTelefone()));
    }

    private CategoriaResumoDTO categoriaResumo(Categoria categoria) {
        if (categoria == null) {
            return null;
        }
        return new CategoriaResumoDTO(categoria.getIdCategoria(), categoria.getDescricao(), categoria.getStatus());
    }

    private String resumoProduto(Produto produto) {
        if (produto == null) {
            return null;
        }
        return "id=" + produto.getIdProduto()
                + ";descricao=" + produto.getDescricao()
                + ";estoque=" + produto.getQuantidadeEstoque()
                + ";preco=" + produto.getValorVenda()
                + ";status=" + produto.getStatus();
    }

    private Long empresaId(HttpServletRequest request) {
        return tenantScopeService.empresaId(request);
    }

    private Long filialId(HttpServletRequest request) {
        return tenantScopeService.filialId(request);
    }
}
