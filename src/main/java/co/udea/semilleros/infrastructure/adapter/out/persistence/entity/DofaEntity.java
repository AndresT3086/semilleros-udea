package co.udea.semilleros.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "dofa")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DofaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dofa") private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_semillero", nullable = false)
    private SemilleroEntity semillero;

    @Column(name = "tipo",        nullable = false, length = 20)  private String tipo;
    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT") private String descripcion;
}
