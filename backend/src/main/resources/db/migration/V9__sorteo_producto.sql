-- Nombre y valor del premio del sorteo (la descripcion ya existia).
ALTER TABLE compromisos_sorteo
    ADD COLUMN premio_nombre VARCHAR(120),
    ADD COLUMN premio_valor  NUMERIC(12,2),
    ADD CONSTRAINT chk_sorteo_premio_valor_positivo
        CHECK (premio_valor IS NULL OR premio_valor > 0);

-- Igual que V8, pero nombre y valor tambien quedan inmutables al salir de PENDIENTE.
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
           NEW.premio_nombre IS DISTINCT FROM OLD.premio_nombre
           OR NEW.premio_descripcion IS DISTINCT FROM OLD.premio_descripcion
           OR NEW.premio_valor IS DISTINCT FROM OLD.premio_valor
           OR NEW.fecha_ratificacion IS DISTINCT FROM OLD.fecha_ratificacion
       )
    THEN
        RAISE EXCEPTION
            'No se pueden modificar los datos de un compromiso de sorteo finalizado';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;