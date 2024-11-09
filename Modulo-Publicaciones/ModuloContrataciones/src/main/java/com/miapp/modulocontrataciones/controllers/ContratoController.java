package com.miapp.modulocontrataciones.controllers;

import com.miapp.modulocontrataciones.Services.Contratos.ContratoService;
import com.miapp.sistemasdistribuidos.dto.ContratoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

@RestController
@RequestMapping("/api/contratos")
public class ContratoController {

    @Autowired
    private ContratoService contratoService;

    @Value("${pageSize}")
    int size;

    // Obtener todos los contratos con posibilidad de filtrar por cliente, publicacion o estado
    @GetMapping
    public ResponseEntity<Page<ContratoDTO>> getAllContratos(
            @RequestParam Optional<Integer> clienteId,
            @RequestParam Optional<Integer> publicacionId,
            @RequestParam Optional<Integer> estadoId,
            @RequestParam(defaultValue = "0") int page
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ContratoDTO> result;

        if (clienteId.isPresent()) {
            result = contratoService.findByCliente(clienteId.get(),  pageable);
        } else if (publicacionId.isPresent()) {
            result = contratoService.findByPublicacion(publicacionId.get(),  pageable);
        } else if (estadoId.isPresent()) {
            result = contratoService.findByEstado(estadoId.get(),pageable);
        } else {
            result = contratoService.getAllContratos(pageable);
        }
        return ResponseEntity.ok(result);
    }

    // Obtener un contrato por ID
    @GetMapping("/{id}")
    public ResponseEntity<ContratoDTO> getContratoById(@PathVariable Integer id) {
        Optional<ContratoDTO> contrato = contratoService.getContratoById(id);
        return contrato.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // Crear un nuevo contrato
    @PostMapping
    public ResponseEntity<ContratoDTO> createContrato(@RequestBody ContratoDTO contratoDTO) {
        ContratoDTO newContrato = contratoService.createContrato(contratoDTO);
        return ResponseEntity.ok(newContrato);
    }

    // Actualizar un contrato existente
    @PutMapping("/{id}")
    public ResponseEntity<ContratoDTO> updateContrato(@PathVariable Integer id, @RequestBody ContratoDTO contratoDTO) {
        contratoDTO.setContratoId(id);
        ContratoDTO updatedContrato = contratoService.updateContrato(id, contratoDTO);
        return updatedContrato != null ? ResponseEntity.ok(updatedContrato) : ResponseEntity.notFound().build();
    }

    // Eliminar un contrato
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContrato(@PathVariable Integer id) {
        contratoService.deleteContrato(id);
        return ResponseEntity.noContent().build();
    }
}
