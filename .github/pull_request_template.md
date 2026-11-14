## Qué cambia y por qué

<!-- Una o dos frases. Enlace al issue si existe: "Cierra #12". -->

## Cómo probarlo

<!-- Pasos para que quien revisa lo vea funcionar. -->

## Checklist

- [ ] El CI pasa (backend, frontend y scripts)
- [ ] Las rutas nuevas quedan protegidas con JWT, o la ruta pública está justificada en `RutasPublicas`
- [ ] Las rutas protegidas filtran por `usuario.id()` y responden 404 para recursos de otro usuario
- [ ] Migración de Flyway nueva (nunca se edita una ya aplicada)
- [ ] Pruebas para la lógica nueva
- [ ] Documentación o ADR actualizada si cambia una decisión
- [ ] Sin secretos en el código (`.env` nunca se sube)
