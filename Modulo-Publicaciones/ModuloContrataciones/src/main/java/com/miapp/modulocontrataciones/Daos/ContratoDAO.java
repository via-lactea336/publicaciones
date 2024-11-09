package com.miapp.modulocontrataciones.Daos;

import com.miapp.sistemasdistribuidos.entity.Cliente;
import com.miapp.sistemasdistribuidos.entity.Contrato;
import com.miapp.sistemasdistribuidos.entity.Estado;
import com.miapp.sistemasdistribuidos.entity.Publicacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContratoDAO extends JpaRepository<Contrato, Integer> {
    Page<Contrato> findAllByClienteId(Cliente cliente, Pageable pageable);
    Page<Contrato> findAllByPublicacionId(Publicacion publicacion, Pageable pageable);
    Page<Contrato> findAllByEstadoId(Estado estado, Pageable pageable);
    Page<Contrato> findAll(Pageable pageable);
}
