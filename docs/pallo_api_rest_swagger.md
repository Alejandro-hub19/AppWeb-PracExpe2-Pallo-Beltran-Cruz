# Parte de Pallo — API REST en vivo + Swagger (Directriz 3)

Basado en el backend real de SGED (`SGED_APPWEB`, Spring Boot 3.2 / Java 21 / springdoc-openapi). No es una app de prueba: son endpoints reales del PFC.

## 1. Ajuste de Swagger — YA APLICADO y verificado en vivo el 2026-08-09

Antes, en `backend/src/main/resources/application.yml`, `/api/docs` servia el JSON crudo de OpenAPI y la interfaz interactiva (Swagger UI) vivia en `/api/swagger-ui.html`. La directriz pide literalmente "Swagger UI accesible en /api/docs", asi que se intercambiaron las dos rutas:

```yaml
springdoc:
  api-docs:
    path: /api/docs.json
  swagger-ui:
    path: /api/docs
    operationsSorter: method
```

Ese cambio solo, por si solo, dejo `/api/docs.json` bloqueado con 401 — `SecurityConfig.java` tenia el permitAll apuntando al nombre de ruta viejo. Segundo ajuste, tambien ya aplicado:

```java
// backend/src/main/java/org/uteq/backend/config/SecurityConfig.java (linea ~60)
.requestMatchers("/api/docs/**", "/api/docs.json", "/api/swagger-ui/**",
        "/api/swagger-ui.html", "/swagger-ui/**",
        "/v3/api-docs/**").permitAll()
```

Verificado en vivo contra el backend real corriendo en `localhost:8080`:
- `GET /api/docs` -> 302 -> `/api/swagger-ui/index.html` (200, text/html) — Swagger UI carga bien
- `GET /api/docs.json` -> 200, application/json — el spec crudo sigue publico para quien lo necesite (Postman puede importarlo)

Nota sobre `make`: esta maquina no tiene GNU Make instalado, asi que `make up` del README no funciona tal cual. Usar directo:
```bash
docker compose up -d --build
```
(mismo efecto que el objetivo `up` del Makefile, menos el mensaje bonito de bienvenida al final). Si prefieren tener `make`, se instala aparte (choco install make, o WSL).

## 2. Los 3 endpoints para la demo en vivo

Orden recomendado (el login va primero: la sesion viaja en cookie HttpOnly, no en un token que se pega a mano en cada request):

**1) POST /api/auth/login** — abre la sesion

Body:
```json
{
  "username": "admin",
  "password": "Admin2026!"
}
```
Credenciales semilla de `db/seed.sql` (ver README de SGED_APPWEB). Postman guarda la cookie solo si el manejo de cookies esta activo (por defecto lo esta). Ojo: hay bloqueo por IP tras varios intentos fallidos seguidos (15 min) — no ensayar fallando el login a proposito.

**2) GET /api/categorias/activas** — lectura simple, con datos reales

Requiere rol ADMINISTRADOR, ENTRENADOR o RECEPCIONISTA (admin cumple). Respuesta real, capturada en vivo el 2026-08-09:
```json
[
  {"idCategoria":1,"nombre":"SUB-12","edadMin":10,"edadMax":12,"descripcion":"Categoría sub-12","activo":true,"createdAt":"2026-08-03T04:44:33.481682Z"},
  {"idCategoria":2,"nombre":"SUB-14","edadMin":12,"edadMax":14,"descripcion":"Categoría sub-14","activo":true,"createdAt":"2026-08-03T04:44:33.481682Z"},
  {"idCategoria":3,"nombre":"SUB-16","edadMin":14,"edadMax":16,"descripcion":"Categoría sub-16","activo":true,"createdAt":"2026-08-03T04:44:33.481682Z"}
]
```

**3) POST /api/categorias** — endpoint que escribe (crear una categoria)

Se probo tambien en vivo: crea con 201 y el `idCategoria` real asignado. Este es el recomendado como tercer endpoint por sobre el de QR — ver nota abajo.

```json
{"nombre":"SUB-18","edadMin":16,"edadMax":18,"descripcion":"Categoria sub-18","activo":true}
```

Nota sobre el endpoint QR (`POST /api/asistencias/qr/sesion/{idSesion}/token`): probado tambien, pero `GET /api/sesiones/hoy` devolvio `[]` — hoy no hay ninguna sesion de entrenamiento sembrada en la base, asi que no hay un `idSesion` valido para usar todavia. Si quieren usar el QR como tercer endpoint en vez de la categoria (es mas vistoso), hay que crear una sesion de entrenamiento antes del laboratorio (`POST /api/sesiones`) para tener un `idSesion` real a mano. Si no, `POST /api/categorias` ya esta verificado y no depende de nada mas.

Importante para el ensayo: si prueban crear una categoria de prueba, bórrenla despues (`DELETE /api/categorias/{id}`) para no dejar basura en los datos que van a mostrarle al docente — asi se hizo aqui (se creo "TEST-BORRAR" con id 4 y se elimino de inmediato; la base quedo con las mismas 3 categorias de siempre).

## 3. Coleccion de Postman

SGED ya tenia su propia coleccion en [`docs/postman/coleccion.json`](postman/coleccion.json) (30 requests, la usan para las pruebas OWASP/bench). Se le agrego una carpeta nueva **arriba de todo**, `00 Demo Unidad IV (Pallo) - Directriz 3`, con exactamente los 3 requests de este documento ya armados (login, categorias/activas, crear categoria) — no hace falta armar nada a mano, solo importar el archivo en Postman/Insomnia y correr esa carpeta en orden. De paso se corrigio el request viejo "OpenAPI 3.0" para que apunte a `/api/docs.json` (la ruta cambio con el fix de arriba).

Variable que usa la coleccion: `base_url` = `http://localhost:8080` (ya viene configurada).

## 4. Checklist de ensayo (la directriz dice "no improvisados")

- [x] Cambio de rutas de Swagger en `application.yml` — aplicado y verificado en vivo
- [x] Fix de `SecurityConfig.java` para que `/api/docs.json` no quede bloqueado — aplicado y verificado
- [x] `http://localhost:8080/api/docs` abre Swagger UI (no el JSON) — confirmado
- [x] Los 3 endpoints (login, categorias/activas, categorias POST) probados en vivo con curl, con datos reales
- [x] Coleccion de Postman lista para importar, con la carpeta de demo ya armada
- [ ] **Pendiente de tu lado:** los 2 fixes de arriba (`application.yml` y `SecurityConfig.java`) estan aplicados SOLO como cambios locales sin commitear en tu repo real `SGED_APPWEB` — no los commiteo yo porque me pediste no tocar ese repo. Si no los subes tu mismo antes del laboratorio, y algo reinicia esa carpeta (otra maquina, un `git stash`, etc.), el fix desaparece y `/api/docs` vuelve a servir el JSON en vez de Swagger UI.
- [ ] Importar `docs/postman/coleccion.json` en Postman/Insomnia y correr la carpeta `00 Demo Unidad IV` una vez de principio a fin, en tu maquina, antes del laboratorio
- [ ] Decidir si el 3er endpoint se queda en `POST /api/categorias` (ya probado) o cambian al QR (falta crear una sesion de entrenamiento primero)
- [ ] `.env` copiado (`cp .env.example .env`) y stack arriba con `docker compose up -d --build` (`make` no esta instalado en esta maquina) ANTES del laboratorio
