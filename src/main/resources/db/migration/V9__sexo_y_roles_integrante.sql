-- ============================================================
-- V9__sexo_y_roles_integrante.sql
-- Datos demográficos para reportes (HU5):
--   * sexo de quien se inscribe y de cada integrante
--   * catálogo de roles desempeñados por los integrantes
-- ============================================================

ALTER TABLE inscripcion
    ADD COLUMN IF NOT EXISTS sexo VARCHAR(20),
    ADD CONSTRAINT chk_inscripcion_sexo CHECK (sexo IN ('FEMENINO', 'MASCULINO', 'OTRO'));

ALTER TABLE semillero_integrante
    ADD COLUMN IF NOT EXISTS sexo VARCHAR(20),
    ADD CONSTRAINT chk_integrante_sexo CHECK (sexo IN ('FEMENINO', 'MASCULINO', 'OTRO'));

-- ----------------------
-- ROL_INTEGRANTE: roles que puede desempeñar un integrante en su semillero.
-- semillero_integrante.tipo_vinculacion guarda el código del rol.
-- ----------------------
CREATE TABLE IF NOT EXISTS rol_integrante (
    codigo VARCHAR(50)  PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    orden  INTEGER      NOT NULL
);

INSERT INTO rol_integrante (codigo, nombre, orden) VALUES
('ESTUDIANTE_INVESTIGADOR', 'Estudiante Investigador', 1),
('AUXILIAR',                'Auxiliar',                2),
('COORDINADOR',             'Coordinador',             3),
('TUTOR',                   'Tutor',                   4),
('SEMILLERISTA_JUNIOR',     'Semillerista Junior',     5)
ON CONFLICT (codigo) DO NOTHING;

-- Las solicitudes aprobadas se registraban como "ESTUDIANTE"
UPDATE semillero_integrante
SET tipo_vinculacion = 'ESTUDIANTE_INVESTIGADOR'
WHERE tipo_vinculacion IS NULL OR tipo_vinculacion = 'ESTUDIANTE';

CREATE INDEX IF NOT EXISTS idx_integrante_semillero ON semillero_integrante (id_semillero);
