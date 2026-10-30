ALTER TABLE materiais ADD COLUMN autor_id UUID REFERENCES usuarios(id) ON DELETE SET NULL;
ALTER TABLE materiais ADD COLUMN publicado_em TIMESTAMP WITH TIME ZONE;

CREATE TABLE comentarios_material (
    id UUID PRIMARY KEY,
    material_id UUID NOT NULL REFERENCES materiais(id) ON DELETE CASCADE,
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    texto VARCHAR(2000) NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_comentarios_material ON comentarios_material(material_id, criado_em, id);
