package com.petsalud.backendpetsalud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository repositorio;
    @InjectMocks
    private UsuarioServiceImpl servicio;

    private final Usuario usuario = Usuario.builder()
            .email("ana@example.com").nombre("Ana").rol(Rol.CLIENTE).build();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void rechazaEmailInvalido(String email) {
        usuario.setEmail(email);
        verificarRechazo("El email es obligatorio");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void rechazaNombreInvalido(String nombre) {
        usuario.setNombre(nombre);
        verificarRechazo("El nombre es obligatorio");
    }

    @Test
    void rechazaRolNulo() {
        usuario.setRol(null);
        verificarRechazo("El rol es obligatorio");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "DESCONOCIDO"})
    void rechazaRolInvalidoAlLeerJson(String rol) {
        var mapper = new ObjectMapper();
        String json = "{\"rol\":\"" + rol + "\"}";
        assertThrows(InvalidFormatException.class, () -> mapper.readValue(json, Usuario.class));
    }

    @ParameterizedTest
    @EnumSource(Rol.class)
    void permiteRegistrarCadaRolValido(Rol rol) {
        usuario.setRol(rol);
        when(repositorio.save(usuario)).thenReturn(usuario);

        assertEquals(rol, servicio.registrarUsuario(usuario).getRol());
        verify(repositorio).save(usuario);
    }

    @Test
    void rechazaEmailDuplicado() {
        when(repositorio.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        verificarRechazo("El email ya está registrado");
    }

    @Test
    void registraClienteConVariasMascotas() {
        Mascota luna = Mascota.builder().nombre("Luna").especie("Canina").build();
        Mascota coco = Mascota.builder().nombre("Coco").especie("Felina").build();
        usuario.setMascotas(List.of(luna, coco));
        when(repositorio.save(same(usuario))).thenReturn(usuario);

        Usuario registrado = servicio.registrarUsuario(usuario);

        assertAll(
                () -> assertEquals(2, registrado.getMascotas().size()),
                () -> assertSame(luna, registrado.getMascotas().get(0)),
                () -> assertSame(coco, registrado.getMascotas().get(1)),
                () -> assertSame(registrado, luna.getPropietario()),
                () -> assertSame(registrado, coco.getPropietario()),
                () -> assertEquals("Luna", luna.getNombre()),
                () -> assertEquals("Felina", coco.getEspecie())
        );
        verify(repositorio).save(same(usuario));
        var json = new ObjectMapper().valueToTree(registrado);
        var propietarioJson = json.path("mascotas").get(0).path("propietario");
        assertEquals("ana@example.com", propietarioJson.path("email").asText());
        assertFalse(propietarioJson.has("mascotas"));
    }

    @Test
    void rechazaMascotaNulaEnElRegistro() {
        usuario.setMascotas(Collections.singletonList(null));
        verificarRechazo("La lista contiene una mascota nula");
    }

    @Test
    void rechazaMascotaConDatosInvalidosEnElRegistro() {
        usuario.setMascotas(List.of(Mascota.builder().nombre("Luna").especie(" ").build()));
        verificarRechazo("La especie de la mascota es obligatoria");
    }

    @Test
    void actualizaNombreYConservaEmailYRol() {
        when(repositorio.findById(1L)).thenReturn(Optional.of(usuario));
        when(repositorio.save(same(usuario))).thenReturn(usuario);
        Usuario cambios = Usuario.builder().nombre("Ana María").email("otro@example.com").rol(Rol.ADMIN).build();

        Usuario actualizado = servicio.actualizarUsuario(1L, cambios);

        assertEquals("Ana María", actualizado.getNombre());
        assertEquals("ana@example.com", actualizado.getEmail());
        assertEquals(Rol.CLIENTE, actualizado.getRol());
    }

    @Test
    void rechazaActualizarNombreVacio() {
        when(repositorio.findById(1L)).thenReturn(Optional.of(usuario));

        assertThrows(IllegalArgumentException.class,
                () -> servicio.actualizarUsuario(1L, Usuario.builder().nombre("").build()));
        assertEquals("Ana", usuario.getNombre());
        verify(repositorio, never()).save(any());
    }

    @Test
    void rechazaActualizarUsuarioInexistente() {
        var error = assertThrows(IllegalArgumentException.class, () -> servicio.actualizarUsuario(1L, usuario));
        assertEquals("Usuario no encontrado", error.getMessage());
        verify(repositorio, never()).save(any());
    }

    private void verificarRechazo(String mensaje) {
        var error = assertThrows(IllegalArgumentException.class, () -> servicio.registrarUsuario(usuario));
        assertEquals(mensaje, error.getMessage());
        verify(repositorio, never()).save(any());
    }
}
