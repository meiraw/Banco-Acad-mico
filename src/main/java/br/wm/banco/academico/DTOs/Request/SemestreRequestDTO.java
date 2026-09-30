package br.wm.banco.academico.DTOs.Request;


import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class SemestreRequestDTO {

    @Setter
    @NotNull
    private Integer numero;

    @Setter
    @NotNull
    private String nome ;

    @Setter
    @NotNull
    private UUID cursoId;

    public SemestreRequestDTO(Integer numero , String nome, UUID cursoId){
        this.numero = numero;
        this.nome = nome;
        this.cursoId = cursoId;
    }
}
