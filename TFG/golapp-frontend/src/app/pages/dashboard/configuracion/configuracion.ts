import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { UsuarioService, UsuarioResponse } from '../../../core/services/usuario.service';

type TabConfig = 'perfil' | 'preferencias' | 'seguridad';

@Component({
  selector: 'app-configuracion',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './configuracion.html',
  styleUrl: './configuracion.css'
})
export class Configuracion implements OnInit {

  private readonly fb = inject(FormBuilder);
  private readonly usuarioService = inject(UsuarioService);

  // ── Estado ────────────────────────────────────
  protected readonly usuario = signal<UsuarioResponse | null>(null);
  protected readonly cargando = signal(true);
  protected readonly tabActiva = signal<TabConfig>('perfil');
  protected readonly guardando = signal(false);
  protected readonly guardandoPassword = signal(false);
  protected readonly subiendoAvatar = signal(false);
  protected mensajeExito = '';
  protected mensajeError = '';

  // ── Tabs ──────────────────────────────────────
  protected readonly tabs: { label: string; value: TabConfig; iconPath: string }[] = [
    { label: 'Perfil', value: 'perfil', iconPath: 'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z' },
    { label: 'Preferencias', value: 'preferencias', iconPath: 'M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z' },
    { label: 'Seguridad', value: 'seguridad', iconPath: 'M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z' }
  ];

  // ── Formularios ───────────────────────────────
  protected readonly perfilForm: FormGroup = this.fb.group({
    nombre: ['', Validators.required],
    telefono: ['']
  });

  protected readonly prefsForm: FormGroup = this.fb.group({
    duracionPartidoDefecto: [90, [Validators.required, Validators.min(1)]]
  });

  protected readonly passwordForm: FormGroup = this.fb.group({
    passwordActual: ['', [Validators.required, Validators.minLength(6)]],
    nuevaPassword: ['', [Validators.required, Validators.minLength(6)]],
    confirmarPassword: ['', [Validators.required, Validators.minLength(6)]]
  });

  // ── Fuerza de contraseña ──────────────────────
  protected readonly fuerzaPassword = signal(0);
  protected readonly fuerzaLabels = ['', 'Muy débil', 'Débil', 'Buena', 'Excelente'];
  protected readonly fuerzaColors = ['', 'bg-red-500', 'bg-golapp-warning', 'bg-golapp-blue', 'bg-golapp-green'];
  protected readonly fuerzaTextColors = ['', 'text-red-400', 'text-golapp-warning', 'text-golapp-blue', 'text-golapp-green'];

  // ── Lifecycle ─────────────────────────────────
  ngOnInit(): void {
    this.cargarPerfil();
  }

  private cargarPerfil(): void {
    this.cargando.set(true);
    this.usuarioService.obtenerPerfil().subscribe({
      next: (u) => {
        this.usuario.set(u);
        this.perfilForm.patchValue({ nombre: u.nombre, telefono: u.telefono || '' });
        this.prefsForm.patchValue({ duracionPartidoDefecto: u.duracionPartidoDefecto || 90 });
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false)
    });
  }

  // ── Tabs ──────────────────────────────────────
  protected setTab(tab: TabConfig): void {
    this.tabActiva.set(tab);
    this.limpiarMensajes();
  }

  // ── Guardar Perfil ────────────────────────────
  protected guardarPerfil(): void {
    if (this.perfilForm.invalid) return;
    this.guardando.set(true);
    this.limpiarMensajes();
    const dto = {
      nombre: this.perfilForm.value.nombre,
      telefono: this.perfilForm.value.telefono,
      avatarUrl: this.usuario()?.avatarUrl || '',
      duracionPartidoDefecto: this.usuario()?.duracionPartidoDefecto || 90
    };
    this.usuarioService.actualizarPerfil(dto).subscribe({
      next: (u) => { this.usuario.set(u); this.guardando.set(false); this.mensajeExito = 'Perfil actualizado correctamente.'; },
      error: (err) => { this.guardando.set(false); this.mensajeError = err.error?.error || 'Error al guardar.'; }
    });
  }

  // ── Guardar Preferencias ──────────────────────
  protected guardarPreferencias(): void {
    if (this.prefsForm.invalid) return;
    this.guardando.set(true);
    this.limpiarMensajes();
    const u = this.usuario();
    const dto = {
      nombre: u?.nombre || '',
      telefono: u?.telefono || '',
      avatarUrl: u?.avatarUrl || '',
      duracionPartidoDefecto: this.prefsForm.value.duracionPartidoDefecto
    };
    this.usuarioService.actualizarPerfil(dto).subscribe({
      next: (updated) => { this.usuario.set(updated); this.guardando.set(false); this.mensajeExito = 'Preferencias guardadas.'; },
      error: (err) => { this.guardando.set(false); this.mensajeError = err.error?.error || 'Error al guardar.'; }
    });
  }

  // ── Cambiar Contraseña ────────────────────────
  protected cambiarPassword(): void {
    this.limpiarMensajes();
    if (this.passwordForm.invalid) { this.passwordForm.markAllAsTouched(); return; }
    const { passwordActual, nuevaPassword, confirmarPassword } = this.passwordForm.value;
    if (nuevaPassword !== confirmarPassword) {
      this.mensajeError = 'Las contraseñas no coinciden.';
      return;
    }
    this.guardandoPassword.set(true);
    this.usuarioService.cambiarPassword({ passwordActual, nuevaPassword }).subscribe({
      next: () => { this.guardandoPassword.set(false); this.mensajeExito = 'Contraseña actualizada correctamente.'; this.passwordForm.reset(); this.fuerzaPassword.set(0); },
      error: (err) => { this.guardandoPassword.set(false); this.mensajeError = err.error?.error || 'Error al cambiar contraseña.'; }
    });
  }

  protected onNuevaPasswordInput(): void {
    this.calcularFuerza(this.passwordForm.value.nuevaPassword || '');
  }

  private calcularFuerza(pass: string): void {
    let f = 0;
    if (pass.length >= 6) f++;
    if (pass.length >= 10) f++;
    if (/[A-Z]/.test(pass) && /[a-z]/.test(pass)) f++;
    if (/[0-9]/.test(pass) && /[^A-Za-z0-9]/.test(pass)) f++;
    this.fuerzaPassword.set(f);
  }

  protected eliminarCuenta(): void {
    if (confirm('⚠️ ¿Estás seguro de que quieres eliminar tu cuenta? Esta acción no se puede deshacer.')) {
      alert('Funcionalidad pendiente de implementación.');
    }
  }

  // ── Subida de Avatar ───────────────────────────
  protected onAvatarSeleccionado(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const file = input.files[0];
    if (file.size > 5 * 1024 * 1024) { this.mensajeError = 'El archivo no puede superar los 5MB.'; return; }
    this.subiendoAvatar.set(true);
    this.limpiarMensajes();
    this.usuarioService.subirAvatar(file).subscribe({
      next: (res) => {
        this.usuario.update(u => u ? { ...u, avatarUrl: res.avatarUrl } : u);
        this.subiendoAvatar.set(false);
        this.mensajeExito = 'Avatar actualizado.';
      },
      error: (err) => { this.subiendoAvatar.set(false); this.mensajeError = err.error?.error || 'Error al subir avatar.'; }
    });
  }

  protected get avatarCompleto(): string {
    const u = this.usuario();
    if (!u?.avatarUrl) return '';
    if (u.avatarUrl.startsWith('http')) return u.avatarUrl;
    return 'http://localhost:8089' + u.avatarUrl;
  }

  // ── Helpers ───────────────────────────────────
  protected getIniciales(): string {
    const u = this.usuario();
    if (!u) return '?';
    return (u.nombre?.charAt(0) || '').toUpperCase() + (u.apellidos?.charAt(0) || '').toUpperCase() || u.nombre?.charAt(0)?.toUpperCase() || '?';
  }

  private limpiarMensajes(): void {
    this.mensajeExito = '';
    this.mensajeError = '';
  }
}
