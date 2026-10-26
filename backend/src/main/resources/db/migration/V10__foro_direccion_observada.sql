-- Texto de la dirección o intersección que escribió el vecino al publicar.
-- Se sigue geocodificando a ubicacion_observada; las publicaciones previas quedan en NULL.
ALTER TABLE foro_publicaciones
    ADD COLUMN direccion_observada TEXT;

COMMENT ON COLUMN foro_publicaciones.direccion_observada IS
    'Dirección o intersección escrita por el autor. Se muestra en el chip de ubicación; NULL si no indicó ninguna.';
