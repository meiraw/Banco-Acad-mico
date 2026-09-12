CREATE TABLE disciplinas (
    id UUID PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    descricao VARCHAR(1000),
    semestre INTEGER NOT NULL,

    CONSTRAINT ck_disciplinas_semestre
    CHECK (semestre BETWEEN 1 AND 8),

     CONSTRAINT uq_disciplinas_nome
     UNIQUE (nome)
);