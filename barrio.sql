--
-- PostgreSQL database dump
--

-- Dumped from database version 17.5 (Debian 17.5-1.pgdg110+1)
-- Dumped by pg_dump version 17.5 (Debian 17.5-1.pgdg110+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: tiger; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA tiger;


--
-- Name: tiger_data; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA tiger_data;


--
-- Name: topology; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA topology;


--
-- Name: SCHEMA topology; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON SCHEMA topology IS 'PostGIS Topology schema';


--
-- Name: btree_gist; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA public;


--
-- Name: EXTENSION btree_gist; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION btree_gist IS 'support for indexing common datatypes in GiST';


--
-- Name: citext; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS citext WITH SCHEMA public;


--
-- Name: EXTENSION citext; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION citext IS 'data type for case-insensitive character strings';


--
-- Name: fuzzystrmatch; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS fuzzystrmatch WITH SCHEMA public;


--
-- Name: EXTENSION fuzzystrmatch; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION fuzzystrmatch IS 'determine similarities and distance between strings';


--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


--
-- Name: postgis; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;


--
-- Name: EXTENSION postgis; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION postgis IS 'PostGIS geometry and geography spatial types and functions';


--
-- Name: postgis_tiger_geocoder; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS postgis_tiger_geocoder WITH SCHEMA tiger;


--
-- Name: EXTENSION postgis_tiger_geocoder; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION postgis_tiger_geocoder IS 'PostGIS tiger geocoder and reverse geocoder';


--
-- Name: postgis_topology; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS postgis_topology WITH SCHEMA topology;


--
-- Name: EXTENSION postgis_topology; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON EXTENSION postgis_topology IS 'PostGIS topology spatial types and functions';


--
-- Name: estado_canje_cupon_enum; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.estado_canje_cupon_enum AS ENUM (
    'PENDIENTE_VALIDACION',
    'VALIDADO',
    'CANCELADO',
    'VENCIDO'
);


--
-- Name: estado_compromiso_enum; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.estado_compromiso_enum AS ENUM (
    'PENDIENTE',
    'RATIFICADO',
    'VENCIDO'
);


--
-- Name: estado_revision_enum; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.estado_revision_enum AS ENUM (
    'PENDIENTE',
    'APROBADO',
    'RECHAZADO'
);


--
-- Name: estado_sorteo_enum; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.estado_sorteo_enum AS ENUM (
    'ACTIVO',
    'CERRADO',
    'SORTEADO',
    'DESIERTO',
    'ANULADO'
);


--
-- Name: tipo_beneficio_enum; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.tipo_beneficio_enum AS ENUM (
    'PORCENTAJE',
    'DOS_POR_UNO',
    'CANTIDAD_ULTIMA_UNIDAD',
    'POR_MAYOR'
);


--
-- Name: tipo_movimiento_puntos_enum; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.tipo_movimiento_puntos_enum AS ENUM (
    'UPVOTE_RECIBIDO',
    'CANJE_CUPON',
    'COMPRA_TICKET_SORTEO',
    'AJUSTE_ADMIN',
    'REEMBOLSO_TICKET_SORTEO'
);


--
-- Name: tipo_usuario_enum; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.tipo_usuario_enum AS ENUM (
    'CLIENTE',
    'COMERCIO',
    'ADMIN'
);


--
-- Name: tipo_vigencia_enum; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.tipo_vigencia_enum AS ENUM (
    'SOLO_HOY',
    'HASTA_AGOTAR_STOCK',
    'TODA_LA_SEMANA',
    'FECHA_ESPECIFICA'
);


--
-- Name: fn_actualizar_saldo_puntos(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_actualizar_saldo_puntos() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    UPDATE perfiles_cliente
    SET saldo_puntos = saldo_puntos + NEW.puntos,
        puntos_totales_historicos = puntos_totales_historicos
            + CASE WHEN NEW.tipo::text = 'REEMBOLSO_TICKET_SORTEO' THEN 0
                   ELSE GREATEST(NEW.puntos, 0) END
    WHERE usuario_id = NEW.cliente_id;
    RETURN NEW;
END;
$$;


--
-- Name: fn_actualizar_updated_at(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_actualizar_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$;


--
-- Name: FUNCTION fn_actualizar_updated_at(); Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON FUNCTION public.fn_actualizar_updated_at() IS 'Trigger BEFORE UPDATE genérico que actualiza la columna updated_at.';


--
-- Name: fn_bloquear_oferta_sin_acceso(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_bloquear_oferta_sin_acceso() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


--
-- Name: FUNCTION fn_bloquear_oferta_sin_acceso(); Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON FUNCTION public.fn_bloquear_oferta_sin_acceso() IS 'Bloquea nuevas ofertas y reactivaciones cuando el comercio no completo el onboarding o tiene acceso restringido.';


--
-- Name: fn_compromiso_sorteo_nace_pendiente(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_compromiso_sorteo_nace_pendiente() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.estado := 'PENDIENTE';
    NEW.fecha_ratificacion := NULL;
    RETURN NEW;
END;
$$;


--
-- Name: fn_crear_perfil_por_tipo_usuario(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_crear_perfil_por_tipo_usuario() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF NEW.tipo_usuario = 'CLIENTE' THEN
        INSERT INTO perfiles_cliente (usuario_id) VALUES (NEW.id);
    ELSIF NEW.tipo_usuario = 'COMERCIO' THEN
        INSERT INTO perfiles_comercio (usuario_id) VALUES (NEW.id);
    END IF;
    RETURN NEW;
END;
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: ofertas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ofertas (
    id bigint NOT NULL,
    comercio_id bigint NOT NULL,
    tipo_oferta public.tipo_beneficio_enum NOT NULL,
    nombre_producto text NOT NULL,
    foto_url text,
    vigencia_tipo public.tipo_vigencia_enum NOT NULL,
    vigente_hasta timestamp with time zone,
    activa boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: fn_oferta_vigente(public.ofertas); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_oferta_vigente(p_oferta public.ofertas) RETURNS boolean
    LANGUAGE sql STABLE
    AS $$
    SELECT p_oferta.activa AND (p_oferta.vigente_hasta IS NULL OR p_oferta.vigente_hasta > now());
$$;


--
-- Name: fn_otorgar_puntos_upvote(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_otorgar_puntos_upvote() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_autor_id BIGINT;
    v_delta    INTEGER;
    v_fila_id  BIGINT;
BEGIN
    v_delta   := CASE WHEN TG_OP = 'INSERT' THEN 1 ELSE -1 END;
    v_fila_id := CASE WHEN TG_OP = 'INSERT' THEN NEW.id ELSE OLD.id END;

    IF TG_TABLE_NAME = 'foro_publicacion_upvotes' THEN
        IF TG_OP = 'INSERT' THEN
            SELECT autor_id INTO v_autor_id FROM foro_publicaciones WHERE id = NEW.publicacion_id;
        ELSE
            SELECT autor_id INTO v_autor_id FROM foro_publicaciones WHERE id = OLD.publicacion_id;
        END IF;
    ELSE
        IF TG_OP = 'INSERT' THEN
            SELECT autor_id INTO v_autor_id FROM foro_comentarios WHERE id = NEW.comentario_id;
        ELSE
            SELECT autor_id INTO v_autor_id FROM foro_comentarios WHERE id = OLD.comentario_id;
        END IF;
    END IF;

    INSERT INTO movimientos_puntos (cliente_id, tipo, puntos, referencia_tabla, referencia_id, descripcion)
    VALUES (
        v_autor_id,
        'UPVOTE_RECIBIDO',
        v_delta,
        TG_TABLE_NAME,
        v_fila_id,
        CASE WHEN TG_OP = 'INSERT' THEN 'Upvote recibido' ELSE 'Upvote retirado' END
    );

    IF TG_OP = 'INSERT' THEN
        RETURN NEW;
    ELSE
        RETURN OLD;
    END IF;
END;
$$;


--
-- Name: fn_procesar_compra_ticket_sorteo(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_procesar_compra_ticket_sorteo() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    INSERT INTO movimientos_puntos (cliente_id, tipo, puntos, referencia_tabla, referencia_id, descripcion)
    VALUES (NEW.cliente_id, 'COMPRA_TICKET_SORTEO', -NEW.puntos_costo, 'tickets_sorteo', NEW.id,
            'Compra de ticket para sorteo');
    RETURN NEW;
END;
$$;


--
-- Name: fn_procesar_transicion_canje(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_procesar_transicion_canje() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


--
-- Name: fn_proteger_compromiso_cuponera(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_proteger_compromiso_cuponera() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


--
-- Name: fn_proteger_compromiso_sorteo(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_proteger_compromiso_sorteo() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


--
-- Name: fn_proteger_cupon_ratificado(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_proteger_cupon_ratificado() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


--
-- Name: fn_proteger_sorteo_finalizado(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_proteger_sorteo_finalizado() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


--
-- Name: fn_validar_comentario_mismo_hilo(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_validar_comentario_mismo_hilo() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_publicacion_padre BIGINT;
BEGIN
    IF NEW.comentario_padre_id IS NOT NULL THEN
        SELECT publicacion_id INTO v_publicacion_padre
        FROM foro_comentarios WHERE id = NEW.comentario_padre_id;

        IF v_publicacion_padre IS DISTINCT FROM NEW.publicacion_id THEN
            RAISE EXCEPTION 'El comentario padre % pertenece a otra publicación', NEW.comentario_padre_id;
        END IF;
    END IF;
    RETURN NEW;
END;
$$;


--
-- Name: fn_validar_detalle_oferta(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_validar_detalle_oferta() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_existe BOOLEAN;
BEGIN
    CASE NEW.tipo_oferta
        WHEN 'PORCENTAJE' THEN
            SELECT EXISTS(SELECT 1 FROM ofertas_porcentaje WHERE oferta_id = NEW.id) INTO v_existe;
        WHEN 'DOS_POR_UNO' THEN
            SELECT EXISTS(SELECT 1 FROM ofertas_2x1 WHERE oferta_id = NEW.id) INTO v_existe;
        WHEN 'CANTIDAD_ULTIMA_UNIDAD' THEN
            SELECT EXISTS(SELECT 1 FROM ofertas_cantidad WHERE oferta_id = NEW.id) INTO v_existe;
        WHEN 'POR_MAYOR' THEN
            SELECT EXISTS(SELECT 1 FROM ofertas_por_mayor WHERE oferta_id = NEW.id) INTO v_existe;
    END CASE;

    IF NOT v_existe THEN
        RAISE EXCEPTION 'La oferta % (tipo %) no tiene su fila de detalle correspondiente',
            NEW.id, NEW.tipo_oferta;
    END IF;

    RETURN NULL;
END;
$$;


--
-- Name: fn_validar_nuevo_canje(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_validar_nuevo_canje() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


--
-- Name: fn_validar_nuevo_ticket_sorteo(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_validar_nuevo_ticket_sorteo() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


--
-- Name: fn_validar_orden_ciclo_sorteo(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_validar_orden_ciclo_sorteo() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


--
-- Name: fn_validar_orden_periodo_cuponera(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_validar_orden_periodo_cuponera() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


--
-- Name: fn_validar_rango_cupones_compromiso(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_validar_rango_cupones_compromiso() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


--
-- Name: fn_validar_ratificacion_cuponera(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_validar_ratificacion_cuponera() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


--
-- Name: fn_validar_ratificacion_sorteo(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_validar_ratificacion_sorteo() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


--
-- Name: fn_validar_tipo_oferta(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_validar_tipo_oferta() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_tipo_esperado tipo_beneficio_enum := TG_ARGV[0]::tipo_beneficio_enum;
    v_tipo_real     tipo_beneficio_enum;
BEGIN
    SELECT tipo_oferta INTO v_tipo_real FROM ofertas WHERE id = NEW.oferta_id;

    IF v_tipo_real IS NULL THEN
        RAISE EXCEPTION 'La oferta % no existe', NEW.oferta_id;
    ELSIF v_tipo_real <> v_tipo_esperado THEN
        RAISE EXCEPTION 'La oferta % es de tipo % pero se intenta insertar detalle de tipo %',
            NEW.oferta_id, v_tipo_real, v_tipo_esperado;
    END IF;

    RETURN NEW;
END;
$$;


--
-- Name: fn_validar_upvote_no_propio(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_validar_upvote_no_propio() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_autor_id BIGINT;
BEGIN
    IF TG_TABLE_NAME = 'foro_publicacion_upvotes' THEN
        SELECT autor_id INTO v_autor_id FROM foro_publicaciones WHERE id = NEW.publicacion_id;
    ELSE
        SELECT autor_id INTO v_autor_id FROM foro_comentarios WHERE id = NEW.comentario_id;
    END IF;

    IF v_autor_id = NEW.usuario_id THEN
        RAISE EXCEPTION 'Un usuario no puede votar su propia publicación o comentario';
    END IF;

    RETURN NEW;
END;
$$;


--
-- Name: fn_vencer_compromisos(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.fn_vencer_compromisos() RETURNS integer
    LANGUAGE plpgsql
    AS $$
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
$$;


--
-- Name: FUNCTION fn_vencer_compromisos(); Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON FUNCTION public.fn_vencer_compromisos() IS 'Marca como vencidos los compromisos pendientes cuyo plazo expiro. Debe ejecutarse periodicamente desde un scheduler.';


--
-- Name: canjes_cupon; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.canjes_cupon (
    id bigint NOT NULL,
    cupon_id bigint NOT NULL,
    cliente_id bigint NOT NULL,
    codigo_qr text DEFAULT (gen_random_uuid())::text NOT NULL,
    puntos_costo integer NOT NULL,
    estado public.estado_canje_cupon_enum DEFAULT 'PENDIENTE_VALIDACION'::public.estado_canje_cupon_enum NOT NULL,
    fecha_canje timestamp with time zone DEFAULT now() NOT NULL,
    fecha_validacion timestamp with time zone,
    fecha_cancelacion timestamp with time zone,
    fecha_vencimiento timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT canjes_cupon_puntos_costo_check CHECK ((puntos_costo > 0))
);


--
-- Name: COLUMN canjes_cupon.puntos_costo; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.canjes_cupon.puntos_costo IS 'Snapshot del costo en puntos fijado por la base de datos al crear el canje.';


--
-- Name: canjes_cupon_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.canjes_cupon ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.canjes_cupon_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: compromisos_cuponera; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.compromisos_cuponera (
    id bigint NOT NULL,
    comercio_id bigint NOT NULL,
    periodo_inicio date NOT NULL,
    periodo_fin date NOT NULL,
    fecha_limite_ratificacion timestamp with time zone NOT NULL,
    estado public.estado_compromiso_enum DEFAULT 'PENDIENTE'::public.estado_compromiso_enum NOT NULL,
    fecha_ratificacion timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_compromiso_cuponera_periodo CHECK ((periodo_fin > periodo_inicio))
);


--
-- Name: compromisos_cuponera_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.compromisos_cuponera ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.compromisos_cuponera_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: compromisos_sorteo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.compromisos_sorteo (
    id bigint NOT NULL,
    comercio_id bigint NOT NULL,
    ciclo_inicio date NOT NULL,
    ciclo_fin date NOT NULL,
    fecha_limite_ratificacion timestamp with time zone NOT NULL,
    premio_descripcion text NOT NULL,
    estado public.estado_compromiso_enum DEFAULT 'PENDIENTE'::public.estado_compromiso_enum NOT NULL,
    fecha_ratificacion timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    premio_nombre character varying(120),
    premio_valor numeric(12,2),
    estado_revision public.estado_revision_enum DEFAULT 'PENDIENTE'::public.estado_revision_enum NOT NULL,
    motivo_rechazo text,
    revisado_en timestamp with time zone,
    premio_foto_url text,
    CONSTRAINT chk_compromiso_sorteo_ciclo CHECK ((ciclo_fin > ciclo_inicio)),
    CONSTRAINT chk_sorteo_motivo_rechazo CHECK (((estado_revision <> 'RECHAZADO'::public.estado_revision_enum) OR (length(btrim(motivo_rechazo)) >= 15))),
    CONSTRAINT chk_sorteo_premio_valor_positivo CHECK (((premio_valor IS NULL) OR (premio_valor > (0)::numeric)))
);


--
-- Name: compromisos_sorteo_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.compromisos_sorteo ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.compromisos_sorteo_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: confirmaciones_canje; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.confirmaciones_canje (
    canje_id bigint NOT NULL,
    cliente_id bigint NOT NULL,
    recibido boolean NOT NULL,
    puntos_otorgados integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT confirmaciones_canje_puntos_otorgados_check CHECK ((puntos_otorgados >= 0))
);


--
-- Name: cupones_cuponera; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cupones_cuponera (
    id bigint NOT NULL,
    compromiso_id bigint NOT NULL,
    costo_puntos integer NOT NULL,
    stock_comprometido smallint NOT NULL,
    stock_canjeado smallint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    nombre_producto text NOT NULL,
    descripcion text,
    foto_url text,
    valor_comercio numeric(12,2),
    estado_revision public.estado_revision_enum DEFAULT 'PENDIENTE'::public.estado_revision_enum NOT NULL,
    motivo_rechazo text,
    revisado_en timestamp with time zone,
    CONSTRAINT chk_cupones_cuponera_stock CHECK (((stock_canjeado >= 0) AND (stock_canjeado <= stock_comprometido))),
    CONSTRAINT chk_cupones_descripcion_no_vacia CHECK (((descripcion IS NULL) OR (length(btrim(descripcion)) > 0))),
    CONSTRAINT chk_cupones_motivo_rechazo CHECK (((estado_revision <> 'RECHAZADO'::public.estado_revision_enum) OR (length(btrim(motivo_rechazo)) >= 15))),
    CONSTRAINT chk_cupones_nombre_no_vacio CHECK ((length(btrim(nombre_producto)) > 0)),
    CONSTRAINT chk_cupones_valor_comercio_positivo CHECK (((valor_comercio IS NULL) OR (valor_comercio > (0)::numeric))),
    CONSTRAINT cupones_cuponera_costo_puntos_check CHECK ((costo_puntos > 0)),
    CONSTRAINT cupones_cuponera_stock_comprometido_check CHECK ((stock_comprometido > 0))
);


--
-- Name: TABLE cupones_cuponera; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.cupones_cuponera IS 'Premios concretos ofrecidos en un compromiso de cuponera, con precio en puntos y stock comprometido.';


--
-- Name: COLUMN cupones_cuponera.stock_canjeado; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.cupones_cuponera.stock_canjeado IS 'Cantidad de canjes validados. Las reservas pendientes se contabilizan a partir de canjes_cupon.';


--
-- Name: COLUMN cupones_cuponera.valor_comercio; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.cupones_cuponera.valor_comercio IS 'Precio de venta del producto en el comercio (pesos) al momento del aporte.';


--
-- Name: cupones_cuponera_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.cupones_cuponera ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.cupones_cuponera_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: favoritos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.favoritos (
    id bigint NOT NULL,
    cliente_id bigint NOT NULL,
    oferta_id bigint NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: favoritos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.favoritos ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.favoritos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: foro_comentario_upvotes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.foro_comentario_upvotes (
    id bigint NOT NULL,
    comentario_id bigint NOT NULL,
    usuario_id bigint NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: foro_comentario_upvotes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.foro_comentario_upvotes ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.foro_comentario_upvotes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: foro_comentarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.foro_comentarios (
    id bigint NOT NULL,
    publicacion_id bigint NOT NULL,
    comentario_padre_id bigint,
    autor_id bigint NOT NULL,
    texto text NOT NULL,
    eliminado_en timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: foro_comentarios_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.foro_comentarios ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.foro_comentarios_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: foro_publicacion_upvotes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.foro_publicacion_upvotes (
    id bigint NOT NULL,
    publicacion_id bigint NOT NULL,
    usuario_id bigint NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: foro_publicacion_upvotes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.foro_publicacion_upvotes ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.foro_publicacion_upvotes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: foro_publicaciones; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.foro_publicaciones (
    id bigint NOT NULL,
    autor_id bigint NOT NULL,
    comercio_id bigint NOT NULL,
    texto text NOT NULL,
    precio_observado numeric(12,2),
    ubicacion_observada public.geography(Point,4326),
    eliminado_en timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    direccion_observada text,
    CONSTRAINT foro_publicaciones_precio_observado_check CHECK ((precio_observado > (0)::numeric))
);


--
-- Name: COLUMN foro_publicaciones.direccion_observada; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.foro_publicaciones.direccion_observada IS 'Dirección o intersección escrita por el autor. Se muestra en el chip de ubicación; NULL si no indicó ninguna.';


--
-- Name: foro_publicaciones_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.foro_publicaciones ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.foro_publicaciones_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: fotos_comercio; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fotos_comercio (
    id bigint NOT NULL,
    comercio_id bigint NOT NULL,
    url text NOT NULL,
    orden smallint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: fotos_comercio_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.fotos_comercio ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.fotos_comercio_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: horarios_atencion; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.horarios_atencion (
    id bigint NOT NULL,
    comercio_id bigint NOT NULL,
    dia_semana integer NOT NULL,
    hora_inicio time without time zone NOT NULL,
    hora_fin time without time zone NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_horarios_atencion_dia CHECK (((dia_semana >= 1) AND (dia_semana <= 7))),
    CONSTRAINT chk_horarios_atencion_rango CHECK ((hora_fin > hora_inicio))
);


--
-- Name: TABLE horarios_atencion; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.horarios_atencion IS 'Horarios de atencion del comercio. dia_semana: 1=lunes ... 7=domingo.';


--
-- Name: horarios_atencion_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.horarios_atencion_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: horarios_atencion_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.horarios_atencion_id_seq OWNED BY public.horarios_atencion.id;


--
-- Name: movimientos_puntos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.movimientos_puntos (
    id bigint NOT NULL,
    cliente_id bigint NOT NULL,
    tipo public.tipo_movimiento_puntos_enum NOT NULL,
    puntos integer NOT NULL,
    referencia_tabla text,
    referencia_id bigint,
    descripcion text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_movimientos_puntos_no_cero CHECK ((puntos <> 0))
);


--
-- Name: TABLE movimientos_puntos; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.movimientos_puntos IS 'Ledger inmutable de puntos. Fuente de verdad de saldo_puntos y puntos_totales_historicos.';


--
-- Name: movimientos_puntos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.movimientos_puntos ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.movimientos_puntos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: ofertas_2x1; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ofertas_2x1 (
    oferta_id bigint NOT NULL,
    unidades_a_pagar smallint NOT NULL,
    unidades_a_llevar smallint NOT NULL,
    precio_unitario numeric(12,2),
    CONSTRAINT ofertas_2x1_check CHECK ((unidades_a_llevar > unidades_a_pagar)),
    CONSTRAINT ofertas_2x1_precio_unitario_check CHECK ((precio_unitario > (0)::numeric)),
    CONSTRAINT ofertas_2x1_unidades_a_pagar_check CHECK ((unidades_a_pagar > 0))
);


--
-- Name: TABLE ofertas_2x1; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.ofertas_2x1 IS 'Cubre 2x1, 3x2 y variantes equivalentes: unidades_a_llevar > unidades_a_pagar (SCRUM-73).';


--
-- Name: ofertas_cantidad; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ofertas_cantidad (
    oferta_id bigint NOT NULL,
    cantidad_requerida smallint NOT NULL,
    precio_unitario numeric(12,2) NOT NULL,
    porcentaje_descuento_ultima_unidad smallint NOT NULL,
    CONSTRAINT ofertas_cantidad_cantidad_requerida_check CHECK ((cantidad_requerida > 0)),
    CONSTRAINT ofertas_cantidad_porcentaje_descuento_ultima_unidad_check CHECK (((porcentaje_descuento_ultima_unidad >= 1) AND (porcentaje_descuento_ultima_unidad <= 100))),
    CONSTRAINT ofertas_cantidad_precio_unitario_check CHECK ((precio_unitario > (0)::numeric))
);


--
-- Name: TABLE ofertas_cantidad; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.ofertas_cantidad IS 'Descuento sobre la última unidad al comprar una cantidad mínima (SCRUM-71).';


--
-- Name: ofertas_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.ofertas ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.ofertas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: ofertas_por_mayor; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ofertas_por_mayor (
    oferta_id bigint NOT NULL,
    cantidad_minima smallint NOT NULL,
    precio_unitario_mayorista numeric(12,2) NOT NULL,
    precio_unitario_regular numeric(12,2),
    CONSTRAINT ofertas_por_mayor_cantidad_minima_check CHECK ((cantidad_minima > 0)),
    CONSTRAINT ofertas_por_mayor_precio_unitario_mayorista_check CHECK ((precio_unitario_mayorista > (0)::numeric)),
    CONSTRAINT ofertas_por_mayor_precio_unitario_regular_check CHECK ((precio_unitario_regular > (0)::numeric))
);


--
-- Name: TABLE ofertas_por_mayor; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.ofertas_por_mayor IS 'SCRUM-74 deja el modelo de precios "a definir": estructura mínima razonable, candidata a ajustarse cuando se cierre esa historia.';


--
-- Name: ofertas_porcentaje; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ofertas_porcentaje (
    oferta_id bigint NOT NULL,
    precio_original numeric(12,2) NOT NULL,
    porcentaje_descuento smallint NOT NULL,
    precio_final numeric(12,2) GENERATED ALWAYS AS (round((precio_original * ((1)::numeric - ((porcentaje_descuento)::numeric / (100)::numeric))), 2)) STORED,
    CONSTRAINT ofertas_porcentaje_porcentaje_descuento_check CHECK (((porcentaje_descuento >= 1) AND (porcentaje_descuento <= 100))),
    CONSTRAINT ofertas_porcentaje_precio_original_check CHECK ((precio_original > (0)::numeric))
);


--
-- Name: perfiles_cliente; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.perfiles_cliente (
    usuario_id bigint NOT NULL,
    saldo_puntos integer DEFAULT 0 NOT NULL,
    puntos_totales_historicos integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    sorteos_desbloqueo_visto_en timestamp with time zone,
    CONSTRAINT chk_perfiles_cliente_historicos_mayor_saldo CHECK ((puntos_totales_historicos >= saldo_puntos)),
    CONSTRAINT chk_perfiles_cliente_historicos_no_negativo CHECK ((puntos_totales_historicos >= 0)),
    CONSTRAINT chk_perfiles_cliente_saldo_no_negativo CHECK ((saldo_puntos >= 0))
);


--
-- Name: COLUMN perfiles_cliente.saldo_puntos; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.perfiles_cliente.saldo_puntos IS 'Saldo gastable actual (sube y baja). Única fuente de verdad: la suma de movimientos_puntos.';


--
-- Name: COLUMN perfiles_cliente.puntos_totales_historicos; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.perfiles_cliente.puntos_totales_historicos IS 'Puntos ganados acumulados de por vida (nunca decrece). Usado para desbloquear sorteos a los 100 puntos (SCRUM-91), independientemente de cuánto haya gastado el cliente después.';


--
-- Name: perfiles_comercio; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.perfiles_comercio (
    usuario_id bigint NOT NULL,
    nombre_negocio text,
    rubro_id smallint,
    descripcion text,
    direccion text,
    ubicacion public.geography(Point,4326),
    onboarding_completado boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_perfiles_comercio_onboarding_completo CHECK (((NOT onboarding_completado) OR ((nombre_negocio IS NOT NULL) AND (rubro_id IS NOT NULL) AND (direccion IS NOT NULL) AND (ubicacion IS NOT NULL) AND (descripcion IS NOT NULL))))
);


--
-- Name: TABLE perfiles_comercio; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.perfiles_comercio IS 'Datos de negocio de un usuario con rol COMERCIO. 1:1 con usuarios.';


--
-- Name: premios_sorteo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.premios_sorteo (
    ticket_id bigint NOT NULL,
    sorteo_id bigint NOT NULL,
    cliente_id bigint NOT NULL,
    token uuid DEFAULT gen_random_uuid() NOT NULL,
    vence_en timestamp with time zone NOT NULL,
    visto_en timestamp with time zone,
    canjeado_en timestamp with time zone,
    canjeado_por bigint,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_premios_canje CHECK (((canjeado_en IS NULL) = (canjeado_por IS NULL)))
);


--
-- Name: reportes_canje; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.reportes_canje (
    id bigint NOT NULL,
    canje_id bigint NOT NULL,
    cliente_id bigint NOT NULL,
    motivo text NOT NULL,
    descripcion text NOT NULL,
    estado text DEFAULT 'ABIERTO'::text NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT reportes_canje_descripcion_check CHECK (((length(btrim(descripcion)) >= 1) AND (length(btrim(descripcion)) <= 300))),
    CONSTRAINT reportes_canje_estado_check CHECK ((estado = ANY (ARRAY['ABIERTO'::text, 'RESUELTO'::text]))),
    CONSTRAINT reportes_canje_motivo_check CHECK ((motivo = ANY (ARRAY['PEDIDO_INCOMPLETO'::text, 'CALIDAD'::text, 'HORARIO'::text, 'OTRO'::text])))
);


--
-- Name: reportes_canje_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.reportes_canje ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.reportes_canje_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: resenas_comercio; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.resenas_comercio (
    id bigint NOT NULL,
    comercio_id bigint NOT NULL,
    cliente_id bigint NOT NULL,
    texto text NOT NULL,
    estrellas smallint NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT resenas_comercio_estrellas_check CHECK (((estrellas >= 1) AND (estrellas <= 5)))
);


--
-- Name: resenas_comercio_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.resenas_comercio ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.resenas_comercio_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: rubros; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.rubros (
    id smallint NOT NULL,
    nombre text NOT NULL
);


--
-- Name: TABLE rubros; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.rubros IS 'Catálogo de rubros de comercio. Extensible con INSERT, sin necesidad de migración.';


--
-- Name: rubros_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.rubros ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.rubros_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: sorteos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sorteos (
    id bigint NOT NULL,
    compromiso_sorteo_id bigint NOT NULL,
    costo_puntos_ticket integer NOT NULL,
    estado public.estado_sorteo_enum DEFAULT 'ACTIVO'::public.estado_sorteo_enum NOT NULL,
    fecha_sorteo timestamp with time zone,
    ticket_ganador_id bigint,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    ejecutado_en timestamp with time zone,
    total_tickets integer,
    CONSTRAINT chk_sorteos_activo_con_fecha CHECK ((((estado)::text <> 'ACTIVO'::text) OR (fecha_sorteo IS NOT NULL))),
    CONSTRAINT sorteos_costo_puntos_ticket_check CHECK ((costo_puntos_ticket > 0))
);


--
-- Name: sorteos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.sorteos ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.sorteos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tickets_sorteo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tickets_sorteo (
    id bigint NOT NULL,
    sorteo_id bigint NOT NULL,
    cliente_id bigint NOT NULL,
    puntos_costo integer NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    estado text DEFAULT 'VIGENTE'::text NOT NULL,
    CONSTRAINT tickets_sorteo_estado_check CHECK ((estado = ANY (ARRAY['VIGENTE'::text, 'GANADOR'::text, 'NO_GANADOR'::text, 'ANULADO'::text]))),
    CONSTRAINT tickets_sorteo_puntos_costo_check CHECK ((puntos_costo > 0))
);


--
-- Name: tickets_sorteo_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tickets_sorteo ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.tickets_sorteo_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: usuarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.usuarios (
    id bigint NOT NULL,
    email public.citext NOT NULL,
    password_hash text NOT NULL,
    tipo_usuario public.tipo_usuario_enum NOT NULL,
    email_verificado boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_usuarios_email_gmail CHECK ((email OPERATOR(public.~*) '^[a-z0-9._%+-]+@gmail\.com$'::public.citext))
);


--
-- Name: TABLE usuarios; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.usuarios IS 'Cuenta de acceso. El rol define qué tabla de perfil se crea automáticamente.';


--
-- Name: usuarios_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.usuarios ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.usuarios_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: v_comercio_acceso_restringido; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.v_comercio_acceso_restringido AS
 SELECT usuario_id AS comercio_id,
    ((NOT (EXISTS ( SELECT 1
           FROM public.compromisos_cuponera cc
          WHERE ((cc.comercio_id = pc.usuario_id) AND (cc.estado = 'RATIFICADO'::public.estado_compromiso_enum))))) OR (NOT (EXISTS ( SELECT 1
           FROM public.compromisos_sorteo cs
          WHERE ((cs.comercio_id = pc.usuario_id) AND (cs.estado = 'RATIFICADO'::public.estado_compromiso_enum))))) OR (EXISTS ( SELECT 1
           FROM public.compromisos_cuponera cc
          WHERE ((cc.comercio_id = pc.usuario_id) AND ((cc.estado = 'VENCIDO'::public.estado_compromiso_enum) OR ((cc.estado = 'PENDIENTE'::public.estado_compromiso_enum) AND (cc.fecha_limite_ratificacion < now()))) AND (NOT (EXISTS ( SELECT 1
                   FROM public.compromisos_cuponera posterior
                  WHERE ((posterior.comercio_id = cc.comercio_id) AND (posterior.estado = 'RATIFICADO'::public.estado_compromiso_enum) AND (posterior.periodo_inicio > cc.periodo_inicio)))))))) OR (EXISTS ( SELECT 1
           FROM public.compromisos_sorteo cs
          WHERE ((cs.comercio_id = pc.usuario_id) AND ((cs.estado = 'VENCIDO'::public.estado_compromiso_enum) OR ((cs.estado = 'PENDIENTE'::public.estado_compromiso_enum) AND (cs.fecha_limite_ratificacion < now()))) AND (NOT (EXISTS ( SELECT 1
                   FROM public.compromisos_sorteo posterior
                  WHERE ((posterior.comercio_id = cs.comercio_id) AND (posterior.estado = 'RATIFICADO'::public.estado_compromiso_enum) AND (posterior.ciclo_inicio > cs.ciclo_inicio)))))))) OR (EXISTS ( SELECT 1
           FROM (public.cupones_cuponera cu
             JOIN public.compromisos_cuponera cc ON ((cc.id = cu.compromiso_id)))
          WHERE ((cc.comercio_id = pc.usuario_id) AND (cu.estado_revision = 'RECHAZADO'::public.estado_revision_enum) AND (cc.estado <> 'RATIFICADO'::public.estado_compromiso_enum) AND (NOT (EXISTS ( SELECT 1
                   FROM public.compromisos_cuponera posterior
                  WHERE ((posterior.comercio_id = cc.comercio_id) AND (posterior.estado = 'RATIFICADO'::public.estado_compromiso_enum) AND (posterior.periodo_inicio > cc.periodo_inicio)))))))) OR (EXISTS ( SELECT 1
           FROM public.compromisos_sorteo cs
          WHERE ((cs.comercio_id = pc.usuario_id) AND (cs.estado_revision = 'RECHAZADO'::public.estado_revision_enum) AND (cs.estado <> 'RATIFICADO'::public.estado_compromiso_enum) AND (NOT (EXISTS ( SELECT 1
                   FROM public.compromisos_sorteo posterior
                  WHERE ((posterior.comercio_id = cs.comercio_id) AND (posterior.estado = 'RATIFICADO'::public.estado_compromiso_enum) AND (posterior.ciclo_inicio > cs.ciclo_inicio))))))))) AS acceso_restringido
   FROM public.perfiles_comercio pc;


--
-- Name: VIEW v_comercio_acceso_restringido; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON VIEW public.v_comercio_acceso_restringido IS 'Indica si un comercio carece de compromisos ratificados requeridos o mantiene vencimientos sin reemplazo ratificado. La aplicacion debe aplicar el modo de solo lectura.';


--
-- Name: validaciones_comunitarias_oferta; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.validaciones_comunitarias_oferta (
    id bigint NOT NULL,
    oferta_id bigint NOT NULL,
    cliente_id bigint NOT NULL,
    sigue_vigente boolean NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: TABLE validaciones_comunitarias_oferta; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.validaciones_comunitarias_oferta IS 'SCRUM-58: el comercio nunca puede votar acá porque cliente_id referencia perfiles_cliente, que no existe para usuarios de tipo COMERCIO.';


--
-- Name: validaciones_comunitarias_oferta_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.validaciones_comunitarias_oferta ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.validaciones_comunitarias_oferta_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: visualizaciones_comercio; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.visualizaciones_comercio (
    id bigint NOT NULL,
    comercio_id bigint NOT NULL,
    usuario_id bigint,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: visualizaciones_comercio_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.visualizaciones_comercio ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.visualizaciones_comercio_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: visualizaciones_oferta; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.visualizaciones_oferta (
    id bigint NOT NULL,
    oferta_id bigint NOT NULL,
    usuario_id bigint,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: visualizaciones_oferta_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.visualizaciones_oferta ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.visualizaciones_oferta_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: horarios_atencion id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.horarios_atencion ALTER COLUMN id SET DEFAULT nextval('public.horarios_atencion_id_seq'::regclass);


--
-- Data for Name: canjes_cupon; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.canjes_cupon (id, cupon_id, cliente_id, codigo_qr, puntos_costo, estado, fecha_canje, fecha_validacion, fecha_cancelacion, fecha_vencimiento, created_at, updated_at) FROM stdin;
1	31	66	123d022b-8aef-416e-b51b-dbdf9271e901	1	CANCELADO	2026-11-02 01:35:34.802859+00	\N	2026-11-02 01:35:54.404886+00	2026-11-16 03:00:00+00	2026-11-02 01:35:34.802859+00	2026-11-02 01:35:54.404886+00
2	31	66	9a9d69a3-ab8a-4245-ab1b-5d3708ab7db7	1	CANCELADO	2026-11-02 01:36:01.612785+00	\N	2026-11-02 01:36:03.734561+00	2026-11-16 03:00:00+00	2026-11-02 01:36:01.612785+00	2026-11-02 01:36:03.734561+00
3	31	66	7d14b804-23dd-4432-880c-740e0c169b86	1	CANCELADO	2026-11-02 02:20:49.297064+00	\N	2026-11-02 02:26:21.538821+00	2026-11-16 03:00:00+00	2026-11-02 02:20:49.297064+00	2026-11-02 02:26:21.538821+00
4	31	66	7476d393-9767-45f4-9bd9-e582bb872744	1	VALIDADO	2026-11-02 02:27:35.848322+00	2026-11-02 02:29:44.902272+00	\N	2026-11-16 03:00:00+00	2026-11-02 02:27:35.848322+00	2026-11-02 02:29:44.902272+00
5	31	66	76097e8f-b0c1-4364-a31a-8b68ba255f33	1	CANCELADO	2026-11-02 02:34:39.487777+00	\N	2026-11-02 02:34:46.92461+00	2026-11-16 03:00:00+00	2026-11-02 02:34:39.487777+00	2026-11-02 02:34:46.92461+00
6	31	66	4d30cd77-f9ab-43c9-8566-81edc3ca791f	1	VALIDADO	2026-11-02 02:36:47.979428+00	2026-11-02 02:37:31.625189+00	\N	2026-11-16 03:00:00+00	2026-11-02 02:36:47.979428+00	2026-11-02 02:37:31.625189+00
7	31	66	98c77770-7ac4-4cf5-a106-e0d5e026c1ca	1	VALIDADO	2026-11-02 02:59:37.204197+00	2026-11-02 03:00:21.611148+00	\N	2026-11-16 03:00:00+00	2026-11-02 02:59:37.204197+00	2026-11-02 03:00:21.611148+00
8	31	66	61e03e5d-cfa1-4687-aa11-2a703a58dc74	1	VALIDADO	2026-11-02 03:02:19.508333+00	2026-11-02 03:02:55.079889+00	\N	2026-11-16 03:00:00+00	2026-11-02 03:02:19.508333+00	2026-11-02 03:02:55.079889+00
9	31	66	7b0e7e63-0c8c-491a-82d9-8e58f2c46bd2	1	CANCELADO	2026-11-02 03:10:43.963913+00	\N	2026-11-02 03:10:58.641396+00	2026-11-16 03:00:00+00	2026-11-02 03:10:43.963913+00	2026-11-02 03:10:58.641396+00
10	31	66	2030232b-38a6-4b7e-9140-42de17a9abcd	1	CANCELADO	2026-11-02 03:20:34.291236+00	\N	2026-11-02 03:20:39.319007+00	2026-11-16 03:00:00+00	2026-11-02 03:20:34.291236+00	2026-11-02 03:20:39.319007+00
11	31	66	6bbf8c58-60a2-4cf3-ac7a-37be803f3d24	1	PENDIENTE_VALIDACION	2026-11-02 03:21:45.062786+00	\N	\N	2026-11-16 03:00:00+00	2026-11-02 03:21:45.062786+00	2026-11-02 03:21:45.062786+00
12	32	78	52eafb80-e1a7-4049-a22d-f916fe341267	3	VALIDADO	2026-11-02 06:45:20.809526+00	2026-11-02 07:14:40.15624+00	\N	2026-11-16 03:00:00+00	2026-11-02 06:45:20.809526+00	2026-11-02 07:14:40.15624+00
13	26	78	7e1d2c4f-844b-4277-bfe1-cbd569b19ff3	9	VALIDADO	2026-11-02 07:16:02.113514+00	2026-11-02 07:17:32.109526+00	\N	2026-11-16 03:00:00+00	2026-11-02 07:16:02.113514+00	2026-11-02 07:17:32.109526+00
14	24	77	fb20fcbf-6d5a-4fae-ab84-b165cd5658a3	7	VALIDADO	2026-11-05 01:24:41.493306+00	2026-11-05 01:25:20.076361+00	\N	2026-11-16 03:00:00+00	2026-11-05 01:24:41.493306+00	2026-11-05 01:25:20.076361+00
15	24	78	41a0f844-af42-42b7-bde2-417218c4ae08	7	CANCELADO	2026-11-05 21:41:23.281485+00	\N	2026-11-05 21:41:34.168507+00	2026-11-16 03:00:00+00	2026-11-05 21:41:23.281485+00	2026-11-05 21:41:34.168507+00
16	24	78	5732d0f2-5260-4060-a60d-1911614c16f1	7	PENDIENTE_VALIDACION	2026-11-05 21:42:07.288454+00	\N	\N	2026-11-16 03:00:00+00	2026-11-05 21:42:07.288454+00	2026-11-05 21:42:07.288454+00
\.


--
-- Data for Name: compromisos_cuponera; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.compromisos_cuponera (id, comercio_id, periodo_inicio, periodo_fin, fecha_limite_ratificacion, estado, fecha_ratificacion, created_at, updated_at) FROM stdin;
41	64	2026-11-16	2026-11-30	2026-11-19 03:00:00+00	PENDIENTE	\N	2026-11-10 21:57:52.206168+00	2026-11-10 21:57:52.206168+00
42	76	2026-11-16	2026-11-30	2026-11-19 03:00:00+00	PENDIENTE	\N	2026-11-10 22:05:00.019877+00	2026-11-10 22:05:00.019877+00
43	65	2026-11-16	2026-11-30	2026-11-19 03:00:00+00	PENDIENTE	\N	2026-11-10 22:05:00.025408+00	2026-11-10 22:05:00.025408+00
44	69	2026-11-16	2026-11-30	2026-11-19 03:00:00+00	PENDIENTE	\N	2026-11-10 22:05:00.030384+00	2026-11-10 22:05:00.030384+00
40	80	2026-11-04	2026-11-10	2026-11-07 03:00:00+00	VENCIDO	\N	2026-11-05 01:30:15.197476+00	2026-11-11 04:35:54.179452+00
45	80	2026-11-11	2026-11-25	2026-11-14 03:00:00+00	PENDIENTE	\N	2026-11-11 04:35:54.179452+00	2026-11-11 04:35:54.179452+00
46	81	2026-11-11	2026-11-25	2026-11-14 03:00:00+00	RATIFICADO	2026-11-11 04:41:53.680659+00	2026-11-11 04:39:37.20993+00	2026-11-11 04:41:53.680659+00
36	64	2026-11-01	2026-11-15	2026-11-04 03:00:00+00	VENCIDO	\N	2026-11-01 20:47:54.507531+00	2026-11-11 05:05:00.002628+00
47	82	2026-11-11	2026-11-25	2026-11-14 03:00:00+00	RATIFICADO	2026-11-11 05:37:02.354482+00	2026-11-11 04:58:55.094659+00	2026-11-11 05:37:02.354482+00
48	83	2026-11-11	2026-11-25	2026-11-14 03:00:00+00	RATIFICADO	2026-11-11 06:11:25.941514+00	2026-11-11 06:10:29.195915+00	2026-11-11 06:11:25.941514+00
49	84	2026-11-11	2026-11-25	2026-11-14 03:00:00+00	RATIFICADO	2026-11-11 06:39:39.660509+00	2026-11-11 06:38:32.376012+00	2026-11-11 06:39:39.660509+00
37	65	2026-11-01	2026-11-15	2026-11-04 03:00:00+00	RATIFICADO	2026-11-01 21:08:10.564326+00	2026-11-01 21:04:08.393107+00	2026-11-01 21:08:10.564326+00
38	69	2026-11-01	2026-11-15	2026-11-04 03:00:00+00	RATIFICADO	2026-11-01 21:30:10.012202+00	2026-11-01 21:28:53.477095+00	2026-11-01 21:30:10.012202+00
39	76	2026-11-01	2026-11-15	2026-11-04 03:00:00+00	RATIFICADO	2026-11-01 23:25:22.563792+00	2026-11-01 23:23:30.512955+00	2026-11-01 23:25:22.563792+00
\.


--
-- Data for Name: compromisos_sorteo; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.compromisos_sorteo (id, comercio_id, ciclo_inicio, ciclo_fin, fecha_limite_ratificacion, premio_descripcion, estado, fecha_ratificacion, created_at, updated_at, premio_nombre, premio_valor, estado_revision, motivo_rechazo, revisado_en, premio_foto_url) FROM stdin;
32	82	2026-11-11	2027-03-10	2026-11-14 03:00:00+00	completa	RATIFICADO	2026-11-11 05:26:25.040687+00	2026-11-11 05:02:23.108217+00	2026-11-11 05:26:25.040687+00	caja navideña	24000.00	PENDIENTE	\N	\N	/uploads/813c2f6c-d1dd-4c35-b103-4487a71a318f.jpg
27	76	2026-11-01	2027-02-28	2026-11-04 03:00:00+00	tabla de fiambres	RATIFICADO	2026-11-01 23:25:52.121208+00	2026-11-01 23:25:22.732696+00	2026-11-01 23:27:58.264254+00	tabla de embutidos	200.00	APROBADO	\N	2026-11-01 23:27:58.264254+00	/uploads/02928001-403d-424a-a7f3-d16fa413a291.jpg
26	69	2026-11-01	2027-02-28	2026-11-04 03:00:00+00	Frasco de vidrio rellenos de golosinas marca Arcor y felfort	RATIFICADO	2026-11-01 21:31:28.276951+00	2026-11-01 21:30:10.105374+00	2026-11-02 04:29:04.930839+00	Frasco de dulce surtidos	40000.00	APROBADO	\N	2026-11-02 04:29:04.930839+00	/uploads/ad00e0d1-5c5e-4d70-afdc-b165fc242b64.jpg
24	64	2026-11-01	2027-02-28	2026-11-04 03:00:00+00	Cajón de mix de verduras de temporada	RATIFICADO	2026-11-01 20:50:28.391723+00	2026-11-01 20:49:09.28744+00	2026-11-02 04:29:13.891143+00	Cajon de mix de verduras	25000.00	APROBADO	\N	2026-11-02 04:29:13.891143+00	/uploads/9998244e-eda9-40ce-a86e-cb35d13d2119.jpg
33	83	2026-11-11	2027-03-10	2026-11-14 03:00:00+00	b	RATIFICADO	2026-11-11 06:12:04.071481+00	2026-11-11 06:11:26.106089+00	2026-11-11 06:12:04.071481+00	a	32000.00	PENDIENTE	\N	\N	/uploads/50fc9650-b072-4bc0-b4ee-5250e08f7937.jpg
34	84	2026-11-11	2027-03-10	2026-11-14 03:00:00+00		PENDIENTE	\N	2026-11-11 06:39:39.793425+00	2026-11-11 06:39:39.793425+00	\N	\N	PENDIENTE	\N	\N	\N
28	80	2026-11-04	2027-03-03	2026-11-07 03:00:00+00	variedad	RATIFICADO	2026-11-05 01:32:17.722621+00	2026-11-05 01:31:33.859855+00	2026-11-05 01:34:43.911577+00	canasta de productos de limpieza	23000.00	APROBADO	\N	2026-11-05 01:34:43.911577+00	/uploads/c61d9dc5-267c-425c-ac21-63c5d84b206a.jpg
25	65	2026-11-01	2026-11-04	2026-11-04 03:00:00+00	Lapiceros Bic	VENCIDO	\N	2026-11-01 21:05:00.04781+00	2026-11-05 22:05:00.031594+00	lapicera	30000.00	RECHAZADO	precio descomunal	2026-11-05 21:30:01.340622+00	/uploads/ccc51dd5-542b-4484-bc13-6486c6696625.jpg
29	65	2026-11-05	2026-11-09	2026-11-08 03:00:00+00		VENCIDO	\N	2026-11-05 22:05:00.031594+00	2026-11-10 22:05:00.026872+00	\N	\N	PENDIENTE	\N	\N	\N
30	65	2026-11-10	2027-03-09	2026-11-13 03:00:00+00		PENDIENTE	\N	2026-11-10 22:05:00.026872+00	2026-11-10 22:05:00.026872+00	\N	\N	PENDIENTE	\N	\N	\N
31	81	2026-11-11	2027-03-10	2026-11-14 03:00:00+00	docena de facturas	RATIFICADO	2026-11-11 04:43:14.67403+00	2026-11-11 04:41:53.806215+00	2026-11-11 04:43:14.67403+00	Docena de facturas	12000.00	PENDIENTE	\N	\N	/uploads/b7426c61-b0f3-4bd9-a8a6-0f03f5ecf88b.jpg
\.


--
-- Data for Name: confirmaciones_canje; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.confirmaciones_canje (canje_id, cliente_id, recibido, puntos_otorgados, created_at) FROM stdin;
4	66	t	1	2026-11-02 02:31:43.702741+00
6	66	f	0	2026-11-02 02:39:29.577162+00
7	66	t	1	2026-11-02 03:01:49.720771+00
8	66	f	0	2026-11-02 03:03:37.614325+00
12	78	f	0	2026-11-02 07:15:35.914665+00
13	78	t	3	2026-11-02 07:18:04.13428+00
14	77	f	0	2026-11-05 01:26:32.620959+00
\.


--
-- Data for Name: cupones_cuponera; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.cupones_cuponera (id, compromiso_id, costo_puntos, stock_comprometido, stock_canjeado, created_at, updated_at, nombre_producto, descripcion, foto_url, valor_comercio, estado_revision, motivo_rechazo, revisado_en) FROM stdin;
34	40	14	5	0	2026-11-05 01:31:33.723261+00	2026-11-11 04:35:00.378011+00	Mopa	\N	/uploads/f0f6587f-5993-4a4f-97a0-3149e0748440.jpg	7000.00	RECHAZADO	precio desubicado	2026-11-11 04:35:00.378011+00
35	46	6	6	0	2026-11-11 04:41:53.680659+00	2026-11-11 04:41:53.680659+00	bolillos de panes	\N	/uploads/1ecfcad1-2866-4131-b4c5-9f3b9205e469.jpg	2600.00	PENDIENTE	\N	\N
36	46	11	5	0	2026-11-11 04:41:53.680659+00	2026-11-11 04:41:53.680659+00	bolsa con media docena de medialunas	\N	/uploads/53f6bdce-138e-4c6d-8210-6210fc35b95b.jpg	5400.00	PENDIENTE	\N	\N
25	36	20	3	0	2026-11-01 20:49:09.17674+00	2026-11-11 04:49:47.519909+00	Cajón de cebollas	\N	/uploads/0bd2bf9b-1275-4443-b3d0-9d4106efbe75.jpg	10000.00	RECHAZADO	No es un producto que valga la pena, no tiene variedad y el precio es inadecuado.	2026-11-11 04:49:47.519909+00
27	37	22	2	0	2026-11-01 21:08:10.564326+00	2026-11-01 21:08:10.564326+00	pack de resaltadores filgo	\N	/uploads/7f32658a-1a7b-4665-b1de-9e3a687dbfb2.jpg	11000.00	PENDIENTE	\N	\N
28	37	13	6	0	2026-11-01 21:08:10.564326+00	2026-11-01 21:08:10.564326+00	pack de hojas tamaño A5 de color	\N	/uploads/83b896de-e714-451f-853e-2fe7e0fd00cf.jpg	6300.00	PENDIENTE	\N	\N
29	38	46	5	0	2026-11-01 21:30:10.012202+00	2026-11-01 21:30:10.012202+00	bandeja de gomitas	\N	/uploads/214abb5a-d6e0-4f62-a9b1-b35d1dfd356f.jpg	23000.00	PENDIENTE	\N	\N
30	38	13	2	0	2026-11-01 21:30:10.012202+00	2026-11-01 21:30:10.012202+00	caja de pepero	\N	/uploads/2af3fb54-8a86-4dc2-a478-0b9002016a66.jpg	6300.00	PENDIENTE	\N	\N
40	47	52	5	0	2026-11-11 05:37:02.354482+00	2026-11-11 05:37:02.354482+00	a	\N	/uploads/b6a74fdc-353b-4c51-97fe-a83f9c50eaba.jpg	26000.00	PENDIENTE	\N	\N
41	47	5	5	0	2026-11-11 05:37:02.354482+00	2026-11-11 05:37:02.354482+00	b	\N	/uploads/e292ff3d-4701-4728-8f2a-f50e9b4d5396.jpg	2380.00	PENDIENTE	\N	\N
42	48	9	5	0	2026-11-11 06:11:25.941514+00	2026-11-11 06:11:25.941514+00	librero	\N	/uploads/36c40d96-85a8-418b-86d5-bec0167097d4.jpg	4300.00	PENDIENTE	\N	\N
43	48	4	5	0	2026-11-11 06:11:25.941514+00	2026-11-11 06:11:25.941514+00	cubo	\N	/uploads/5b453cee-4ac8-4953-af7b-b85c68874285.jpg	2000.00	PENDIENTE	\N	\N
44	49	4	5	0	2026-11-11 06:39:39.660509+00	2026-11-11 06:39:39.660509+00	perro	\N	/uploads/f5a1979f-1adf-44f6-81ce-bd03ad9bf90e.jpg	3894.00	PENDIENTE	\N	\N
31	39	1	5	4	2026-11-01 23:25:22.563792+00	2026-11-02 03:02:55.079889+00	Tira de chorizo	\N	/uploads/ce2818c3-a59d-4d8b-9e5a-b842b8d32cad.jpg	300.00	APROBADO	\N	2026-11-01 23:28:04.030987+00
32	39	3	5	1	2026-11-01 23:25:22.563792+00	2026-11-02 07:14:40.15624+00	pechuga de pollo	\N	/uploads/a7f52650-4063-4a8e-bbd9-b1dda43c5902.jpg	1300.00	APROBADO	\N	2026-11-01 23:28:00.654397+00
26	37	9	3	1	2026-11-01 21:08:10.564326+00	2026-11-02 07:17:32.109526+00	Señalizadores	\N	/uploads/dc8a86f1-0303-4605-ba84-c701655c4f1f.jpg	4200.00	APROBADO	\N	2026-11-01 23:05:42.505428+00
24	36	7	5	1	2026-11-01 20:49:09.17674+00	2026-11-05 01:25:20.076361+00	Racimo de bananas	\N	/uploads/5d985ed2-24c9-47f6-96b0-e4088e2a0655.jpg	3500.00	APROBADO	\N	2026-11-01 23:05:21.342197+00
33	40	7	5	0	2026-11-05 01:31:33.723261+00	2026-11-05 21:30:18.56482+00	guante y esponja	\N	/uploads/44c5d1f0-f88e-4f2f-8787-f0fca3339fe5.jpg	3500.00	APROBADO	\N	2026-11-05 21:30:18.56482+00
45	49	3	5	0	2026-11-11 06:39:39.660509+00	2026-11-11 06:39:39.660509+00	logo	\N	/uploads/00676797-d8d5-4405-a444-84c3581b1795.jpg	3000.00	PENDIENTE	\N	\N
\.


--
-- Data for Name: favoritos; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.favoritos (id, cliente_id, oferta_id, created_at) FROM stdin;
7	66	27	2026-11-01 21:15:54.023131+00
8	70	32	2026-11-01 21:57:23.343716+00
9	78	27	2026-11-05 21:35:48.336412+00
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	init	SQL	V1__init.sql	-563397387	barrioAhorro	2026-09-27 02:03:48.512943	60	t
2	2	add business location and schedule	SQL	V2__add_business_location_and_schedule.sql	-925816915	barrioAhorro	2026-09-27 17:22:19.875654	18	t
3	3	change dia semana to integer	SQL	V3__change_dia_semana_to_integer.sql	-186415888	barrioAhorro	2026-09-27 17:32:14.486918	7	t
4	4	unificar horarios atencion	SQL	V4__unificar_horarios_atencion.sql	1901663792	barrioAhorro	2026-10-13 15:38:39.49914	43	t
5	5	cuponera productos concretos	SQL	V5__cuponera_productos_concretos.sql	1578043109	barrioAhorro	2026-10-13 15:42:48.894662	9	t
6	6	periodos ratificaciones y acceso	SQL	V6__periodos_ratificaciones_y_acceso.sql	-1538970688	barrioAhorro	2026-10-13 15:42:48.921578	8	t
7	7	cuponera precio	SQL	V7__cuponera_precio.sql	817286460	barrioAhorro	2026-10-13 15:47:38.175858	18	t
8	8	renovacion periodos y canjes	SQL	V8__renovacion_periodos_y_canjes.sql	-125137649	barrioAhorro	2026-10-28 01:09:31.604768	13	t
9	9	sorteo producto	SQL	V9__sorteo_producto.sql	1966697535	barrioAhorro	2026-10-28 01:09:31.630798	8	t
10	10	foro direccion observada	SQL	V10__foro_direccion_observada.sql	-684906912	barrioAhorro	2026-10-28 01:09:31.644263	3	t
11	11	admin y revision	SQL	V11__admin_y_revision.sql	-114879555	barrioAhorro	2026-10-28 01:09:31.651222	9	t
12	12	rechazo vuelve a pendiente	SQL	V12__rechazo_vuelve_a_pendiente.sql	1390091262	barrioAhorro	2026-10-28 01:09:31.673627	9	t
13	13	sorteo foto	SQL	V13__sorteo_foto.sql	379646873	barrioAhorro	2026-10-28 02:29:37.86795	4	t
14	14	confirmacion recepcion canje	SQL	V14__confirmacion_recepcion_canje.sql	207247384	barrioAhorro	2026-11-01 23:26:55.73452	37	t
15	15	sorteos vecinos	SQL	V15__sorteos_vecinos.sql	-923579428	barrioAhorro	2026-11-03 23:18:19.238487	36	t
16	16	premios sorteo	SQL	V16__premios_sorteo.sql	1753271452	barrioAhorro	2026-11-04 22:10:44.075059	19	t
\.


--
-- Data for Name: foro_comentario_upvotes; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.foro_comentario_upvotes (id, comentario_id, usuario_id, created_at) FROM stdin;
1	2	67	2026-11-05 19:29:02.097382+00
\.


--
-- Data for Name: foro_comentarios; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.foro_comentarios (id, publicacion_id, comentario_padre_id, autor_id, texto, eliminado_en, created_at, updated_at) FROM stdin;
1	1	\N	67	Re, preciosa super inflados, te conviene caminar hasta rivadavia y ver alguna librería por allá.	\N	2026-11-01 21:22:04.032967+00	2026-11-01 21:22:04.032972+00
2	1	\N	68	Yo compré una sola vez porque era el único lugar abierto, tampoco recomiendo	\N	2026-11-01 21:23:59.315333+00	2026-11-01 21:23:59.315338+00
\.


--
-- Data for Name: foro_publicacion_upvotes; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.foro_publicacion_upvotes (id, publicacion_id, usuario_id, created_at) FROM stdin;
2	1	68	2026-11-01 21:24:00.782934+00
3	1	67	2026-11-01 21:36:13.607891+00
4	2	70	2026-11-01 21:57:58.875528+00
6	1	70	2026-11-01 23:09:52.753187+00
9	1	78	2026-11-05 21:38:09.905198+00
10	3	78	2026-11-05 21:38:46.919368+00
\.


--
-- Data for Name: foro_publicaciones; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.foro_publicaciones (id, autor_id, comercio_id, texto, precio_observado, ubicacion_observada, eliminado_en, created_at, updated_at, direccion_observada) FROM stdin;
1	66	65	Siento que es demasiado caro, y la mayoría de sus productos no valen la pena, son las mismas opciones de siempre y en cualquiera negocio las encontras a mitad de precio. Pasé y una microfibras me parecieron muy caras, alguien mas conmigo?	\N	0101000020E6100000BEF90D130D3B4DC03A483E65EB5141C0	\N	2026-11-01 21:20:29.570769+00	2026-11-01 21:20:29.570775+00	\N
2	67	69	El local mas completo que vi en mi vida, esta repleto y es enorme	\N	0101000020E610000078B56AA627324DC0DC8DC47EA44D41C0	\N	2026-11-01 21:36:04.961476+00	2026-11-01 21:36:04.961477+00	\N
3	70	69	Genial si vas a comprar por mayor	\N	0101000020E61000009E0ED0948D3A4DC058D05DF9044D41C0	\N	2026-11-01 21:57:52.922103+00	2026-11-01 21:57:52.922114+00	\N
4	78	69	Lindo	\N	0101000020E6100000351603DA0C3B4DC04C570B47EB5141C0	\N	2026-11-11 03:50:28.562546+00	2026-11-11 03:50:28.562558+00	\N
\.


--
-- Data for Name: fotos_comercio; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.fotos_comercio (id, comercio_id, url, orden, created_at) FROM stdin;
5	64	/uploads/630ea9f1-821b-43e2-b6c9-8de577dcdd99.jpg	0	2026-11-01 20:51:02.335098+00
6	65	/uploads/03137ebb-ae20-44a5-93fd-82efa09742ea.jpg	0	2026-11-01 21:12:52.679069+00
7	69	/uploads/275f0448-feac-4ff9-a73f-e47386f728f4.jpg	0	2026-11-01 21:33:37.489438+00
8	76	/uploads/4cfe9831-391b-437e-95ba-89e265a969bf.jpg	0	2026-11-01 23:26:01.480151+00
9	81	/uploads/d89623a6-9a17-4f2e-a7e8-3e2d720f885f.jpg	0	2026-11-11 04:43:46.582947+00
10	82	/uploads/cabe1298-99cf-4196-831b-1aa958e082df.jpg	0	2026-11-11 05:04:29.597177+00
\.


--
-- Data for Name: horarios_atencion; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.horarios_atencion (id, comercio_id, dia_semana, hora_inicio, hora_fin, activo, created_at, updated_at) FROM stdin;
194	76	1	09:00:00	19:00:00	t	2026-11-01 23:23:30.060345+00	2026-11-01 23:23:30.060345+00
195	76	2	09:00:00	19:00:00	t	2026-11-01 23:23:30.060345+00	2026-11-01 23:23:30.060345+00
196	76	3	09:00:00	19:00:00	t	2026-11-01 23:23:30.060345+00	2026-11-01 23:23:30.060345+00
197	76	4	09:00:00	19:00:00	t	2026-11-01 23:23:30.060345+00	2026-11-01 23:23:30.060345+00
198	76	5	09:00:00	19:00:00	t	2026-11-01 23:23:30.060345+00	2026-11-01 23:23:30.060345+00
199	80	1	09:00:00	19:00:00	t	2026-11-05 01:30:15.108114+00	2026-11-05 01:30:15.108114+00
200	80	2	09:00:00	19:00:00	t	2026-11-05 01:30:15.108114+00	2026-11-05 01:30:15.108114+00
201	80	3	09:00:00	19:00:00	t	2026-11-05 01:30:15.108114+00	2026-11-05 01:30:15.108114+00
202	80	4	09:00:00	19:00:00	t	2026-11-05 01:30:15.108114+00	2026-11-05 01:30:15.108114+00
203	80	5	09:00:00	19:00:00	t	2026-11-05 01:30:15.108114+00	2026-11-05 01:30:15.108114+00
204	81	1	09:00:00	19:00:00	t	2026-11-11 04:39:37.136531+00	2026-11-11 04:39:37.136531+00
205	81	2	09:00:00	19:00:00	t	2026-11-11 04:39:37.136531+00	2026-11-11 04:39:37.136531+00
206	81	3	09:00:00	19:00:00	t	2026-11-11 04:39:37.136531+00	2026-11-11 04:39:37.136531+00
207	81	4	09:00:00	19:00:00	t	2026-11-11 04:39:37.136531+00	2026-11-11 04:39:37.136531+00
208	81	5	09:00:00	19:00:00	t	2026-11-11 04:39:37.136531+00	2026-11-11 04:39:37.136531+00
209	82	1	09:00:00	19:00:00	t	2026-11-11 04:58:54.64208+00	2026-11-11 04:58:54.64208+00
210	82	2	09:00:00	19:00:00	t	2026-11-11 04:58:54.64208+00	2026-11-11 04:58:54.64208+00
211	82	3	09:00:00	19:00:00	t	2026-11-11 04:58:54.64208+00	2026-11-11 04:58:54.64208+00
212	82	4	09:00:00	19:00:00	t	2026-11-11 04:58:54.64208+00	2026-11-11 04:58:54.64208+00
213	82	5	09:00:00	19:00:00	t	2026-11-11 04:58:54.64208+00	2026-11-11 04:58:54.64208+00
214	83	1	09:00:00	19:00:00	t	2026-11-11 06:10:28.621482+00	2026-11-11 06:10:28.621482+00
215	83	2	09:00:00	19:00:00	t	2026-11-11 06:10:28.621482+00	2026-11-11 06:10:28.621482+00
216	83	3	09:00:00	19:00:00	t	2026-11-11 06:10:28.621482+00	2026-11-11 06:10:28.621482+00
217	83	4	09:00:00	19:00:00	t	2026-11-11 06:10:28.621482+00	2026-11-11 06:10:28.621482+00
218	83	5	09:00:00	19:00:00	t	2026-11-11 06:10:28.621482+00	2026-11-11 06:10:28.621482+00
219	84	1	09:00:00	19:00:00	t	2026-11-11 06:38:32.282899+00	2026-11-11 06:38:32.282899+00
220	84	4	09:00:00	19:00:00	t	2026-11-11 06:38:32.282899+00	2026-11-11 06:38:32.282899+00
221	84	5	09:00:00	19:00:00	t	2026-11-11 06:38:32.282899+00	2026-11-11 06:38:32.282899+00
174	64	1	09:00:00	15:30:00	t	2026-11-01 20:47:54.04509+00	2026-11-01 20:47:54.04509+00
175	64	1	18:00:00	20:00:00	t	2026-11-01 20:47:54.04509+00	2026-11-01 20:47:54.04509+00
176	64	2	09:00:00	15:30:00	t	2026-11-01 20:47:54.04509+00	2026-11-01 20:47:54.04509+00
177	64	2	18:00:00	20:00:00	t	2026-11-01 20:47:54.04509+00	2026-11-01 20:47:54.04509+00
178	64	3	09:00:00	15:30:00	t	2026-11-01 20:47:54.04509+00	2026-11-01 20:47:54.04509+00
179	64	3	18:00:00	20:00:00	t	2026-11-01 20:47:54.04509+00	2026-11-01 20:47:54.04509+00
180	64	4	09:00:00	15:30:00	t	2026-11-01 20:47:54.04509+00	2026-11-01 20:47:54.04509+00
181	64	4	18:00:00	20:00:00	t	2026-11-01 20:47:54.04509+00	2026-11-01 20:47:54.04509+00
182	64	5	09:00:00	15:30:00	t	2026-11-01 20:47:54.04509+00	2026-11-01 20:47:54.04509+00
183	64	5	18:00:00	20:00:00	t	2026-11-01 20:47:54.04509+00	2026-11-01 20:47:54.04509+00
184	64	6	09:00:00	15:30:00	t	2026-11-01 20:47:54.04509+00	2026-11-01 20:47:54.04509+00
185	65	1	09:00:00	19:00:00	t	2026-11-01 21:04:08.325525+00	2026-11-01 21:04:08.325525+00
186	65	2	09:00:00	19:00:00	t	2026-11-01 21:04:08.325525+00	2026-11-01 21:04:08.325525+00
187	65	3	09:00:00	19:00:00	t	2026-11-01 21:04:08.325525+00	2026-11-01 21:04:08.325525+00
188	65	4	09:00:00	19:00:00	t	2026-11-01 21:04:08.325525+00	2026-11-01 21:04:08.325525+00
189	69	1	09:00:00	19:00:00	t	2026-11-01 21:28:53.402435+00	2026-11-01 21:28:53.402435+00
190	69	2	09:00:00	19:00:00	t	2026-11-01 21:28:53.402435+00	2026-11-01 21:28:53.402435+00
191	69	3	09:00:00	19:00:00	t	2026-11-01 21:28:53.402435+00	2026-11-01 21:28:53.402435+00
192	69	4	09:00:00	19:00:00	t	2026-11-01 21:28:53.402435+00	2026-11-01 21:28:53.402435+00
193	69	5	09:00:00	19:00:00	t	2026-11-01 21:28:53.402435+00	2026-11-01 21:28:53.402435+00
\.


--
-- Data for Name: movimientos_puntos; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.movimientos_puntos (id, cliente_id, tipo, puntos, referencia_tabla, referencia_id, descripcion, created_at) FROM stdin;
1	66	UPVOTE_RECIBIDO	1	foro_publicacion_upvotes	1	Upvote recibido	2026-11-01 21:21:32.43885+00
2	66	UPVOTE_RECIBIDO	1	foro_publicacion_upvotes	2	Upvote recibido	2026-11-01 21:24:00.782934+00
3	66	UPVOTE_RECIBIDO	-1	foro_publicacion_upvotes	1	Upvote retirado	2026-11-01 21:36:13.004619+00
4	66	UPVOTE_RECIBIDO	1	foro_publicacion_upvotes	3	Upvote recibido	2026-11-01 21:36:13.607891+00
5	67	UPVOTE_RECIBIDO	1	foro_publicacion_upvotes	4	Upvote recibido	2026-11-01 21:57:58.875528+00
6	66	UPVOTE_RECIBIDO	1	foro_publicacion_upvotes	5	Upvote recibido	2026-11-01 23:06:09.922391+00
7	66	UPVOTE_RECIBIDO	-1	foro_publicacion_upvotes	5	Upvote retirado	2026-11-01 23:09:52.395179+00
8	66	UPVOTE_RECIBIDO	1	foro_publicacion_upvotes	6	Upvote recibido	2026-11-01 23:09:52.753187+00
9	70	UPVOTE_RECIBIDO	1	foro_publicacion_upvotes	7	Upvote recibido	2026-11-01 23:10:33.652026+00
10	70	UPVOTE_RECIBIDO	-1	foro_publicacion_upvotes	7	Upvote retirado	2026-11-01 23:10:34.211372+00
11	67	UPVOTE_RECIBIDO	1	foro_publicacion_upvotes	8	Upvote recibido	2026-11-01 23:10:34.793507+00
12	67	UPVOTE_RECIBIDO	-1	foro_publicacion_upvotes	8	Upvote retirado	2026-11-01 23:10:35.136317+00
13	66	CANJE_CUPON	-1	canjes_cupon	4	Canje de premio validado por el comercio	2026-11-02 02:29:44.902272+00
14	66	CANJE_CUPON	-1	canjes_cupon	6	Canje de premio validado por el comercio	2026-11-02 02:37:31.625189+00
15	66	CANJE_CUPON	-1	canjes_cupon	7	Canje de premio validado por el comercio	2026-11-02 03:00:21.611148+00
16	66	CANJE_CUPON	-1	canjes_cupon	8	Canje de premio validado por el comercio	2026-11-02 03:02:55.079889+00
17	77	AJUSTE_ADMIN	100	usuarios	77	Carga inicial de puntos para cuenta de prueba	2026-11-02 04:22:52.629876+00
18	78	AJUSTE_ADMIN	200	usuarios	78	Carga inicial de puntos para cuenta de prueba	2026-11-02 04:22:52.629876+00
19	79	AJUSTE_ADMIN	150	usuarios	79	Carga inicial de puntos para cuenta de prueba	2026-11-02 04:22:52.629876+00
20	78	CANJE_CUPON	-3	canjes_cupon	12	Canje de premio validado por el comercio	2026-11-02 07:14:40.15624+00
21	78	CANJE_CUPON	-9	canjes_cupon	13	Canje de premio validado por el comercio	2026-11-02 07:17:32.109526+00
22	77	COMPRA_TICKET_SORTEO	-30	tickets_sorteo	1	Compra de ticket para sorteo	2026-11-04 05:18:14.023973+00
23	77	COMPRA_TICKET_SORTEO	-30	tickets_sorteo	2	Compra de ticket para sorteo	2026-11-04 05:18:14.023973+00
24	78	COMPRA_TICKET_SORTEO	-30	tickets_sorteo	3	Compra de ticket para sorteo	2026-11-04 05:33:54.024078+00
25	79	COMPRA_TICKET_SORTEO	-30	tickets_sorteo	4	Compra de ticket para sorteo	2026-11-04 05:34:20.859036+00
26	77	CANJE_CUPON	-7	canjes_cupon	14	Canje de premio validado por el comercio	2026-11-05 01:25:20.076361+00
27	68	UPVOTE_RECIBIDO	1	foro_comentario_upvotes	1	Upvote recibido	2026-11-05 19:29:02.097382+00
28	78	COMPRA_TICKET_SORTEO	-23	tickets_sorteo	5	Compra de ticket para sorteo	2026-11-05 21:04:37.52426+00
29	78	COMPRA_TICKET_SORTEO	-23	tickets_sorteo	6	Compra de ticket para sorteo	2026-11-05 21:04:37.52426+00
30	77	COMPRA_TICKET_SORTEO	-23	tickets_sorteo	7	Compra de ticket para sorteo	2026-11-05 21:05:13.28729+00
31	66	UPVOTE_RECIBIDO	1	foro_publicacion_upvotes	9	Upvote recibido	2026-11-05 21:38:09.905198+00
32	70	UPVOTE_RECIBIDO	1	foro_publicacion_upvotes	10	Upvote recibido	2026-11-05 21:38:46.919368+00
\.


--
-- Data for Name: ofertas; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.ofertas (id, comercio_id, tipo_oferta, nombre_producto, foto_url, vigencia_tipo, vigente_hasta, activa, created_at, updated_at) FROM stdin;
27	64	PORCENTAJE	Frutilla × 1kg	/uploads/d24d42be-ed70-4d07-b92e-d9aa2ed04286.jpg	FECHA_ESPECIFICA	2026-11-06 01:00:00+00	t	2026-11-01 20:58:37.70607+00	2026-11-01 20:58:37.706072+00
29	65	PORCENTAJE	Cartuchera completa	/uploads/61f4c568-f2b5-4016-ac48-89481d95f6fd.jpg	TODA_LA_SEMANA	2026-11-08 21:11:48.201463+00	t	2026-11-01 21:11:48.20147+00	2026-11-01 21:11:48.201471+00
30	65	DOS_POR_UNO	cuadernos A3	/uploads/2ef9bc6d-a5bf-4832-af12-7e96a480fd7a.jpg	HASTA_AGOTAR_STOCK	\N	t	2026-11-01 21:12:23.288302+00	2026-11-01 21:12:23.288306+00
31	69	DOS_POR_UNO	Caja de rogletons	/uploads/f89be3b9-0e7d-4ca3-adc3-ea4945e8220d.jpg	HASTA_AGOTAR_STOCK	\N	t	2026-11-01 21:32:04.546718+00	2026-11-01 21:32:04.54672+00
32	69	PORCENTAJE	chocolate	/uploads/81ced012-ac86-44b2-a3f3-751e55db5abb.jpg	SOLO_HOY	2026-11-02 03:00:00+00	t	2026-11-01 21:33:21.153656+00	2026-11-01 21:33:21.153659+00
33	80	POR_MAYOR	Detergente, botella de 1L	/uploads/1a2bb084-43fd-4258-8718-548746f91585.jpg	TODA_LA_SEMANA	2026-11-12 01:33:33.399511+00	t	2026-11-05 01:33:33.399678+00	2026-11-05 01:33:33.399685+00
26	64	CANTIDAD_ULTIMA_UNIDAD	Papá × 3kg	/uploads/3c457bd5-e5db-4ec6-9ed1-93174cc6ee40.jpg	SOLO_HOY	2026-11-06 03:00:00+00	t	2026-11-01 20:54:53.979045+00	2026-11-05 21:11:49.409252+00
25	64	DOS_POR_UNO	Bandeja de verduras cortadas	/uploads/7a55198b-9f6a-4c59-932b-8d84e6210018.jpg	FECHA_ESPECIFICA	2026-11-12 23:00:00+00	f	2026-11-01 20:52:19.679213+00	2026-11-10 23:28:28.549444+00
28	64	PORCENTAJE	Sandia 1kg	/uploads/0ac6c3e8-ad77-46ea-97bd-696ced2f05ec.jpg	FECHA_ESPECIFICA	2026-11-18 23:00:00+00	f	2026-11-01 21:00:11.079705+00	2026-11-11 00:04:51.433805+00
34	83	PORCENTAJE	galletitas	/uploads/228ac7af-d4cd-48e4-9004-d03568b997f6.jpg	HASTA_AGOTAR_STOCK	\N	t	2026-11-11 06:35:16.155162+00	2026-11-11 06:35:16.15517+00
35	69	CANTIDAD_ULTIMA_UNIDAD	paragüitas	/uploads/e3b0c95d-8992-4c46-8135-99f20c1ee61d.jpg	HASTA_AGOTAR_STOCK	\N	t	2026-11-12 00:00:49.277657+00	2026-11-12 00:00:49.277662+00
36	69	DOS_POR_UNO	jugo baggio	/uploads/82c8d38a-0099-4755-a45d-a8aac08dd0ed.jpg	HASTA_AGOTAR_STOCK	\N	t	2026-11-12 00:01:16.132464+00	2026-11-12 00:01:16.132466+00
37	69	PORCENTAJE	chupetines	/uploads/befbca29-84e1-47f5-8ee7-db1160eecbd8.jpg	HASTA_AGOTAR_STOCK	\N	t	2026-11-12 00:02:03.457435+00	2026-11-12 00:02:03.457437+00
38	69	CANTIDAD_ULTIMA_UNIDAD	palitos de la selva x 2kg	/uploads/3784c62a-b6a3-4e5c-b86f-3fb7e40299d9.jpg	HASTA_AGOTAR_STOCK	\N	t	2026-11-12 00:03:02.954872+00	2026-11-12 00:03:02.954877+00
39	69	CANTIDAD_ULTIMA_UNIDAD	monedas de chocolates x 2kg	/uploads/e8890e56-ea67-4de2-b322-94bce15dd6c9.jpg	HASTA_AGOTAR_STOCK	\N	t	2026-11-12 00:04:04.362034+00	2026-11-12 00:04:04.362038+00
\.


--
-- Data for Name: ofertas_2x1; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.ofertas_2x1 (oferta_id, unidades_a_pagar, unidades_a_llevar, precio_unitario) FROM stdin;
25	1	2	2300.00
30	4	5	1200.00
31	1	2	16000.00
36	1	2	1700.00
\.


--
-- Data for Name: ofertas_cantidad; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.ofertas_cantidad (oferta_id, cantidad_requerida, precio_unitario, porcentaje_descuento_ultima_unidad) FROM stdin;
26	3	4100.00	70
35	7	1200.00	15
38	2	2200.00	40
39	2	3600.00	60
\.


--
-- Data for Name: ofertas_por_mayor; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.ofertas_por_mayor (oferta_id, cantidad_minima, precio_unitario_mayorista, precio_unitario_regular) FROM stdin;
33	4	2700.00	3000.00
\.


--
-- Data for Name: ofertas_porcentaje; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.ofertas_porcentaje (oferta_id, precio_original, porcentaje_descuento) FROM stdin;
27	8000.00	50
28	5000.00	25
29	40000.00	15
32	13000.00	15
34	6000.00	15
37	1300.00	12
\.


--
-- Data for Name: perfiles_cliente; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.perfiles_cliente (usuario_id, saldo_puntos, puntos_totales_historicos, created_at, updated_at, sorteos_desbloqueo_visto_en) FROM stdin;
67	1	2	2026-11-01 21:20:54.547461+00	2026-11-01 23:10:35.136317+00	\N
79	120	150	2026-11-02 04:22:52.629876+00	2026-11-04 05:34:20.859036+00	2026-11-04 02:20:22.327381+00
68	1	1	2026-11-01 21:22:48.921394+00	2026-11-05 19:29:02.097382+00	\N
78	115	203	2026-11-02 04:22:52.629876+00	2026-11-05 21:04:37.52426+00	2026-11-04 05:33:39.580346+00
77	10	100	2026-11-02 04:22:52.629876+00	2026-11-05 21:05:13.28729+00	2026-11-04 02:22:27.144904+00
66	2	8	2026-11-01 21:13:27.948256+00	2026-11-05 21:38:09.905198+00	\N
70	1	2	2026-11-01 21:56:33.406693+00	2026-11-05 21:38:46.919368+00	\N
\.


--
-- Data for Name: perfiles_comercio; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.perfiles_comercio (usuario_id, nombre_negocio, rubro_id, descripcion, direccion, ubicacion, onboarding_completado, created_at, updated_at) FROM stdin;
76	Turri	2	Buenos cortes	Avenida Rivadavia 6879	0101000020E61000000000006E2A3B4DC0383B8388765041C0	t	2026-11-01 23:21:09.68233+00	2026-11-01 23:23:30.060345+00
80	Limpieza	6	Buena variedad	Laguna 1412	0101000020E61000000000001E1F3C4DC0B67616BFDB5241C0	t	2026-11-05 01:27:56.057926+00	2026-11-05 01:30:15.108114+00
81	Prueba	4	Es el testo de los sorteos, envío de notificaciones y asi	Rivera Indarte 591	0101000020E6100000000000F6C03A4DC0DC7771986F5141C0	t	2026-11-11 04:38:54.4684+00	2026-11-11 04:39:37.136531+00
82	Almacen	1	Almacen con gran variedad y super limpio.	Moreto 1181	0101000020E610000000000004BF3D4DC05A624967FA5241C0	t	2026-11-11 04:57:53.980209+00	2026-11-11 04:58:54.64208+00
83	Prueba	7	Prueba de onboarding	Calle Ana Maria Janer 3487	0101000020E6100000BA6FDA56B8394DC0B08695DE845441C0	t	2026-11-11 05:45:59.512686+00	2026-11-11 06:10:28.621482+00
84	Prueba	8	Viendo si los puntos tienen consistencia	José Martí 627	0101000020E6100000000000A60F3C4DC098A180F9C85141C0	t	2026-11-11 06:37:39.621142+00	2026-11-11 06:38:32.282899+00
64	San Marino	3	Verdulera de calidad. Frescura y variedad garantizados.	Baldomero Fernández Moreno 2900	0101000020E6100000705CC64D0D3B4DC046240A2DEB5141C0	t	2026-11-01 20:45:37.937262+00	2026-11-01 20:47:54.04509+00
65	La Terminal	8	Papeleria de primer nivel	Francisco Bilbao 2694	0101000020E6100000000000E42C3B4DC0963827138B5141C0	t	2026-11-01 21:03:19.234453+00	2026-11-01 21:04:08.325525+00
69	Lucky	8	Golosineria premium	Guatemala 4815	0101000020E6100000149CE16EFB364DC0CE0094D6464D41C0	t	2026-11-01 21:27:25.177875+00	2026-11-01 21:28:53.402435+00
\.


--
-- Data for Name: premios_sorteo; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.premios_sorteo (ticket_id, sorteo_id, cliente_id, token, vence_en, visto_en, canjeado_en, canjeado_por, created_at) FROM stdin;
2	3	77	b9e9ffd0-ca1d-4027-a0c9-6f1b26ef6726	2026-12-05 01:10:44.084201+00	2026-11-05 01:12:53.533766+00	2026-11-05 01:15:54.484086+00	76	2026-11-05 01:10:44.084201+00
7	4	77	eb0fb0b9-10aa-4ed5-be43-a422e9afd144	2026-12-05 21:10:00.012953+00	2026-11-05 21:47:05.123902+00	\N	\N	2026-11-05 21:10:00.012953+00
\.


--
-- Data for Name: reportes_canje; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.reportes_canje (id, canje_id, cliente_id, motivo, descripcion, estado, created_at) FROM stdin;
1	6	66	OTRO	No me dejaron retirar a menos que realizara otra compra	ABIERTO	2026-11-02 02:39:29.577162+00
2	8	66	OTRO	podrido	ABIERTO	2026-11-02 03:03:37.614325+00
3	12	78	CALIDAD	mal estado de la carne	ABIERTO	2026-11-02 07:15:35.914665+00
4	14	77	PEDIDO_INCOMPLETO	Era una sola banana	ABIERTO	2026-11-05 01:26:32.620959+00
\.


--
-- Data for Name: resenas_comercio; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.resenas_comercio (id, comercio_id, cliente_id, texto, estrellas, created_at, updated_at) FROM stdin;
3	64	66	Muy buena atención y gran variedad, podrían mejorar agregando personal, se llena bastante y uno demora mucho.	4	2026-11-01 21:17:11.617427+00	2026-11-01 21:17:21.191253+00
\.


--
-- Data for Name: rubros; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.rubros (id, nombre) FROM stdin;
1	Almacén
2	Carnicería
3	Verdulería
4	Panadería
5	Farmacia
6	Limpieza
7	Kiosco
8	Otro rubro
\.


--
-- Data for Name: sorteos; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.sorteos (id, compromiso_sorteo_id, costo_puntos_ticket, estado, fecha_sorteo, ticket_ganador_id, created_at, updated_at, ejecutado_en, total_tickets) FROM stdin;
1	24	30	ACTIVO	2026-11-30 23:00:00+00	\N	2026-11-04 03:10:00.003678+00	2026-11-04 03:10:00.003678+00	\N	\N
2	26	30	ACTIVO	2026-11-30 23:00:00+00	\N	2026-11-04 03:10:00.003678+00	2026-11-04 03:10:00.003678+00	\N	\N
3	27	30	SORTEADO	2026-11-04 05:44:21.278914+00	2	2026-11-04 03:10:00.003678+00	2026-11-04 05:53:00.032103+00	2026-11-04 05:53:00.032103+00	4
4	28	23	SORTEADO	2026-11-05 21:10:00+00	7	2026-11-05 01:35:00.001493+00	2026-11-05 21:10:00.012953+00	2026-11-05 21:10:00.012953+00	3
\.


--
-- Data for Name: spatial_ref_sys; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.spatial_ref_sys (srid, auth_name, auth_srid, srtext, proj4text) FROM stdin;
\.


--
-- Data for Name: tickets_sorteo; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tickets_sorteo (id, sorteo_id, cliente_id, puntos_costo, created_at, estado) FROM stdin;
1	3	77	30	2026-11-04 05:18:14.023973+00	NO_GANADOR
2	3	77	30	2026-11-04 05:18:14.023973+00	GANADOR
3	3	78	30	2026-11-04 05:33:54.024078+00	NO_GANADOR
4	3	79	30	2026-11-04 05:34:20.859036+00	NO_GANADOR
5	4	78	23	2026-11-05 21:04:37.52426+00	NO_GANADOR
6	4	78	23	2026-11-05 21:04:37.52426+00	NO_GANADOR
7	4	77	23	2026-11-05 21:05:13.28729+00	GANADOR
\.


--
-- Data for Name: usuarios; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.usuarios (id, email, password_hash, tipo_usuario, email_verificado, created_at, updated_at) FROM stdin;
77	sorteo1@gmail.com	$2a$10$QdRTQpmwtkEAXwMgPTSlEuZWJRVqJfY1m4IiyyDW6kY9eBkQ7iKgy	CLIENTE	t	2026-11-02 04:22:52.629876+00	2026-11-02 04:22:52.629876+00
78	sorteo2@gmail.com	$2a$10$QdRTQpmwtkEAXwMgPTSlEuZWJRVqJfY1m4IiyyDW6kY9eBkQ7iKgy	CLIENTE	t	2026-11-02 04:22:52.629876+00	2026-11-02 04:22:52.629876+00
79	sorteo3@gmail.com	$2a$10$QdRTQpmwtkEAXwMgPTSlEuZWJRVqJfY1m4IiyyDW6kY9eBkQ7iKgy	CLIENTE	t	2026-11-02 04:22:52.629876+00	2026-11-02 04:22:52.629876+00
80	sorteo4@gmail.com	$2a$10$JBjTY7me4OdLfEjZZYhDsOM/qlnAxy9L8DLZSacFa0UDcGcCg10xe	COMERCIO	f	2026-11-05 01:27:56.030717+00	2026-11-05 01:27:56.030728+00
81	prueba1@gmail.com	$2a$10$aZCKvUSuR12svrAx9Jcjc.CjNsCMf2lm1soYBmXKXIKu6U3rEdBai	COMERCIO	f	2026-11-11 04:38:54.439912+00	2026-11-11 04:38:54.439924+00
83	onboarding@gmail.com	$2a$10$QMhn/DuZaXOb/R1aKJWrs.iK.wvNrwwbcASe27/Vvzc9/EVlGsWkm	COMERCIO	f	2026-11-11 05:45:59.509391+00	2026-11-11 05:45:59.509406+00
82	almacenpepe57@gmail.com	$2a$10$2vT84mwnYwmDQ/OIavsO7eM3X0dgyV1zdbgF416Q.RlZxtfBZToYG	COMERCIO	f	2026-11-11 04:57:53.941517+00	2026-11-11 04:57:53.941548+00
59	moderador@gmail.com	$2a$10$pkFxC..gIfMLpF/UBihrtu2YLkSWPMehQ9ncQUJ31ejRb8AXJOrSO	ADMIN	t	2026-10-28 04:30:58.349875+00	2026-10-28 04:30:58.349875+00
84	prueba2@gmail.com	$2a$10$w1q6t4NOKVA3eG8.DtbTVOFusWSTBlolzUQHn4jHST6cL2S6qsYqS	COMERCIO	f	2026-11-11 06:37:39.619036+00	2026-11-11 06:37:39.619049+00
64	cami@gmail.com	$2a$10$knN3sxq49Pvw2/0USxgT4O62lz6M6tXXaSZpG4oFPfBMvAG4qEXpS	COMERCIO	f	2026-11-01 20:45:37.850139+00	2026-11-01 20:45:37.85017+00
65	joseTissera@gmail.com	$2a$10$e.GKoKhBeR75bKxl.5yU4OyPpKoqLtxG4m4b3ePaLVuFmbTMlt3uG	COMERCIO	f	2026-11-01 21:03:19.232755+00	2026-11-01 21:03:19.232759+00
66	alexis@gmail.com	$2a$10$3bqzBVtepf.U94f3t9pvLuAiY9sWq8OBJuY1chPnRuHMypMy45mv.	CLIENTE	f	2026-11-01 21:13:28.021338+00	2026-11-01 21:13:28.021341+00
67	santiago@gmail.com	$2a$10$PvbLVER0S6np9VBj77hYWelw0wgzk6gcXDYBbWPOD04rcUYqrXAdu	CLIENTE	f	2026-11-01 21:20:54.611986+00	2026-11-01 21:20:54.611988+00
68	mateo@gmail.com	$2a$10$8H8A/6QzIZZiB6hhNS8h7u/R3wOtdUVW1eBdSje5//1ZYzuJ51r2C	CLIENTE	f	2026-11-01 21:22:48.986152+00	2026-11-01 21:22:48.986154+00
69	pilar@gmail.com	$2a$10$W77khy9uOszQ0bQan.RW4.XxX72FDHmRZKztt.NWtqc8ea2P5Fzg.	COMERCIO	f	2026-11-01 21:27:25.176298+00	2026-11-01 21:27:25.176301+00
70	claudia@gmail.com	$2a$10$tVENSmskqlAxZNJUuXLj6et1u7CGK92rg.2oEAS8k6xvnzcQY7x96	CLIENTE	f	2026-11-01 21:56:33.484733+00	2026-11-01 21:56:33.48476+00
76	aleTurri@gmail.com	$2a$10$TZM.fAVvUbo8xmu67g24keT7TmDB//U2fv/1Z70wJik0Jxqnv80VS	COMERCIO	f	2026-11-01 23:21:09.660891+00	2026-11-01 23:21:09.660915+00
\.


--
-- Data for Name: validaciones_comunitarias_oferta; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.validaciones_comunitarias_oferta (id, oferta_id, cliente_id, sigue_vigente, created_at, updated_at) FROM stdin;
1	38	66	t	2026-11-12 00:04:53.071403+00	2026-11-12 00:04:53.071403+00
2	39	67	t	2026-11-12 00:05:44.152117+00	2026-11-12 00:05:44.152117+00
3	36	68	t	2026-11-12 00:06:07.226132+00	2026-11-12 00:06:07.226132+00
4	39	70	t	2026-11-12 00:06:56.020512+00	2026-11-12 00:06:56.020512+00
\.


--
-- Data for Name: visualizaciones_comercio; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.visualizaciones_comercio (id, comercio_id, usuario_id, created_at) FROM stdin;
1	64	66	2026-11-01 21:16:28.775221+00
2	83	66	2026-11-11 06:35:58.301579+00
\.


--
-- Data for Name: visualizaciones_oferta; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.visualizaciones_oferta (id, oferta_id, usuario_id, created_at) FROM stdin;
1	25	66	2026-11-01 21:16:26.976418+00
2	32	70	2026-11-01 21:57:24.274236+00
3	34	66	2026-11-11 06:35:44.561342+00
4	38	66	2026-11-12 00:04:50.231928+00
5	37	66	2026-11-12 00:04:56.6566+00
6	36	66	2026-11-12 00:04:59.640099+00
7	35	66	2026-11-12 00:05:03.981414+00
8	35	67	2026-11-12 00:05:34.503587+00
9	36	67	2026-11-12 00:05:37.990249+00
10	39	67	2026-11-12 00:05:43.01112+00
11	36	68	2026-11-12 00:06:05.822824+00
12	37	68	2026-11-12 00:06:09.789848+00
13	39	68	2026-11-12 00:06:13.607522+00
14	38	68	2026-11-12 00:06:16.174537+00
15	35	68	2026-11-12 00:06:22.709989+00
16	38	70	2026-11-12 00:06:52.375093+00
17	39	70	2026-11-12 00:06:54.752136+00
18	31	70	2026-11-12 00:07:00.57854+00
\.


--
-- Data for Name: geocode_settings; Type: TABLE DATA; Schema: tiger; Owner: -
--

COPY tiger.geocode_settings (name, setting, unit, category, short_desc) FROM stdin;
\.


--
-- Data for Name: pagc_gaz; Type: TABLE DATA; Schema: tiger; Owner: -
--

COPY tiger.pagc_gaz (id, seq, word, stdword, token, is_custom) FROM stdin;
\.


--
-- Data for Name: pagc_lex; Type: TABLE DATA; Schema: tiger; Owner: -
--

COPY tiger.pagc_lex (id, seq, word, stdword, token, is_custom) FROM stdin;
\.


--
-- Data for Name: pagc_rules; Type: TABLE DATA; Schema: tiger; Owner: -
--

COPY tiger.pagc_rules (id, rule, is_custom) FROM stdin;
\.


--
-- Data for Name: topology; Type: TABLE DATA; Schema: topology; Owner: -
--

COPY topology.topology (id, name, srid, "precision", hasz) FROM stdin;
\.


--
-- Data for Name: layer; Type: TABLE DATA; Schema: topology; Owner: -
--

COPY topology.layer (topology_id, layer_id, schema_name, table_name, feature_column, feature_type, level, child_id) FROM stdin;
\.


--
-- Name: canjes_cupon_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.canjes_cupon_id_seq', 16, true);


--
-- Name: compromisos_cuponera_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.compromisos_cuponera_id_seq', 49, true);


--
-- Name: compromisos_sorteo_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.compromisos_sorteo_id_seq', 34, true);


--
-- Name: cupones_cuponera_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.cupones_cuponera_id_seq', 45, true);


--
-- Name: favoritos_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.favoritos_id_seq', 9, true);


--
-- Name: foro_comentario_upvotes_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.foro_comentario_upvotes_id_seq', 1, true);


--
-- Name: foro_comentarios_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.foro_comentarios_id_seq', 2, true);


--
-- Name: foro_publicacion_upvotes_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.foro_publicacion_upvotes_id_seq', 10, true);


--
-- Name: foro_publicaciones_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.foro_publicaciones_id_seq', 4, true);


--
-- Name: fotos_comercio_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.fotos_comercio_id_seq', 10, true);


--
-- Name: horarios_atencion_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.horarios_atencion_id_seq', 221, true);


--
-- Name: movimientos_puntos_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.movimientos_puntos_id_seq', 32, true);


--
-- Name: ofertas_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.ofertas_id_seq', 39, true);


--
-- Name: reportes_canje_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.reportes_canje_id_seq', 4, true);


--
-- Name: resenas_comercio_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.resenas_comercio_id_seq', 4, true);


--
-- Name: rubros_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.rubros_id_seq', 8, true);


--
-- Name: sorteos_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.sorteos_id_seq', 4, true);


--
-- Name: tickets_sorteo_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tickets_sorteo_id_seq', 7, true);


--
-- Name: usuarios_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.usuarios_id_seq', 84, true);


--
-- Name: validaciones_comunitarias_oferta_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.validaciones_comunitarias_oferta_id_seq', 4, true);


--
-- Name: visualizaciones_comercio_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.visualizaciones_comercio_id_seq', 2, true);


--
-- Name: visualizaciones_oferta_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.visualizaciones_oferta_id_seq', 18, true);


--
-- Name: topology_id_seq; Type: SEQUENCE SET; Schema: topology; Owner: -
--

SELECT pg_catalog.setval('topology.topology_id_seq', 1, false);


--
-- Name: canjes_cupon canjes_cupon_codigo_qr_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.canjes_cupon
    ADD CONSTRAINT canjes_cupon_codigo_qr_key UNIQUE (codigo_qr);


--
-- Name: canjes_cupon canjes_cupon_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.canjes_cupon
    ADD CONSTRAINT canjes_cupon_pkey PRIMARY KEY (id);


--
-- Name: compromisos_cuponera compromisos_cuponera_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compromisos_cuponera
    ADD CONSTRAINT compromisos_cuponera_pkey PRIMARY KEY (id);


--
-- Name: compromisos_sorteo compromisos_sorteo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compromisos_sorteo
    ADD CONSTRAINT compromisos_sorteo_pkey PRIMARY KEY (id);


--
-- Name: confirmaciones_canje confirmaciones_canje_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.confirmaciones_canje
    ADD CONSTRAINT confirmaciones_canje_pkey PRIMARY KEY (canje_id);


--
-- Name: cupones_cuponera cupones_cuponera_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cupones_cuponera
    ADD CONSTRAINT cupones_cuponera_pkey PRIMARY KEY (id);


--
-- Name: compromisos_cuponera ex_compromisos_cuponera_sin_solape; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compromisos_cuponera
    ADD CONSTRAINT ex_compromisos_cuponera_sin_solape EXCLUDE USING gist (comercio_id WITH =, daterange(periodo_inicio, periodo_fin, '[]'::text) WITH &&);


--
-- Name: compromisos_sorteo ex_compromisos_sorteo_sin_solape; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compromisos_sorteo
    ADD CONSTRAINT ex_compromisos_sorteo_sin_solape EXCLUDE USING gist (comercio_id WITH =, daterange(ciclo_inicio, ciclo_fin, '[]'::text) WITH &&);


--
-- Name: horarios_atencion ex_horarios_atencion_sin_solape; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.horarios_atencion
    ADD CONSTRAINT ex_horarios_atencion_sin_solape EXCLUDE USING gist (comercio_id WITH =, dia_semana WITH =, tsrange(('2000-01-01'::date + hora_inicio), ('2000-01-01'::date + hora_fin), '[)'::text) WITH &&) WHERE (activo);


--
-- Name: CONSTRAINT ex_horarios_atencion_sin_solape ON horarios_atencion; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON CONSTRAINT ex_horarios_atencion_sin_solape ON public.horarios_atencion IS 'Impide que dos franjas activas del mismo comercio se solapen en un mismo dia.';


--
-- Name: favoritos favoritos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.favoritos
    ADD CONSTRAINT favoritos_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: foro_comentario_upvotes foro_comentario_upvotes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.foro_comentario_upvotes
    ADD CONSTRAINT foro_comentario_upvotes_pkey PRIMARY KEY (id);


--
-- Name: foro_comentarios foro_comentarios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.foro_comentarios
    ADD CONSTRAINT foro_comentarios_pkey PRIMARY KEY (id);


--
-- Name: foro_publicacion_upvotes foro_publicacion_upvotes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.foro_publicacion_upvotes
    ADD CONSTRAINT foro_publicacion_upvotes_pkey PRIMARY KEY (id);


--
-- Name: foro_publicaciones foro_publicaciones_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.foro_publicaciones
    ADD CONSTRAINT foro_publicaciones_pkey PRIMARY KEY (id);


--
-- Name: fotos_comercio fotos_comercio_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fotos_comercio
    ADD CONSTRAINT fotos_comercio_pkey PRIMARY KEY (id);


--
-- Name: horarios_atencion horarios_atencion_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.horarios_atencion
    ADD CONSTRAINT horarios_atencion_pkey PRIMARY KEY (id);


--
-- Name: movimientos_puntos movimientos_puntos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.movimientos_puntos
    ADD CONSTRAINT movimientos_puntos_pkey PRIMARY KEY (id);


--
-- Name: ofertas_2x1 ofertas_2x1_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ofertas_2x1
    ADD CONSTRAINT ofertas_2x1_pkey PRIMARY KEY (oferta_id);


--
-- Name: ofertas_cantidad ofertas_cantidad_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ofertas_cantidad
    ADD CONSTRAINT ofertas_cantidad_pkey PRIMARY KEY (oferta_id);


--
-- Name: ofertas ofertas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ofertas
    ADD CONSTRAINT ofertas_pkey PRIMARY KEY (id);


--
-- Name: ofertas_por_mayor ofertas_por_mayor_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ofertas_por_mayor
    ADD CONSTRAINT ofertas_por_mayor_pkey PRIMARY KEY (oferta_id);


--
-- Name: ofertas_porcentaje ofertas_porcentaje_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ofertas_porcentaje
    ADD CONSTRAINT ofertas_porcentaje_pkey PRIMARY KEY (oferta_id);


--
-- Name: perfiles_cliente perfiles_cliente_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.perfiles_cliente
    ADD CONSTRAINT perfiles_cliente_pkey PRIMARY KEY (usuario_id);


--
-- Name: perfiles_comercio perfiles_comercio_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.perfiles_comercio
    ADD CONSTRAINT perfiles_comercio_pkey PRIMARY KEY (usuario_id);


--
-- Name: premios_sorteo premios_sorteo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.premios_sorteo
    ADD CONSTRAINT premios_sorteo_pkey PRIMARY KEY (ticket_id);


--
-- Name: premios_sorteo premios_sorteo_token_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.premios_sorteo
    ADD CONSTRAINT premios_sorteo_token_key UNIQUE (token);


--
-- Name: reportes_canje reportes_canje_canje_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reportes_canje
    ADD CONSTRAINT reportes_canje_canje_id_key UNIQUE (canje_id);


--
-- Name: reportes_canje reportes_canje_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reportes_canje
    ADD CONSTRAINT reportes_canje_pkey PRIMARY KEY (id);


--
-- Name: resenas_comercio resenas_comercio_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resenas_comercio
    ADD CONSTRAINT resenas_comercio_pkey PRIMARY KEY (id);


--
-- Name: rubros rubros_nombre_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rubros
    ADD CONSTRAINT rubros_nombre_key UNIQUE (nombre);


--
-- Name: rubros rubros_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rubros
    ADD CONSTRAINT rubros_pkey PRIMARY KEY (id);


--
-- Name: sorteos sorteos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sorteos
    ADD CONSTRAINT sorteos_pkey PRIMARY KEY (id);


--
-- Name: tickets_sorteo tickets_sorteo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tickets_sorteo
    ADD CONSTRAINT tickets_sorteo_pkey PRIMARY KEY (id);


--
-- Name: compromisos_cuponera uq_compromiso_cuponera_periodo; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compromisos_cuponera
    ADD CONSTRAINT uq_compromiso_cuponera_periodo UNIQUE (comercio_id, periodo_inicio);


--
-- Name: compromisos_sorteo uq_compromiso_sorteo_ciclo; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compromisos_sorteo
    ADD CONSTRAINT uq_compromiso_sorteo_ciclo UNIQUE (comercio_id, ciclo_inicio);


--
-- Name: favoritos uq_favoritos_cliente_oferta; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.favoritos
    ADD CONSTRAINT uq_favoritos_cliente_oferta UNIQUE (cliente_id, oferta_id);


--
-- Name: foro_comentario_upvotes uq_foro_comentario_upvote; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.foro_comentario_upvotes
    ADD CONSTRAINT uq_foro_comentario_upvote UNIQUE (comentario_id, usuario_id);


--
-- Name: foro_publicacion_upvotes uq_foro_publicacion_upvote; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.foro_publicacion_upvotes
    ADD CONSTRAINT uq_foro_publicacion_upvote UNIQUE (publicacion_id, usuario_id);


--
-- Name: premios_sorteo uq_premios_sorteo_sorteo; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.premios_sorteo
    ADD CONSTRAINT uq_premios_sorteo_sorteo UNIQUE (sorteo_id);


--
-- Name: resenas_comercio uq_resenas_comercio_cliente; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resenas_comercio
    ADD CONSTRAINT uq_resenas_comercio_cliente UNIQUE (comercio_id, cliente_id);


--
-- Name: sorteos uq_sorteos_compromiso; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sorteos
    ADD CONSTRAINT uq_sorteos_compromiso UNIQUE (compromiso_sorteo_id);


--
-- Name: validaciones_comunitarias_oferta uq_validacion_oferta_cliente; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.validaciones_comunitarias_oferta
    ADD CONSTRAINT uq_validacion_oferta_cliente UNIQUE (oferta_id, cliente_id);


--
-- Name: usuarios usuarios_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_email_key UNIQUE (email);


--
-- Name: usuarios usuarios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_pkey PRIMARY KEY (id);


--
-- Name: validaciones_comunitarias_oferta validaciones_comunitarias_oferta_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.validaciones_comunitarias_oferta
    ADD CONSTRAINT validaciones_comunitarias_oferta_pkey PRIMARY KEY (id);


--
-- Name: visualizaciones_comercio visualizaciones_comercio_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.visualizaciones_comercio
    ADD CONSTRAINT visualizaciones_comercio_pkey PRIMARY KEY (id);


--
-- Name: visualizaciones_oferta visualizaciones_oferta_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.visualizaciones_oferta
    ADD CONSTRAINT visualizaciones_oferta_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_canjes_cupon_cliente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_canjes_cupon_cliente ON public.canjes_cupon USING btree (cliente_id);


--
-- Name: idx_canjes_cupon_cupon; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_canjes_cupon_cupon ON public.canjes_cupon USING btree (cupon_id);


--
-- Name: idx_compromisos_cuponera_comercio; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_compromisos_cuponera_comercio ON public.compromisos_cuponera USING btree (comercio_id);


--
-- Name: idx_compromisos_sorteo_comercio; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_compromisos_sorteo_comercio ON public.compromisos_sorteo USING btree (comercio_id);


--
-- Name: idx_cupones_cuponera_compromiso; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cupones_cuponera_compromiso ON public.cupones_cuponera USING btree (compromiso_id);


--
-- Name: idx_favoritos_cliente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_favoritos_cliente ON public.favoritos USING btree (cliente_id);


--
-- Name: idx_foro_comentarios_padre; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_foro_comentarios_padre ON public.foro_comentarios USING btree (comentario_padre_id);


--
-- Name: idx_foro_comentarios_publicacion; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_foro_comentarios_publicacion ON public.foro_comentarios USING btree (publicacion_id);


--
-- Name: idx_foro_publicaciones_autor; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_foro_publicaciones_autor ON public.foro_publicaciones USING btree (autor_id);


--
-- Name: idx_foro_publicaciones_comercio; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_foro_publicaciones_comercio ON public.foro_publicaciones USING btree (comercio_id);


--
-- Name: idx_foro_publicaciones_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_foro_publicaciones_created_at ON public.foro_publicaciones USING btree (created_at DESC);


--
-- Name: idx_foro_publicaciones_ubicacion; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_foro_publicaciones_ubicacion ON public.foro_publicaciones USING gist (ubicacion_observada);


--
-- Name: idx_fotos_comercio_comercio; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_fotos_comercio_comercio ON public.fotos_comercio USING btree (comercio_id, orden);


--
-- Name: idx_horarios_atencion_comercio; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_horarios_atencion_comercio ON public.horarios_atencion USING btree (comercio_id);


--
-- Name: idx_movimientos_puntos_cliente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_movimientos_puntos_cliente ON public.movimientos_puntos USING btree (cliente_id, created_at DESC);


--
-- Name: idx_ofertas_activas; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ofertas_activas ON public.ofertas USING btree (activa, vigente_hasta) WHERE activa;


--
-- Name: idx_ofertas_comercio; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ofertas_comercio ON public.ofertas USING btree (comercio_id);


--
-- Name: idx_perfiles_comercio_rubro; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_perfiles_comercio_rubro ON public.perfiles_comercio USING btree (rubro_id);


--
-- Name: idx_perfiles_comercio_ubicacion; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_perfiles_comercio_ubicacion ON public.perfiles_comercio USING gist (ubicacion);


--
-- Name: idx_premios_sorteo_pendientes; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_premios_sorteo_pendientes ON public.premios_sorteo USING btree (cliente_id) WHERE (visto_en IS NULL);


--
-- Name: idx_reportes_canje_cliente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_reportes_canje_cliente ON public.reportes_canje USING btree (cliente_id);


--
-- Name: idx_resenas_comercio_comercio; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_resenas_comercio_comercio ON public.resenas_comercio USING btree (comercio_id);


--
-- Name: idx_sorteos_compromiso; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sorteos_compromiso ON public.sorteos USING btree (compromiso_sorteo_id);


--
-- Name: idx_tickets_sorteo_cliente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tickets_sorteo_cliente ON public.tickets_sorteo USING btree (cliente_id);


--
-- Name: idx_tickets_sorteo_sorteo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tickets_sorteo_sorteo ON public.tickets_sorteo USING btree (sorteo_id);


--
-- Name: idx_tickets_sorteo_sorteo_estado; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tickets_sorteo_sorteo_estado ON public.tickets_sorteo USING btree (sorteo_id, estado);


--
-- Name: idx_validaciones_oferta; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_validaciones_oferta ON public.validaciones_comunitarias_oferta USING btree (oferta_id);


--
-- Name: idx_visualizaciones_comercio_agregacion; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_visualizaciones_comercio_agregacion ON public.visualizaciones_comercio USING btree (comercio_id, created_at);


--
-- Name: idx_visualizaciones_oferta_agregacion; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_visualizaciones_oferta_agregacion ON public.visualizaciones_oferta USING btree (oferta_id, created_at);


--
-- Name: ofertas trg_bloquear_oferta_sin_acceso; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_bloquear_oferta_sin_acceso BEFORE INSERT OR UPDATE OF activa ON public.ofertas FOR EACH ROW EXECUTE FUNCTION public.fn_bloquear_oferta_sin_acceso();


--
-- Name: canjes_cupon trg_canjes_cupon_transicion; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_canjes_cupon_transicion BEFORE UPDATE OF estado ON public.canjes_cupon FOR EACH ROW EXECUTE FUNCTION public.fn_procesar_transicion_canje();


--
-- Name: canjes_cupon trg_canjes_cupon_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_canjes_cupon_updated_at BEFORE UPDATE ON public.canjes_cupon FOR EACH ROW EXECUTE FUNCTION public.fn_actualizar_updated_at();


--
-- Name: compromisos_sorteo trg_compromiso_sorteo_nace_pendiente; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_compromiso_sorteo_nace_pendiente BEFORE INSERT ON public.compromisos_sorteo FOR EACH ROW EXECUTE FUNCTION public.fn_compromiso_sorteo_nace_pendiente();


--
-- Name: compromisos_cuponera trg_compromisos_cuponera_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_compromisos_cuponera_updated_at BEFORE UPDATE ON public.compromisos_cuponera FOR EACH ROW EXECUTE FUNCTION public.fn_actualizar_updated_at();


--
-- Name: compromisos_sorteo trg_compromisos_sorteo_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_compromisos_sorteo_updated_at BEFORE UPDATE ON public.compromisos_sorteo FOR EACH ROW EXECUTE FUNCTION public.fn_actualizar_updated_at();


--
-- Name: cupones_cuponera trg_cupones_cuponera_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_cupones_cuponera_updated_at BEFORE UPDATE ON public.cupones_cuponera FOR EACH ROW EXECUTE FUNCTION public.fn_actualizar_updated_at();


--
-- Name: foro_comentario_upvotes trg_foro_comentario_upvotes_no_propio; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_foro_comentario_upvotes_no_propio BEFORE INSERT ON public.foro_comentario_upvotes FOR EACH ROW EXECUTE FUNCTION public.fn_validar_upvote_no_propio();


--
-- Name: foro_comentario_upvotes trg_foro_comentario_upvotes_puntos; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_foro_comentario_upvotes_puntos AFTER INSERT OR DELETE ON public.foro_comentario_upvotes FOR EACH ROW EXECUTE FUNCTION public.fn_otorgar_puntos_upvote();


--
-- Name: foro_comentarios trg_foro_comentarios_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_foro_comentarios_updated_at BEFORE UPDATE ON public.foro_comentarios FOR EACH ROW EXECUTE FUNCTION public.fn_actualizar_updated_at();


--
-- Name: foro_comentarios trg_foro_comentarios_validar_hilo; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_foro_comentarios_validar_hilo BEFORE INSERT OR UPDATE ON public.foro_comentarios FOR EACH ROW EXECUTE FUNCTION public.fn_validar_comentario_mismo_hilo();


--
-- Name: foro_publicacion_upvotes trg_foro_publicacion_upvotes_no_propio; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_foro_publicacion_upvotes_no_propio BEFORE INSERT ON public.foro_publicacion_upvotes FOR EACH ROW EXECUTE FUNCTION public.fn_validar_upvote_no_propio();


--
-- Name: foro_publicacion_upvotes trg_foro_publicacion_upvotes_puntos; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_foro_publicacion_upvotes_puntos AFTER INSERT OR DELETE ON public.foro_publicacion_upvotes FOR EACH ROW EXECUTE FUNCTION public.fn_otorgar_puntos_upvote();


--
-- Name: foro_publicaciones trg_foro_publicaciones_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_foro_publicaciones_updated_at BEFORE UPDATE ON public.foro_publicaciones FOR EACH ROW EXECUTE FUNCTION public.fn_actualizar_updated_at();


--
-- Name: horarios_atencion trg_horarios_atencion_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_horarios_atencion_updated_at BEFORE UPDATE ON public.horarios_atencion FOR EACH ROW EXECUTE FUNCTION public.fn_actualizar_updated_at();


--
-- Name: movimientos_puntos trg_movimientos_puntos_actualizar_saldo; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_movimientos_puntos_actualizar_saldo AFTER INSERT ON public.movimientos_puntos FOR EACH ROW EXECUTE FUNCTION public.fn_actualizar_saldo_puntos();


--
-- Name: ofertas trg_ofertas_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_ofertas_updated_at BEFORE UPDATE ON public.ofertas FOR EACH ROW EXECUTE FUNCTION public.fn_actualizar_updated_at();


--
-- Name: perfiles_cliente trg_perfiles_cliente_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_perfiles_cliente_updated_at BEFORE UPDATE ON public.perfiles_cliente FOR EACH ROW EXECUTE FUNCTION public.fn_actualizar_updated_at();


--
-- Name: perfiles_comercio trg_perfiles_comercio_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_perfiles_comercio_updated_at BEFORE UPDATE ON public.perfiles_comercio FOR EACH ROW EXECUTE FUNCTION public.fn_actualizar_updated_at();


--
-- Name: compromisos_cuponera trg_proteger_compromiso_cuponera; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_proteger_compromiso_cuponera BEFORE UPDATE ON public.compromisos_cuponera FOR EACH ROW EXECUTE FUNCTION public.fn_proteger_compromiso_cuponera();


--
-- Name: compromisos_sorteo trg_proteger_compromiso_sorteo; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_proteger_compromiso_sorteo BEFORE UPDATE ON public.compromisos_sorteo FOR EACH ROW EXECUTE FUNCTION public.fn_proteger_compromiso_sorteo();


--
-- Name: cupones_cuponera trg_proteger_cupon_ratificado; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_proteger_cupon_ratificado BEFORE INSERT OR DELETE OR UPDATE ON public.cupones_cuponera FOR EACH ROW EXECUTE FUNCTION public.fn_proteger_cupon_ratificado();


--
-- Name: sorteos trg_proteger_sorteo_finalizado; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_proteger_sorteo_finalizado BEFORE UPDATE ON public.sorteos FOR EACH ROW EXECUTE FUNCTION public.fn_proteger_sorteo_finalizado();


--
-- Name: resenas_comercio trg_resenas_comercio_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_resenas_comercio_updated_at BEFORE UPDATE ON public.resenas_comercio FOR EACH ROW EXECUTE FUNCTION public.fn_actualizar_updated_at();


--
-- Name: sorteos trg_sorteos_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_sorteos_updated_at BEFORE UPDATE ON public.sorteos FOR EACH ROW EXECUTE FUNCTION public.fn_actualizar_updated_at();


--
-- Name: tickets_sorteo trg_tickets_sorteo_procesar_compra; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_tickets_sorteo_procesar_compra AFTER INSERT ON public.tickets_sorteo FOR EACH ROW EXECUTE FUNCTION public.fn_procesar_compra_ticket_sorteo();


--
-- Name: usuarios trg_usuarios_crear_perfil; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_usuarios_crear_perfil AFTER INSERT ON public.usuarios FOR EACH ROW EXECUTE FUNCTION public.fn_crear_perfil_por_tipo_usuario();


--
-- Name: usuarios trg_usuarios_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_usuarios_updated_at BEFORE UPDATE ON public.usuarios FOR EACH ROW EXECUTE FUNCTION public.fn_actualizar_updated_at();


--
-- Name: validaciones_comunitarias_oferta trg_validaciones_comunitarias_oferta_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_validaciones_comunitarias_oferta_updated_at BEFORE UPDATE ON public.validaciones_comunitarias_oferta FOR EACH ROW EXECUTE FUNCTION public.fn_actualizar_updated_at();


--
-- Name: ofertas trg_validar_detalle_oferta; Type: TRIGGER; Schema: public; Owner: -
--

CREATE CONSTRAINT TRIGGER trg_validar_detalle_oferta AFTER INSERT OR UPDATE OF tipo_oferta ON public.ofertas DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION public.fn_validar_detalle_oferta();


--
-- Name: canjes_cupon trg_validar_nuevo_canje; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_validar_nuevo_canje BEFORE INSERT ON public.canjes_cupon FOR EACH ROW EXECUTE FUNCTION public.fn_validar_nuevo_canje();


--
-- Name: tickets_sorteo trg_validar_nuevo_ticket_sorteo; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_validar_nuevo_ticket_sorteo BEFORE INSERT ON public.tickets_sorteo FOR EACH ROW EXECUTE FUNCTION public.fn_validar_nuevo_ticket_sorteo();


--
-- Name: compromisos_sorteo trg_validar_orden_ciclo_sorteo; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_validar_orden_ciclo_sorteo BEFORE INSERT ON public.compromisos_sorteo FOR EACH ROW EXECUTE FUNCTION public.fn_validar_orden_ciclo_sorteo();


--
-- Name: compromisos_cuponera trg_validar_orden_periodo_cuponera; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_validar_orden_periodo_cuponera BEFORE INSERT ON public.compromisos_cuponera FOR EACH ROW EXECUTE FUNCTION public.fn_validar_orden_periodo_cuponera();


--
-- Name: cupones_cuponera trg_validar_rango_cupones_compromiso; Type: TRIGGER; Schema: public; Owner: -
--

CREATE CONSTRAINT TRIGGER trg_validar_rango_cupones_compromiso AFTER INSERT OR DELETE OR UPDATE ON public.cupones_cuponera DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION public.fn_validar_rango_cupones_compromiso();


--
-- Name: compromisos_cuponera trg_validar_ratificacion_cuponera; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_validar_ratificacion_cuponera BEFORE UPDATE OF estado ON public.compromisos_cuponera FOR EACH ROW EXECUTE FUNCTION public.fn_validar_ratificacion_cuponera();


--
-- Name: compromisos_sorteo trg_validar_ratificacion_sorteo; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_validar_ratificacion_sorteo BEFORE UPDATE OF estado ON public.compromisos_sorteo FOR EACH ROW EXECUTE FUNCTION public.fn_validar_ratificacion_sorteo();


--
-- Name: ofertas_2x1 trg_validar_tipo_ofertas_2x1; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_validar_tipo_ofertas_2x1 BEFORE INSERT OR UPDATE ON public.ofertas_2x1 FOR EACH ROW EXECUTE FUNCTION public.fn_validar_tipo_oferta('DOS_POR_UNO');


--
-- Name: ofertas_cantidad trg_validar_tipo_ofertas_cantidad; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_validar_tipo_ofertas_cantidad BEFORE INSERT OR UPDATE ON public.ofertas_cantidad FOR EACH ROW EXECUTE FUNCTION public.fn_validar_tipo_oferta('CANTIDAD_ULTIMA_UNIDAD');


--
-- Name: ofertas_por_mayor trg_validar_tipo_ofertas_por_mayor; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_validar_tipo_ofertas_por_mayor BEFORE INSERT OR UPDATE ON public.ofertas_por_mayor FOR EACH ROW EXECUTE FUNCTION public.fn_validar_tipo_oferta('POR_MAYOR');


--
-- Name: ofertas_porcentaje trg_validar_tipo_ofertas_porcentaje; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_validar_tipo_ofertas_porcentaje BEFORE INSERT OR UPDATE ON public.ofertas_porcentaje FOR EACH ROW EXECUTE FUNCTION public.fn_validar_tipo_oferta('PORCENTAJE');


--
-- Name: canjes_cupon canjes_cupon_cliente_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.canjes_cupon
    ADD CONSTRAINT canjes_cupon_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES public.perfiles_cliente(usuario_id) ON DELETE CASCADE;


--
-- Name: canjes_cupon canjes_cupon_cupon_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.canjes_cupon
    ADD CONSTRAINT canjes_cupon_cupon_id_fkey FOREIGN KEY (cupon_id) REFERENCES public.cupones_cuponera(id);


--
-- Name: compromisos_cuponera compromisos_cuponera_comercio_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compromisos_cuponera
    ADD CONSTRAINT compromisos_cuponera_comercio_id_fkey FOREIGN KEY (comercio_id) REFERENCES public.perfiles_comercio(usuario_id) ON DELETE CASCADE;


--
-- Name: compromisos_sorteo compromisos_sorteo_comercio_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compromisos_sorteo
    ADD CONSTRAINT compromisos_sorteo_comercio_id_fkey FOREIGN KEY (comercio_id) REFERENCES public.perfiles_comercio(usuario_id) ON DELETE CASCADE;


--
-- Name: confirmaciones_canje confirmaciones_canje_canje_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.confirmaciones_canje
    ADD CONSTRAINT confirmaciones_canje_canje_id_fkey FOREIGN KEY (canje_id) REFERENCES public.canjes_cupon(id) ON DELETE CASCADE;


--
-- Name: confirmaciones_canje confirmaciones_canje_cliente_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.confirmaciones_canje
    ADD CONSTRAINT confirmaciones_canje_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES public.perfiles_cliente(usuario_id) ON DELETE CASCADE;


--
-- Name: cupones_cuponera cupones_cuponera_compromiso_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cupones_cuponera
    ADD CONSTRAINT cupones_cuponera_compromiso_id_fkey FOREIGN KEY (compromiso_id) REFERENCES public.compromisos_cuponera(id) ON DELETE CASCADE;


--
-- Name: favoritos favoritos_cliente_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.favoritos
    ADD CONSTRAINT favoritos_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES public.perfiles_cliente(usuario_id) ON DELETE CASCADE;


--
-- Name: favoritos favoritos_oferta_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.favoritos
    ADD CONSTRAINT favoritos_oferta_id_fkey FOREIGN KEY (oferta_id) REFERENCES public.ofertas(id) ON DELETE CASCADE;


--
-- Name: sorteos fk_sorteos_ticket_ganador; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sorteos
    ADD CONSTRAINT fk_sorteos_ticket_ganador FOREIGN KEY (ticket_ganador_id) REFERENCES public.tickets_sorteo(id);


--
-- Name: foro_comentario_upvotes foro_comentario_upvotes_comentario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.foro_comentario_upvotes
    ADD CONSTRAINT foro_comentario_upvotes_comentario_id_fkey FOREIGN KEY (comentario_id) REFERENCES public.foro_comentarios(id) ON DELETE CASCADE;


--
-- Name: foro_comentario_upvotes foro_comentario_upvotes_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.foro_comentario_upvotes
    ADD CONSTRAINT foro_comentario_upvotes_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.perfiles_cliente(usuario_id) ON DELETE CASCADE;


--
-- Name: foro_comentarios foro_comentarios_autor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.foro_comentarios
    ADD CONSTRAINT foro_comentarios_autor_id_fkey FOREIGN KEY (autor_id) REFERENCES public.perfiles_cliente(usuario_id) ON DELETE CASCADE;


--
-- Name: foro_comentarios foro_comentarios_comentario_padre_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.foro_comentarios
    ADD CONSTRAINT foro_comentarios_comentario_padre_id_fkey FOREIGN KEY (comentario_padre_id) REFERENCES public.foro_comentarios(id) ON DELETE CASCADE;


--
-- Name: foro_comentarios foro_comentarios_publicacion_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.foro_comentarios
    ADD CONSTRAINT foro_comentarios_publicacion_id_fkey FOREIGN KEY (publicacion_id) REFERENCES public.foro_publicaciones(id) ON DELETE CASCADE;


--
-- Name: foro_publicacion_upvotes foro_publicacion_upvotes_publicacion_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.foro_publicacion_upvotes
    ADD CONSTRAINT foro_publicacion_upvotes_publicacion_id_fkey FOREIGN KEY (publicacion_id) REFERENCES public.foro_publicaciones(id) ON DELETE CASCADE;


--
-- Name: foro_publicacion_upvotes foro_publicacion_upvotes_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.foro_publicacion_upvotes
    ADD CONSTRAINT foro_publicacion_upvotes_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.perfiles_cliente(usuario_id) ON DELETE CASCADE;


--
-- Name: foro_publicaciones foro_publicaciones_autor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.foro_publicaciones
    ADD CONSTRAINT foro_publicaciones_autor_id_fkey FOREIGN KEY (autor_id) REFERENCES public.perfiles_cliente(usuario_id) ON DELETE CASCADE;


--
-- Name: foro_publicaciones foro_publicaciones_comercio_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.foro_publicaciones
    ADD CONSTRAINT foro_publicaciones_comercio_id_fkey FOREIGN KEY (comercio_id) REFERENCES public.perfiles_comercio(usuario_id);


--
-- Name: fotos_comercio fotos_comercio_comercio_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fotos_comercio
    ADD CONSTRAINT fotos_comercio_comercio_id_fkey FOREIGN KEY (comercio_id) REFERENCES public.perfiles_comercio(usuario_id) ON DELETE CASCADE;


--
-- Name: horarios_atencion horarios_atencion_comercio_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.horarios_atencion
    ADD CONSTRAINT horarios_atencion_comercio_id_fkey FOREIGN KEY (comercio_id) REFERENCES public.perfiles_comercio(usuario_id) ON DELETE CASCADE;


--
-- Name: movimientos_puntos movimientos_puntos_cliente_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.movimientos_puntos
    ADD CONSTRAINT movimientos_puntos_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES public.perfiles_cliente(usuario_id) ON DELETE CASCADE;


--
-- Name: ofertas_2x1 ofertas_2x1_oferta_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ofertas_2x1
    ADD CONSTRAINT ofertas_2x1_oferta_id_fkey FOREIGN KEY (oferta_id) REFERENCES public.ofertas(id) ON DELETE CASCADE;


--
-- Name: ofertas_cantidad ofertas_cantidad_oferta_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ofertas_cantidad
    ADD CONSTRAINT ofertas_cantidad_oferta_id_fkey FOREIGN KEY (oferta_id) REFERENCES public.ofertas(id) ON DELETE CASCADE;


--
-- Name: ofertas ofertas_comercio_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ofertas
    ADD CONSTRAINT ofertas_comercio_id_fkey FOREIGN KEY (comercio_id) REFERENCES public.perfiles_comercio(usuario_id) ON DELETE CASCADE;


--
-- Name: ofertas_por_mayor ofertas_por_mayor_oferta_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ofertas_por_mayor
    ADD CONSTRAINT ofertas_por_mayor_oferta_id_fkey FOREIGN KEY (oferta_id) REFERENCES public.ofertas(id) ON DELETE CASCADE;


--
-- Name: ofertas_porcentaje ofertas_porcentaje_oferta_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ofertas_porcentaje
    ADD CONSTRAINT ofertas_porcentaje_oferta_id_fkey FOREIGN KEY (oferta_id) REFERENCES public.ofertas(id) ON DELETE CASCADE;


--
-- Name: perfiles_cliente perfiles_cliente_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.perfiles_cliente
    ADD CONSTRAINT perfiles_cliente_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id) ON DELETE CASCADE;


--
-- Name: perfiles_comercio perfiles_comercio_rubro_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.perfiles_comercio
    ADD CONSTRAINT perfiles_comercio_rubro_id_fkey FOREIGN KEY (rubro_id) REFERENCES public.rubros(id);


--
-- Name: perfiles_comercio perfiles_comercio_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.perfiles_comercio
    ADD CONSTRAINT perfiles_comercio_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id) ON DELETE CASCADE;


--
-- Name: premios_sorteo premios_sorteo_canjeado_por_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.premios_sorteo
    ADD CONSTRAINT premios_sorteo_canjeado_por_fkey FOREIGN KEY (canjeado_por) REFERENCES public.perfiles_comercio(usuario_id);


--
-- Name: premios_sorteo premios_sorteo_cliente_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.premios_sorteo
    ADD CONSTRAINT premios_sorteo_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES public.perfiles_cliente(usuario_id) ON DELETE CASCADE;


--
-- Name: premios_sorteo premios_sorteo_sorteo_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.premios_sorteo
    ADD CONSTRAINT premios_sorteo_sorteo_id_fkey FOREIGN KEY (sorteo_id) REFERENCES public.sorteos(id) ON DELETE CASCADE;


--
-- Name: premios_sorteo premios_sorteo_ticket_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.premios_sorteo
    ADD CONSTRAINT premios_sorteo_ticket_id_fkey FOREIGN KEY (ticket_id) REFERENCES public.tickets_sorteo(id) ON DELETE CASCADE;


--
-- Name: reportes_canje reportes_canje_canje_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reportes_canje
    ADD CONSTRAINT reportes_canje_canje_id_fkey FOREIGN KEY (canje_id) REFERENCES public.canjes_cupon(id) ON DELETE CASCADE;


--
-- Name: reportes_canje reportes_canje_cliente_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reportes_canje
    ADD CONSTRAINT reportes_canje_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES public.perfiles_cliente(usuario_id) ON DELETE CASCADE;


--
-- Name: resenas_comercio resenas_comercio_cliente_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resenas_comercio
    ADD CONSTRAINT resenas_comercio_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES public.perfiles_cliente(usuario_id) ON DELETE CASCADE;


--
-- Name: resenas_comercio resenas_comercio_comercio_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resenas_comercio
    ADD CONSTRAINT resenas_comercio_comercio_id_fkey FOREIGN KEY (comercio_id) REFERENCES public.perfiles_comercio(usuario_id) ON DELETE CASCADE;


--
-- Name: sorteos sorteos_compromiso_sorteo_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sorteos
    ADD CONSTRAINT sorteos_compromiso_sorteo_id_fkey FOREIGN KEY (compromiso_sorteo_id) REFERENCES public.compromisos_sorteo(id);


--
-- Name: tickets_sorteo tickets_sorteo_cliente_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tickets_sorteo
    ADD CONSTRAINT tickets_sorteo_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES public.perfiles_cliente(usuario_id) ON DELETE CASCADE;


--
-- Name: tickets_sorteo tickets_sorteo_sorteo_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tickets_sorteo
    ADD CONSTRAINT tickets_sorteo_sorteo_id_fkey FOREIGN KEY (sorteo_id) REFERENCES public.sorteos(id) ON DELETE CASCADE;


--
-- Name: validaciones_comunitarias_oferta validaciones_comunitarias_oferta_cliente_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.validaciones_comunitarias_oferta
    ADD CONSTRAINT validaciones_comunitarias_oferta_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES public.perfiles_cliente(usuario_id) ON DELETE CASCADE;


--
-- Name: validaciones_comunitarias_oferta validaciones_comunitarias_oferta_oferta_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.validaciones_comunitarias_oferta
    ADD CONSTRAINT validaciones_comunitarias_oferta_oferta_id_fkey FOREIGN KEY (oferta_id) REFERENCES public.ofertas(id) ON DELETE CASCADE;


--
-- Name: visualizaciones_comercio visualizaciones_comercio_comercio_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.visualizaciones_comercio
    ADD CONSTRAINT visualizaciones_comercio_comercio_id_fkey FOREIGN KEY (comercio_id) REFERENCES public.perfiles_comercio(usuario_id) ON DELETE CASCADE;


--
-- Name: visualizaciones_comercio visualizaciones_comercio_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.visualizaciones_comercio
    ADD CONSTRAINT visualizaciones_comercio_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id) ON DELETE SET NULL;


--
-- Name: visualizaciones_oferta visualizaciones_oferta_oferta_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.visualizaciones_oferta
    ADD CONSTRAINT visualizaciones_oferta_oferta_id_fkey FOREIGN KEY (oferta_id) REFERENCES public.ofertas(id) ON DELETE CASCADE;


--
-- Name: visualizaciones_oferta visualizaciones_oferta_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.visualizaciones_oferta
    ADD CONSTRAINT visualizaciones_oferta_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id) ON DELETE SET NULL;


--
-- PostgreSQL database dump complete
--

