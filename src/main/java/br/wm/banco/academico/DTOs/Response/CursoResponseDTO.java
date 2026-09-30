package br.wm.banco.academico.DTOs.Response;
import br.wm.banco.academico.Model.CursoModel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

public class CursoResponseDTO {

    private UUID id;
    private String nome;

    public CursoResponseDTO (CursoModel model){
        this.id = model.getId();
        this.nome = model.getNome();
    }

    public UUID getId(){
        return id;
    }

    public String getNome(){
        return nome;
    }
}
