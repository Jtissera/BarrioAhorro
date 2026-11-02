CREATE TABLE confirmaciones_canje (
    canje_id         BIGINT PRIMARY KEY REFERENCES canjes_cupon(id) ON DELETE CASCADE,
    cliente_id       BIGINT NOT NULL REFERENCES perfiles_cliente(usuario_id) ON DELETE CASCADE,
    recibido         BOOLEAN NOT NULL,
    puntos_otorgados INTEGER NOT NULL DEFAULT 0 CHECK (puntos_otorgados >= 0),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE reportes_canje (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    canje_id    BIGINT NOT NULL UNIQUE REFERENCES canjes_cupon(id) ON DELETE CASCADE,
    cliente_id  BIGINT NOT NULL REFERENCES perfiles_cliente(usuario_id) ON DELETE CASCADE,
    motivo      TEXT NOT NULL CHECK (motivo IN ('PEDIDO_INCOMPLETO', 'CALIDAD', 'HORARIO', 'OTRO')),
    descripcion TEXT NOT NULL CHECK (length(btrim(descripcion)) BETWEEN 1 AND 300),
    estado      TEXT NOT NULL DEFAULT 'ABIERTO' CHECK (estado IN ('ABIERTO', 'RESUELTO')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_reportes_canje_cliente ON reportes_canje(cliente_id);