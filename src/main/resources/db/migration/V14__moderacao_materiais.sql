-- Mantém a visibilidade anterior dos materiais legados, sem inventar decisões.
ALTER TABLE materiais ADD COLUMN status_moderacao VARCHAR(20);
UPDATE materiais SET status_moderacao = 'APROVADO';
ALTER TABLE materiais ALTER COLUMN status_moderacao SET DEFAULT 'PENDENTE';
ALTER TABLE materiais ALTER COLUMN status_moderacao SET NOT NULL;
ALTER TABLE materiais ADD CONSTRAINT ck_material_status
    CHECK (status_moderacao IN ('PENDENTE', 'APROVADO', 'REJEITADO'));
ALTER TABLE materiais ADD COLUMN decisao_em TIMESTAMP WITH TIME ZONE;
ALTER TABLE materiais ADD COLUMN moderador_id UUID REFERENCES usuarios(id);
ALTER TABLE materiais ADD COLUMN justificativa_rejeicao VARCHAR(2000);
CREATE INDEX idx_materiais_moderacao ON materiais(status_moderacao, publicado_em, id);
