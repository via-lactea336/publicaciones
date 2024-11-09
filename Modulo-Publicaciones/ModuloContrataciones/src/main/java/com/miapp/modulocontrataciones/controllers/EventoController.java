package com.miapp.modulocontrataciones.controllers;

import com.miapp.modulocontrataciones.Services.Eventos.EventoService;
import com.miapp.sistemasdistribuidos.dto.EventoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    @Autowired
    private EventoService eventoService;

    @Value("${pageSize}")
    private int pageSize;

    // Obtener todos los eventos con paginación
    @GetMapping
    public ResponseEntity<Page<EventoDTO>> getAllEventos(
            @RequestParam(defaultValue = "0") int page
    ) {
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<EventoDTO> eventos = eventoService.getAll(pageable);
        return ResponseEntity.ok(eventos);
    }

    // Obtener un evento por su ID
    @GetMapping("/{id}")
    public ResponseEntity<EventoDTO> getEventoById(@PathVariable Integer id) {
        EventoDTO eventoDTO = eventoService.getById(id);
        return eventoDTO != null ? ResponseEntity.ok(eventoDTO) : ResponseEntity.notFound().build();
    }

    // Crear un nuevo evento
    @PostMapping
    public ResponseEntity<EventoDTO> createEvento(@RequestBody EventoDTO eventoDTO) {
        EventoDTO nuevoEvento = eventoService.create(eventoDTO);
        return ResponseEntity.ok(nuevoEvento);
    }

    // Actualizar un evento existente
    @PutMapping("/{id}")
    public ResponseEntity<EventoDTO> updateEvento(
            @PathVariable Integer id,
            @RequestBody EventoDTO eventoDTO
    ) {
        eventoDTO.setEventoId(id); 
        EventoDTO eventoActualizado = eventoService.update(eventoDTO);
        return eventoActualizado != null ? ResponseEntity.ok(eventoActualizado) : ResponseEntity.notFound().build();
    }

    // Eliminar un evento
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvento(@PathVariable Integer id) {
        eventoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
