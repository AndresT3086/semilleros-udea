-- ============================================================
-- V10__sesiones_y_asistencia.sql
-- Registro de actividades (sesiones) de cada semillero y la
-- asistencia de sus integrantes. Lo diligencia el coordinador.
--
-- % asistencia = PRESENTE / (PRESENTE + AUSENTE) × 100
-- Las ausencias EXCUSADAS se descuentan del total esperado. La lista de
-- cada sesión se toma de los integrantes activos al registrarla, así
-- a quien ingresa después no se le cuentan sesiones anteriores.
-- ============================================================

CREATE TABLE IF NOT EXISTS sesion_semillero (
    id_sesion      BIGSERIAL    PRIMARY KEY,
    id_semillero   BIGINT       NOT NULL,
    id_actividad   BIGINT,
    titulo         VARCHAR(200) NOT NULL,
    fecha          DATE         NOT NULL,
    fecha_creacion TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMPTZ,
    CONSTRAINT fk_sesion_semillero FOREIGN KEY (id_semillero) REFERENCES semillero (id_semillero),
    CONSTRAINT fk_sesion_actividad FOREIGN KEY (id_actividad) REFERENCES actividad_cientifica (id_actividad)
);

CREATE TABLE IF NOT EXISTS asistencia_sesion (
    id_sesion     BIGINT      NOT NULL,
    id_integrante BIGINT      NOT NULL,
    estado        VARCHAR(20) NOT NULL,
    PRIMARY KEY (id_sesion, id_integrante),
    CONSTRAINT fk_asistencia_sesion    FOREIGN KEY (id_sesion)     REFERENCES sesion_semillero (id_sesion) ON DELETE CASCADE,
    CONSTRAINT fk_asistencia_integrante FOREIGN KEY (id_integrante) REFERENCES semillero_integrante (id),
    CONSTRAINT chk_asistencia_estado   CHECK (estado IN ('PRESENTE', 'AUSENTE', 'EXCUSADO'))
);

CREATE INDEX IF NOT EXISTS idx_sesion_semillero_fecha ON sesion_semillero (id_semillero, fecha);
CREATE INDEX IF NOT EXISTS idx_asistencia_integrante  ON asistencia_sesion (id_integrante);
