import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TorneoService, TorneoResponse } from '../../../../core/services/torneo.service';
import { EquipoService, EquipoResponse } from '../../../../core/services/equipo.service';
import { PartidoService, PartidoResponse, EventoDto } from '../../../../core/services/partido.service';
import { JugadorService, JugadorResponse } from '../../../../core/services/jugador.service';

type TabActivo = 'resumen' | 'equipos' | 'calendario' | 'clasificacion';

@Component({
  selector: 'app-torneo-detalle',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule, FormsModule],
  templateUrl: './torneo-detalle.html',
  styleUrl: './torneo-detalle.css'
})
export class TorneoDetalle implements OnInit {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly torneoService = inject(TorneoService);
  private readonly equipoService = inject(EquipoService);
  private readonly partidoService = inject(PartidoService);
  private readonly jugadorService = inject(JugadorService);

  // ── Estado ────────────────────────────────────
  protected readonly torneo = signal<TorneoResponse | null>(null);
  protected readonly equiposTorneo = signal<EquipoResponse[]>([]);
  protected readonly misEquipos = signal<EquipoResponse[]>([]);
  protected readonly partidosTorneo = signal<PartidoResponse[]>([]);
  protected readonly clasificacion = signal<any[]>([]);
  protected readonly bracket = signal<{ [fase: string]: any[] }>({});
  protected readonly cargando = signal(true);
  protected readonly tabActivo = signal<TabActivo>('equipos');
  protected readonly mostrarFormulario = signal(false);
  protected readonly guardandoEquipo = signal(false);
  protected readonly mostrarFormPartido = signal(false);
  protected readonly guardandoPartido = signal(false);
  protected readonly sorteando = signal(false);
  protected errorEquipo = '';
  protected errorPartido = '';
  protected errorSorteo = '';
  protected exitoSorteo = '';

  // Estado del Acta
  protected jugadoresLocal = signal<JugadorResponse[]>([]);
  protected jugadoresVisitante = signal<JugadorResponse[]>([]);
  protected eventosTemporales = signal<EventoDto[]>([]);
  protected eventoJugadorId = 0;
  protected eventoTipo: 'GOL' | 'AMARILLA' | 'ROJA' = 'GOL';

  private torneoId = 0;

  // ── Tabs ──────────────────────────────────────
  protected readonly tabs: { label: string; value: TabActivo; icon: string }[] = [
    { label: 'Resumen', value: 'resumen', icon: 'M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z' },
    { label: 'Equipos', value: 'equipos', icon: 'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z' },
    { label: 'Calendario', value: 'calendario', icon: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z' },
    { label: 'Clasificación', value: 'clasificacion', icon: 'M3 10h18M3 14h18m-9-4v8m-7 0h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z' }
  ];

  // ── Formularios ───────────────────────────────
  protected readonly vincularForm: FormGroup = this.fb.group({ equipoId: ['', Validators.required] });
  protected readonly partidoForm: FormGroup = this.fb.group({
    equipoLocalId: ['', Validators.required],
    equipoVisitanteId: ['', Validators.required],
    fechaPartido: ['', Validators.required]
  });

  // ── Lifecycle ─────────────────────────────────
  ngOnInit(): void {
    this.torneoId = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.torneoId) { this.router.navigate(['/dashboard/torneos']); return; }
    this.cargarDatos();
  }

  private cargarDatos(): void {
    this.cargando.set(true);
    this.torneoService.obtenerPorId(this.torneoId).subscribe({
      next: (torneo) => { this.torneo.set(torneo); this.cargarEquipos(); this.cargarPartidos(); },
      error: () => { this.cargando.set(false); this.router.navigate(['/dashboard/torneos']); }
    });
  }

  private cargarEquipos(): void {
    this.equipoService.obtenerEquiposPorTorneo(this.torneoId).subscribe({
      next: (equipos) => { this.equiposTorneo.set(equipos); this.cargando.set(false); },
      error: () => this.cargando.set(false)
    });
  }

  private cargarMisEquipos(): void {
    this.equipoService.obtenerMisEquipos().subscribe({ next: (e) => this.misEquipos.set(e) });
  }

  protected cargarPartidos(): void {
    this.partidoService.obtenerPorTorneo(this.torneoId).subscribe({
      next: (p) => this.partidosTorneo.set(p), error: () => {}
    });
  }

  protected get equiposDisponibles(): EquipoResponse[] {
    const ids = new Set(this.equiposTorneo().map(e => e.id));
    return this.misEquipos().filter(e => !ids.has(e.id));
  }

  protected get bracketFases(): string[] {
    return Object.keys(this.bracket());
  }

  /** Primera ronda: mitad izquierda */
  protected get bracketIzquierdo(): any[] {
    const b = this.bracket();
    const fases = Object.keys(b);
    if (fases.length === 0) return [];
    const primera = b[fases[0]] || [];
    const mitad = Math.ceil(primera.length / 2);
    return primera.slice(0, mitad);
  }

  /** Primera ronda: mitad derecha */
  protected get bracketDerecho(): any[] {
    const b = this.bracket();
    const fases = Object.keys(b);
    if (fases.length === 0) return [];
    const primera = b[fases[0]] || [];
    const mitad = Math.ceil(primera.length / 2);
    return primera.slice(mitad);
  }

  /** Nombre de la primera fase */
  protected get primeraFase(): string {
    const fases = Object.keys(this.bracket());
    return fases.length > 0 ? fases[0] : '';
  }

  /** Rondas intermedias (sin la primera) */
  protected get rondasIntermedias(): { fase: string; partidos: any[] }[] {
    const b = this.bracket();
    const fases = Object.keys(b);
    if (fases.length <= 1) return [];
    return fases.slice(1).map(f => ({ fase: f, partidos: b[f] || [] }));
  }

  /** Torneo editable (BORRADOR o null/legacy) */
  protected get esBorrador(): boolean {
    const t = this.torneo();
    return !t || !t.estado || t.estado === 'BORRADOR';
  }

  /** Partidos agrupados por fase/jornada para el calendario */
  protected get partidosPorFase(): { fase: string; partidos: PartidoResponse[] }[] {
    const mapa = new Map<string, PartidoResponse[]>();
    for (const p of this.partidosTorneo()) {
      const key = p.fase || (p.jornada ? `Jornada ${p.jornada}` : 'Sin asignar');
      if (!mapa.has(key)) mapa.set(key, []);
      mapa.get(key)!.push(p);
    }
    return Array.from(mapa.entries()).map(([fase, partidos]) => ({ fase, partidos }));
  }

  // ── Tabs ──────────────────────────────────────
  protected setTab(tab: TabActivo): void {
    this.tabActivo.set(tab);
    if (tab === 'calendario') this.cargarPartidos();
    if (tab === 'clasificacion') this.cargarClasificacion();
  }

  // ── Sorteo ────────────────────────────────────
  protected sortearTorneo(): void {
    this.sorteando.set(true);
    this.errorSorteo = '';
    this.exitoSorteo = '';
    this.torneoService.generarCalendario(this.torneoId).subscribe({
      next: () => {
        this.sorteando.set(false);
        this.exitoSorteo = '¡Calendario generado con éxito! El torneo está ACTIVO.';
        this.cargarDatos();
      },
      error: (err) => {
        this.sorteando.set(false);
        this.errorSorteo = err.error?.error || 'Error al generar el calendario.';
      }
    });
  }

  // ── Clasificación ─────────────────────────────
  private cargarClasificacion(): void {
    const t = this.torneo();
    if (!t) return;
    if (t.tipoTorneo === 'LIGA') {
      this.torneoService.getClasificacionLiga(this.torneoId).subscribe({
        next: (c) => this.clasificacion.set(c), error: () => {}
      });
    } else {
      this.torneoService.getBracket(this.torneoId).subscribe({
        next: (b) => this.bracket.set(b), error: () => {}
      });
    }
  }

  // ── Equipos ───────────────────────────────────
  protected toggleFormulario(): void {
    this.mostrarFormulario.update(v => !v);
    this.errorEquipo = '';
    if (this.mostrarFormulario()) this.cargarMisEquipos();
    this.vincularForm.reset();
  }

  protected vincularEquipo(): void {
    if (this.vincularForm.invalid) { this.vincularForm.markAllAsTouched(); return; }
    this.guardandoEquipo.set(true); this.errorEquipo = '';
    const equipoId = Number(this.vincularForm.value.equipoId);
    this.equipoService.vincularEquipoATorneo(this.torneoId, equipoId).subscribe({
      next: () => { this.guardandoEquipo.set(false); this.mostrarFormulario.set(false); this.vincularForm.reset(); this.cargarEquipos(); },
      error: (err) => { this.guardandoEquipo.set(false); this.errorEquipo = err.error?.error || 'Error al vincular.'; }
    });
  }

  protected desvincularEquipo(equipoId: number): void {
    this.equipoService.eliminarEquipoDeTorneo(this.torneoId, equipoId).subscribe({
      next: () => this.cargarEquipos(),
      error: (err) => { this.errorEquipo = err.error?.error || 'Error al desvincular.'; }
    });
  }

  // ── Partidos ──────────────────────────────────
  protected toggleFormPartido(): void {
    this.mostrarFormPartido.update(v => !v);
    this.errorPartido = '';
    this.partidoForm.reset();
  }

  protected crearPartido(): void {
    if (this.partidoForm.invalid) { this.partidoForm.markAllAsTouched(); return; }
    this.guardandoPartido.set(true); this.errorPartido = '';
    const dto = {
      equipoLocalId: Number(this.partidoForm.value.equipoLocalId),
      equipoVisitanteId: Number(this.partidoForm.value.equipoVisitanteId),
      fechaPartido: this.partidoForm.value.fechaPartido
    };
    this.partidoService.crearPartido(this.torneoId, dto).subscribe({
      next: () => { this.guardandoPartido.set(false); this.mostrarFormPartido.set(false); this.partidoForm.reset(); this.cargarPartidos(); },
      error: (err) => { this.guardandoPartido.set(false); this.errorPartido = err.error?.error || 'Error al crear.'; }
    });
  }

  // ── Edición de fecha ──────────────────────────
  protected editandoFechaId: number | null = null;
  protected nuevaFecha = '';

  protected editarFecha(partido: PartidoResponse): void {
    this.editandoFechaId = partido.id;
    this.nuevaFecha = partido.fechaPartido ? partido.fechaPartido.substring(0, 16) : '';
  }

  protected cancelarEdicionFecha(): void {
    this.editandoFechaId = null;
    this.nuevaFecha = '';
  }

  protected guardarFecha(partidoId: number): void {
    if (!this.nuevaFecha) return;
    this.partidoService.actualizarFecha(partidoId, this.nuevaFecha).subscribe({
      next: () => { this.editandoFechaId = null; this.nuevaFecha = ''; this.cargarPartidos(); },
      error: () => {}
    });
  }

  // ── Edición de resultado (Acta) ─────────────────
  protected editandoResultadoId: number | null = null;
  protected golesLocalForm: number = 0;
  protected golesVisitanteForm: number = 0;

  protected editarResultado(partido: PartidoResponse): void {
    this.editandoResultadoId = partido.id;
    this.golesLocalForm = partido.golesLocal || 0;
    this.golesVisitanteForm = partido.golesVisitante || 0;
    this.eventosTemporales.set([]);
    this.eventoJugadorId = 0;
    this.eventoTipo = 'GOL';

    this.jugadoresLocal.set([]);
    this.jugadoresVisitante.set([]);
    this.jugadorService.obtenerJugadoresPublico(partido.equipoLocal.id).subscribe({ next: (j) => this.jugadoresLocal.set(j), error: () => {} });
    this.jugadorService.obtenerJugadoresPublico(partido.equipoVisitante.id).subscribe({ next: (j) => this.jugadoresVisitante.set(j), error: () => {} });
  }

  protected cancelarEdicionResultado(): void {
    this.editandoResultadoId = null;
  }

  protected agregarEventoTemporal(): void {
    if (this.eventoJugadorId <= 0) return;
    this.eventosTemporales.update(e => [...e, {
      jugadorId: Number(this.eventoJugadorId),
      tipoEvento: this.eventoTipo
    }]);
    this.eventoJugadorId = 0;
  }

  protected eliminarEventoTemporal(index: number): void {
    this.eventosTemporales.update(e => e.filter((_, i) => i !== index));
  }

  protected getNombreJugador(id: number): string {
    const j = [...this.jugadoresLocal(), ...this.jugadoresVisitante()].find(x => x.id === id);
    return j ? j.nombreCompleto : 'Desconocido';
  }

  protected guardarResultado(partidoId: number): void {
    if (this.golesLocalForm < 0 || this.golesVisitanteForm < 0) return;
    
    const req = {
      golesLocal: this.golesLocalForm,
      golesVisitante: this.golesVisitanteForm,
      eventos: this.eventosTemporales()
    };

    this.partidoService.registrarResultado(partidoId, req).subscribe({
      next: () => {
        this.editandoResultadoId = null;
        this.cargarDatos(); // Recargar torneo y partidos para actualizar brackets/clasificación y estado del torneo
      },
      error: () => {}
    });
  }

  // ── Helpers ───────────────────────────────────
  protected formatFecha(fecha: string): string {
    if (!fecha) return '—';
    return new Date(fecha).toLocaleDateString('es-ES', { day: '2-digit', month: 'long', year: 'numeric' });
  }
  protected formatFechaCorta(fecha: string | null): string {
    if (!fecha) return 'Sin fecha';
    return new Date(fecha).toLocaleDateString('es-ES', { weekday: 'short', day: '2-digit', month: 'short' });
  }
  protected formatHora(fecha: string | null): string {
    if (!fecha) return '—';
    return new Date(fecha).toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' });
  }
  protected getTipoClasses(tipo: string): string {
    return tipo === 'LIGA' ? 'text-golapp-blue bg-golapp-blue/10' : 'text-golapp-green bg-golapp-green/10';
  }
  protected getEstadoTorneoClasses(estado: string): string {
    switch (estado) {
      case 'BORRADOR': return 'text-golapp-warning bg-golapp-warning/10 border-golapp-warning/30';
      case 'ACTIVO': return 'text-golapp-green bg-golapp-green/10 border-golapp-green/30';
      case 'FINALIZADO': return 'text-golapp-text-muted bg-slate-500/10 border-slate-500/30';
      default: return '';
    }
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
      default: return '';
    }
  }
  protected getEstadoLabel(estado: string, fecha?: string | null): string {
    if (estado === 'FINALIZADO') return 'Completado';
    if (estado !== 'FINALIZADO' && this.partidoYaPasado(fecha ?? null)) return '⚠️ Falta Resultado';
    const m: Record<string, string> = { PROGRAMADO: 'Programado', EN_CURSO: 'En Curso', SUSPENDIDO: 'Suspendido' };
    return m[estado] || estado;
  }
  protected getPosClasses(i: number): string {
    if (i === 0) return 'text-yellow-400';
    if (i === 1) return 'text-slate-300';
    if (i === 2) return 'text-amber-600';
    return 'text-golapp-text-muted';
  }
  protected campo(nombre: string) { return this.vincularForm.get(nombre)!; }
  protected campoPartido(nombre: string) { return this.partidoForm.get(nombre)!; }
}
