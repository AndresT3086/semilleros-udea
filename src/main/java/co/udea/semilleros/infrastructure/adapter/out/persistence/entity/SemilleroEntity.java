package co.udea.semilleros.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "semillero")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SemilleroEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_semillero")
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "nombre", nullable = true, length = 300)
    private String nombre;

    @Column(name = "siglas", length = 30)
    private String siglas;

    @Column(name = "correo_principal", length = 150)
    private String correoSemillero;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "anio_creacion")
    private Integer anioCreacion;

    @Column(name = "mision", columnDefinition = "TEXT")
    private String mision;

    @Column(name = "vision", columnDefinition = "TEXT")
    private String vision;

    @Column(name = "objetivo", columnDefinition = "TEXT")
    private String objetivo;

    @Column(name = "lineas_investigacion", columnDefinition = "TEXT")
    private String lineasInvestigacion;

    @Column(name = "palabras_clave", length = 500)
    private String palabrasClave;

    @Column(name = "grupo_investigacion", length = 200)
    private String grupoInvestigacion;

    @Column(name = "estado", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private EstadoSemilleroJpa estado;

    @Column(name = "estado_caracterizacion", length = 255)
    private String estadoCaracterizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_unidad_academica")
    private UnidadAcademicaEntity unidadAcademica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_campus")
    private CampusEntity campus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_area_ocde")
    private AreaOcdeEntity areaOcde;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_coordinador")
    private CoordinadorEntity coordinador;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(name = "subarea_ocde", length = 200)
    private String subareaOcde;

    @Column(name = "ods_principal")
    private Long odsPrincipal;

    @Column(name = "observaciones_finales", columnDefinition = "TEXT")
    private String observacionesFinales;

    public enum EstadoSemilleroJpa {
        ACTIVO, INACTIVO, BORRADOR, CARACTERIZADO
    }

}
