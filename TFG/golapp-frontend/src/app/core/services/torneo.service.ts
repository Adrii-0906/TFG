import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Payload para crear un torneo (lo que envía el formulario) */
export interface CrearTorneoRequest {
  nombre: string;
  descripcion: string;
  fondoUrl?: string;
  tipoTorneo: 'LIGA' | 'ELIMINATORIA';
  fechaInicio: string;   // formato ISO: 'YYYY-MM-DD'
  fechaFin: string;
}

/** Respuesta del backend al crear / consultar un torneo */
export interface TorneoResponse {
  id: number;
  nombre: string;
  descripcion: string;
  fondoUrl: string | null;
  tipoTorneo: 'LIGA' | 'ELIMINATORIA';
  estado: 'BORRADOR' | 'ACTIVO' | 'FINALIZADO';
  fechaInicio: string;
  fechaFin: string;
  fechaCreacion: string;
  organizador: { id: number; username: string; nombre: string };
  equipos: any[];
}

/**
 * Servicio HTTP para la gestión de torneos.
 * El token JWT se inyecta automáticamente a través del AuthInterceptor.
 */
@Injectable({ providedIn: 'root' })
export class TorneoService {

  private readonly http = inject(HttpClient);
  private readonly API_URL = 'http://localhost:8089/api/torneos';

  /**
   * Crea un nuevo torneo.
   * POST /api/torneos?organizadorId={id}
   */
  crearTorneo(data: CrearTorneoRequest, organizadorId: number): Observable<TorneoResponse> {
    const params = new HttpParams().set('organizadorId', organizadorId);
    return this.http.post<TorneoResponse>(this.API_URL, data, { params });
  }

  /**
   * Obtiene todos los torneos del organizador autenticado.
   * GET /api/torneos/organizador/{organizadorId}
   */
  obtenerMisTorneos(organizadorId: number): Observable<TorneoResponse[]> {
    return this.http.get<TorneoResponse[]>(`${this.API_URL}/organizador/${organizadorId}`);
  }

  /**
   * Obtiene un torneo por su ID.
   * GET /api/torneos/{id}
   */
  obtenerPorId(id: number): Observable<TorneoResponse> {
    return this.http.get<TorneoResponse>(`${this.API_URL}/${id}`);
  }

  /**
   * Obtiene todos los torneos (público).
   * GET /api/torneos
   */
  obtenerTodos(): Observable<TorneoResponse[]> {
    return this.http.get<TorneoResponse[]>(this.API_URL);
  }

  /**
   * Genera el calendario automático (sorteo).
   * POST /api/torneos/{id}/generar-calendario
   */
  generarCalendario(torneoId: number): Observable<any> {
    return this.http.post(`${this.API_URL}/${torneoId}/generar-calendario`, {});
  }

  /**
   * Obtiene la clasificación de liga.
   */
  getClasificacionLiga(torneoId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.API_URL}/${torneoId}/clasificacion/liga`);
  }

  /**
   * Obtiene el bracket de eliminatoria.
   */
  getBracket(torneoId: number): Observable<any> {
    return this.http.get<any>(`${this.API_URL}/${torneoId}/clasificacion/bracket`);
  }
}
