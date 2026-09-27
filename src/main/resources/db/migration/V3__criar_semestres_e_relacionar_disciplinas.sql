CREATE TABLE semestres
(
    id      UUID PRIMARY KEY,
    ano     INTEGER NOT NULL,
    periodo INTEGER NOT NULL
);
    ALTER TABLE disciplinas
    ADD COLUMN semestre_id UUID NOT NULL;

    ALTER TABLE disciplinas
    ADD CONSTRAINT fk_disciplina_semestre
    FOREIGN KEY (semestre_id)
    REFERENCES semestres(id);

