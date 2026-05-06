import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TorneoService, TorneoResponse } from '../../core/services/torneo.service';
import { AuthService } from '../../core/services/auth.service';

interface Estadistica {
  label: string;
  valor: string;
  iconPath: string;
  colorClasses: string;
}

@Component({
  selector: 'app-dashboard-home',
  imports: [RouterLink],
  templateUrl: './dashboard-home.html',
  styleUrl: './dashboard-home.css'
})
export class DashboardHome implements OnInit {

  private readonly torneoService = inject(TorneoService);
  private readonly authService = inject(AuthService);

  // ── Estado ────────────────────────────────────
  protected readonly torneos = signal<TorneoResponse[]>([]);
  protected readonly cargando = signal(true);
  protected nombreUsuario = '';

  // ── KPIs reactivos (se calculan a partir de los torneos reales) ──
  protected readonly estadisticas = computed<Estadistica[]>(() => {
    const lista = this.torneos();
    const totalEquipos = lista.reduce((sum, t) => sum + (t.equipos?.length || 0), 0);

    return [
      {
        label: 'Mis Torneos',
        valor: String(lista.length),
        iconPath: 'M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z',
        colorClasses: 'text-golapp-green bg-golapp-green/10'
      },
      {
        label: 'Equipos Participantes',
        valor: String(totalEquipos),
        iconPath: 'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z',
        colorClasses: 'text-golapp-blue bg-golapp-blue/10'
      },
      {
        label: 'Partidos Programados',
        valor: '0',
        iconPath: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z',
        colorClasses: 'text-golapp-warning bg-golapp-warning/10'
      }
    ];
  });

  // ── Lifecycle ─────────────────────────────────
  ngOnInit(): void {
    const user = this.authService.obtenerUsuario();
    this.nombreUsuario = user?.nombre || 'Organizador';

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
  protected getTipoClasses(tipo: string): string {
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
