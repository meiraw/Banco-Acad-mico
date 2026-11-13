CREATE TABLE favoritos (
    usuario_id UUID NOT NULL,
    material_id UUID NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_favoritos PRIMARY KEY (usuario_id, material_id),
    CONSTRAINT fk_favoritos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_favoritos_material FOREIGN KEY (material_id) REFERENCES materiais(id) ON DELETE CASCADE
);

CREATE INDEX idx_favoritos_usuario_data ON favoritos(usuario_id, criado_em DESC, material_id);
CREATE INDEX idx_favoritos_material ON favoritos(material_id);
