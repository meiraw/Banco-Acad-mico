package br.wm.banco.academico.DTOs.Request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CadastroUsuarioRequestDTO {
    @NotBlank(message = "Informe seu nome completo.")
    @Size(max = 200, message = "O nome deve ter no máximo 200 caracteres.")
    private String nome;

    @NotBlank(message = "Informe seu e-mail.")
    @Email(message = "Informe um e-mail válido.")
    @Size(max = 200, message = "O e-mail deve ter no máximo 200 caracteres.")
    private String email;

    @NotNull(message = "Selecione seu semestre atual.")
    @Min(value = 1, message = "O semestre atual deve estar entre 1 e 8.")
    @Max(value = 8, message = "O semestre atual deve estar entre 1 e 8.")
    private Integer semestreAtual;

    @NotBlank(message = "Informe sua senha.")
    @Size(min = 8, message = "A senha deve possuir no mínimo 8 caracteres.")
    private String senha;
}
