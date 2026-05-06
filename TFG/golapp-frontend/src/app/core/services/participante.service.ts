import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface EquipoPublico {
  id: number;
  nombre: string;
  codigoAcceso: string;
  nombreDelegado: string;
  telefonoContacto: string;
  emailContacto: string;
}

export interface PartidoPublico {
  id: number;
  fase: string;
  fechaPartido: string | null;
  golesLocal: number;
  golesVisitante: number;
  estado: string;
  equipoLocal: { id: number; nombre: string };
  equipoVisitante: { id: number; nombre: string };
}

export interface JugadorPublico {
  id: number;
  nombreCompleto: string;
  dorsal: number;
  posicion: string;
}

@Injectable({ providedIn: 'root' })
export class ParticipanteService {
  private readonly http = inject(HttpClient);
  private readonly API = 'http://localhost:8089/api/public/participantes';

  obtenerEquipo(codigo: string): Observable<EquipoPublico> {
    return this.http.get<EquipoPublico>(`${this.API}/equipo/${codigo}`);
  }

  obtenerPartidos(codigo: string): Observable<PartidoPublico[]> {
    return this.http.get<PartidoPublico[]>(`${this.API}/equipo/${codigo}/partidos`);
  }

  obtenerPlantilla(codigo: string): Observable<JugadorPublico[]> {
    return this.http.get<JugadorPublico[]>(`${this.API}/equipo/${codigo}/plantilla`);
  }
}
