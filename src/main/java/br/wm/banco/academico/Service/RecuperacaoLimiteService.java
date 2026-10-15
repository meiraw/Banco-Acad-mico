package br.wm.banco.academico.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service
public class RecuperacaoLimiteService {
 private final JdbcTemplate jdbc;
 public RecuperacaoLimiteService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
 @Transactional
 public boolean permitir(String chave, int limite) {
  long janela = java.time.Instant.now().getEpochSecond() / 900;
  jdbc.update("DELETE FROM limites_recuperacao WHERE janela < ?", janela - 1);
  Integer tentativas = jdbc.queryForObject("""
   INSERT INTO limites_recuperacao(chave, janela, tentativas) VALUES (?, ?, 1)
   ON CONFLICT (chave) DO UPDATE SET
    tentativas = CASE WHEN limites_recuperacao.janela = EXCLUDED.janela
      THEN LEAST(limites_recuperacao.tentativas + 1, 1000000) ELSE 1 END,
    janela = EXCLUDED.janela RETURNING tentativas
   """, Integer.class, RecuperacaoSenhaService.hash(chave), janela);
  return tentativas != null && tentativas <= limite;
 }
}
