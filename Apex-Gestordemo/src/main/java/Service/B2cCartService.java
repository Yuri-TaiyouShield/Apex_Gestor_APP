package Service;

import DTO.B2cCartMergeItemDTO;
import DTO.B2cCartMergeRequestDTO;
import DTO.B2cCartMergeResponseDTO;
import Model.B2cCart;
import Model.B2cCartItem;
import Model.Cliente;
import Model.Produto;
import Repository.B2cCartRepository;
import Repository.ClienteRepository;
import Repository.ProdutoRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class B2cCartService {

    private static final String STATUS_OPEN = "OPEN";

    private final B2cCartRepository cartRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;

    public B2cCartService(B2cCartRepository cartRepository, ClienteRepository clienteRepository, ProdutoRepository produtoRepository) {
        this.cartRepository = cartRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
    }

    @Transactional
    public B2cCartMergeResponseDTO merge(B2cCartMergeRequestDTO request) {
        Cliente cliente = resolveCliente(request.clienteId());
        B2cCart cart = cartRepository.findByClienteIdClienteAndStatus(request.clienteId(), STATUS_OPEN)
                .orElseGet(() -> newCart(cliente));

        Map<Long, Produto> produtos = produtoRepository.findByIdProdutoIn(request.itens().stream()
                        .map(B2cCartMergeItemDTO::produtoId)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(Produto::getIdProduto, Function.identity()));

        cart.getItens().clear();
        cart.setAtualizadoEm(LocalDateTime.now());
        for (B2cCartMergeItemDTO item : request.itens()) {
            Produto produto = produtos.get(item.produtoId());
            if (produto == null || produto.getStatus() == 0) {
                throw new IllegalArgumentException("Produto indisponivel no catalogo.");
            }
            B2cCartItem cartItem = new B2cCartItem();
            cartItem.setCart(cart);
            cartItem.setProduto(produto);
            cartItem.setQuantidade(item.quantidade());
            cartItem.setPrecoUnitarioSnapshot(produto.getValorVenda() == null ? BigDecimal.ZERO : produto.getValorVenda());
            cart.getItens().add(cartItem);
        }

        B2cCart saved = cartRepository.save(cart);
        int totalItems = saved.getItens().stream().mapToInt(B2cCartItem::getQuantidade).sum();
        BigDecimal subtotal = saved.getItens().stream()
                .map(item -> item.getPrecoUnitarioSnapshot().multiply(BigDecimal.valueOf(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new B2cCartMergeResponseDTO(saved.getIdB2cCart(), cliente.getIdCliente(), totalItems, subtotal, saved.getStatus());
    }

    private Cliente resolveCliente(Long requestedClienteId) {
        String login = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isB2cCustomer = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> "ROLE_CLIENTE_B2C".equals(authority.getAuthority()));
        if (isB2cCustomer) {
            return clienteRepository.findByUsuarioLogin(login)
                    .orElseThrow(() -> new AccessDeniedException("Cliente B2C nao vinculado ao usuario autenticado."));
        }
        return clienteRepository.findById(requestedClienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente B2C nao encontrado."));
    }

    private B2cCart newCart(Cliente cliente) {
        B2cCart cart = new B2cCart();
        cart.setCliente(cliente);
        cart.setStatus(STATUS_OPEN);
        cart.setCriadoEm(LocalDateTime.now());
        cart.setAtualizadoEm(LocalDateTime.now());
        cart.setItens(new ArrayList<>());
        return cart;
    }
}
