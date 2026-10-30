ALTER TABLE respostas_comentario ADD COLUMN resposta_pai_id UUID REFERENCES respostas_comentario(id) ON DELETE CASCADE;
CREATE INDEX idx_resposta_pai ON respostas_comentario(resposta_pai_id);
