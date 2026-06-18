package Controller;

import Model.Notificacao;
import Repository.NotificacaoRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notificacoes")
@CrossOrigin(origins = "http://localhost:4200")
public class NotificacaoController {

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @GetMapping("/pendentes")
    public List<Notificacao> listarPendentes() {
        return notificacaoRepository.findByStatusOrderByCriadoEmDesc("PENDENTE");
    }
}
