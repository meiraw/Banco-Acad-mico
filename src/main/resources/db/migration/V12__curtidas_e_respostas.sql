CREATE TABLE curtidas_material (
    material_id UUID NOT NULL REFERENCES materiais(id) ON DELETE CASCADE,
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    PRIMARY KEY (material_id, usuario_id)
);

CREATE TABLE respostas_comentario (
    id UUID PRIMARY KEY,
    comentario_id UUID NOT NULL REFERENCES comentarios_material(id) ON DELETE CASCADE,
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    texto VARCHAR(2000) NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_respostas_comentario ON respostas_comentario(comentario_id, criado_em, id);
