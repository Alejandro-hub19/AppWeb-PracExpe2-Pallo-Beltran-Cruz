# AppWeb-PracExpe2-Pallo-Beltran-Cruz

Repositorio de entrega para la **Práctica Experimental Unidad IV** (Temas 13–16), asignatura Aplicaciones Web. Equipo: **Pallo**, **Beltrán**, **Cruz**.

---

## ⚠️ Nota de autoría — leer antes de evaluar

**Las estadísticas de `git` de este repositorio NO reflejan la autoría real y no deben usarse como medida de contribución individual.**

Un conteo directo arroja aproximadamente:

| Autor | Commits | Líneas |
|---|---:|---:|
| Alejandro-hub19 (Pallo) | 12 | ~108 600 |
| JustynCruz04 (Cruz) | 4 | ~6 900 |
| Fred Beltrán | 5 | ~540 |

Esa lectura es **engañosa por dos razones**:

1. **~96 400 de las líneas atribuidas a Pallo son una copia plana del proyecto SGED**, volcada en un solo commit (`a9b4cac`). Ese código **no se escribió para esta práctica** y **no es de autoría exclusiva de Pallo**: el equipo de SGED lo integran Pallo Pinto Alejandro Daniel, Vélez López Ricardo Elías y Arcalle Grefa Darwin Orlando. La autoría real, con roles CRediT y evidencia por integrante, está en [`CONTRIBUTORS.md`](CONTRIBUTORS.md), y el historial verdadero vive en el repositorio original: <https://github.com/DarwinSM21/SGED_APPWEB>
2. **El volumen no mide el valor.** Las ~540 líneas de Beltrán son una revisión de pares con nueve hallazgos trazados a evidencia, un test de integración que reproduce un fallo real y una prueba de carga con Apache Bench. Es de las contribuciones más densas del repositorio y la que peor sale en un conteo de líneas.

Ver también [`LEEME_PRIMERO.md`](LEEME_PRIMERO.md).

---

## Qué aportó cada integrante a *esta* práctica

**Pallo** — Directrices 3 y 4, y Paso 3 de la guía:
- API REST documentada con Swagger UI en `/api/docs` (requirió dos correcciones: `application.yml` y `SecurityConfig.java`).
- Comparativa SOAP vs. REST con llamada SOAP real ejecutada y su equivalente REST.
- **Consumo de API REST externa** (Open-Meteo) con patrón *cache aside* en Redis, manejo de errores y presentación en la interfaz: `backend/src/main/java/org/uteq/backend/common/clima/`, 6 pruebas en `ClimaServiceTest`.
- Secciones 3, 6.4, 7 y 9 del informe, Anexo C y migración de las referencias a norma IEEE.
- Documentación: [`docs/pallo_api_rest_swagger.md`](docs/pallo_api_rest_swagger.md), [`docs/pallo_soap_vs_rest.md`](docs/pallo_soap_vs_rest.md), [`docs/pallo_api_externa_cache.md`](docs/pallo_api_externa_cache.md).

**Beltrán** — Directrices 1 y 2:
- Revisión entre pares del SGED con nueve hallazgos verificados en vivo: [`docs/observaciones/REVISION-BELTRAN.md`](docs/observaciones/REVISION-BELTRAN.md) y capturas en `docs/observaciones/evidencias-beltran/`.
- Test de integración que reproduce un 500 por `sort` inválido: `CategoriaSortValidationIntegrationTest`.
- Prueba de carga con Apache Bench: [`docs/mediciones/perf/ab-categorias-activas.txt`](docs/mediciones/perf/ab-categorias-activas.txt).
- Bibliografía con verificación propia de fuentes.

**Cruz** — Directrices 5 y 6:
- Estructura y redacción del informe técnico en LaTeX: [`docs/informe_unidad4.tex`](docs/informe_unidad4.tex).
- Sección de tendencias emergentes (Jamstack, PWA, IA generativa).
- Scripts de compilación reproducible del informe (`docs/compilar.bat`, `docs/compilar-docker.bat`).

## Estructura

- **`docs/informe_unidad4.pdf`** — informe técnico final (46 páginas).
- **`docs/pallo_*.md`**, **`docs/observaciones/`**, **`docs/mediciones/`** — trabajo específico de esta práctica y su evidencia.
- **`backend/`, `frontend/`, `db/`, `docs/adr/`, `docs/requisitos/`** — copia del proyecto SGED (ver nota de autoría arriba).
- **`README-SGED-original.md`** — README original de SGED, tal como está en su repositorio.

## Declaración de asistencia de IA

Siguiendo el mismo criterio ya adoptado por el equipo de SGED en su `CONTRIBUTORS.md`, se declara el uso de IA generativa (Claude, de Anthropic) como herramienta de apoyo en parte de este trabajo: implementación y revisión de código, redacción de documentación y análisis de evidencia. El diseño, las decisiones técnicas y la verificación de que el sistema funciona fueron hechos y revisados por los integrantes. La autoría de cada commit corresponde únicamente a la persona que lo realizó.
