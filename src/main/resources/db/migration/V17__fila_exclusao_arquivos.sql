-- Sem FK com materiais: a tarefa deve sobreviver à exclusão da publicação.
CREATE TABLE arquivos_para_excluir (
    material_id UUID PRIMARY KEY,
    caminho_arquivo VARCHAR(255) NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
