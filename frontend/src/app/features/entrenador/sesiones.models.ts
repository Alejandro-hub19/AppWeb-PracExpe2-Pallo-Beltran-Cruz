export interface Sesion {
  idSesion: number;
  categoria: string;
  entrenador: string;
  fecha: string;
  horaInicio: string | null;
  horaFin: string | null;
  campo: string | null;
  estado: string;
  tieneEvaluacion: boolean;
}

export interface CategoriaOpcion {
  idCategoria: number;
  nombre: string;
}

export interface SesionCrearRequest {
  idCategoria: number;
  fecha: string;
  horaInicio: string;
  horaFin: string;
  campo: string | null;
}

/** Espeja ClimaDtos.PronosticoEntrenamiento del backend. */
export interface PronosticoEntrenamiento {
  ubicacion: string;
  fecha: string;
  desde: string;
  hasta: string;
  temperaturaMaxC: number;
  probabilidadLluviaMax: number;
  precipitacionTotalMm: number;
  recomendacion: string;
  consultadoEn: string;
}

/**
 * Espeja ClimaDtos.ClimaEntrenamientoResponse. `pronostico` viene en null
 * cuando el proveedor externo no respondio: la tarjeta muestra el motivo en
 * vez de romperse.
 */
export interface ClimaEntrenamiento {
  disponible: boolean;
  origen: 'api-externa' | 'cache' | 'no-disponible';
  motivo: string | null;
  pronostico: PronosticoEntrenamiento | null;
}
