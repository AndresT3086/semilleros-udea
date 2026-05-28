package co.udea.semilleros.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "produccion_academica")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProduccionAcademicaEntity {

    @Id
    @Column(name = "id_semillero")
    private Long idSemillero;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id_semillero")
    private SemilleroEntity semillero;

    @Column(name = "tienen_articulos", nullable = false)
    @Builder.Default
    private Boolean tienenArticulos = false;

    @Column(name = "cantidad_articulos", nullable = false)
    @Builder.Default
    private Integer cantidadArticulos = 0;

    @Column(name = "tienen_libros", nullable = false)
    @Builder.Default
    private Boolean tienenLibros = false;

    @Column(name = "cantidad_libros", nullable = false)
    @Builder.Default
    private Integer cantidadLibros = 0;

    @Column(name = "organizan_eventos", nullable = false)
    @Builder.Default
    private Boolean organizanEventos = false;

    @Column(name = "cantidad_eventos", nullable = false)
    @Builder.Default
    private Integer cantidadEventos = 0;

    @Column(name = "participan_eventos", nullable = false)
    @Builder.Default
    private Boolean participaEnEventos  = false;

    @Column(name = "cantidad_participaciones", nullable = false)
    @Builder.Default
    private Integer cantidadParticipaciones = 0;
}
