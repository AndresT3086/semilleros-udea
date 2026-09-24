-- ============================================================
-- V12__solicitudes_acceso_e_invitaciones.sql
-- Registro de coordinadores:
--   1. La persona diligencia el formulario (queda PENDIENTE_VERIFICACION)
--   2. Confirma su correo @udea.edu.co con un enlace de un solo uso (PENDIENTE)
--   3. Un ADMIN aprueba o rechaza. Al aprobar se crea el usuario inactivo
--      y se envía un enlace para crear la contraseña (token_cuenta)
-- El ADMIN también puede invitar directamente: crea el usuario y el token.
-- Los tokens se guardan como hash SHA-256; el valor real solo viaja en el correo.
-- ============================================================

CREATE TABLE IF NOT EXISTS solicitud_acceso (
    id_solicitud        BIGSERIAL    PRIMARY KEY,
    nombres             VARCHAR(100) NOT NULL,
    apellidos           VARCHAR(100) NOT NULL,
    cedula              VARCHAR(15)  NOT NULL,
    correo              VARCHAR(150) NOT NULL,
    id_unidad_academica BIGINT,
    justificacion       TEXT         NOT NULL,
    estado              VARCHAR(30)  NOT NULL DEFAULT 'PENDIENTE_VERIFICACION',
    token_hash          VARCHAR(64),
    token_expira        TIMESTAMPTZ,
    envios_verificacion INTEGER      NOT NULL DEFAULT 0,
    ultimo_envio        TIMESTAMPTZ,
    ip_origen           VARCHAR(64),
    fecha_creacion      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    fecha_verificacion  TIMESTAMPTZ,
    id_revisor          BIGINT,
    fecha_revision      TIMESTAMPTZ,
    motivo_rechazo      TEXT,
    bloqueada           BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_solicitud_unidad  FOREIGN KEY (id_unidad_academica) REFERENCES unidad_academica (id_unidad),
    CONSTRAINT fk_solicitud_revisor FOREIGN KEY (id_revisor)          REFERENCES usuario (id_usuario),
    CONSTRAINT chk_solicitud_estado CHECK (estado IN ('PENDIENTE_VERIFICACION', 'PENDIENTE', 'APROBADA', 'RECHAZADA')),
    CONSTRAINT chk_solicitud_cedula CHECK (cedula ~ '^[0-9]{7,10}$')
);

-- Una sola solicitud en curso por correo y por cédula
CREATE UNIQUE INDEX IF NOT EXISTS uq_solicitud_correo_en_curso
    ON solicitud_acceso (correo) WHERE estado IN ('PENDIENTE_VERIFICACION', 'PENDIENTE');
CREATE UNIQUE INDEX IF NOT EXISTS uq_solicitud_cedula_en_curso
    ON solicitud_acceso (cedula) WHERE estado IN ('PENDIENTE_VERIFICACION', 'PENDIENTE');
CREATE UNIQUE INDEX IF NOT EXISTS uq_solicitud_token ON solicitud_acceso (token_hash);
CREATE INDEX IF NOT EXISTS idx_solicitud_estado ON solicitud_acceso (estado, fecha_creacion);

CREATE TABLE IF NOT EXISTS token_cuenta (
    id_token       BIGSERIAL   PRIMARY KEY,
    id_usuario     BIGINT      NOT NULL,
    token_hash     VARCHAR(64) NOT NULL,
    origen         VARCHAR(20) NOT NULL,
    expira         TIMESTAMPTZ NOT NULL,
    usado          BOOLEAN     NOT NULL DEFAULT FALSE,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_token_usuario  FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    CONSTRAINT chk_token_origen  CHECK (origen IN ('APROBACION', 'INVITACION')),
    CONSTRAINT uq_token_cuenta   UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS idx_token_cuenta_usuario ON token_cuenta (id_usuario);
