package com.miapp.modulocontrataciones.Daos;

import com.miapp.sistemasdistribuidos.entity.Categoria;
import com.miapp.sistemasdistribuidos.entity.Publicacion;
import com.miapp.sistemasdistribuidos.entity.TipoDePrecio;
import com.miapp.sistemasdistribuidos.entity.Trabajador;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface PublicacionDAO extends JpaRepository<Publicacion, Integer> {
    Page<Publicacion> findAll(Pageable pageable);
    Page<Publicacion> findAllByCategoriaId(Categoria categoria, Pageable pageable);
    Page<Publicacion> findAllByTrabajadorId(Trabajador trabajador, Pageable pageable);
    Page<Publicacion> findAllByTipoDePrecioId(TipoDePrecio tipoDePrecio, Pageable pageable);
    @Query("SELECT p FROM Publicacion p WHERE p.categoriaId.nombreCategoria ILIKE :nombreCategoria")
    Page<Publicacion> findAllByNombreCategoria(@Param("nombreCategoria") String nombreCategoria, Pageable pageable);

    Page<Publicacion> findAllByDescripcionContainingIgnoreCaseOrTituloContainingIgnoreCase(String descripcion, String titulo, Pageable pageable);


}
