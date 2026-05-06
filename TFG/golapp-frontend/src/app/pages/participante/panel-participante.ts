import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ParticipanteService, EquipoPublico, PartidoPublico, JugadorPublico } from '../../core/services/participante.service';

type Tab = 'partidos' | 'plantilla';

@Component({
  selector: 'app-panel-participante',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './panel-participante.html',
  styleUrl: './panel-participante.css'
})
export class PanelParticipante implements OnInit {

  private readonly router = inject(Router);
  private readonly service = inject(ParticipanteService);

  protected readonly equipo = signal<EquipoPublico | null>(null);
  protected readonly partidos = signal<PartidoPublico[]>([]);
  protected readonly jugadores = signal<JugadorPublico[]>([]);
  protected readonly cargando = signal(true);
  protected readonly tab = signal<Tab>('partidos');
  private codigo = '';

  ngOnInit(): void {
    this.codigo = localStorage.getItem('golapp_equipo_codigo') || '';
    if (!this.codigo) { this.router.navigate(['/entrar']); return; }
    this.cargarDatos();
  }

  private cargarDatos(): void {
    this.cargando.set(true);
    this.service.obtenerEquipo(this.codigo).subscribe({
      next: (e) => {
        this.equipo.set(e);
        this.service.obtenerPartidos(this.codigo).subscribe(p => this.partidos.set(p));
        this.service.obtenerPlantilla(this.codigo).subscribe(j => this.jugadores.set(j));
        this.cargando.set(false);
      },
      error: () => { localStorage.removeItem('golapp_equipo_codigo'); this.router.navigate(['/entrar']); }
    });
  }

  protected setTab(t: Tab): void { this.tab.set(t); }

  protected salir(): void {
    localStorage.removeItem('golapp_equipo_codigo');
    this.router.navigate(['/entrar']);
  }

  protected formatFecha(fecha: string | null): string {
    if (!fecha) return 'Sin fecha';
    return new Date(fecha).toLocaleDateString('es-ES', { weekday: 'short', day: '2-digit', month: 'short' });
  }

  protected formatHora(fecha: string | null): string {
    if (!fecha) return '—';
    return new Date(fecha).toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' });
  }

  protected getEstadoClasses(estado: string): string {
    switch (estado) {
      case 'PROGRAMADO': return 'text-blue-400 bg-blue-500/10 border-blue-500/30';
      case 'FINALIZADO': return 'text-golapp-text-muted bg-slate-500/10 border-slate-500/30';
      default: return 'text-golapp-text-muted bg-golapp-dark border-golapp-border';
    }
  }

  protected posicionLabel(pos: string): string {
    const map: Record<string, string> = { PORTERO: '🧤 POR', DEFENSA: '🛡️ DEF', CENTROCAMPISTA: '🎯 MED', DELANTERO: '⚡ DEL' };
    return map[pos] || pos;
  }
}
