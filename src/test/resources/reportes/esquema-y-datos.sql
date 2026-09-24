-- Esquema mínimo (columnas usadas por los reportes) y datos de prueba para ReportesRepositoryAdapterTest.

CREATE TABLE campus (id_campus BIGINT PRIMARY KEY, nombre VARCHAR(150) NOT NULL);
CREATE TABLE unidad_academica (id_unidad BIGINT PRIMARY KEY, nombre VARCHAR(200) NOT NULL, id_campus BIGINT);
CREATE TABLE semillero (
    id_semillero        BIGINT PRIMARY KEY,
    codigo              VARCHAR(30) NOT NULL,
    nombre              VARCHAR(300),
    anio_creacion       INTEGER,
    estado              VARCHAR(30) NOT NULL,
    id_unidad_academica BIGINT,
    id_campus           BIGINT,
    id_coordinador      BIGINT,
    fecha_actualizacion TIMESTAMP
);
CREATE TABLE semillero_integrante (
    id               BIGINT PRIMARY KEY,
    id_semillero     BIGINT NOT NULL,
    cedula           VARCHAR(15) NOT NULL,
    sexo             VARCHAR(20),
    tipo_vinculacion VARCHAR(50),
    activo           BOOLEAN NOT NULL,
    fecha_ingreso    DATE
);
CREATE TABLE actividad_cientifica (id_actividad BIGINT PRIMARY KEY, nombre VARCHAR(200) NOT NULL);
CREATE TABLE semillero_actividad (id_semillero BIGINT, id_actividad BIGINT, realiza BOOLEAN NOT NULL);
CREATE TABLE rol_integrante (codigo VARCHAR(50) PRIMARY KEY, nombre VARCHAR(100) NOT NULL, orden INTEGER NOT NULL);
CREATE TABLE sesion_semillero (
    id_sesion BIGINT PRIMARY KEY, id_semillero BIGINT NOT NULL, id_actividad BIGINT, fecha DATE NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP, fecha_actualizacion TIMESTAMP
);
CREATE TABLE asistencia_sesion (id_sesion BIGINT NOT NULL, id_integrante BIGINT NOT NULL, estado VARCHAR(20) NOT NULL);
CREATE TABLE inscripcion (id_inscripcion BIGINT PRIMARY KEY, estado VARCHAR(20), fecha_actualizacion TIMESTAMP);

INSERT INTO campus VALUES (1, 'Medellín'), (2, 'Apartadó'), (3, 'Caucasia');

INSERT INTO unidad_academica VALUES
(1, 'Facultad de Ingeniería', 1),
(2, 'Escuela de Idiomas', 1),
(3, 'Corporación Académica Ambiental', 1),
(4, 'Seccional Apartadó', 2);

INSERT INTO semillero VALUES
(1, 'SEM-1', 'Semillero IA', 2022, 'ACTIVO',   1, 1, 10, NULL),
(2, 'SEM-2', 'Robótica',     2024, 'ACTIVO',   1, 1, 11, NULL),
(3, 'SEM-3', 'Lenguas',      2025, 'ACTIVO',   2, 1, 10, NULL),
(4, 'SEM-4', 'Ambiental',    NULL, 'ACTIVO',   3, 2, 12, NULL),
(5, 'SEM-5', 'Inactivo',     2023, 'INACTIVO', 1, 1, 10, NULL),
(6, 'SEM-6', NULL,           NULL, 'BORRADOR', 1, 1, 10, NULL);

-- La persona A pertenece a dos semilleros; C está inactiva; E está en un semillero inactivo
INSERT INTO semillero_integrante VALUES
(1, 1, 'A', 'FEMENINO',  'ESTUDIANTE_INVESTIGADOR', TRUE,  DATE '2024-03-01'),
(2, 1, 'B', 'MASCULINO', 'TUTOR',                   TRUE,  DATE '2025-08-01'),
(3, 2, 'A', 'FEMENINO',  'AUXILIAR',                TRUE,  DATE '2025-02-01'),
(4, 2, 'C', NULL,        'ESTUDIANTE_INVESTIGADOR', FALSE, DATE '2023-05-01'),
(5, 3, 'D', 'OTRO',      'ESTUDIANTE_INVESTIGADOR', TRUE,  NULL),
(6, 5, 'E', 'FEMENINO',  'ESTUDIANTE_INVESTIGADOR', TRUE,  DATE '2024-01-01');

INSERT INTO actividad_cientifica VALUES (1, 'Seminarios'), (2, 'Talleres'), (3, 'Conversatorios');

INSERT INTO semillero_actividad VALUES
(1, 1, TRUE), (1, 2, TRUE), (2, 2, TRUE), (3, 2, FALSE), (3, 3, TRUE), (5, 1, TRUE);

INSERT INTO rol_integrante VALUES
('ESTUDIANTE_INVESTIGADOR', 'Estudiante Investigador', 1),
('AUXILIAR', 'Auxiliar', 2),
('COORDINADOR', 'Coordinador', 3),
('TUTOR', 'Tutor', 4),
('SEMILLERISTA_JUNIOR', 'Semillerista Junior', 5);

INSERT INTO inscripcion VALUES (1, 'PENDIENTE', NULL);

-- Sesiones: dos del semillero 1, una del 2 y una del semillero inactivo 5
INSERT INTO sesion_semillero (id_sesion, id_semillero, id_actividad, fecha) VALUES
(1, 1, 1, DATE '2025-03-10'), (2, 1, 2, DATE '2026-02-01'), (3, 2, 2, DATE '2026-08-01'), (4, 5, 1, DATE '2026-01-10');
INSERT INTO asistencia_sesion VALUES
(1, 1, 'PRESENTE'), (1, 2, 'AUSENTE'),
(2, 1, 'PRESENTE'), (2, 2, 'EXCUSADO'),
(3, 3, 'AUSENTE'),
(4, 6, 'PRESENTE');
