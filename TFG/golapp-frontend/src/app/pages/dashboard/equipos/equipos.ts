import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { EquipoService, EquipoResponse } from '../../../core/services/equipo.service';

@Component({
  selector: 'app-equipos',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './equipos.html',
  styleUrl: './equipos.css'
})
export class Equipos implements OnInit {

  private readonly equipoService = inject(EquipoService);

  // ── Estado ────────────────────────────────────
  protected readonly equipos = signal<EquipoResponse[]>([]);
  protected readonly cargando = signal(true);
  protected readonly searchQuery = signal('');

  // ── Filtrado reactivo ─────────────────────────
  protected readonly equiposFiltrados = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    if (!query) return this.equipos();
    return this.equipos().filter(e =>
      e.nombre.toLowerCase().includes(query) ||
      (e.nombreDelegado?.toLowerCase().includes(query) ?? false)
    );
  });

  // ── Lifecycle ─────────────────────────────────
  ngOnInit(): void {
    this.cargarEquipos();
  }

  private cargarEquipos(): void {
    this.cargando.set(true);
    this.equipoService.obtenerMisEquipos().subscribe({
      next: (equipos) => {
        this.equipos.set(equipos);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
      }
    });
  }

  // ── Acciones ──────────────────────────────────
  protected onDelete(equipo: EquipoResponse): void {
    this.equipoService.eliminarEquipo(equipo.id).subscribe({
      next: () => this.cargarEquipos()
    });
  }

  protected updateSearch(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchQuery.set(input.value);
  }

  // ── Selección ─────────────────────────────────
  protected readonly equiposSeleccionados = signal<Set<number>>(new Set());

  protected toggleSeleccion(id: number): void {
    const seleccionados = new Set(this.equiposSeleccionados());
    if (seleccionados.has(id)) {
      seleccionados.delete(id);
    } else {
      seleccionados.add(id);
    }
    this.equiposSeleccionados.set(seleccionados);
  }

  protected toggleSeleccionarTodos(): void {
    const filtradosIds = this.equiposFiltrados().map(e => e.id);
    const seleccionados = this.equiposSeleccionados();
    
    // Si están todos seleccionados (de la vista filtrada), desmarcamos todos los visibles
    const todosSeleccionados = filtradosIds.every(id => seleccionados.has(id));
    
    const nuevosSeleccionados = new Set(seleccionados);
    if (todosSeleccionados) {
      filtradosIds.forEach(id => nuevosSeleccionados.delete(id));
    } else {
      filtradosIds.forEach(id => nuevosSeleccionados.add(id));
    }
    
    this.equiposSeleccionados.set(nuevosSeleccionados);
  }

  protected isSeleccionado(id: number): boolean {
    return this.equiposSeleccionados().has(id);
  }

  protected get todosSeleccionados(): boolean {
    const filtradosIds = this.equiposFiltrados();
    if (filtradosIds.length === 0) return false;
    return filtradosIds.every(e => this.equiposSeleccionados().has(e.id));
  }

  // ── Import / Export ───────────────────────────
  protected exportarEquipos(): void {
    const idsAExportar = Array.from(this.equiposSeleccionados());
    if (idsAExportar.length === 0) return;

    this.equipoService.exportarEquipos(idsAExportar).subscribe({
      next: (data) => {
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'mis_equipos_golapp.json';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: () => alert('Error al exportar los equipos.')
    });
  }

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      const reader = new FileReader();
      
      reader.onload = (e) => {
        try {
          const json = JSON.parse(e.target?.result as string);
          if (!Array.isArray(json)) throw new Error('El archivo no tiene el formato correcto.');
          
          this.cargando.set(true);
          this.equipoService.importarEquipos(json).subscribe({
            next: () => {
              alert('Equipos importados correctamente.');
              this.cargarEquipos();
            },
            error: (err) => {
              alert('Error al importar: ' + (err.error?.error || 'Verifica el formato del archivo.'));
              this.cargando.set(false);
            }
          });
        } catch (error) {
          alert('Error: El archivo no es un JSON válido.');
        }
      };
      
      reader.readAsText(file);
      input.value = ''; // Reset the input so the same file can be selected again
    }
  }

  // ── Helpers ───────────────────────────────────
  protected getInitials(name: string | null): string {
    if (!name) return '?';
    const parts = name.trim().split(' ');
    return (parts[0]?.charAt(0) || '') + (parts[1]?.charAt(0) || '');
  }
}
