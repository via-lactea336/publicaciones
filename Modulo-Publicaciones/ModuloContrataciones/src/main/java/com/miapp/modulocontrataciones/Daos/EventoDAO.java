package com.miapp.modulocontrataciones.Daos;

import com.miapp.sistemasdistribuidos.entity.Evento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface EventoDAO extends JpaRepository<Evento, Integer> {
    Page<Evento> findAll(Pageable pageable);
}
