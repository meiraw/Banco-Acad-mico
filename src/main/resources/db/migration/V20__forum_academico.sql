CREATE TABLE discussoes_forum (
    id UUID PRIMARY KEY,
    titulo VARCHAR(200) NOT NULL,
    conteudo VARCHAR(10000) NOT NULL,
    disciplina_id UUID NOT NULL REFERENCES disciplinas(id) ON DELETE RESTRICT,
    autor_id UUID REFERENCES usuarios(id) ON DELETE SET NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    excluido_em TIMESTAMP WITH TIME ZONE,
    versao BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_discussao_titulo CHECK (CHAR_LENGTH(TRIM(titulo)) > 0),
    CONSTRAINT ck_discussao_conteudo CHECK (CHAR_LENGTH(TRIM(conteudo)) > 0)
);
CREATE TABLE respostas_forum (
    id UUID PRIMARY KEY,
    discussao_id UUID NOT NULL REFERENCES discussoes_forum(id) ON DELETE RESTRICT,
    autor_id UUID REFERENCES usuarios(id) ON DELETE SET NULL,
    conteudo VARCHAR(5000) NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    excluido_em TIMESTAMP WITH TIME ZONE,
    versao BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_resposta_conteudo CHECK (CHAR_LENGTH(TRIM(conteudo)) > 0)
);
CREATE INDEX idx_forum_discussao_data ON discussoes_forum(excluido_em, criado_em, id);
CREATE INDEX idx_forum_discussao_disciplina ON discussoes_forum(disciplina_id, excluido_em, criado_em, id);
CREATE INDEX idx_forum_discussao_autor ON discussoes_forum(autor_id, excluido_em, criado_em, id);
CREATE INDEX idx_forum_resposta_discussao ON respostas_forum(discussao_id, excluido_em, criado_em, id);
CREATE INDEX idx_forum_resposta_autor ON respostas_forum(autor_id);
