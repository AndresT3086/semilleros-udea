-- ============================================================
-- V2__datos_referencia.sql
-- Datos de referencia: campus, unidades, áreas OCDE, ODS,
-- recursos, fuentes de financiación y actividades científicas
-- ============================================================

-- ----------------------
-- CAMPUS
-- ----------------------
INSERT INTO campus (nombre, ciudad, departamento, direccion) VALUES
('Ciudad Universitaria (Medellín)', 'Medellín',    'Antioquia', 'Calle 67 No. 53-108'),
('Apartadó',                        'Apartadó',    'Antioquia', 'Carretera al Mar'),
('Caucasia',                        'Caucasia',    'Antioquia', 'Cra. 14 No. 14-23'),
('Ituango',                         'Ituango',     'Antioquia', 'Calle 20 No. 18-52'),
('Marinilla',                       'Marinilla',   'Antioquia', 'Vía al Aeropuerto'),
('Sonsón',                          'Sonsón',      'Antioquia', 'Calle 9 No. 7-25'),
('Turbo',                           'Turbo',       'Antioquia', 'Carretera al Puerto'),
('Yarumal',                         'Yarumal',     'Antioquia', 'Cra. 20 No. 22-10')
ON CONFLICT DO NOTHING;

-- ----------------------
-- UNIDADES ACADÉMICAS
-- ----------------------
INSERT INTO unidad_academica (nombre, siglas, id_campus) VALUES
('Facultad de Ingeniería',                    'FING',  1),
('Facultad de Ciencias Exactas y Naturales',  'FCEN',  1),
('Facultad de Medicina',                      'FMED',  1),
('Facultad de Ciencias Sociales y Humanas',   'FCSH',  1),
('Facultad de Derecho y Ciencias Políticas',  'FDCP',  1),
('Facultad de Educación',                     'FEDU',  1),
('Facultad de Ciencias Económicas',           'FECO',  1),
('Facultad de Comunicaciones y Filología',    'FCF',   1),
('Facultad de Artes',                         'FART',  1),
('Facultad de Ciencias Agrarias',             'FCAG',  1),
('Facultad de Ciencias Farmacéuticas y Alimentarias', 'FCFA', 1),
('Facultad de Enfermería',                    'FENF',  1),
('Facultad de Odontología',                   'FODO',  1),
('Escuela de Idiomas',                        'EIID',  1),
('Escuela de Microbiología',                  'EMIC',  1),
('Escuela de Nutrición y Dietética',          'ENYD',  1),
('Instituto de Educación Física',             'IEF',   1),
('Instituto de Filosofía',                    'IFIL',  1),
('Seccional Apartadó',                        'SAPT',  2),
('Seccional Caucasia',                        'SCAU',  3)
ON CONFLICT DO NOTHING;

-- ----------------------
-- ÁREAS OCDE
-- ----------------------
INSERT INTO area_ocde (nombre, descripcion) VALUES
('Ciencias Naturales',                          'Matemáticas, Ciencias de la computación, Física, Química, Ciencias de la Tierra'),
('Ingeniería y Tecnología',                     'Ingeniería Civil, Eléctrica, Electrónica, Mecánica, Química, Materiales, Médica, Ambiental, Industrial'),
('Ciencias Médicas y de la Salud',              'Medicina Básica, Medicina Clínica, Ciencias de la Salud, Biotecnología Médica'),
('Ciencias Agrícolas',                          'Agricultura, Silvicultura y Pesca, Ciencias Animales y Lácteos, Veterinaria, Biotecnología Agrícola'),
('Ciencias Sociales',                           'Psicología, Economía y Negocios, Ciencias de la Educación, Sociología, Derecho, Ciencias Políticas, Geografía Social'),
('Humanidades',                                 'Historia y Arqueología, Lenguas y Literatura, Filosofía, Ética y Religión, Arte')
ON CONFLICT DO NOTHING;

-- ----------------------
-- ODS (17 Objetivos de Desarrollo Sostenible)
-- ----------------------
INSERT INTO ods (numero, nombre) VALUES
(1,  'Fin de la pobreza'),
(2,  'Hambre cero'),
(3,  'Salud y bienestar'),
(4,  'Educación de calidad'),
(5,  'Igualdad de género'),
(6,  'Agua limpia y saneamiento'),
(7,  'Energía asequible y no contaminante'),
(8,  'Trabajo decente y crecimiento económico'),
(9,  'Industria, innovación e infraestructura'),
(10, 'Reducción de las desigualdades'),
(11, 'Ciudades y comunidades sostenibles'),
(12, 'Producción y consumo responsables'),
(13, 'Acción por el clima'),
(14, 'Vida submarina'),
(15, 'Vida de ecosistemas terrestres'),
(16, 'Paz, justicia e instituciones sólidas'),
(17, 'Alianzas para lograr los objetivos')
ON CONFLICT DO NOTHING;

-- ----------------------
-- RECURSOS
-- ----------------------
INSERT INTO recurso (nombre) VALUES
('Laboratorio'),
('Sala de cómputo'),
('Espacio físico propio'),
('Biblioteca / Bases de datos'),
('Equipos de medición'),
('Financiación externa'),
('Software especializado'),
('Instrumentos musicales o artísticos'),
('Semillero virtual / plataforma digital'),
('Ninguno')
ON CONFLICT DO NOTHING;

-- ----------------------
-- FUENTES DE FINANCIACIÓN
-- ----------------------
INSERT INTO fuente_financiacion (nombre) VALUES
('Recursos propios de la universidad'),
('Convocatoria interna UdeA'),
('Minciencias'),
('Gobernación de Antioquia'),
('Alcaldía'),
('Empresa privada'),
('Cooperación internacional'),
('ONG'),
('Sin financiación'),
('Otra')
ON CONFLICT DO NOTHING;

-- ----------------------
-- ACTIVIDADES CIENTÍFICAS
-- ----------------------
INSERT INTO actividad_cientifica (nombre, categoria) VALUES
('Investigación formativa',               'Investigación'),
('Participación en proyectos de investigación', 'Investigación'),
('Publicación de artículos científicos',  'Producción académica'),
('Ponencias en eventos nacionales',       'Difusión'),
('Ponencias en eventos internacionales',  'Difusión'),
('Semillero de lectura y escritura',      'Formación'),
('Talleres y capacitaciones internas',    'Formación'),
('Visitas técnicas',                      'Extensión'),
('Proyectos de extensión comunitaria',    'Extensión'),
('Participación en ferias de ciencia',    'Difusión'),
('Colaboración con grupos de investigación', 'Articulación'),
('Movilidad académica',                   'Internacionalización')
ON CONFLICT DO NOTHING;
