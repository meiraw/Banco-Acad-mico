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
    @NotBlank(message = "O titulo não pode ser vazio")
    @Size(min = 2 , max = 100 , message = " O titulo tem que ser de 2 à 100 caracteres!")
    private String titulo;

    @Setter
    @NotBlank(message = "A descrição  não pode ser vazio")
    @Size(min = 2 , max = 100 , message = " A descrição  tem que ser de 2 à 100 caracteres!")
    private String descricao;

    @NotBlank(message = "O nome não pode ser vazio!")
    private String nomeArquivo;

    private String tipoArquivo;
    private String caminhoArquivo;
    private Long tamanhoArquivo;

    @NotNull
    private UUID disciplinaId;

    public MaterialRequestDTO (String titulo , String descricao,String nomeArquivo, String tipoArquivo , String caminhoArquivo , Long tamanhoArquivo){
        this.titulo = titulo;
        this.descricao = descricao;
        this.nomeArquivo = nomeArquivo;
        this.caminhoArquivo = caminhoArquivo;
        this.tamanhoArquivo = tamanhoArquivo;
        this.tipoArquivo = tipoArquivo;
    }
}
