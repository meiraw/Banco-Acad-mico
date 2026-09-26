package br.wm.banco.academico.DTOs.Response;

import br.wm.banco.academico.Model.SemestreModel;

import java.util.UUID;

public class SemestreResponseDTO {

    private UUID id;
    private Integer ano;
    private Integer periodo;

    public SemestreResponseDTO(SemestreModel model){
        this.id = model.getId();
        this.ano = model.getAno();
        this.periodo = model.getPeriodo();
    }

    public UUID getId() {
        return id;
    }

    public Integer getAno() {
        return ano;
    }

    public Integer getPeriodo() {
        return periodo;
    }
}
