package org.uteq.backend.common.response.propuesta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de referencia para el prototipo de ApiResponse (ver REV-10 en
 * REVISION-BELTRAN.md). No depende del contexto de Spring: es una prueba
 * unitaria pura de serializacion, deliberadamente simple para poder
 * ejecutarse sin levantar la aplicacion.
 */
class ApiResponseTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .findAndRegisterModules();

    @Test
    @DisplayName("ok() produce exactamente las 5 claves que exige la guia: success, data, message, errors, meta")
    void respuestaExitosaTieneLasCincoClavesExigidas() throws Exception {
        ApiResponse<Map<String, Object>> respuesta = ApiResponse.ok(
                Map.of("idCategoria", 1, "nombre", "SUB-12"),
                "Categoría encontrada"
        );

        String json = mapper.writeValueAsString(respuesta);
        JsonNode nodo = mapper.readTree(json);

        assertTrue(nodo.has("success"));
        assertTrue(nodo.has("data"));
        assertTrue(nodo.has("message"));
        assertTrue(nodo.has("errors"));
        assertTrue(nodo.has("meta"));
        assertEquals(5, nodo.size(),
                "El sobre no debe tener mas ni menos que las 5 claves que exige la guia");

        assertTrue(nodo.get("success").asBoolean());
        assertEquals("SUB-12", nodo.get("data").get("nombre").asText());
        assertEquals("Categoría encontrada", nodo.get("message").asText());
        assertTrue(nodo.get("errors").isArray());
        assertEquals(0, nodo.get("errors").size());
        assertTrue(nodo.get("meta").has("apiVersion"));
        assertTrue(nodo.get("meta").has("timestamp"));
    }

    @Test
    @DisplayName("error() reporta success=false, data=null (omitido) y la lista de errores")
    void respuestaDeErrorReportaFallosCorrectamente() throws Exception {
        ApiResponse<Void> respuesta = ApiResponse.error(
                "Revisa los datos enviados",
                List.of("cedula: debe tener 10 dígitos", "correo: formato inválido")
        );

        String json = mapper.writeValueAsString(respuesta);
        JsonNode nodo = mapper.readTree(json);

        assertEquals(false, nodo.get("success").asBoolean());
        assertTrue(nodo.get("data") == null || nodo.get("data").isMissingNode(),
                "data debe omitirse (@JsonInclude NON_NULL) cuando es null, no serializarse como \"data\": null");
        assertEquals(2, nodo.get("errors").size());
    }

    @Test
    @DisplayName("desdeProblemDetail() aplana errores por campo al formato de lista que exige la guia")
    void adaptaProblemDetailExistenteSinRomperElManejadorActual() throws Exception {
        ApiResponse<Void> respuesta = ApiResponse.desdeProblemDetail(
                "Error de validación",
                Map.of("cedula", List.of("debe tener 10 dígitos"))
        );

        assertEquals(1, respuesta.errors().size());
        assertTrue(respuesta.errors().get(0).contains("cedula"));
        assertTrue(respuesta.errors().get(0).contains("debe tener 10 dígitos"));
    }
}