-- ============================================================
-- V8__rol_admin.sql
-- Roles de usuario del sistema: ADMIN y COORDINADOR.
-- El rol viaja en el JWT (claim "rol") y Spring Security lo expone
-- como ROLE_ADMIN / ROLE_COORDINADOR.
-- ============================================================

-- Normalizar valores existentes antes de restringir el dominio
UPDATE coordinador SET rol = UPPER(TRIM(rol));
UPDATE coordinador SET rol = 'COORDINADOR' WHERE rol NOT IN ('ADMIN', 'COORDINADOR');

ALTER TABLE coordinador
    ADD CONSTRAINT chk_coordinador_rol CHECK (rol IN ('ADMIN', 'COORDINADOR'));

-- ----------------------
-- Administradora del sistema: Yiyi López
-- Si ya existe como usuario se promueve a ADMIN conservando su contraseña.
-- Si no existe se crea con la contraseña de ejemplo de V3 ("UdeA2024*"),
-- que debe cambiarse tras el primer ingreso.
-- ----------------------
INSERT INTO coordinador (nombres, apellidos, correo, password_hash, telefono, rol, activo) VALUES
('Yiyi', 'López', 'yiyi.lopez@udea.edu.co',
 '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBpj0V.GmpTWie', NULL, 'ADMIN', TRUE)
ON CONFLICT (correo) DO UPDATE SET rol = 'ADMIN', activo = TRUE;
