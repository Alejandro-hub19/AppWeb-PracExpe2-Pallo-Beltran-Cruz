# Revisión entre pares — Directriz 2 (Beltrán)

Hallazgos de la verificación en vivo del SGED realizada por Beltrán como parte de la
actividad de revisión entre pares de la Práctica Experimental Unidad IV. A diferencia de
`OBSERVACIONES.md` (que registra observaciones del docente sobre el equipo SGED en sus
propias entregas), esta tabla documenta hallazgos de un revisor externo al equipo, sobre el
sistema ya entregado, con el mismo estándar de evidencia trazable.

| Código | Módulo | Hallazgo | Severidad | Causa raíz confirmada | Evidencia |
|---|---|---|---|---|---|
| REV-01 | Alta de usuario | El mensaje de error para cualquier `422` es un texto fijo que siempre culpa a cédula/fecha, sin importar cuál campo falló realmente | Media (UX) | `crear-usuario.component.ts`, método `mensajeDeError(status)`: `switch` sin distinción por campo | Reproducido: cédula y fecha válidas, error real era formato de "Usuario" (no es un correo) |
| REV-02 | `GET /api/categorias` | `sort` con columna inexistente produce `500` en vez de `400` | Media (robustez) | `CategoriaController.listarPaginado` vincula `Pageable` sin validar contra columnas reales de la entidad | Swagger: `sort=noExisteEstaColumna` → `status: 500`; test de integración `CategoriaSortValidationIntegrationTest` |
| REV-03 | Categorías (frontend) | CRUD completo existe en backend; no hay pantalla de administración | Media (funcionalidad incompleta) | Frontend solo consume `GET /api/categorias/activas` para un `<select>`; sin ruta ni componente para el CRUD completo | Búsqueda en `frontend/src/app`: cero referencias a `POST/PUT/DELETE /api/categorias` |
| REV-04 | Alta de usuario | Etiqueta "Usuario (correo de acceso)" ambigua; sin validación de formato visible antes de enviar | Baja (UX) | `RegisterRequest.username` es `@Email` obligatorio; el campo del formulario no tiene `type="email"` ni validación en cliente | Reproducido en vivo |
| REV-05 | Alta de Entrenador | Crear usuario con rol `ENTRENADOR` no habilita el módulo de Sesiones: falta un segundo registro en `deportivo.entrenadores`, vinculado por `@OneToOne` obligatorio a `Usuario`, sin flujo ni pantalla que lo conecte | **Alta** (funcionalidad crítica) | `Entrenador.java`: relación `@OneToOne(nullable = false)` hacia `Usuario` y `Persona`; `EntrenadorController`: `POST /api/entrenadores` restringido a `ADMINISTRADOR`, sin vista en frontend | Reproducido de punta a punta: alta de usuario → bloqueo confirmado ("No hay un entrenador asociado a esta cuenta") → vínculo creado vía `POST /api/entrenadores` en Swagger → módulo de Sesiones verificado funcional tras el fix |

## Nota metodológica

Cada hallazgo se verificó en un entorno levantado desde una clonación limpia
(`docker compose up -d --build`), no sobre un ambiente pre-configurado, para asegurar que el
comportamiento observado es el que tendría cualquier usuario nuevo del sistema. REV-05 se
llevó deliberadamente hasta confirmar tanto la causa raíz como la solución (no solo el
síntoma), replicando la metodología de diagnóstico usada por el propio equipo SGED en su
tabla `OBSERVACIONES.md`.