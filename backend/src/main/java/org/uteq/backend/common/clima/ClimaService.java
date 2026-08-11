package org.uteq.backend.common.clima;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.uteq.backend.common.clima.dto.ClimaDtos.ClimaEntrenamientoResponse;
import org.uteq.backend.common.clima.dto.ClimaDtos.PronosticoEntrenamiento;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

/**
 * Consumo de la API externa de clima con patron <em>cache aside</em>.
 *
 * <p>El flujo es el clasico de tres pasos, escrito de forma explicita y no con
 * {@code @Cacheable}, por dos motivos que importan aqui:
 *
 * <ol>
 *   <li>Un fallo del proveedor <b>no debe cachearse</b>. Con la anotacion, el
 *       valor de degradacion quedaria guardado y el sistema seguiria diciendo
 *       "no disponible" durante todo el TTL aunque el proveedor ya se hubiera
 *       recuperado.</li>
 *   <li>La respuesta necesita declarar de donde salio ({@code origen}), y eso
 *       solo se puede decidir en el punto donde se sabe si hubo acierto de
 *       cache.</li>
 * </ol>
 *
 * <p>El TTL es de 10 minutos. No es un numero arbitrario: el pronostico horario
 * de Open-Meteo se recalcula en escalas mucho mayores, asi que pedirlo mas
 * seguido no traeria informacion nueva, solo consumo de cuota. Cachear tambien
 * protege del limite de peticiones del proveedor y de su indisponibilidad.
 *
 * <p>Redis es cache, no fuente de verdad: si esta caido, sus fallos se
 * registran y se sigue hacia la API externa. El sistema pierde velocidad, no
 * funcionalidad.
 */
@Service
public class ClimaService {

    private static final Logger log = LoggerFactory.getLogger(ClimaService.class);

    /**
     * Prefijo de las claves en Redis. Se mantiene legible a proposito para
     * poder auditar el cache en vivo con {@code redis-cli KEYS "clima:*"}.
     */
    static final String PREFIJO_CLAVE = "clima:entrenamiento:";

    private final OpenMeteoClient client;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    private final Duration ttl;
    private final double latitud;
    private final double longitud;
    private final String zonaHoraria;
    private final String ubicacion;

    public ClimaService(
            OpenMeteoClient client,
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            @Value("${clima.cache-ttl-segundos:600}") long ttlSegundos,
            @Value("${clima.latitud:-1.0286}") double latitud,
            @Value("${clima.longitud:-79.4636}") double longitud,
            @Value("${clima.zona-horaria:America/Guayaquil}") String zonaHoraria,
            @Value("${clima.ubicacion:Quevedo, Los Rios}") String ubicacion) {

        this.client = client;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofSeconds(ttlSegundos);
        this.latitud = latitud;
        this.longitud = longitud;
        this.zonaHoraria = zonaHoraria;
        this.ubicacion = ubicacion;
    }

    /**
     * Pronostico para la franja de entrenamiento indicada.
     *
     * <p>Nunca lanza excepcion por fallo del proveedor externo: devuelve un
     * resultado no disponible con el motivo, y la interfaz decide si lo muestra
     * u omite la tarjeta.
     */
    public ClimaEntrenamientoResponse pronosticoDe(LocalDate fecha, LocalTime desde, LocalTime hasta) {
        String clave = claveDe(fecha, desde, hasta);

        // 1. Consultar el cache.
        Optional<PronosticoEntrenamiento> enCache = leerDeCache(clave);
        if (enCache.isPresent()) {
            return ClimaEntrenamientoResponse.desdeCache(enCache.get());
        }

        // 2. Fallo de cache: ir a la API externa.
        try {
            JsonNode crudo = client.pronosticoHorario(latitud, longitud, zonaHoraria, fecha);
            PronosticoEntrenamiento pronostico = interpretar(crudo, fecha, desde, hasta);

            // 3. Guardar solo el exito.
            guardarEnCache(clave, pronostico);
            return ClimaEntrenamientoResponse.desdeApiExterna(pronostico);

        } catch (RestClientException e) {
            // Timeout, DNS, 4xx y 5xx entran todos por aqui.
            log.warn("Open-Meteo no respondio ({}): la pantalla se sirve sin pronostico",
                    e.getClass().getSimpleName());
            return ClimaEntrenamientoResponse.noDisponible(
                    "El servicio de clima no responde en este momento");

        } catch (RuntimeException e) {
            // Cuerpo con forma inesperada: el proveedor contesto 200 pero lo que
            // mando no sirve. Se trata igual que una caida, no se propaga.
            log.warn("Respuesta de Open-Meteo no interpretable ({}): {}",
                    e.getClass().getSimpleName(), e.getMessage());
            return ClimaEntrenamientoResponse.noDisponible(
                    "El servicio de clima devolvio datos incompletos");
        }
    }

    // ------------------------------------------------------------------
    // Cache
    // ------------------------------------------------------------------

    /**
     * La clave incluye la franja, no solo el dia: dos sesiones del mismo dia en
     * horarios distintos tienen pronosticos distintos y no deben compartir
     * entrada.
     */
    static String claveDe(LocalDate fecha, LocalTime desde, LocalTime hasta) {
        return PREFIJO_CLAVE + fecha + ":" + desde + "-" + hasta;
    }

    private Optional<PronosticoEntrenamiento> leerDeCache(String clave) {
        try {
            String json = redis.opsForValue().get(clave);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, PronosticoEntrenamiento.class));
        } catch (Exception e) {
            // Redis caido, o una entrada vieja con formato anterior. En ambos
            // casos se degrada a fallo de cache y se consulta la API externa.
            log.warn("No se pudo leer el clima desde Redis ({}): se consulta la API externa",
                    e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private void guardarEnCache(String clave, PronosticoEntrenamiento pronostico) {
        try {
            redis.opsForValue().set(clave, objectMapper.writeValueAsString(pronostico), ttl);
        } catch (Exception e) {
            // El dato ya se obtuvo; no poder cachearlo solo cuesta una llamada
            // extra la proxima vez.
            log.warn("No se pudo cachear el clima en Redis ({}): la respuesta se entrega igual",
                    e.getClass().getSimpleName());
        }
    }

    // ------------------------------------------------------------------
    // Interpretacion de la respuesta externa
    // ------------------------------------------------------------------

    private PronosticoEntrenamiento interpretar(
            JsonNode raiz, LocalDate fecha, LocalTime desde, LocalTime hasta) {

        if (raiz == null) {
            throw new IllegalStateException("respuesta vacia");
        }

        JsonNode horario = raiz.path("hourly");
        JsonNode tiempos = horario.path("time");
        JsonNode temperaturas = horario.path("temperature_2m");
        JsonNode probabilidades = horario.path("precipitation_probability");
        JsonNode precipitaciones = horario.path("precipitation");

        if (!tiempos.isArray() || tiempos.isEmpty()) {
            throw new IllegalStateException("respuesta sin serie horaria");
        }

        double temperaturaMax = Double.NEGATIVE_INFINITY;
        int probabilidadMax = 0;
        double precipitacionTotal = 0;
        int horasEnFranja = 0;

        for (int i = 0; i < tiempos.size(); i++) {
            LocalTime hora = LocalDateTime.parse(tiempos.get(i).asText()).toLocalTime();
            if (hora.isBefore(desde) || hora.isAfter(hasta)) {
                continue;
            }
            horasEnFranja++;
            temperaturaMax = Math.max(temperaturaMax, temperaturas.path(i).asDouble());
            probabilidadMax = Math.max(probabilidadMax, probabilidades.path(i).asInt());
            precipitacionTotal += precipitaciones.path(i).asDouble();
        }

        if (horasEnFranja == 0) {
            throw new IllegalStateException("la franja " + desde + "-" + hasta + " no tiene datos");
        }

        return new PronosticoEntrenamiento(
                ubicacion,
                fecha,
                desde,
                hasta,
                redondear(temperaturaMax),
                probabilidadMax,
                redondear(precipitacionTotal),
                recomendar(probabilidadMax, temperaturaMax),
                Instant.now());
    }

    /**
     * Umbrales operativos acordados por el equipo para una escuela formativa,
     * no un criterio medico. La recomendacion acompana la decision del
     * entrenador; no la sustituye.
     */
    private String recomendar(int probabilidadLluvia, double temperaturaMax) {
        if (probabilidadLluvia >= 70) {
            return "Alta probabilidad de lluvia en la franja: considerar reprogramar o mover a cubierto.";
        }
        if (probabilidadLluvia >= 40) {
            return "Posibilidad de lluvia: conviene tener un plan alterno antes de salir a la cancha.";
        }
        if (temperaturaMax >= 32) {
            return "Calor alto: reforzar la hidratacion y programar pausas mas frecuentes.";
        }
        return "Condiciones aptas para entrenar en cancha abierta.";
    }

    private double redondear(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
}
