import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// ── Interfaces ─────────────────────────────────

export interface PartidoResponse {
  id: number;
  fechaPartido: string | null;
  jornada: number | null;
  fase: string | null;
  golesLocal: number;
  golesVisitante: number;
  estado: 'PROGRAMADO' | 'EN_CURSO' | 'FINALIZADO' | 'SUSPENDIDO';
  torneoNombre: string;
  torneoId: number;
  equipoLocal: { id: number; nombre: string; escudoUrl?: string };
  equipoVisitante: { id: number; nombre: string; escudoUrl?: string };
}

export interface CrearPartidoRequest {
  equipoLocalId: number;
  equipoVisitanteId: number;
  fechaPartido: string;
}

export interface EventoDto {
  jugadorId: number;
  tipoEvento: 'GOL' | 'AMARILLA' | 'ROJA';
  minuto?: number;
}

export interface ResultadoRequest {
  golesLocal: number;
  golesVisitante: number;
  eventos?: EventoDto[];
}

/**
 * Servicio para la gestión de partidos.
 * Consume los endpoints de la API REST de partidos.
 */
@Injectable({ providedIn: 'root' })
export class PartidoService {

  private readonly http = inject(HttpClient);
  private readonly API_URL = 'http://localhost:8089/api';

  /**
   * Obtiene todos los partidos del organizador autenticado.
   */
  obtenerMisPartidos(): Observable<PartidoResponse[]> {
    return this.http.get<PartidoResponse[]>(`${this.API_URL}/partidos/mis-partidos`);
  }

  /**
   * Obtiene los partidos de un torneo concreto.
   */
  obtenerPorTorneo(torneoId: number): Observable<PartidoResponse[]> {
    return this.http.get<PartidoResponse[]>(`${this.API_URL}/partidos/torneo/${torneoId}`);
  }

  /**
   * Crea un partido manualmente en un torneo.
   */
  crearPartido(torneoId: number, dto: CrearPartidoRequest): Observable<PartidoResponse> {
    return this.http.post<PartidoResponse>(`${this.API_URL}/torneos/${torneoId}/partidos`, dto);
  }

  /**
   * Actualiza la fecha/hora de un partido.
   */
  actualizarFecha(partidoId: number, fechaPartido: string): Observable<PartidoResponse> {
    return this.http.put<PartidoResponse>(`${this.API_URL}/partidos/${partidoId}/fecha`, { fechaPartido });
  }

  /**
   * Registra el resultado final de un partido.
   */
  registrarResultado(partidoId: number, req: ResultadoRequest): Observable<PartidoResponse> {
    return this.http.put<PartidoResponse>(`${this.API_URL}/partidos/${partidoId}/resultado`, req);
  }
}
