CREATE TABLE fotos_perfil (
    usuario_id UUID PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,
    conteudo BYTEA NOT NULL,
    CONSTRAINT chk_fotos_perfil_tamanho CHECK (octet_length(conteudo) <= 2097152)
);
