package com.miapp.modulocontrataciones.Services.Publicaciones;

import com.miapp.sistemasdistribuidos.dto.PublicacionDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface IPublicacionService {
    Page<PublicacionDTO> findAllPublicacion(Pageable pageable);
    Optional<PublicacionDTO> findById(Integer id);
    PublicacionDTO createPublicacion(PublicacionDTO publicacionDTO);

    PublicacionDTO updatePublicacion(Integer id, PublicacionDTO publicacionDTO);

    void deletePublicacion(Integer id);

    Page<PublicacionDTO> findAllByTrabajadorId(Integer trabajadorId, Pageable pageable);
    Page<PublicacionDTO> findAllByCategoriaId(Integer categoriaId, Pageable pageable);
    Page<PublicacionDTO> findAllByTipoDePrecioId(Integer tipoDePrecioId, Pageable pageable);
    Page<PublicacionDTO> findAllByNombreCategoria(String nombreCategoria, Pageable pageable);
    Page<PublicacionDTO> findAllByDescripcionContainingIgnoreCaseOrTituloContainingIgnoreCase(String descripcion, String titulo, Pageable pageable);
}
