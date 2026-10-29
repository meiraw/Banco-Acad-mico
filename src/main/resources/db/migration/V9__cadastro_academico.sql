ALTER TABLE usuarios ADD COLUMN curso VARCHAR(200) NOT NULL DEFAULT 'Ciência da Computação';
-- Usuários antigos não têm semestre conhecido. Novos cadastros exigem o campo na API.
ALTER TABLE usuarios ADD COLUMN semestre_atual INTEGER;
ALTER TABLE usuarios ADD CONSTRAINT chk_usuarios_semestre_atual CHECK (semestre_atual BETWEEN 1 AND 8);
