package com.miapp.modulocontrataciones.Services.Eventos;

import com.miapp.sistemasdistribuidos.dto.EventoDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IEventoService {
    Page<EventoDTO> getAll(Pageable pageable);
    EventoDTO getById(Integer id);
    EventoDTO create(EventoDTO eventoDTO);
    EventoDTO update(EventoDTO eventoDTO);
    void delete(Integer id);
}
