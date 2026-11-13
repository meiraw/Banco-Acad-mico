package br.wm.banco.academico.DTOs.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@NoArgsConstructor
@Getter
public class MaterialRequestDTO {
    @Setter
    private Boolean responsabilidadeConfirmada;
    @Setter
    @NotNull(message = "Selecione o tipo de publicação.")
    private br.wm.banco.academico.Enum.TipoPublicacao tipoPublicacao;

    @Setter
    @Size(max = 500, message = "A referência bibliográfica deve ter até 500 caracteres.")
    private String referenciaBibliografica;

    @jakarta.validation.constraints.AssertTrue(message = "Informe a referência bibliográfica do artigo.")
    public boolean isReferenciaObrigatoriaPreenchida() {
        return tipoPublicacao != br.wm.banco.academico.Enum.TipoPublicacao.ARTIGO
                || (referenciaBibliografica != null && !referenciaBibliografica.isBlank());
    }

    @jakarta.validation.constraints.AssertTrue(message = "A referência bibliográfica só é permitida para Livro ou Artigo.")
    public boolean isReferenciaCompativel() {
        return referenciaBibliografica == null || referenciaBibliografica.isBlank()
                || tipoPublicacao == br.wm.banco.academico.Enum.TipoPublicacao.LIVRO
                || tipoPublicacao == br.wm.banco.academico.Enum.TipoPublicacao.ARTIGO;
    }

    @Setter
    @NotBlank(message = "O titulo não pode ser vazio")
    @Size(min = 2 , max = 100 , message = " O titulo tem que ser de 2 à 100 caracteres!")
    private String titulo;

    @Setter
    @NotBlank(message = "A descrição  não pode ser vazio")
    @Size(min = 2 , max = 100 , message = " A descrição  tem que ser de 2 à 100 caracteres!")
    private String descricao;


    @NotNull
    private UUID disciplinaId;

    public MaterialRequestDTO (String titulo , String descricao, UUID disciplinaId,
            br.wm.banco.academico.Enum.TipoPublicacao tipoPublicacao, String referenciaBibliografica){
        this.tipoPublicacao = tipoPublicacao;
        this.referenciaBibliografica = referenciaBibliografica;
        this.titulo = titulo;
        this.descricao = descricao;
        this.disciplinaId = disciplinaId;
    }
}
