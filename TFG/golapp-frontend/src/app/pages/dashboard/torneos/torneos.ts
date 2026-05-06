import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TorneoService, TorneoResponse } from '../../../core/services/torneo.service';
import { AuthService } from '../../../core/services/auth.service';

type TipoTorneo = 'LIGA' | 'ELIMINATORIA';

@Component({
  selector: 'app-torneos',
  imports: [RouterLink],
  templateUrl: './torneos.html',
  styleUrl: './torneos.css'
})
export class Torneos implements OnInit {

  private readonly torneoService = inject(TorneoService);
  private readonly authService = inject(AuthService);

  // ── Estado ────────────────────────────────────
  protected readonly torneos = signal<TorneoResponse[]>([]);
  protected readonly cargando = signal(true);

  // ── Lifecycle ─────────────────────────────────
  ngOnInit(): void {
    const user = this.authService.obtenerUsuario();
    if (user?.userId) {
      this.torneoService.obtenerMisTorneos(user.userId).subscribe({
        next: (data) => {
          this.torneos.set(data);
          this.cargando.set(false);
        },
        error: () => {
          this.cargando.set(false);
        }
      });
    } else {
      this.cargando.set(false);
    }
  }

  // ── Helpers ───────────────────────────────────
  protected getTipoClasses(tipo: TipoTorneo): string {
    return tipo === 'LIGA'
      ? 'text-golapp-blue bg-golapp-blue/10'
      : 'text-golapp-green bg-golapp-green/10';
  }

  protected formatFecha(fecha: string): string {
    if (!fecha) return '—';
    const d = new Date(fecha);
    return d.toLocaleDateString('es-ES', { day: '2-digit', month: 'short', year: 'numeric' });
  }
}
