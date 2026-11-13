package br.wm.banco.academico.DTOs.Response;

import br.wm.banco.academico.Model.MaterialModel;


import java.util.UUID;
import java.time.Instant;

public class MaterialResponseDTO {
    private final boolean arquivoValidado;
    public boolean isArquivoValidado() { return arquivoValidado; }
    private br.wm.banco.academico.Enum.TipoPublicacao tipoPublicacao;
    private String referenciaBibliografica;
    public br.wm.banco.academico.Enum.TipoPublicacao getTipoPublicacao() { return tipoPublicacao; }
    public String getReferenciaBibliografica() { return referenciaBibliografica; }
    private br.wm.banco.academico.Enum.StatusModeracao statusModeracao;
    private Instant decisaoEm;
    private UUID moderadorId;
    private String justificativaRejeicao;
    private String disciplinaNome;
    public br.wm.banco.academico.Enum.StatusModeracao getStatusModeracao() { return statusModeracao; }
    public Instant getDecisaoEm() { return decisaoEm; }
    public UUID getModeradorId() { return moderadorId; }
    public String getJustificativaRejeicao() { return justificativaRejeicao; }
    public String getDisciplinaNome() { return disciplinaNome; }
    private String autorNome;
    private Instant publicadoEm;
    public String getAutorNome() { return autorNome; }
    public Instant getPublicadoEm() { return publicadoEm; }

    private UUID id;
    private String titulo;
    private String descricao;
    private String nomeArquivo;
    private String tipoArquivo;
    private String caminhoArquivo;
    private Long tamanhoArquivo;
    private UUID disciplinaId;

    public MaterialResponseDTO(MaterialModel model){
        this.arquivoValidado = br.wm.banco.academico.Service.ArmazenamentoService.validado(model.getCaminhoArquivo());
        this.tipoPublicacao = model.getTipoPublicacao();
        this.referenciaBibliografica = model.getReferenciaBibliografica();
        this.statusModeracao = model.getStatusModeracao();
        this.decisaoEm = model.getDecisaoEm();
        this.moderadorId = model.getModerador() == null ? null : model.getModerador().getId();
        this.justificativaRejeicao = model.getJustificativaRejeicao();
        this.disciplinaNome = model.getDisciplina().getNome();
        this.autorNome = model.getAutor() == null ? null : model.getAutor().getNome();
        this.publicadoEm = model.getPublicadoEm();
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
