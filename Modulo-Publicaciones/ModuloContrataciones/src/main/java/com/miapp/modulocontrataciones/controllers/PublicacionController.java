package com.miapp.modulocontrataciones.controllers;
import com.miapp.modulocontrataciones.Services.Publicaciones.PublicacionService;
import com.miapp.sistemasdistribuidos.dto.PublicacionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/publicaciones")
public class PublicacionController {

    @Autowired
    private PublicacionService publicacionService;

    @Value("${pageSize}")
    int size;

    // Obtener todas las publicaciones con posibilidad de filtrar por trabajador, categoría o tipo de precio
    @GetMapping
    public ResponseEntity<Page<PublicacionDTO>> getAllPublicaciones(
            @RequestParam Optional<Integer> trabajadorId,
            @RequestParam Optional<Integer> categoriaId,
            @RequestParam Optional<Integer> tipoDePrecioId,
            @RequestParam Optional<String> nombreCategoria,
            @RequestParam Optional<String> query,
            @RequestParam(defaultValue = "0") int page
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PublicacionDTO> result;

        if (trabajadorId.isPresent()) {
            result = publicacionService.findAllByTrabajadorId(trabajadorId.get(), pageable);
        } else if (categoriaId.isPresent()) {
            result = publicacionService.findAllByCategoriaId(categoriaId.get(), pageable);
        } else if (tipoDePrecioId.isPresent()) {
            result = publicacionService.findAllByTipoDePrecioId(tipoDePrecioId.get(), pageable);
        }else if (nombreCategoria.isPresent()) {
            result = publicacionService.findAllByNombreCategoria(nombreCategoria.get(), pageable);
        } else if (query.isPresent()) {
            result = publicacionService.findAllByDescripcionContainingIgnoreCaseOrTituloContainingIgnoreCase(query.get(), query.get(), pageable);
        }
        else {
            result = publicacionService.findAllPublicacion(pageable);
        }
        return ResponseEntity.ok(result);
    }

    // Obtener una publicación por ID
    @GetMapping("/{id}")
    public ResponseEntity<PublicacionDTO> getPublicacionById(@PathVariable Integer id) {
        Optional<PublicacionDTO> publicacionDTO = publicacionService.findById(id);
        return publicacionDTO.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // Crear una nueva publicación
    @PostMapping
    public ResponseEntity<PublicacionDTO> createPublicacion(@RequestBody PublicacionDTO publicacionDTO) {
        PublicacionDTO createdPublicacion = publicacionService.createPublicacion(publicacionDTO);
        return ResponseEntity.ok(createdPublicacion);
    }

    // Actualizar una publicación existente
    @PutMapping("/{id}")
    public ResponseEntity<PublicacionDTO> updatePublicacion(@PathVariable Integer id, @RequestBody PublicacionDTO publicacionDTO) {
        publicacionDTO.setPublicacionId(id);
        PublicacionDTO updatedPublicacion = publicacionService.updatePublicacion(id, publicacionDTO);

        if (updatedPublicacion == null) {
                return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updatedPublicacion);
    }


    // Eliminar una publicación
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePublicacion(@PathVariable Integer id) {
        Optional<PublicacionDTO> existingPublicacion = publicacionService.findById(id);
        if (existingPublicacion.isPresent()) {
            publicacionService.deletePublicacion(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
