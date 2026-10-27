-- Rol admin (el valor nuevo no se puede usar dentro de esta misma migración).
ALTER TYPE tipo_usuario_enum ADD VALUE IF NOT EXISTS 'ADMIN';

CREATE TYPE estado_revision_enum AS ENUM ('PENDIENTE', 'APROBADO', 'RECHAZADO');

-- Lo que ya existe queda aprobado; lo nuevo nace pendiente de revisión.
ALTER TABLE cupones_cuponera
    ADD COLUMN estado_revision estado_revision_enum NOT NULL DEFAULT 'APROBADO',
    ADD COLUMN motivo_rechazo  TEXT,
    ADD COLUMN revisado_en     TIMESTAMPTZ,
    ADD CONSTRAINT chk_cupones_motivo_rechazo CHECK (
        estado_revision <> 'RECHAZADO' OR length(btrim(motivo_rechazo)) >= 15);
ALTER TABLE cupones_cuponera ALTER COLUMN estado_revision SET DEFAULT 'PENDIENTE';

ALTER TABLE compromisos_sorteo
    ADD COLUMN estado_revision estado_revision_enum NOT NULL DEFAULT 'APROBADO',
    ADD COLUMN motivo_rechazo  TEXT,
    ADD COLUMN revisado_en     TIMESTAMPTZ,
    ADD CONSTRAINT chk_sorteo_motivo_rechazo CHECK (
        estado_revision <> 'RECHAZADO' OR length(btrim(motivo_rechazo)) >= 15);
ALTER TABLE compromisos_sorteo ALTER COLUMN estado_revision SET DEFAULT 'PENDIENTE';

-- V6 prohíbe cambiar el estado de un sorteo ya ratificado.
-- Se agrega UNA excepción: RATIFICADO -> VENCIDO (rechazo del admin).
CREATE OR REPLACE FUNCTION fn_validar_ratificacion_sorteo()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.estado = 'RATIFICADO' AND NEW.estado = 'VENCIDO' THEN
        RETURN NEW;
    END IF;

    IF OLD.estado <> 'PENDIENTE' AND NEW.estado IS DISTINCT FROM OLD.estado THEN
        RAISE EXCEPTION 'No se puede cambiar el estado final del compromiso de sorteo';
    END IF;

    IF NEW.estado = 'RATIFICADO' AND OLD.estado = 'PENDIENTE' THEN
        IF now() >= NEW.fecha_limite_ratificacion THEN
            RAISE EXCEPTION 'El plazo para ratificar el sorteo ya vencio';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM perfiles_comercio
                       WHERE usuario_id = NEW.comercio_id AND onboarding_completado) THEN
            RAISE EXCEPTION 'El comercio debe completar el onboarding antes de ratificar';
        END IF;
        IF length(btrim(NEW.premio_descripcion)) = 0 THEN
            RAISE EXCEPTION 'La descripcion del premio no puede estar vacia';
        END IF;
        NEW.fecha_ratificacion := now();

    ELSIF NEW.estado = 'VENCIDO' AND OLD.estado = 'PENDIENTE' THEN
        IF now() < NEW.fecha_limite_ratificacion THEN
            RAISE EXCEPTION 'No se puede vencer un compromiso antes de su plazo limite';
        END IF;
        NEW.fecha_ratificacion := NULL;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;