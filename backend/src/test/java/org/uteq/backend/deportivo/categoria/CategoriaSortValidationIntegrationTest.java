package org.uteq.backend.deportivo.categoria;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.categoria.repository.CategoriaRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reproduce, con base de datos real (H2 en modo PostgreSQL, no mocks),
 * el Hallazgo 2 documentado en la revision entre pares del SGED
 * (docs/observaciones/REVISION-BELTRAN.md): GET /api/categorias vincula
 * el parametro de query "sort" directamente a un Pageable de Spring Data
 * sin validarlo contra las columnas reales de la entidad.
 *
 * Un valor de "sort" que no corresponde a ninguna propiedad de Categoria
 * (por ejemplo "sort=noExisteEstaColumna") hace que Hibernate intente
 * generar un ORDER BY sobre una columna inexistente. La excepcion SQL
 * resultante no esta contemplada como caso de validacion de entrada, asi
 * que el GlobalExceptionHandler la traduce en un 500 Internal Server
 * Error -- cuando, segun Masse (2011, "REST API Design Rulebook"), el
 * codigo correcto para una entrada mal formada del cliente es 400 Bad
 * Request, no un error de servidor.
 *
 * Este test queda en rojo (falla) mientras el defecto no se corrija en
 * el proyecto original SGED_APPWEB; documenta el comportamiento actual,
 * no el deseado, con fines de evidencia para la revision de la Directriz 2.
 *
 * Nota de infraestructura: este es el primer test de integracion real
 * (@SpringBootTest sobre base de datos, no mocks) del proyecto que
 * consulta una entidad con esquema con nombre (deportivo.categorias).
 * La configuracion H2 compartida en application-test.yml
 * (ddl-auto: create-drop) nunca habia sido ejercida contra un esquema
 * con nombre, y H2 no crea esquemas no-default automaticamente a partir
 * de las anotaciones JPA. Para no modificar la configuracion compartida
 * de test (usada por el resto del equipo SGED), este test sobreescribe
 * la URL de datasource solo para si mismo, agregando la creacion del
 * esquema en el propio string de conexion H2.
 *
 * Por el mismo motivo (primer test que activa el contexto de seguridad
 * completo, no un standaloneSetup con controller mockeado), las
 * peticiones se autentican con @WithMockUser: sin esto, Spring Security
 * responde 401 antes de que la peticion llegue al controller y el bug
 * real (500 en el manejo de "sort") nunca se ejercita.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:categoria_sort_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS deportivo"
})
class CategoriaSortValidationIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CategoriaRepository categoriaRepository;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void seedCategoriaValida() {
        categoriaRepository.deleteAll();
        categoriaRepository.save(
                Categoria.builder()
                        .nombre("SUB-12")
                        .edadMin((short) 10)
                        .edadMax((short) 12)
                        .descripcion("Categoria sub-12")
                        .activo(true)
                        .build()
        );
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Hallazgo 2 (reproducido): sort con columna inexistente da 500, no 400")
    void listarConSortInvalido_devuelveQuinientosEnVezDeCuatrocientos() throws Exception {
        // Comportamiento ACTUAL del SGED: 500 (bug documentado).
        // Lo correcto segun REST API Design Rulebook (Masse, 2011) seria 400.
        mockMvc.perform(get("/api/categorias")
                        .param("sort", "noExisteEstaColumna"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Control: sort con columna real (nombre) funciona correctamente")
    void listarConSortValido_devuelveDoscientos() throws Exception {
        // Confirma que el endpoint SI funciona con un sort legitimo,
        // aislando el defecto al manejo de parametros invalidos y no a
        // un problema mas amplio del endpoint.
        mockMvc.perform(get("/api/categorias")
                        .param("sort", "nombre,asc"))
                .andExpect(status().isOk());
    }
}