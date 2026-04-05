package co.udea.semilleros.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "semillero_actividad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(SemilleroActividadId.class)
public class SemilleroActividadEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_semillero", nullable = false)
    private SemilleroEntity semillero;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_actividad", nullable = false)
    private ActividadCientificaEntity actividad;

    @Column(name = "realiza", nullable = false)
    @Builder.Default
    private Boolean realiza = false;
}
