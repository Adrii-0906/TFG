import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  // ── Rutas Públicas (con PublicLayout) ──────────
  {
    path: '',
    loadComponent: () => import('./core/layout/public-layout').then(m => m.PublicLayout),
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/landing/landing').then(m => m.Landing),
        title: 'GOLAPP — Gestión de Torneos de Fútbol Amateur'
      },
      {
        path: 'login',
        loadComponent: () => import('./features/auth/login/login').then(m => m.Login),
        title: 'Iniciar Sesión — GOLAPP'
      },
      {
        path: 'registro',
        loadComponent: () => import('./features/auth/registro/registro').then(m => m.Registro),
        title: 'Registro — GOLAPP'
      }
    ]
  },

  // ── Rutas del Dashboard (protegidas con AuthGuard) ──
  {
    path: 'dashboard',
    loadComponent: () => import('./core/layout/dashboard-layout').then(m => m.DashboardLayout),
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/dashboard/dashboard-home').then(m => m.DashboardHome),
        title: 'Dashboard — GOLAPP'
      },
      {
        path: 'equipos',
        children: [
          {
            path: '',
            loadComponent: () => import('./pages/dashboard/equipos/equipos').then(m => m.Equipos),
            title: 'Equipos — GOLAPP'
          },
          {
            path: 'nuevo',
            loadComponent: () => import('./features/dashboard/equipos/nuevo-equipo/nuevo-equipo').then(m => m.NuevoEquipo),
            title: 'Registrar Equipo — GOLAPP'
          },
          {
            path: ':id',
            loadComponent: () => import('./features/dashboard/equipos/detalle/equipo-detalle').then(m => m.EquipoDetalle),
            title: 'Detalle Equipo — GOLAPP'
          }
        ]
      },
      {
        path: 'torneos',
        children: [
          {
            path: '',
            loadComponent: () => import('./pages/dashboard/torneos/torneos').then(m => m.Torneos),
            title: 'Mis Torneos — GOLAPP'
          },
          {
            path: 'nuevo',
            loadComponent: () => import('./features/dashboard/torneos/nuevo-torneo/nuevo-torneo').then(m => m.NuevoTorneo),
            title: 'Crear Torneo — GOLAPP'
          },
          {
            path: ':id',
            loadComponent: () => import('./features/dashboard/torneos/detalle/torneo-detalle').then(m => m.TorneoDetalle),
            title: 'Gestionar Torneo — GOLAPP'
          }
        ]
      },
      {
        path: 'calendario',
        loadComponent: () => import('./pages/dashboard/calendario/calendario').then(m => m.Calendario),
        title: 'Calendario — GOLAPP'
      },
      {
        path: 'configuracion',
        loadComponent: () => import('./pages/dashboard/configuracion/configuracion').then(m => m.Configuracion),
        title: 'Configuración — GOLAPP'
      }
    ]
  },

  // ── Portal del Participante (público, sin AuthGuard) ──
  {
    path: 'entrar',
    loadComponent: () => import('./pages/participante/acceso-participante').then(m => m.AccesoParticipante),
    title: 'Accede a tu Equipo — GOLAPP'
  },
  {
    path: 'mi-equipo',
    loadComponent: () => import('./pages/participante/panel-participante').then(m => m.PanelParticipante),
    title: 'Mi Equipo — GOLAPP'
  },

  // ── Wildcard → Redirigir al inicio ─────────────
  { path: '**', redirectTo: '' }
];
