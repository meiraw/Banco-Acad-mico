package br.wm.banco.academico.DTOs.Request;


import br.wm.banco.academico.Enum.PerfilUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Cadastro publico
//Ao que o usuario manda para o back end
@NoArgsConstructor
@Getter
public class UsuarioRequestDTO {
    @Setter
    @NotBlank(message = "O nome não pode ser vazio")
    private String nome;

    @Setter
    @NotBlank(message = "O email não pode ser vazio")
    @Email(message = "Email inválido")
    private String email; // verifica se o formato parece um e-mail válido.

    @Setter
    @NotBlank(message = "A senha não pode ser vazia")
    @Size(min = 8, message = "A senha deve possuir no mínimo 8 caracteres")
    private String senha;

    public UsuarioRequestDTO(String nome, String email, String senha) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
    }
}