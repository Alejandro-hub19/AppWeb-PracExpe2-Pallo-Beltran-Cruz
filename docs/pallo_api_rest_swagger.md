# Parte de Pallo — API REST en vivo + Swagger (Directriz 3)

Basado en el backend real de SGED (`SGED_APPWEB`, Spring Boot 3.2 / Java 21 / springdoc-openapi). No es una app de prueba: son endpoints reales del PFC.

## 1. Ajuste necesario en SGED_APPWEB (aplicar allá, no en este repo)

Hoy, en `backend/src/main/resources/application.yml`, la configuracion es:

```yaml
springdoc:
  api-docs:
    path: /api/docs
  swagger-ui:
    path: /api/swagger-ui.html
    operationsSorter: method
```

`/api/docs` sirve el JSON crudo de OpenAPI; la interfaz interactiva (Swagger UI) vive en `/api/swagger-ui.html`. La directriz pide literalmente "Swagger UI accesible en /api/docs", asi que hay que intercambiar las dos rutas:

```yaml
springdoc:
  api-docs:
    path: /api/docs.json
  swagger-ui:
    path: /api/docs
    operationsSorter: method
```

Con este cambio, entrar a `http://localhost:8080/api/docs` abre directamente Swagger UI (no el JSON). Es un cambio de 2 lineas en tu repo real `SGED_APPWEB` — aplicarlo ahi, volver a levantar (`make up` o reiniciar el backend) y comprobar que carga antes del laboratorio.

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

Requiere rol ADMINISTRADOR, ENTRENADOR o RECEPCIONISTA (admin cumple). Forma real de la respuesta (segun `CategoriaResponse.java`):
```json
[
  {
    "idCategoria": 1,
    "nombre": "Sub-12",
    "edadMin": 10,
    "edadMax": 12,
    "descripcion": "Categoria formativa sub-12",
    "activo": true,
    "createdAt": "2026-03-01T10:00:00Z"
  }
]
```
Los valores son de ejemplo — al ejecutarlo de verdad va a devolver lo que tengan cargado en su base.

**3) POST /api/asistencias/qr/sesion/{idSesion}/token** — la pieza distintiva del sistema (emision de token QR para asistencia)

Requiere rol ADMINISTRADOR o RECEPCIONISTA, y un `idSesion` real que ya exista (de una sesion de entrenamiento creada de antemano). Vale la pena mostrar este porque no es CRUD generico.

Alternativa mas simple si no quieren depender de tener una sesion de entrenamiento ya creada: cambiar el endpoint 3 por `POST /api/categorias` (crear una categoria nueva) — mismo espiritu de "endpoint que escribe", sin depender de datos previos.

Opcional / avanzado (no como uno de los 3 obligatorios, pero impresiona si sale bien): completar el flujo QR con `POST /api/asistencias/qr/marcar` desde una sesion de ESTUDIANTE aparte — necesita dos usuarios logueados a la vez (dos entornos de Postman), mas riesgoso de ensayar sin fallar en vivo.

## 3. Checklist de ensayo (la directriz dice "no improvisados")

- [ ] Aplicado el cambio de rutas de Swagger en `application.yml` (en SGED_APPWEB)
- [ ] `make up` corrido ANTES del laboratorio, stack completo arriba
- [ ] `http://localhost:8080/api/docs` abre Swagger UI (no el JSON)
- [ ] Coleccion de Postman/Insomnia guardada con las 3 requests, cookies funcionando
- [ ] Ensayado el orden login -> categorias/activas -> qr/token (o categorias POST), de corrido, sin errores
- [ ] Plan B si algun endpoint falla (reintentar login, revisar que el idSesion exista)
