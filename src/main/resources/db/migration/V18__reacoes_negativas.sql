-- Preserva todas as curtidas existentes e a unicidade por usuário/publicação.
ALTER TABLE curtidas_material ADD COLUMN tipo VARCHAR(10) NOT NULL DEFAULT 'CURTIDA';
ALTER TABLE curtidas_material ADD CONSTRAINT ck_reacao_material_tipo CHECK (tipo IN ('CURTIDA', 'DESLIKE'));
