package com.golapp.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración de seguridad de la aplicación GOLAPP.
 *
 * Política de acceso:
 * - POST /api/auth/** → Público (registro y login)
 * - GET  /api/torneos/**, /api/partidos/**, /api/clasificacion/**, /api/equipos/** → Público (Read-Only)
 * - POST, PUT, DELETE → Requiere autenticación JWT
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    // ── Cadena de filtros de seguridad ───────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Desactivar CSRF (API REST stateless)
                .csrf(AbstractHttpConfigurer::disable)

                // Configurar CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Configurar autorización de peticiones
                .authorizeHttpRequests(auth -> auth
                        // ── Rutas de error (para ver excepciones) ──
                        .requestMatchers("/error").permitAll()

                        // ── Rutas públicas de autenticación ──
                        .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()

                        // ── Lectura pública (Read-Only para visitantes) ──
                        .requestMatchers(HttpMethod.GET, "/api/torneos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/partidos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/clasificacion/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/equipos/mis-equipos").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/equipos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/me/password").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/me/avatar").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/**").permitAll()
                        .requestMatchers("/archivos/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()

                        // ── Escritura protegida (requiere JWT) ──
                        .requestMatchers(HttpMethod.GET, "/api/partidos/mis-partidos").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/equipos/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/equipos/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/equipos/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/torneos/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/torneos/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/torneos/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/partidos/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/partidos/**").authenticated()

                        // ── Todas las demás peticiones requieren autenticación ──
                        .anyRequest().authenticated()
                )

                // Sesión STATELESS (no se guarda estado en el servidor)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Proveedor de autenticación
                .authenticationProvider(authenticationProvider())

                // Añadir filtro JWT antes del filtro de usuario/contraseña
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ── CORS ────────────────────────────────────────

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Permitir cualquier origen en desarrollo (Angular usa puertos dinámicos)
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/archivos/**", configuration);
        return source;
    }

    // ── Beans de autenticación ──────────────────────

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
