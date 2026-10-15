package br.wm.banco.academico.Controller;

import br.wm.banco.academico.DTOs.Request.ConfirmarEmailRequestDTO;
import br.wm.banco.academico.DTOs.Request.EsqueciSenhaRequestDTO;
import br.wm.banco.academico.Service.ConfirmacaoEmailService;
import br.wm.banco.academico.Service.RecuperacaoLimiteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class ConfirmacaoEmailController {
    private final ConfirmacaoEmailService service;
    private final RecuperacaoLimiteService limites;
    public ConfirmacaoEmailController(ConfirmacaoEmailService service, RecuperacaoLimiteService limites) {
        this.service = service; this.limites = limites;
    }
    private void limitar(String chave, int max) {
        if (!limites.permitir(chave, max))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas. Aguarde até 15 minutos.");
    }
    @PostMapping("/reenviar-confirmacao")
    public Map<String, String> reenviar(@Valid @RequestBody EsqueciSenhaRequestDTO dto, HttpServletRequest req) {
        limitar("confirmacao-envio-ip:" + req.getRemoteAddr(), 10);
        String email = dto.email().trim().toLowerCase(Locale.ROOT);
        if (limites.permitir("confirmacao-envio-email:" + email, 3)) service.solicitar(email);
        return Map.of("mensagem", "Se houver uma conta pendente, enviaremos um link de confirmação.");
    }
    @PostMapping("/confirmar-email")
    public Map<String, String> confirmar(@Valid @RequestBody ConfirmarEmailRequestDTO dto, HttpServletRequest req) {
        limitar("confirmacao-uso-ip:" + req.getRemoteAddr(), 20);
        limitar("confirmacao-token:" + dto.token(), 5);
        service.confirmar(dto.token());
        return Map.of("mensagem", "E-mail confirmado. Entre com sua senha.");
    }
}
