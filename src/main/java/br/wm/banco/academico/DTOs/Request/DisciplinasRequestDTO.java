package br.wm.banco.academico.DTOs.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
public class DisciplinasRequestDTO {


    @Setter
    @NotBlank(message = " O nome não pode ser vazio!")
    private String nome;

    @Setter
    @NotBlank(message = "A descrição não pode ser vazio!")
    private String descricao;

    public DisciplinasRequestDTO(String nome , String descricao){
        this.nome = nome;
        this.descricao = descricao;
    }


}
