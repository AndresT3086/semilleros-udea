-- ============================================================
-- V1__crear_tablas_base.sql
-- Sistema de Semilleros de Investigación - Universidad de Antioquia
-- Sprint 1 - Tablas base del modelo ER
-- ============================================================

-- ----------------------
-- CAMPUS
-- ----------------------
CREATE TABLE IF NOT EXISTS campus (
    id_campus    SERIAL PRIMARY KEY,
    nombre       VARCHAR(150) NOT NULL,
    ciudad       VARCHAR(100),
    departamento VARCHAR(100),
    direccion    VARCHAR(255)
);

-- ----------------------
-- UNIDAD_ACADEMICA (Facultades / Escuelas)
-- ----------------------
CREATE TABLE IF NOT EXISTS unidad_academica (
    id_unidad  SERIAL PRIMARY KEY,
    nombre     VARCHAR(200) NOT NULL,
    siglas     VARCHAR(20),
    id_campus  INTEGER NOT NULL,
    CONSTRAINT fk_unidad_campus FOREIGN KEY (id_campus) REFERENCES campus (id_campus)
);

-- ----------------------
-- PROGRAMA_ACADEMICO
-- ----------------------
CREATE TABLE IF NOT EXISTS programa_academico (
    id_programa INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre      VARCHAR(200) NOT NULL,
    codigo      VARCHAR(20),
    id_unidad   INTEGER NOT NULL,
    CONSTRAINT fk_programa_unidad FOREIGN KEY (id_unidad) REFERENCES unidad_academica (id_unidad)
);

-- ----------------------
-- AREA_OCDE
-- ----------------------
CREATE TABLE IF NOT EXISTS area_ocde (
    id_area     SERIAL PRIMARY KEY,
    nombre      VARCHAR(200) NOT NULL,
    descripcion TEXT
);

-- ----------------------
-- COORDINADOR (usuarios del sistema)
-- ----------------------
CREATE TABLE IF NOT EXISTS coordinador (
    id_coordinador SERIAL PRIMARY KEY,
    nombres        VARCHAR(100) NOT NULL,
    apellidos      VARCHAR(100) NOT NULL,
    correo         VARCHAR(150) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    telefono       VARCHAR(20),
    rol            VARCHAR(50)  NOT NULL DEFAULT 'COORDINADOR',
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ----------------------
-- SEMILLERO
-- ----------------------
CREATE TABLE IF NOT EXISTS semillero (
    id_semillero          SERIAL PRIMARY KEY,
    codigo                VARCHAR(30)  NOT NULL UNIQUE,
    nombre                VARCHAR(300) NOT NULL,
    siglas                VARCHAR(30),
    correo_principal      VARCHAR(150),
    telefono              VARCHAR(20),
    anio_creacion         INTEGER,
    mision                TEXT,
    vision                TEXT,
    objetivo              TEXT,
    lineas_investigacion  TEXT,
    palabras_clave        VARCHAR(500),
    grupo_investigacion   VARCHAR(200),
    estado                VARCHAR(30)  NOT NULL DEFAULT 'BORRADOR',
    estado_caracterizacion VARCHAR(50) DEFAULT 'GENERAL_PENDIENTE',
    id_unidad_academica   INTEGER,
    id_campus             INTEGER,
    id_area_ocde          INTEGER,
    id_coordinador        INTEGER,
    fecha_creacion        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_actualizacion   TIMESTAMPTZ,
    CONSTRAINT fk_semillero_unidad      FOREIGN KEY (id_unidad_academica) REFERENCES unidad_academica (id_unidad),
    CONSTRAINT fk_semillero_campus      FOREIGN KEY (id_campus)           REFERENCES campus (id_campus),
    CONSTRAINT fk_semillero_area        FOREIGN KEY (id_area_ocde)         REFERENCES area_ocde (id_area),
    CONSTRAINT fk_semillero_coordinador FOREIGN KEY (id_coordinador)       REFERENCES coordinador (id_coordinador),
    CONSTRAINT chk_semillero_estado     CHECK (estado IN ('ACTIVO','INACTIVO','BORRADOR','CARACTERIZADO'))
);

-- ----------------------
-- INSCRIPCION
-- ----------------------
CREATE TABLE IF NOT EXISTS inscripcion (
    id_inscripcion      SERIAL PRIMARY KEY,
    id_semillero        INTEGER      NOT NULL,
    nombres             VARCHAR(100) NOT NULL,
    apellidos           VARCHAR(100) NOT NULL,
    cedula              VARCHAR(15)  NOT NULL,
    correo              VARCHAR(150) NOT NULL,
    telefono            VARCHAR(15)  NOT NULL,
    programa            VARCHAR(200),
    semestre            VARCHAR(10),
    motivacion          TEXT,
    acepta_terminos     BOOLEAN      NOT NULL DEFAULT FALSE,
    estado              VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    fecha_inscripcion   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMPTZ,
    CONSTRAINT fk_inscripcion_semillero FOREIGN KEY (id_semillero) REFERENCES semillero (id_semillero),
    CONSTRAINT chk_inscripcion_estado   CHECK (estado IN ('PENDIENTE','APROBADO','RECHAZADO')),
    CONSTRAINT chk_cedula_numerica      CHECK (cedula ~ '^[0-9]{7,10}$'),
    CONSTRAINT chk_telefono_numerico    CHECK (telefono ~ '^[0-9]{10}$')
);

-- Índice para evitar duplicados activos por correo + semillero
CREATE UNIQUE INDEX IF NOT EXISTS idx_inscripcion_activa
    ON inscripcion (correo, id_semillero)
    WHERE estado IN ('PENDIENTE', 'APROBADO');

-- ----------------------
-- SEMILLERO_INTEGRANTE
-- ----------------------
CREATE TABLE IF NOT EXISTS semillero_integrante (
    id               SERIAL PRIMARY KEY,
    id_semillero     INTEGER      NOT NULL,
    nombres          VARCHAR(100) NOT NULL,
    apellidos        VARCHAR(100) NOT NULL,
    cedula           VARCHAR(15)  NOT NULL,
    correo           VARCHAR(150),
    telefono         VARCHAR(20),
    id_programa      INTEGER,
    tipo_vinculacion VARCHAR(50),
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_ingreso    DATE,
    CONSTRAINT fk_integrante_semillero FOREIGN KEY (id_semillero) REFERENCES semillero (id_semillero),
    CONSTRAINT fk_integrante_programa  FOREIGN KEY (id_programa)  REFERENCES programa_academico (id_programa)
);

-- ----------------------
-- RECURSO
-- ----------------------
CREATE TABLE IF NOT EXISTS recurso (
    id_recurso  SERIAL PRIMARY KEY,
    nombre      VARCHAR(150) NOT NULL,
    descripcion TEXT
);

-- ----------------------
-- SEMILLERO_RECURSO (relación N:M)
-- ----------------------
CREATE TABLE IF NOT EXISTS semillero_recurso (
    id_semillero INTEGER NOT NULL,
    id_recurso   INTEGER NOT NULL,
    PRIMARY KEY (id_semillero, id_recurso),
    CONSTRAINT fk_sr_semillero FOREIGN KEY (id_semillero) REFERENCES semillero (id_semillero),
    CONSTRAINT fk_sr_recurso   FOREIGN KEY (id_recurso)   REFERENCES recurso (id_recurso)
);

-- ----------------------
-- FUENTE_FINANCIACION
-- ----------------------
CREATE TABLE IF NOT EXISTS fuente_financiacion (
    id_fuente   SERIAL PRIMARY KEY,
    nombre      VARCHAR(150) NOT NULL,
    descripcion TEXT
);

-- ----------------------
-- SEMILLERO_FINANCIACION (relación N:M)
-- ----------------------
CREATE TABLE IF NOT EXISTS semillero_financiacion (
    id_semillero INTEGER NOT NULL,
    id_fuente    INTEGER NOT NULL,
    PRIMARY KEY (id_semillero, id_fuente),
    CONSTRAINT fk_sf_semillero FOREIGN KEY (id_semillero) REFERENCES semillero (id_semillero),
    CONSTRAINT fk_sf_fuente    FOREIGN KEY (id_fuente)    REFERENCES fuente_financiacion (id_fuente)
);

-- ----------------------
-- ODS (Objetivos de Desarrollo Sostenible)
-- ----------------------
CREATE TABLE IF NOT EXISTS ods (
    id_ods   SERIAL PRIMARY KEY,
    numero   INTEGER      NOT NULL,
    nombre   VARCHAR(200) NOT NULL,
    icono    VARCHAR(255)
);

-- ----------------------
-- SEMILLERO_ODS (relación N:M)
-- ----------------------
CREATE TABLE IF NOT EXISTS semillero_ods (
    id_semillero INTEGER NOT NULL,
    id_ods       INTEGER NOT NULL,
    PRIMARY KEY (id_semillero, id_ods),
    CONSTRAINT fk_so_semillero FOREIGN KEY (id_semillero) REFERENCES semillero (id_semillero),
    CONSTRAINT fk_so_ods       FOREIGN KEY (id_ods)       REFERENCES ods (id_ods)
);

-- ----------------------
-- DOFA
-- ----------------------
CREATE TABLE IF NOT EXISTS dofa (
    id_dofa      SERIAL PRIMARY KEY,
    id_semillero INTEGER     NOT NULL,
    tipo         VARCHAR(20) NOT NULL,   -- FORTALEZA, DEBILIDAD, OPORTUNIDAD, AMENAZA
    descripcion  TEXT        NOT NULL,
    CONSTRAINT fk_dofa_semillero FOREIGN KEY (id_semillero) REFERENCES semillero (id_semillero),
    CONSTRAINT chk_dofa_tipo     CHECK (tipo IN ('FORTALEZA','DEBILIDAD','OPORTUNIDAD','AMENAZA'))
);

-- ----------------------
-- ACTIVIDAD_CIENTIFICA
-- ----------------------
CREATE TABLE IF NOT EXISTS actividad_cientifica (
    id_actividad SERIAL PRIMARY KEY,
    nombre       VARCHAR(200) NOT NULL,
    categoria    VARCHAR(100)
);

-- ----------------------
-- SEMILLERO_ACTIVIDAD (registro de si el semillero realiza cada actividad)
-- ----------------------
CREATE TABLE IF NOT EXISTS semillero_actividad (
    id_semillero INTEGER NOT NULL,
    id_actividad INTEGER NOT NULL,
    realiza      BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id_semillero, id_actividad),
    CONSTRAINT fk_sa_semillero  FOREIGN KEY (id_semillero) REFERENCES semillero (id_semillero),
    CONSTRAINT fk_sa_actividad  FOREIGN KEY (id_actividad) REFERENCES actividad_cientifica (id_actividad)
);

-- ----------------------
-- PRODUCCION_ACADEMICA
-- ----------------------
CREATE TABLE IF NOT EXISTS produccion_academica (
    id_produccion SERIAL PRIMARY KEY,
    id_semillero  INTEGER      NOT NULL,
    tipo          VARCHAR(100) NOT NULL,
    titulo        VARCHAR(500) NOT NULL,
    anio          INTEGER,
    autores       TEXT,
    enlace        VARCHAR(500),
    CONSTRAINT fk_prod_semillero FOREIGN KEY (id_semillero) REFERENCES semillero (id_semillero)
);

-- ----------------------
-- EVENTO
-- ----------------------
CREATE TABLE IF NOT EXISTS evento (
    id_evento    SERIAL PRIMARY KEY,
    nombre       VARCHAR(300) NOT NULL,
    tipo         VARCHAR(100),
    fecha        DATE,
    ciudad       VARCHAR(100),
    pais         VARCHAR(100)
);

-- ----------------------
-- SEMILLERO_EVENTO (participación en eventos)
-- ----------------------
CREATE TABLE IF NOT EXISTS semillero_evento (
    id            SERIAL PRIMARY KEY,
    id_semillero  INTEGER NOT NULL,
    id_evento     INTEGER NOT NULL,
    modalidad     VARCHAR(50),
    titulo_ponencia VARCHAR(400),
    anio          INTEGER,
    CONSTRAINT fk_se_semillero FOREIGN KEY (id_semillero) REFERENCES semillero (id_semillero),
    CONSTRAINT fk_se_evento    FOREIGN KEY (id_evento)    REFERENCES evento (id_evento)
);

-- ----------------------
-- Índices de rendimiento
-- ----------------------
CREATE INDEX IF NOT EXISTS idx_semillero_estado          ON semillero (estado);
CREATE INDEX IF NOT EXISTS idx_semillero_unidad          ON semillero (id_unidad_academica);
CREATE INDEX IF NOT EXISTS idx_semillero_campus          ON semillero (id_campus);
CREATE INDEX IF NOT EXISTS idx_semillero_area            ON semillero (id_area_ocde);
CREATE INDEX IF NOT EXISTS idx_semillero_coordinador     ON semillero (id_coordinador);
CREATE INDEX IF NOT EXISTS idx_semillero_nombre_lower    ON semillero (LOWER(nombre));
CREATE INDEX IF NOT EXISTS idx_inscripcion_correo        ON inscripcion (correo);
CREATE INDEX IF NOT EXISTS idx_inscripcion_semillero     ON inscripcion (id_semillero);
CREATE INDEX IF NOT EXISTS idx_coordinador_correo        ON coordinador (correo);
