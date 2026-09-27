CREATE EXTENSION IF NOT EXISTS postgis;

ALTER TABLE perfiles_comercio
    ADD COLUMN IF NOT EXISTS direccion VARCHAR(255),
    ADD COLUMN IF NOT EXISTS descripcion TEXT,
    ADD COLUMN IF NOT EXISTS ubicacion GEOGRAPHY(POINT, 4326);

CREATE INDEX IF NOT EXISTS idx_perfiles_comercio_ubicacion
    ON perfiles_comercio USING GIST (ubicacion);

CREATE TABLE IF NOT EXISTS horarios_atencion (
                                                 id BIGSERIAL PRIMARY KEY,
                                                 comercio_id BIGINT NOT NULL REFERENCES perfiles_comercio(usuario_id) ON DELETE CASCADE,
    dia_semana SMALLINT NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_horarios_atencion_comercio ON horarios_atencion(comercio_id);