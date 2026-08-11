package org.uteq.backend.common.response.propuesta;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * PROTOTIPO DE REFERENCIA -- no integrado a ningun controller existente.
 *
 * Contexto: la guia de Practica Experimental Unidad IV (OE2) exige que la API
 * REST propia responda con el sobre estructurado
 * {@code {success, data, message, errors, meta}}. La API actual del SGED usa
 * ProblemDetail / RFC 7807 ({@code type, title, status, detail, instance})
 * para los errores, que es un estandar legitimo pero no coincide con el
 * formato que pide textualmente la guia (ver REV-10 en
 * docs/observaciones/REVISION-BELTRAN.md).
 *
 * Esta clase, y el test que la acompana, son una RECOMENDACION de como se
 * veria el envoltorio pedido, con un ejemplo compilable y probado -- no una
 * modificacion del comportamiento real de la API. Adoptarla, adaptarla o
 * descartarla queda a criterio del equipo autor de SGED (Pallo, Velez,
 * Arcalle).
 *
 * @param success indica si la operacion se completo correctamente
 * @param data    el recurso solicitado; {@code null} en caso de error
 * @param message mensaje legible para humanos, siempre presente
 * @param errors  lista de errores de validacion o de negocio; vacia si
 *                {@code success} es {@code true}
 * @param meta    metadatos de la respuesta (paginacion, marca temporal,
 *                version de la API); nunca nulo
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        List<String> errors,
        Meta meta
) {

    /**
     * Metadatos minimos sugeridos por la guia: version de la API y marca
     * temporal de la respuesta. Se puede extender con campos de paginacion
     * ({@code page}, {@code size}, {@code totalElements}) cuando el recurso
     * lo requiera.
     */
    public record Meta(String apiVersion, Instant timestamp) {
        public static Meta ahora() {
            return new Meta("v1", Instant.now());
        }
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, List.of(), Meta.ahora());
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ok(data, "Operación completada correctamente.");
    }

    public static <T> ApiResponse<T> error(String message, List<String> errores) {
        return new ApiResponse<>(false, null, message, errores, Meta.ahora());
    }

    public static <T> ApiResponse<T> error(String message) {
        return error(message, List.of());
    }

    /**
     * Ejemplo de adaptacion desde un ProblemDetail existente, para migrar
     * gradualmente sin reescribir el GlobalExceptionHandler actual de una
     * sola vez.
     */
    public static <T> ApiResponse<T> desdeProblemDetail(String detail, Map<String, List<String>> erroresPorCampo) {
        List<String> aplanados = erroresPorCampo == null
                ? List.of()
                : erroresPorCampo.entrySet().stream()
                        .flatMap(entrada -> entrada.getValue().stream()
                                .map(mensaje -> entrada.getKey() + ": " + mensaje))
                        .toList();
        return error(detail, aplanados);
    }
}