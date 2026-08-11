package org.uteq.backend.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * Habilita caché HTTP con validación por {@code ETag} en los catálogos de
 * solo lectura.
 *
 * <p>Motivo: Spring Security emite por omisión
 * {@code Cache-Control: no-cache, no-store, max-age=0, must-revalidate} en
 * todo endpoint autenticado. Es una postura sensata como valor por defecto
 * ---evita que un proxy compartido guarde datos de una sesión ajena--- pero
 * deja la API incumpliendo la restricción <em>cacheable</em> de Fielding: sin
 * excepción alguna, ninguna caché puede intervenir y un catálogo que casi
 * nunca cambia se transfiere entero en cada petición.
 *
 * <p>Se corrige solo donde es correcto hacerlo: categorías y estados
 * generales son catálogos pequeños y estables. Los controladores respectivos
 * declaran {@code Cache-Control: private, no-cache}, que autoriza a
 * almacenar la respuesta pero obliga a revalidarla siempre; este filtro
 * calcula el {@code ETag} del cuerpo y responde 304 cuando el cliente ya
 * tiene la versión vigente.
 *
 * <p>Se eligió revalidación y no {@code max-age} a propósito: con un tiempo
 * de frescura fijo, crear una categoría y listarla a continuación podría
 * devolver la lista anterior. Con {@code ETag} el cliente nunca ve datos
 * obsoletos ---la única diferencia es que, si nada cambió, la respuesta viaja
 * sin cuerpo---.
 *
 * <p>{@code private} es deliberado: la respuesta puede guardarla el
 * navegador del usuario, nunca una caché compartida, porque el contenido
 * depende del rol de quien pregunta.
 */
@Configuration
public class HttpCacheConfig {

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> etagDeCatalogos() {
        var registro = new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        // Acotado a los catálogos: el filtro almacena la respuesta en memoria
        // para calcular el hash, y no tiene sentido pagar eso en listados
        // paginados grandes ni en escrituras.
        registro.addUrlPatterns("/api/categorias/*", "/api/estados_generales");
        registro.setName("etagDeCatalogos");
        return registro;
    }
}
