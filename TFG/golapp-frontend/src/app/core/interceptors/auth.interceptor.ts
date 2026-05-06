import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Interceptor funcional que adjunta el token JWT a todas las peticiones HTTP salientes.
 * Excluye las rutas de autenticación (login/registro) para evitar enviar tokens expirados.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // No adjuntar token a las rutas de autenticación
  if (req.url.includes('/api/auth/')) {
    return next(req);
  }

  const authService = inject(AuthService);
  const token = authService.obtenerToken();

  if (token) {
    const clonedReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    return next(clonedReq);
  }

  return next(req);
};
