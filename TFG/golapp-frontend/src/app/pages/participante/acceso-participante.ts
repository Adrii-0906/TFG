import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ParticipanteService } from '../../core/services/participante.service';

@Component({
  selector: 'app-acceso-participante',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './acceso-participante.html',
  styleUrl: './acceso-participante.css'
})
export class AccesoParticipante implements OnInit {

  private readonly router = inject(Router);
  private readonly participanteService = inject(ParticipanteService);

  protected codigo = '';
  protected readonly cargando = signal(false);
  protected error = '';

  ngOnInit(): void {
    const saved = localStorage.getItem('golapp_equipo_codigo');
    if (saved) {
      this.router.navigate(['/mi-equipo']);
    }
  }

  protected acceder(): void {
    const cod = this.codigo.trim().toUpperCase();
    if (!cod) { this.error = 'Introduce un código.'; return; }
    this.cargando.set(true);
    this.error = '';
    this.participanteService.obtenerEquipo(cod).subscribe({
      next: () => {
        localStorage.setItem('golapp_equipo_codigo', cod);
        this.cargando.set(false);
        this.router.navigate(['/mi-equipo']);
      },
      error: () => {
        this.cargando.set(false);
        this.error = 'Código no válido. Verifica con tu organizador.';
      }
    });
  }
}
