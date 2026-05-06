import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

/** Respuesta del backend tras login/registro exitoso */
export interface AuthResponse {
  token: string;
  tipo: string;
  userId: number;
  username: string;
  email: string;
  nombre: string;
  apellidos: string;
  rol: 'ORGANIZADOR' | 'PARTICIPANTE';
}

/** Payload para login */
export interface LoginRequest {
  email: string;
  password: string;
}

/** Payload para registro */
export interface RegistroRequest {
  username: string;
  email: string;
  password: string;
  nombre: string;
  apellidos?: string;
}

/**
 * Servicio de autenticación.
 * Gestiona login, registro, almacenamiento del JWT y estado de sesión.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly API_URL = 'http://localhost:8089/api/auth';
  private readonly TOKEN_KEY = 'golapp_token';
  private readonly USER_KEY = 'golapp_user';

  // ── Login ─────────────────────────────────────
  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/login`, credentials).pipe(
      tap(response => {
        this.guardarToken(response.token);
        this.guardarUsuario(response);
      })
    );
  }

  // ── Registro ──────────────────────────────────
  registro(data: RegistroRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/registro`, data).pipe(
      tap(response => {
        this.guardarToken(response.token);
        this.guardarUsuario(response);
      })
    );
  }

  // ── Token Management ──────────────────────────
  guardarToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  obtenerToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  private guardarUsuario(user: AuthResponse): void {
    localStorage.setItem(this.USER_KEY, JSON.stringify({
      userId: user.userId,
      username: user.username,
      email: user.email,
      nombre: user.nombre,
      apellidos: user.apellidos,
      rol: user.rol
    }));
  }

  obtenerUsuario(): Partial<AuthResponse> | null {
    const raw = localStorage.getItem(this.USER_KEY);
    return raw ? JSON.parse(raw) : null;
  }

  estaLogueado(): boolean {
    const token = this.obtenerToken();
    return token !== null && token.length > 0;
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.router.navigate(['/']);
  }
}
