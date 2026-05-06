package com.golapp.repository;

import com.golapp.model.Usuario;
import com.golapp.model.enums.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad {@link Usuario}.
 * Proporciona operaciones CRUD y consultas personalizadas sobre usuarios.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca un usuario por su dirección de email.
     *
     * @param email email del usuario
     * @return un Optional con el usuario si existe
     */
    Optional<Usuario> findByEmail(String email);

    /**
     * Busca un usuario por su nombre de usuario.
     *
     * @param username nombre de usuario
     * @return un Optional con el usuario si existe
     */
    Optional<Usuario> findByUsername(String username);

    /**
     * Comprueba si ya existe un usuario con el email dado.
     *
     * @param email email a comprobar
     * @return true si el email ya está registrado
     */
    boolean existsByEmail(String email);

    /**
     * Comprueba si ya existe un usuario con el username dado.
     *
     * @param username username a comprobar
     * @return true si el username ya está registrado
     */
    boolean existsByUsername(String username);

    /**
     * Obtiene todos los usuarios que tengan un rol determinado.
     *
     * @param rol rol a filtrar (ORGANIZADOR, PARTICIPANTE)
     * @return lista de usuarios con ese rol
     */
    List<Usuario> findByRol(Rol rol);
}
