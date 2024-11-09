package com.miapp.modulocontrataciones.Daos;


import com.miapp.sistemasdistribuidos.entity.Actividades;
import com.miapp.sistemasdistribuidos.entity.Participantes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParticipantesDAO extends JpaRepository<Participantes, Integer> {
    Page<Participantes> getAllByActividadId(Actividades actividad, Pageable pageable);
}
