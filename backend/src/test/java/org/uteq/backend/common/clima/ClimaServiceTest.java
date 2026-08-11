package org.uteq.backend.common.clima;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.client.ResourceAccessException;
import org.uteq.backend.common.clima.dto.ClimaDtos.ClimaEntrenamientoResponse;
import org.uteq.backend.common.clima.dto.ClimaDtos.Origen;
import org.uteq.backend.common.clima.dto.ClimaDtos.PronosticoEntrenamiento;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba del consumo de la API externa con cache aside (Paso 3 de la guia
 * PE-U4).
 *
 * <p>Ninguna prueba sale a internet: {@link OpenMeteoClient} esta mockeado. Lo
 * que se verifica aqui no es que Open-Meteo funcione -eso no es
 * responsabilidad del SGED- sino las tres decisiones propias: que un acierto
 * de cache evite la llamada externa, que un fallo del proveedor no se cachee,
 * y que ni el proveedor ni Redis caidos rompan la pantalla.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClimaServiceTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 8, 10);
    private static final LocalTime DESDE = LocalTime.of(15, 0);
    private static final LocalTime HASTA = LocalTime.of(19, 0);

    /**
     * Respuesta con forma real de Open-Meteo. Incluye a proposito una hora
     * antes (14:00) y una despues (20:00) de la franja, con valores extremos:
     * si el filtro de franja fallara, la temperatura maxima saldria 40 y la
     * probabilidad 95 en vez de 30 y 14.
     */
    private static final String RESPUESTA_OPEN_METEO = """
            {
              "hourly": {
                "time": ["2026-08-10T14:00","2026-08-10T15:00","2026-08-10T16:00",
                         "2026-08-10T17:00","2026-08-10T18:00","2026-08-10T19:00",
                         "2026-08-10T20:00"],
                "temperature_2m":            [35.0, 30.0, 28.8, 28.8, 27.8, 26.3, 40.0],
                "precipitation_probability": [90,   11,   14,   12,   9,    14,   95],
                "precipitation":             [9.9,  0.0,  0.1,  0.1,  0.5,  1.0,  9.9]
              }
            }
            """;

    @Mock private OpenMeteoClient client;
    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> operacionesDeValor;

    private ObjectMapper objectMapper;
    private ClimaService climaService;

    @BeforeEach
    void preparar() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        when(redis.opsForValue()).thenReturn(operacionesDeValor);

        climaService = new ClimaService(
                client, redis, objectMapper,
                600, -1.0286, -79.4636, "America/Guayaquil", "Quevedo, Los Rios");
    }

    private JsonNode respuestaValida() throws Exception {
        return objectMapper.readTree(RESPUESTA_OPEN_METEO);
    }

    @Test
    @DisplayName("Fallo de cache: consulta la API externa, la interpreta y guarda el resultado")
    void fallo_de_cache_consulta_api_externa() throws Exception {
        when(operacionesDeValor.get(anyString())).thenReturn(null);
        when(client.pronosticoHorario(anyDouble(), anyDouble(), anyString(), any()))
                .thenReturn(respuestaValida());

        ClimaEntrenamientoResponse respuesta = climaService.pronosticoDe(FECHA, DESDE, HASTA);

        assertThat(respuesta.disponible()).isTrue();
        assertThat(respuesta.origen()).isEqualTo(Origen.API_EXTERNA);

        PronosticoEntrenamiento pronostico = respuesta.pronostico();
        // Solo la franja 15:00-19:00: las 14:00 y 20:00 quedan fuera.
        assertThat(pronostico.temperaturaMaxC()).isEqualTo(30.0);
        assertThat(pronostico.probabilidadLluviaMax()).isEqualTo(14);
        assertThat(pronostico.precipitacionTotalMm()).isEqualTo(1.7);
        assertThat(pronostico.recomendacion()).contains("aptas");

        verify(operacionesDeValor).set(
                eqClave(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Acierto de cache: no vuelve a llamar a la API externa")
    void acierto_de_cache_no_llama_api_externa() throws Exception {
        PronosticoEntrenamiento cacheado = new PronosticoEntrenamiento(
                "Quevedo, Los Rios", FECHA, DESDE, HASTA,
                29.5, 20, 0.4, "Condiciones aptas para entrenar en cancha abierta.", Instant.now());
        when(operacionesDeValor.get(anyString()))
                .thenReturn(objectMapper.writeValueAsString(cacheado));

        ClimaEntrenamientoResponse respuesta = climaService.pronosticoDe(FECHA, DESDE, HASTA);

        assertThat(respuesta.disponible()).isTrue();
        assertThat(respuesta.origen()).isEqualTo(Origen.CACHE);
        assertThat(respuesta.pronostico().temperaturaMaxC()).isEqualTo(29.5);

        verify(client, never()).pronosticoHorario(anyDouble(), anyDouble(), anyString(), any());
    }

    @Test
    @DisplayName("Proveedor externo caido: responde no disponible y NO cachea el fallo")
    void proveedor_caido_no_cachea_el_fallo() {
        when(operacionesDeValor.get(anyString())).thenReturn(null);
        when(client.pronosticoHorario(anyDouble(), anyDouble(), anyString(), any()))
                .thenThrow(new ResourceAccessException("connect timed out"));

        ClimaEntrenamientoResponse respuesta = climaService.pronosticoDe(FECHA, DESDE, HASTA);

        assertThat(respuesta.disponible()).isFalse();
        assertThat(respuesta.pronostico()).isNull();
        assertThat(respuesta.motivo()).isNotBlank();

        // Clave del asunto: si el fallo se cacheara, el sistema seguiria
        // diciendo "no disponible" durante los 10 minutos del TTL aunque el
        // proveedor se recuperara al segundo siguiente.
        verify(operacionesDeValor, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Redis caido: degrada a fallo de cache y sigue sirviendo desde la API externa")
    void redis_caido_no_rompe_la_consulta() throws Exception {
        when(operacionesDeValor.get(anyString()))
                .thenThrow(new RuntimeException("Unable to connect to Redis"));
        when(client.pronosticoHorario(anyDouble(), anyDouble(), anyString(), any()))
                .thenReturn(respuestaValida());

        ClimaEntrenamientoResponse respuesta = climaService.pronosticoDe(FECHA, DESDE, HASTA);

        assertThat(respuesta.disponible()).isTrue();
        assertThat(respuesta.origen()).isEqualTo(Origen.API_EXTERNA);
    }

    @Test
    @DisplayName("Respuesta externa sin serie horaria: no disponible, sin propagar excepcion")
    void respuesta_incompleta_degrada_elegantemente() throws Exception {
        when(operacionesDeValor.get(anyString())).thenReturn(null);
        when(client.pronosticoHorario(anyDouble(), anyDouble(), anyString(), any()))
                .thenReturn(objectMapper.readTree("{\"hourly\":{}}"));

        ClimaEntrenamientoResponse respuesta = climaService.pronosticoDe(FECHA, DESDE, HASTA);

        assertThat(respuesta.disponible()).isFalse();
        assertThat(respuesta.motivo()).isNotBlank();
    }

    @Test
    @DisplayName("La clave de cache separa franjas distintas del mismo dia")
    void la_clave_distingue_franjas() {
        String manana = ClimaService.claveDe(FECHA, LocalTime.of(8, 0), LocalTime.of(10, 0));
        String tarde = ClimaService.claveDe(FECHA, DESDE, HASTA);

        assertThat(manana).isNotEqualTo(tarde);
        assertThat(manana).startsWith(ClimaService.PREFIJO_CLAVE);
    }

    /** La clave exacta que debe usarse, para que la prueba falle si cambia el formato. */
    private String eqClave() {
        return org.mockito.ArgumentMatchers.eq(ClimaService.claveDe(FECHA, DESDE, HASTA));
    }
}
