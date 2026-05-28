-- Adaptar produccion_academica al modelo real del front (Sí/No + cantidad)
ALTER TABLE produccion_academica
    DROP COLUMN IF EXISTS tipo,
    DROP COLUMN IF EXISTS titulo,
    DROP COLUMN IF EXISTS anio,
    DROP COLUMN IF EXISTS autores,
    DROP COLUMN IF EXISTS enlace;

ALTER TABLE produccion_academica
    ADD COLUMN IF NOT EXISTS tienen_articulos         BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS cantidad_articulos       INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS tienen_libros            BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS cantidad_libros          INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS organizan_eventos        BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS cantidad_eventos         INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS participan_eventos       BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS cantidad_participaciones INTEGER NOT NULL DEFAULT 0;

-- Hacer que sea 1 registro por semillero (relación 1:1)
ALTER TABLE produccion_academica
    DROP COLUMN IF EXISTS id_produccion;

ALTER TABLE produccion_academica
    ADD COLUMN IF NOT EXISTS id_semillero_pk BIGINT;

-- Si la tabla tiene datos previos limpiarla
TRUNCATE TABLE produccion_academica;

-- Redefinir como 1:1 con semillero
ALTER TABLE produccion_academica
    ADD PRIMARY KEY (id_semillero);

-- ── Nueva tabla para relacionamiento ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS semillero_relacionamiento (
    id_semillero             BIGINT PRIMARY KEY,
    adscrito_grupo           BOOLEAN      NOT NULL DEFAULT FALSE,
    grupo_investigacion      VARCHAR(300),
    relacion_grupo           VARCHAR(200),
    centro_investigaciones   VARCHAR(300),
    relacion_centro          VARCHAR(200),
    departamento             VARCHAR(200),
    relacion_departamento    VARCHAR(200),
    facultad                 VARCHAR(200),
    relacion_facultad        VARCHAR(200),
    CONSTRAINT fk_rel_semillero FOREIGN KEY (id_semillero) REFERENCES semillero (id_semillero)
);

-- ── Sub-área OCDE y observaciones finales (pestaña ODS) ──────────────────────
ALTER TABLE semillero
    ADD COLUMN IF NOT EXISTS subarea_ocde        VARCHAR(200),
    ADD COLUMN IF NOT EXISTS ods_principal       BIGINT,
    ADD COLUMN IF NOT EXISTS observaciones_finales TEXT,
    ADD CONSTRAINT fk_semillero_ods_principal
        FOREIGN KEY (ods_principal) REFERENCES ods (id_ods);

-- ── Actividades: actualizar nombres según el front ───────────────────────────
-- Limpiar las actividades de referencia e insertar las correctas
TRUNCATE TABLE semillero_actividad;
TRUNCATE TABLE actividad_cientifica RESTART IDENTITY CASCADE;

INSERT INTO actividad_cientifica (nombre, categoria) VALUES
('Clubes de Revista',        'Formativas'),
('Seminarios',               'Formativas'),
('Salidas de campo',         'Formativas'),
('Talleres',                 'Formativas'),
('Conversatorios',           'Formativas'),
('Jornadas Universitarias',  'Socialización y difusión'),
('Eventos RedCOLSI',         'Socialización y difusión'),
('Ponencias Nacionales',     'Socialización y difusión'),
('Ponencias Internacionales','Socialización y difusión');