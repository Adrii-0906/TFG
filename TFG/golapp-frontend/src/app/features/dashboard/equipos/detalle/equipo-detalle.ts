import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { EquipoService, EquipoResponse } from '../../../../core/services/equipo.service';
import { JugadorService, JugadorResponse, CrearJugadorRequest } from '../../../../core/services/jugador.service';

@Component({
  selector: 'app-equipo-detalle',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './equipo-detalle.html',
  styleUrl: './equipo-detalle.css'
})
export class EquipoDetalle implements OnInit {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly equipoService = inject(EquipoService);
  private readonly jugadorService = inject(JugadorService);

  // ── Estado ────────────────────────────────────
  protected readonly equipo = signal<EquipoResponse | null>(null);
  protected readonly jugadores = signal<JugadorResponse[]>([]);
  protected readonly cargando = signal(true);
  protected readonly mostrarFormulario = signal(false);
  protected readonly guardando = signal(false);
  protected errorMsg = '';
  protected readonly codigoCopied = signal(false);

  private equipoId = 0;

  // ── Posiciones para el select ─────────────────
  protected readonly posiciones = ['Portero', 'Defensa', 'Centrocampista', 'Delantero'];

  // ── Formulario ────────────────────────────────
  protected readonly jugadorForm: FormGroup = this.fb.group({
    nombreCompleto: ['', [Validators.required, Validators.maxLength(150)]],
    dorsal: [null],
    posicion: ['']
  });

  // ── Variables calculadas (plantilla) ──────────
  protected readonly totalJugadores = computed(() => this.jugadores().length);
  protected readonly plantillaCompleta = computed(() => this.totalJugadores() >= 22);
  protected readonly plantillaMinima = computed(() => this.totalJugadores() >= 11);

  // ── Agrupación por posición ───────────────────
  protected readonly porteros = computed(() =>
    this.jugadores().filter(j => j.posicion === 'Portero'));
  protected readonly defensas = computed(() =>
    this.jugadores().filter(j => j.posicion === 'Defensa'));
  protected readonly centrocampistas = computed(() =>
    this.jugadores().filter(j => j.posicion === 'Centrocampista'));
  protected readonly delanteros = computed(() =>
    this.jugadores().filter(j => j.posicion === 'Delantero'));
  protected readonly sinPosicion = computed(() =>
    this.jugadores().filter(j => !j.posicion || !this.posiciones.includes(j.posicion)));

  // ── Lifecycle ─────────────────────────────────
  ngOnInit(): void {
    this.equipoId = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.equipoId) {
      this.router.navigate(['/dashboard/equipos']);
      return;
    }
    this.cargarDatos();
  }

  private cargarDatos(): void {
    this.cargando.set(true);
    this.equipoService.obtenerPorId(this.equipoId).subscribe({
      next: (equipo) => {
        this.equipo.set(equipo);
        this.cargarJugadores();
      },
      error: () => {
        this.cargando.set(false);
        this.router.navigate(['/dashboard/equipos']);
      }
    });
  }

  private cargarJugadores(): void {
    this.jugadorService.obtenerJugadores(this.equipoId).subscribe({
      next: (jugadores) => {
        this.jugadores.set(jugadores);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false)
    });
  }

  // ── Acciones ──────────────────────────────────
  protected toggleFormulario(): void {
    this.mostrarFormulario.update(v => !v);
    this.errorMsg = '';
    if (!this.mostrarFormulario()) this.jugadorForm.reset();
  }

  protected guardarJugador(): void {
    if (this.jugadorForm.invalid) {
      this.jugadorForm.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    this.errorMsg = '';

    const data: CrearJugadorRequest = {
      nombreCompleto: this.jugadorForm.value.nombreCompleto.trim(),
      dorsal: this.jugadorForm.value.dorsal || undefined,
      posicion: this.jugadorForm.value.posicion || undefined
    };

    this.jugadorService.añadirJugador(this.equipoId, data).subscribe({
      next: () => {
        this.guardando.set(false);
        this.mostrarFormulario.set(false);
        this.jugadorForm.reset();
        this.cargarJugadores();
      },
      error: (err) => {
        this.guardando.set(false);
        this.errorMsg = err.error?.error || 'Error al añadir el jugador.';
      }
    });
  }

  protected eliminarJugador(jugadorId: number): void {
    this.jugadorService.eliminarJugador(this.equipoId, jugadorId).subscribe({
      next: () => this.cargarJugadores(),
      error: (err) => {
        this.errorMsg = err.error?.error || 'Error al eliminar el jugador.';
      }
    });
  }

  // ── Helpers ───────────────────────────────────
  protected campo(nombre: string) {
    return this.jugadorForm.get(nombre)!;
  }

  protected getPosicionColor(posicion: string | null): string {
    switch (posicion) {
      case 'Portero': return 'text-golapp-warning bg-golapp-warning/10';
      case 'Defensa': return 'text-golapp-blue bg-golapp-blue/10';
      case 'Centrocampista': return 'text-golapp-green bg-golapp-green/10';
      case 'Delantero': return 'text-red-400 bg-red-400/10';
      default: return 'text-golapp-text-muted bg-golapp-dark';
    }
  }

  protected getPosicionIcon(posicion: string): string {
    switch (posicion) {
      case 'Portero': return '🧤';
      case 'Defensa': return '🛡️';
      case 'Centrocampista': return '⚙️';
      case 'Delantero': return '⚡';
      default: return '👤';
    }
  }

  protected getPosicionTitleColor(posicion: string): string {
    switch (posicion) {
      case 'Portero': return 'text-golapp-warning';
      case 'Defensa': return 'text-golapp-blue';
      case 'Centrocampista': return 'text-golapp-green';
      case 'Delantero': return 'text-red-400';
      default: return 'text-golapp-text-muted';
    }
  }

  /** Porcentaje de plantilla para la barra de progreso (0-100) */
  protected readonly plantillaPorcentaje = computed(() =>
    Math.min(Math.round((this.totalJugadores() / 22) * 100), 100));

  protected copiarCodigo(): void {
    const codigo = this.equipo()?.codigoAcceso;
    if (!codigo) return;
    navigator.clipboard.writeText(codigo).then(() => {
      this.codigoCopied.set(true);
      setTimeout(() => this.codigoCopied.set(false), 2000);
    });
  }
}
