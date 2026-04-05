package co.udea.semilleros.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "actividad_cientifica")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActividadCientificaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_actividad")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "categoria", length = 100)
    private String categoria;
}
