-- =============================================================================
-- V8: RENOVACION DE PERIODOS Y VENCIMIENTO DE CANJES
-- =============================================================================
-- 1. Un compromiso VENCIDO puede recortar su fin (nunca extenderlo), para que
--    el comercio pueda ratificar un periodo nuevo que empiece hoy sin chocar
--    con la restriccion de no solapamiento.
-- 2. Un canje pendiente puede pasar a VENCIDO.


CREATE OR REPLACE FUNCTION fn_proteger_compromiso_cuponera()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.comercio_id IS DISTINCT FROM OLD.comercio_id
       OR NEW.periodo_inicio IS DISTINCT FROM OLD.periodo_inicio
       OR NEW.fecha_limite_ratificacion IS DISTINCT FROM OLD.fecha_limite_ratificacion
    THEN
        RAISE EXCEPTION
            'No se pueden cambiar el comercio, el inicio del periodo ni el plazo de ratificacion';
    END IF;

    IF NEW.periodo_fin IS DISTINCT FROM OLD.periodo_fin
       AND NOT (OLD.estado = 'VENCIDO' AND NEW.periodo_fin < OLD.periodo_fin)
    THEN
        RAISE EXCEPTION
            'Solo se puede recortar el fin de un periodo vencido';
    END IF;

    IF OLD.estado <> 'PENDIENTE'
       AND NEW.estado IS NOT DISTINCT FROM OLD.estado
    THEN
        IF NEW.fecha_ratificacion IS DISTINCT FROM OLD.fecha_ratificacion THEN
            RAISE EXCEPTION
                'No se puede modificar manualmente la fecha de ratificacion';
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION fn_proteger_compromiso_sorteo()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.comercio_id IS DISTINCT FROM OLD.comercio_id
       OR NEW.ciclo_inicio IS DISTINCT FROM OLD.ciclo_inicio
       OR NEW.fecha_limite_ratificacion IS DISTINCT FROM OLD.fecha_limite_ratificacion
    THEN
        RAISE EXCEPTION
            'No se pueden cambiar el comercio, el inicio del ciclo ni el plazo de ratificacion';
    END IF;

    IF NEW.ciclo_fin IS DISTINCT FROM OLD.ciclo_fin
       AND NOT (OLD.estado = 'VENCIDO' AND NEW.ciclo_fin < OLD.ciclo_fin)
    THEN
        RAISE EXCEPTION
            'Solo se puede recortar el fin de un ciclo vencido';
    END IF;

    IF OLD.estado <> 'PENDIENTE'
       AND (
           NEW.premio_descripcion IS DISTINCT FROM OLD.premio_descripcion
           OR NEW.fecha_ratificacion IS DISTINCT FROM OLD.fecha_ratificacion
       )
    THEN
        RAISE EXCEPTION
            'No se pueden modificar los datos de un compromiso de sorteo finalizado';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION fn_procesar_transicion_canje()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.estado IS NOT DISTINCT FROM OLD.estado THEN
        RETURN NEW;
    END IF;

    IF OLD.estado <> 'PENDIENTE_VALIDACION' THEN
        RAISE EXCEPTION
            'Solo un canje pendiente puede cambiar a validado, cancelado o vencido';
    END IF;

    IF NEW.estado = 'VALIDADO' THEN
        IF NOT EXISTS (
            SELECT 1
            FROM canjes_cupon c
            JOIN cupones_cuponera p ON p.id = c.cupon_id
            JOIN compromisos_cuponera cc ON cc.id = p.compromiso_id
            WHERE c.id = OLD.id
              AND cc.estado = 'RATIFICADO'
        ) THEN
            RAISE EXCEPTION
                'No se puede validar el canje porque su compromiso ya no esta ratificado';
        END IF;

        UPDATE cupones_cuponera
        SET stock_canjeado = stock_canjeado + 1
        WHERE id = OLD.cupon_id
          AND stock_canjeado < stock_comprometido;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'No queda stock para validar este canje';
        END IF;

        NEW.fecha_validacion := now();

        INSERT INTO movimientos_puntos (
            cliente_id, tipo, puntos, referencia_tabla, referencia_id, descripcion
        )
        VALUES (
            NEW.cliente_id,
            'CANJE_CUPON',
            -NEW.puntos_costo,
            'canjes_cupon',
            NEW.id,
            'Canje de premio validado por el comercio'
        );

    ELSIF NEW.estado = 'CANCELADO' THEN
        NEW.fecha_cancelacion := now();

    ELSIF NEW.estado = 'VENCIDO' THEN
        NULL; -- libera la reserva de stock: no tiene otros efectos

    ELSE
        RAISE EXCEPTION
            'Transicion de canje no permitida: % -> %',
            OLD.estado, NEW.estado;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;