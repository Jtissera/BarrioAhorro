CREATE TABLE premios_sorteo (
    ticket_id    BIGINT PRIMARY KEY REFERENCES tickets_sorteo(id) ON DELETE CASCADE,
    sorteo_id    BIGINT NOT NULL REFERENCES sorteos(id) ON DELETE CASCADE,
    cliente_id   BIGINT NOT NULL REFERENCES perfiles_cliente(usuario_id) ON DELETE CASCADE,
    token        UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    vence_en     TIMESTAMPTZ NOT NULL,
    visto_en     TIMESTAMPTZ,
    canjeado_en  TIMESTAMPTZ,
    canjeado_por BIGINT REFERENCES perfiles_comercio(usuario_id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_premios_sorteo_sorteo UNIQUE (sorteo_id),
    CONSTRAINT chk_premios_canje CHECK ((canjeado_en IS NULL) = (canjeado_por IS NULL))
);

CREATE INDEX idx_premios_sorteo_pendientes ON premios_sorteo (cliente_id) WHERE visto_en IS NULL;

-- Ganadores que ya existían (por ejemplo, de tus pruebas).
INSERT INTO premios_sorteo (ticket_id, sorteo_id, cliente_id, vence_en)
SELECT t.id, t.sorteo_id, t.cliente_id, now() + interval '30 days'
FROM tickets_sorteo t
WHERE t.estado = 'GANADOR'
ON CONFLICT DO NOTHING;