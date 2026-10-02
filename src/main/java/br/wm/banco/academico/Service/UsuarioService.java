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
    private final PasswordEncoder passwordEncoder; //Colocamos a injeção de dependencia de password da senha

    public UsuarioService (UsuarioRepository usuarioRepository , PasswordEncoder passwordEncoder ){
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    //Nosso criar possui : E-mail não repetido , BCrypt e perfil USER automatico
    public UsuarioModel criar (UsuarioRequestDTO dto ){

        if (usuarioRepository.findByEmail(dto.getEmail()).isPresent()) { // Aqui verificamos se o email do usuario já existe , para não ter um possivel cadastro no mesmo email.
            throw new RegraNegocioException("Este email já está cadastrado!"); //aplicamos aqui também o uso do  optional , aplicado no repository do usuario
        }

        UsuarioModel usuario = new UsuarioModel();

        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());


        String senhaCriptografada = passwordEncoder.encode(dto.getSenha());
        usuario.setSenha(senhaCriptografada);

        usuario.setPerfil(PerfilUsuario.USER);

        return usuarioRepository.save(usuario);
    }

    public UsuarioModel login (LoginRequestDTO dto){
        UsuarioModel usuario = usuarioRepository.findByEmail(dto.getEmail()).orElseThrow(() -> new RegraNegocioException("Email ou senha inválidos!"));

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

    public UsuarioModel atualizar(UsuarioRequestDTO dto, UUID id){

        UsuarioModel usuario = buscarId(id); // buscar o id do usuario já cadastrado

        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());

        String senhaCriptografada = passwordEncoder.encode(dto.getSenha());
        usuario.setSenha(senhaCriptografada);

        return usuarioRepository.save(usuario);
    }

    public void excluir (UUID id ){
        UsuarioModel deletar = buscarId(id);
        usuarioRepository.delete(deletar);
    }
}
