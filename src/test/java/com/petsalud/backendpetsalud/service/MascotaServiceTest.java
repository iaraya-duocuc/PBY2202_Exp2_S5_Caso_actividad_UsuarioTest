package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.MascotaRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MascotaServiceTest {

    @Mock
    private MascotaRepository mascotas;
    @Mock
    private UsuarioRepository usuarios;
    @InjectMocks
    private MascotaServiceImpl servicio;

    private final Usuario cliente = Usuario.builder().id(1L).rol(Rol.CLIENTE).build();
    private final Mascota mascota = Mascota.builder().nombre("Luna").especie("Canina").build();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void rechazaNombreInvalido(String nombre) {
        mascota.setNombre(nombre);
        verificarRegistroRechazado("El nombre de la mascota es obligatorio");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void rechazaEspecieInvalida(String especie) {
        mascota.setEspecie(especie);
        verificarRegistroRechazado("La especie de la mascota es obligatoria");
    }

    @Test
    void registraMascotaValidaConClienteExistente() {
        when(usuarios.findById(1L)).thenReturn(Optional.of(cliente));
        when(mascotas.save(same(mascota))).thenReturn(mascota);

        Mascota registrada = servicio.registrarMascota(mascota, 1L);

        assertSame(cliente, registrada.getPropietario());
        assertEquals("Luna", registrada.getNombre());
        assertEquals("Canina", registrada.getEspecie());
        verify(mascotas).save(same(mascota));
    }

    @Test
    void actualizaNombreYEspecieValidos() {
        when(mascotas.findById(2L)).thenReturn(Optional.of(mascota));
        when(mascotas.save(same(mascota))).thenReturn(mascota);
        Mascota cambios = Mascota.builder().nombre("Coco").especie("Felina").build();

        Mascota actualizada = servicio.actualizarMascota(2L, cambios);

        assertEquals("Coco", actualizada.getNombre());
        assertEquals("Felina", actualizada.getEspecie());
    }

    @Test
    void rechazaActualizacionConEspecieVacia() {
        when(mascotas.findById(2L)).thenReturn(Optional.of(mascota));
        Mascota cambios = Mascota.builder().nombre("Coco").especie("").build();

        assertThrows(IllegalArgumentException.class, () -> servicio.actualizarMascota(2L, cambios));
        assertEquals("Luna", mascota.getNombre());
        verify(mascotas, never()).save(any());
    }

    @Test
    void rechazaActualizarMascotaInexistente() {
        var error = assertThrows(IllegalArgumentException.class, () -> servicio.actualizarMascota(2L, mascota));

        assertEquals("Mascota no encontrada", error.getMessage());
        verify(mascotas, never()).save(any());
    }

    @Test
    void listaLasMascotasDelPropietarioSolicitado() {
        when(mascotas.findByPropietarioId(1L)).thenReturn(List.of(mascota));

        assertEquals(List.of(mascota), servicio.listarMascotasPorPropietario(1L));
    }

    private void verificarRegistroRechazado(String mensaje) {
        when(usuarios.findById(1L)).thenReturn(Optional.of(cliente));

        var error = assertThrows(IllegalArgumentException.class, () -> servicio.registrarMascota(mascota, 1L));

        assertEquals(mensaje, error.getMessage());
        verify(mascotas, never()).save(any());
    }
}
