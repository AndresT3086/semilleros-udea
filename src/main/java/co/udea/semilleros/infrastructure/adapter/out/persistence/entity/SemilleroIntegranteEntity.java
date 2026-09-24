package co.udea.semilleros.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "semillero_integrante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SemilleroIntegranteEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_semillero", nullable = false)
    private SemilleroEntity semillero;

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "cedula", nullable = false, length = 15)
    private String cedula;

    @Column(name = "correo", length = 150)
    private String correo;

    @Column(name = "sexo", length = 20)
    private String sexo;

    @Column(name = "tipo_vinculacion", length = 50)
    private String tipoVinculacion;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;
}
