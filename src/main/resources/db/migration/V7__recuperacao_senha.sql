ALTER TABLE usuarios ADD COLUMN versao_credenciais BIGINT NOT NULL DEFAULT 0;
CREATE TABLE recuperacoes_senha (
 token_hash CHAR(64) PRIMARY KEY,
 usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
 expira_em TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_recuperacoes_usuario ON recuperacoes_senha(usuario_id);
CREATE TABLE limites_recuperacao (
 chave CHAR(64) PRIMARY KEY,
 janela BIGINT NOT NULL,
 tentativas INTEGER NOT NULL
);
