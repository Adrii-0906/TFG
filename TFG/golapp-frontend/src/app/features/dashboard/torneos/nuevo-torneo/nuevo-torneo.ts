import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TorneoService, CrearTorneoRequest } from '../../../../core/services/torneo.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ArchivoService } from '../../../../core/services/archivo.service';

@Component({
  selector: 'app-nuevo-torneo',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './nuevo-torneo.html',
  styleUrl: './nuevo-torneo.css'
})
export class NuevoTorneo {

  private readonly fb = inject(FormBuilder);
  private readonly torneoService = inject(TorneoService);
  private readonly authService = inject(AuthService);
  private readonly archivoService = inject(ArchivoService);
  private readonly router = inject(Router);

  protected cargando = false;
  protected errorMsg = '';
  protected fondoUrl = signal<string | null>(null);

  protected readonly form: FormGroup = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
    descripcion: ['', [Validators.required, Validators.maxLength(500)]],
    tipoTorneo: ['', Validators.required],
    fechaInicio: ['', Validators.required],
    fechaFin: ['', Validators.required]
  });

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      this.cargando = true;
      this.archivoService.subirImagen(file).subscribe({
        next: (res) => {
          this.fondoUrl.set(res.url);
          this.cargando = false;
        },
        error: () => {
          this.errorMsg = 'Error al subir la imagen. Inténtalo de nuevo.';
          this.cargando = false;
        }
      });
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const usuario = this.authService.obtenerUsuario();
    if (!usuario?.userId) {
      this.errorMsg = 'No se pudo identificar al usuario. Inicia sesión de nuevo.';
      return;
    }

    this.cargando = true;
    this.errorMsg = '';

    const data: CrearTorneoRequest = {
      nombre: this.form.value.nombre.trim(),
      descripcion: this.form.value.descripcion.trim(),
      fondoUrl: this.fondoUrl() || undefined,
      tipoTorneo: this.form.value.tipoTorneo,
      fechaInicio: this.form.value.fechaInicio,
      fechaFin: this.form.value.fechaFin
    };

    this.torneoService.crearTorneo(data, usuario.userId).subscribe({
      next: () => {
        this.router.navigate(['/dashboard/torneos']);
      },
      error: (err) => {
        this.cargando = false;
        this.errorMsg = err.error?.error || 'Error al crear el torneo. Inténtalo de nuevo.';
      }
    });
  }

  /** Helper para acceso rápido a los controles del formulario */
  protected campo(nombre: string) {
    return this.form.get(nombre)!;
  }

  protected get fechaMinima(): string {
    return new Date().toISOString().split('T')[0];
  }
}
