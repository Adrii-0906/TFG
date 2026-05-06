import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Representación de un equipo del backend */
export interface EquipoResponse {
  id: number;
  nombre: string;
  escudoUrl: string | null;
  nombreDelegado: string | null;
  telefonoContacto: string | null;
  emailContacto: string | null;
  codigoAcceso: string | null;
  usuario: { id: number; username: string; nombre: string } | null;
}

/** Payload para crear un equipo */
export interface CrearEquipoRequest {
  nombre: string;
  escudoUrl?: string;
  nombreDelegado?: string;
  telefonoContacto?: string;
  emailContacto?: string;
}

/**
 * Servicio HTTP para la gestión de equipos.
 * - Endpoints propios: /api/equipos (CRUD del organizador)
 * - Endpoints de torneo: /api/torneos/{id}/equipos (equipos dentro de un torneo)
 * El token JWT se inyecta automáticamente a través del AuthInterceptor.
 */
@Injectable({ providedIn: 'root' })
export class EquipoService {

  private readonly http = inject(HttpClient);
  private readonly API_URL = 'http://localhost:8089/api/equipos';
  private readonly TORNEOS_URL = 'http://localhost:8089/api/torneos';

  // ══════════════════════════════════════════════════
  // ── Endpoints propios del organizador ─────────────
  // ══════════════════════════════════════════════════

  /**
   * Obtiene los equipos del organizador autenticado.
   * GET /api/equipos/mis-equipos
   */
  obtenerMisEquipos(): Observable<EquipoResponse[]> {
    return this.http.get<EquipoResponse[]>(`${this.API_URL}/mis-equipos`);
  }

  /**
   * Exporta los equipos a formato JSON.
   * Permite filtrar por IDs si se proveen.
   * GET /api/equipos/exportar
   */
  exportarEquipos(ids?: number[]): Observable<any[]> {
    let url = `${this.API_URL}/exportar`;
    if (ids && ids.length > 0) {
      url += `?ids=${ids.join(',')}`;
    }
    return this.http.get<any[]>(url);
  }

  /**
   * Importa equipos desde un JSON.
   * POST /api/equipos/importar
   */
  importarEquipos(jsonData: any[]): Observable<any> {
    return this.http.post(`${this.API_URL}/importar`, jsonData);
  }

  /**
   * Crea un equipo nuevo asociado al organizador autenticado.
   * POST /api/equipos
   */
  crearEquipo(data: CrearEquipoRequest): Observable<EquipoResponse> {
    return this.http.post<EquipoResponse>(this.API_URL, data);
  }

  /**
   * Elimina un equipo del organizador.
   * DELETE /api/equipos/{id}
   */
  eliminarEquipo(equipoId: number): Observable<any> {
    return this.http.delete(`${this.API_URL}/${equipoId}`);
  }

  /**
   * Obtiene un equipo por su ID.
   * GET /api/equipos/{id}
   */
  obtenerPorId(equipoId: number): Observable<EquipoResponse> {
    return this.http.get<EquipoResponse>(`${this.API_URL}/${equipoId}`);
  }

  // ══════════════════════════════════════════════════
  // ── Endpoints de equipos dentro de un torneo ──────
  // ══════════════════════════════════════════════════

  /**
   * Obtiene los equipos de un torneo.
   * GET /api/torneos/{torneoId}/equipos
   */
  obtenerEquiposPorTorneo(torneoId: number): Observable<EquipoResponse[]> {
    return this.http.get<EquipoResponse[]>(`${this.TORNEOS_URL}/${torneoId}/equipos`);
  }

  /**
   * Crea un equipo nuevo y lo inscribe en el torneo.
   * POST /api/torneos/{torneoId}/equipos
   */
  crearEquipoEnTorneo(torneoId: number, data: CrearEquipoRequest): Observable<EquipoResponse> {
    return this.http.post<EquipoResponse>(`${this.TORNEOS_URL}/${torneoId}/equipos`, data);
  }

  /**
   * Elimina un equipo de un torneo (desvincula).
   * DELETE /api/torneos/{torneoId}/equipos/{equipoId}
   */
  eliminarEquipoDeTorneo(torneoId: number, equipoId: number): Observable<any> {
    return this.http.delete(`${this.TORNEOS_URL}/${torneoId}/equipos/${equipoId}`);
  }

  /**
   * Vincula un equipo existente a un torneo.
   * POST /api/torneos/{torneoId}/equipos/{equipoId}
   */
  vincularEquipoATorneo(torneoId: number, equipoId: number): Observable<any> {
    return this.http.post(`${this.TORNEOS_URL}/${torneoId}/equipos/${equipoId}`, {});
  }
}
