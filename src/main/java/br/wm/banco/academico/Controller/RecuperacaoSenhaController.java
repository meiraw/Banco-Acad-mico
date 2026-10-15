package br.wm.banco.academico.Controller;
import br.wm.banco.academico.DTOs.Request.*;
import br.wm.banco.academico.Service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
@RestController
@RequestMapping("/auth")
public class RecuperacaoSenhaController {
 private final RecuperacaoSenhaService service;
 private final RecuperacaoLimiteService limites;
 public RecuperacaoSenhaController(RecuperacaoSenhaService service, RecuperacaoLimiteService limites) {
  this.service = service; this.limites = limites;
 }
 private void limitar(String chave, int max) {
  if (!limites.permitir(chave, max)) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
      "Muitas tentativas. Aguarde até 15 minutos.");
 }
 @PostMapping("/esqueci-senha")
 public Map<String, String> solicitar(@Valid @RequestBody EsqueciSenhaRequestDTO dto, HttpServletRequest req) {
  limitar("envio-ip:" + req.getRemoteAddr(), 10);
  String email = dto.email().trim().toLowerCase(Locale.ROOT);
  // Supressão silenciosa por endereço evita revelar se existe uma conta.
  if (limites.permitir("envio-email:" + email, 3)) service.solicitar(email);
  return Map.of("mensagem", "Se houver uma conta para este e-mail, enviaremos um link de redefinição.");
 }
 @PostMapping("/redefinir-senha")
 public Map<String, String> redefinir(@Valid @RequestBody RedefinirSenhaRequestDTO dto, HttpServletRequest req) {
  limitar("redefinir-ip:" + req.getRemoteAddr(), 20);
  limitar("redefinir-token:" + dto.token(), 5);
  service.redefinir(dto.token(), dto.novaSenha());
  return Map.of("mensagem", "Senha alterada com sucesso. Entre com sua nova senha.");
 }
}
