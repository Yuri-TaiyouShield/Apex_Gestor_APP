package Service;

import DTO.CondicaoPagamentoRequestDTO;
import DTO.CondicaoPagamentoResponseDTO;
import Model.CondicaoPagamento;
import Repository.CondicaoPagamentoRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CondicaoPagamentoService {

    @Autowired
    private CondicaoPagamentoRepository repository;
    @Autowired
    private TenantScopeService tenantScopeService;
    @Autowired
    private AuditTrailService auditTrailService;

    public List<CondicaoPagamentoResponseDTO> listar(HttpServletRequest request) {
        return repository.findByEmpresaIdAndFilialIdOrderByNomeAsc(empresaId(request), filialId(request)).stream()
                .map(this::toResponse)
                .toList();
    }

    public CondicaoPagamentoResponseDTO salvar(CondicaoPagamentoRequestDTO requestBody, HttpServletRequest request) {
        CondicaoPagamento condicao = toEntity(requestBody, new CondicaoPagamento(), request);
        CondicaoPagamento salva = repository.save(condicao);
        auditTrailService.registrarPrivacidade("CONDICAO_PAGAMENTO_CRIADA", "CondicaoPagamento",
                salva.getIdCondicaoPagamento(), "Financeiro", null, resumo(salva), request,
                "Criacao de condicao de pagamento.");
        return toResponse(salva);
    }

    public CondicaoPagamentoResponseDTO atualizar(Long id, CondicaoPagamentoRequestDTO requestBody,
            HttpServletRequest request) {
        CondicaoPagamento existente = repository
                .findByIdCondicaoPagamentoAndEmpresaIdAndFilialId(id, empresaId(request), filialId(request))
                .orElseThrow(() -> new EntityNotFoundException("Condicao de pagamento nao encontrada."));
        String anterior = resumo(existente);
        CondicaoPagamento salva = repository.save(toEntity(requestBody, existente, request));
        auditTrailService.registrarPrivacidade("CONDICAO_PAGAMENTO_ATUALIZADA", "CondicaoPagamento",
                salva.getIdCondicaoPagamento(), "Financeiro", anterior, resumo(salva), request,
                "Atualizacao de condicao de pagamento.");
        return toResponse(salva);
    }

    public CondicaoPagamentoResponseDTO inativar(Long id, HttpServletRequest request) {
        CondicaoPagamento existente = repository
                .findByIdCondicaoPagamentoAndEmpresaIdAndFilialId(id, empresaId(request), filialId(request))
                .orElseThrow(() -> new EntityNotFoundException("Condicao de pagamento nao encontrada."));
        String anterior = resumo(existente);
        existente.setStatus(0);
        CondicaoPagamento salva = repository.save(existente);
        auditTrailService.registrarPrivacidade("CONDICAO_PAGAMENTO_INATIVADA", "CondicaoPagamento",
                salva.getIdCondicaoPagamento(), "Financeiro", anterior, resumo(salva), request,
                "Inativacao de condicao de pagamento.");
        return toResponse(salva);
    }

    private CondicaoPagamento toEntity(CondicaoPagamentoRequestDTO requestBody, CondicaoPagamento entity,
            HttpServletRequest request) {
        entity.setNome(requestBody.nome());
        entity.setTipoPagamento(requestBody.tipoPagamento());
        entity.setQuantidadeParcelas(requestBody.quantidadeParcelas());
        entity.setPrazoDias(requestBody.prazoDias());
        entity.setPercentual(requestBody.percentual());
        entity.setRestricoes(requestBody.restricoes());
        entity.setStatus(requestBody.status() == null ? 1 : requestBody.status());
        entity.setEmpresaId(requestBody.empresaId() == null ? empresaId(request) : requestBody.empresaId());
        entity.setFilialId(requestBody.filialId() == null ? filialId(request) : requestBody.filialId());
        validarEscopo(entity, request);
        return entity;
    }

    private void validarEscopo(CondicaoPagamento entity, HttpServletRequest request) {
        if (!java.util.Objects.equals(entity.getEmpresaId(), empresaId(request))
                || !java.util.Objects.equals(entity.getFilialId(), filialId(request))) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Condicao pertence a outra empresa ou filial.");
        }
    }

    private CondicaoPagamentoResponseDTO toResponse(CondicaoPagamento condicao) {
        return new CondicaoPagamentoResponseDTO(
                condicao.getIdCondicaoPagamento(),
                condicao.getNome(),
                condicao.getTipoPagamento(),
                condicao.getQuantidadeParcelas(),
                condicao.getPrazoDias(),
                condicao.getPercentual(),
                condicao.getRestricoes(),
                condicao.getStatus(),
                condicao.getEmpresaId(),
                condicao.getFilialId());
    }

    private Long empresaId(HttpServletRequest request) {
        return tenantScopeService.empresaId(request);
    }

    private Long filialId(HttpServletRequest request) {
        return tenantScopeService.filialId(request);
    }

    private String resumo(CondicaoPagamento condicao) {
        return "id=" + condicao.getIdCondicaoPagamento()
                + ";nome=" + condicao.getNome()
                + ";tipo=" + condicao.getTipoPagamento()
                + ";parcelas=" + condicao.getQuantidadeParcelas()
                + ";status=" + condicao.getStatus();
    }
}
