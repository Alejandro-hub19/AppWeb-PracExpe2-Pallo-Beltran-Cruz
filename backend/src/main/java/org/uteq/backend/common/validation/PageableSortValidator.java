package org.uteq.backend.common.validation;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.uteq.backend.common.exception.OrdenamientoInvalidoException;

import java.util.Set;

/**
 * Valida que las propiedades de ordenamiento (sort) de un Pageable
 * correspondan a columnas reales de la entidad, antes de que Spring Data
 * las traduzca en un ORDER BY sobre una columna inexistente (que produce
 * un 500 en vez de un 400 -- REV-02, docs/observaciones/REVISION-BELTRAN.md).
 *
 * Reutilizable: aplica a cualquier endpoint paginado del proyecto, no solo
 * a Categoria. Uso sugerido en el controller, antes de delegar al service:
 *
 * <pre>{@code
 * PageableSortValidator.validar(pageable, Set.of("nombre", "createdAt"));
 * }</pre>
 */
public final class PageableSortValidator {

    private PageableSortValidator() {
    }

    public static void validar(Pageable pageable, Set<String> propiedadesPermitidas) {
        for (Sort.Order orden : pageable.getSort()) {
            if (!propiedadesPermitidas.contains(orden.getProperty())) {
                throw new OrdenamientoInvalidoException(
                        "La propiedad de ordenamiento '" + orden.getProperty()
                                + "' no existe. Propiedades válidas: " + propiedadesPermitidas);
            }
        }
    }
}