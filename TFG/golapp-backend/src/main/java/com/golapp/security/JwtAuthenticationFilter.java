package com.golapp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro JWT que intercepta cada petición HTTP.
 * Extrae y valida el token del header "Authorization: Bearer ..."
 * y establece la autenticación en el SecurityContext.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Obtener el header Authorization
        final String authHeader = request.getHeader("Authorization");

        // 2. Si no hay header o no empieza con "Bearer ", continuar sin autenticar
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extraer el token (quitar "Bearer ")
        final String jwt = authHeader.substring(7);

        try {
            // 4. Extraer el username/email del token
            final String userEmail = jwtService.extractUsername(jwt);

            // 5. Si tenemos email y no hay autenticación previa en el contexto
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 6. Cargar el usuario desde la BD
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // 7. Validar el token
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // 8. Crear el objeto de autenticación
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // 9. Establecer la autenticación en el SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.info("✅ JWT válido - Usuario: {} | Método: {} {} | Authorities: {}",
                            userEmail, request.getMethod(), request.getRequestURI(), userDetails.getAuthorities());
                } else {
                    log.warn("❌ JWT inválido para: {} | Método: {} {}", userEmail, request.getMethod(), request.getRequestURI());
                }
            }
        } catch (Exception e) {
            log.warn("❌ Error al procesar token JWT: {} | Método: {} {}", e.getMessage(), request.getMethod(), request.getRequestURI());
        }

        // 10. Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }
}
