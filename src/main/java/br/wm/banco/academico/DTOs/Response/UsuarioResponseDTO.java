package br.wm.banco.academico.DTOs.Response;

import br.wm.banco.academico.Enum.PerfilUsuario;
import br.wm.banco.academico.Model.Usuario.UsuarioModel;

import java.util.UUID;

public class UsuarioResponseDTO {

    // O que o back pode devolver para o  front-end
    private UUID id;
    private String nome;
    private String email;
    private PerfilUsuario perfil;
    private String curso;
    private Integer semestreAtual;
    private boolean possuiFoto;

    public UsuarioResponseDTO(UsuarioModel model){
        this.id = model.getId();
        this.nome = model.getNome();
        this.email = model.getEmail();
        this.perfil = model.getPerfil();
        this.curso = model.getCurso();
        this.semestreAtual = model.getSemestreAtual();
    }

    public UsuarioResponseDTO(UsuarioModel model, boolean possuiFoto) {
        this(model);
        this.possuiFoto = possuiFoto;
    }

    public boolean isPossuiFoto() { return possuiFoto; }

    public UUID getId (){
        return id;
    }

    public String getNome (){
        return nome;
    }

    public String getEmail (){
        return email;
    }

    public PerfilUsuario getPerfil(){
        return perfil;
    }

    public String getCurso() { return curso; }

    public Integer getSemestreAtual() { return semestreAtual; }

    //Tiramos a senha pelo fato de não devolver ao front end, por além de ser sensivel
    // O perfil implementamos para o front end entender quais são as permissões que aquele usuario possui.

}
