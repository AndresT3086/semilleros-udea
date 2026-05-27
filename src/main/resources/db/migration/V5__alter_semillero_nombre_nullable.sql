-- El nombre del semillero se hace obligatorio solo al completar la pestaña General,
-- no al crear el borrador inicial
ALTER TABLE semillero ALTER COLUMN nombre DROP NOT NULL;