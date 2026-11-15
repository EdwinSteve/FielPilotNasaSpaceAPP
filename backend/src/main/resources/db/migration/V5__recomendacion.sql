-- Módulo recomendacion: las recomendaciones calculadas por el planificador estacional para cada terreno.
-- Se guarda la respuesta completa del planificador (JSON) para volver a mostrarla sin recalcular y para
-- saber de qué recomendación salió cada rotación.
create schema if not exists recomendacion;

create table recomendacion.recomendaciones (
    id             uuid        primary key,
    -- si la cuenta o el terreno se borran, sus recomendaciones también (Ley 1581 de 2012)
    usuario_id     uuid        not null references identidad.usuarios (id) on delete cascade,
    terreno_id     uuid        not null references fincas.terrenos (id) on delete cascade,
    creada_en      timestamptz not null,
    -- la fecha que pidió el productor; null si se compararon las del próximo mes
    siembra_pedida date,
    -- "bf" (oráculo sobre el pronóstico) o "red"; y el bundle del modelo
    motor          varchar(10) not null,
    modelo         varchar(60) not null,
    plan_json      text        not null
);
create index recomendaciones_terreno on recomendacion.recomendaciones (terreno_id, usuario_id, creada_en desc);
