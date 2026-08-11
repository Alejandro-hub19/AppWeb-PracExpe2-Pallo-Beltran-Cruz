import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { CategoriaOpcion, ClimaEntrenamiento, Sesion, SesionCrearRequest } from './sesiones.models';

@Injectable({ providedIn: 'root' })
export class SesionesService {
  private readonly http = inject(HttpClient);

  /** Historial completo del entrenador autenticado, no solo las de hoy. */
  listarMias(page = 0, size = 20) {
    return this.http.get<Sesion[]>(`/api/sesiones/mias?page=${page}&size=${size}`);
  }

  crear(request: SesionCrearRequest) {
    return this.http.post<Sesion>('/api/sesiones', request);
  }

  listarCategoriasActivas() {
    return this.http.get<CategoriaOpcion[]>('/api/categorias/activas');
  }

  /**
   * Pronostico del clima para la cancha (API REST externa, cacheada en Redis
   * por el backend). Cuando hay sesion programada se envia su franja real; si
   * no, el backend usa el horario vespertino por defecto.
   */
  climaEntrenamiento(fecha: string, desde: string | null, hasta: string | null) {
    let params = new HttpParams().set('fecha', fecha);
    if (desde) {
      params = params.set('desde', desde);
    }
    if (hasta) {
      params = params.set('hasta', hasta);
    }
    return this.http.get<ClimaEntrenamiento>('/api/clima/entrenamiento', { params });
  }
}
