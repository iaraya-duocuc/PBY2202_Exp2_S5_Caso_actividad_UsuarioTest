package com.petsalud.backendpetsalud.repository;

import com.petsalud.backendpetsalud.entity.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Usuario> findForUpdateById(Long id);

    Optional<Usuario> findByEmail(String email);
}
