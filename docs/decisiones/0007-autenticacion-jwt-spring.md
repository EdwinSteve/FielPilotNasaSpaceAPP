# 0007: Backend en Java con Spring Boot y autenticación JWT

- Estado: aceptada
- Fecha: 2026-11-14
- Reemplaza: el backend FastAPI del repositorio anterior (Nasa_2026_Nameless)

## Contexto
El equipo va a desarrollar el backend en Java con Spring Boot. Cada integrante construirá un módulo (fincas, diagnóstico, rotación, alertas...) y todos necesitan proteger sus rutas de la misma forma, sin reimplementar la autenticación. El frontend ya maneja access token + refresh token y renueva la sesión ante un 401.

## Decisión
- **Spring Boot 4.1 con Java 21**, monolito modular con arquitectura hexagonal (ver [arquitectura.md](../arquitectura.md)). `ArquitecturaTest` (ArchUnit) hace cumplir las reglas.
- **Validación del JWT con el OAuth2 Resource Server de Spring Security** en lugar de un filtro propio. Es código mantenido y probado por Spring, y valida firma, vencimiento, emisor y audiencia antes de llegar al controlador.
- **Seguro por defecto**: toda ruta exige token salvo la lista explícita de `RutasPublicas`. Los controladores reciben `@AuthenticationPrincipal UsuarioAutenticado` y piden roles con `@PreAuthorize`.
- **Access token**: JWT HS256 de 15 minutos con `sub`, `rol`, `iss`, `aud`, `iat`, `exp` y `jti`, sin datos personales.
- **Refresh token**: opaco, de 30 días y un solo uso. En la base va solo su hash SHA-256. Se rota en cada uso, y si llega un token ya usado se revoca toda la familia (detección de robo).
- **Contraseñas** con BCrypt mediante el codificador delegante de Spring, que permite migrar de algoritmo. Bloqueo de 15 minutos tras 5 fallos seguidos.
- **HS256 con un secreto compartido** porque hoy una sola aplicación emite y valida. La API no arranca sin `JWT_SECRETO` de 64+ caracteres.
- El modelo predictivo y el servicio de datos siguen en Python (`ml/`); los módulos Java los consumirán por medio de adaptadores.

**Alternativas descartadas:**
- *Filtro JWT propio con JJWT*: más código propio que mantener y probar, sin ganancia.
- *Keycloak u otro servidor de identidad*: un servicio más que desplegar, y su login no se adapta al registro por celular con consentimiento de la Ley 1581.
- *Sesiones de servidor con cookie*: el frontend en Next.js y una futura app móvil se integran más simple con tokens.

## Consecuencias
- Un módulo nuevo queda protegido sin configurar nada; el README del backend explica cómo leer el usuario y exigir roles.
- Cerrar sesión no invalida el access token al instante: puede durar hasta 15 minutos. Por eso es corto.
- Si otro servicio tuviera que validar los tokens, habría que pasar a RS256 (clave privada para firmar, pública para validar) y publicar un JWKS.
- Pendiente antes de producción: límite de peticiones en login y registro, refresh token en cookie `httpOnly` y HTTPS (ver backend/README.md).
