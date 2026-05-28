package co.udea.semilleros.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "semillero_relacionamiento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SemilleroRelacionamientoEntity {

    @Id
    @Column(name = "id_semillero")
    private Long idSemillero;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id_semillero")
    private SemilleroEntity semillero;

    @Column(name = "adscrito_grupo", nullable = false)
    @Builder.Default
    private Boolean adscritoGrupo = false;

    @Column(name = "grupo_investigacion", length = 300)
    private String grupoInvestigacion;

    @Column(name = "relacion_grupo", length = 200)
    private String relacionGrupo;

    @Column(name = "centro_investigaciones", length = 300)
    private String centroInvestigaciones;

    @Column(name = "relacion_centro", length = 200)
    private String relacionCentro;

    @Column(name = "departamento", length = 200)
    private String departamento;

    @Column(name = "relacion_departamento", length = 200)
    private String relacionDepartamento;

    @Column(name = "facultad", length = 200)
    private String facultad;

    @Column(name = "relacion_facultad", length = 200)
    private String relacionFacultad;
}
