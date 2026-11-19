package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Cita;
import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.CitaRepository;
import com.petsalud.backendpetsalud.repository.MascotaRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CitaServiceTest {

    private static final long MASCOTA_ID = 1L;
    private static final long VETERINARIO_ID = 2L;
    private static final long PROPIETARIO_ID = 3L;
    private static final LocalDateTime FECHA_HORA = LocalDateTime.of(2030, 6, 12, 10, 30);
    private static final String MOTIVO = "Control veterinario";

    @Mock
    private CitaRepository citaRepository;
    @Mock
    private MascotaRepository mascotaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @InjectMocks
    private CitaServiceImpl servicio;

    private final Usuario propietario = Usuario.builder().id(PROPIETARIO_ID).rol(Rol.CLIENTE).build();
    private final Usuario veterinario = Usuario.builder().id(VETERINARIO_ID).rol(Rol.VETERINARIO).build();
    private final Mascota mascota = Mascota.builder().id(MASCOTA_ID).propietario(propietario).build();

    @Test
    void agendaCitaConVeterinarioYPropietarioRegistrados() {
        simularMascotaYVeterinario();
        when(usuarioRepository.existsById(PROPIETARIO_ID)).thenReturn(true);
        when(citaRepository.existsByVeterinarioIdAndFechaHoraAndEstadoNot(VETERINARIO_ID, FECHA_HORA, "Cancelada"))
                .thenReturn(false);
        when(citaRepository.save(any(Cita.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Cita cita = servicio.agendarCita(MASCOTA_ID, VETERINARIO_ID, FECHA_HORA, MOTIVO);

        assertAll(
                () -> assertEquals("Agendada", cita.getEstado()),
                () -> assertEquals(FECHA_HORA, cita.getFechaHora()),
                () -> assertEquals(MOTIVO, cita.getMotivo()),
                () -> assertSame(veterinario, cita.getVeterinario()),
                () -> assertSame(mascota, cita.getMascota()),
                () -> assertSame(propietario, cita.getMascota().getPropietario())
        );
        verify(usuarioRepository).existsById(PROPIETARIO_ID);
        verify(citaRepository).save(same(cita));
    }

    @Test
    void rechazaHorarioOcupado() {
        simularMascotaYVeterinario();
        when(usuarioRepository.existsById(PROPIETARIO_ID)).thenReturn(true);
        when(citaRepository.existsByVeterinarioIdAndFechaHoraAndEstadoNot(VETERINARIO_ID, FECHA_HORA, "Cancelada"))
                .thenReturn(true);

        assertAgendamientoRechazado("El veterinario ya tiene una cita en ese horario");
    }

    @Test
    void rechazaMascotaInexistente() {
        when(mascotaRepository.findById(MASCOTA_ID)).thenReturn(Optional.empty());

        assertAgendamientoRechazado("Mascota no encontrada");
    }

    @Test
    void rechazaVeterinarioInexistente() {
        when(mascotaRepository.findById(MASCOTA_ID)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findForUpdateById(VETERINARIO_ID)).thenReturn(Optional.empty());

        assertAgendamientoRechazado("Veterinario no encontrado");
    }

    @ParameterizedTest
    @EnumSource(value = Rol.class, names = {"CLIENTE", "ADMIN"})
    void rechazaUsuarioConRolDistintoDeVeterinario(Rol rol) {
        veterinario.setRol(rol);
        simularMascotaYVeterinario();

        assertAgendamientoRechazado("El usuario seleccionado no es veterinario");
    }

    @Test
    void rechazaMascotaSinPropietario() {
        mascota.setPropietario(null);
        simularMascotaYVeterinario();

        assertAgendamientoRechazado("La mascota debe tener un propietario registrado");
    }

    @Test
    void rechazaPropietarioSinIdentificador() {
        propietario.setId(null);
        simularMascotaYVeterinario();

        assertAgendamientoRechazado("La mascota debe tener un propietario registrado");
    }

    @Test
    void rechazaPropietarioNoRegistrado() {
        simularMascotaYVeterinario();
        when(usuarioRepository.existsById(PROPIETARIO_ID)).thenReturn(false);

        assertAgendamientoRechazado("La mascota debe tener un propietario registrado");
    }

    @Test
    void actualizaElEstadoDeUnaCitaExistente() {
        Cita cita = Cita.builder().id(1L).estado("Agendada").build();
        when(citaRepository.findById(1L)).thenReturn(Optional.of(cita));
        when(citaRepository.save(cita)).thenReturn(cita);

        assertEquals("Completada", servicio.actualizarEstadoCita(1L, "Completada").getEstado());
        verify(citaRepository).save(cita);
    }

    @Test
    void rechazaActualizarUnaCitaInexistente() {
        when(citaRepository.findById(1L)).thenReturn(Optional.empty());

        var excepcion = assertThrows(IllegalArgumentException.class,
                () -> servicio.actualizarEstadoCita(1L, "Completada"));

        assertEquals("Cita no encontrada", excepcion.getMessage());
        verify(citaRepository, never()).save(any());
    }

    @Test
    void cancelaLaCitaPorIdentificador() {
        servicio.cancelarCita(1L);

        verify(citaRepository).deleteById(1L);
    }

    private void simularMascotaYVeterinario() {
        when(mascotaRepository.findById(MASCOTA_ID)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findForUpdateById(VETERINARIO_ID)).thenReturn(Optional.of(veterinario));
    }

    private void assertAgendamientoRechazado(String mensaje) {
        var excepcion = assertThrows(IllegalArgumentException.class,
                () -> servicio.agendarCita(MASCOTA_ID, VETERINARIO_ID, FECHA_HORA, MOTIVO));

        assertEquals(mensaje, excepcion.getMessage());
        verify(citaRepository, never()).save(any());
    }
}
