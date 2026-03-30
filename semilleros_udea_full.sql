-- ============================================================
-- semilleros_udea_full.sql
-- Script completo de creación de tablas y datos de ejemplo
-- Sistema de Semilleros de Investigación - Universidad de Antioquia
-- Compatible con PostgreSQL 14+
-- Uso: psql -U <usuario> -d <base_de_datos> -f semilleros_udea_full.sql
-- ============================================================

-- Asegurar extensiones necesarias
CREATE EXTENSION IF NOT EXISTS unaccent;

-- ─── LIMPIEZA PREVIA (sólo desarrollo) ──────────────────────
-- Descomentar sólo si se desea recrear desde cero en DEV:
/*
DROP TABLE IF EXISTS semillero_actividad   CASCADE;
DROP TABLE IF EXISTS semillero_evento      CASCADE;
DROP TABLE IF EXISTS semillero_ods         CASCADE;
DROP TABLE IF EXISTS semillero_financiacion CASCADE;
DROP TABLE IF EXISTS semillero_recurso     CASCADE;
DROP TABLE IF EXISTS semillero_integrante  CASCADE;
DROP TABLE IF EXISTS produccion_academica  CASCADE;
DROP TABLE IF EXISTS dofa                  CASCADE;
DROP TABLE IF EXISTS inscripcion           CASCADE;
DROP TABLE IF EXISTS semillero             CASCADE;
DROP TABLE IF EXISTS coordinador           CASCADE;
DROP TABLE IF EXISTS actividad_cientifica  CASCADE;
DROP TABLE IF EXISTS evento                CASCADE;
DROP TABLE IF EXISTS fuente_financiacion   CASCADE;
DROP TABLE IF EXISTS recurso               CASCADE;
DROP TABLE IF EXISTS ods                   CASCADE;
DROP TABLE IF EXISTS area_ocde             CASCADE;
DROP TABLE IF EXISTS programa_academico    CASCADE;
DROP TABLE IF EXISTS unidad_academica      CASCADE;
DROP TABLE IF EXISTS campus                CASCADE;
*/

-- ============================================================
-- TABLAS
-- ============================================================

CREATE TABLE IF NOT EXISTS campus (
    id_campus    SERIAL PRIMARY KEY,
    nombre       VARCHAR(150) NOT NULL,
    ciudad       VARCHAR(100),
    departamento VARCHAR(100),
    direccion    VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS unidad_academica (
    id_unidad  SERIAL PRIMARY KEY,
    nombre     VARCHAR(200) NOT NULL,
    siglas     VARCHAR(20),
    id_campus  INTEGER NOT NULL,
    CONSTRAINT fk_unidad_campus FOREIGN KEY (id_campus) REFERENCES campus (id_campus)
);

CREATE TABLE IF NOT EXISTS programa_academico (
    id_programa INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre      VARCHAR(200) NOT NULL,
    codigo      VARCHAR(20),
    id_unidad   INTEGER NOT NULL,
    CONSTRAINT fk_programa_unidad FOREIGN KEY (id_unidad) REFERENCES unidad_academica (id_unidad)
);

CREATE TABLE IF NOT EXISTS area_ocde (
    id_area     SERIAL PRIMARY KEY,
    nombre      VARCHAR(200) NOT NULL,
    descripcion TEXT
);

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

CREATE TABLE IF NOT EXISTS semillero (
    id_semillero           SERIAL PRIMARY KEY,
    codigo                 VARCHAR(30)  NOT NULL UNIQUE,
    nombre                 VARCHAR(300) NOT NULL,
    siglas                 VARCHAR(30),
    correo_principal       VARCHAR(150),
    telefono               VARCHAR(20),
    anio_creacion          INTEGER,
    mision                 TEXT,
    vision                 TEXT,
    objetivo               TEXT,
    lineas_investigacion   TEXT,
    palabras_clave         VARCHAR(500),
    grupo_investigacion    VARCHAR(200),
    estado                 VARCHAR(30)  NOT NULL DEFAULT 'BORRADOR'
                               CHECK (estado IN ('ACTIVO','INACTIVO','BORRADOR','CARACTERIZADO')),
    estado_caracterizacion VARCHAR(50)  DEFAULT 'GENERAL_PENDIENTE',
    id_unidad_academica    INTEGER,
    id_campus              INTEGER,
    id_area_ocde           INTEGER,
    id_coordinador         INTEGER,
    fecha_creacion         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    fecha_actualizacion    TIMESTAMPTZ,
    CONSTRAINT fk_semillero_unidad      FOREIGN KEY (id_unidad_academica) REFERENCES unidad_academica (id_unidad),
    CONSTRAINT fk_semillero_campus      FOREIGN KEY (id_campus)           REFERENCES campus (id_campus),
    CONSTRAINT fk_semillero_area        FOREIGN KEY (id_area_ocde)         REFERENCES area_ocde (id_area),
    CONSTRAINT fk_semillero_coordinador FOREIGN KEY (id_coordinador)       REFERENCES coordinador (id_coordinador)
);

CREATE TABLE IF NOT EXISTS inscripcion (
    id_inscripcion      SERIAL PRIMARY KEY,
    id_semillero        INTEGER      NOT NULL,
    nombres             VARCHAR(100) NOT NULL,
    apellidos           VARCHAR(100) NOT NULL,
    cedula              VARCHAR(15)  NOT NULL CHECK (cedula ~ '^[0-9]{7,10}$'),
    correo              VARCHAR(150) NOT NULL,
    telefono            VARCHAR(15)  NOT NULL CHECK (telefono ~ '^[0-9]{10}$'),
    programa            VARCHAR(200),
    semestre            VARCHAR(10),
    motivacion          TEXT,
    acepta_terminos     BOOLEAN      NOT NULL DEFAULT FALSE,
    estado              VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE'
                            CHECK (estado IN ('PENDIENTE','APROBADO','RECHAZADO')),
    fecha_inscripcion   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMPTZ,
    CONSTRAINT fk_inscripcion_semillero FOREIGN KEY (id_semillero) REFERENCES semillero (id_semillero)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_inscripcion_activa
    ON inscripcion (correo, id_semillero)
    WHERE estado IN ('PENDIENTE', 'APROBADO');

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

CREATE TABLE IF NOT EXISTS recurso (
    id_recurso  SERIAL PRIMARY KEY,
    nombre      VARCHAR(150) NOT NULL,
    descripcion TEXT
);

CREATE TABLE IF NOT EXISTS semillero_recurso (
    id_semillero INTEGER NOT NULL,
    id_recurso   INTEGER NOT NULL,
    PRIMARY KEY (id_semillero, id_recurso),
    CONSTRAINT fk_sr_semillero FOREIGN KEY (id_semillero) REFERENCES semillero (id_semillero),
    CONSTRAINT fk_sr_recurso   FOREIGN KEY (id_recurso)   REFERENCES recurso (id_recurso)
);

CREATE TABLE IF NOT EXISTS fuente_financiacion (
    id_fuente   SERIAL PRIMARY KEY,
    nombre      VARCHAR(150) NOT NULL,
    descripcion TEXT
);

CREATE TABLE IF NOT EXISTS semillero_financiacion (
    id_semillero INTEGER NOT NULL,
    id_fuente    INTEGER NOT NULL,
    PRIMARY KEY (id_semillero, id_fuente),
    CONSTRAINT fk_sf_semillero FOREIGN KEY (id_semillero) REFERENCES semillero (id_semillero),
    CONSTRAINT fk_sf_fuente    FOREIGN KEY (id_fuente)    REFERENCES fuente_financiacion (id_fuente)
);

CREATE TABLE IF NOT EXISTS ods (
    id_ods   SERIAL PRIMARY KEY,
    numero   INTEGER      NOT NULL,
    nombre   VARCHAR(200) NOT NULL,
    icono    VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS semillero_ods (
    id_semillero INTEGER NOT NULL,
    id_ods       INTEGER NOT NULL,
    PRIMARY KEY (id_semillero, id_ods),
    CONSTRAINT fk_so_semillero FOREIGN KEY (id_semillero) REFERENCES semillero (id_semillero),
    CONSTRAINT fk_so_ods       FOREIGN KEY (id_ods)       REFERENCES ods (id_ods)
);

CREATE TABLE IF NOT EXISTS dofa (
    id_dofa      SERIAL PRIMARY KEY,
    id_semillero INTEGER     NOT NULL,
    tipo         VARCHAR(20) NOT NULL
                     CHECK (tipo IN ('FORTALEZA','DEBILIDAD','OPORTUNIDAD','AMENAZA')),
    descripcion  TEXT        NOT NULL,
    CONSTRAINT fk_dofa_semillero FOREIGN KEY (id_semillero) REFERENCES semillero (id_semillero)
);

CREATE TABLE IF NOT EXISTS actividad_cientifica (
    id_actividad SERIAL PRIMARY KEY,
    nombre       VARCHAR(200) NOT NULL,
    categoria    VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS semillero_actividad (
    id_semillero INTEGER NOT NULL,
    id_actividad INTEGER NOT NULL,
    realiza      BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id_semillero, id_actividad),
    CONSTRAINT fk_sa_semillero FOREIGN KEY (id_semillero) REFERENCES semillero (id_semillero),
    CONSTRAINT fk_sa_actividad FOREIGN KEY (id_actividad) REFERENCES actividad_cientifica (id_actividad)
);

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

CREATE TABLE IF NOT EXISTS evento (
    id_evento    SERIAL PRIMARY KEY,
    nombre       VARCHAR(300) NOT NULL,
    tipo         VARCHAR(100),
    fecha        DATE,
    ciudad       VARCHAR(100),
    pais         VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS semillero_evento (
    id              SERIAL PRIMARY KEY,
    id_semillero    INTEGER NOT NULL,
    id_evento       INTEGER NOT NULL,
    modalidad       VARCHAR(50),
    titulo_ponencia VARCHAR(400),
    anio            INTEGER,
    CONSTRAINT fk_se_semillero FOREIGN KEY (id_semillero) REFERENCES semillero (id_semillero),
    CONSTRAINT fk_se_evento    FOREIGN KEY (id_evento)    REFERENCES evento (id_evento)
);

-- ─── Índices ─────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_semillero_estado       ON semillero (estado);
CREATE INDEX IF NOT EXISTS idx_semillero_unidad       ON semillero (id_unidad_academica);
CREATE INDEX IF NOT EXISTS idx_semillero_campus       ON semillero (id_campus);
CREATE INDEX IF NOT EXISTS idx_semillero_area         ON semillero (id_area_ocde);
CREATE INDEX IF NOT EXISTS idx_semillero_coordinador  ON semillero (id_coordinador);
CREATE INDEX IF NOT EXISTS idx_semillero_nombre_lower ON semillero (LOWER(nombre));
CREATE INDEX IF NOT EXISTS idx_inscripcion_correo     ON inscripcion (correo);
CREATE INDEX IF NOT EXISTS idx_inscripcion_semillero  ON inscripcion (id_semillero);
CREATE INDEX IF NOT EXISTS idx_coordinador_correo     ON coordinador (correo);

-- ============================================================
-- DATOS DE REFERENCIA
-- ============================================================

INSERT INTO campus (nombre, ciudad, departamento, direccion) VALUES
('Ciudad Universitaria (Medellín)', 'Medellín',  'Antioquia', 'Calle 67 No. 53-108'),
('Apartadó',                        'Apartadó',  'Antioquia', 'Carretera al Mar'),
('Caucasia',                        'Caucasia',  'Antioquia', 'Cra. 14 No. 14-23'),
('Ituango',                         'Ituango',   'Antioquia', 'Calle 20 No. 18-52'),
('Marinilla',                       'Marinilla', 'Antioquia', 'Vía al Aeropuerto'),
('Sonsón',                          'Sonsón',    'Antioquia', 'Calle 9 No. 7-25'),
('Turbo',                           'Turbo',     'Antioquia', 'Carretera al Puerto'),
('Yarumal',                         'Yarumal',   'Antioquia', 'Cra. 20 No. 22-10')
ON CONFLICT DO NOTHING;

INSERT INTO unidad_academica (nombre, siglas, id_campus) VALUES
('Facultad de Ingeniería',                              'FING',  1),
('Facultad de Ciencias Exactas y Naturales',            'FCEN',  1),
('Facultad de Medicina',                                'FMED',  1),
('Facultad de Ciencias Sociales y Humanas',             'FCSH',  1),
('Facultad de Derecho y Ciencias Políticas',            'FDCP',  1),
('Facultad de Educación',                               'FEDU',  1),
('Facultad de Ciencias Económicas',                     'FECO',  1),
('Facultad de Comunicaciones y Filología',              'FCF',   1),
('Facultad de Artes',                                   'FART',  1),
('Facultad de Ciencias Agrarias',                       'FCAG',  1),
('Facultad de Ciencias Farmacéuticas y Alimentarias',   'FCFA',  1),
('Facultad de Enfermería',                              'FENF',  1),
('Facultad de Odontología',                             'FODO',  1),
('Escuela de Idiomas',                                  'EIID',  1),
('Escuela de Microbiología',                            'EMIC',  1),
('Escuela de Nutrición y Dietética',                    'ENYD',  1),
('Instituto de Educación Física',                       'IEF',   1),
('Instituto de Filosofía',                              'IFIL',  1),
('Seccional Apartadó',                                  'SAPT',  2),
('Seccional Caucasia',                                  'SCAU',  3)
ON CONFLICT DO NOTHING;

INSERT INTO area_ocde (nombre, descripcion) VALUES
('Ciencias Naturales',             'Matemáticas, Física, Química, Ciencias de la Tierra'),
('Ingeniería y Tecnología',        'Ingeniería Civil, Eléctrica, Mecánica, Química, de Sistemas'),
('Ciencias Médicas y de la Salud', 'Medicina Básica, Clínica, Biotecnología Médica'),
('Ciencias Agrícolas',             'Agricultura, Ciencias Animales, Veterinaria'),
('Ciencias Sociales',              'Psicología, Economía, Educación, Sociología, Derecho'),
('Humanidades',                    'Historia, Lenguas, Filosofía, Arte')
ON CONFLICT DO NOTHING;

INSERT INTO ods (numero, nombre) VALUES
(1,'Fin de la pobreza'),(2,'Hambre cero'),(3,'Salud y bienestar'),
(4,'Educación de calidad'),(5,'Igualdad de género'),(6,'Agua limpia y saneamiento'),
(7,'Energía asequible y no contaminante'),(8,'Trabajo decente y crecimiento económico'),
(9,'Industria, innovación e infraestructura'),(10,'Reducción de las desigualdades'),
(11,'Ciudades y comunidades sostenibles'),(12,'Producción y consumo responsables'),
(13,'Acción por el clima'),(14,'Vida submarina'),(15,'Vida de ecosistemas terrestres'),
(16,'Paz, justicia e instituciones sólidas'),(17,'Alianzas para lograr los objetivos')
ON CONFLICT DO NOTHING;

INSERT INTO recurso (nombre) VALUES
('Laboratorio'),('Sala de cómputo'),('Espacio físico propio'),
('Biblioteca / Bases de datos'),('Equipos de medición'),('Financiación externa'),
('Software especializado'),('Instrumentos musicales o artísticos'),
('Semillero virtual / plataforma digital'),('Ninguno')
ON CONFLICT DO NOTHING;

INSERT INTO fuente_financiacion (nombre) VALUES
('Recursos propios de la universidad'),('Convocatoria interna UdeA'),
('Minciencias'),('Gobernación de Antioquia'),('Alcaldía'),
('Empresa privada'),('Cooperación internacional'),('ONG'),
('Sin financiación'),('Otra')
ON CONFLICT DO NOTHING;

INSERT INTO actividad_cientifica (nombre, categoria) VALUES
('Investigación formativa',                    'Investigación'),
('Participación en proyectos de investigación', 'Investigación'),
('Publicación de artículos científicos',        'Producción académica'),
('Ponencias en eventos nacionales',             'Difusión'),
('Ponencias en eventos internacionales',        'Difusión'),
('Semillero de lectura y escritura',            'Formación'),
('Talleres y capacitaciones internas',          'Formación'),
('Visitas técnicas',                            'Extensión'),
('Proyectos de extensión comunitaria',          'Extensión'),
('Participación en ferias de ciencia',          'Difusión'),
('Colaboración con grupos de investigación',    'Articulación'),
('Movilidad académica',                         'Internacionalización')
ON CONFLICT DO NOTHING;

-- ============================================================
-- DATOS DE EJEMPLO
-- Contraseña para todos los coordinadores: UdeA2024*
-- ============================================================

INSERT INTO coordinador (nombres, apellidos, correo, password_hash, telefono, rol) VALUES
('Carlos',  'Hernández Ríos',  'carlos.hernandez@udea.edu.co',
 '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBpj0V.GmpTWie','3001234567','COORDINADOR'),
('María',   'González López',  'maria.gonzalez@udea.edu.co',
 '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBpj0V.GmpTWie','3009876543','COORDINADOR'),
('Andrés',  'Martínez Duque',  'andres.martinez@udea.edu.co',
 '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBpj0V.GmpTWie','3005551234','COORDINADOR'),
('Luisa',   'Ramírez Patiño',  'luisa.ramirez@udea.edu.co',
 '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBpj0V.GmpTWie','3112223344','COORDINADOR'),
('Jorge',   'Valencia Osorio', 'jorge.valencia@udea.edu.co',
 '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBpj0V.GmpTWie','3204445566','COORDINADOR')
ON CONFLICT (correo) DO NOTHING;

INSERT INTO semillero (
    codigo, nombre, siglas, correo_principal, telefono, anio_creacion,
    mision, vision, objetivo, lineas_investigacion, palabras_clave,
    grupo_investigacion, estado, estado_caracterizacion,
    id_unidad_academica, id_campus, id_area_ocde, id_coordinador
) VALUES
('SEM-UDEA-0001','Semillero de Inteligencia Artificial y Machine Learning','SIAML',
 'siaml@udea.edu.co','3001112233',2018,
 'Formar estudiantes en fundamentos y aplicaciones de inteligencia artificial.',
 'Ser referente regional en investigación formativa sobre IA y ML.',
 'Desarrollar proyectos de investigación formativa en inteligencia artificial.',
 'Aprendizaje profundo, NLP, Visión computacional',
 'inteligencia artificial, machine learning, deep learning',
 'GIDIA - Grupo de Investigación en Inteligencia Artificial',
 'ACTIVO','COMPLETO',1,1,2,1),
('SEM-UDEA-0002','Semillero de Salud Pública y Epidemiología','SSPE',
 'sspe@udea.edu.co','3007778899',2015,
 'Contribuir a la formación en salud pública mediante investigación epidemiológica.',
 'Constituirnos en referente de investigación formativa en salud pública.',
 'Analizar los determinantes de la salud colectiva.',
 'Epidemiología social, Salud mental, Vigilancia epidemiológica',
 'salud pública, epidemiología, determinantes sociales',
 'Grupo de Epidemiología - Facultad de Medicina',
 'ACTIVO','COMPLETO',3,1,3,2),
('SEM-UDEA-0003','Semillero de Derecho Ambiental y Sostenibilidad','SDAS',
 'sdas@udea.edu.co','3113334455',2020,
 'Promover el estudio del derecho ambiental y la justicia ecológica.',
 'Ser reconocidos como semillero pionero en derecho ambiental.',
 'Investigar marcos normativos en materia ambiental.',
 'Derecho ambiental, Litigio estratégico, Justicia climática',
 'derecho ambiental, sostenibilidad, ODS',
 'Grupo de Investigación en Derecho y Sociedad',
 'ACTIVO','COMPLETO',5,1,5,3),
('SEM-UDEA-0004','Semillero de Educación Matemática','SEM',
 'sem@udea.edu.co','3224445566',2017,
 'Fomentar la reflexión didáctica y pedagógica en matemáticas.',
 'Convertirnos en espacio de innovación didáctica en matemáticas.',
 'Diseñar estrategias didácticas para la enseñanza de las matemáticas.',
 'Didáctica de matemáticas, TIC en matemáticas',
 'educación matemática, didáctica, innovación pedagógica',
 'Grupo de Investigación en Educación Matemática',
 'ACTIVO','COMPLETO',6,1,6,4),
('SEM-UDEA-0005','Semillero de Biotecnología Agrícola','SBAG',
 'sbag@udea.edu.co','3005556677',2019,
 'Impulsar el conocimiento biotecnológico aplicado a la mejora de cultivos.',
 'Ser referente regional en biotecnología agrícola.',
 'Desarrollar investigación en mejoramiento genético vegetal.',
 'Mejoramiento genético, Biopesticidas, Agricultura de precisión',
 'biotecnología, agricultura, mejoramiento genético',
 'Grupo de Biotecnología Vegetal',
 'ACTIVO','COMPLETO',10,1,4,5)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO inscripcion (id_semillero,nombres,apellidos,cedula,correo,telefono,
    programa,semestre,motivacion,acepta_terminos,estado)
VALUES
(1,'Valentina','Suárez Torres','1040123456','valentina.suarez@udea.edu.co','3001234567',
 'Ingeniería de Sistemas','6','Me apasiona la IA.',TRUE,'PENDIENTE'),
(1,'Santiago','Cano Restrepo','1035987654','santiago.cano@udea.edu.co','3009988776',
 'Ingeniería de Sistemas','8','Tengo experiencia en Python.',TRUE,'APROBADO'),
(2,'Laura','Ospina Vélez','1027112233','laura.ospina@udea.edu.co','3116677889',
 'Medicina','4','Me interesa la epidemiología comunitaria.',TRUE,'PENDIENTE'),
(3,'Mateo','Giraldo Moreno','1061445566','mateo.giraldo@udea.edu.co','3224455667',
 'Derecho','7','Quiero profundizar en litigio ambiental.',TRUE,'PENDIENTE')
ON CONFLICT DO NOTHING;
