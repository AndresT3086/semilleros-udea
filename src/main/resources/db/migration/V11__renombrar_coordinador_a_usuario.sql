-- ============================================================
-- V11__renombrar_coordinador_a_usuario.sql
-- La tabla "coordinador" guarda todas las cuentas del sistema (ADMIN y
-- COORDINADOR), por eso pasa a llamarse "usuario".
-- semillero.id_coordinador conserva su nombre: es el coordinador del semillero.
-- ============================================================

ALTER TABLE coordinador RENAME TO usuario;
ALTER TABLE usuario RENAME COLUMN id_coordinador TO id_usuario;

-- Nombres derivados (secuencia, restricciones e índice) según el nombre anterior
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_class WHERE relkind = 'S' AND relname = 'coordinador_id_coordinador_seq') THEN
        ALTER SEQUENCE coordinador_id_coordinador_seq RENAME TO usuario_id_usuario_seq;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'coordinador_pkey') THEN
        ALTER TABLE usuario RENAME CONSTRAINT coordinador_pkey TO usuario_pkey;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'coordinador_correo_key') THEN
        ALTER TABLE usuario RENAME CONSTRAINT coordinador_correo_key TO usuario_correo_key;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_coordinador_rol') THEN
        ALTER TABLE usuario RENAME CONSTRAINT chk_coordinador_rol TO chk_usuario_rol;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_class WHERE relkind = 'i' AND relname = 'idx_coordinador_correo') THEN
        ALTER INDEX idx_coordinador_correo RENAME TO idx_usuario_correo;
    END IF;
END $$;

COMMENT ON TABLE usuario IS 'Cuentas del sistema: administradores (ADMIN) y coordinadores de semillero (COORDINADOR)';
