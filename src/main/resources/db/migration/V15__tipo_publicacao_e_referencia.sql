-- NULL preserva materiais legados cujo tipo de conteúdo não é conhecido.
ALTER TABLE materiais ADD COLUMN tipo_publicacao VARCHAR(32);
ALTER TABLE materiais ADD COLUMN referencia_bibliografica VARCHAR(500);
ALTER TABLE materiais ADD CONSTRAINT ck_material_tipo_publicacao CHECK (
    tipo_publicacao IN ('SLIDE', 'ANOTACAO', 'RESUMO', 'LISTA_EXERCICIOS',
                       'RESOLUCAO_EXERCICIOS', 'LIVRO', 'ARTIGO')
);
ALTER TABLE materiais ADD CONSTRAINT ck_material_referencia_livro CHECK (
    referencia_bibliografica IS NULL OR (tipo_publicacao IS NOT NULL AND tipo_publicacao = 'LIVRO')
);
