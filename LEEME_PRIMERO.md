# Nota de origen y autoria

Todo lo que ves en la raiz de este repo (backend/, frontend/, db/, docs/, etc.) es una **copia (snapshot)** del proyecto real `SGED_APPWEB`, sincronizada por ultima vez el 2026-08-09 para la Practica Experimental Unidad IV. No es un proyecto nuevo ni escrito desde cero aqui — ver [`README.md`](README.md) para que es especificamente este repo.

**Repositorio original, con el historial de commits real:**
https://github.com/DarwinSM21/SGED_APPWEB

**Autoria real del proyecto:** por ser una copia plana (sin el historial `.git` original), el historial de *este* repo va a mostrar todo bajo una sola cuenta. Eso no refleja quien escribio que. La autoria real, con roles y evidencia cuantitativa por integrante, esta en [`CONTRIBUTORS.md`](CONTRIBUTORS.md) de esta misma carpeta: el equipo de SGED es **Pallo Pinto Alejandro Daniel**, **Velez Lopez Ricardo Elias** y **Arcalle Grefa Darwin Orlando**.

**Que incluye esta copia:** el ultimo commit real (`main`) mas dos fixes puntuales encima, ya probados en vivo, que todavia no estan commiteados en el repo original:
- `backend/src/main/resources/application.yml` — Swagger UI servida en `/api/docs`
- `backend/src/main/java/org/uteq/backend/config/SecurityConfig.java` — permitAll actualizado para que `/api/docs.json` no quede bloqueado

**Que NO incluye:** trabajo en curso sin commitear en el repo real (por ejemplo una funcionalidad de notificaciones a representantes, todavia sin terminar) — eso se queda fuera a propósito, no es parte de la entrega de Unidad IV.

Esta copia existe solo para tener el codigo accesible desde el repo de la practica (demo del API REST, Swagger, etc. — ver [`docs/pallo_api_rest_swagger.md`](docs/pallo_api_rest_swagger.md)). No reemplaza al repositorio original como fuente de verdad del PFC; los cambios de verdad se hacen alla. El README propio de SGED (antes de esta copia) quedo como [`README-SGED-original.md`](README-SGED-original.md).
