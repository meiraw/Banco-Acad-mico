ALTER TABLE materiais DROP CONSTRAINT ck_material_referencia_livro;
ALTER TABLE materiais ADD CONSTRAINT ck_material_referencia_publicacao CHECK (
    referencia_bibliografica IS NULL OR (
        tipo_publicacao IS NOT NULL AND tipo_publicacao IN ('LIVRO', 'ARTIGO')
    )
);
