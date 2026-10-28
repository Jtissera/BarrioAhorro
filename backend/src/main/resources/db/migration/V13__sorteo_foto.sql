ALTER TABLE compromisos_sorteo ADD COLUMN premio_foto_url TEXT;

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
        RAISE EXCEPTION 'Solo se puede recortar el fin de un ciclo vencido';
    END IF;

    IF OLD.estado <> 'PENDIENTE'
       AND (
           NEW.premio_nombre IS DISTINCT FROM OLD.premio_nombre
           OR NEW.premio_descripcion IS DISTINCT FROM OLD.premio_descripcion
           OR NEW.premio_valor IS DISTINCT FROM OLD.premio_valor
           OR NEW.premio_foto_url IS DISTINCT FROM OLD.premio_foto_url
           OR NEW.fecha_ratificacion IS DISTINCT FROM OLD.fecha_ratificacion
       )
    THEN
        RAISE EXCEPTION
            'No se pueden modificar los datos de un compromiso de sorteo finalizado';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;