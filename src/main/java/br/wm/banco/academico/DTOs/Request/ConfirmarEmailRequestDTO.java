package br.wm.banco.academico.DTOs.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConfirmarEmailRequestDTO(
    @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token) {}
