-- Remove os dados de teste que dependem dos semestres antigos
DELETE FROM materiais;
DELETE FROM disciplinas;
DELETE FROM semestres;


-- Cria a tabela de cursos
CREATE TABLE cursos (
                        id UUID PRIMARY KEY,
                        nome VARCHAR(200) NOT NULL UNIQUE
);


-- Remove a estrutura antiga de semestre
ALTER TABLE semestres
DROP COLUMN ano;

ALTER TABLE semestres
DROP COLUMN periodo;


-- Adiciona a nova estrutura
ALTER TABLE semestres
    ADD COLUMN nome VARCHAR(100) NOT NULL;

ALTER TABLE semestres
    ADD COLUMN numero INTEGER NOT NULL;

ALTER TABLE semestres
    ADD COLUMN curso_id UUID NOT NULL;


-- Garante que o semestre seja de 1 até 8
ALTER TABLE semestres
    ADD CONSTRAINT ck_semestre_numero
        CHECK (numero BETWEEN 1 AND 8);


-- Relacionamento Semestre -> Curso
ALTER TABLE semestres
    ADD CONSTRAINT fk_semestre_curso
        FOREIGN KEY (curso_id)
            REFERENCES cursos(id);