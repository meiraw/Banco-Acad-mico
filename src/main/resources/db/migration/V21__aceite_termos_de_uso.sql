ALTER TABLE usuarios ADD COLUMN versao_termos_aceita VARCHAR(32);
ALTER TABLE usuarios ADD COLUMN termos_aceitos_em TIMESTAMP WITH TIME ZONE;
