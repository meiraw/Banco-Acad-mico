package br.wm.banco.academico.DTOs.Response;
import java.time.Instant;
import java.util.UUID;
public record RespostaForumResponseDTO(UUID id, UUID discussaoId, UUID autorId, String autorNome,
    String conteudo, Instant criadoEm, Instant atualizadoEm, long versao) {}
