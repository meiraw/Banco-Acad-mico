CREATE TABLE usuarios(
    id UUID PRIMARY KEY ,
    nome VARCHAR (200) NOT NULL,
    email VARCHAR (200) NOT NULL unique,
    senha VARCHAR (200) NOT NULL,
    perfil VARCHAR (50) NOT NULL
);