package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public Usuario registrarUsuario(Usuario usuario) {
        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            throw new IllegalArgumentException("El email es obligatorio");
        }
        validarNombre(usuario.getNombre());
        if (usuario.getRol() == null) {
            throw new IllegalArgumentException("El rol es obligatorio");
        }

        // Verificar si el email ya está registrado
        Optional<Usuario> usuarioExistente = usuarioRepository.findByEmail(usuario.getEmail());
        if (usuarioExistente.isPresent()) {
            throw new IllegalArgumentException("El email ya está registrado");
        }
        if (usuario.getMascotas() != null) {
            for (Mascota mascota : usuario.getMascotas()) {
                if (mascota == null) {
                    throw new IllegalArgumentException("La lista contiene una mascota nula");
                }
                mascota.validarNombreYEspecie();
            }
            usuario.getMascotas().forEach(mascota -> mascota.setPropietario(usuario));
        }
        return usuarioRepository.save(usuario);
    }

    @Override
    public Optional<Usuario> obtenerUsuarioPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    @Override
    public Optional<Usuario> obtenerUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    @Override
    public Usuario actualizarUsuario(Long id, Usuario usuarioActualizado) {
        return usuarioRepository.findById(id).map(usuario -> {
            validarNombre(usuarioActualizado.getNombre());
            usuario.setNombre(usuarioActualizado.getNombre());
            usuario.setApellido(usuarioActualizado.getApellido());
            usuario.setTelefono(usuarioActualizado.getTelefono());
            usuario.setDireccion(usuarioActualizado.getDireccion());
            // No actualizar email ni password aquí por seguridad
            return usuarioRepository.save(usuario);
        }).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
    }

    @Override
    public void eliminarUsuario(Long id) {
        usuarioRepository.deleteById(id);
    }

    @Override
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }
}
