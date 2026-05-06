import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { EquipoService } from '../../../../core/services/equipo.service';
import { ArchivoService } from '../../../../core/services/archivo.service';

@Component({
  selector: 'app-nuevo-equipo',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './nuevo-equipo.html',
  styleUrl: './nuevo-equipo.css'
})
export class NuevoEquipo {

  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly equipoService = inject(EquipoService);
  private readonly archivoService = inject(ArchivoService);

  // ── Estado ────────────────────────────────────
  protected readonly guardando = signal(false);
  protected errorMsg = '';
  protected exitoMsg = '';
  protected escudoUrl = signal<string | null>(null);

  // ── Formulario ────────────────────────────────
  protected readonly form: FormGroup = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
    nombreDelegado: ['', Validators.maxLength(100)],
    telefonoContacto: ['', Validators.maxLength(20)],
    emailContacto: ['', [Validators.maxLength(150), Validators.email]]
  });

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      this.guardando.set(true);
      this.archivoService.subirImagen(file).subscribe({
        next: (res) => {
          this.escudoUrl.set(res.url);
          this.guardando.set(false);
        },
        error: () => {
          this.errorMsg = 'Error al subir el escudo. Inténtalo de nuevo.';
          this.guardando.set(false);
        }
      });
    }
  }

  // ── Submit ────────────────────────────────────
  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    this.errorMsg = '';
    this.exitoMsg = '';

    const data = {
      nombre: this.form.value.nombre.trim(),
      escudoUrl: this.escudoUrl() || undefined,
      nombreDelegado: this.form.value.nombreDelegado?.trim() || undefined,
      telefonoContacto: this.form.value.telefonoContacto?.trim() || undefined,
      emailContacto: this.form.value.emailContacto?.trim() || undefined
    };

    this.equipoService.crearEquipo(data).subscribe({
      next: () => {
        this.guardando.set(false);
        this.exitoMsg = '¡Equipo registrado correctamente!';
        setTimeout(() => {
          this.router.navigate(['/dashboard/equipos']);
        }, 1200);
      },
      error: (err) => {
        this.guardando.set(false);
        this.errorMsg = err.error?.error || 'Error al crear el equipo. Inténtalo de nuevo.';
      }
    });
  }

  // ── Helper ────────────────────────────────────
  protected campo(nombre: string) {
    return this.form.get(nombre)!;
  }
}
