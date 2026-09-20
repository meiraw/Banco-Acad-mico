package br.wm.banco.academico.DTOs.Response;

import br.wm.banco.academico.Model.MaterialModel;


import java.util.UUID;

public class MaterialResponseDTO {

    private UUID id;
    private String titulo;
    private String descricao;
    private String nomeArquivo;
    private String tipoArquivo;
    private String caminhoArquivo;
    private Long tamanhoArquivo;
    private UUID disciplinaId;

    public MaterialResponseDTO(MaterialModel model){
        this.id = model.getId();
        this.titulo = model.getTitulo();
        this.descricao = model.getDescricao();
        this.nomeArquivo = model.getNomeArquivo();
        this.tipoArquivo = model.getTipoArquivo();
        this.caminhoArquivo = model.getCaminhoArquivo();
        this.tamanhoArquivo = model.getTamanhoArquivo();
        this.disciplinaId = model.getDisciplina().getId();
    }


    public UUID getId(){
        return id;
    }
    public String getTitulo(){
        return titulo;
    }

    public String getDescricao(){
        return descricao;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public String getTipoArquivo() {
        return tipoArquivo;
    }

    public String getCaminhoArquivo() {
        return caminhoArquivo;
    }

    public Long getTamanhoArquivo() {
        return tamanhoArquivo;
    }

    public UUID getDisciplinaId() {
        return disciplinaId;
    }
}
