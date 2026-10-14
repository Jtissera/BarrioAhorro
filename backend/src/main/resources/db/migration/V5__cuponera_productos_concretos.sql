
-- =============================================================================
-- V5: CUPONERA CON PREMIOS CONCRETOS
-- =============================================================================


-- =============================================================================
-- 1. REDISENAR LOS CUPONES COMO PREMIOS
-- =============================================================================

ALTER TABLE cupones_cuponera
    ADD COLUMN nombre_producto TEXT,
    ADD COLUMN descripcion TEXT,
    ADD COLUMN foto_url TEXT;

-- Migracion de los datos anteriores, si los hubiera.
UPDATE cupones_cuponera
SET nombre_producto = detalle_beneficio;

ALTER TABLE cupones_cuponera
    ALTER COLUMN nombre_producto SET NOT NULL,
    ADD CONSTRAINT chk_cupones_nombre_no_vacio
        CHECK (length(btrim(nombre_producto)) > 0),
    ADD CONSTRAINT chk_cupones_descripcion_no_vacia
        CHECK (descripcion IS NULL OR length(btrim(descripcion)) > 0),
    DROP COLUMN tipo_beneficio,
    DROP COLUMN detalle_beneficio;

-- No eliminamos tipo_beneficio_enum: tambien lo utiliza la tabla ofertas.

-- =============================================================================
-- 2. VALIDAR LA CANTIDAD DE PREMIOS POR COMPROMISO
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_validar_rango_cupones_compromiso()
RETURNS TRIGGER AS $$
DECLARE
    v_compromiso_id BIGINT;
    v_cantidad      INTEGER;
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_compromiso_id := OLD.compromiso_id;
    ELSE
        v_compromiso_id := NEW.compromiso_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM compromisos_cuponera
        WHERE id = v_compromiso_id
    ) THEN
        RETURN NULL;
    END IF;

    SELECT COUNT(*)
    INTO v_cantidad
    FROM cupones_cuponera
    WHERE compromiso_id = v_compromiso_id;

    IF v_cantidad NOT BETWEEN 2 AND 4 THEN
        RAISE EXCEPTION
            'El compromiso % debe tener entre 2 y 4 premios; tiene %',
            v_compromiso_id, v_cantidad;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- 3. PROTEGER LOS PREMIOS DE COMPROMISOS RATIFICADOS
-- =============================================================================
-- Un compromiso pendiente permite preparar y editar sus premios.
-- Cuando deja de estar pendiente, los datos comerciales del premio no cambian.
-- stock_canjeado se actualiza durante la validacion de un canje.

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


CREATE TRIGGER trg_proteger_cupon_ratificado
    BEFORE INSERT OR UPDATE OR DELETE ON cupones_cuponera
    FOR EACH ROW
    EXECUTE FUNCTION fn_proteger_cupon_ratificado();


-- =============================================================================
-- 4. PROTEGER LOS DATOS DEL COMPROMISO RATIFICADO
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_proteger_compromiso_cuponera()
RETURNS TRIGGER AS $$
BEGIN
    -- El comercio y el periodo identifican el compromiso y son inmutables.
    IF NEW.comercio_id IS DISTINCT FROM OLD.comercio_id
       OR NEW.periodo_inicio IS DISTINCT FROM OLD.periodo_inicio
       OR NEW.periodo_fin IS DISTINCT FROM OLD.periodo_fin
       OR NEW.fecha_limite_ratificacion
            IS DISTINCT FROM OLD.fecha_limite_ratificacion
    THEN
        RAISE EXCEPTION
            'No se pueden cambiar el comercio, el periodo ni el plazo de ratificacion';
    END IF;

    -- Una vez que deja de estar pendiente, no se puede modificar el compromiso.
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


CREATE TRIGGER trg_proteger_compromiso_cuponera
    BEFORE UPDATE ON compromisos_cuponera
    FOR EACH ROW
    EXECUTE FUNCTION fn_proteger_compromiso_cuponera();


-- =============================================================================
-- 5. VALIDAR LA RATIFICACION DEL COMPROMISO
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_validar_ratificacion_cuponera()
RETURNS TRIGGER AS $$
DECLARE
    v_cantidad INTEGER;
BEGIN
    IF OLD.estado <> 'PENDIENTE'
       AND NEW.estado IS DISTINCT FROM OLD.estado
    THEN
        RAISE EXCEPTION
            'No se puede cambiar el estado final del compromiso de cuponera';
    END IF;

    IF NEW.estado = 'RATIFICADO'
       AND OLD.estado = 'PENDIENTE'
    THEN
        IF now() >= NEW.fecha_limite_ratificacion THEN
            RAISE EXCEPTION
                'El plazo para ratificar el compromiso ya vencio';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM perfiles_comercio
            WHERE usuario_id = NEW.comercio_id
              AND onboarding_completado
        ) THEN
            RAISE EXCEPTION
                'El comercio debe completar el onboarding antes de ratificar';
        END IF;

        SELECT COUNT(*)
        INTO v_cantidad
        FROM cupones_cuponera
        WHERE compromiso_id = NEW.id;

        IF v_cantidad NOT BETWEEN 2 AND 4 THEN
            RAISE EXCEPTION
                'El compromiso debe tener entre 2 y 4 premios para ratificarse';
        END IF;

        NEW.fecha_ratificacion := now();

    ELSIF NEW.estado = 'VENCIDO'
          AND OLD.estado = 'PENDIENTE'
    THEN
        IF now() < NEW.fecha_limite_ratificacion THEN
            RAISE EXCEPTION
                'No se puede vencer un compromiso antes de su plazo limite';
        END IF;

        NEW.fecha_ratificacion := NULL;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


CREATE TRIGGER trg_validar_ratificacion_cuponera
    BEFORE UPDATE OF estado ON compromisos_cuponera
    FOR EACH ROW
    EXECUTE FUNCTION fn_validar_ratificacion_cuponera();


-- =============================================================================
-- 6. VALIDAR Y RESERVAR STOCK AL CREAR UN CANJE
-- =============================================================================
-- Se consideran reservadas las unidades de canjes pendientes de validacion
-- y las unidades ya validadas. El bloqueo de la fila del premio serializa
-- las reservas concurrentes de ese mismo premio.
--
-- Los puntos se descuentan al validar el canje, como en el modelo original.

CREATE OR REPLACE FUNCTION fn_validar_nuevo_canje()
RETURNS TRIGGER AS $$
DECLARE
    v_estado_compromiso estado_compromiso_enum;
    v_costo             INTEGER;
    v_stock             INTEGER;
    v_reservados        BIGINT;
BEGIN
    SELECT cc.estado, c.costo_puntos, c.stock_comprometido
    INTO v_estado_compromiso, v_costo, v_stock
    FROM cupones_cuponera c
    JOIN compromisos_cuponera cc ON cc.id = c.compromiso_id
    WHERE c.id = NEW.cupon_id
    FOR UPDATE OF c;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El premio indicado no existe';
    END IF;

    IF v_estado_compromiso <> 'RATIFICADO' THEN
        RAISE EXCEPTION
            'Solo se pueden canjear premios de compromisos ratificados';
    END IF;

    SELECT COUNT(*)
    INTO v_reservados
    FROM canjes_cupon
    WHERE cupon_id = NEW.cupon_id
      AND estado IN ('PENDIENTE_VALIDACION', 'VALIDADO');

    IF v_reservados >= v_stock THEN
        RAISE EXCEPTION 'El premio no tiene stock disponible';
    END IF;

    -- El precio del canje lo determina la base de datos.
    NEW.puntos_costo := v_costo;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


CREATE TRIGGER trg_validar_nuevo_canje
    BEFORE INSERT ON canjes_cupon
    FOR EACH ROW
    EXECUTE FUNCTION fn_validar_nuevo_canje();


-- =============================================================================
-- 7. PROCESAR LA TRANSICION DE ESTADO DE UN CANJE
-- =============================================================================
-- Se reemplaza la funcion existente para reforzar las transiciones permitidas.
-- Un canje pendiente puede validarse o cancelarse.
-- El descuento de puntos y el aumento de stock se confirman en la misma
-- transaccion que el cambio de estado.

CREATE OR REPLACE FUNCTION fn_procesar_transicion_canje()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.estado IS NOT DISTINCT FROM OLD.estado THEN
        RETURN NEW;
    END IF;

    IF OLD.estado <> 'PENDIENTE_VALIDACION' THEN
        RAISE EXCEPTION
            'Solo un canje pendiente puede cambiar a validado o cancelado';
    END IF;

    IF NEW.estado = 'VALIDADO' THEN
        -- El premio debe seguir perteneciendo a un compromiso ratificado.
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

    ELSE
        RAISE EXCEPTION
            'Transicion de canje no permitida: % -> %',
            OLD.estado, NEW.estado;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


COMMENT ON TABLE cupones_cuponera IS
    'Premios concretos ofrecidos en un compromiso de cuponera, con precio en puntos y stock comprometido.';

COMMENT ON COLUMN cupones_cuponera.stock_canjeado IS
    'Cantidad de canjes validados. Las reservas pendientes se contabilizan a partir de canjes_cupon.';

COMMENT ON COLUMN canjes_cupon.puntos_costo IS
    'Snapshot del costo en puntos fijado por la base de datos al crear el canje.';
