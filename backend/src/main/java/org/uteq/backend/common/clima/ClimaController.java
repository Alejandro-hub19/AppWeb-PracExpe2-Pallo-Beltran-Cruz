package org.uteq.backend.common.clima;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.uteq.backend.common.clima.dto.ClimaDtos.ClimaEntrenamientoResponse;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Pronostico del clima para la cancha, consumido de una API REST externa.
 *
 * <p>Los tres parametros son opcionales para que la tarjeta de la pantalla de
 * sesiones funcione sin configurar nada, pero el frontend envia la franja real
 * de la sesion cuando la conoce. Asi el backend no necesita consultar la base
 * para saber a que hora se entrena: quien ya tiene ese dato lo pasa.
 */
@RestController
@RequestMapping("/api/clima")
@RequiredArgsConstructor
public class ClimaController {

    private final ClimaService climaService;

    @Value("${clima.entrenamiento.hora-inicio-defecto:15:00}")
    private String horaInicioDefecto;

    @Value("${clima.entrenamiento.hora-fin-defecto:19:00}")
    private String horaFinDefecto;

    @GetMapping("/entrenamiento")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'ENTRENADOR', 'RECEPCIONISTA')")
    public ResponseEntity<ClimaEntrenamientoResponse> pronosticoEntrenamiento(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime hasta) {

        LocalDate dia = fecha != null ? fecha : LocalDate.now();
        LocalTime inicio = desde != null ? desde : LocalTime.parse(horaInicioDefecto);
        LocalTime fin = hasta != null ? hasta : LocalTime.parse(horaFinDefecto);

        // Siempre 200: un proveedor externo caido no es un error del SGED.
        // La respuesta trae disponible=false y el motivo (ver ClimaDtos).
        return ResponseEntity.ok(climaService.pronosticoDe(dia, inicio, fin));
    }
}
