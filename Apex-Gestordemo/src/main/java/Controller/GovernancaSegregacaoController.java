package Controller;

import DTO.PoliticaSegregacaoRequestDTO;
import DTO.PoliticaSegregacaoResponseDTO;
import DTO.ValidacaoSegregacaoRequestDTO;
import DTO.ValidacaoSegregacaoResponseDTO;
import Service.SegregacaoFuncoesService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/financeiro/governanca")
public class GovernancaSegregacaoController {

    private final SegregacaoFuncoesService segregacaoFuncoesService;

    public GovernancaSegregacaoController(SegregacaoFuncoesService segregacaoFuncoesService) {
        this.segregacaoFuncoesService = segregacaoFuncoesService;
    }

    @GetMapping("/politicas-sod")
    public List<PoliticaSegregacaoResponseDTO> listarPoliticas(HttpServletRequest request) {
        return segregacaoFuncoesService.listarPoliticas(request);
    }

    @PutMapping("/politicas-sod")
    public PoliticaSegregacaoResponseDTO salvarPolitica(
            @Valid @RequestBody PoliticaSegregacaoRequestDTO body,
            HttpServletRequest request) {
        return segregacaoFuncoesService.salvarPolitica(body, request);
    }

    @PostMapping("/validacao-aprovacao")
    public ValidacaoSegregacaoResponseDTO validarAprovacao(
            @Valid @RequestBody ValidacaoSegregacaoRequestDTO body,
            HttpServletRequest request) {
        return segregacaoFuncoesService.validarAprovacaoEndpoint(body, request);
    }
}
