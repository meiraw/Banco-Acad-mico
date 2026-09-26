package br.wm.banco.academico.DTOs.Request;


import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
public class SemestreRequestDTO {

    @Setter
    @NotNull
    private Integer ano;

    @Setter
    @NotNull
    private Integer periodo;

    public SemestreRequestDTO(Integer ano ,  Integer periodo){
        this.ano = ano;
        this.periodo = periodo;
    }
}
