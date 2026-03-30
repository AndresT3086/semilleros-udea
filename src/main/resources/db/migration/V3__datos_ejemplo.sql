-- ============================================================
-- V3__datos_ejemplo.sql
-- Datos de ejemplo para pruebas en entornos dev y cert.
-- Contraseñas hasheadas con BCrypt (factor 12).
-- Password de ejemplo para todos: "UdeA2024*"
-- ============================================================

-- ----------------------
-- COORDINADORES DE EJEMPLO
-- Contraseña: UdeA2024*  (BCrypt factor 12)
-- ----------------------
INSERT INTO coordinador (nombres, apellidos, correo, password_hash, telefono, rol, activo) VALUES
('Carlos',    'Hernández Ríos',    'carlos.hernandez@udea.edu.co',
 '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBpj0V.GmpTWie', '3001234567', 'COORDINADOR', TRUE),
('María',     'González López',    'maria.gonzalez@udea.edu.co',
 '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBpj0V.GmpTWie', '3009876543', 'COORDINADOR', TRUE),
('Andrés',    'Martínez Duque',    'andres.martinez@udea.edu.co',
 '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBpj0V.GmpTWie', '3005551234', 'COORDINADOR', TRUE),
('Luisa',     'Ramírez Patiño',    'luisa.ramirez@udea.edu.co',
 '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBpj0V.GmpTWie', '3112223344', 'COORDINADOR', TRUE),
('Jorge',     'Valencia Osorio',   'jorge.valencia@udea.edu.co',
 '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBpj0V.GmpTWie', '3204445566', 'COORDINADOR', TRUE)
ON CONFLICT (correo) DO NOTHING;

-- ----------------------
-- SEMILLEROS DE EJEMPLO
-- ----------------------
INSERT INTO semillero (
    codigo, nombre, siglas, correo_principal, telefono, anio_creacion,
    mision, vision, objetivo, lineas_investigacion, palabras_clave,
    grupo_investigacion, estado, estado_caracterizacion,
    id_unidad_academica, id_campus, id_area_ocde, id_coordinador
) VALUES
(
    'SEM-UDEA-0001',
    'Semillero de Inteligencia Artificial y Machine Learning',
    'SIAML',
    'siaml@udea.edu.co',
    '3001112233',
    2018,
    'Formar estudiantes en fundamentos y aplicaciones de inteligencia artificial y aprendizaje automático, fomentando la investigación formativa y el pensamiento computacional.',
    'Ser un referente regional en investigación formativa sobre IA y ML, contribuyendo al desarrollo tecnológico y social de Colombia.',
    'Desarrollar proyectos de investigación formativa en inteligencia artificial, aprendizaje profundo y procesamiento de datos.',
    'Aprendizaje profundo, Procesamiento de lenguaje natural, Visión computacional, Sistemas de recomendación',
    'inteligencia artificial, machine learning, deep learning, redes neuronales, NLP',
    'GIDIA - Grupo de Investigación en Inteligencia Artificial',
    'ACTIVO',
    'COMPLETO',
    1, 1, 2, 1
),
(
    'SEM-UDEA-0002',
    'Semillero de Salud Pública y Epidemiología',
    'SSPE',
    'sspe@udea.edu.co',
    '3007778899',
    2015,
    'Contribuir a la formación de talento humano en salud pública mediante investigación epidemiológica y análisis de determinantes sociales de la salud.',
    'Constituirnos en un espacio referente de investigación formativa en salud pública, con impacto en las políticas locales y regionales.',
    'Analizar los determinantes de la salud colectiva e impulsar propuestas de intervención basadas en evidencia.',
    'Epidemiología social, Salud mental comunitaria, Determinantes sociales de salud, Vigilancia epidemiológica',
    'salud pública, epidemiología, determinantes sociales, vigilancia, comunidad',
    'Grupo de Epidemiología - Facultad de Medicina',
    'ACTIVO',
    'COMPLETO',
    3, 1, 3, 2
),
(
    'SEM-UDEA-0003',
    'Semillero de Derecho Ambiental y Sostenibilidad',
    'SDAS',
    'sdas@udea.edu.co',
    '3113334455',
    2020,
    'Promover el estudio crítico del derecho ambiental y la justicia ecológica, formando juristas comprometidos con la sostenibilidad.',
    'Ser reconocidos como semillero pionero en la articulación entre derecho, medio ambiente y desarrollo sostenible en la región andina.',
    'Investigar los marcos normativos nacionales e internacionales en materia ambiental y proponer instrumentos de política pública.',
    'Derecho ambiental, Litigio estratégico ambiental, Justicia climática, ODS y marco legal',
    'derecho ambiental, sostenibilidad, ODS, litigio climático, justicia ecológica',
    'Grupo de Investigación en Derecho y Sociedad',
    'ACTIVO',
    'COMPLETO',
    5, 1, 5, 3
),
(
    'SEM-UDEA-0004',
    'Semillero de Educación Matemática',
    'SEM',
    'sem@udea.edu.co',
    '3224445566',
    2017,
    'Fomentar la reflexión didáctica y pedagógica en matemáticas, articulando teoría y práctica en contextos de aula.',
    'Convertirnos en un espacio de innovación didáctica en matemáticas con proyección a nivel nacional.',
    'Diseñar y evaluar estrategias didácticas innovadoras para la enseñanza y aprendizaje de las matemáticas.',
    'Didáctica de las matemáticas, Pensamiento numérico, Resolución de problemas, TIC en matemáticas',
    'educación matemática, didáctica, innovación pedagógica, pensamiento matemático',
    'Grupo de Investigación en Educación Matemática',
    'ACTIVO',
    'COMPLETO',
    6, 1, 6, 4
),
(
    'SEM-UDEA-0005',
    'Semillero de Biotecnología Agrícola',
    'SBAG',
    'sbag@udea.edu.co',
    '3005556677',
    2019,
    'Impulsar el conocimiento biotecnológico aplicado a la mejora de cultivos y la producción agrícola sostenible en Antioquia.',
    'Ser un referente regional en biotecnología agrícola, contribuyendo a la soberanía alimentaria y la innovación rural.',
    'Desarrollar investigación formativa en mejoramiento genético vegetal, biopesticidas y agricultura de precisión.',
    'Mejoramiento genético de cultivos, Biopesticidas, Agricultura de precisión, Producción sostenible',
    'biotecnología, agricultura, mejoramiento genético, sostenibilidad, cultivos',
    'Grupo de Biotecnología Vegetal',
    'ACTIVO',
    'COMPLETO',
    10, 1, 4, 5
)
ON CONFLICT (codigo) DO NOTHING;

-- ----------------------
-- INSCRIPCIONES DE EJEMPLO
-- ----------------------
INSERT INTO inscripcion (
    id_semillero, nombres, apellidos, cedula, correo, telefono,
    programa, semestre, motivacion, acepta_terminos, estado
) VALUES
(
    1, 'Valentina', 'Suárez Torres', '1040123456',
    'valentina.suarez@udea.edu.co', '3001234567',
    'Ingeniería de Sistemas', '6',
    'Me apasiona la inteligencia artificial y deseo contribuir con proyectos de investigación formativa.',
    TRUE, 'PENDIENTE'
),
(
    1, 'Santiago', 'Cano Restrepo', '1035987654',
    'santiago.cano@udea.edu.co', '3009988776',
    'Ingeniería de Sistemas', '8',
    'Tengo experiencia en Python y deseo profundizar en redes neuronales.',
    TRUE, 'APROBADO'
),
(
    2, 'Laura', 'Ospina Vélez', '1027112233',
    'laura.ospina@udea.edu.co', '3116677889',
    'Medicina', '4',
    'Me interesa la epidemiología comunitaria y la salud pública en poblaciones vulnerables.',
    TRUE, 'PENDIENTE'
),
(
    3, 'Mateo', 'Giraldo Moreno', '1061445566',
    'mateo.giraldo@udea.edu.co', '3224455667',
    'Derecho', '7',
    'Quiero profundizar en el litigio estratégico ambiental y la justicia climática.',
    TRUE, 'PENDIENTE'
)
ON CONFLICT DO NOTHING;
