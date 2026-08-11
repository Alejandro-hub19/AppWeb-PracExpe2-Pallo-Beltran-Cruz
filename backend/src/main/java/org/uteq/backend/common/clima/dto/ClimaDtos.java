package org.uteq.backend.common.clima.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public final class ClimaDtos {

    private ClimaDtos() {}

    /**
     * Datos del pronostico para la franja de una sesion de entrenamiento.
     *
     * <p>Es lo unico que se guarda en Redis, y a proposito no incluye el origen
     * ni la disponibilidad: esos son metadatos de <em>una consulta concreta</em>,
     * no del dato en si. Si se cachearan juntos, un valor servido desde Redis
     * seguiria diciendo que vino de la API externa.
     */
    public record PronosticoEntrenamiento(
            String ubicacion,
            LocalDate fecha,
            LocalTime desde,
            LocalTime hasta,
            double temperaturaMaxC,
            int probabilidadLluviaMax,
            double precipitacionTotalMm,
            String recomendacion,
            Instant consultadoEn
    ) {}

    /**
     * Envoltura de la respuesta. Sigue el mismo criterio que
     * {@code ResultadoFeedback} del modulo de IA: un servicio externo caido no
     * es un error del SGED, es un dato que hoy no esta. Por eso el endpoint
     * responde 200 con {@code disponible=false} y no un 5xx.
     */
    public record ClimaEntrenamientoResponse(
            boolean disponible,
            String origen,
            String motivo,
            PronosticoEntrenamiento pronostico
    ) {

        public static ClimaEntrenamientoResponse desdeApiExterna(PronosticoEntrenamiento pronostico) {
            return new ClimaEntrenamientoResponse(true, Origen.API_EXTERNA, null, pronostico);
        }

        public static ClimaEntrenamientoResponse desdeCache(PronosticoEntrenamiento pronostico) {
            return new ClimaEntrenamientoResponse(true, Origen.CACHE, null, pronostico);
        }

        public static ClimaEntrenamientoResponse noDisponible(String motivo) {
            return new ClimaEntrenamientoResponse(false, Origen.NINGUNO, motivo, null);
        }
    }

    /** De donde salio la respuesta. Se expone para poder demostrar el cache en vivo. */
    public static final class Origen {
        public static final String API_EXTERNA = "api-externa";
        public static final String CACHE = "cache";
        public static final String NINGUNO = "no-disponible";

        private Origen() {}
    }
}
