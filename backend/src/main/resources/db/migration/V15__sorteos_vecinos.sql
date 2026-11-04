-- Los valores nuevos de un enum no se pueden usar dentro de esta misma migracion.
ALTER TYPE estado_sorteo_enum ADD VALUE IF NOT EXISTS 'DESIERTO';
ALTER TYPE estado_sorteo_enum ADD VALUE IF NOT EXISTS 'ANULADO';
ALTER TYPE tipo_movimiento_puntos_enum ADD VALUE IF NOT EXISTS 'REEMBOLSO_TICKET_SORTEO';

-- 1. Un premio ratificado = un sorteo
ALTER TABLE sorteos
    ADD COLUMN ejecutado_en  TIMESTAMPTZ,
    ADD COLUMN total_tickets INTEGER,
    ADD CONSTRAINT uq_sorteos_compromiso UNIQUE (compromiso_sorteo_id),
    ADD CONSTRAINT chk_sorteos_activo_con_fecha
        CHECK (estado::text <> 'ACTIVO' OR fecha_sorteo IS NOT NULL);

-- 2. Estado de cada ticket
ALTER TABLE tickets_sorteo
    ADD COLUMN estado TEXT NOT NULL DEFAULT 'VIGENTE'
        CHECK (estado IN ('VIGENTE', 'GANADOR', 'NO_GANADOR', 'ANULADO'));
CREATE INDEX idx_tickets_sorteo_sorteo_estado ON tickets_sorteo (sorteo_id, estado);

-- 3. Animacion del candado: se muestra una sola vez
ALTER TABLE perfiles_cliente ADD COLUMN sorteos_desbloqueo_visto_en TIMESTAMPTZ;

-- 4. Los reembolsos devuelven saldo pero no cuentan como puntos ganados
CREATE OR REPLACE FUNCTION fn_actualizar_saldo_puntos()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE perfiles_cliente
    SET saldo_puntos = saldo_puntos + NEW.puntos,
        puntos_totales_historicos = puntos_totales_historicos
            + CASE WHEN NEW.tipo::text = 'REEMBOLSO_TICKET_SORTEO' THEN 0
                   ELSE GREATEST(NEW.puntos, 0) END
    WHERE usuario_id = NEW.cliente_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 5. El precio y la validez del ticket los fija la base (igual que en canjes)
CREATE OR REPLACE FUNCTION fn_validar_nuevo_ticket_sorteo()
RETURNS TRIGGER AS $$
DECLARE
    v_estado TEXT;
    v_fecha  TIMESTAMPTZ;
    v_costo  INTEGER;
BEGIN
    SELECT estado::text, fecha_sorteo, costo_puntos_ticket
    INTO v_estado, v_fecha, v_costo
    FROM sorteos WHERE id = NEW.sorteo_id FOR SHARE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El sorteo indicado no existe';
    END IF;
    IF v_estado <> 'ACTIVO' OR v_fecha IS NULL OR v_fecha <= now() THEN
        RAISE EXCEPTION 'El sorteo no esta abierto a la compra de tickets';
    END IF;

    NEW.puntos_costo := v_costo;
    NEW.estado := 'VIGENTE';
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validar_nuevo_ticket_sorteo
    BEFORE INSERT ON tickets_sorteo
    FOR EACH ROW EXECUTE FUNCTION fn_validar_nuevo_ticket_sorteo();

-- 6. Un sorteo finalizado no se reabre ni cambia de ganador
CREATE OR REPLACE FUNCTION fn_proteger_sorteo_finalizado()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.estado::text IN ('SORTEADO', 'DESIERTO', 'ANULADO')
       AND (NEW.estado IS DISTINCT FROM OLD.estado
            OR NEW.ticket_ganador_id IS DISTINCT FROM OLD.ticket_ganador_id
            OR NEW.compromiso_sorteo_id IS DISTINCT FROM OLD.compromiso_sorteo_id)
    THEN
        RAISE EXCEPTION 'El sorteo % ya finalizo y no puede modificarse', OLD.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_proteger_sorteo_finalizado
    BEFORE UPDATE ON sorteos
    FOR EACH ROW EXECUTE FUNCTION fn_proteger_sorteo_finalizado();