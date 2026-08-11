# Parte de Pallo — Consumo de API REST externa con caché y manejo de errores (Paso 3)

Implementa el **Paso 3** de la guía oficial PE-U4 y aporta material para la sección **5.3** del informe (Consumo de Servicios Web Externos).

Criterio de verificación de la guía: *"La API externa se muestra en la interfaz del PFC. El Redis almacena la respuesta cacheada (verificar con `redis-cli KEYS`). Los errores de red se manejan elegantemente (mensaje amigable al usuario)."* — los tres puntos quedan verificados abajo con salidas reales.

## 1. Qué API externa y por qué esa

**Open-Meteo** (`https://api.open-meteo.com/v1/forecast`), pronóstico meteorológico horario.

Dos razones concretas, no de relleno:

1. **Pertinencia de dominio.** El SGED gestiona una escuela de fútbol formativo: los entrenamientos son en cancha abierta y la lluvia es la razón real por la que una sesión se reprograma. El dato consumido se usa para decidir algo, no para cumplir un requisito. La guía misma pone "datos meteorológicos: 10 min" como ejemplo de TTL por dominio.
2. **Sin clave de API.** Es pública y gratuita. El sistema levanta completo desde una clonación limpia sin depender de un secreto que un evaluador no tiene. Este criterio pesó: el módulo de IA del proyecto (Gemini) sí exige clave y por eso viene deshabilitado por defecto, lo que lo vuelve imposible de demostrar en vivo.

## 2. Aclaración sobre el hallazgo REV-06 de Beltrán

`docs/observaciones/REVISION-BELTRAN.md` afirma en REV-06 que el backend no consume ninguna API externa, con evidencia *"cero ocurrencias de `RestTemplate` o `WebClient`"*.

**El hallazgo es inexacto en su premisa, pero correcto en su conclusión práctica.** El proyecto sí tenía un cliente HTTP externo antes de este trabajo: `common/ia/GeminiFeedbackService.java` consume la API de Gemini usando **`RestClient`**, el cliente introducido en Spring Framework 6.1 que reemplaza a `RestTemplate`. La búsqueda no lo encontró porque buscó los dos nombres antiguos.

Dicho eso, ese consumo **no cumplía el Paso 3**: no cachea en Redis, y viene deshabilitado por defecto sin clave, así que no era demostrable. Lo que se implementa aquí sí cumple el paso completo.

## 3. Diseño

| Pieza | Archivo | Responsabilidad |
|---|---|---|
| Cliente HTTP | `backend/.../common/clima/OpenMeteoClient.java` | Solo la llamada, con timeout. Propaga el fallo. |
| Cache-aside + degradación | `backend/.../common/clima/ClimaService.java` | Redis → miss → API externa → guardar. Decide qué hacer ante fallos. |
| DTOs | `backend/.../common/clima/dto/ClimaDtos.java` | `PronosticoEntrenamiento` (lo cacheado) y la envoltura de respuesta. |
| Endpoint | `backend/.../common/clima/ClimaController.java` | `GET /api/clima/entrenamiento` |
| Interfaz | `frontend/.../entrenador/sesiones.component.ts` | Tarjeta "Clima en la cancha" en *Mis sesiones*. |
| Pruebas | `backend/src/test/.../common/clima/ClimaServiceTest.java` | 6 pruebas, sin salir a internet. |

### Por qué cache-aside explícito y no `@Cacheable`

El resto del proyecto usa `@Cacheable` (ver `EstudianteService`). Aquí se escribió el patrón a mano, a propósito, por dos razones que la anotación no permite resolver:

1. **Un fallo del proveedor no debe cachearse.** Con `@Cacheable`, la respuesta de degradación quedaría guardada y el sistema seguiría respondiendo "no disponible" durante los 10 minutos del TTL aunque el proveedor se recuperara al segundo siguiente.
2. **La respuesta declara su origen** (`api-externa` / `cache`), y eso solo se puede decidir donde se sabe si hubo acierto de caché. Además hace el caché demostrable en vivo.

### TTL: 10 minutos

No es arbitrario. El pronóstico horario de Open-Meteo no se recalcula más rápido que eso, así que pedirlo más seguido no traería información nueva, solo consumo de cuota. Cachear cumple los tres motivos que la guía enumera: evitar el *rate limiting*, reducir latencia y proteger contra la indisponibilidad del servicio externo.

### Si Redis falla

Redis es caché, no fuente de verdad. Sus errores se registran y el flujo continúa hacia la API externa: el sistema pierde velocidad, no funcionalidad. Esto responde directamente a la pregunta *"¿Qué pasaría si Redis falla?"* de la sección 5.2 de la guía (preguntas frecuentes del docente).

## 4. Evidencia real (verificada el 2026-08-11)

Entorno: `docker compose up -d --build backend` (Postgres + Redis + backend), autenticado como `admin`.

**Redis antes de la primera llamada** — vacío:
```
$ docker exec sged_redis redis-cli KEYS "clima:*"
(sin resultados)
```

**Primera llamada — fallo de caché, va a la API externa:**
```
GET /api/clima/entrenamiento
{"disponible":true,"origen":"api-externa","motivo":null,
 "pronostico":{"ubicacion":"Quevedo, Los Rios","fecha":"2026-08-11",
 "desde":"15:00:00","hasta":"19:00:00","temperaturaMaxC":29.7,
 "probabilidadLluviaMax":14,"precipitacionTotalMm":0.2,
 "recomendacion":"Condiciones aptas para entrenar en cancha abierta.",
 "consultadoEn":"2026-08-11T01:06:57.441010753Z"}}
HTTP 200 — 1.310 s
```

**Redis después — criterio de verificación del Paso 3:**
```
$ docker exec sged_redis redis-cli KEYS "clima:*"
clima:entrenamiento:2026-08-11:15:00-19:00

$ docker exec sged_redis redis-cli TTL clima:entrenamiento:2026-08-11:15:00-19:00
598          # 10 min menos los 2 s transcurridos
```

**Segunda llamada — servida desde caché:**
```
{"disponible":true,"origen":"cache", ... mismos datos, mismo consultadoEn ...}
HTTP 200 — 0.032 s
```

### Tabla de rendimiento con y sin caché

Sirve directamente para el punto *"[2 min] Métricas: tabla comparativa de rendimiento con y sin caché"* de la defensa oral (sección 5.1 de la guía).

| Escenario | Origen | Tiempo de respuesta | Salida a internet |
|---|---|---|---|
| Primera llamada (caché vacío) | `api-externa` | 1310 ms | Sí |
| Segunda llamada (dentro del TTL) | `cache` | 32 ms | No |
| **Mejora** | | **≈41× más rápido** | **1 llamada evitada** |

### Manejo de errores, contra un fallo real

Se forzó un 4xx real del proveedor pidiendo una fecha fuera de su rango:
```
GET /api/clima/entrenamiento?fecha=2029-12-31
{"disponible":false,"origen":"no-disponible",
 "motivo":"El servicio de clima no responde en este momento","pronostico":null}
HTTP 200
```
Responde **200, no 5xx**: un proveedor externo caído no es un error del SGED. La interfaz muestra el motivo y la pantalla de sesiones sigue siendo utilizable. Mismo criterio que ya usaba `ResultadoFeedback.noDisponible` en el módulo de IA.

**Y el fallo no se cacheó:**
```
$ docker exec sged_redis redis-cli EVAL "return #redis.call('keys','clima:*2029*')" 0
0            # ninguna clave del intento fallido
```

### Claves separadas por franja horaria
```
clima:entrenamiento:2026-08-11:15:00-19:00
clima:entrenamiento:2026-08-11:08:00-10:00
```
Dos sesiones el mismo día en horarios distintos tienen pronósticos distintos y no comparten entrada de caché.

### Publicado en OpenAPI
```
GET /api/clima/entrenamiento
parámetros: ['fecha', 'desde', 'hasta']
```
Visible en Swagger UI (`/api/docs`), sin anotaciones manuales: springdoc lo genera del mapeo, como el resto de la API.

## 5. Pruebas automatizadas

`ClimaServiceTest` — 6 pruebas, ninguna sale a internet (el cliente está mockeado). No verifican que Open-Meteo funcione (eso no es responsabilidad del SGED) sino las decisiones propias:

- Fallo de caché consulta la API externa, la interpreta y guarda el resultado.
- Acierto de caché **no** vuelve a llamar a la API externa.
- Proveedor caído → no disponible, y **no** se cachea el fallo.
- Redis caído → degrada a fallo de caché y sigue sirviendo desde la API externa.
- Respuesta externa incompleta → no disponible, sin propagar excepción.
- La clave de caché distingue franjas del mismo día.

**Suite completa del backend tras el cambio: `Tests run: 211, Failures: 0, Errors: 0 — BUILD SUCCESS`** (cumple con holgura el mínimo de 10 pruebas del OE1).

El frontend compila limpio con plantillas estrictas: `Application bundle generation complete`, el chunk `sesiones-component` pasa de 9.02 kB a 11.88 kB con la tarjeta incluida.

## 6. Cómo reproducirlo

```bash
cp .env.example .env    # ajustar DB_URL a la opción de Docker Compose local
docker compose up -d --build
```

```bash
curl -s -c cookies.txt -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"Admin2026!"}'
```

```bash
curl -s -b cookies.txt "http://localhost:8080/api/clima/entrenamiento"
```

```bash
docker exec sged_redis redis-cli KEYS "clima:*"
```

En la interfaz: entrar como entrenador y abrir **Mis sesiones**; la tarjeta "Clima en la cancha" aparece arriba, con una insignia que indica si el dato vino de la API externa o del caché.

## 7. Pendiente

- Este trabajo está en el repo de la práctica. Para que forme parte del PFC real hay que portarlo a `SGED_APPWEB` (son 5 archivos nuevos y 4 modificados, todos listados en la tabla de la sección 3).
- Ver la tarjeta en el navegador requiere una cuenta con rol `ENTRENADOR` vinculada en `deportivo.entrenadores` — ver el hallazgo REV-05 de Beltrán sobre ese flujo.
