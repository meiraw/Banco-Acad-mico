package br.wm.banco.academico.DTOs.Response;
import br.wm.banco.academico.Model.DisciplinasModel;

import java.util.UUID;

public class DisciplinasResponseDTO {

    private UUID id;
    private String nome;
    private String descricao;
    private UUID semestreId;

    public DisciplinasResponseDTO(DisciplinasModel model){

        this.id = model.getId();
        this.nome = model.getNome();
        this.descricao = model.getDescricao();
        this.semestreId = model.getSemestre().getId();
    }

    public String getDescricao() {
        return descricao;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public UUID getSemestreId() {
        return semestreId;
    }
}
