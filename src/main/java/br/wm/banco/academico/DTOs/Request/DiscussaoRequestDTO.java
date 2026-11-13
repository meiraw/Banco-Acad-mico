package br.wm.banco.academico.DTOs.Request;
import jakarta.validation.constraints.*;
import java.util.UUID;
public record DiscussaoRequestDTO(
    @NotBlank(message = "Informe o título.") @Size(max = 200, message = "O título deve ter até 200 caracteres.") String titulo,
    @NotNull(message = "Selecione uma disciplina.") UUID disciplinaId,
    @NotBlank(message = "Escreva o conteúdo da discussão.") @Size(max = 10000, message = "O conteúdo deve ter até 10.000 caracteres.") String conteudo,
    @PositiveOrZero(message = "Versão inválida.") Long versao
) {}
