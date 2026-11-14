-- Módulo identidad: cuentas, sesiones (refresh tokens), política de datos y consentimientos.
-- Cada módulo tiene su propio esquema; ningún módulo lee las tablas de otro.
create schema if not exists identidad;

create table identidad.politicas_datos (
    version      varchar(20)  primary key,
    url_texto    varchar(200) not null,
    contenido    text         not null,
    vigente      boolean      not null default false,
    publicada_en timestamptz  not null default now()
);
-- Solo una política puede estar vigente a la vez
create unique index politicas_datos_una_vigente on identidad.politicas_datos (vigente) where vigente;

create table identidad.usuarios (
    id                   uuid         primary key,
    nombre_completo      varchar(120) not null,
    -- 10 dígitos, móvil colombiano (empieza por 3)
    celular              varchar(10)  not null,
    -- en minúsculas; opcional
    correo               varchar(254),
    -- "{bcrypt}$2a$10$..." (nunca la contraseña)
    contrasena_hash      varchar(255) not null,
    -- código DIVIPOLA de 5 dígitos; los 2 primeros son el departamento
    municipio_residencia varchar(5)   not null,
    rol                  varchar(20)  not null default 'PRODUCTOR',
    intentos_fallidos    integer      not null default 0,
    bloqueado_hasta      timestamptz,
    creado_en            timestamptz  not null,
    constraint usuarios_celular_key unique (celular),
    constraint usuarios_correo_key unique (correo),
    constraint usuarios_celular_formato check (celular ~ '^3[0-9]{9}$'),
    constraint usuarios_municipio_formato check (municipio_residencia ~ '^[0-9]{5}$'),
    constraint usuarios_rol_valido check (rol in ('PRODUCTOR', 'ADMIN'))
);

create table identidad.consentimientos (
    id               uuid        primary key,
    usuario_id       uuid        not null references identidad.usuarios (id) on delete cascade,
    version_politica varchar(20) not null references identidad.politicas_datos (version),
    aceptado_en      timestamptz not null
);
create index consentimientos_usuario on identidad.consentimientos (usuario_id);

-- Un refresh token emitido. Solo se guarda su hash SHA-256 (64 caracteres hexadecimales).
create table identidad.sesiones (
    id          uuid        primary key,
    usuario_id  uuid        not null references identidad.usuarios (id) on delete cascade,
    -- todos los refresh tokens de un mismo inicio de sesión comparten familia (rotación)
    familia     uuid        not null,
    hash_token  varchar(64) not null unique check (hash_token ~ '^[0-9a-f]{64}$'),
    creada_en   timestamptz not null,
    expira_en   timestamptz not null,
    usada_en    timestamptz,
    revocada_en timestamptz
);
create index sesiones_familia on identidad.sesiones (familia);
create index sesiones_usuario on identidad.sesiones (usuario_id);
