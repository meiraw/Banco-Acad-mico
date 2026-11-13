package br.wm.banco.academico.DTOs.Request;
import jakarta.validation.constraints.*;
public record RespostaForumRequestDTO(
    @NotBlank(message = "Escreva uma resposta.") @Size(max = 5000, message = "A resposta deve ter até 5.000 caracteres.") String conteudo,
    @PositiveOrZero(message = "Versão inválida.") Long versao
) {}
