package org.uteq.backend.common.clima;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDate;

/**
 * Cliente HTTP hacia Open-Meteo, la API REST externa que consume el SGED.
 *
 * <p>Se eligio este proveedor por dos razones concretas. La primera es que no
 * exige registro ni clave: el sistema levanta desde una clonacion limpia sin
 * depender de un secreto que un evaluador no tiene (el modulo de IA, que si
 * necesita clave, viene deshabilitado por defecto justamente por eso). La
 * segunda es de dominio: los entrenamientos de la escuela ocurren en cancha
 * abierta, y la lluvia es la razon real por la que una sesion se reprograma,
 * de modo que el dato consumido se usa para decidir algo, no para rellenar un
 * requisito.
 *
 * <p>Esta clase solo hace la llamada y deja propagar el fallo. Que hacer
 * cuando el proveedor no responde es decision de {@link ClimaService}, que es
 * quien conoce la politica de cache y de degradacion.
 */
@Component
public class OpenMeteoClient {

    private final RestClient restClient;

    public OpenMeteoClient(
            @Value("${clima.base-url:https://api.open-meteo.com/v1}") String baseUrl,
            @Value("${clima.timeout-segundos:5}") int timeoutSegundos) {

        var factory = new SimpleClientHttpRequestFactory();
        // Timeout corto y simetrico. El clima es informacion de apoyo en la
        // pantalla de sesiones: si el proveedor tarda, es preferible pintar la
        // pantalla sin el pronostico que dejar al entrenador esperando.
        factory.setConnectTimeout((int) Duration.ofSeconds(timeoutSegundos).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(timeoutSegundos).toMillis());

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    /**
     * Serie horaria de temperatura, probabilidad de lluvia y precipitacion para
     * un dia y una ubicacion.
     *
     * @throws org.springframework.web.client.RestClientException si el servicio
     *         externo no responde, supera el timeout o devuelve 4xx/5xx
     */
    public JsonNode pronosticoHorario(double latitud, double longitud, String zonaHoraria, LocalDate fecha) {
        return restClient.get()
                .uri(uri -> uri.path("/forecast")
                        .queryParam("latitude", latitud)
                        .queryParam("longitude", longitud)
                        .queryParam("hourly", "temperature_2m,precipitation_probability,precipitation")
                        .queryParam("timezone", zonaHoraria)
                        .queryParam("start_date", fecha)
                        .queryParam("end_date", fecha)
                        .build())
                .retrieve()
                .body(JsonNode.class);
    }
}
