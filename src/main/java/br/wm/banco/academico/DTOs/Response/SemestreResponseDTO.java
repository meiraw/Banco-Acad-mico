package br.wm.banco.academico.DTOs.Response;

import br.wm.banco.academico.Model.SemestreModel;

import java.util.UUID;

public class SemestreResponseDTO {

    private UUID id;
    private Integer numero;
    private String nome;
    private UUID cursoId;

    public SemestreResponseDTO(SemestreModel model){
        this.id = model.getId();
        this.numero = model.getNumero();
        this.nome = model.getNome();
        this.cursoId = model.getCurso().getId();
    }

    public UUID getId() {
        return id;
    }

    public Integer getNumero() {
        return numero;
    }

    public String getNome() {
        return nome;
    }

    public UUID getCursoId() {
        return cursoId;
    }
}
