-- =============================================================================
-- V7: VALOR DEL PRODUCTO EN EL COMERCIO PARA LOS PREMIOS DE CUPONERA
-- =============================================================================
-- El comercio informa cuanto vale el producto en su local (en pesos).
-- costo_puntos sigue siendo el precio en puntos que pagan los vecinos; lo
-- calcula el backend a partir de este valor.

ALTER TABLE cupones_cuponera
    ADD COLUMN valor_comercio NUMERIC(12,2),
    ADD CONSTRAINT chk_cupones_valor_comercio_positivo
        CHECK (valor_comercio IS NULL OR valor_comercio > 0);

COMMENT ON COLUMN cupones_cuponera.valor_comercio IS
    'Precio de venta del producto en el comercio (pesos) al momento del aporte.';


-- =============================================================================
-- PROTEGER EL VALOR UNA VEZ RATIFICADO
-- =============================================================================
-- Igual que la funcion de V5, pero valor_comercio tambien queda inmutable
-- cuando el compromiso deja de estar pendiente.

CREATE OR REPLACE FUNCTION fn_proteger_cupon_ratificado()
RETURNS TRIGGER AS $$
DECLARE
    v_compromiso_id BIGINT;
    v_estado        estado_compromiso_enum;
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_compromiso_id := OLD.compromiso_id;
    ELSE
        v_compromiso_id := NEW.compromiso_id;
    END IF;

    -- Permitir eliminaciones que formen parte de un borrado en cascada.
    IF TG_OP = 'DELETE' AND pg_trigger_depth() > 1 THEN
        RETURN OLD;
    END IF;

    SELECT estado
    INTO v_estado
    FROM compromisos_cuponera
    WHERE id = v_compromiso_id;

    -- El compromiso puede estar desapareciendo por un borrado en cascada.
    IF NOT FOUND THEN
        IF TG_OP = 'DELETE' THEN
            RETURN OLD;
        END IF;
        RETURN NEW;
    END IF;

    IF v_estado <> 'PENDIENTE' THEN
        IF TG_OP = 'INSERT' THEN
            RAISE EXCEPTION
                'No se pueden agregar premios a un compromiso no pendiente';
        ELSIF TG_OP = 'DELETE' THEN
            RAISE EXCEPTION
                'No se pueden eliminar premios de un compromiso no pendiente';
        ELSE
            IF NEW.compromiso_id IS DISTINCT FROM OLD.compromiso_id
               OR NEW.nombre_producto IS DISTINCT FROM OLD.nombre_producto
               OR NEW.descripcion IS DISTINCT FROM OLD.descripcion
               OR NEW.foto_url IS DISTINCT FROM OLD.foto_url
               OR NEW.costo_puntos IS DISTINCT FROM OLD.costo_puntos
               OR NEW.stock_comprometido IS DISTINCT FROM OLD.stock_comprometido
               OR NEW.valor_comercio IS DISTINCT FROM OLD.valor_comercio
            THEN
                RAISE EXCEPTION
                    'No se pueden modificar los datos comerciales de un premio ratificado o vencido';
            END IF;
        END IF;
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;