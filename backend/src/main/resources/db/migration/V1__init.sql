
-- =============================================================================
-- SECCIÓN 0: EXTENSIONES
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS citext;   -- email case-insensitive
CREATE EXTENSION IF NOT EXISTS pgcrypto; -- gen_random_uuid() para códigos QR
CREATE EXTENSION IF NOT EXISTS postgis;  -- tipo geography + índices GIST para cercanía


-- =============================================================================
-- SECCIÓN 1: UTILIDADES COMUNES
-- =============================================================================

CREATE OR REPLACE FUNCTION fn_actualizar_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION fn_actualizar_updated_at() IS
    'Trigger BEFORE UPDATE genérico que actualiza la columna updated_at.';


-- =============================================================================
-- SECCIÓN 2: TIPOS ENUMERADOS DEL DOMINIO
-- =============================================================================

CREATE TYPE tipo_usuario_enum AS ENUM ('CLIENTE', 'COMERCIO');

CREATE TYPE tipo_beneficio_enum AS ENUM (
    'PORCENTAJE',
    'DOS_POR_UNO',
    'CANTIDAD_ULTIMA_UNIDAD',
    'POR_MAYOR'
);

CREATE TYPE tipo_vigencia_enum AS ENUM (
    'SOLO_HOY',
    'HASTA_AGOTAR_STOCK',
    'TODA_LA_SEMANA',
    'FECHA_ESPECIFICA'
);

CREATE TYPE estado_compromiso_enum AS ENUM ('PENDIENTE', 'RATIFICADO', 'VENCIDO');

CREATE TYPE estado_canje_cupon_enum AS ENUM (
    'PENDIENTE_VALIDACION',
    'VALIDADO',
    'CANCELADO',
    'VENCIDO'
);

CREATE TYPE estado_sorteo_enum AS ENUM ('ACTIVO', 'CERRADO', 'SORTEADO');

CREATE TYPE tipo_movimiento_puntos_enum AS ENUM (
    'UPVOTE_RECIBIDO',
    'CANJE_CUPON',
    'COMPRA_TICKET_SORTEO',
    'AJUSTE_ADMIN'
);


-- =============================================================================
-- SECCIÓN 3: IDENTIDAD Y PERFILES (usuarios, clientes, comercios)
-- =============================================================================

CREATE TABLE usuarios (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email             CITEXT NOT NULL UNIQUE,
    password_hash     TEXT NOT NULL,
    tipo_usuario      tipo_usuario_enum NOT NULL,
    email_verificado  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_usuarios_email_gmail
        CHECK (email ~* '^[a-z0-9._%+-]+@gmail\.com$')
);

CREATE TRIGGER trg_usuarios_updated_at
    BEFORE UPDATE ON usuarios
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_updated_at();

COMMENT ON TABLE usuarios IS 'Cuenta de acceso. El rol define qué tabla de perfil se crea automáticamente.';


CREATE TABLE rubros (
    id      SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre  TEXT NOT NULL UNIQUE
);

COMMENT ON TABLE rubros IS
    'Catálogo de rubros de comercio. Extensible con INSERT, sin necesidad de migración.';

INSERT INTO rubros (nombre) VALUES
    ('Almacén'),
    ('Carnicería'),
    ('Verdulería'),
    ('Panadería'),
    ('Farmacia'),
    ('Limpieza'),
    ('Kiosco'),
    ('Otro rubro');


CREATE TABLE perfiles_cliente (
    usuario_id                  BIGINT PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,
    saldo_puntos                INTEGER NOT NULL DEFAULT 0,
    puntos_totales_historicos   INTEGER NOT NULL DEFAULT 0,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_perfiles_cliente_saldo_no_negativo CHECK (saldo_puntos >= 0),
    CONSTRAINT chk_perfiles_cliente_historicos_no_negativo CHECK (puntos_totales_historicos >= 0),
    CONSTRAINT chk_perfiles_cliente_historicos_mayor_saldo CHECK (puntos_totales_historicos >= saldo_puntos)
);

CREATE TRIGGER trg_perfiles_cliente_updated_at
    BEFORE UPDATE ON perfiles_cliente
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_updated_at();

COMMENT ON COLUMN perfiles_cliente.saldo_puntos IS
    'Saldo gastable actual (sube y baja). Única fuente de verdad: la suma de movimientos_puntos.';
COMMENT ON COLUMN perfiles_cliente.puntos_totales_historicos IS
    'Puntos ganados acumulados de por vida (nunca decrece). Usado para desbloquear sorteos a los 100 puntos (SCRUM-91), independientemente de cuánto haya gastado el cliente después.';


CREATE TABLE perfiles_comercio (
    usuario_id              BIGINT PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,
    nombre_negocio          TEXT,
    rubro_id                SMALLINT REFERENCES rubros(id),
    descripcion             TEXT,
    direccion               TEXT,
    ubicacion               geography(Point, 4326),
    onboarding_completado   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),


    CONSTRAINT chk_perfiles_comercio_onboarding_completo CHECK (
        NOT onboarding_completado
        OR (nombre_negocio IS NOT NULL
            AND rubro_id IS NOT NULL
            AND direccion IS NOT NULL
            AND ubicacion IS NOT NULL
            AND descripcion IS NOT NULL)
    )
);

CREATE TRIGGER trg_perfiles_comercio_updated_at
    BEFORE UPDATE ON perfiles_comercio
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_updated_at();

CREATE INDEX idx_perfiles_comercio_ubicacion ON perfiles_comercio USING GIST (ubicacion);
CREATE INDEX idx_perfiles_comercio_rubro ON perfiles_comercio (rubro_id);

COMMENT ON TABLE perfiles_comercio IS 'Datos de negocio de un usuario con rol COMERCIO. 1:1 con usuarios.';



CREATE OR REPLACE FUNCTION fn_crear_perfil_por_tipo_usuario()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.tipo_usuario = 'CLIENTE' THEN
        INSERT INTO perfiles_cliente (usuario_id) VALUES (NEW.id);
    ELSIF NEW.tipo_usuario = 'COMERCIO' THEN
        INSERT INTO perfiles_comercio (usuario_id) VALUES (NEW.id);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_usuarios_crear_perfil
    AFTER INSERT ON usuarios
    FOR EACH ROW EXECUTE FUNCTION fn_crear_perfil_por_tipo_usuario();


CREATE TABLE horarios_atencion_comercio (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    comercio_id   BIGINT NOT NULL REFERENCES perfiles_comercio(usuario_id) ON DELETE CASCADE,
    dia_semana    SMALLINT NOT NULL CHECK (dia_semana BETWEEN 0 AND 6),
    cerrado       BOOLEAN NOT NULL DEFAULT FALSE,
    hora_apertura TIME,
    hora_cierre   TIME,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_horarios_coherencia CHECK (
        (cerrado AND hora_apertura IS NULL AND hora_cierre IS NULL)
        OR (NOT cerrado AND hora_apertura IS NOT NULL AND hora_cierre IS NOT NULL AND hora_cierre > hora_apertura)
    )
);

CREATE TRIGGER trg_horarios_atencion_comercio_updated_at
    BEFORE UPDATE ON horarios_atencion_comercio
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_updated_at();

CREATE INDEX idx_horarios_atencion_comercio_comercio ON horarios_atencion_comercio (comercio_id);


CREATE TABLE fotos_comercio (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    comercio_id  BIGINT NOT NULL REFERENCES perfiles_comercio(usuario_id) ON DELETE CASCADE,
    url          TEXT NOT NULL,
    orden        SMALLINT NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_fotos_comercio_comercio ON fotos_comercio (comercio_id, orden);


-- =============================================================================
-- SECCIÓN 4: SISTEMA DE PUNTOS (ledger)
-- =============================================================================
-- Se crea antes que ofertas/foro/cuponera porque varios triggers posteriores
-- insertan movimientos acá. El saldo de perfiles_cliente se mantiene 100%
-- derivado de este ledger: nunca se actualiza a mano desde la aplicación.

CREATE TABLE movimientos_puntos (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cliente_id       BIGINT NOT NULL REFERENCES perfiles_cliente(usuario_id) ON DELETE CASCADE,
    tipo             tipo_movimiento_puntos_enum NOT NULL,
    puntos           INTEGER NOT NULL,
    referencia_tabla TEXT,
    referencia_id    BIGINT,
    descripcion      TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_movimientos_puntos_no_cero CHECK (puntos <> 0)
);

CREATE INDEX idx_movimientos_puntos_cliente ON movimientos_puntos (cliente_id, created_at DESC);

COMMENT ON TABLE movimientos_puntos IS
    'Ledger inmutable de puntos. Fuente de verdad de saldo_puntos y puntos_totales_historicos.';


CREATE OR REPLACE FUNCTION fn_actualizar_saldo_puntos()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE perfiles_cliente
    SET saldo_puntos = saldo_puntos + NEW.puntos,
        puntos_totales_historicos = puntos_totales_historicos + GREATEST(NEW.puntos, 0)
    WHERE usuario_id = NEW.cliente_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


CREATE TRIGGER trg_movimientos_puntos_actualizar_saldo
    AFTER INSERT ON movimientos_puntos
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_saldo_puntos();


-- =============================================================================
-- SECCIÓN 5: OFERTAS DEL COMERCIO
-- =============================================================================
-- Modelo elegido: tabla base "ofertas" (columnas comunes) + una tabla de
-- detalle por tipo (normalizado). Dos triggers garantizan la consistencia
-- 1:1 entre la oferta y su detalle, en ambas direcciones.

CREATE TABLE ofertas (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    comercio_id      BIGINT NOT NULL REFERENCES perfiles_comercio(usuario_id) ON DELETE CASCADE,
    tipo_oferta      tipo_beneficio_enum NOT NULL,
    nombre_producto  TEXT NOT NULL,
    foto_url         TEXT,
    vigencia_tipo    tipo_vigencia_enum NOT NULL,
    vigente_hasta    TIMESTAMPTZ,
    activa           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_ofertas_updated_at
    BEFORE UPDATE ON ofertas
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_updated_at();

CREATE INDEX idx_ofertas_comercio ON ofertas (comercio_id);
CREATE INDEX idx_ofertas_activas ON ofertas (activa, vigente_hasta) WHERE activa;


CREATE OR REPLACE FUNCTION fn_oferta_vigente(p_oferta ofertas)
RETURNS BOOLEAN AS $$
    SELECT p_oferta.activa AND (p_oferta.vigente_hasta IS NULL OR p_oferta.vigente_hasta > now());
$$ LANGUAGE sql STABLE;


CREATE TABLE ofertas_porcentaje (
    oferta_id             BIGINT PRIMARY KEY REFERENCES ofertas(id) ON DELETE CASCADE,
    precio_original       NUMERIC(12,2) NOT NULL CHECK (precio_original > 0),
    porcentaje_descuento  SMALLINT NOT NULL CHECK (porcentaje_descuento BETWEEN 1 AND 100),
    precio_final          NUMERIC(12,2) GENERATED ALWAYS AS (
                               ROUND(precio_original * (1 - porcentaje_descuento::numeric / 100), 2)
                           ) STORED
);

CREATE TABLE ofertas_2x1 (
    oferta_id          BIGINT PRIMARY KEY REFERENCES ofertas(id) ON DELETE CASCADE,
    unidades_a_pagar   SMALLINT NOT NULL CHECK (unidades_a_pagar > 0),
    unidades_a_llevar  SMALLINT NOT NULL CHECK (unidades_a_llevar > unidades_a_pagar),
    precio_unitario    NUMERIC(12,2) CHECK (precio_unitario > 0)
);

COMMENT ON TABLE ofertas_2x1 IS
    'Cubre 2x1, 3x2 y variantes equivalentes: unidades_a_llevar > unidades_a_pagar (SCRUM-73).';

CREATE TABLE ofertas_cantidad (
    oferta_id                            BIGINT PRIMARY KEY REFERENCES ofertas(id) ON DELETE CASCADE,
    cantidad_requerida                   SMALLINT NOT NULL CHECK (cantidad_requerida > 0),
    precio_unitario                      NUMERIC(12,2) NOT NULL CHECK (precio_unitario > 0),
    porcentaje_descuento_ultima_unidad   SMALLINT NOT NULL CHECK (porcentaje_descuento_ultima_unidad BETWEEN 1 AND 100)
);

COMMENT ON TABLE ofertas_cantidad IS
    'Descuento sobre la última unidad al comprar una cantidad mínima (SCRUM-71).';

CREATE TABLE ofertas_por_mayor (
    oferta_id                  BIGINT PRIMARY KEY REFERENCES ofertas(id) ON DELETE CASCADE,
    cantidad_minima             SMALLINT NOT NULL CHECK (cantidad_minima > 0),
    precio_unitario_mayorista   NUMERIC(12,2) NOT NULL CHECK (precio_unitario_mayorista > 0),
    precio_unitario_regular     NUMERIC(12,2) CHECK (precio_unitario_regular > 0)
);

COMMENT ON TABLE ofertas_por_mayor IS
    'SCRUM-74 deja el modelo de precios "a definir": estructura mínima razonable, '
    'candidata a ajustarse cuando se cierre esa historia.';



CREATE OR REPLACE FUNCTION fn_validar_tipo_oferta()
RETURNS TRIGGER AS $$
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
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validar_tipo_ofertas_porcentaje
    BEFORE INSERT OR UPDATE ON ofertas_porcentaje
    FOR EACH ROW EXECUTE FUNCTION fn_validar_tipo_oferta('PORCENTAJE');

CREATE TRIGGER trg_validar_tipo_ofertas_2x1
    BEFORE INSERT OR UPDATE ON ofertas_2x1
    FOR EACH ROW EXECUTE FUNCTION fn_validar_tipo_oferta('DOS_POR_UNO');

CREATE TRIGGER trg_validar_tipo_ofertas_cantidad
    BEFORE INSERT OR UPDATE ON ofertas_cantidad
    FOR EACH ROW EXECUTE FUNCTION fn_validar_tipo_oferta('CANTIDAD_ULTIMA_UNIDAD');

CREATE TRIGGER trg_validar_tipo_ofertas_por_mayor
    BEFORE INSERT OR UPDATE ON ofertas_por_mayor
    FOR EACH ROW EXECUTE FUNCTION fn_validar_tipo_oferta('POR_MAYOR');


CREATE OR REPLACE FUNCTION fn_validar_detalle_oferta()
RETURNS TRIGGER AS $$
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
$$ LANGUAGE plpgsql;


CREATE CONSTRAINT TRIGGER trg_validar_detalle_oferta
    AFTER INSERT OR UPDATE OF tipo_oferta ON ofertas
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION fn_validar_detalle_oferta();


-- =============================================================================
-- SECCIÓN 6: DESCUBRIMIENTO — VALIDACIÓN COMUNITARIA Y FAVORITOS
-- =============================================================================

CREATE TABLE validaciones_comunitarias_oferta (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    oferta_id     BIGINT NOT NULL REFERENCES ofertas(id) ON DELETE CASCADE,
    cliente_id    BIGINT NOT NULL REFERENCES perfiles_cliente(usuario_id) ON DELETE CASCADE,
    sigue_vigente BOOLEAN NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),


    CONSTRAINT uq_validacion_oferta_cliente UNIQUE (oferta_id, cliente_id)
);

CREATE TRIGGER trg_validaciones_comunitarias_oferta_updated_at
    BEFORE UPDATE ON validaciones_comunitarias_oferta
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_updated_at();

CREATE INDEX idx_validaciones_oferta ON validaciones_comunitarias_oferta (oferta_id);

COMMENT ON TABLE validaciones_comunitarias_oferta IS
    'SCRUM-58: el comercio nunca puede votar acá porque cliente_id referencia '
    'perfiles_cliente, que no existe para usuarios de tipo COMERCIO.';


CREATE TABLE favoritos (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cliente_id  BIGINT NOT NULL REFERENCES perfiles_cliente(usuario_id) ON DELETE CASCADE,
    oferta_id   BIGINT NOT NULL REFERENCES ofertas(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_favoritos_cliente_oferta UNIQUE (cliente_id, oferta_id)
);

CREATE INDEX idx_favoritos_cliente ON favoritos (cliente_id);


-- =============================================================================
-- SECCIÓN 7: RESEÑAS
-- =============================================================================

CREATE TABLE resenas_comercio (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    comercio_id  BIGINT NOT NULL REFERENCES perfiles_comercio(usuario_id) ON DELETE CASCADE,
    cliente_id   BIGINT NOT NULL REFERENCES perfiles_cliente(usuario_id) ON DELETE CASCADE,
    texto        TEXT NOT NULL,
    estrellas    SMALLINT NOT NULL CHECK (estrellas BETWEEN 1 AND 5),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),


    CONSTRAINT uq_resenas_comercio_cliente UNIQUE (comercio_id, cliente_id)
);

CREATE TRIGGER trg_resenas_comercio_updated_at
    BEFORE UPDATE ON resenas_comercio
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_updated_at();

CREATE INDEX idx_resenas_comercio_comercio ON resenas_comercio (comercio_id);


-- =============================================================================
-- SECCIÓN 8: FORO COMUNITARIO
-- =============================================================================

CREATE TABLE foro_publicaciones (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    autor_id             BIGINT NOT NULL REFERENCES perfiles_cliente(usuario_id) ON DELETE CASCADE,
    comercio_id          BIGINT NOT NULL REFERENCES perfiles_comercio(usuario_id),
    texto                TEXT NOT NULL,
    precio_observado     NUMERIC(12,2) CHECK (precio_observado > 0),
    ubicacion_observada  geography(Point, 4326),
    eliminado_en         TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_foro_publicaciones_updated_at
    BEFORE UPDATE ON foro_publicaciones
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_updated_at();

CREATE INDEX idx_foro_publicaciones_comercio ON foro_publicaciones (comercio_id);
CREATE INDEX idx_foro_publicaciones_autor ON foro_publicaciones (autor_id);
CREATE INDEX idx_foro_publicaciones_ubicacion ON foro_publicaciones USING GIST (ubicacion_observada);
CREATE INDEX idx_foro_publicaciones_created_at ON foro_publicaciones (created_at DESC);


CREATE TABLE foro_comentarios (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    publicacion_id       BIGINT NOT NULL REFERENCES foro_publicaciones(id) ON DELETE CASCADE,
    comentario_padre_id  BIGINT REFERENCES foro_comentarios(id) ON DELETE CASCADE,
    autor_id             BIGINT NOT NULL REFERENCES perfiles_cliente(usuario_id) ON DELETE CASCADE,
    texto                TEXT NOT NULL,
    eliminado_en         TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_foro_comentarios_updated_at
    BEFORE UPDATE ON foro_comentarios
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_updated_at();

CREATE INDEX idx_foro_comentarios_publicacion ON foro_comentarios (publicacion_id);
CREATE INDEX idx_foro_comentarios_padre ON foro_comentarios (comentario_padre_id);

-- Un comentario que responde a otro comentario debe pertenecer a la misma publicación.
CREATE OR REPLACE FUNCTION fn_validar_comentario_mismo_hilo()
RETURNS TRIGGER AS $$
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
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_foro_comentarios_validar_hilo
    BEFORE INSERT OR UPDATE ON foro_comentarios
    FOR EACH ROW EXECUTE FUNCTION fn_validar_comentario_mismo_hilo();


CREATE TABLE foro_publicacion_upvotes (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    publicacion_id  BIGINT NOT NULL REFERENCES foro_publicaciones(id) ON DELETE CASCADE,
    usuario_id      BIGINT NOT NULL REFERENCES perfiles_cliente(usuario_id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_foro_publicacion_upvote UNIQUE (publicacion_id, usuario_id)
);

CREATE TABLE foro_comentario_upvotes (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    comentario_id  BIGINT NOT NULL REFERENCES foro_comentarios(id) ON DELETE CASCADE,
    usuario_id     BIGINT NOT NULL REFERENCES perfiles_cliente(usuario_id) ON DELETE CASCADE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_foro_comentario_upvote UNIQUE (comentario_id, usuario_id)
);

CREATE OR REPLACE FUNCTION fn_validar_upvote_no_propio()
RETURNS TRIGGER AS $$
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
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_foro_publicacion_upvotes_no_propio
    BEFORE INSERT ON foro_publicacion_upvotes
    FOR EACH ROW EXECUTE FUNCTION fn_validar_upvote_no_propio();

CREATE TRIGGER trg_foro_comentario_upvotes_no_propio
    BEFORE INSERT ON foro_comentario_upvotes
    FOR EACH ROW EXECUTE FUNCTION fn_validar_upvote_no_propio();



CREATE OR REPLACE FUNCTION fn_otorgar_puntos_upvote()
RETURNS TRIGGER AS $$
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
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_foro_publicacion_upvotes_puntos
    AFTER INSERT OR DELETE ON foro_publicacion_upvotes
    FOR EACH ROW EXECUTE FUNCTION fn_otorgar_puntos_upvote();

CREATE TRIGGER trg_foro_comentario_upvotes_puntos
    AFTER INSERT OR DELETE ON foro_comentario_upvotes
    FOR EACH ROW EXECUTE FUNCTION fn_otorgar_puntos_upvote();


-- =============================================================================
-- SECCIÓN 9: CUPONERA (compromiso quincenal, cupones, canjes con QR)
-- =============================================================================

CREATE TABLE compromisos_cuponera (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    comercio_id                 BIGINT NOT NULL REFERENCES perfiles_comercio(usuario_id) ON DELETE CASCADE,
    periodo_inicio               DATE NOT NULL,
    periodo_fin                  DATE NOT NULL,
    fecha_limite_ratificacion    TIMESTAMPTZ NOT NULL,
    estado                       estado_compromiso_enum NOT NULL DEFAULT 'PENDIENTE',
    fecha_ratificacion           TIMESTAMPTZ,
    created_at                   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_compromiso_cuponera_periodo UNIQUE (comercio_id, periodo_inicio),
    CONSTRAINT chk_compromiso_cuponera_periodo CHECK (periodo_fin > periodo_inicio)
);

CREATE TRIGGER trg_compromisos_cuponera_updated_at
    BEFORE UPDATE ON compromisos_cuponera
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_updated_at();

CREATE INDEX idx_compromisos_cuponera_comercio ON compromisos_cuponera (comercio_id);


CREATE TABLE cupones_cuponera (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    compromiso_id         BIGINT NOT NULL REFERENCES compromisos_cuponera(id) ON DELETE CASCADE,
    tipo_beneficio        tipo_beneficio_enum NOT NULL,
    detalle_beneficio     TEXT NOT NULL,
    costo_puntos          INTEGER NOT NULL CHECK (costo_puntos > 0),
    stock_comprometido    SMALLINT NOT NULL CHECK (stock_comprometido > 0),
    stock_canjeado        SMALLINT NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_cupones_cuponera_stock CHECK (stock_canjeado BETWEEN 0 AND stock_comprometido)
);

CREATE TRIGGER trg_cupones_cuponera_updated_at
    BEFORE UPDATE ON cupones_cuponera
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_updated_at();

CREATE INDEX idx_cupones_cuponera_compromiso ON cupones_cuponera (compromiso_id);


CREATE OR REPLACE FUNCTION fn_validar_rango_cupones_compromiso()
RETURNS TRIGGER AS $$
DECLARE
    v_compromiso_id BIGINT := COALESCE(NEW.compromiso_id, OLD.compromiso_id);
    v_cantidad      INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_cantidad FROM cupones_cuponera WHERE compromiso_id = v_compromiso_id;

    IF v_cantidad NOT BETWEEN 2 AND 4 THEN
        RAISE EXCEPTION 'El compromiso % debe tener entre 2 y 4 cupones (tiene %)', v_compromiso_id, v_cantidad;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_validar_rango_cupones_compromiso
    AFTER INSERT OR UPDATE OR DELETE ON cupones_cuponera
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION fn_validar_rango_cupones_compromiso();


CREATE TABLE canjes_cupon (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cupon_id            BIGINT NOT NULL REFERENCES cupones_cuponera(id),
    cliente_id          BIGINT NOT NULL REFERENCES perfiles_cliente(usuario_id) ON DELETE CASCADE,
    codigo_qr           TEXT NOT NULL UNIQUE DEFAULT gen_random_uuid()::text,
    puntos_costo        INTEGER NOT NULL CHECK (puntos_costo > 0), -- snapshot del costo al momento del canje
    estado              estado_canje_cupon_enum NOT NULL DEFAULT 'PENDIENTE_VALIDACION',
    fecha_canje         TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_validacion    TIMESTAMPTZ,
    fecha_cancelacion   TIMESTAMPTZ,
    fecha_vencimiento   TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_canjes_cupon_updated_at
    BEFORE UPDATE ON canjes_cupon
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_updated_at();

CREATE INDEX idx_canjes_cupon_cliente ON canjes_cupon (cliente_id);
CREATE INDEX idx_canjes_cupon_cupon ON canjes_cupon (cupon_id);

CREATE OR REPLACE FUNCTION fn_procesar_transicion_canje()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.estado = 'VALIDADO' THEN
        IF OLD.estado <> 'PENDIENTE_VALIDACION' THEN
            RAISE EXCEPTION 'El canje % no puede validarse: su estado actual es % (se requiere PENDIENTE_VALIDACION)',
                NEW.id, OLD.estado;
        END IF;

        NEW.fecha_validacion := now();
        UPDATE cupones_cuponera SET stock_canjeado = stock_canjeado + 1 WHERE id = NEW.cupon_id;

        INSERT INTO movimientos_puntos (cliente_id, tipo, puntos, referencia_tabla, referencia_id, descripcion)
        VALUES (NEW.cliente_id, 'CANJE_CUPON', -NEW.puntos_costo, 'canjes_cupon', NEW.id,
                'Canje de cupón validado por el comercio');

    ELSIF NEW.estado = 'CANCELADO' THEN
        IF OLD.estado <> 'PENDIENTE_VALIDACION' THEN
            RAISE EXCEPTION 'El canje % no puede cancelarse: su estado actual es % (se requiere PENDIENTE_VALIDACION)',
                NEW.id, OLD.estado;
        END IF;

        NEW.fecha_cancelacion := now();
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_canjes_cupon_transicion
    BEFORE UPDATE OF estado ON canjes_cupon
    FOR EACH ROW EXECUTE FUNCTION fn_procesar_transicion_canje();


-- =============================================================================
-- SECCIÓN 10: SORTEOS (compromiso de premio, tickets)
-- =============================================================================

CREATE TABLE compromisos_sorteo (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    comercio_id                 BIGINT NOT NULL REFERENCES perfiles_comercio(usuario_id) ON DELETE CASCADE,
    ciclo_inicio                 DATE NOT NULL,
    ciclo_fin                    DATE NOT NULL,
    fecha_limite_ratificacion    TIMESTAMPTZ NOT NULL,
    premio_descripcion           TEXT NOT NULL,
    estado                       estado_compromiso_enum NOT NULL DEFAULT 'PENDIENTE',
    fecha_ratificacion           TIMESTAMPTZ,
    created_at                   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_compromiso_sorteo_ciclo UNIQUE (comercio_id, ciclo_inicio),
    CONSTRAINT chk_compromiso_sorteo_ciclo CHECK (ciclo_fin > ciclo_inicio)
);

CREATE TRIGGER trg_compromisos_sorteo_updated_at
    BEFORE UPDATE ON compromisos_sorteo
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_updated_at();

CREATE INDEX idx_compromisos_sorteo_comercio ON compromisos_sorteo (comercio_id);


CREATE TABLE sorteos (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    compromiso_sorteo_id   BIGINT NOT NULL REFERENCES compromisos_sorteo(id),
    costo_puntos_ticket    INTEGER NOT NULL CHECK (costo_puntos_ticket > 0),
    estado                 estado_sorteo_enum NOT NULL DEFAULT 'ACTIVO',
    fecha_sorteo           TIMESTAMPTZ,
    ticket_ganador_id      BIGINT,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_sorteos_updated_at
    BEFORE UPDATE ON sorteos
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_updated_at();

CREATE INDEX idx_sorteos_compromiso ON sorteos (compromiso_sorteo_id);


CREATE TABLE tickets_sorteo (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sorteo_id     BIGINT NOT NULL REFERENCES sorteos(id) ON DELETE CASCADE,
    cliente_id    BIGINT NOT NULL REFERENCES perfiles_cliente(usuario_id) ON DELETE CASCADE,
    puntos_costo  INTEGER NOT NULL CHECK (puntos_costo > 0), -- snapshot del costo al comprar
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_tickets_sorteo_sorteo ON tickets_sorteo (sorteo_id);
CREATE INDEX idx_tickets_sorteo_cliente ON tickets_sorteo (cliente_id);

ALTER TABLE sorteos
    ADD CONSTRAINT fk_sorteos_ticket_ganador
    FOREIGN KEY (ticket_ganador_id) REFERENCES tickets_sorteo(id);

CREATE OR REPLACE FUNCTION fn_procesar_compra_ticket_sorteo()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO movimientos_puntos (cliente_id, tipo, puntos, referencia_tabla, referencia_id, descripcion)
    VALUES (NEW.cliente_id, 'COMPRA_TICKET_SORTEO', -NEW.puntos_costo, 'tickets_sorteo', NEW.id,
            'Compra de ticket para sorteo');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_tickets_sorteo_procesar_compra
    AFTER INSERT ON tickets_sorteo
    FOR EACH ROW EXECUTE FUNCTION fn_procesar_compra_ticket_sorteo();


CREATE OR REPLACE VIEW v_comercio_acceso_restringido AS
SELECT
    pc.usuario_id AS comercio_id,
    (
        EXISTS (
            SELECT 1 FROM compromisos_cuponera cc
            WHERE cc.comercio_id = pc.usuario_id
              AND (cc.estado = 'VENCIDO' OR (cc.estado = 'PENDIENTE' AND cc.fecha_limite_ratificacion < now()))
        )
        OR EXISTS (
            SELECT 1 FROM compromisos_sorteo cs
            WHERE cs.comercio_id = pc.usuario_id
              AND (cs.estado = 'VENCIDO' OR (cs.estado = 'PENDIENTE' AND cs.fecha_limite_ratificacion < now()))
        )
    ) AS acceso_restringido
FROM perfiles_comercio pc;

COMMENT ON VIEW v_comercio_acceso_restringido IS
    'SCRUM-93: true si el comercio dejó vencer un compromiso sin ratificar. '
    'El comercio conserva acceso de solo lectura a su perfil (se resuelve en la capa de aplicación).';


-- =============================================================================
-- SECCIÓN 11: ESTADÍSTICAS — VISUALIZACIONES
-- =============================================================================
-- Eventos crudos (no contadores), para poder calcular variación semana/mes
-- contra el período anterior (SCRUM-96) con agregaciones sobre created_at.

CREATE TABLE visualizaciones_comercio (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    comercio_id  BIGINT NOT NULL REFERENCES perfiles_comercio(usuario_id) ON DELETE CASCADE,
    usuario_id   BIGINT REFERENCES usuarios(id) ON DELETE SET NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_visualizaciones_comercio_agregacion ON visualizaciones_comercio (comercio_id, created_at);

CREATE TABLE visualizaciones_oferta (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    oferta_id   BIGINT NOT NULL REFERENCES ofertas(id) ON DELETE CASCADE,
    usuario_id  BIGINT REFERENCES usuarios(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_visualizaciones_oferta_agregacion ON visualizaciones_oferta (oferta_id, created_at);