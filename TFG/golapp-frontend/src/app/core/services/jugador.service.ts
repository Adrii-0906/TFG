import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Representación de un jugador del backend */
export interface JugadorResponse {
  id: number;
  nombreCompleto: string;
  dorsal: number | null;
  posicion: string | null;
  goles: number;
  tarjetasAmarillas: number;
  tarjetasRojas: number;
}

/** Payload para crear un jugador */
export interface CrearJugadorRequest {
  nombreCompleto: string;
  dorsal?: number;
  posicion?: string;
}

/**
 * Servicio HTTP para la gestión de jugadores dentro de un equipo.
 * Ruta base: /api/equipos/{equipoId}/jugadores
 */
@Injectable({ providedIn: 'root' })
export class JugadorService {

  private readonly http = inject(HttpClient);
  private readonly API_URL = 'http://localhost:8089/api/equipos';

  /**
   * Obtiene los jugadores de un equipo.
   * GET /api/equipos/{equipoId}/jugadores
   */
  obtenerJugadores(equipoId: number): Observable<JugadorResponse[]> {
    return this.http.get<JugadorResponse[]>(`${this.API_URL}/${equipoId}/jugadores`);
  }

  /**
   * Obtiene los jugadores de un equipo sin verificar propiedad.
   * GET /api/equipos/{equipoId}/jugadores/publico
   */
  obtenerJugadoresPublico(equipoId: number): Observable<JugadorResponse[]> {
    return this.http.get<JugadorResponse[]>(`${this.API_URL}/${equipoId}/jugadores/publico`);
  }

  /**
   * Añade un jugador al equipo.
   * POST /api/equipos/{equipoId}/jugadores
   */
  añadirJugador(equipoId: number, data: CrearJugadorRequest): Observable<JugadorResponse> {
    return this.http.post<JugadorResponse>(`${this.API_URL}/${equipoId}/jugadores`, data);
  }

  /**
   * Elimina un jugador del equipo.
   * DELETE /api/equipos/{equipoId}/jugadores/{jugadorId}
   */
  eliminarJugador(equipoId: number, jugadorId: number): Observable<any> {
    return this.http.delete(`${this.API_URL}/${equipoId}/jugadores/${jugadorId}`);
  }
}
