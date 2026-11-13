package br.wm.banco.academico.Service;

import br.wm.banco.academico.Exception.ResourceNotFoundException;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Guarda a remoção na mesma transação da publicação e repete falhas de disco. */
@Service
public class LimpezaArquivoService {
    public record ArquivoExcluido(UUID materialId) {}
    private final JdbcTemplate jdbc;
    private final ApplicationEventPublisher eventos;
    private final TransactionTemplate transacao;
    private final ArmazenamentoService armazenamento;

    public LimpezaArquivoService(JdbcTemplate jdbc, ApplicationEventPublisher eventos, PlatformTransactionManager manager,
            ArmazenamentoService armazenamento) {
        this.armazenamento = armazenamento;
        this.jdbc = jdbc; this.eventos = eventos;
        this.transacao = new TransactionTemplate(manager);
        this.transacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public Path caminhoPermitido(String caminho) {
        return armazenamento.permitido(caminho);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void agendar(UUID id, String caminho) {
        caminhoPermitido(caminho);
        jdbc.update("INSERT INTO arquivos_para_excluir(material_id, caminho_arquivo) VALUES (?, ?)", id, caminho);
        eventos.publishEvent(new ArquivoExcluido(id));
    }

    @TransactionalEventListener
    public void aposExclusao(ArquivoExcluido evento) { tentarLimpar(evento.materialId()); }

    public void tentarLimpar(UUID id) {
        try {
            transacao.executeWithoutResult(status -> {
                var caminhos = jdbc.queryForList("SELECT caminho_arquivo FROM arquivos_para_excluir WHERE material_id = ? FOR UPDATE", String.class, id);
                if (caminhos.isEmpty()) return;
                try { Files.deleteIfExists(caminhoPermitido(caminhos.getFirst())); }
                catch (java.io.IOException ex) { throw new java.io.UncheckedIOException(ex); }
                jdbc.update("DELETE FROM arquivos_para_excluir WHERE material_id = ?", id);
            });
        } catch (RuntimeException ex) {
            LoggerFactory.getLogger(getClass()).warn("Remoção do arquivo do material {} pendente; será tentada novamente.", id);
        }
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void repetirPendentes() {
        try {
            jdbc.queryForList("SELECT material_id FROM arquivos_para_excluir ORDER BY criado_em, material_id LIMIT 50", UUID.class)
                .forEach(this::tentarLimpar);
        } catch (RuntimeException ex) {
            LoggerFactory.getLogger(getClass()).warn("Não foi possível consultar a fila de remoção de arquivos.");
        }
    }
}
