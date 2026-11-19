package com.petsalud.backendpetsalud.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "mascotas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mascota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private String especie;

    private String raza;

    private LocalDate fechaNacimiento;

    private String sexo;

    public void validarNombreYEspecie() {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la mascota es obligatorio");
        }
        if (especie == null || especie.isBlank()) {
            throw new IllegalArgumentException("La especie de la mascota es obligatoria");
        }
    }

    // Relaciones
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    @JsonIgnoreProperties("mascotas")
    private Usuario propietario;

    @OneToMany(mappedBy = "mascota", cascade = CascadeType.ALL)
    private List<Cita> citas;

    @OneToMany(mappedBy = "mascota", cascade = CascadeType.ALL)
    private List<ExpedienteMedico> expedientesMedicos;
}
