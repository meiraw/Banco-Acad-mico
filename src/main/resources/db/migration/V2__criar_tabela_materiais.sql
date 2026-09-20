CREATE TABLE materiais(
     id UUID PRIMARY KEY ,
     titulo VARCHAR (255) NOT NULL,
     descricao TEXT,
    disciplina_id UUID NOT NULL,
    nome_arquivo VARCHAR (255) NOT NULL,
    tipo_arquivo VARCHAR (255)NOT NULL,
    caminho_arquivo VARCHAR (255) NOT NULL,
    tamanho_arquivo BIGINT,

    CONSTRAINT fk_material_disciplina
        FOREIGN KEY (disciplina_id)
        REFERENCES disciplinas(id)
);