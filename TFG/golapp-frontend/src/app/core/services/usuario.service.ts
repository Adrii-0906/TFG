import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UsuarioResponse {
  id: number;
  username: string;
  email: string;
  nombre: string;
  apellidos: string | null;
  telefono: string | null;
  avatarUrl: string | null;
  duracionPartidoDefecto: number;
  rol: string;
  fechaRegistro: string;
}

export interface ActualizarPerfilRequest {
  nombre: string;
  telefono: string;
  avatarUrl: string;
  duracionPartidoDefecto: number;
}

export interface CambiarPasswordRequest {
  passwordActual: string;
  nuevaPassword: string;
}

@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = 'http://localhost:8089/api/usuarios';

  obtenerPerfil(): Observable<UsuarioResponse> {
    return this.http.get<UsuarioResponse>(`${this.API_URL}/me`);
  }

  actualizarPerfil(dto: ActualizarPerfilRequest): Observable<UsuarioResponse> {
    return this.http.put<UsuarioResponse>(`${this.API_URL}/me`, dto);
  }

  cambiarPassword(dto: CambiarPasswordRequest): Observable<any> {
    return this.http.put<any>(`${this.API_URL}/me/password`, dto);
  }

  subirAvatar(file: File): Observable<{ avatarUrl: string }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ avatarUrl: string }>(`${this.API_URL}/me/avatar`, formData);
  }
}
