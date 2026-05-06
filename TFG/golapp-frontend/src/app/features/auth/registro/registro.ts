import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { EquipoService } from '../../../core/services/equipo.service';

@Component({
  selector: 'app-registro',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './registro.html',
  styleUrl: './registro.css'
})
export class Registro {

  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly equipoService = inject(EquipoService);
  private readonly router = inject(Router);

  protected readonly error = signal('');
  protected readonly cargando = signal(false);
  protected equiposAImportar: any[] | null = null;

  protected readonly form = this.fb.group({
    nombre: ['', [Validators.required]],
    apellidos: [''],
    username: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      const reader = new FileReader();
      
      reader.onload = (e) => {
        try {
          const json = JSON.parse(e.target?.result as string);
          if (!Array.isArray(json)) throw new Error('El archivo no tiene el formato correcto.');
          this.equiposAImportar = json;
          this.error.set(''); // Clear any previous errors
        } catch (error) {
          this.equiposAImportar = null;
          this.error.set('Error: El archivo no es un JSON válido para importar.');
          input.value = '';
        }
      };
      
      reader.readAsText(file);
    } else {
      this.equiposAImportar = null;
    }
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (this.error() && !this.equiposAImportar) {
      // Don't submit if there was a JSON parse error that wasn't cleared
      return;
    }

    this.cargando.set(true);
    this.error.set('');

    const data = this.form.getRawValue();

    this.authService.registro({
      nombre: data.nombre!,
      apellidos: data.apellidos || undefined,
      username: data.username!,
      email: data.email!,
      password: data.password!
    }).subscribe({
      next: () => {
        if (this.equiposAImportar) {
          // Si el usuario proporcionó equipos, los importamos usando su nueva sesión (token almacenado por AuthService)
          this.equipoService.importarEquipos(this.equiposAImportar).subscribe({
            next: () => {
              this.cargando.set(false);
              this.router.navigate(['/dashboard']);
            },
            error: (err) => {
              this.cargando.set(false);
              // Podríamos mostrar una alerta, pero ya están registrados, mejor llevarlos al dashboard e informar.
              alert('Te has registrado correctamente, pero hubo un error importando los equipos: ' + (err.error?.error || ''));
              this.router.navigate(['/dashboard']);
            }
          });
        } else {
          this.cargando.set(false);
          this.router.navigate(['/dashboard']);
        }
      },
      error: (err) => {
        this.cargando.set(false);
        this.error.set(
          err.error?.error || 'Error al registrar. Inténtalo de nuevo.'
        );
      }
    });
  }
}
