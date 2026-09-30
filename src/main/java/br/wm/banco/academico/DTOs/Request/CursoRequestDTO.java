package br.wm.banco.academico.DTOs.Request;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
public class CursoRequestDTO {

    @Setter
    @NotBlank(message = "O nome não pode ser nulo!")
    private String nome;

    public CursoRequestDTO (String nome ){
        this.nome = nome;
    }
}
