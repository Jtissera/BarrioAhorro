
-- =============================================================================
-- V4: UNIFICAR HORARIOS DE ATENCION
-- =============================================================================

-- Necesaria para usar igualdad sobre bigint/integer en exclusion constraints.
CREATE EXTENSION IF NOT EXISTS btree_gist;


-- =============================================================================
-- 1. ELIMINAR LA TABLA LEGADA
-- =============================================================================
-- horarios_atencion es la tabla utilizada por el backend.
-- No usamos CASCADE: si hay dependencias inesperadas, queremos detectarlas.

DROP TABLE horarios_atencion_comercio;


-- =============================================================================
-- 2. VALIDACIONES DE HORARIOS
-- =============================================================================

ALTER TABLE horarios_atencion
    ADD CONSTRAINT chk_horarios_atencion_dia
        CHECK (dia_semana BETWEEN 1 AND 7),
    ADD CONSTRAINT chk_horarios_atencion_rango
        CHECK (hora_fin > hora_inicio);


-- =============================================================================
-- 3. EVITAR SOLAPAMIENTOS DE HORARIOS ACTIVOS
-- =============================================================================
-- Convención del backend:
--   1 = lunes ... 7 = domingo.
--
-- Los rangos son semiabiertos [inicio, fin):
--   08:00-12:00 y 12:00-16:00 son válidos.
--
-- date + time produce timestamp sin zona horaria, apropiado para
-- representar horas locales del comercio en un día de referencia.

ALTER TABLE horarios_atencion
    ADD CONSTRAINT ex_horarios_atencion_sin_solape
    EXCLUDE USING gist (
        comercio_id WITH =,
        dia_semana WITH =,
        (
            tsrange(
                DATE '2000-01-01' + hora_inicio,
                DATE '2000-01-01' + hora_fin,
                '[)'
            )
        ) WITH &&
    )
    WHERE (activo);


-- =============================================================================
-- 4. ACTUALIZAR updated_at
-- =============================================================================

CREATE TRIGGER trg_horarios_atencion_updated_at
    BEFORE UPDATE ON horarios_atencion
    FOR EACH ROW
    EXECUTE FUNCTION fn_actualizar_updated_at();


COMMENT ON TABLE horarios_atencion IS
    'Horarios de atencion del comercio. dia_semana: 1=lunes ... 7=domingo.';

COMMENT ON CONSTRAINT ex_horarios_atencion_sin_solape
    ON horarios_atencion IS
    'Impide que dos franjas activas del mismo comercio se solapen en un mismo dia.';
