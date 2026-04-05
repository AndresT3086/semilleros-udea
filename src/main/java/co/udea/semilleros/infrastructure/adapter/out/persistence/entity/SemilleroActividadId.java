package co.udea.semilleros.infrastructure.adapter.out.persistence.entity;

import java.io.Serializable;
import java.util.Objects;

public class SemilleroActividadId implements Serializable {

    private Long semillero;
    private Long actividad;

    public SemilleroActividadId() {}

    public SemilleroActividadId(Long semillero, Long actividad) {
        this.semillero = semillero;
        this.actividad = actividad;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SemilleroActividadId that)) return false;
        return Objects.equals(semillero, that.semillero)
                && Objects.equals(actividad, that.actividad);
    }

    @Override
    public int hashCode() {
        return Objects.hash(semillero, actividad);
    }
}
