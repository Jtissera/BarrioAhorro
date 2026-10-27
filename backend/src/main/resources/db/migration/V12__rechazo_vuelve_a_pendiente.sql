-- Cuponera: se agrega la transición RATIFICADO -> PENDIENTE (rechazo del admin).
CREATE OR REPLACE FUNCTION fn_validar_ratificacion_cuponera()
RETURNS TRIGGER AS $$
DECLARE
    v_cantidad INTEGER;
BEGIN
    IF OLD.estado = 'RATIFICADO' AND NEW.estado = 'PENDIENTE' THEN
        NEW.fecha_ratificacion := NULL;
        RETURN NEW;
    END IF;

    IF OLD.estado <> 'PENDIENTE' AND NEW.estado IS DISTINCT FROM OLD.estado THEN
        RAISE EXCEPTION 'No se puede cambiar el estado final del compromiso de cuponera';
    END IF;

    IF NEW.estado = 'RATIFICADO' AND OLD.estado = 'PENDIENTE' THEN
        IF now() >= NEW.fecha_limite_ratificacion THEN
            RAISE EXCEPTION 'El plazo para ratificar el compromiso ya vencio';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM perfiles_comercio
                       WHERE usuario_id = NEW.comercio_id AND onboarding_completado) THEN
            RAISE EXCEPTION 'El comercio debe completar el onboarding antes de ratificar';
        END IF;
        SELECT COUNT(*) INTO v_cantidad FROM cupones_cuponera WHERE compromiso_id = NEW.id;
        IF v_cantidad NOT BETWEEN 2 AND 4 THEN
            RAISE EXCEPTION 'El compromiso debe tener entre 2 y 4 premios para ratificarse';
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

-- Sorteo: igual (reemplaza la excepcion RATIFICADO -> VENCIDO de V11).
CREATE OR REPLACE FUNCTION fn_validar_ratificacion_sorteo()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.estado = 'RATIFICADO' AND NEW.estado = 'PENDIENTE' THEN
        NEW.fecha_ratificacion := NULL;
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

-- Un rechazo sin resolver deja al comercio restringido (V6 + 2 clausulas nuevas).
CREATE OR REPLACE VIEW v_comercio_acceso_restringido AS
SELECT
    pc.usuario_id AS comercio_id,
    (
        NOT EXISTS (SELECT 1 FROM compromisos_cuponera cc
                    WHERE cc.comercio_id = pc.usuario_id AND cc.estado = 'RATIFICADO')
        OR NOT EXISTS (SELECT 1 FROM compromisos_sorteo cs
                       WHERE cs.comercio_id = pc.usuario_id AND cs.estado = 'RATIFICADO')
        OR EXISTS (
            SELECT 1 FROM compromisos_cuponera cc
            WHERE cc.comercio_id = pc.usuario_id
              AND (cc.estado = 'VENCIDO'
                   OR (cc.estado = 'PENDIENTE' AND cc.fecha_limite_ratificacion < now()))
              AND NOT EXISTS (SELECT 1 FROM compromisos_cuponera posterior
                              WHERE posterior.comercio_id = cc.comercio_id
                                AND posterior.estado = 'RATIFICADO'
                                AND posterior.periodo_inicio > cc.periodo_inicio)
        )
        OR EXISTS (
            SELECT 1 FROM compromisos_sorteo cs
            WHERE cs.comercio_id = pc.usuario_id
              AND (cs.estado = 'VENCIDO'
                   OR (cs.estado = 'PENDIENTE' AND cs.fecha_limite_ratificacion < now()))
              AND NOT EXISTS (SELECT 1 FROM compromisos_sorteo posterior
                              WHERE posterior.comercio_id = cs.comercio_id
                                AND posterior.estado = 'RATIFICADO'
                                AND posterior.ciclo_inicio > cs.ciclo_inicio)
        )
        -- NUEVO: premio de cuponera rechazado y todavia sin reemplazar
        OR EXISTS (
            SELECT 1 FROM cupones_cuponera cu
            JOIN compromisos_cuponera cc ON cc.id = cu.compromiso_id
            WHERE cc.comercio_id = pc.usuario_id
              AND cu.estado_revision = 'RECHAZADO'
              AND cc.estado <> 'RATIFICADO'
              AND NOT EXISTS (SELECT 1 FROM compromisos_cuponera posterior
                              WHERE posterior.comercio_id = cc.comercio_id
                                AND posterior.estado = 'RATIFICADO'
                                AND posterior.periodo_inicio > cc.periodo_inicio)
        )
        -- NUEVO: sorteo rechazado y todavia sin reemplazar
        OR EXISTS (
            SELECT 1 FROM compromisos_sorteo cs
            WHERE cs.comercio_id = pc.usuario_id
              AND cs.estado_revision = 'RECHAZADO'
              AND cs.estado <> 'RATIFICADO'
              AND NOT EXISTS (SELECT 1 FROM compromisos_sorteo posterior
                              WHERE posterior.comercio_id = cs.comercio_id
                                AND posterior.estado = 'RATIFICADO'
                                AND posterior.ciclo_inicio > cs.ciclo_inicio)
        )
    ) AS acceso_restringido
FROM perfiles_comercio pc;