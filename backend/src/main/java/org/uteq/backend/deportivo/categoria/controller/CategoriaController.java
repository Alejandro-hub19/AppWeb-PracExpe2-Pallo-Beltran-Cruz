package org.uteq.backend.deportivo.categoria.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.common.validation.PageableSortValidator;
import org.uteq.backend.deportivo.categoria.dto.CategoriaRequest;
import org.uteq.backend.deportivo.categoria.dto.CategoriaResponse;
import org.uteq.backend.deportivo.categoria.service.CategoriaService;

import java.util.List;
import java.util.Set;

/**
 * CRUD del catalogo de categorias. La lectura la necesitan todos los roles
 * (el formulario de estudiante ofrece las categorias activas); la escritura
 * altera un catalogo del que dependen los estudiantes por clave foranea, y
 * queda restringida a ADMINISTRADOR.
 */
@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;

    /**
     * Propiedades reales de Categoria admitidas para ordenar (REV-02:
     * antes de esta lista blanca, un "sort" con cualquier otro valor
     * producia un 500 en vez de un 400).
     */
    private static final Set<String> PROPIEDADES_ORDENABLES = Set.of(
            "idCategoria", "nombre", "edadMin", "edadMax", "descripcion", "activo", "createdAt");

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<Page<CategoriaResponse>> listarPaginado(Pageable pageable) {
        PageableSortValidator.validar(pageable, PROPIEDADES_ORDENABLES);
        return ResponseEntity.ok(categoriaService.listarPaginado(pageable));
    }

    @GetMapping("/activas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'RECEPCIONISTA')")
    public ResponseEntity<List<CategoriaResponse>> listarActivas() {
        return ResponseEntity.ok(categoriaService.listarTodasActivas());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR')")
    public ResponseEntity<CategoriaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CategoriaResponse> editar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.ok(categoriaService.editar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        categoriaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}