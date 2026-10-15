package br.wm.banco.academico.Service;

import br.wm.banco.academico.DTOs.Request.LoginRequestDTO;
import br.wm.banco.academico.DTOs.Request.UsuarioRequestDTO;
import br.wm.banco.academico.Enum.PerfilUsuario;
import br.wm.banco.academico.Exception.RegraNegocioException;
import br.wm.banco.academico.Exception.ResourceNotFoundException;
import br.wm.banco.academico.Model.Usuario.UsuarioModel;
import br.wm.banco.academico.Repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service

public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ConfirmacaoEmailService confirmacao;
    private final PasswordEncoder passwordEncoder; //Colocamos a injeção de dependencia de password da senha

    public UsuarioService (UsuarioRepository usuarioRepository , PasswordEncoder passwordEncoder, ConfirmacaoEmailService confirmacao ){
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.confirmacao = confirmacao;
    }

    //Nosso criar possui : E-mail não repetido , BCrypt e perfil USER automatico
    @org.springframework.transaction.annotation.Transactional
    public UsuarioModel criar (UsuarioRequestDTO dto ){

        dto.setEmail(dto.getEmail().trim().toLowerCase(java.util.Locale.ROOT));

        if (usuarioRepository.findByEmail(dto.getEmail()).isPresent()) { // Aqui verificamos se o email do usuario já existe , para não ter um possivel cadastro no mesmo email.
            throw new RegraNegocioException("Este email já está cadastrado!"); //aplicamos aqui também o uso do  optional , aplicado no repository do usuario
        }

        UsuarioModel usuario = new UsuarioModel();

        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());


        String senhaCriptografada = passwordEncoder.encode(dto.getSenha());
        usuario.setSenha(senhaCriptografada);

        usuario.setPerfil(PerfilUsuario.USER);

        UsuarioModel salvo = usuarioRepository.saveAndFlush(usuario);
        return salvo;
    }

    public UsuarioModel login (LoginRequestDTO dto){
        UsuarioModel usuario = usuarioRepository.findByEmail(dto.getEmail().trim().toLowerCase(java.util.Locale.ROOT)).orElseThrow(() -> new RegraNegocioException("Email ou senha inválidos!"));

        boolean senhaCorreta = passwordEncoder.matches(
                dto.getSenha(),
                usuario.getSenha()
        );

        if (!senhaCorreta) {
            throw new RegraNegocioException("Email ou senha inválidos!");
        }

        return usuario;
    }

    public Page<UsuarioModel> listar (Pageable pageable  ){
        return usuarioRepository.findAll(pageable);
    }

    public UsuarioModel buscarId(UUID id){
        return usuarioRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("O id "+id+" não foi encontrado!"));
    }

    @org.springframework.transaction.annotation.Transactional
    public UsuarioModel atualizar(UsuarioRequestDTO dto, UUID id){

        UsuarioModel usuario = buscarId(id); // buscar o id do usuario já cadastrado

        String email = dto.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
        boolean mudouEmail = !usuario.getEmail().equals(email);
        if (mudouEmail) {
            usuario.setEmailConfirmado(false);
            usuario.setVersaoCredenciais(usuario.getVersaoCredenciais() + 1);
        }
        usuario.setNome(dto.getNome());
        usuario.setEmail(email);

        String senhaCriptografada = passwordEncoder.encode(dto.getSenha());
        usuario.setSenha(senhaCriptografada);

        UsuarioModel salvo = usuarioRepository.saveAndFlush(usuario);
        if (mudouEmail) {
            confirmacao.revogarRecuperacoes(salvo.getId());
        }
        return salvo;
    }

    public void excluir (UUID id ){
        UsuarioModel deletar = buscarId(id);
        usuarioRepository.delete(deletar);
    }
}
