package com.miapp.modulocontrataciones.controllers;

import com.miapp.modulocontrataciones.Services.Actividades.ActividadesService;
import com.miapp.sistemasdistribuidos.dto.ActividadesDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/actividades")
public class ActividadesController {

    @Autowired
    private ActividadesService actividadesService;

    @Value("${pageSize}")
    int size;

    // Obtener todas las actividades por ID del evento con paginación
    @GetMapping("/evento/{eventoId}")
    public ResponseEntity<Page<ActividadesDTO>> getActividadesByEventoId(
            @PathVariable Integer eventoId,
            @RequestParam(defaultValue = "0") int page
    ) {
        Pageable pageable = PageRequest.of(page, size );
        Page<ActividadesDTO> actividades = actividadesService.getAllAByEventoID(pageable, eventoId);
        return ResponseEntity.ok(actividades);
    }

    // Obtener una actividad por su ID
    @GetMapping("/{id}")
    public ResponseEntity<ActividadesDTO> getActividadById(@PathVariable Integer id) {
        ActividadesDTO actividadDTO = actividadesService.getById(id);
        return actividadDTO != null ? ResponseEntity.ok(actividadDTO) : ResponseEntity.notFound().build();
    }

    // Crear una nueva actividad
    @PostMapping
    public ResponseEntity<ActividadesDTO> createActividad(@RequestBody ActividadesDTO actividadesDTO) {
        ActividadesDTO nuevaActividad = actividadesService.create(actividadesDTO);
        return ResponseEntity.ok(nuevaActividad);
    }

    // Actualizar una actividad existente
    @PutMapping("/{id}")
    public ResponseEntity<ActividadesDTO> updateActividad(
            @PathVariable Integer id,
            @RequestBody ActividadesDTO actividadesDTO
    ) {
        actividadesDTO.setActividadId(id);
        ActividadesDTO actividadActualizada = actividadesService.update(actividadesDTO);
        return actividadActualizada != null ? ResponseEntity.ok(actividadActualizada) : ResponseEntity.notFound().build();
    }

    // Eliminar una actividad
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteActividad(@PathVariable Integer id) {
        ActividadesDTO actividadEliminada = actividadesService.delete(id);
        return actividadEliminada != null ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
