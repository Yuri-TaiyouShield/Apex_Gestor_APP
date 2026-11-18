package Service;

import Model.Perfil;
import Model.Usuario;
import Repository.PerfilRepository;
import Repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private PerfilRepository perfilRepository;

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public List<Usuario> listarTodos(Long empresaId, Long filialId) {
        return usuarioRepository.findByEmpresaIdAndFilialIdOrderByNomeAsc(empresaId, filialId);
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    public Optional<Usuario> buscarPorId(Long id, Long empresaId, Long filialId) {
        return usuarioRepository.findByIdUsuarioAndEmpresaIdAndFilialId(id, empresaId, filialId);
    }

    public Optional<Usuario> buscarPorLogin(String login) {
        return usuarioRepository.findByLogin(login);
    }

    public Usuario salvar(Usuario usuario) {
        return salvar(usuario, usuario.getEmpresaId(), usuario.getFilialId());
    }

    public Usuario salvar(Usuario usuario, Long empresaId, Long filialId) {
        Long empresaAtual = empresaId == null ? 1L : empresaId;
        Long filialAtual = filialId == null ? 1L : filialId;
        if (usuario.getNome() == null || usuario.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome do usuario e obrigatorio.");
        }
        if (usuario.getLogin() == null || usuario.getLogin().isBlank()) {
            throw new IllegalArgumentException("Login do usuario e obrigatorio.");
        }
        Optional<Usuario> anterior = usuario.getIdUsuario() == null
                ? Optional.empty()
                : buscarPorId(usuario.getIdUsuario(), empresaAtual, filialAtual);
        if (usuario.getIdUsuario() != null && anterior.isEmpty()) {
            throw new EntityNotFoundException("Usuario nao encontrado no escopo da empresa/filial.");
        }
        if (usuario.getSenha() == null || usuario.getSenha().isBlank()) {
            usuario.setSenha(anterior.map(Usuario::getSenha)
                    .orElseThrow(() -> new IllegalArgumentException("Senha inicial e obrigatoria.")));
        } else if (!isEncoded(usuario.getSenha())) {
            validarSenhaForte(usuario.getSenha());
            usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        }
        if (usuario.getDataNascimento() == null) {
            usuario.setDataNascimento(anterior.map(Usuario::getDataNascimento).orElse(LocalDate.of(2000, 1, 1)));
        }
        if (usuario.getIdUsuario() == null && usuario.getStatus() == 0) {
            usuario.setStatus(1);
        }
        if (usuario.getPerfil() == null || usuario.getPerfil().getIdPerfil() == null) {
            usuario.setPerfil(resolvePerfilPadrao());
        }
        usuario.setEmpresaId(empresaAtual);
        usuario.setFilialId(filialAtual);
        return usuarioRepository.save(usuario);
    }

    public Usuario desativar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nao encontrado"));
        return desativar(usuario, usuario.getEmpresaId(), usuario.getFilialId());
    }

    public Usuario desativar(Long id, Long empresaId, Long filialId) {
        Usuario usuario = buscarPorId(id, empresaId, filialId)
                .orElseThrow(() -> new EntityNotFoundException("Nao encontrado"));
        return desativar(usuario, empresaId, filialId);
    }

    private Usuario desativar(Usuario usuario, Long empresaId, Long filialId) {
        usuario.setStatus(0);
        usuario.setEmpresaId(empresaId);
        usuario.setFilialId(filialId);
        return usuarioRepository.save(usuario);
    }

    private void validarSenhaForte(String senha) {
        boolean forte = senha.length() >= 8
                && senha.matches(".*[A-Z].*")
                && senha.matches(".*[a-z].*")
                && senha.matches(".*\\d.*")
                && senha.matches(".*[^A-Za-z0-9].*");
        if (!forte) {
            throw new IllegalArgumentException(
                    "A senha deve ter ao menos 8 caracteres, letra maiuscula, minuscula, numero e simbolo.");
        }
    }

    private boolean isEncoded(String senha) {
        return senha.startsWith("$2a$") || senha.startsWith("$2b$") || senha.startsWith("$2y$");
    }

    private Perfil resolvePerfilPadrao() {
        return perfilRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    Perfil perfil = new Perfil();
                    perfil.setNome("Operador");
                    perfil.setStatus(1);
                    return perfilRepository.save(perfil);
                });
    }
}
