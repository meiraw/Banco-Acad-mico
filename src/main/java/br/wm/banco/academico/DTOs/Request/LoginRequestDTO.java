package br.wm.banco.academico.DTOs.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
public class LoginRequestDTO {

    @Setter
    @NotBlank(message = "O email não pode ser vazio")
    @Email(message = "Email inválido")
    private String email;

    @Setter
    @NotBlank(message = "A senha não pode ser vazia")
    private String senha;

    public LoginRequestDTO(String email, String senha) {
        this.email = email;
        this.senha = senha;
    }

}
