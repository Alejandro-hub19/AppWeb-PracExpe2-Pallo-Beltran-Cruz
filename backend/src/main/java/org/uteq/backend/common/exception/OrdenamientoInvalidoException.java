package org.uteq.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * El parametro de ordenamiento (sort) hace referencia a una propiedad que
 * no existe en la entidad consultada.
 *
 * Corrige REV-02 de la revision entre pares (docs/observaciones/
 * REVISION-BELTRAN.md): antes de esta clase, un valor de "sort" invalido
 * llegaba sin validar hasta Hibernate y producia un 500 Internal Server
 * Error en vez de un 400 Bad Request.
 */
public class OrdenamientoInvalidoException extends ApiException {
    public OrdenamientoInvalidoException(String mensaje) {
        super(HttpStatus.BAD_REQUEST, mensaje);
    }
}