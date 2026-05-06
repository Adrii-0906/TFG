import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ArchivoService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = 'http://localhost:8089/api/archivos';

  /**
   * Sube una imagen al servidor y devuelve la URL generada.
   * POST /api/archivos/subir
   */
  subirImagen(archivo: File): Observable<{ url: string }> {
    const formData = new FormData();
    formData.append('file', archivo);
    return this.http.post<{ url: string }>(`${this.API_URL}/subir`, formData);
  }
}
