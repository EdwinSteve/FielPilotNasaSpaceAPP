-- Módulo rotacion: las rotaciones calculadas para cada lote y el ciclo que eligió el agricultor.
create schema if not exists rotacion;

-- Las 3 mejores rotaciones de un cálculo, con el ranking y la lluvia esperada (JSON). Se guardan para
-- que la elección del agricultor apunte a un plan que no cambia aunque se recalcule.
create table rotacion.planes (
    id               uuid        primary key,
    -- si la cuenta o el terreno se borran, sus planes también (Ley 1581 de 2012)
    usuario_id       uuid        not null references identidad.usuarios (id) on delete cascade,
    terreno_id       uuid        not null references fincas.terrenos (id) on delete cascade,
    -- la recomendación del modelo de la que salió (sin llave foránea: cada módulo es dueño de sus datos)
    recomendacion_id uuid        not null,
    creado_en        timestamptz not null,
    contenido_json   text        not null
);
create index planes_terreno on rotacion.planes (terreno_id, usuario_id, creado_en desc);

-- El ciclo activo de cada lote: uno por lote; elegir otro lo reemplaza.
create table rotacion.selecciones (
    terreno_id  uuid         primary key references fincas.terrenos (id) on delete cascade,
    usuario_id  uuid         not null references identidad.usuarios (id) on delete cascade,
    plan_id     uuid         not null references rotacion.planes (id) on delete cascade,
    -- id estable de la rotación dentro del plan ("maiz-frijol-sorgo")
    rotacion_id varchar(200) not null,
    -- nombres de los cultivos en orden, separados por " → "
    secuencia   varchar(300) not null,
    elegida_en  timestamptz  not null
);
