package br.wm.banco.academico.DTOs.Request;
import jakarta.validation.constraints.*;

public record AtualizarPerfilRequestDTO(
    @NotNull(message = "Selecione seu semestre.")
    @Min(value = 1, message = "Selecione um semestre entre 1 e 8.")
    @Max(value = 8, message = "Selecione um semestre entre 1 e 8.") Integer semestreAtual
) {}
