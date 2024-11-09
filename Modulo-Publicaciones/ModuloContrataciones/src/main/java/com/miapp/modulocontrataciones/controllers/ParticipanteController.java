package com.miapp.modulocontrataciones.controllers;

import com.miapp.modulocontrataciones.Services.Participantes.ParticipantesService;
import com.miapp.sistemasdistribuidos.dto.ParticipantesDTO;
import com.miapp.sistemasdistribuidos.dto.EventoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/participantes")
public class ParticipanteController {

    @Autowired
    private ParticipantesService participantesService;

    @Value("${pageSize}")
    int size;

    // Obtener todos los participantes paginados
    @GetMapping("/actividad/{actividadId}")
    public ResponseEntity<Page<ParticipantesDTO>> getAllParticipantes(
            @RequestParam(defaultValue = "0") int page,
            @PathVariable Integer actividadId
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ParticipantesDTO> result = participantesService.getByActividad(actividadId, pageable);
        return ResponseEntity.ok(result);
    }

    // Obtener un participante por su ID
    @GetMapping("/{id}")
    public ResponseEntity<ParticipantesDTO> getParticipanteById(@PathVariable Integer id) {
        Optional<ParticipantesDTO> participante = participantesService.getByID(id);
        return participante.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // Crear un nuevo participante
    @PostMapping
    public ResponseEntity<EventoDTO> createParticipante(@RequestBody ParticipantesDTO participantesDTO) {
        EventoDTO newParticipante = participantesService.create(participantesDTO);
        return ResponseEntity.ok(newParticipante);
    }

    // Actualizar un participante existente
    @PutMapping("/{id}")
    public ResponseEntity<EventoDTO> updateParticipante(@PathVariable Integer id, @RequestBody ParticipantesDTO participantesDTO) {
        EventoDTO updatedParticipante = participantesService.update(id, participantesDTO);
        return updatedParticipante != null ? ResponseEntity.ok(updatedParticipante) : ResponseEntity.notFound().build();
    }

    // Eliminar un participante por su ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteParticipante(@PathVariable Integer id) {
        participantesService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

