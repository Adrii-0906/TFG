import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PartidoService, PartidoResponse } from '../../../core/services/partido.service';

@Component({
  selector: 'app-calendario',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './calendario.html',
  styleUrl: './calendario.css'
})
export class Calendario implements OnInit {

  private readonly partidoService = inject(PartidoService);

  // ── Estado ────────────────────────────────────
  protected readonly partidos = signal<PartidoResponse[]>([]);
  protected readonly cargando = signal(true);

  // ── Lifecycle ─────────────────────────────────
  ngOnInit(): void {
    this.cargarPartidos();
  }

  private cargarPartidos(): void {
    this.cargando.set(true);
    this.partidoService.obtenerMisPartidos().subscribe({
      next: (data) => {
        this.partidos.set(data);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false)
    });
  }

  /** Partidos agrupados por torneo */
  protected get partidosPorTorneo(): { torneo: string; torneoId: number; partidos: PartidoResponse[] }[] {
    const mapa = new Map<string, { torneoId: number; partidos: PartidoResponse[] }>();
    for (const p of this.partidos()) {
      const key = p.torneoNombre || 'Sin torneo';
      if (!mapa.has(key)) mapa.set(key, { torneoId: p.torneoId, partidos: [] });
      mapa.get(key)!.partidos.push(p);
    }
    return Array.from(mapa.entries()).map(([torneo, data]) => ({ torneo, torneoId: data.torneoId, partidos: data.partidos }));
  }

  // ── Helpers ───────────────────────────────────
  protected formatFecha(fecha: string | null): string {
    if (!fecha) return 'Sin fecha';
    const d = new Date(fecha);
    return d.toLocaleDateString('es-ES', {
      weekday: 'short',
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    });
  }

  protected formatHora(fecha: string | null): string {
    if (!fecha) return '—';
    const d = new Date(fecha);
    return d.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' });
  }

  protected partidoYaPasado(fecha: string | null): boolean {
    if (!fecha) return false;
    return new Date(fecha) < new Date();
  }

  protected getEstadoClasses(estado: string, fecha?: string | null): string {
    if (estado === 'FINALIZADO') return 'text-emerald-400 bg-emerald-500/10 border-emerald-500/30';
    if (estado !== 'FINALIZADO' && this.partidoYaPasado(fecha ?? null)) return 'text-orange-400 bg-orange-500/10 border-orange-500/30';
    switch (estado) {
      case 'PROGRAMADO': return 'text-golapp-blue bg-golapp-blue/10 border-golapp-blue/30';
      case 'EN_CURSO': return 'text-golapp-green bg-golapp-green/10 border-golapp-green/30';
      case 'SUSPENDIDO': return 'text-red-400 bg-red-500/10 border-red-500/30';
      default: return 'text-golapp-text-muted bg-golapp-dark';
    }
  }

  protected getEstadoLabel(estado: string, fecha?: string | null): string {
    if (estado === 'FINALIZADO') return 'Completado';
    if (estado !== 'FINALIZADO' && this.partidoYaPasado(fecha ?? null)) return '⚠️ Falta Resultado';
    switch (estado) {
      case 'PROGRAMADO': return 'Programado';
      case 'EN_CURSO': return 'En Curso';
      case 'SUSPENDIDO': return 'Suspendido';
      default: return estado;
    }
  }
}
