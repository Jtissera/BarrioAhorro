
-- =============================================================================
-- V6: PERIODOS, RATIFICACIONES Y CONTROL DE ACCESO
-- =============================================================================


-- =============================================================================
-- 1. EVITAR SOLAPAMIENTOS EN COMPROMISOS DE CUPONERA
-- =============================================================================
-- Se aplica a todos los compromisos, incluidos los vencidos: los registros
-- representan periodos historicos y no deben superponerse.

ALTER TABLE compromisos_cuponera
    ADD CONSTRAINT ex_compromisos_cuponera_sin_solape
    EXCLUDE USING gist (
        comercio_id WITH =,
        (daterange(periodo_inicio, periodo_fin, '[]')) WITH &&
    );


-- =============================================================================
-- 2. EXIGIR QUE LOS PERIODOS DE CUPONERA AVANCEN EN ORDEN
-- =============================================================================
-- El advisory lock evita que dos transacciones simultaneas para el mismo
-- comercio superen la validacion consultando el mismo MAX(periodo_inicio).

CREATE OR REPLACE FUNCTION fn_validar_orden_periodo_cuponera()
RETURNS TRIGGER AS $$
DECLARE
    v_ultimo_inicio DATE;
BEGIN
    PERFORM pg_advisory_xact_lock(
        hashtextextended(NEW.comercio_id::TEXT, 0)
    );

    SELECT MAX(periodo_inicio)
    INTO v_ultimo_inicio
    FROM compromisos_cuponera
    WHERE comercio_id = NEW.comercio_id;

    IF v_ultimo_inicio IS NOT NULL
       AND NEW.periodo_inicio <= v_ultimo_inicio
    THEN
        RAISE EXCEPTION
            'El nuevo periodo de cuponera debe comenzar despues del ultimo periodo registrado (%)',
            v_ultimo_inicio;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


CREATE TRIGGER trg_validar_orden_periodo_cuponera
    BEFORE INSERT ON compromisos_cuponera
    FOR EACH ROW
    EXECUTE FUNCTION fn_validar_orden_periodo_cuponera();


-- =============================================================================
-- 3. EVITAR SOLAPAMIENTOS EN CICLOS DE SORTEO
-- =============================================================================

ALTER TABLE compromisos_sorteo
    ADD CONSTRAINT ex_compromisos_sorteo_sin_solape
    EXCLUDE USING gist (
        comercio_id WITH =,
        (daterange(ciclo_inicio, ciclo_fin, '[]')) WITH &&
    );


-- =============================================================================
-- 4. EXIGIR QUE LOS CICLOS DE SORTEO AVANCEN EN ORDEN
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_validar_orden_ciclo_sorteo()
RETURNS TRIGGER AS $$
DECLARE
    v_ultimo_inicio DATE;
BEGIN
    PERFORM pg_advisory_xact_lock(
        hashtextextended(NEW.comercio_id::TEXT, 0)
    );

    SELECT MAX(ciclo_inicio)
    INTO v_ultimo_inicio
    FROM compromisos_sorteo
    WHERE comercio_id = NEW.comercio_id;

    IF v_ultimo_inicio IS NOT NULL
       AND NEW.ciclo_inicio <= v_ultimo_inicio
    THEN
        RAISE EXCEPTION
            'El nuevo ciclo de sorteo debe comenzar despues del ultimo ciclo registrado (%)',
            v_ultimo_inicio;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


CREATE TRIGGER trg_validar_orden_ciclo_sorteo
    BEFORE INSERT ON compromisos_sorteo
    FOR EACH ROW
    EXECUTE FUNCTION fn_validar_orden_ciclo_sorteo();


-- =============================================================================
-- 5. INICIALIZAR LOS COMPROMISOS DE SORTEO COMO PENDIENTES
-- =============================================================================
-- El estado inicial y la fecha de ratificacion no los decide el cliente.

CREATE OR REPLACE FUNCTION fn_compromiso_sorteo_nace_pendiente()
RETURNS TRIGGER AS $$
BEGIN
    NEW.estado := 'PENDIENTE';
    NEW.fecha_ratificacion := NULL;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


CREATE TRIGGER trg_compromiso_sorteo_nace_pendiente
    BEFORE INSERT ON compromisos_sorteo
    FOR EACH ROW
    EXECUTE FUNCTION fn_compromiso_sorteo_nace_pendiente();


-- =============================================================================
-- 6. PROTEGER LOS DATOS DE LOS COMPROMISOS DE SORTEO
-- =============================================================================
-- El comercio, el ciclo y el plazo no se modifican despues de la creacion.
-- La descripcion del premio puede editarse mientras el compromiso este
-- pendiente, pero no despues de ratificarse o vencer.

CREATE OR REPLACE FUNCTION fn_proteger_compromiso_sorteo()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.comercio_id IS DISTINCT FROM OLD.comercio_id
       OR NEW.ciclo_inicio IS DISTINCT FROM OLD.ciclo_inicio
       OR NEW.ciclo_fin IS DISTINCT FROM OLD.ciclo_fin
       OR NEW.fecha_limite_ratificacion
            IS DISTINCT FROM OLD.fecha_limite_ratificacion
    THEN
        RAISE EXCEPTION
            'No se pueden cambiar el comercio, el ciclo ni el plazo de ratificacion';
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


CREATE TRIGGER trg_proteger_compromiso_sorteo
    BEFORE UPDATE ON compromisos_sorteo
    FOR EACH ROW
    EXECUTE FUNCTION fn_proteger_compromiso_sorteo();


-- =============================================================================
-- 7. VALIDAR LA RATIFICACION DEL COMPROMISO DE SORTEO
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_validar_ratificacion_sorteo()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.estado <> 'PENDIENTE'
       AND NEW.estado IS DISTINCT FROM OLD.estado
    THEN
        RAISE EXCEPTION
            'No se puede cambiar el estado final del compromiso de sorteo';
    END IF;

    IF NEW.estado = 'RATIFICADO'
       AND OLD.estado = 'PENDIENTE'
    THEN
        IF now() >= NEW.fecha_limite_ratificacion THEN
            RAISE EXCEPTION
                'El plazo para ratificar el sorteo ya vencio';
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

        IF length(btrim(NEW.premio_descripcion)) = 0 THEN
            RAISE EXCEPTION
                'La descripcion del premio no puede estar vacia';
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


CREATE TRIGGER trg_validar_ratificacion_sorteo
    BEFORE UPDATE OF estado ON compromisos_sorteo
    FOR EACH ROW
    EXECUTE FUNCTION fn_validar_ratificacion_sorteo();


-- =============================================================================
-- 8. VISTA DE ACCESO RESTRINGIDO DEL COMERCIO
-- =============================================================================
-- Se considera restringido al comercio que:
--   a) no tiene ningun compromiso de cuponera ratificado;
--   b) no tiene ningun compromiso de sorteo ratificado; o
--   c) dejo vencer un compromiso sin ratificar y aun no lo reemplazo por uno
--      posterior ratificado del mismo tipo.
--
-- La vista no modifica permisos ni bloquea operaciones por si sola. La usa
-- la aplicacion y los triggers que necesitan aplicar estas reglas.

CREATE OR REPLACE VIEW v_comercio_acceso_restringido AS
SELECT
    pc.usuario_id AS comercio_id,
    (
        NOT EXISTS (
            SELECT 1
            FROM compromisos_cuponera cc
            WHERE cc.comercio_id = pc.usuario_id
              AND cc.estado = 'RATIFICADO'
        )
        OR NOT EXISTS (
            SELECT 1
            FROM compromisos_sorteo cs
            WHERE cs.comercio_id = pc.usuario_id
              AND cs.estado = 'RATIFICADO'
        )
        OR EXISTS (
            SELECT 1
            FROM compromisos_cuponera cc
            WHERE cc.comercio_id = pc.usuario_id
              AND (
                  cc.estado = 'VENCIDO'
                  OR (
                      cc.estado = 'PENDIENTE'
                      AND cc.fecha_limite_ratificacion < now()
                  )
              )
              AND NOT EXISTS (
                  SELECT 1
                  FROM compromisos_cuponera posterior
                  WHERE posterior.comercio_id = cc.comercio_id
                    AND posterior.estado = 'RATIFICADO'
                    AND posterior.periodo_inicio > cc.periodo_inicio
              )
        )
        OR EXISTS (
            SELECT 1
            FROM compromisos_sorteo cs
            WHERE cs.comercio_id = pc.usuario_id
              AND (
                  cs.estado = 'VENCIDO'
                  OR (
                      cs.estado = 'PENDIENTE'
                      AND cs.fecha_limite_ratificacion < now()
                  )
              )
              AND NOT EXISTS (
                  SELECT 1
                  FROM compromisos_sorteo posterior
                  WHERE posterior.comercio_id = cs.comercio_id
                    AND posterior.estado = 'RATIFICADO'
                    AND posterior.ciclo_inicio > cs.ciclo_inicio
              )
        )
    ) AS acceso_restringido
FROM perfiles_comercio pc;


COMMENT ON VIEW v_comercio_acceso_restringido IS
    'Indica si un comercio carece de compromisos ratificados requeridos o mantiene vencimientos sin reemplazo ratificado. La aplicacion debe aplicar el modo de solo lectura.';


-- =============================================================================
-- 9. FUNCION PARA VENCER COMPROMISOS CUYO PLAZO EXPIRO
-- =============================================================================
-- Esta funcion debe ser invocada periodicamente por el backend o por un
-- scheduler externo. Crear la funcion no programa su ejecucion automatica.

CREATE OR REPLACE FUNCTION fn_vencer_compromisos()
RETURNS INTEGER AS $$
DECLARE
    v_cuponera INTEGER := 0;
    v_sorteos  INTEGER := 0;
BEGIN
    UPDATE compromisos_cuponera
    SET estado = 'VENCIDO'
    WHERE estado = 'PENDIENTE'
      AND fecha_limite_ratificacion <= now();

    GET DIAGNOSTICS v_cuponera = ROW_COUNT;

    UPDATE compromisos_sorteo
    SET estado = 'VENCIDO'
    WHERE estado = 'PENDIENTE'
      AND fecha_limite_ratificacion <= now();

    GET DIAGNOSTICS v_sorteos = ROW_COUNT;

    RETURN v_cuponera + v_sorteos;
END;
$$ LANGUAGE plpgsql;


COMMENT ON FUNCTION fn_vencer_compromisos() IS
    'Marca como vencidos los compromisos pendientes cuyo plazo expiro. Debe ejecutarse periodicamente desde un scheduler.';


-- =============================================================================
-- 10. BLOQUEAR LA CREACION O REACTIVACION DE OFERTAS SIN ACCESO
-- =============================================================================
-- Se permite editar o desactivar ofertas existentes.
-- No se permite crear ofertas nuevas ni reactivar una oferta desactivada
-- mientras el comercio tenga el acceso restringido.

CREATE OR REPLACE FUNCTION fn_bloquear_oferta_sin_acceso()
RETURNS TRIGGER AS $$
DECLARE
    v_onboarding BOOLEAN;
    v_restringido BOOLEAN;
BEGIN
    IF TG_OP = 'UPDATE' THEN
        -- No bloqueamos ediciones ni desactivaciones de ofertas existentes.
        IF OLD.activa OR NOT NEW.activa THEN
            RETURN NEW;
        END IF;
    END IF;

    SELECT onboarding_completado
    INTO v_onboarding
    FROM perfiles_comercio
    WHERE usuario_id = NEW.comercio_id;

    IF NOT FOUND OR NOT v_onboarding THEN
        RAISE EXCEPTION
            'El comercio debe completar el onboarding antes de publicar ofertas';
    END IF;

    SELECT acceso_restringido
    INTO v_restringido
    FROM v_comercio_acceso_restringido
    WHERE comercio_id = NEW.comercio_id;

    IF COALESCE(v_restringido, TRUE) THEN
        RAISE EXCEPTION
            'El comercio tiene el acceso restringido y no puede publicar ni reactivar ofertas';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


CREATE TRIGGER trg_bloquear_oferta_sin_acceso
    BEFORE INSERT OR UPDATE OF activa ON ofertas
    FOR EACH ROW
    EXECUTE FUNCTION fn_bloquear_oferta_sin_acceso();


COMMENT ON FUNCTION fn_bloquear_oferta_sin_acceso() IS
    'Bloquea nuevas ofertas y reactivaciones cuando el comercio no completo el onboarding o tiene acceso restringido.';
