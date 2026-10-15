ALTER TABLE usuarios ADD COLUMN email_confirmado BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE usuarios ALTER COLUMN email_confirmado SET DEFAULT FALSE;
CREATE TABLE confirmacoes_email (
 token_hash CHAR(64) PRIMARY KEY,
 usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
 expira_em TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_confirmacoes_usuario ON confirmacoes_email(usuario_id);
