-- ============================================================
-- V13__datos_ejemplo_sesiones_y_asistencia.sql
-- Datos de ejemplo para los reportes "Actividades por Tipo" y
-- "Asistencia a actividades" (tablas sesion_semillero y asistencia_sesion).
--
-- Para cada semillero ACTIVO:
--   1. 5 integrantes de ejemplo (cedulas que empiezan por 9).
--   2. 8 sesiones repartidas en los ultimos ~8 meses, rotando los tipos
--      del catalogo actividad_cientifica. Los titulos empiezan por "Ejemplo".
--   3. Asistencia de cada integrante activo a cada sesion de ejemplo
--      (aprox. 70% presente, 20% ausente, 10% excusado).
--
-- Es idempotente: no duplica filas si se vuelve a ejecutar.
-- Para retirar los datos de ejemplo: borrar las sesiones con titulo
-- LIKE 'Ejemplo %' (la asistencia se elimina en cascada) y los
-- integrantes con cedula LIKE '9%' creados por esta migracion.
-- ============================================================

-- 1. Integrantes de ejemplo
INSERT INTO semillero_integrante
    (id_semillero, nombres, apellidos, cedula, correo, tipo_vinculacion, activo, fecha_ingreso, sexo)
SELECT t.id_semillero, t.nombres, t.apellidos, t.cedula,
       'integrante' || t.n || '.sem' || t.id_semillero || '@example.com',
       t.rol, TRUE, CURRENT_DATE - 300, t.sexo
FROM (
    SELECT s.id_semillero, d.n, d.nombres, d.apellidos, d.rol, d.sexo,
           '9' || LPAD(s.id_semillero::text, 4, '0') || LPAD(d.n::text, 2, '0') AS cedula
    FROM semillero s
    CROSS JOIN (VALUES
        (1, 'Camila',   'Restrepo Gomez', 'ESTUDIANTE_INVESTIGADOR', 'FEMENINO'),
        (2, 'Julian',   'Cardona Ruiz',   'ESTUDIANTE_INVESTIGADOR', 'MASCULINO'),
        (3, 'Mariana',  'Zapata Henao',   'AUXILIAR',                'FEMENINO'),
        (4, 'Santiago', 'Ospina Mejia',   'SEMILLERISTA_JUNIOR',     'MASCULINO'),
        (5, 'Valeria',  'Londono Arias',  'ESTUDIANTE_INVESTIGADOR', 'FEMENINO')
    ) AS d(n, nombres, apellidos, rol, sexo)
    WHERE s.estado = 'ACTIVO'
) t
WHERE NOT EXISTS (
    SELECT 1 FROM semillero_integrante x
    WHERE x.id_semillero = t.id_semillero AND x.cedula = t.cedula
);

-- 2. Sesiones de ejemplo (un tipo del catalogo por sesion, rotando por semillero)
INSERT INTO sesion_semillero (id_semillero, id_actividad, titulo, fecha)
SELECT t.id_semillero, t.id_actividad, t.titulo, t.fecha
FROM (
    SELECT sr.id_semillero,
           ac.id_actividad,
           'Ejemplo ' || n.n || ': ' || ac.nombre AS titulo,
           CURRENT_DATE - (n.n * 28 + (sr.rk % 5)::int * 3) AS fecha
    FROM (
        SELECT id_semillero, ROW_NUMBER() OVER (ORDER BY id_semillero) AS rk
        FROM semillero
        WHERE estado = 'ACTIVO'
    ) sr
    CROSS JOIN generate_series(1, 8) AS n(n)
    JOIN (
        SELECT id_actividad, nombre,
               ROW_NUMBER() OVER (ORDER BY id_actividad) AS rn,
               COUNT(*) OVER () AS total
        FROM actividad_cientifica
    ) ac ON ac.rn = ((n.n + sr.rk) % ac.total) + 1
) t
WHERE NOT EXISTS (
    SELECT 1 FROM sesion_semillero x
    WHERE x.id_semillero = t.id_semillero AND x.titulo = t.titulo
);

-- 3. Asistencia a las sesiones de ejemplo
INSERT INTO asistencia_sesion (id_sesion, id_integrante, estado)
SELECT ss.id_sesion, si.id,
       CASE WHEN (ss.id_sesion + si.id) % 10 < 7 THEN 'PRESENTE'
            WHEN (ss.id_sesion + si.id) % 10 < 9 THEN 'AUSENTE'
            ELSE 'EXCUSADO' END
FROM sesion_semillero ss
JOIN semillero_integrante si ON si.id_semillero = ss.id_semillero
WHERE ss.titulo LIKE 'Ejemplo %'
  AND si.activo = TRUE
  AND (si.fecha_ingreso IS NULL OR si.fecha_ingreso <= ss.fecha)
ON CONFLICT (id_sesion, id_integrante) DO NOTHING;
