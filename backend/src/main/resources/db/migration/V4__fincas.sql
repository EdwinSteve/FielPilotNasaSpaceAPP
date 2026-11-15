-- Módulo fincas: los terrenos (lotes) del productor, dibujados sobre el mapa.
-- Cada módulo tiene su propio esquema; ningún módulo lee las tablas de otro.
create schema if not exists fincas;

create table fincas.terrenos (
    id              uuid             primary key,
    -- dueño del terreno. Si la cuenta se borra, sus terrenos también (Ley 1581 de 2012)
    usuario_id      uuid             not null references identidad.usuarios (id) on delete cascade,
    nombre          varchar(60)      not null,
    -- código DIVIPOLA de 5 dígitos; los 2 primeros son el departamento
    municipio       varchar(5)       not null,
    riego           boolean          not null default false,
    -- polígono en GeoJSON (RFC 7946: [longitud, latitud], anillo cerrado). Listo para PostGIS:
    -- ST_GeomFromGeoJSON(poligono_geojson)
    poligono_geojson text            not null,
    numero_vertices integer          not null,
    area_ha         double precision not null,
    -- punto medio del polígono: el que usan el diagnóstico y el planificador estacional
    centroide_lat   double precision not null,
    centroide_lon   double precision not null,
    creado_en       timestamptz      not null,
    constraint terrenos_municipio_formato check (municipio ~ '^[0-9]{5}$'),
    constraint terrenos_vertices check (numero_vertices between 3 and 200),
    constraint terrenos_area_positiva check (area_ha > 0),
    constraint terrenos_centroide_colombia check (centroide_lat between -4.3 and 13.6
                                                  and centroide_lon between -82.0 and -66.8)
);
create index terrenos_usuario on fincas.terrenos (usuario_id, creado_en);
